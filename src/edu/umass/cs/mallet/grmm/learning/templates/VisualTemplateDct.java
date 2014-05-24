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

public class VisualTemplateDct extends ACRF.ContinuousTemplate{
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private double[][] likelihoodTable;
    	private ArrayList<String> trackOrder;
    	private  HashMap<String,Integer> modelOrder;
    	private ArrayList<String> types;
    	private FeatureVectorSequence segmentNumbers;
    	private String show;
        public VisualTemplateDct(int factor){
    		super(factor);
    	}
    	 public void setAttributes(InstanceRepere inst){
    		 super.setTable(inst.getVCostTable());
    		 super.setModelOrder(inst.getLabelIdx());
    		 //likelihoodTable=inst.getVSiftCostTable();
    		 //modelOrder=inst.getLabelIdx();
    		 types=inst.getTypes();
    		 trackOrder=inst.getSegmentNames();
    		 show= (String)inst.getName();
    		 segmentNumbers=(FeatureVectorSequence)inst.getData();
    	 }
    	 public boolean hasModel(int trackId)throws Exception{
    		 for(int i=0;i<trackOrder.size();i++){
    			 if(types.get(i).equals("faceTrack")){
    				 FeatureVector fv = segmentNumbers.getFeatureVector (i);
    				 int trackNumber =(int)fv.getValues()[0];
    				 if(trackNumber==trackId){
    					 String trackName=trackOrder.get(i);
    					 if(modelOrder.containsValue(trackName))
    						 return true;
    					 else{
    						 return false;						 
    					 }

    				 }
    			 }
    		 }
    		 throw new Exception("facetrack index: "+trackId+" not found");
    	 }

    	 public void addInstantiatedCliques (ACRF.UnrolledGraph graph,
    			    FeatureVectorSequence fvs,
    			    LabelsSequence lblseq)
    		{
    			System.out.println("ACRF version is used with Instance repere, no method set yet to instantiate clique without the whole instance");
    		}
    	 public void addInstantiatedCliques (UnrolledGraph graph, Instance instance){
    			InstanceRepere inst  = (InstanceRepere)instance;
    			setAttributes(inst);
    			FeatureVectorSequence fvs = (FeatureVectorSequence) inst.getData ();
    			LabelsSequence lblseq = (LabelsSequence) inst.getTarget ();
                        int factor=super.getFactor();
    			for (int i = 0; i < lblseq.size(); i++) {
    				Variable v = graph.getVarForLabel (i, factor);
    				FeatureVector fv = fvs.getFeatureVector (i);
    				if(types.get(i).equals("faceTrack")){
    					int[] indices = {0};
    				    double[] values = {fv.getValues()[0]};
    				    fv = new AugmentableFeatureVector (fv.getAlphabet(), indices,values,1,1,false,false,false);
    				    Variable[] vars = new Variable[] { v };
    				    assert v != null : "Couldn't get label factor "+factor+" time "+i;
    				    ACRF.UnrolledVarSet clique = new ACRF.UnrolledVarSet (graph, this, vars, fv);
    				    graph.addClique (clique);
    				}
    		    }			
    	 }
    	public ArrayList<String> getTrackOrder(){
    		return trackOrder;
    	}	
    	
    
}	
