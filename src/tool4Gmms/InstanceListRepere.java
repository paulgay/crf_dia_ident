/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */



package tool4Gmms;

import java.util.*;

import edu.umass.cs.mallet.base.types.Labeling;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.PipeOutputAccumulator;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureSequence;
import edu.umass.cs.mallet.base.pipe.FeatureSequence2FeatureVector;
import edu.umass.cs.mallet.base.pipe.Target2Label;
import edu.umass.cs.mallet.base.pipe.iterator.PipeInputIterator;
import edu.umass.cs.mallet.base.pipe.iterator.RandomTokenSequenceIterator;
import edu.umass.cs.mallet.base.util.MalletLogger;
import edu.umass.cs.mallet.base.util.PropertyList;
import edu.umass.cs.mallet.base.util.Random;
import edu.umass.cs.mallet.base.util.DoubleList;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;

import java.util.logging.*;
import java.io.*;
public class InstanceListRepere extends InstanceList
{
    Pipe pipe;
    ArrayList instances;
	private HashSet<String> unaryFeatures,pairWiseFeatures;
    public InstanceListRepere (Pipe pipe)
    {
	this.pipe=pipe;
	this.instances = new ArrayList (10);
	unaryFeatures=new HashSet<String>();
	pairWiseFeatures=new HashSet<String>();
    }
    public void add (PipeInputIterator pi)
    {
		while (pi.hasNext()) {
		    InstanceRepere carrier = ((InstanceFactory)pi).nextInstance();
		    carrier.setPipe(this.pipe);
		    if(carrier.useUnary())
		    	unaryFeatures.addAll(carrier.getUnaryFeaturesSet());
		    if(carrier.usePairWise())
		    	pairWiseFeatures.addAll(carrier.getPairWiseFeaturesSet());
		    add (carrier);
		}
    }
	public HashSet<String> getUnaryFeatures(){ return unaryFeatures;}
	public HashSet<String> getPairWiseFeatures() { return pairWiseFeatures;	}
}