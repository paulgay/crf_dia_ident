# Copyright (C) 2001,2002 Python Software Foundation
# Author: barry@zope.com (Barry Warsaw)

"""Classes to generate plain text from a message object tree.
"""

import re
import sys
import time
import locale
import random

from types import ListType, StringType
from cStringIO import StringIO

from email.Header import Header
from email.Parser import NLCRE

try:
    from email._compat22 import _isstring
except SyntaxError:
    from email._compat21 import _isstring

try:
    True, False
except NameError:
    True = 1
    False = 0

EMPTYSTRING = ''
SEMISPACE = '; '
BAR = '|'
UNDERSCORE = '_'
NL = '\n'
NLTAB = '\n\t'
SEMINLTAB = ';\n\t'
SPACE8 = ' ' * 8

fcre = re.compile(r'^From ', re.MULTILINE)

def _is8bitstring(s):
    if isinstance(s, StringType):
        try:
            unicode(s, 'us-ascii')
        except UnicodeError:
            return True
    return False



class Generator:
    """Generates output from a Message object tree.

    This basic generator writes the message to the given file object as plain
    text.
    """
    #
    # Public interface
    #

    def __init__(self, outfp, mangle_from_=True, maxheaderlen=78):
        """Create the generator for message flattening.

        outfp is the output file-like object for writing the message to.  It
        must have a write() method.

        Optional mangle_from_ is a flag that, when True (the default), escapes
        From_ lines in the body of the message by putting a `>' in front of
        them.

        Optional maxheaderlen specifies the longest length for a non-continued
        header.  When a header line is longer (in characters, with tabs
        expanded to 8 spaces), than maxheaderlen, the header will be broken on
        semicolons and continued as per RFC 2822.  If no semicolon is found,
        then the header is left alone.  Set to zero to disable wrapping
        headers.  Default is 78, as recommended (but not required by RFC
        2822.
        """
        self._fp = outfp
        self._mangle_from_ = mangle_from_
        self.__maxheaderlen = maxheaderlen

    def write(self, s):
        # Just delegate to the file object
        self._fp.write(s)

    def flatten(self, msg, unixfrom=False):
        """Print the message object tree rooted at msg to the output file
        specified when the Generator instance was created.

        unixfrom is a flag that forces the printing of a Unix From_ delimiter
        before the first object in the message tree.  If the original message
        has no From_ delimiter, a `standard' one is crafted.  By default, this
        is False to inhibit the printing of any From_ delimiter.

        Note that for subobjects, no From_ line is printed.
        """
        if unixfrom:
            ufrom = msg.get_unixfrom()
            if not ufrom:
                ufrom = 'From nobody ' + time.ctime(time.time())
            print >> self._fp, ufrom
        self._write(msg)

    # For backwards compatibility, but this is slower
    __call__ = flatten

    def clone(self, fp):
        """Clone this generator with the exact same options."""
        return self.__class__(fp, self._mangle_from_, self.__maxheaderlen)

    #
    # Protected interface - undocumented ;/
    #

    def _write(self, msg):
        # We can't write the headers yet because of the following scenario:
        # say a multipart message includes the boundary string somewhere in
        # its body.  We'd have to calculate the new boundary /before/ we write
        # the headers so that we can write the correct Content-Type:
        # parameter.
        #
        # The way we do this, so as to make the _handle_*() methods simpler,
        # is to cache any subpart writes into a StringIO.  The we write the
        # headers and the StringIO contents.  That way, subpart handlers can
        # Do The Right Thing, and can still modify the Content-Type: header if
        # necessary.
        oldfp = self._fp
        try:
            self._fp = sfp = StringIO()
            self._dispatch(msg)
        finally:
            self._fp = oldfp
        # Write the headers.  First we see if the message object wants to
        # handle that itself.  If not, we'll do it generically.
        meth = getattr(msg, '_write_headers', None)
        if meth is None:
            self._write_headers(msg)
        else:
            meth(self)
        self._fp.write(sfp.getvalue())

    def _dispatch(self, msg):
        # Get the Content-Type: for the message, then try to dispatch to
        # self._handle_<maintype>_<subtype>().  If there's no handler for the
        # full MIME type, then dispatch to self._handle_<maintype>().  If
        # that's missing too, then dispatch to self._writeBody().
        main = msg.get_content_maintype()
        sub = msg.get_content_subtype()
        specific = UNDERSCORE.join((main, sub)).replace('-', '_')
        meth = getattr(self, '_handle_' + specific, None)
        if meth is None:
            generic = main.replace('-', '_')
            meth = getattr(self, '_handle_' + generic, None)
            if meth is None:
                meth = self._writeBody
        meth(msg)

    #
    # Default handlers
    #

    def _write_headers(self, msg):
        for h, v in msg.items():
            print >> self._fp, '%s:' % h,
            if self.__maxheaderlen == 0:
                # Explicit no-wrapping
                print >> self._fp, v
            elif isinstance(v, Header):
                # Header instances know what to do
                print >> self._fp, v.encode()
            elif _is8bitstring(v):
                # If we have raw 8bit data in a byte string, we have no idea
                # what the encoding is.  There is no safe way to split this
                # string.  If it's ascii-subset, then we could do a normal
                # ascii split, but if it's multibyte then we could break the
                # string.  There's no way to know so the least harm seems to
                # be to not split the string and risk it being too long.
                print >> self._fp, v
            else:
                # Header's got lots of smarts, so use it.
                print >> self._fp, Header(
                    v, maxlinelen=self.__maxheaderlen,
                    header_name=h, continuation_ws='\t').encode()
        # A blank line always separates headers from body
        print >> self._fp

    #
    # Handlers for writing types and subtypes
    #

    def _handle_text(self, msg):
        payload = msg.get_payload()
        if payload is None:
            return
        cset = msg.get_charset()
        if cset is not None:
            payload = cset.body_encode(payload)
        if not _isstring(payload):
            raise TypeError, 'string payload expected: %s' % type(payload)
        if self._mangle_from_:
            payload = fcre.sub('>From ', payload)
        self._fp.write(payload)

    # Default body handler
    _writeBody = _handle_text

    def _handle_multipart(self, msg):
        # The trick here is to write out each part separately, merge them all
        # together, and then make sure that the boundary we've chosen isn't
        # present in the payload.
        msgtexts = []
        subparts = msg.get_payload()
        if subparts is None:
            # Nothing has ever been attached
            boundary = msg.get_boundary(failobj=_make_boundary())
            print >> self._fp, '--' + boundary
            print >> self._fp, '\n'
            print >> self._fp, '--' + boundary + '--'
            return
        elif _isstring(subparts):
            # e.g. a non-strict parse of a message with no starting boundary.
            self._fp.write(subparts)
            return
        elif not isinstance(subparts, ListType):
            # Scalar payload
            subparts = [subparts]
        for part in subparts:
            s = StringIO()
            g = self.clone(s)
            g.flatten(part, unixfrom=False)
            msgtexts.append(s.getvalue())
        # Now make sure the boundary we've selected doesn't appear in any of
        # the message texts.
        alltext = NL.join(msgtexts)
        # BAW: What about boundaries that are wrapped in double-quotes?
        boundary = msg.get_boundary(failobj=_make_boundary(alltext))
        # If we had to calculate a new boundary because the body text
        # contained that string, set the new boundary.  We don't do it
        # unconditionally because, while set_boundary() preserves order, it
        # doesn't preserve newlines/continuations in headers.  This is no big
        # deal in practice, but turns out to be inconvenient for the unittest
        # suite.
        if msg.get_boundary() <> boundary:
            msg.set_boundary(boundary)
        # Write out any preamble
        if msg.preamble is not None:
            self._fp.write(msg.preamble)
            # If preamble is the empty string, the length of the split will be
            # 1, but the last element will be the empty string.  If it's
            # anything else but does not end in a line separator, the length
            # will be > 1 and not end in an empty string.  We need to
            # guarantee a newline after the preamble, but don't add too many.
            plines = NLCRE.split(msg.preamble)
            if plines <> [''] and plines[-1] <> '':
                self._fp.write('\n')
        # First boundary is a bit different; it doesn't have a leading extra
        # newline.
        print >> self._fp, '--' + boundary
        # Join and write the individual parts
        joiner = '\n--' + boundary + '\n'
        self._fp.write(joiner.join(msgtexts))
        print >> self._fp, '\n--' + boundary + '--',
        # Write out any epilogue
        if msg.epilogue is not None:
            if not msg.epilogue.startswith('\n'):
                print >> self._fp
            self._fp.write(msg.epilogue)

    def _handle_message_delivery_status(self, msg):
        # We can't just write the headers directly to self's file object
        # because this will leave an extra newline between the last header
        # block and the boundary.  Sigh.
        blocks = []
        for part in msg.get_payload():
            s = StringIO()
            g = self.clone(s)
            g.flatten(part, unixfrom=False)
            text = s.getvalue()
            lines = text.split('\n')
            # Strip off the unnecessary trailing empty line
            if lines and lines[-1] == '':
                blocks.append(NL.join(lines[:-1]))
            else:
                blocks.append(text)
        # Now join all the blocks with an empty line.  This has the lovely
        # effect of separating each block with an empty line, but not adding
        # an extra one after the last one.
        self._fp.write(NL.join(blocks))

    def _handle_message(self, msg):
        s = StringIO()
        g = self.clone(s)
        # The payload of a message/rfc822 part should be a multipart sequence
        # of length 1.  The zeroth element of the list should be the Message
        # object for the subpart.  Extract that object, stringify it, and
        # write it out.
        g.flatten(msg.get_payload(0), unixfrom=False)
        self._fp.write(s.getvalue())



class DecodedGenerator(Generator):
    """Generator a text representation of a message.

    Like the Generator base class, except that non-text parts are substituted
    with a format string representing the part.
    """
    def __init__(self, outfp, mangle_from_=True, maxheaderlen=78, fmt=None):
        """Like Generator.__init__() except that an additional optional
        argument is allowed.

        Walks through all subparts of a message.  If the subpart is of main
        type `text', then it prints the decoded payload of the subpart.

        Otherwise, fmt is a format string that is used instead of the message
        payload.  fmt is expanded with the following keywords (in
        %(keyword)s format):

        type       : Full MIME type of the non-text part
        maintype   : Main MIME type of the non-text part
        subtype    : Sub-MIME type of the non-text part
        filename   : Filename of the non-text part
        description: Description associated with the non-text part
        encoding   : Content transfer encoding of the non-text part

        The default value for fmt is None, meaning

        [Non-text (%(type)s) part of message omitted, filename %(filename)s]
        """
        Generator.__init__(self, outfp, mangle_from_, maxheaderlen)
        if fmt is None:
            fmt = ('[Non-text (%(type)s) part of message omitted, '
                   'filename %(filename)s]')
        self._fmt = fmt

    def _dispatch(self, msg):
        for part in msg.walk():
            maintype = part.get_main_type('text')
            if maintype == 'text':
                print >> self, part.get_payload(decode=True)
            elif maintype == 'multipart':
                # Just skip this
                pass
            else:
                print >> self, self._fmt % {
                    'type'       : part.get_type('[no MIME type]'),
                    'maintype'   : part.get_main_type('[no main MIME type]'),
                    'subtype'    : part.get_subtype('[no sub-MIME type]'),
                    'filename'   : part.get_filename('[no filename]'),
                    'description': part.get('Content-Description',
                                            '[no description]'),
                    'encoding'   : part.get('Content-Transfer-Encoding',
                                            '[no encoding]'),
                    }



# Helper
_width = len(repr(sys.maxint-1))
_fmt = '%%0%dd' % _width

def _make_boundary(text=None):
    # Craft a random boundary.  If text is given, ensure that the chosen
    # boundary doesn't appear in the text.
    token = random.randint(0, sys.maxint-1)
    boundary = ('=' * 15) + (_fmt % token) + '=='
    if text is None:
        return boundary
    b = boundary
    counter = 0
    while True:
        cre = re.compile('^--' + re.escape(b) + '(--)?$', re.MULTILINE)
        if not cre.search(text):
            break
        b = boundary + '.' + str(counter)
        counter += 1
    return b
