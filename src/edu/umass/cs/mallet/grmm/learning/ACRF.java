/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.umass.cs.mallet.grmm.learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import edu.umass.cs.mallet.base.maximize.Maximizable;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.util.MalletLogger;
import edu.umass.cs.mallet.base.types.*;

import java.io.*;

import edu.umass.cs.mallet.grmm.inference.*;
import edu.umass.cs.mallet.grmm.learning.templates.PairWiseTemplate;
import edu.umass.cs.mallet.grmm.learning.templates.UnaryTemplate;
import edu.umass.cs.mallet.grmm.types.*;

import java.util.*;

import gnu.trove.*;
import tool4Gmms.InstanceListRepere;
import tool4Gmms.InstanceRepere; 


/**
 * Class for Arbitrary CRFs.  These are CRFs with completely
 *  arbitrary graphical structure.  The user passes in a list
 *  of instances of ACRF.CliqueFactory, which get to look at
 *  the sequence and decide what
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: ACRF.java,v 1.8 2006/05/25 03:24:57 casutton Exp $
 */
public class ACRF implements Serializable {

    transient private static Logger logger = MalletLogger.getLogger (ACRF.class.getName());

    Template[] templates;
    List fixedPtls = new ArrayList (0);
    private GraphPostProcessor graphProcessor;

    Alphabet inputAlphabet;

    private Inferencer globalInferencer = new TRP();
    private Inferencer viterbi = TRP.createForMaxProduct ();

    int defaultFeatureIndex;
    int iterNumber;
    // If true, use only supported features.
    private boolean supportedOnly = true;

    private Pipe inputPipe;

    private boolean cacheUnrolledGraphs = false;
    transient private Map graphCache = new THashMap ();

    /**
     *  Create a ACRF for a 1-d sequence.  Needs an array
     *   of Templates.
     */
    public ACRF (Pipe inputPipe, Template[] tmpls)
	throws IllegalArgumentException
    {
	this.inputPipe = inputPipe;
	this.templates = tmpls;
	this.inputAlphabet = inputPipe.getDataAlphabet();
	this.defaultFeatureIndex = inputAlphabet.size ();
	for (int tidx = 0; tidx < templates.length; tidx++) templates [tidx].index = tidx;
    }

    //  Accessors

    public Alphabet getInputAlphabet () { return inputAlphabet; }
    public int getDefaultFeatureIndex () { return defaultFeatureIndex; }

    public Inferencer getInferencer () { return globalInferencer; }
    public void setInferencer (Inferencer inf) { globalInferencer = inf; }

    public Inferencer getViterbiInferencer () { return viterbi; }
    public void setViterbiInferencer (Inferencer inf) { viterbi = inf; }

    public boolean getSupportedOnly () { return supportedOnly; }
    public void setSupportedOnly (boolean b) {
	supportedOnly = b;
	for (int i = 0; i < templates.length; i++) {
	    templates[i].setSupportedOnly (b);
	}
    }

    public boolean isCacheUnrolledGraphs () { return cacheUnrolledGraphs; }
    public void setCacheUnrolledGraphs (boolean cacheUnrolledGraphs) { this.cacheUnrolledGraphs = cacheUnrolledGraphs; }

    public void setFixedPotentials (Template[] fixed) {
	this.fixedPtls = java.util.Arrays.asList (fixed);
	for (int tidx = 0; tidx < fixed.length; tidx++) fixed [tidx].index = -1;
    }

    public void addFixedPotentials (Template[] tmpls) {
	for (int i = 0; i < tmpls.length; i++) {
	    Template tmpl = tmpls[i];
	    tmpl.setTrainable (false);
	    fixedPtls.add (tmpl);
	    tmpl.index = -1;
	}
    }

    public Template[] getTemplates () { return templates; }

    public void setTemplates(Template[] temp){templates=temp;};
    public Pipe getInputPipe () { return inputPipe; }

    public Template[] getFixedTemplates ()
    {
	return (Template[]) fixedPtls.toArray (new Template [fixedPtls.size()]);
    }

    public void addFixedPotential (Template tmpl)
    {
	tmpl.setTrainable (false);
	fixedPtls.add (tmpl);
	tmpl.index = -1;
    }

    public void setGraphProcessor (GraphPostProcessor graphProcessor)
    {
	this.graphProcessor = graphProcessor;
    }

    /**
     * Interface for making global transformations to an unrolled graph after it has been generated.
     *   For example, directed models can be simulated by selectively normalizing potentials.
     */
    public static interface GraphPostProcessor extends Serializable {
	void process (UnrolledGraph graph, Instance inst);
    }

    /**
     *  A type of clique in the model.  Each type of clique is assumed
     *   to have the same number of possibl outcomes and the same set
     *   of weights
     */
    // TODO Make an interface, implement with LogLinearTemplate & FixedTemplate
    public abstract static class Template implements Serializable
    {

	/**
	 * Adds all instiated cliques for an instance.  This method is
	 *  called as a graph is being unrolled for an instance.
	 *
	 *  @param graph The graph that the cliques will be added to.
	 *  @param instance Instance to unroll grpah for.  Subclasses are free
	 *   to specify what types they expect in the Instance's slots.
	 */
	public abstract void addInstantiatedCliques (UnrolledGraph graph, Instance instance);

	/**
	 * Modifies a factor computed from this template.  This is useful for templates that
	 *  wish to implement special normalization, etc. The default implementation does nothing.
	 * <P>
	 * WARNING: If you implement this method, it is likely that you will change the derivative of
	 *  this factor with respect to weights[].  This means that you will not be able to use the
	 * default <tt>ACRFTrainer</tt> for this template.
	 *
	 * @param unrolledGraph The graph in which the factor sits
	 * @param clique The set of nodes which are the domain of the factor
	 * @param ptl The factor to modify
	 */
	protected void modifyPotential (UnrolledGraph unrolledGraph, UnrolledVarSet clique, AbstractTableFactor ptl) {};

	protected SparseVector[] weights;
	private BitSet assignmentsPresent;
	private boolean supportedOnly;

	protected boolean isSupportedOnly ()
	{
	    return supportedOnly;
	}

	void setSupportedOnly (boolean supportedOnly)
	{
	    this.supportedOnly = supportedOnly;
	}

	protected BitSet getAssignmentsPresent ()
	{
	    return assignmentsPresent;
	}

	/**
	 * Returns the weights for this clique template.  Each possible
	 *  assignment to the clique can in general have a different set of
	 *  weights ,so this method returns an array of SparseVectors w,
	 *  where w[i] are the weights for assignment i.
	 */
	public SparseVector[] getWeights () { return weights; }

	/**
	 * Initializes the weight vectors to the appropriate size for a
	 * set of training data.
	 * @return Number of weights created.
	 */
	public int initWeights (InstanceList training)
	{
	    if (supportedOnly) {
		return initSparseWeights (training);
	    } else {
		return initDenseWeights (training);
	    }
	}

	protected int initDenseWeights (InstanceList training)
	{
	    int numf = training.getDataAlphabet ().size ();
	    //int numf = DataAlphabetSizeFromInstance (training);
	    int total = 0;

	    // handle default weights
	    int size = cliqueSizeFromInstance (training);
	    total += allocateDefaultWeights (size);

	    // and regular weights
	    SparseVector[] newWeights = new SparseVector [size];
	    for (int i = 0; i < size; i++) {
		newWeights [i] = new SparseVector (new double[numf], false);
		if (weights != null)
		    newWeights [i].plusEqualsSparse (weights [i]);
		total += numf;
		logger.info ("ACRF template "+this+" weights number: "+i+", num features: "+numf);
	    }

	    logger.info ("ACRF template "+this+" total num weights = "+total);
	    weights = newWeights;

	    return total;
	}

	private int initSparseWeights (InstanceList training)
	{
	    int total = 0;
	    // Build this bitsets that tell us what weights occur in the data
	    int size = cliqueSizeFromInstance (training);
	    BitSet[] weightsPresent = new BitSet [size];
	    for (int i = 0; i < size; i++) {
		weightsPresent [i] = new BitSet ();
	    }
	    assignmentsPresent = new BitSet (size);

	    collectWeightsPresent (training, weightsPresent);
	    if (weights != null) {
		addInCurrentWeights (weightsPresent);
	    }

	    // We can allocate default Weights now
	    total += allocateDefaultWeights (size);

	    // Use those to allocate the SparseVectors
	    SparseVector[] newWeights = new SparseVector [size];
	    for (int i = 0; i < size; i++) {
		// Create a sparse vector, with the allowable indices
		// specified in advance.
		int numLocations = weightsPresent [i].cardinality ();
		int indices[] = new int [numLocations];
		for (int j = 0; j < numLocations; j++) {
		    indices[j] = weightsPresent [i].nextSetBit (j == 0 ? 0 : indices[j-1]+1);
		    //					System.out.println ("ACRF "+this+" ["+i+"] has index "+indices[j]);
		}
		newWeights [i] = new HashedSparseVector (indices, new double[numLocations],
							 numLocations, numLocations, false, false, false);
		if (weights != null)
		    newWeights [i].plusEqualsSparse (weights [i]);
		total += numLocations;
		if (numLocations != 0)
		    logger.info ("ACRF template "+this+" weights ["+i+"] num features "+numLocations);
	    }

	    logger.info ("ACRF template "+this+" total num weights = "+total);
	    this.weights = newWeights;
	    return total;
	}

	protected int allocateDefaultWeights (int size)
	{
	    SparseVector newdefaultWeights = new SparseVector (new double [size], false);
	    if (defaultWeights != null) newdefaultWeights.plusEqualsSparse (defaultWeights);
	    defaultWeights = newdefaultWeights;
	    return size;
	}

	protected int cliqueSizeFromInstance (InstanceList training)
	{
	    for (int i = 0; i < training.size(); i++) {
		Instance instance = training.getInstance (i);
		UnrolledGraph unrolled = new UnrolledGraph (instance, new Template[] { this }, null, false);
		for (Iterator it = unrolled.varSetIterator (); it.hasNext();) {
		    UnrolledVarSet clique = (UnrolledVarSet) it.next ();
		    if (clique.tmpl == this) {
			return clique.weight ();
		    }
		}
	    }

	    logger.warning ("***ACRF: Don't know size of "+this+". Never needed in training data.");
	    return 0;
	}

	private void addInCurrentWeights (BitSet[] weightsPresent)
	{
	    for (int assn = 0; assn < weights.length; assn++) {
		for (int j = 0; j < weights[assn].numLocations(); j++) {
		    weightsPresent[assn].set (weights[assn].indexAtLocation (j));
		}
	    }
	}

	private void collectWeightsPresent (InstanceList ilist, BitSet[] weightsPresent)
	{
	    for (int inum = 0; inum < ilist.size(); inum++) {
		Instance inst = ilist.getInstance (inum);
		UnrolledGraph unrolled = new UnrolledGraph (inst, new Template[] { this }, null, false);
		collectTransitionsPresentForGraph (unrolled);
		collectWeightsPresentForGraph (unrolled, weightsPresent);
	    }
	}

	private void collectTransitionsPresentForGraph (UnrolledGraph unrolled)
	{
	    for (Iterator it = unrolled.varSetIterator (); it.hasNext();) {
		UnrolledVarSet clique = (UnrolledVarSet) it.next ();
		if (clique.tmpl == this) {
		    int assnNo = clique.lookupAssignmentNumber ();
		    assignmentsPresent.set (assnNo);
		}
	    }
	}

	private void collectWeightsPresentForGraph (UnrolledGraph unrolled, BitSet[] weightsPresent)
	{
	    for (Iterator it = unrolled.varSetIterator (); it.hasNext();) {
		UnrolledVarSet clique = (UnrolledVarSet) it.next ();
		if (clique.tmpl == this) {
		    for (int i = 0; i < clique.fv.numLocations(); i++) {
			int index = clique.fv.indexAtLocation (i);
			int assn = clique.lookupAssignmentNumber ();
			weightsPresent [assn].set (index);
		    }
		}
	    }
	}


	public AbstractTableFactor computeFactor (UnrolledVarSet clique)
	{
	    Matrix phi = createFactorMatrix(clique);
	    SparseVector[] weights = getWeights();

	    //				System.out.println("UnrolledClique "+clique);
	    //				System.out.println("FV : "+clique.fv);

	    for (int loc = 0; loc < phi.numLocations(); loc++) {
		int idx = phi.indexAtLocation(loc);
		SparseVector w = weights[idx];
		//					System.out.println("Weights "+i+" : "+w);
		//					w.print();
		double dp = w.dotProduct(clique.fv);
		dp += getDefaultWeight(idx);
		phi.setValueAtLocation(loc, dp);
	    }

	    AbstractTableFactor ptl = new LogTableFactor(clique);
	    ptl.setValues(phi);
	    return ptl;
	}

	/**
	 * Creates an empty matrix for use in storing factor values when this template is unrolled.
	 *   By overriding this method, subclasses may enforce that factors generated be sparse.
	 * @param clique
	 * @return An empty Matrixn
	 */
	protected Matrix createFactorMatrix (UnrolledVarSet clique)
	{
	    int[] szs = clique.varDimensions ();
	    /*System.out.println("creating new phi")
	    for(int i=0;i<szs.length;i++)
	    	System.out.println("size"+szs[i]);*/
	    Matrixn phi = new Matrixn (szs);
	    return phi;
	}


	public int index;

	private SparseVector defaultWeights;

	public double getDefaultWeight (int i) { return defaultWeights.value (i); }
	public SparseVector getDefaultWeights () { return defaultWeights; }
	public void setDefaultWeights (SparseVector w) { defaultWeights = w; }
	public void setDefaultWeight (int i, double w) { defaultWeights.setValue (i, w); }

	private boolean trainable = true;
	public boolean isTrainable () { return trainable; }
	public void setTrainable (boolean tr) { trainable = tr; }

	// I hate serialization

	private static final long serialVersionUID = -727618747254644076L; //8830720632081401678L;

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException
	{
	    in.defaultReadObject ();
	    if (assignmentsPresent == null && isTrainable()) {
	    	
		assignmentsPresent = new BitSet (weights.length);
		assignmentsPresent.flip (0, assignmentsPresent.size ());
	    }
	}

    }

    /** Abstract class for Templates that expect a (FeatureVectorSequence, LabelsSequence) for their instances. */
    public abstract static class SequenceTemplate extends Template
    {

	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	/**
	 * Adds all instiated cliques for an instance.  This method is
	 *  called as a graph is being unrolled for an instance.
	 *
	 *  @param graph The graph that the cliques will be added to.
	 *  @param fvs The input features of the instance to unroll the
	 *    cliques for.
	 *  @param lblseq The label sequence of the instance being unrolled.
	 */
	protected abstract void addInstantiatedCliques (UnrolledGraph graph, FeatureVectorSequence fvs, LabelsSequence lblseq);

	public void addInstantiatedCliques (UnrolledGraph graph, Instance instance)
	{
	    FeatureVectorSequence fvs = (FeatureVectorSequence) instance.getData ();
	    LabelsSequence lblseq = (LabelsSequence) instance.getTarget ();
	    addInstantiatedCliques (graph, fvs, lblseq);
	}
	public abstract int getTemplateSize();
    }

    // Abstract class for potentials that have no weights, but that know
    //  how te construct a potential
    public abstract static class FixedFactorTemplate extends Template {

	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	public int initWeights (InstanceList training) { return 0; }
	public SparseVector[] getWeights () { return new SparseVector [0]; }
	public SparseVector getDefaultWeights () { return new SparseVector (); }


	public boolean isTrainable () { return false; }

	public void setTrainable (boolean tr)
	{
	    if (tr)
		throw new IllegalArgumentException ("This template is never trainable.");
	}

	public abstract AbstractTableFactor computeFactor (UnrolledVarSet clique);
    }

    /**
     * A clique in the unrolled graphical model (an instantiation of
     *  some Template).  Contains a pointer to its corresponding
     *  template and a FeatureVector.
     */
    public static class UnrolledVarSet extends HashVarSet
    {
	Template tmpl;    // Template that generated this clique
	FeatureVector fv; // Features for the clique
	private Variable[] vars;
	UnrolledGraph graph;
	private double[][] ff;

	public int[] varDimensions ()
	{
	    int[] dims = new int[size()];
	    for (int i = 0; i < size(); i++) {
		dims [i] = get(i).getNumOutcomes();
	    }
	    return dims;
	}

	public UnrolledVarSet (UnrolledGraph graph, Template tmpl, Variable[] vars, FeatureVector fv)
	{
	    super (vars);
	    this.graph = graph;
	    this.setVars(vars);
	    this.tmpl = tmpl;
	    this.fv = fv;
	}

	Assignment getAssignmentByNumber (int assn)
	{
	    int[] sizes = varDimensions();
	    int[] indices = new int [sizes.length];
	    Matrixn.singleToIndices (assn, indices, sizes);
	    return new Assignment (getVars(), indices);
	}

	public int lookupAssignmentNumber ()
	{
	    int[] sizes = varDimensions();
	    int[] indices = new int [sizes.length];

	    for (int i = 0; i < indices.length; i++) {
		Label label = (Label) graph.var2label.get (getVars() [i]);
		indices [i] = label.getIndex ();
	    }

	    return Matrixn.singleIndex (sizes, indices);
	}

	Assignment lookupAssignment ()
	{
	    int[] indices = new int [getVars().length];

	    for (int i = 0; i < indices.length; i++) {
		Label label = (Label) graph.var2label.get (getVars() [i]);
		indices [i] = label.getIndex ();
	    }
	    return new Assignment (getVars(), indices);
	}

	public int lookupNumberOfAssignment (Assignment assn)
	{
	    int[] sizes = varDimensions();
	    int[] indices = new int [sizes.length];

	    for (int i = 0; i < indices.length; i++) {
		indices[i] = assn.get (getVars()[i]);
	    }

	    return Matrixn.singleIndex (sizes, indices);
	}

	public Template getTemplate ()
	{
	    return tmpl;
	}

	public FeatureVector getFv () { return fv; }

	public Variable[] getVars() {
		return vars;
	}

	public void setVars(Variable[] vars) {
		this.vars = vars;
	}

	public double[][] getFf() {
		return ff;
	}

	public void setFf(double[][] ff2) {
		this.ff = ff2;
	}

    }

    public static class UnrolledGraph extends UndirectedModel
					      //TODO:	public static class UnrolledGraph extends FactorGraph
					      //	implements Compactible
    {

	/** Array of Variables containing all nodes in model. */
	List allVars = new ArrayList ();

	/** Array containing all instantiated cliques (UnrolledClique) in the model. */
	List cliques = new ArrayList ();

	/** Maps variables ==> which label they correspond to */
	THashMap var2label = new THashMap ();
	Variable[][] label2var;

	/** The number of Label objects in each Labels object */
	int numSlices;

	boolean isCached = false;

	Instance instance;
	LabelsSequence lblseq;
	FeatureVectorSequence fvs;

	LabelAlphabet[] outputAlphabets;

	ACRF acrf;

	List allTemplates;
	private boolean isFactorsAdded = false;

	public UnrolledGraph (Instance inst, Template[] templates, Template[] fixed) {
	    this (inst, templates, java.util.Arrays.asList (fixed));
	}

	UnrolledGraph (Instance inst, Template[] templates, List fixed) { this (inst, templates, fixed, true); }

	/**
	 *  Creates a graphical model for a given instance.
	 *   This is called unrolling a dynamic model.
	 */
	public UnrolledGraph (Instance inst, Template[] templates, List fixed, boolean setupPotentials)
	{
	    super (initialCapacity (inst));
	    instance = inst;
	    fvs = (FeatureVectorSequence) inst.getData ();
	    lblseq = (LabelsSequence) inst.getTarget ();
	    allTemplates = new ArrayList ();
	    if (fixed != null) {
		allTemplates.addAll (fixed);
	    }
	    allTemplates.addAll (java.util.Arrays.asList (templates));
	    setupLabel2var ();
	    setupGraph ();
	    initOutputAlphabets ();
	    if (setupPotentials) {
		computeCPFs ();
	    }
	}

	// Guesses how much cache the undirected model should have space for.
	private static int initialCapacity (Instance inst)
	{
	    LabelsSequence lbls = (LabelsSequence) inst.getTarget();
	    int T = lbls.size ();
	    int K = lbls.getLabels (0).size ();
	    return (K+1) * T; // add some slack
	}

	private void setupGraph () {
	    assert lblseq.size() == fvs.size();
	    for (Iterator it = allTemplates.iterator (); it.hasNext ();) {
		Template tmpl = (Template) it.next ();
//		System.out.println(tmpl);
//		System.out.println(cliques);
		tmpl.addInstantiatedCliques (this, instance);
//		System.out.println(cliques);
//		for(int i=0;i<cliques.size();i++){
//			System.out.println("taille de la clique "+i+" :"+((UnrolledVarSet)cliques.get(0)).size());
//		}
	    }


	} // setupGraph

	private void setupLabel2var () {
	    label2var = new Variable [lblseq.size()][];
	    for (int t = 0; t < lblseq.size(); t++) {
		Labels lbls = lblseq.getLabels (t);
		label2var [t] = new Variable [lbls.size()];
	    }
	}

	public Variable getVarForLabel (int t, int j)
	{
	    if (label2var [t][j] == null)
		createVarForLabel (t, j);
	    return label2var [t][j];
	}

	private void createVarForLabel (int t, int i)
	{
	    Labels lbls = lblseq.getLabels (t);
	    Label label = lbls.get (i);
	    Variable var = new Variable (label.getLabelAlphabet ());
	    var.setLabel ("VAR[factor="+i+"][tm="+t+"]");
	    label2var[t][i] = var;
	    var2label.put (var, label);
	    allVars.add (var);
	}

	public Variable lookupVarForLabel (int t, int j) { return label2var[t][j]; }

	public int getNumSlices ()
	{
	    return numSlices;
	}

	public int getNumTimeSteps ()
	{
	    return label2var.length;
	}

	private void initOutputAlphabets ()
	{
	    //  TODO: this probably assumes that all the Labels in the sequene
	    //  have the same dimension.  This could be generalized.
	    Labels lbls = lblseq.getLabels (0);
	    numSlices = lbls.size ();
	    outputAlphabets = new LabelAlphabet [numSlices];
	    for (int i = 0; i < numSlices; i++) {
		Label lbl = lbls.get (i);
		outputAlphabets[i] = lbl.getLabelAlphabet ();
	    }
	}

	public LabelAlphabet getOutputAlphabet (int j) { return outputAlphabets [j]; }

	public void addClique (UnrolledVarSet clique)
	{
	    cliques.add (clique);
	}

	private void computeCPFs () {
	    isFactorsAdded = true;
	    for (Iterator it = cliques.iterator(); it.hasNext();) {
		UnrolledVarSet clique = (UnrolledVarSet) it.next();
		AbstractTableFactor ptl = clique.tmpl.computeFactor (clique);
		addFactor (ptl);
		clique.tmpl.modifyPotential (this, clique, ptl);
	    }
	}

	private void recomputeFactors () {
	    clear ();
	    computeCPFs ();
	}
	// Accessors

	int getMaxTime () { return lblseq.size(); }
	int getNumFactors () { return outputAlphabets.length;  }

	/**
	 *  Returns an Assignment object that corresponds to the
	 *   LabelSequence for which this graph was unrolled.
	 */
	public Assignment getAssignment ()
	{
	    final Assignment assn = new Assignment ();

	    TObjectObjectProcedure proc = new TObjectObjectProcedure() {
		    public boolean execute (Object o1, Object o2) {
			Variable var = (Variable) o1;
			Label label = (Label) o2;
			int idx = (isObserved(var)) ? observedValue(var) : label.getIndex ();
			assn.setValue(var, idx);
			return true;
		    }
		};
	    var2label.forEachEntry (proc);

	    return assn;
	}


	// xxx These should be refactor to UndirectedModel, and automatically add EvidencePotentials

	TObjectIntHashMap observedVars = new TObjectIntHashMap ();

	private boolean isObserved (Variable var)
	{
	    return observedVars.contains (var);
	}

	public void setObserved (Variable var, int outcome)
	{
	    observedVars.put (var, outcome);
	}

	public int observedValue (Variable var)
	{
	    return observedVars.get (var);
	}

	public Iterator varSetIterator ()
	{
	    return cliques.iterator ();
	}

	public UnrolledVarSet getVarSet (int cnum)
	{
	    return (UnrolledVarSet) cliques.get (cnum);
	}

	public int getIndex (VarSet vs)
	{
	    return cliques.indexOf (vs);
	}

	public Variable get (int idx)
	{
	    if (isFactorsAdded) {
		return super.get (idx);
	    } else {
		return (Variable) allVars.get (idx);
	    }
	}

	public int getIndex (Variable var)
	{
	    if (isFactorsAdded) {
		return super.getIndex (var);
	    } else {
		return allVars.indexOf (var);
	    }
	}

	public double getLogNumAssignments ()
	{
	    double total = 0;
	    for (int i = 0; i < numVariables (); i++) {
		Variable var = get(i);
		total += Math.log (var.getNumOutcomes ());
	    }
	    return total;
	}
	public List getCliques(){
		return cliques;
	}
    }


    public Maximizable.ByGradient getMaximizable (InstanceList ilst) {
	return new MaximizableACRF (ilst);
    }


    public List getBestLabels (InstanceList lst)
    {
	List ret = new ArrayList (lst.size());
	for (int i = 0; i < lst.size(); i++) {
	    ret.add (getBestLabels (lst.getInstance (i)));
	}
	return ret;
    }

    transient private boolean warnOnNoMax = true;

    public LabelsSequence getBestLabels (Instance inst)
    {
	// Compute the MAP assignment
	UnrolledGraph unrolled  = unroll (inst);
	List cliques=unrolled.getCliques();
    for (Iterator it = cliques.iterator(); it.hasNext();) {
        VarSet c = (VarSet) it.next();
       	}

	// Vertices count could be zero if all nodes pruned out of model.
	//  In that case, don't bother calling Viterbi, and if statement below
	//  will ensure that the LabelsSequence returned is arbitrary.
	if (unrolled.numVariables () != 0) {
	    viterbi.computeMarginals (unrolled);
	}

	// And change it into a LabelsSequence

	int numFactors = unrolled.getNumFactors ();
	int maxTime = unrolled.getMaxTime ();
	Labels[] lbls = new Labels [maxTime];
	for (int t = 0; t < maxTime; t++) {
	    Label[] theseLabels = new Label [numFactors];
	    for (int i = 0; i < numFactors; i++) {
		Variable var = unrolled.lookupVarForLabel (t, i);
		int maxidx;
		if (var != null) {
		    Factor marg = viterbi.lookupMarginal (var);
		    maxidx = marg.argmax ();
		} else {
		    // Unrolled graph does not contain variable.  This happens when the templates for var assign no neighbors
		    //  and no potentials, so that var is pruned.
		    if (warnOnNoMax) {
			logger.warning ("Could not determine max label for instance "+inst+" time "+t+" factor "+i+" [...and probably others...]");
			warnOnNoMax = false;
		    }

		    maxidx = 0;
		}
		//				System.out.println("Max idx "+maxidx+" ("+var.getLabelAlphabet().lookupLabel (maxidx)+")\n"+marg+"\n\n");

		theseLabels [i] = unrolled.getOutputAlphabet(i).lookupLabel (maxidx);
	    }
	    lbls [t] = new Labels (theseLabels);
	}
	return new LabelsSequence (lbls);
    }

    public HashMap<Integer, String> getBestLabelsUniqEnforce (InstanceRepere inst)
    {
	// Compute the MAP assignment
	// Vertices count could be zero if all nodes pruned out of model.
	//  In that case, don't bother calling Viterbi, and if statement below
	//  will ensure that the LabelsSequence returned is arbitrary.
	UnrolledGraph unrolled=null;
	List cliques ;
    boolean converged=false;
	int idx=0;
	int cliqueSizeMax[]= {10, 5, 3,2,1}; 
    while(!converged  && idx<cliqueSizeMax.length){
		unrolled = unroll (inst);
		if (unrolled.numVariables () != 0) {
		    converged=((LoopyBP) viterbi).computeMarginalsAndSayIfconverged (unrolled);
			if(! converged)
				if(inst.removePairLinks(cliqueSizeMax[idx])){
					System.out.println("No convergence, I removed some uniq links , biggest clique:"+cliqueSizeMax[idx]);
					idx+=1;
				}
	    }
	}
    if(!converged){
		unrolled  = unroll (inst);
		((LoopyBP) viterbi).computeMarginalsAndSayIfconverged (unrolled);
    }
	cliques=unrolled.getCliques();
    for (Iterator it = cliques.iterator(); it.hasNext();) {
        VarSet c = (VarSet) it.next();
    }
	int numFactors = unrolled.getNumFactors ();
	int maxTime = unrolled.getMaxTime ();
	//Labels[] lbls = new Labels [maxTime];
	double table[][] = new double[maxTime][unrolled.getOutputAlphabet(0).size()];
	for (int t = 0; t < maxTime; t++) {
	    for (int i = 0; i < numFactors; i++) {
	    	Variable var = unrolled.lookupVarForLabel (t, i);
	    	if (var != null) {
	    		Factor marg = viterbi.lookupMarginal (var);
	    		for(int l=0;l<var.getLabelAlphabet().size();l++)
	    			table[t][l]=new Double( ((DiscreteFactor) marg).valueAtLocation(l));
		} else {
		    if (warnOnNoMax) {
			logger.warning ("Could not determine max label for instance "+inst+" time "+t+" factor "+i+" [...and probably others...]");
			warnOnNoMax = false;
		    }
		}
	    }
	}
	int maxi[] = null;
    maxi = MatrixOps.maxIndex(table);
    HashMap<Integer,String> seg2label = new HashMap<Integer,String>(); 
    while(table[maxi[0]][maxi[1]]>0){
    	/*System.out.println(table[0][0]+" "+table[0][1]+" "+table[0][2]);
    	System.out.println(table[1][0]+" "+table[1][1]+" "+table[1][2]);
    	System.out.println(table[2][0]+" "+table[2][1]+" "+table[2][2]);
    	System.out.println(table[3][0]+" "+table[3][1]+" "+table[3][2]);
    	System.out.println(table[4][0]+" "+table[4][1]+" "+table[4][2]);
    	System.out.println(table[5][0]+" "+table[5][1]+" "+table[5][2]);
    	System.out.println(maxi[0]+" "+maxi[1]);*/
    	if(maxi[0]==0){
    		int a=0;
    	}
    	for(int j=0;j<table[0].length;j++)
    		table[maxi[0]][j]=-1;
    	for(ArrayList<String> pair: inst.getUniqPairsOri()){
    		if(pair.get(0).equals(inst.getSegmentNames().get(maxi[0])))
    				table[inst.getSegmentNames().indexOf(pair.get(1))][maxi[1]]=-1;
    		if(pair.get(1).equals(inst.getSegmentNames().get(maxi[0])))
				table[inst.getSegmentNames().indexOf(pair.get(0))][maxi[1]]=-1;
    	}
    	seg2label.put(new Integer(maxi[0]), unrolled.getOutputAlphabet(0).lookupLabel(maxi[1]).toString());
        maxi = MatrixOps.maxIndex(table);
    }
	return seg2label;
    }
    
	public HashMap<String,HashMap<String, Double>> getMargin(InstanceRepere inst) {
		// Compute the MAP assignment
		HashMap<String,HashMap<String, Double>> margin = new HashMap<String,HashMap<String, Double>>(); 
		UnrolledGraph unrolled  = unroll (inst);

		// Vertices count could be zero if all nodes pruned out of model.
		//  In that case, don't bother calling Viterbi, and if statement below
		//  will ensure that the LabelsSequence returned is arbitrary.
		if (unrolled.numVariables () != 0) {
		    viterbi.computeMarginals(unrolled);
		}

		// And change it into a LabelsSequence

		int numFactors = unrolled.getNumFactors ();
		int maxTime = unrolled.getMaxTime ();
		//Labels[] lbls = new Labels [maxTime];
		for (int t = 0; t < maxTime; t++) {
		    //Label[] theseLabels = new Label [numFactors];
		    for (int i = 0; i < numFactors; i++) {
			Variable var = unrolled.lookupVarForLabel (t, i);
			int maxidx;
			if (var != null) {
				HashMap<String,Double> margs=new HashMap<String,Double>(); 
			    Factor marg = viterbi.lookupMarginal (var);
			    for(int l=0;l<var.getLabelAlphabet().size();l++)
			    	margs.put(unrolled.getOutputAlphabet(i).lookupLabel(l).toString(), new Double( ((DiscreteFactor) marg).valueAtLocation(l)));
			    
			    	//margs.put(var.getLabelAlphabet().lookupLabel(l).toString(), ((DiscreteFactor) marg).valueAtLocation(l));
			    margin.put(inst.getSegmentNames().get(t),margs);			    
			    maxidx = marg.argmax ();
			} else {
			    // Unrolled graph does not contain variable.  This happens when the templates for var assign no neighbors
			    //  and no potentials, so that var is pruned.
			    if (warnOnNoMax) {
				logger.warning ("Could not determine max label for instance "+inst+" time "+t+" factor "+i+" [...and probably others...]");
				warnOnNoMax = false;
			    }

			    maxidx = 0;
			}
			//				System.out.println("Max idx "+maxidx+" ("+var.getLabelAlphabet().lookupLabel (maxidx)+")\n"+marg+"\n\n");
			//theseLabels [i] = unrolled.getOutputAlphabet(i).lookupLabel (maxidx);
		    }
		    //lbls [t] = new Labels (theseLabels);
		}		
		return margin;
	}

	public HashMap<String,HashMap<String, Double>> getMarginAware(InstanceRepere inst) {
		// Compute the MAP assignment
		HashMap<String,HashMap<String, Double>> margin = new HashMap<String,HashMap<String, Double>>(); 
		// Vertices count could be zero if all nodes pruned out of model.
		//  In that case, don't bother calling Viterbi, and if statement below
		//  will ensure that the LabelsSequence returned is arbitrary.
		
		UnrolledGraph unrolled=null;
		List cliques ;
	    boolean converged=false;
		int idx=0;
		int cliqueSizeMax[]= {10, 5, 3,2,1}; 
	    while(!converged  && idx<cliqueSizeMax.length){
			unrolled = unroll (inst);
			if (unrolled.numVariables () != 0) {
			    converged=((LoopyBP) viterbi).computeMarginalsAndSayIfconverged (unrolled);
				if(! converged)
					if(inst.removePairLinks(cliqueSizeMax[idx])){
						System.out.println("No convergence, I removed some uniq links , biggest clique:"+cliqueSizeMax[idx]);
						idx+=1;
					}
		    }
		}
	    if(!converged){
			unrolled  = unroll (inst);
			((LoopyBP) viterbi).computeMarginalsAndSayIfconverged (unrolled);
	    }
		cliques=unrolled.getCliques();
	    
	    //viterbi.computeMarginalsAndSayIfconverged (unrolled);
		// And change it into a LabelsSequence

		int numFactors = unrolled.getNumFactors ();
		int maxTime = unrolled.getMaxTime ();
		//Labels[] lbls = new Labels [maxTime];
		for (int t = 0; t < maxTime; t++) {
		    //Label[] theseLabels = new Label [numFactors];
		    for (int i = 0; i < numFactors; i++) {
			Variable var = unrolled.lookupVarForLabel (t, i);
			int maxidx;
			if (var != null) {
				HashMap<String,Double> margs=new HashMap<String,Double>(); 
			    Factor marg = viterbi.lookupMarginal (var);
			    for(int l=0;l<var.getLabelAlphabet().size();l++)
			    	margs.put(unrolled.getOutputAlphabet(i).lookupLabel(l).toString(), new Double( ((DiscreteFactor) marg).valueAtLocation(l)));
			    
			    	//margs.put(var.getLabelAlphabet().lookupLabel(l).toString(), ((DiscreteFactor) marg).valueAtLocation(l));
			    margin.put(inst.getSegmentNames().get(t),margs);			    
			    maxidx = marg.argmax ();
			} else {
			    // Unrolled graph does not contain variable.  This happens when the templates for var assign no neighbors
			    //  and no potentials, so that var is pruned.
			    if (warnOnNoMax) {
				logger.warning ("Could not determine max label for instance "+inst+" time "+t+" factor "+i+" [...and probably others...]");
				warnOnNoMax = false;
			    }

			    maxidx = 0;
			}
			//				System.out.println("Max idx "+maxidx+" ("+var.getLabelAlphabet().lookupLabel (maxidx)+")\n"+marg+"\n\n");
			//theseLabels [i] = unrolled.getOutputAlphabet(i).lookupLabel (maxidx);
		    }
		    //lbls [t] = new Labels (theseLabels);
		}		
		return margin;
	}
    
    
    public UnrolledGraph unroll (Instance inst)
    {
	UnrolledGraph g;
	if (cacheUnrolledGraphs && graphCache.containsKey (inst)) {
	    g = (UnrolledGraph) graphCache.get (inst);
	    g.recomputeFactors ();
	} else {
	    g = new UnrolledGraph (inst, templates, fixedPtls);
	    if (graphProcessor != null)
		graphProcessor.process (g, inst);
	}

	if (cacheUnrolledGraphs) graphCache.put (inst, g);

	return g;
    }

    public UnrolledGraph unrollStructureOnly (Instance inst)
    {
	UnrolledGraph g;
	if (cacheUnrolledGraphs && graphCache.containsKey (inst)) {
	    g = (UnrolledGraph) graphCache.get (inst);
	    g.recomputeFactors ();
	} else {
	    g = new UnrolledGraph (inst, templates, fixedPtls, false);
	    if (graphProcessor != null)
		graphProcessor.process (g, inst);
	}

	if (cacheUnrolledGraphs) graphCache.put (inst, g);

	return g;
    }

    private void reportOnGraphCache ()
    {
	logger.info ("Number of cached graphs = "+graphCache.size ());
    }


    public class MaximizableACRF implements Maximizable.ByGradient, Serializable {

	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	InstanceList trainData;
	double cachedValue = -123456789;
	double[] cachedGradient;
	protected BitSet infiniteValues = null;
	boolean cachedValueStale, cachedGradientStale;
	private	int numParameters;

	private static final double DEFAULT_GAUSSIAN_PRIOR_VARIANCE = 10;
	private int totalNodes = 0;

	public double getGaussianPriorVariance ()
	{
	    return gaussianPriorVariance;
	}

	public void setGaussianPriorVariance (double gaussianPriorVariance)
	{
	    this.gaussianPriorVariance = gaussianPriorVariance;
	}

	private double gaussianPriorVariance = DEFAULT_GAUSSIAN_PRIOR_VARIANCE;

	/** An unrolled version of the ACRF. */
	transient private UnrolledGraph graph;

	protected Inferencer inferencer = globalInferencer.duplicate();
		
	/* Vectors that contain the counts of features observed in the
	   training data. Maps
	   (clique-template x feature-number) => count
	*/
	SparseVector constraints[][];

	/* Vectors that contain the expected value over the
	 *  labels of all the features, have seen the training data
	 *  (but not the training labels).
	 */
	SparseVector expectations[][];

	SparseVector defaultConstraints[];
	SparseVector defaultExpectations[];

	private void initWeights (InstanceList training)
	{
	    for (int tidx = 0; tidx < templates.length; tidx++) {
		numParameters += templates[tidx].initWeights (training);
	    }
	}

	/* Initialize constraints[][] and expectations[][]
	 *  to have the same dimensions as weights, but to
	 *  be all zero.
	 */
	private void initConstraintsExpectations ()
	{
	    // Do the defaults first
	    defaultConstraints = new SparseVector [templates.length];
	    defaultExpectations = new SparseVector [templates.length];
	    for (int tidx = 0; tidx < templates.length; tidx++) {
		//cliqueSizeFromInstance (training)
	    	
		//int size =((ContinuousTemplate)templates[tidx]).getTemplateSize();
		//SparseVector defaults = new SparseVector (new double [size], false);
		SparseVector defaults =  templates[tidx].getDefaultWeights();
		defaultConstraints[tidx] = (SparseVector) defaults.cloneMatrixZeroed ();
		defaultExpectations[tidx] = (SparseVector) defaults.cloneMatrixZeroed ();
	    }
			
	    // And now the others
	    constraints = new SparseVector [templates.length][];
	    expectations = new SparseVector [templates.length][];
	    for (int tidx = 0; tidx < templates.length; tidx++) {
			Template tmpl = templates [tidx];
			SparseVector[] weights = tmpl.getWeights();
			//int size =((ContinuousTemplate)templates[tidx]).getTemplateSize();//set to 1 in a contiuous template
			//constraints [tidx] = new SparseVector [size];
			//expectations [tidx] = new SparseVector [size];
			constraints [tidx] = new SparseVector [weights.length];
			expectations [tidx] = new SparseVector [weights.length];
	
			for (int i = 0; i < weights.length ; i++) {
			    constraints[tidx][i] = (SparseVector) weights[0].cloneMatrixZeroed ();
			    expectations[tidx][i] = (SparseVector) weights[0].cloneMatrixZeroed ();
			}
	    }
	}

	/**
	 * Set all expectations to 0 after they've been
	 *    initialized.
	 */
	void resetExpectations ()
	{
	    for (int tidx = 0; tidx < expectations.length; tidx++) {
		defaultExpectations [tidx].setAll (0.0);
		for (int i = 0; i < expectations[tidx].length; i++) {
		    expectations[tidx][i].setAll (0.0);
		}
	    }
	}

	protected MaximizableACRF (InstanceList ilist)
	{
	    logger.finest ("Initializing MaximizableACRF.");

	    /* allocate for weights, constraints and expectations */
	    this.trainData = ilist;
	    initWeights(trainData);
	    initConstraintsExpectations();

	    int numInstances = trainData.size();
	    cachedGradient = new double[numParameters];

	    cachedValueStale = cachedGradientStale = true;

	    /*
	      if (cacheUnrolledGraphs) {
	      unrolledGraphs = new UnrolledGraph [numInstances];
	      }
	    */

	    logger.info("Number of training instances = " + numInstances );
	    logger.info("Number of parameters = " + numParameters );
	    logger.info("Default feature index = " + defaultFeatureIndex );
	    describePrior();

	    logger.fine("Computing constraints");
	    collectConstraints (trainData);//1401 instancier les cliques
	    //System.out.println("les contraintes: ");
	    //for(int j=0;j<expectations.length;j++)
	    //	System.out.println("template index Number: "+j+" constraint value: "+constraints[j][0]+" parameter value: "+templates [j].getWeights()[0]);

	}

	private void describePrior ()
	{
	    logger.info ("Using gaussian prior with variance "+gaussianPriorVariance);
	}
	public int getNumParameters() { return numParameters; }


	/* Negate initialValue and finalValue because the parameters are in
	 * terms of "weights", not "values".
	 */
	public void getParameters (double[] buf) {

	    if ( buf.length != numParameters )
		throw new IllegalArgumentException("Argument is not of the " +
						   " correct dimensions");
	    int idx = 0;
	    for (int tidx = 0; tidx < templates.length; tidx++) {
		Template tmpl = templates [tidx];
		SparseVector defaults = tmpl.getDefaultWeights ();
		double[] values = defaults.getValues();
		System.arraycopy (values, 0, buf, idx, values.length);
		idx += values.length;
	    }
			
	    for (int tidx = 0; tidx < templates.length; tidx++) {
		Template tmpl = templates [tidx];
		SparseVector[] weights = tmpl.getWeights();
		for (int assn = 0; assn < weights.length; assn++) {
		    double[] values = weights [assn].getValues ();
		    System.arraycopy (values, 0, buf, idx, values.length);
		    idx += values.length;
		}
	    }

	}


	public void setParameters (double[] params)
	{
	    if ( params.length != numParameters )
		throw new IllegalArgumentException("Argument is not of the " +
						   " correct dimensions");

	    cachedValueStale = cachedGradientStale = true;

	    int idx = 0;
	    for (int tidx = 0; tidx < templates.length; tidx++) {
		Template tmpl = templates [tidx];
		SparseVector defaults = tmpl.getDefaultWeights();
		double[] values = defaults.getValues ();
		System.arraycopy (params, idx, values, 0, values.length);
		idx += values.length;
	    }

	    for (int tidx = 0; tidx < templates.length; tidx++) {
		Template tmpl = templates [tidx];
		SparseVector[] weights = tmpl.getWeights();
		for (int assn = 0; assn < weights.length; assn++) {
		    double[] values = weights [assn].getValues ();
		    System.arraycopy (params, idx, values, 0, values.length);
		    idx += values.length;
		}
	    }
	}

	// Functions for unit tests to get constraints and expectations
	//  I'm too lazy to make a deep copy.  Callers should not
	//  modify these.

	public SparseVector[] getExpectations (int cnum) { return expectations [cnum]; }
	public SparseVector[] getConstraints (int cnum) { return constraints [cnum]; }

	/** print weights */
	public void printParameters()
	{
	    double[] buf = new double[numParameters];
	    getParameters(buf);

	    int len = buf.length;
	    for (int w = 0; w < len; w++)
		System.out.print(buf[w] + "\t");
	    System.out.println();
	}


	public double getParameter (int index) { return(0.0); }
	public void setParameter (int index, double value) {}


	/** Returns the log probability of the training sequence labels */
	public double getValue ()
	{
	    if (cachedValueStale)
		{
		    cachedValue = computeLogLikelihood ();
		    cachedValueStale = false;
		    cachedGradientStale = true;

		    /*
		      if(saveNum++ % savePeriod == 0)  {
		      System.out.println ("saving ACRF ...");
		      ACRF.this.writeWeights(weightFile);
		      System.out.println ("Done ....");
		      }
		    */

		    logger.info ("getValue() (loglikelihood) = " + cachedValue);
		}

	    if(Double.isNaN(cachedValue))
		{
		    logger.warning("value is NaN");
		    cachedValue = 0;
		}

	    return cachedValue;
	}


	protected double computeLogLikelihood () {
	    double retval = 0.0;
	    int numInstances = trainData.size();

	    long start = System.currentTimeMillis();
	    long unrollTime = 0;

	    /* Instance values must either always or never be included in
     * the total values; we can't just sometimes skip a value
	     * because it is infinite, that throws off the total values.
	     * We only allow an instance to have infinite value if it happens
	     * from the start (we don't compute the value for the instance
	     * after the first round. If any other instance has infinite
	     * value after that it is an error. */

	    boolean initializingInfiniteValues = false;

	    if (infiniteValues == null) {
		/* We could initialize bitset with one slot for every
		 * instance, but it is *probably* cheaper not to, taking the
		 * time hit to allocate the space if a bit becomes
		 * necessary. */
		infiniteValues = new BitSet ();
		initializingInfiniteValues = true;
	    }

	    /* Clear the sufficient statistics that we are about to fill */
	    resetExpectations();

	    /* Fill in expectations for each instance */
	    for (int i = 0; i < numInstances; i++)
		{
		    Instance instance = trainData.getInstance(i);

		    /* Compute marginals for each clique */
		    long unrollStart = System.currentTimeMillis ();
		    UnrolledGraph unrolled = unroll (instance);
		    long unrollEnd = System.currentTimeMillis ();
		    unrollTime += (unrollEnd - unrollStart);
        
		    if (unrolled.numVariables () == 0) continue;   // Happens if all nodes are pruned.
		    inferencer.computeMarginals (unrolled);
		    //				unrolled.dump();

		    /* Save the expected value of each feature for when we
		       compute the gradient. */
		    //System.out.println(instance.getName());
		    //System.out.println("Old expectations: ");
		    //for(int j=0;j<expectations.length;j++)
		    //	System.out.println("template index Number: "+j+" expectation value: "+expectations[j][0]+" parameter value: "+templates [j].getWeights()[0]);
		    collectExpectations (unrolled, inferencer);
		    //System.out.println("new expectations: ");
		    //for(int j=0;j<expectations.length;j++)
		    //	System.out.println("template index Number: "+j+" expectation value: "+expectations[j][0]+" parameter value: "+templates [j].getWeights()[0]);

		    /* Add in the joint prob of the labeling. */
		    Assignment jointAssn = unrolled.getAssignment ();
		    double value = inferencer.lookupLogJoint (jointAssn);
		    
		    if (Double.isInfinite(value))
			{
			    if (initializingInfiniteValues) {
				logger.warning ("Instance " + instance.getName() +
						" has infinite value; skipping.");
				infiniteValues.set (i);
				continue;
			    } else if (!infiniteValues.get(i)) {
				logger.warning ("Infinite value on instance "+instance.getName()+
						"returning -infinity");
				return Double.NEGATIVE_INFINITY;
				/*
				  printDebugInfo (unrolled);
				  throw new IllegalStateException
				  ("Instance " + instance.getName()+ " used to have non-infinite"
				  + " value, but now it has infinite value.");
				*/
			    }
			} else if (Double.isNaN (value)) {
			System.out.println("NaN on instance "+i+" : "+instance.getName ());
			printDebugInfo (unrolled);
			/*					throw new IllegalStateException
								("Value is NaN in ACRF.getValue() Instance "+i);
			*/
			logger.warning ("Value is NaN in ACRF.getValue() Instance "+i+" : "+
					"returning -infinity... ");
			return Double.NEGATIVE_INFINITY;
		    } else {
			retval += value;
		    }

		}

	    /* Incorporate Gaussian prior on parameters. This means
	       that for each weight, we will add w^2 / (2 * variance) to the
	       log probability. */

	    double priorDenom = 2 * gaussianPriorVariance;

	    for (int tidx = 0; tidx < templates.length; tidx++) {
		SparseVector[] weights = templates [tidx].getWeights ();
		for (int j = 0; j < weights.length; j++) {
		    for (int fnum = 0; fnum < weights[j].numLocations(); fnum++) {
			double w = weights [j].valueAtLocation (fnum);
			if (weightValid (w, tidx, j)) {
			    retval += -w*w/priorDenom;
			}
		    }
		}
	    }

	    if (cacheUnrolledGraphs) reportOnGraphCache ();

	    long end = System.currentTimeMillis ();
	    logger.info ("ACRF Inference time (ms) = "+(end-start));
	    logger.info ("ACRF unroll time (ms) = "+unrollTime);
	    logger.info ("getValue (loglikelihood) = "+retval);

	    return retval;
	}


	/**
	 *  Computes the graident of the penalized log likelihood
	 *   of the ACRF, and returns it in buf[].
	 */
	public void getValueGradient(double[] buf)
	{
	    if (cachedGradientStale)
		{
		    /* This will fill in the expectations */
		    if (cachedValueStale) getValue ();

		    /*
		      if ( checkForNaN() )
		      throw new IllegalStateException
		      ("NaN found in weight, constraint, or expectation");
		    */

		    computeGradient ();

		    // 				System.out.println("gradient:");
		    // 				MatrixOps.print (cachedGradient);
		    //				dumpDefaults();
		    // 				double[] params = new double [numParameters];
		    // 				getParameters (params);
		    //			MatrixOps.print (params);
		    //				dumpValues ("Constraints", constraints);
		    //				dumpValues ("Expectations", expectations);
		}

	    if (buf.length != numParameters)
		throw new IllegalArgumentException
		    ("Incorrect length buffer to getValueGradient(). Expected "
		     + numParameters + ", received " + buf.length);

	    System.arraycopy (cachedGradient, 0, buf, 0, cachedGradient.length);
	}


	/**
	 *  Computes the gradient of the penalized log likelihood of the
	 *   ACRF, and places it in cachedGradient[].
	 *
	 * Gradient is
	 *   constraint - expectation - parameters/gaussianPriorVariance
	 */
	private void computeGradient ()
	{
	    /* Index into current element of cachedGradient[] array. */
	    int gidx = 0;

	    // First do gradient wrt defaultWeights
	    for (int tidx = 0; tidx < templates.length; tidx++) {
		SparseVector theseWeights = templates[tidx].getDefaultWeights ();
		SparseVector theseConstraints = defaultConstraints [tidx];
		SparseVector theseExpectations = defaultExpectations [tidx];
		for (int j = 0; j < theseWeights.numLocations(); j++) {
		    double weight = theseWeights.valueAtLocation (j);
		    double constraint = theseConstraints.valueAtLocation (j);
		    double expectation = theseExpectations.valueAtLocation (j);
		    //					System.out.println(" gradient ["+gidx+"] = "+constraint+" (ctr) - "+expectation+" (exp) - "+
		    //													 (weight / gaussianPriorVariance)+" (reg) ");
		    cachedGradient [gidx++] = constraint - expectation - (weight / gaussianPriorVariance);
		}
	    }

	    // Now do other weights
	    for (int tidx = 0; tidx < templates.length; tidx++) {
		Template tmpl = templates [tidx];
		SparseVector[] weights = tmpl.getWeights ();
		for (int i = 0; i < weights.length; i++) {
		    SparseVector thisWeightVec = weights [i];
		    SparseVector thisConstraintVec = constraints [tidx][i];
		    SparseVector thisExpectationVec = expectations [tidx][i];

		    for (int j = 0; j < thisWeightVec.numLocations(); j++) {
			double w = thisWeightVec.valueAtLocation (j);
			double gradient;  // Computed below

			/* A parameter may be set to -infinity by an external user.
			 * We set gradient to 0 because the parameter's value can
			 * never change anyway and it will mess up future calculations
			 * on the matrix. */
			if (Double.isInfinite(w)) {
			    logger.warning("Infinite weight for node index " +i+
					   " feature " +
					   inputAlphabet.lookupObject(j) );
			    gradient = 0.0;
			} else {
			    gradient = thisConstraintVec.valueAtLocation(j)
				- (w/gaussianPriorVariance)
				- thisExpectationVec.valueAtLocation(j);
			}

			cachedGradient[gidx++] = gradient;
		    }
		}
	    }
	}

	/**
	 * For every feature f_k, computes the expected value of f_k
	 *  over all possible label sequences given the list of instances
	 *  we have.
	 *
	 *  These values are stored in collector, that is,
	 *    collector[i][j][k]  gets the expected value for the
	 *    feature for clique i, label assignment j, and input features k.
	 */
	private void collectExpectations (UnrolledGraph unrolled, Inferencer inferencer)
	{
	    double ffValue;
	    for (Iterator it = unrolled.varSetIterator (); it.hasNext();) {//for each clique in the graph
			UnrolledVarSet clique = (UnrolledVarSet) it.next();
			int tidx = clique.tmpl.index;
			if (tidx == -1) continue;

			//System.out.println(clique.tmpl);
			//System.out.println(clique.getFv());
			Factor ptl = inferencer.lookupMarginal (clique);
			int numAssignments = clique.weight ();
			// for each assigment to the clique
			//  Note that we get the AssignmentIterator from the factor (rather than the clique), because the
			//   factor objects knows about any potential sparsity.
			AssignmentIterator assnIt = ptl.assignmentIterator ();
			if(tidx==4){
				int a=0;
			}
			while (assnIt.hasNext ()) { 
			    double marginal = ptl.value (assnIt);
			    int idx = assnIt.indexOfCurrentAssn ();
			    //		    factorValue = unrolled.factorOf(clique).value(assnIt);
			    //		    expectations [tidx][idx].plusEqualsSparse (clique.fv, marginal);
			    //ffValue = clique.ff.singleValue(idx);
			    //expectations [tidx][0].incrementValue(0,ffValue*marginal);

			    //int numf=((SequenceTemplate)(clique.tmpl)).getTemplateSize();
			    int numf=expectations [tidx][0].numLocations();
			    int[] sizes = new int[2];
			    //clique.getFf().getDimensions (sizes);
			    for(int i=0; i<numf;i++){
			    	int [] indices=new int[2];
			    	indices[0]=idx;
			    	indices[1]=i;
			    	expectations [tidx][0].incrementValue(i,clique.getFf()[idx][i]*marginal);
			    }
			    if (defaultExpectations[tidx].location (0) != -1)
				defaultExpectations [tidx].incrementValue (0, 0);
			    assnIt.advance (); idx++;
			}
	    }
	}

	/**
	 *Collect the number of apparition of each feature for the given training instance. Contraints are stored in arrays where each cell corresponds to a possible assignment. the function looks at the assignment reference number of the training example and increment the corresponding value in the array of the constraints.
	 */
	public void collectConstraints (InstanceList ilist)
	{
	    double ffValue;
	    for (int inum = 0; inum < ilist.size(); inum++) {
			logger.finest ("*** Collecting constraints for instance "+inum);
			Instance inst = ilist.getInstance (inum);
			UnrolledGraph unrolled = new UnrolledGraph (inst, templates, null, true);//584
			totalNodes =+ unrolled.numVariables ();
			for (Iterator it = unrolled.varSetIterator (); it.hasNext();) {//for each clique in the graph
			    UnrolledVarSet clique = (UnrolledVarSet) it.next();
			    int tidx = clique.tmpl.index;
			    if (tidx == -1) continue;
			    int assn = clique.lookupAssignmentNumber ();
			    //int numf=((SequenceTemplate)(clique.tmpl)).getTemplateSize();
			    int numf=expectations [tidx][0].numLocations();
			    int[] sizes = new int[2];
			    //clique.getFf().getDimensions (sizes);
			    for(int i=0; i<numf;i++){
			    	int [] indices=new int[2];
			    	indices[0]=assn;
			    	indices[1]=i;
			    	constraints [tidx][0].incrementValue(i,clique.getFf()[assn][i]);
			    }
			    //for(int i=0; i<numf;i++)
			    //	constraints [tidx][0].incrementValue (i,clique.getFf()[assn][i]);
			    	
			    //constraints [tidx][0].incrementValue (0,ffValue);
			    //constraints [tidx][assn].plusEqualsSparse (clique.fv);//augment the number of apparition of feature assn if present in the clique
			    if (defaultConstraints[tidx].location (0) != -1)
				defaultConstraints [tidx].incrementValue (0, 0);
			}
	    }
	    int a=0;
	}

	void dumpGradientToFile (String fileName)
	{
	    try {
		PrintStream w = new PrintStream (new FileOutputStream (fileName));
		for (int i = 0; i < numParameters; i++) {
		    w.println (cachedGradient[i]);
		}
		w.close ();
	    } catch (IOException e) {
		System.err.println("Could not open output file.");
		e.printStackTrace ();
	    }
	}

	void dumpDefaults ()
	{
	    System.out.println("Default constraints");
	    for (int i = 0; i < defaultConstraints.length; i++) {
		System.out.println("Template "+i);
		defaultConstraints[i].print ();
	    }
	    System.out.println("Default expectations");
	    for (int i = 0; i < defaultExpectations.length; i++) {
		System.out.println("Template "+i);
		defaultExpectations[i].print ();
	    }
	}

	void printDebugInfo (UnrolledGraph unrolled)
	{
	    print (System.err);
	    Assignment assn = unrolled.getAssignment ();
	    for (Iterator it = unrolled.varSetIterator (); it.hasNext();) {
		UnrolledVarSet clique = (UnrolledVarSet) it.next();
		System.out.println("Clique "+clique);
		dumpAssnForClique (assn, clique);
		Factor ptl = unrolled.factorOf (clique);
		System.out.println("Value = "+ptl.value (assn));
		System.out.println(ptl);
	    }
	}

	void dumpAssnForClique (Assignment assn, UnrolledVarSet clique)
	{
	    for (Iterator it = clique.iterator(); it.hasNext();) {
		Variable var = (Variable) it.next();
		System.out.println(var+" ==> "+assn.getObject (var)
				   +"  ("+assn.get (var)+")");
	    }
	}


	private boolean weightValid (double w, int cnum, int j)
	{
	    if (Double.isInfinite (w)) {
		logger.warning ("Weight is infinite for clique "+cnum+"assignment "+j);
		return false;
	    } else if (Double.isNaN (w)) {
		logger.warning ("Weight is Nan for clique "+cnum+"assignment "+j);
		return false;
	    } else {
		return true;
	    }
	}

	public void report ()
	{
	    int nmsg = -1;
	    if (inferencer instanceof AbstractBeliefPropagation) {
		nmsg = ((AbstractBeliefPropagation)inferencer).getTotalMessagesSent();
	    } else if (inferencer instanceof JunctionTreeInferencer) {
		nmsg = ((JunctionTreeInferencer)inferencer).getTotalMessagesSent();
	    }

	    if (nmsg != -1)
		logger.info ("Total messages sent = "+nmsg);
	}

	public void forceStale ()
	{
	    cachedValueStale = cachedGradientStale = true;
	}

	public int getTotalNodes ()
	{
	    return totalNodes;
	}
    } // MaximizableACRF

    // printing functions

    public void print (OutputStream os)
    {
	PrintStream out = new PrintStream (os);
	out.println ("ACRF. Number of templates: == "+templates.length);

	out.println ("Weights");
	for (int tidx = 0; tidx < templates.length; tidx++) {
	    Template tmpl = templates [tidx];
	    out.println ("TEMPLATE "+tidx+" == "+tmpl);
	    out.println ("Default weights: ");
	    SparseVector defaults = tmpl.getDefaultWeights ();
	    for (int loc = 0; loc < defaults.numLocations (); loc++)
		out.println (" ["+defaults.indexAtLocation (loc)+"] = "+defaults.valueAtLocation (loc));
	    ArrayList<String> features=null;
	    if(tmpl instanceof ContinuousTemplate)
	    	features=((ContinuousTemplate) tmpl).getFeatures();
	    SparseVector[] weights = tmpl.getWeights ();
	    for (int assn = 0; assn < weights.length; assn++) {
			out.println ("Weigth for the Assignment "+assn);
			SparseVector w = weights[assn];
			double [] param=w.getValues();
			for(int i=0; i<param.length;i++){
				if(features!=null){
					out.print(features.get(i)+" ");
				}
				out.println(param[i]);
			}
	    }
	}
    }

    private static void dumpValues (String title, SparseVector[][] values)
    {
	try {
	    for (int cnum = 0; cnum < values.length; cnum++) {
		System.out.println (title+" Clique: "+cnum);
		writeCliqueValues (values [cnum]);
	    }
	} catch (IOException e) {
	    System.err.println("Error writing to file!");
	    e.printStackTrace ();
	}
    }

    private static void writeCliqueValues (SparseVector[] values)
	throws IOException
    {
	System.out.println("Num assignments = "+values.length);
	for (int assn = 0; assn < values.length; assn++) {
	    System.out.println("Num locations = "+values[assn].numLocations());
	    for (int j = 0; j < values[assn].numLocations(); j++) {
		int idx = values[assn].indexAtLocation (j);
		System.out.print ("sparse ["+assn+"]["+idx+"] = ");
		System.out.println (values[assn].valueAtLocation (j));
	    }
	}
    }

    private void dumpOneGraph (UnrolledGraph unrolled)
    {
	Assignment assn = unrolled.getAssignment ();
	for (Iterator it = unrolled.varSetIterator (); it.hasNext();) {
	    UnrolledVarSet clique = (UnrolledVarSet) it.next();
	    System.out.println("Clique "+clique);
	    //				dumpAssnForClique (assn, clique);
	    Factor ptl = unrolled.factorOf (clique);
	    if (ptl != null) System.out.println (ptl);
	}
    }

    public void dumpUnrolledGraphs (InstanceList lst)
    {
	for (int i = 0; i < lst.size(); i++) {
	    Instance inst = lst.getInstance (i);
	    System.out.println("INSTANCE "+i+" : "+inst.getName ());
	    UnrolledGraph unrolled = unroll (inst);
	    dumpOneGraph (unrolled);
	}
    }


    // Templates

    /**
     * A template that adds edges between adjacent nodes in a label
     *  sequence for one factor.
     */
    public static class BigramTemplate extends ACRF.SequenceTemplate {

	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	int factor;

	public BigramTemplate (int factor)
	{
	    this.factor = factor;
	}

	public void addInstantiatedCliques (ACRF.UnrolledGraph graph,
					    FeatureVectorSequence fvs,
					    LabelsSequence lblseq)
	{
	    for (int i = 0; i < lblseq.size() - 1; i++) {
		Variable v1 = graph.getVarForLabel (i, factor);
		Variable v2 = graph.getVarForLabel (i + 1, factor);
		FeatureVector fv = fvs.getFeatureVector (i);

		Variable[] vars = new Variable[] { v1, v2 };
		assert v1 != null : "Couldn't get label factor "+factor+" time "+i;
		assert v2 != null : "Couldn't get label factor "+factor+" time "+(i+1);

		ACRF.UnrolledVarSet clique = new ACRF.UnrolledVarSet (graph, this, vars, fv);
		graph.addClique (clique);
	    }
	}

	@Override
	public int getTemplateSize() {
		// TODO Auto-generated method stub
		return 0;
	}

    }


    /**
     * A template that adds node potentials for a given factor.
     */
    public static class UnigramTemplate extends ACRF.SequenceTemplate {

	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	int factor;

	public UnigramTemplate (int factor)
	{
	    this.factor = factor;
	}

	public void addInstantiatedCliques (ACRF.UnrolledGraph graph,
					    FeatureVectorSequence fvs,
					    LabelsSequence lblseq)
	{
	    for (int i = 0; i < lblseq.size(); i++) {
		Variable v = graph.getVarForLabel (i, factor);
		FeatureVector fv = fvs.getFeatureVector (i);

		Variable[] vars = new Variable[] { v };
		assert v != null : "Couldn't get label factor "+factor+" time "+i;

		ACRF.UnrolledVarSet clique = new ACRF.UnrolledVarSet (graph, this, vars, fv);
		graph.addClique (clique);
	    }
	}

	@Override
	public int getTemplateSize() {
		// TODO Auto-generated method stub
		return 0;
	}
    }
	
    /**
     * A template that adds edges between cotemporal nodes of a given pair
     *  of factors.
     */
    public static class PairwiseFactorTemplate extends ACRF.SequenceTemplate {

	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	int factor0;
	int factor1;

	public PairwiseFactorTemplate (int factor0, int factor1)
	{
	    this.factor0 = factor0;
	    this.factor1 = factor1;
	}

	public void addInstantiatedCliques (ACRF.UnrolledGraph graph,
					    FeatureVectorSequence fvs,
					    LabelsSequence lblseq)
	{
	    for (int i = 0; i < lblseq.size(); i++) {
		Variable v1 = graph.getVarForLabel (i, factor0);
		Variable v2 = graph.getVarForLabel (i, factor1);
		FeatureVector fv = fvs.getFeatureVector (i);

		Variable[] vars = new Variable[] { v1, v2 };
		assert v1 != null : "Couldn't get label factor "+factor0+" time "+i;
		assert v2 != null : "Couldn't get label factor "+factor1+" time "+i;

		ACRF.UnrolledVarSet clique = new ACRF.UnrolledVarSet (graph, this, vars, fv);
		graph.addClique (clique);
	    }
	}

	@Override
	public int getTemplateSize() {
		// TODO Auto-generated method stub
		return 0;
	}

    }

    public static abstract class ContinuousTemplate extends ACRF.SequenceTemplate{
	/**
		 * 
		 */
	private static final long serialVersionUID = 1L;
	int templateSize;
	int factor;
	private double[][] likelihoodTable;
	private ArrayList<String> features;
	private HashMap<String,Integer> modelOrder;
	public ContinuousTemplate (int factor)
	{
	    this.factor=factor;
	}

	protected int initDenseWeights (InstanceList training)
	{
	    int numf = training.getDataAlphabet ().size ();
	    int total = 0;

	    // handle default weights
	    templateSize = 1;// cliqueSizeFromInstance (training);//Store the template size to allocate the expectations and the constraints in the Maximizable
	    int size =1;// we want 1 weitghs for the whole template
	    total += allocateDefaultWeights (size);

	    // and regular weights
	    SparseVector[] newWeights = new SparseVector [size];
	    for (int i = 0; i < size; i++) {
		newWeights [i] = new SparseVector (new double[numf], false);
		if (weights != null)
		    newWeights [i].plusEqualsSparse (weights [i]);
		total += numf;
		logger.info ("ACRF template "+this+" weights ["+i+"] num features "+numf);
	    }

	    logger.info ("ACRF template "+this+" total num weights = "+total);
	    weights = newWeights;
	    return total;
	}
	public  AbstractTableFactor computeFactor (UnrolledVarSet clique){
    	Matrix phi = createFactorMatrix(clique);
    	double[][] ff = new double[phi.numLocations()][this.getTemplateSize()]; //varDimensions ()[0] is the number of labels
    	
    	SparseVector[] weights = getWeights();
	String outcome;
	Variable v;
	int[] indices = new int[1];
	int label;
	int uttIdx=0;
	double[] values = new double[1];
	AugmentableFeatureVector fv;
	for (int loc = 0; loc < phi.numLocations(); loc++) {
	    int idx = phi.indexAtLocation(loc);
	    phi.singleToIndices(idx,indices);
	    v = clique.getVars()[0];
	    outcome=v.getLabelAlphabet().lookupLabel(indices[0]).toString();
	    int modIdx = (int)modelOrder.get(outcome);
	    uttIdx=(int)clique.fv.getValues()[0];
	    values[0]=likelihoodTable[uttIdx][modIdx];
	    SparseVector w = weights[0];//Assume that the parameters are tied over all the assignments
	    fv = new AugmentableFeatureVector (clique.fv.getAlphabet(), clique.fv.getIndices(),values,1,1,false,false,false);
	    double dp = w.dotProduct(fv);
	    //			System.out.println("outcome acoustic: "+outcome+" et de N(): "+values[0]+" dp: "+dp+" default: "+getDefaultWeight(idx));
	    //		System.out.println("feature: "+x+" et de N(): "+values[0]+" dp: "+dp+" default: "+getDefaultWeight(idx));
	    dp += getDefaultWeight(idx);
	    for(int i=0; i<values.length;i++){
	    	ff[loc][i]= values[i];
	    }
	    phi.setValueAtLocation(loc, dp);

	}
	if(uttIdx==470){
	    int a=0;
	}
	AbstractTableFactor ptl = new LogTableFactor(clique);
	ptl.setValues(phi);
	clique.setFf(ff);
	return ptl;
	}
	public int getFactor(){ return factor;}
	public int getTemplateSize(){return templateSize;}
	public void setTemplateSize(int templateSize){this.templateSize=templateSize;}
	public void setModelOrder(HashMap<String,Integer> modelOrder) { this.modelOrder=modelOrder;}
	public void setTable(double[][] table) { this.likelihoodTable =table;}
	public  HashMap<String,Integer> getModelOrder() { return this.modelOrder;}
	public double[][]  getTable() { return this.likelihoodTable;}
	public ArrayList<String>  getFeatures() { return this.features;}
    }

    

    
    
    
    
    // Convenient methods for constructing ACRFs
    public static ACRF makeFactorial (Pipe p, int numLevels)
    {
	ArrayList t = new ArrayList ();
	for (int i = 0; i < numLevels; i++) {
	    t.add (new BigramTemplate (i));
	    if (i+1 < numLevels)
		t.add (new PairwiseFactorTemplate (i, i+1));
	}
	Template[] tmpls = (Template[]) t.toArray (new Template [t.size()]);
	return new ACRF (p, tmpls);
    }

    // I hate serialization

    private static final long serialVersionUID = 2865175696692468236L;//2113750667182393436L;

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException
    {
	in.defaultReadObject ();
	graphCache = new THashMap ();
    }



}	// ACRF

