package tool4Gmms;

import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.FeatureVector;
import java.util.*;
import java.io.*;
public class InstanceRepere extends Instance{
    private double[][] aVCostTable;
    private double[][] ladCostTable;
    private double[][] posCostTable;
    private double[][] aCostTable;
    private double[][] vCostTable;
    private double[][] vSiftCostTable;
    private HashSet<String> guestAr1;
    private String dir;
    private ArrayList<String> types;
    private ArrayList<String> segmentNames;
    private HashMap<String,ArrayList<String>> shotcarac;
    private HashMap<String,ArrayList<String>> features;
    private HashMap<String,double[][]> tables;
    private double[][] shots;
    private HashMap<String,Integer> segmentOrder;
    private HashMap<String,Integer> labelIdx;
    boolean locked = false;
    Pipe pipe;
    Object name;
	private HashMap<String, HashMap<String, String>> captions;
	//private HashMap<String, String> speaker2head;
	private ArrayList<ArrayList<String>> uniqPairs;
	private String outputFile;

    public void setPipe(Pipe p){
    	if (!locked){
    		if (p != null) {
    			p.pipe (this);
    			locked = true;
    		}
        	pipe = p;
    	}
    	else throw new IllegalStateException ("Instance is locked.");
    }
    public InstanceRepere(ArrayList<String> segmentNames,Object target,Object data,ArrayList<String> types,HashMap<String,Integer> segmentOrder,HashMap<String,Integer> labelIdx){
    	super(data, target, null, null);
    	this.types = types;
    	this.segmentNames = segmentNames;
    	this.labelIdx =labelIdx;
    	this.segmentOrder =segmentOrder;
    }
    public void dumpInstance(){
		System.out.println("-----Instance: "+getName()+"---------");
		ArrayList<Integer> segmentNumbers = (ArrayList<Integer>)this.getData();
		ArrayList<String> labels = (ArrayList<String>)this.getTarget();
		for(int i=0;i<types.size();i++)
		    System.out.println(segmentNames.get(i)+" "+types.get(i)+" "+segmentNumbers.get(i)+" "+labels.get(i));
    }
    public void dumpAVTable(int iterNumber)throws Exception{
		String show = (String)name;
		ArrayList<Integer> segmentNumbers=(ArrayList<Integer>)this.getData();
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+show+"-fusion.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
		    if(types.get(i).equals("utter")){
		    	int utterNumber =(int)segmentNumbers.get(i);
		    	for(int j=0;j<segmentNames.size();j++){
		    		if(types.get(j).equals("faceTrack")){
		    			int trackNumber =(int)segmentNumbers.get(j);
		    			double value=aVCostTable[utterNumber][trackNumber];
		    			out.write(segmentNames.get(i)+" "+segmentNames.get(j)+" "+value+"\n");
		    		}
		    	}
		    }
		}
		out.close();
    }
	public HashSet<String> getShotFeatureSet() {
		HashSet<String> featureset=new HashSet<String>();
		for(String segment : segmentNames)
			if(shotcarac.containsKey(segment))
				featureset.addAll(shotcarac.get(segment));
		return featureset;
	}

    public void dumpLadTable(int iterNumber)throws Exception{
		String show = (String)name;
		ArrayList<Integer> segmentNumbers=(ArrayList<Integer>)this.getData();
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+show+"-lad.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
		    if(types.get(i).equals("utter")){
		    	int utterNumber =(int)segmentNumbers.get(i);
		    	for(int j=0;j<segmentNames.size();j++){
		    		if(types.get(j).equals("faceTrack")){
		    			int trackNumber =(int)segmentNumbers.get(j);
		    			double value=ladCostTable[utterNumber][trackNumber];
		    			out.write(segmentNames.get(i)+" "+segmentNames.get(j)+" "+value+"\n");
		    		}
		    	}
		    }
		}
		out.close();
    }
    
    public void dumpATable(int iterNumber)throws Exception{
		ArrayList<Integer> segmentNumbers=(ArrayList<Integer>)this.getData();
		ArrayList<String> labels = (ArrayList<String>) this.getTarget();
		String show = (String)name;
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+show+"-audio.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
			if(types.get(i).equals("utter")){
				String track = segmentNames.get(i);
				int idxTrack = (int)segmentOrder.get(track);
				for (Map.Entry<String, Integer> entry : labelIdx.entrySet()){
					out.write(track+" "+entry.getKey()+" "+aCostTable[idxTrack][(int)entry.getValue()]+"\n");
				}
			}
		}
		out.close();
    }
    
    public void dumpVTableSift(int iterNumber)throws Exception{
		ArrayList<Integer> segmentNumbers=(ArrayList<Integer>)this.getData();
		ArrayList<String> labels = (ArrayList<String>) this.getTarget();
		String show = (String)name;
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+show+"-visualSift.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
			if(types.get(i).equals("faceTrack")){
				String track = segmentNames.get(i);
				int idxTrack = (int)segmentOrder.get(track);
				for (Map.Entry<String, Integer> entry : labelIdx.entrySet()){
					out.write(track+" "+entry.getKey()+" "+vSiftCostTable[idxTrack][(int)entry.getValue()]+"\n");
				}
			}
		}
		out.close();
    }

    public void dumpVCostTable(int iterNumber) throws Exception{
		ArrayList<Integer> segmentNumbers=(ArrayList<Integer>)this.getData();
		ArrayList<String> labels = (ArrayList<String>) this.getTarget();
		String show = (String)name;
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+show+"-visualDct.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
			if(types.get(i).equals("faceTrack")){
				String track = segmentNames.get(i);
				int idxTrack = (int)segmentOrder.get(track);
				for (Map.Entry<String, Integer> entry : labelIdx.entrySet()){
					out.write(track+" "+entry.getKey()+" "+vCostTable[idxTrack][(int)entry.getValue()]+"\n");
				}
			}
		}
		out.close();

    }
	public void dumpShotCarac(int iterNumber, String filename) throws Exception{
		String show = (String)name;
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+filename);
		BufferedWriter out = new BufferedWriter(fstream);
		for(String name : guestAr1)
			out.write(name+" ");
		out.write("\n");
		for (Map.Entry entry : shotcarac.entrySet()) {
			out.write(entry.getKey()+" ");
			for(String f : (ArrayList<String>)entry.getValue() )
				out.write(f+" ");
			out.write("\n");
		}
		out.close();
	}
	public void dumpCaptions(int iterNumber, String filename) throws IOException {
		String show = (String)name;
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+filename);
		BufferedWriter out = new BufferedWriter(fstream);
		for (Map.Entry entry : captions.entrySet()) {
			out.write(entry.getKey()+" ");
			for (Map.Entry namevalue : ((HashMap<String, ArrayList<String>>) entry.getValue()).entrySet()) 
				out.write(namevalue.getKey()+" "+namevalue.getValue()+" ");
			out.write("\n");
		}
		out.close();
	}

    public void dumpATable(int iterNumber, String filename, double[][] table, boolean audio)throws Exception{
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+filename);
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
			if(types.get(i).equals("utter") && audio){
				String track = segmentNames.get(i);
				int idxTrack = (int)segmentOrder.get(track);
				for (Map.Entry<String, Integer> entry : labelIdx.entrySet()){
					out.write(track+" "+entry.getKey()+" "+table[idxTrack][(int)entry.getValue()]+"\n");
				}
			}
			else
				if(types.get(i).equals("faceTrack") && !audio){
					String track = segmentNames.get(i);
					int idxTrack = (int)segmentOrder.get(track);
					for (Map.Entry<String, Integer> entry : labelIdx.entrySet()){
						out.write(track+" "+entry.getKey()+" "+table[idxTrack][(int)entry.getValue()]+"\n");
					}
				}
					
		}
		out.close();
    }
    
    public int getNumUtterances(){
	int nbreUt=0;
	for (String type : types)
	    if(type.equals("utter"))
		nbreUt+=1;
	return nbreUt;
    }
    public int getNumFaceTracks(){
	int nbreFt=0;
	for (String type : types)
	    if(type.equals("faceTrack"))
		nbreFt+=1;
	return nbreFt;
    }
    public double[][] getShots() {return shots;}
    public void setShots(double[][] shots ){this.shots=shots;}
    public HashSet<String> getGuestAR1() {return guestAr1;}
    public void setGuestAR1(HashSet<String> guestAr1){this.guestAr1=guestAr1;}
    public double[][] getAVCostTable(){return aVCostTable;}
    public void setAvCostTable(double[][] aVCostTable){this.aVCostTable=aVCostTable;}
    public double[][] getLadCostTable(){return ladCostTable;}
    public void setLadCostTable(double[][] ladCostTable){this.ladCostTable=ladCostTable;}
    public double[][] getPosCostTable(){return posCostTable;}
    public void setPosCostTable(double[][] posCostTable){this.posCostTable=posCostTable;}
    public double[][] getACostTable(){return aCostTable;}
    public void setACostTable(double[][] aCostTable){this.aCostTable=aCostTable;}
    public double[][] getVCostTable(){return vCostTable;}
    public void setVCostTable(double[][] vCostTable){this.vCostTable=vCostTable;}
    public double[][] getVSiftCostTable(){return vSiftCostTable;}
    public void setVSiftCostTable(double[][] vSiftCostTable){this.vSiftCostTable=vSiftCostTable;}
    public ArrayList<String> getTypes(){return types;}
    public ArrayList<String> getSegmentNames(){return segmentNames;}
    public HashMap<String,Integer> getSegmentOrder(){return segmentOrder;}
    public void setLabelIdx(HashMap<String,Integer> labelIdx){this.labelIdx=labelIdx;}
    public HashMap<String,Integer> getLabelIdx(){return labelIdx;}
    public Object getName(){return name;}
    public void setName(Object name){this.name= name;}
    public String getDir(){return dir;}
    public void setDir(String dir){this.dir=dir;}
    public Pipe getPipe(){return pipe;}
	public HashMap<String,ArrayList<String>> getShotcarac() {return shotcarac;}
	public void setShotcarac(HashMap<String,ArrayList<String>> shotcarac) {	this.shotcarac = shotcarac;	}
	public boolean useshot() {
		if(shotcarac!=null)
			return true;
		else
			return false;
	}
	public boolean usecaption() {
		if(captions!=null)
			return true;
		else
			return false;
	}
	public HashSet<String> getCaptionFeaturesSet() {
		HashSet<String> captionFeatureSet=new HashSet<String>();
		for(String segment : segmentNames)
			if(captions.containsKey(segment))
				for (Map.Entry entry : captions.get(segment).entrySet()) {
					captionFeatureSet.add((String) entry.getValue());
				}
		return captionFeatureSet;
		}
	public HashMap<String, HashMap<String, String>> getCaptions() {	return captions;}
	public void setCaptions(HashMap<String, HashMap<String, String>> captions) {	this.captions=captions;}
	//public HashMap<String, String> getSpeaker2Head() { return speaker2head;	}
	//public void setSpeaker2head(HashMap<String, String> s2h){ this.speaker2head=s2h;}
	public HashMap<String,double[][]> getTables() {
		return tables;
	}
	public void setTables(HashMap<String,double[][]> tables) {
		this.tables = tables;
	}
	public HashMap<String,ArrayList<String>> getFeatures() {
		return features;
	}
	public void setFeatures(HashMap<String,ArrayList<String>> features) {
		this.features = features;
	}
	public boolean useTable() {
		if(features!=null)
			return true;
		else
			return false;
	}
	public Collection<String> getFeaturesSet() {
		HashSet<String> featureset=new HashSet<String>();
		for (Map.Entry<String,double[][]> entry : tables.entrySet())
			featureset.add(entry.getKey());
		return featureset;
	}
	public void dumpTables(int iterNumber, String filename) throws IOException {
		String show = (String)name;
		String outdirname=this.dir+"/iter"+iterNumber;
		File outputdir=new File(outdirname);
		if(!outputdir.exists())
		    outputdir.mkdir();
		FileWriter fstream = new FileWriter(outdirname+"/"+filename);
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
			String track = segmentNames.get(i);
			if(!features.containsKey(track))
				continue;
			for (Map.Entry<String, Integer> label : labelIdx.entrySet()){
				out.write(track+" ");
				out.write(label.getKey()+" ");
				for (String feat : features.get(track)) {
					out.write(feat+":");
					double [][] tab=tables.get(feat);
					out.write(new Double(tab[i][label.getValue()]).toString()+" ");
				}
				out.write("\n");
			}
		}
		out.close();		
	}
	public void setUniqPairs(ArrayList<ArrayList<String>> arrayList) { this.uniqPairs=arrayList;	}
	public ArrayList<ArrayList<String>> getUniqPairs() {	return uniqPairs; }
	public String getoutputFile() {	return outputFile;	}
}
	
