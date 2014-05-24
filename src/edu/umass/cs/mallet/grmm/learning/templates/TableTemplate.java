package edu.umass.cs.mallet.grmm.learning.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import tool4Gmms.InstanceListRepere;
import tool4Gmms.InstanceRepere;
import edu.umass.cs.mallet.base.types.AugmentableFeatureVector;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.LabelsSequence;
import edu.umass.cs.mallet.base.types.Matrix;
import edu.umass.cs.mallet.base.types.Matrixn;
import edu.umass.cs.mallet.base.types.SparseVector;
import edu.umass.cs.mallet.grmm.learning.ACRF;
import edu.umass.cs.mallet.grmm.learning.ACRF.UnrolledGraph;
import edu.umass.cs.mallet.grmm.learning.ACRF.UnrolledVarSet;
import edu.umass.cs.mallet.grmm.types.AbstractTableFactor;
import edu.umass.cs.mallet.grmm.types.LogTableFactor;
import edu.umass.cs.mallet.grmm.types.Variable;

public class TableTemplate extends ACRF.ContinuousTemplate{
	private static final long serialVersionUID = 1L;
	private ArrayList<String> segmentNames;
	private String show;
	private HashMap<String, ArrayList<String>> featuresBySegment;
	private HashMap<String, double[][]> tables;
	private ArrayList<String> features;
	public TableTemplate(int factor){
		super(factor);
	}
	 public void setAttributes(InstanceRepere inst){
		 segmentNames=inst.getSegmentNames();
		 super.setModelOrder(inst.getLabelIdx());
		 featuresBySegment=inst.getFeatures();
		 tables=inst.getTables();
		 show= (String)inst.getName();
		 super.setModelOrder(inst.getLabelIdx());
	 }
		protected int initDenseWeights (InstanceList training)
		{
			features = new ArrayList<String>();
			features.addAll(((InstanceListRepere)training).getFeatures());
		    int numf = features.size();
		    int total = 0;

		    // handle default weights
		    int size =1;// we want 1 weitghs for the whole template
		    total += allocateDefaultWeights (size);

		    // and regular weights
		    SparseVector[] newWeights = new SparseVector [size];
		    for (int i = 0; i < size; i++) {
			newWeights [i] = new SparseVector (new double[numf], false);
			if (weights != null)
			    newWeights [i].plusEqualsSparse (weights [i]);
			total += numf;
		    }

		    weights = newWeights;
		    return total;
		}

	 public void addInstantiatedCliques (UnrolledGraph graph, Instance instance){
		InstanceRepere inst  = (InstanceRepere)instance;
		setAttributes(inst);
		FeatureVectorSequence fvs = (FeatureVectorSequence) inst.getData ();
		LabelsSequence lblseq = (LabelsSequence) inst.getTarget ();
		int factor=super.getFactor();
	    for (int i = 0; i < lblseq.size(); i++) {
	    	if(featuresBySegment.containsKey(segmentNames.get(i))){
    			Variable v = graph.getVarForLabel (i, factor);
    			FeatureVector fv = fvs.getFeatureVector (i);
    			int[] indices = {0};
    			double[] values = {i};//the index of the segment
    			fv = new AugmentableFeatureVector (fv.getAlphabet(), indices,values,1,1,false,false,false);
    			Variable[] vars = new Variable[] { v };
    			assert v != null : "Couldn't get label factor "+factor+" time "+i;
    			ACRF.UnrolledVarSet clique = new ACRF.UnrolledVarSet (graph, this, vars, fv);
    			graph.addClique (clique);
    		}
	    }
	 }
	public void addInstantiatedCliques (ACRF.UnrolledGraph graph,
					    FeatureVectorSequence fvs,
					    LabelsSequence lblseq)
	{
		System.out.println("ACRF version is used with Instance repere, no method set yet to instantiate clique without the whole instance");
	}
	public ArrayList<String> getUtterOrder(){
		return segmentNames;
	}
	public  AbstractTableFactor computeFactor (UnrolledVarSet clique){
    	Matrix phi = createFactorMatrix(clique);
    	SparseVector[] weights = getWeights();
    	String outcome;
    	Variable v;
    	AugmentableFeatureVector fv;
		int segIdx=(int)clique.getFv().getValues()[0];
		ArrayList<String> featSegment = featuresBySegment.get(segmentNames.get(segIdx));
		SparseVector w = weights[0];//Assume that the parameters are tied over all the assignments
		double [] param = w.getValues();
    	int [] inds = new int[2];
    	inds[0]=clique.varDimensions ()[0];
    	inds[1]=param.length;
    	double ff[][] = new double[phi.numLocations()][param.length]; //varDimensions ()[0] is the number of labels
    	double[] values = new double[param.length];
    	int[] indices = new int[1];
    	for (int loc = 0; loc < phi.numLocations(); loc++) {
    		int idx = phi.indexAtLocation(loc);
    		phi.singleToIndices(idx,indices);
    		v = clique.getVars()[0];
    		outcome=v.getLabelAlphabet().lookupLabel(indices[0]).toString();
        	for(int i=0; i<values.length;i++){
        		values[i]=0;
        	}
        	int modIdx = (int)super.getModelOrder().get(outcome);
        	for(String feature: featSegment ){
        		double[][] table = tables.get(feature);
        		idx=features.indexOf(feature);
        		values[idx]=table[segIdx][modIdx];
        	}
    		//fv = new AugmentableFeatureVector (clique.getFv().getAlphabet(), indices,values,1,1,false,false,false);
    		//double dp = w.dotProduct(fv);
    		double dp=0;
    		//for(int i=0;i<values.length;i++)
    		//	dp+=param[i]*values[i];
			dp += getDefaultWeight(idx);
			for(int i=0; i<values.length;i++){
				dp+=param[i]*values[i];
				inds[0]=loc;inds[1]=i;
				ff[loc][i]=values[i];
			}
			phi.setValueAtLocation(loc, dp);

		   }
    	AbstractTableFactor ptl = new LogTableFactor(clique);
    	ptl.setValues(phi);
    	clique.setFf(ff);
    	return ptl;
	}
	public int getTemplateSize() {
		return 1;
	}
	public ArrayList<String> getFeatures() { return features;}
}
