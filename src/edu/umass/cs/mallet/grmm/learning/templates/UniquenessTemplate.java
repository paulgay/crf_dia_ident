package edu.umass.cs.mallet.grmm.learning.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

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

public class UniquenessTemplate extends  ACRF.FixedFactorTemplate{
	private static final long serialVersionUID = 1L;
	private ArrayList<String> segmentNames;
	private String show;
	private ArrayList<ArrayList<String>> pairs;
	public UniquenessTemplate(){
	    super();
	}
	public void setAttributes(InstanceRepere inst){
		show= (String)inst.getName();
		segmentNames=inst.getSegmentNames();
		pairs = inst.getUniqPairs();
	}
	
	/*protected int initDenseWeights (InstanceList training)
	{
	    int numf = 1;
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
	}*/
	public void addInstantiatedCliques (ACRF.UnrolledGraph graph,   FeatureVectorSequence fvs,  LabelsSequence lblseq)
		{
			System.out.println("ACRF version is used with Instance repere, no method set yet to instantiate clique without the whole instance");
		}
	
	public void addInstantiatedCliques (UnrolledGraph graph, Instance instance){
		InstanceRepere inst  = (InstanceRepere)instance;
		setAttributes(inst);
		FeatureVectorSequence fvs = (FeatureVectorSequence) inst.getData ();
		inst.getTarget ();
		for (ArrayList<String> a : pairs) {
			int j = segmentNames.indexOf(a.get(0));
			int i = segmentNames.indexOf(a.get(1));
			if(i==-1 || j==-1 || i==j )
				continue;
			Variable ft1 = graph.getVarForLabel (j, 0);
			Variable ft2 = graph.getVarForLabel (i, 0);
			double[] values = {0};
			int[] indices = {0};
			FeatureVector ftfv = fvs.getFeatureVector (j);
			AugmentableFeatureVector fv = new AugmentableFeatureVector (ftfv.getAlphabet(), indices,values,1,1,false,false,false);
			Variable[] vars = new Variable[] { ft1, ft2 };
			assert ft1 != null : "Couldn't get label factor "+"0"+" time "+i;
			assert ft2 != null : "Couldn't get label factor "+"0"+" time "+(i+1);
			ACRF.UnrolledVarSet clique = new ACRF.UnrolledVarSet (graph, this, vars, fv);
			 graph.addClique (clique);
		}
	}
	public AbstractTableFactor computeFactor (UnrolledVarSet clique)
	{
	    Matrix phi = createFactorMatrix(clique);//enable to get the labels from the matrix position
	    SparseVector[] weights = getWeights();
	    String outcome1,outcome2;
	    Variable v;
	    //SparseVector w = weights[0];
	    //double[] param=w.getValues();
	    //double[][] ff = new double[phi.numLocations()][param.length]; //varDimensions ()[0] is the number of labels
	    double[][] ff = new double[phi.numLocations()][1];
	    for (int loc = 0; loc < phi.numLocations(); loc++) {
			int[] indices = new int[2];
			int idx = phi.indexAtLocation(loc);
			phi.singleToIndices(idx,indices);
			v = clique.getVars()[0];
			outcome1=v.getLabelAlphabet().lookupLabel(indices[0]).toString();
			v = clique.getVars()[1];
			outcome2=v.getLabelAlphabet().lookupLabel(indices[1]).toString();
			//double[] values = new double[param.length];
			double[] values = new double[1];
			for(int i=0;i<values.length;i++)
			    values[i]=0;
			if(outcome1.equals(outcome2))
			    //values[0]=-1;
				values[0]=-1000;
			else{
			    values[0]=1000;
				//values[0]=1;
			}
			double dp=0;
			for(int i=0; i<values.length;i++){
			    //dp+=param[i]*values[i];
			    dp+=values[i];
			    ff[loc][i]=values[i];
			}
			//		System.out.println("outcome 1: "+outcome1+" outcome2: "+outcome2+" feature: "+x+" et de N(): "+values[0]+" dp: "+dp+" default: "+getDefaultWeight(idx));
			//dp += getDefaultWeight(idx);
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

}
