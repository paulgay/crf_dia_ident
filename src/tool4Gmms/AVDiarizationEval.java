package tool4Gmms;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.grmm.inference.Inferencer;
import edu.umass.cs.mallet.grmm.learning.ACRF;
import edu.umass.cs.mallet.grmm.learning.ACRFTrainer;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.Label;
import edu.umass.cs.mallet.base.types.Labels;
import edu.umass.cs.mallet.base.types.LabelsSequence;

public class AVDiarizationEval{
    public static void writeResults(ACRF acrf, InstanceListRepere testing,String dir){
	File outputdir=new File(dir);
	if(!outputdir.exists())
	    outputdir.mkdir();
	for (int i = 0; i < testing.size(); i++) {
	    InstanceRepere inst =(InstanceRepere)testing.getInstance (i);
	    LabelsSequence lbls= acrf.getBestLabels (inst);
	    writeMapping(lbls,inst,outputdir);
		}
    }
    public static void writeResultsUniqEnforce(ACRF acrf, InstanceListRepere testing,String dir){
    	File outputdir=new File(dir);
    	if(!outputdir.exists())
    	    outputdir.mkdir();
    	for (int i = 0; i < testing.size(); i++) {
    	    InstanceRepere inst =(InstanceRepere)testing.getInstance (i);
    	    HashMap<Integer,String> lbls= acrf.getBestLabelsUniqEnforce (inst);
    	    writeMapping(lbls,inst,outputdir);
    		}
        }
    
	public static void writeMargin(ACRF acrf, InstanceListRepere testing,String dir){
    	File outputdir=new File(dir);
    	if(!outputdir.exists())
    	    outputdir.mkdir();
    	for (int i = 0; i < testing.size(); i++) {
    	    InstanceRepere inst =(InstanceRepere)testing.getInstance (i);
    	    HashMap<String, HashMap<String, Double>> margin= acrf.getMarginAware (inst);
    	    writeMarginInFile(margin,inst,outputdir);
    	    //writeAcousticMdtm(lbls,inst,iterNumber);
    	    //writeAssocWithNewLabels(lbls,inst,iterNumber);
    	    //displayStats(lbls,inst);
    		}
        }

    private static void writeMarginInFile(
			HashMap<String, HashMap<String, Double>> margin, InstanceRepere inst,
			File outputdir) {
    	try{
			inst.getData ();
		    ArrayList<String> segmentNames=inst.getSegmentNames();
		    String show= (String)inst.getName();
		    String outputFile=outputdir.getAbsolutePath()+"/"+show+".margin";
		    FileWriter fstream;
		    fstream = new FileWriter(outputFile);
		    BufferedWriter out = new BufferedWriter(fstream);
		    for(Entry<String,HashMap<String, Double>> entry: margin.entrySet()){
		    	HashMap<String, Double> marg=entry.getValue();
		    	out.write(entry.getKey());
		    	for (Entry<String, Double> label : marg.entrySet())
		    		out.write(" "+label.getKey()+":"+label.getValue());
		    	out.write("\n");
		    }
		    out.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	public static void writeMapping(LabelsSequence lbls,InstanceRepere instance,File outputdir){
	try {
	    InstanceRepere inst = (InstanceRepere)instance;
	    LabelsSequence lblseqRef = (LabelsSequence) inst.getTarget ();
	    ArrayList<String> segmentNames=inst.getSegmentNames();
	    String show= (String)inst.getName();
	    String outputFile=outputdir.getAbsolutePath()+"/"+show+".mapping";
	    FileWriter fstream;
	    fstream = new FileWriter(outputFile);
	    BufferedWriter out = new BufferedWriter(fstream);
	    for(int i=0;i<lbls.size();i++){
			out.write(lbls.get(i)+" "+segmentNames.get(i)+" "+lblseqRef.get(i)+"\n");
	    }
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
	private static void writeMapping(HashMap<Integer, String> lbls,	InstanceRepere inst, File outputdir) {
		try {
		    LabelsSequence lblseqRef = (LabelsSequence) inst.getTarget ();
		    ArrayList<String> segmentNames=inst.getSegmentNames();
		    String show= (String)inst.getName();
		    String outputFile=outputdir.getAbsolutePath()+"/"+show+".mapping";
		    FileWriter fstream;
		    fstream = new FileWriter(outputFile);
		    BufferedWriter out = new BufferedWriter(fstream);
		    for(int i=0;i<lbls.size();i++){
				out.write(lbls.get(new Integer(i))+" "+segmentNames.get(i)+" "+lblseqRef.get(i)+"\n");
		    }
		    out.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}	
	}
	
	public static void writeMarginAndResults(ACRF acrf,
			InstanceListRepere testing, String getoutputDir) {
		// TODO Auto-generated method stub
		
	}
}