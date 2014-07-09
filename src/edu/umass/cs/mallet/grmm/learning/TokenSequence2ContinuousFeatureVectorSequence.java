/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */






package edu.umass.cs.mallet.grmm.learning;

import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.AugmentableFeatureVector;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.util.PropertyList;
import java.io.*;
/**
 * Convert the token sequence in the data field of each instance to a feature vector sequence.
 @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
*/

public class TokenSequence2ContinuousFeatureVectorSequence extends Pipe implements Serializable
{
    boolean augmentable;									// Create AugmentableFeatureVector's in the sequence
    boolean binary;												// Create binary (Augmentable)FeatureVector's in the sequence
    boolean growAlphabet = true;
	
    public TokenSequence2ContinuousFeatureVectorSequence (Alphabet dataDict,
						boolean binary, boolean augmentable)
    {
	super (dataDict, null);
	this.augmentable = augmentable;
	this.binary = binary;
    }

    public TokenSequence2ContinuousFeatureVectorSequence (Alphabet dataDict)
    {
	this (dataDict, false, false);
    }
	
    public TokenSequence2ContinuousFeatureVectorSequence (boolean binary, boolean augmentable)
    {
	super (Alphabet.class, null);
	this.augmentable = augmentable;
	this.binary = binary;
    }

    public TokenSequence2ContinuousFeatureVectorSequence ()
    {
	this (false, false);
    }
	
    public Instance pipe (Instance carrier)
    {
	TokenSequence tokens = (TokenSequence)carrier.getData();
	FeatureVector[] sequence = new FeatureVector[tokens.size()];
	PropertyList pl;
	PropertyList.Iterator iter;
	Alphabet alpha;
	String fakeEntry="noDictionnary";
	for (int i = 0; i < tokens.size(); i++){
	    pl =  tokens.getToken(i).getFeatures();
	    iter  = pl.numericIterator();
	    double[] values =  new double[pl.size()];
	    int[] indices = new int[values.length];
	    for(int j=0;j<values.length;j++){
		iter.nextProperty();
		values[j] = Double.parseDouble(iter.getKey());
		indices[j]=j;
	    }
	    alpha = (Alphabet)getDataAlphabet();
	    alpha.lookupIndex(fakeEntry);// An antry is added so that the size of the feature's alphabet is one. I do not use alphabet with continuous feature but the size corresponds to the number of parameters.
	    sequence[i] = new AugmentableFeatureVector (alpha, null,values,values.length,values.length,false,false,false);
	}
	carrier.setData(new FeatureVectorSequence (sequence));
	return carrier;
    }
    //trying to use continuous feature with one weight for all the features values, so the size of the alphabet is limited to 1
    public void setGrowAlphabet(boolean growAlphabet) {
	this.growAlphabet = growAlphabet;
    }

    // Serialization
	
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;
	
    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeInt (CURRENT_SERIAL_VERSION);
	out.writeBoolean(augmentable);
	out.writeBoolean(binary);
	out.writeBoolean(growAlphabet);
    }
	
    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
	int version = in.readInt ();
	augmentable = in.readBoolean();
	binary = in.readBoolean();
	//		growAlphabet = true;
	growAlphabet = in.readBoolean();
    }

}