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
    public static void writeResults(ACRF acrf, InstanceListRepere testing,String dir, int iterNumber){
	File outputdir=new File(dir);
	if(!outputdir.exists())
	    outputdir.mkdir();
	for (int i = 0; i < testing.size(); i++) {
	    InstanceRepere inst =(InstanceRepere)testing.getInstance (i);
	    LabelsSequence lbls= acrf.getBestLabels (inst);
	    writeMapping(lbls,inst,outputdir, iterNumber);
	    //writeAcousticMdtm(lbls,inst,iterNumber);
	    //writeAssocWithNewLabels(lbls,inst,iterNumber);
	    //displayStats(lbls,inst);
		}
    }
    public static void writeResults(ACRF acrf, InstanceListRepere testing, int iterNumber){
	File outputdir=new File(((InstanceRepere)testing.getInstance(0)).getDir()+"/iter"+iterNumber+"/list/");
	if(!outputdir.exists())
	    outputdir.mkdir();
	for (int i = 0; i < testing.size(); i++) {
	    InstanceRepere inst =(InstanceRepere)testing.getInstance (i);
	    LabelsSequence lbls= acrf.getBestLabels (inst);
	    writeMapping(lbls,inst,outputdir,iterNumber);
	    //writeAcousticMdtm(lbls,inst,iterNumber);
	    //writeAssocWithNewLabels(lbls,inst,iterNumber);
	    //displayStats(lbls,inst);
		}
    }
    public static void writeMargin(ACRF acrf, InstanceListRepere testing,String dir, int iterNumber){
    	File outputdir=new File(dir);
    	if(!outputdir.exists())
    	    outputdir.mkdir();
    	for (int i = 0; i < testing.size(); i++) {
    	    InstanceRepere inst =(InstanceRepere)testing.getInstance (i);
    	    HashMap<String, HashMap<String, Double>> margin= acrf.getMargin (inst);
    	    writeMarginInFile(margin,inst,outputdir);
    	    //writeAcousticMdtm(lbls,inst,iterNumber);
    	    //writeAssocWithNewLabels(lbls,inst,iterNumber);
    	    //displayStats(lbls,inst);
    		}
        }

    public static void writeMargin(ACRF acrf, InstanceListRepere testing,int iterNumber){
    	File outputdir=new File(((InstanceRepere)testing.getInstance(0)).getDir()+"/iter"+iterNumber+"/list/");
    	if(!outputdir.exists())
    	    outputdir.mkdir();
    	for (int i = 0; i < testing.size(); i++) {
    	    InstanceRepere inst =(InstanceRepere)testing.getInstance (i);
    	    HashMap<String, HashMap<String, Double>> margin= acrf.getMargin (inst);
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
		    inst.getTypes();
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
	public static void writeMapping(LabelsSequence lbls,InstanceRepere instance,File outputdir, int iterNumber){
	try {
	    InstanceRepere inst = (InstanceRepere)instance;
	    LabelsSequence lblseqRef = (LabelsSequence) inst.getTarget ();
	    FeatureVectorSequence fvs = (FeatureVectorSequence) inst.getData ();
	    ArrayList<String> segmentNames=inst.getSegmentNames();
	    ArrayList<String> types=inst.getTypes();
	    String show= (String)inst.getName();
	    String outputFile=outputdir.getAbsolutePath()+"/"+show+".mapping";
	    FileWriter fstream;
	    double idx;
	    fstream = new FileWriter(outputFile);
	    BufferedWriter out = new BufferedWriter(fstream);
	    for(int i=0;i<lbls.size();i++){
			FeatureVector fv = fvs.getFeatureVector (i);
			idx = fv.getValues()[0];
			out.write(lbls.get(i)+" "+segmentNames.get(i)+" "+idx+" "+types.get(i)+" "+lblseqRef.get(i)+"\n");
	    }
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    public static void writeAssocWithNewLabels(LabelsSequence lbls,InstanceRepere inst,int iterNumber){
	try{
	    LabelsSequence lblseqRef = (LabelsSequence) inst.getTarget ();
	    inst.getTypes();
	    HashMap<String,String> oldToPred = new HashMap<String,String>();
	    for(int i=0;i<lbls.size();i++)
		oldToPred.put(((Labels)lblseqRef.get(i)).get(0).toString(),((Labels)lbls.get(i)).get(0).toString());
	    String show= (String)inst.getName();
	    File outputdir=new File(inst.getDir()+"/fusion/iter"+(iterNumber+1));
	    if(!outputdir.exists())
		outputdir.mkdir();
	    String outputFile=inst.getDir()+"/fusion/iter"+(iterNumber+1)+"/"+show+".value4dec";
	    BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
	    FileReader finStream;
	    if(iterNumber==1)
		finStream = new FileReader(inst.getDir()+"/fusion/iter"+iterNumber+"/"+show+".value4dec2");
	    else
		finStream= new FileReader(inst.getDir()+"/fusion/iter"+iterNumber+"/"+show+".value4dec");
	    BufferedReader in = new BufferedReader(finStream);
	    String line;
	    String pattern="[ ]+";
	    String[] tokens = null;
	    while((line = in.readLine())!=null){
		tokens=line.split(pattern);
		out.write(line.replace(tokens[0],oldToPred.get(tokens[0])));
		out.write(line.replace(tokens[1],oldToPred.get(tokens[1])));
	    }
	    out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(0);
	}
    }
    public static void writeAcousticMdtm(LabelsSequence lbls,InstanceRepere instance,int iterNumber){
	try{
	    InstanceRepere inst = (InstanceRepere)instance;
	    inst.getTarget ();
	    ArrayList<String> segmentNames=inst.getSegmentNames();
	    ArrayList<String> types=inst.getTypes();
	    String show= (String)inst.getName();
	    String outputFile=inst.getDir()+"/crfOutputs/iter"+iterNumber+"/"+show+".amdtm";
	    FileWriter fstream;
	    fstream = new FileWriter(outputFile);
	    BufferedWriter out = new BufferedWriter(fstream);
	    FileReader finStream;
	    BufferedReader in;
	    String line;
	    String pattern="[ ]+";
	    String[] tokens = null;
	    String tempSegmentName="";
	    for(int i=0;i<lbls.size();i++){
		if(types.get(i).equals("utterance")){
		    double startTime=-1;
		    double length=-1;
		    String utterName = segmentNames.get(i);
		    finStream= new FileReader(inst.getDir()+"/audio/mdtm/iter"+iterNumber+"/"+show+".segset");
		    in = new BufferedReader(finStream);
		    while((line = in.readLine())!=null && !utterName.equals(tempSegmentName)){
			tokens=line.split(pattern);
			tempSegmentName=tokens[4];
			startTime= Double.parseDouble(tokens[1])/100;
			length = Double.parseDouble(tokens[2])/100 - startTime;
		    }
		    if(startTime==-1)
			throw new Exception("impossible negative start time, no utterance in the seg file: "+inst.getDir()+"/audio/mdtm/"+show+".segset has been found to match with the predicted utterance"+utterName);
		    out.write(show+" 1 "+startTime+" "+length+" speaker 0 adult_male "+lbls.get(i)+"\n");
		}
	    }
	    out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(0);
	}
    }
    public static void writeAcousticSeg(LabelsSequence lbls,InstanceRepere instance,int iterNumber){
	try{
	    InstanceRepere inst = (InstanceRepere)instance;
	    inst.getTarget ();
	    ArrayList<String> segmentNames=inst.getSegmentNames();
	    ArrayList<String> types=inst.getTypes();
	    String show= (String)inst.getName();
	    String outputFile=inst.getDir()+"/crfOutputs/iter"+iterNumber+"/"+show+".aseg";
	    FileWriter fstream = new FileWriter(outputFile);
	    BufferedWriter out = new BufferedWriter(fstream);
	    FileReader finStream;
	    BufferedReader in;
	    String line;
	    String pattern="[ ]+";
	    String[] tokens = null;
	    String tempSegmentName="";
	    for(int i=0;i<lbls.size();i++){
		if(types.get(i).equals("utterance")){
		    int startTime=-1;
		    int length=-1;
		    String utterName = segmentNames.get(i);
		    finStream= new FileReader(inst.getDir()+"/audio/mdtm/iter"+iterNumber+"/"+show+".segset");
		    in = new BufferedReader(finStream);
		    while((line = in.readLine())!=null && !utterName.equals(tempSegmentName)){
			tokens=line.split(pattern);
			tempSegmentName=tokens[4];
			startTime= Integer.parseInt(tokens[1]);
			length = Integer.parseInt(tokens[2]) - startTime;
		    }
		    if(startTime==-1)
			throw new Exception("impossible negative start time, no utterance in the seg file: "+inst.getDir()+"/audio/mdtm/"+show+".segset has been found to match with the predicted utterance"+utterName);
		    out.write(show+" 1 "+startTime+" "+length+" U U U "+lbls.get(i)+"\n");
		}
	    }
	    out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(0);
	}
    }

    public static void displayStats(LabelsSequence lbls,InstanceRepere inst){
	inst.getTarget ();
	FeatureVectorSequence fvs = (FeatureVectorSequence) inst.getData ();
	inst.getSegmentNames();
	ArrayList<String> types=inst.getTypes();
	HashMap<String,Integer> labelIdx = inst.getLabelIdx();
	double[][] aVCostTable = inst.getAVCostTable();
	double[][] aCostTable=inst.getACostTable();
	double[][] vCostTable=inst.getVCostTable();
	int vmmma=0,vmmap=0,vmpma=0,vmpap=0,ammma=0,ammap=0,ampma=0,ampap=0,numberOfUtt=0,numberOfTrack=0;
	//processing of the face tracks
	for(int i=0;i<lbls.size();i++){
	    FeatureVector fv = fvs.getFeatureVector (i);
	    int idxSeg = (int)fv.getValues()[0];
	    boolean mm,ma;
	    String  predLabel=((Labels)lbls.get(i)).get(0).toString();//assume the interesting label is at 0 i.e. 1 label per segment
	    if(types.get(i).equals("faceTrack")){
		numberOfTrack+=1;
		//finding the best biometric model
		int bestModelIdx=0;
		for(int j=0;j<labelIdx.size();j++){
		    if(vCostTable[idxSeg][j]<vCostTable[idxSeg][bestModelIdx])
			bestModelIdx=j;
		}
		mm=(bestModelIdx==(int)labelIdx.get(predLabel));
		//finding the best association
		double bestAssocScore=0;
		String bestAssocName="";
		HashMap<String,Double> assocPerCluster = new HashMap<String,Double>();
		for(int j=0;j<fvs.size();j++){
		    if(types.get(j).equals("utterance")){
			String clusName=((Labels)lbls.get(j)).get(0).toString();
			int usegIdx=(int)fvs.getFeatureVector(j).getValues()[0];
			if(assocPerCluster.containsKey(clusName))
			    assocPerCluster.put(clusName,new Double(assocPerCluster.get(clusName)+aVCostTable[usegIdx][idxSeg]));
			else
			    assocPerCluster.put(clusName,new Double(aVCostTable[usegIdx][idxSeg]));
		    }
		}
		for (Map.Entry<String, Double> entry : assocPerCluster.entrySet()) {
		    if((double)entry.getValue()>bestAssocScore){
			bestAssocScore=(double)entry.getValue();
			bestAssocName=entry.getKey();
		    }
		}
		ma=(bestAssocName.equals(predLabel));
		if(ma && mm)
		    vmmma+=1;
		if(!ma && mm)
		    vmmap+=1;
		if(ma&&!mm)
		    vmpma+=1;
		if(!ma&&!mm)
		    vmpap+=1;
	    }
	    else{
		//finding the best biometric model
		int bestModelIdx=0;
		for(int j=0;j<labelIdx.size();j++){
		    if(aCostTable[idxSeg][j]>aCostTable[idxSeg][bestModelIdx])
			bestModelIdx=j;
		}
		mm=(bestModelIdx==(int)labelIdx.get(predLabel));
		//finding the best association
		double bestAssocScore=0;
		String bestAssocName="";
		HashMap<String,Double> assocPerCluster = new HashMap<String,Double>();
		for(int j=0;j<fvs.size();j++){
		    if(types.get(j).equals("faceTrack")){
			int ftsegIdx = (int)fvs.getFeatureVector(j).getValues()[0];
			String clusName=((Labels)lbls.get(j)).get(0).toString();
			if(assocPerCluster.containsKey(clusName))
			    assocPerCluster.put(clusName,new Double(assocPerCluster.get(clusName)+aVCostTable[idxSeg][ftsegIdx]));
			else
			    assocPerCluster.put(clusName,new Double(aVCostTable[idxSeg][ftsegIdx]));
		    }
		}
		for (Map.Entry<String, Double> entry : assocPerCluster.entrySet()) {
		    if((double)entry.getValue()>bestAssocScore){
			bestAssocScore=(double)entry.getValue();
			bestAssocName=entry.getKey();
		    }
		}
		ma=(bestAssocName.equals(predLabel));
		if(ma && mm)
		    ammma+=1;
		if(!ma && mm)
		    ammap+=1;
		if(ma&&!mm)
		    ampma+=1;
		if(!ma&&!mm)
		    ampap+=1;
		numberOfUtt+=1;
	    }
	}
	System.out.println("For instance: "+inst.getName());
	System.out.println("For the face tracks: "+numberOfTrack);
	System.out.println("meilleur modele, meilleure association: "+vmmma);
	System.out.println("meilleur modele, pas meilleure association: "+vmmap);
	System.out.println("pas meilleur model, meilleure association: "+vmpma);
	System.out.println("pas meilleur model, pas meilleure association: "+vmpap);
	System.out.println("For the utterances: "+numberOfUtt);
	System.out.println("meilleur modele, meilleure association: "+ammma);
	System.out.println("meilleur modele, association pire: "+ammap);
	System.out.println("model pire, meilleure association: "+ampma);
	System.out.println("model pire, association pire: "+ampap);
    }
    //public static getBestModel
}