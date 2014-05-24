/* Copyright (C) 2002 Dept. of Computer Science, Univ. of Massachusetts, Amherst

   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet

   This program toolkit free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  For more
   details see the GNU General Public License and the file README-LEGAL.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA. */


/**
	 @author Ben Wellner
 */

package edu.umass.cs.mallet.projects.seg_plus_coref.anaphora;

import java.util.*;
import java.util.regex.*;

import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.pipe.*;
import edu.umass.cs.mallet.base.pipe.iterator.*;
import edu.umass.cs.mallet.base.util.PropertyList;

public class AcronymOf extends Pipe
{

	public AcronymOf ()
	{

	}

	public String getAcronym (String s)
	{
		StringBuffer buf = new StringBuffer();
		char sArray[] = s.toCharArray();

		for (int i=0; i<s.length(); i++) {
			if (Character.isUpperCase(sArray[i]))
				buf.append(Character.toString(sArray[i]));
		}
		return buf.toString();
	}

	public boolean isAcronymOf (String s1, String s2)
	{
		String acro = getAcronym(s1);
		return s2.equals(acro);
	}

	public Instance pipe (Instance carrier)
	{
		MentionPair pair = (MentionPair)carrier.getData();
		Mention ant = pair.getAntecedent();
		Mention ref = pair.getReferent();

		if (ant != null) {
			if ((isAcronymOf (ant.getString(), ref.getString()))
					|| (isAcronymOf (ref.getString(), ant.getString()))) {
			//System.out.println("Acronym between: " + ant.getString() + " and " + ref.getString());
				pair.setFeatureValue("isAcronymOf", 1);
			}
		}
		return carrier;
	}
				
}
