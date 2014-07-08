package edu.umass.cs.mallet.grmm.learning.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.sun.xml.internal.fastinfoset.sax.Features;

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

public class PairWiseTemplate extends ACRF.ContinuousTemplate{
	private static final long serialVersionUID = 1L;
	private ArrayList<String> segmentNames;
	private String show;
	private HashMap<String, double[][]> tables;
	private ArrayList<String> features;
	public PairWiseTemplate(){
		super(0);
	}
	 public void setAttributes(InstanceRepere inst){
		 segmentNames=inst.getSegmentNames();
		 super.setModelOrder(inst.getLabelIdx());
		 tables=inst.getPairWise();
		 show= (String)inst.getName();
	 }
	protected int initDenseWeights (InstanceList training){
		features = new ArrayList<String>(((InstanceListRepere)training).getPairWiseFeatures());
	    int numf = features.size();//the number of used features
	    int total = 0;
	    // handle default weights
	    int size =1;// we want 1 weitghs for all labels
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
		LabelsSequence lblseq = (LabelsSequence) inst.getTarget ();//contains labels for each segment
		int factor=super.getFactor();
	    for (int i = 0; i < lblseq.size(); i++) {
	    	for (int j = 0; j < lblseq.size(); j++) {
	    		for(Map.Entry<String, double[][]> entry : tables.entrySet()){
	    			double[][] table=entry.getValue();
	    			if(table[i][j]!=0){
		    			Variable v1 = graph.getVarForLabel (i, factor);
		    			Variable v2 = graph.getVarForLabel (j, factor);
		    			FeatureVector fv1 = fvs.getFeatureVector (i);
		    			int[] indices = {0, 1};
		    			double[] values = {i, j };//the index of the segment
		    			AugmentableFeatureVector fv = new AugmentableFeatureVector (fv1.getAlphabet(), indices,values,1,1,false,false,false);
		    			Variable[] vars = new Variable[] { v1, v2 };
		    			assert v1 != null : "Couldn't get label factor "+factor+" time "+i;
		    			assert v2 != null : "Couldn't get label factor "+factor+" time "+(i+1);
		    			ACRF.UnrolledVarSet clique = new ACRF.UnrolledVarSet (graph, this, vars, fv);
		    			graph.addClique (clique);
		    			break;
		    		}	    			
	    		}
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
    	String outcome1, outcome2;
    	Variable v1,v2;
    	AugmentableFeatureVector fv;
    	int seg1Idx=(int)clique.getFv().getValues()[0];
    	int seg2Idx=(int)clique.getFv().getValues()[1];
		SparseVector w = weights[0];//Assume that the parameters are tied over all the assignments
		double [] param = w.getValues();
    	double ff[][] = new double[phi.numLocations()][param.length]; //varDimensions ()[0] is the number of labels
    	double[] values = new double[param.length];
    	int[] indices = new int[2];
    	for (int loc = 0; loc < phi.numLocations(); loc++) {
    		int idx = phi.indexAtLocation(loc);
    		phi.singleToIndices(idx,indices);
    		v1 = clique.getVars()[0];
    		v2 = clique.getVars()[1];
    		outcome1=v1.getLabelAlphabet().lookupLabel(indices[0]).toString();
    		outcome2=v2.getLabelAlphabet().lookupLabel(indices[1]).toString();
        	for(int i=0;i<features.size();i++){
        		double[][] table = tables.get(features.get(i));
        		if(outcome1.equals(outcome2))
        			values[i]=table[seg1Idx][seg2Idx];
        		else
        			values[i]=-table[seg1Idx][seg2Idx];
        	}
    		double dp=0;
			dp += getDefaultWeight(idx);
			for(int i=0; i<values.length;i++){
				dp+=param[i]*values[i];
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
