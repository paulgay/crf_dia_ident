package tool4Gmms;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import edu.umass.cs.mallet.base.pipe.iterator.AbstractPipeInputIterator;
import edu.umass.cs.mallet.grmm.learning.ACRF;
import edu.umass.cs.mallet.grmm.learning.templates.TableTemplate;
import edu.umass.cs.mallet.grmm.learning.templates.VisualTemplateDct;
import edu.umass.cs.mallet.grmm.learning.templates.UniquenessTemplate;
public class InstanceFactory  extends AbstractPipeInputIterator{

    private HashMap<String,String> association;
    private	ArrayList<String> segmentNames=new ArrayList<String> ();
    private	ArrayList<String> labels= new ArrayList<String> ();
    private	ArrayList<Integer> segmentIdx =new ArrayList<Integer> ();
    private	ArrayList<String> types= new ArrayList<String> ();
    private	HashMap<String,Integer> segmentOrder = new HashMap<String,Integer>();
    private	HashMap<String,Integer> labelIdx;
    private double[][] aVCostTable;
    private double[][] aCostTable;
    private ACRF.Template[] templates;
    private ArrayList<String> shows;
    private String dir;
    private int nextShow;
    private int iterNumber=-1;
    private     HashMap<String,String> lists; //show listFile
    private     HashMap<String,String> uniqConstraints; //show listFile
    private     HashMap<String,ArrayList<String>> unaries; //show, set of filename with unary features
    private     HashMap<String,ArrayList<String>> pairs; //show, set of filename with pairwise features
	private Iterator<Entry<String, String>> it;
	private String logdir="";
	private String outputDir;
    public InstanceFactory(String configfile)throws Exception{
    	String line, pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(configfile));
    	String show="";
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    switch (new Integer(tokens[0])) {
    	    case 0: show=tokens[1];
    	    			break;
    	    case 1: lists.put(show, tokens[1]);
    	    				break;
    	    case 2: if(!unaries.containsKey(show))
    	    	unaries.put(show, new ArrayList<String>());
    	    	ArrayList<String> unariesThisShow = unaries.get(show);
    	    	unariesThisShow.add(tokens[1]);
    	    break;
    	    case 3 :if(!pairs.containsKey(show))
    	    	pairs.put(show, new ArrayList<String>());
    	    	ArrayList<String> pairsThisShow = pairs.get(show);
    	    	pairsThisShow.add(tokens[1]);
    	    break;
    	    case 4:uniqConstraints.put(show, tokens[1]);
    	    break;
    	    case 5:outputDir=tokens[1];
    	    break;
    	    case 6:logdir=tokens[1];
    	    break;
    	    default: break;
    	    }
    	}
    	in.close();
		it=lists.entrySet().iterator();
    }
    public InstanceRepere loadMapping(String show)throws Exception{
    	segmentNames=new ArrayList<String> ();
    	labels= new ArrayList<String> ();
    	labelIdx= new HashMap<String,Integer> ();
    	String line, pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(lists.get(show)));
    	int lIdx=0,sIdx=0;
    	while((line = in.readLine())!=null){
    		tokens=line.split(pattern);
    		labels.add(tokens[0]);
    		segmentNames.add(tokens[1]);
    		segmentOrder.put(tokens[0], new Integer(sIdx));
    		sIdx+=1;
    		if(!labelIdx.containsKey(tokens[0])){
    			labelIdx.put(tokens[0],new Integer(lIdx));
    			lIdx+=1;
    		}
    	}
    	in.close();
    	if(unaries.containsKey(show)){
    		for(String file: unaries.get(show)){
				in = new BufferedReader(new FileReader(file));
    			while((line = in.readLine())!=null){
    	    		tokens=line.split(pattern);
    	    		segmentNames.add(tokens[1]);
    	    		if(!labelIdx.containsKey(tokens[0])){
    	    			labelIdx.put(tokens[0],new Integer(lIdx));
    	    			lIdx+=1;
    	    		}
    	    	}   
    	    	in.close();
    		}
    	}
    	return new InstanceRepere(segmentNames,segmentOrder, labels,labelIdx);
    }
    public void addNoise(String aScoreFile, InstanceRepere carrier) throws Exception{
    	String line, pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(aScoreFile));
    	int lIdx=labelIdx.size();
    	while((line = in.readLine())!=null){
    		tokens=line.split(pattern);
    		if(!labelIdx.containsKey(tokens[1])){
    			labelIdx.put(tokens[1],new Integer(lIdx));
    			lIdx+=1;
    	    }
    	}
    	in.close();
    }
    public void setAVScore (String avcostFile, InstanceRepere carrier)throws Exception{
		String line;
		String pattern="[ ]+";
		String[] tokens;
		aVCostTable=new double[carrier.getSegmentNames().size()][carrier.getSegmentNames().size()];
		for(int i=0;i<aVCostTable.length;i++)
		    for(int j=0;j<aVCostTable[0].length;j++){
		    	aVCostTable[i][j]=-9999;
		    }
		BufferedReader in = new BufferedReader(new FileReader(avcostFile));
		while((line = in.readLine())!=null){
		    tokens=line.split(pattern);
		    String utterName=tokens[1];
		    int uttIdx=(int)segmentOrder.get(utterName);
		    String trackName=tokens[0];
		    if(!segmentOrder.containsKey(trackName))
		    	continue;
		    int trackIdx = (int)segmentOrder.get(trackName);
		    double value;
	    	value = Double.parseDouble(tokens[9]);
		    aVCostTable[uttIdx][trackIdx]=value;	
		    
		}
		in.close();
		carrier.setAvCostTable(aVCostTable);
		/*HashMap<String, String> s2h = new HashMap<String, String>();
		in = new BufferedReader(new FileReader(hungarianFile));
		while((line = in.readLine())!=null){
		    tokens=line.split(pattern);
		    s2h.put(tokens[0], tokens[1]);
		}
		carrier.setSpeaker2head(s2h);
		in.close();*/
    }

    public double[][] getVScore2(String vcostFile, InstanceRepere carrier, double neutral, boolean audio)throws Exception{
    	double[][] vCostTable; 
    	if(audio)
    		vCostTable = new double[carrier.getNumUtterances()][labelIdx.size()];
    	else
    		vCostTable = new double[carrier.getNumFaceTracks()][labelIdx.size()];
	for(int i=0;i<vCostTable.length;i++)
	    for(int j=0;j<vCostTable[0].length;j++)
	    	vCostTable[i][j]=neutral;
	String line;
	String pattern="[ ]+";
	String[] tokens;
	BufferedReader in = new BufferedReader(new FileReader(vcostFile));
	while((line = in.readLine())!=null){
	    tokens=line.split(pattern);
	    String trackName=tokens[0];
	    String clusName=tokens[1];
	    double value=(double)(new Double(tokens[2]));
	    int idxLabel = labelIdx.get(clusName);
	    if(!segmentOrder.containsKey(trackName))
	    	continue;	    
	    int idxTrack = segmentOrder.get(trackName);
	    vCostTable[idxTrack][idxLabel]=value;
	}
	in.close();
	return vCostTable;
    }

    public HashSet<String> getOcrR1(String ocrr1File)throws Exception{
    	HashSet<String>  ocrr1= new HashSet<String> ();
    	String line;
    	String pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(ocrr1File));
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    ocrr1.add(tokens[0]);
    	}    	
    	in.close();
    	return ocrr1;
    }
    public InstanceRepere nextInstance(){
    	Map.Entry pairs = (Map.Entry)it.next();
	InstanceRepere carrier = getInstanceForShow((String) pairs.getKey());
	return carrier;
    }
    public boolean hasNext (){
    	return it.hasNext();
    }
    public InstanceRepere getInstanceForShow(String currentShow){
    	InstanceRepere carrier=null;
    	System.out.println("getting instance for show: "+currentShow);
	try{
		carrier=loadMapping(currentShow);
	    carrier.setName(currentShow);
	    if(unaries.containsKey(currentShow)){
	    	for(String file: unaries.get(currentShow)){
	    		System.out.println(file);
	    		HashMap<String,ArrayList<String>> features = new HashMap<String,ArrayList<String>>();
	    		carrier.setTables(getTables(file, features));
	    		carrier.setFeatures(features);
	    	}
	    	if(!logdir.equals("")){
	    		carrier.dumpTables(logdir+"/"+currentShow+"-tables");	    		
	    	}
	    }
	    if(pairs.containsKey(currentShow)){
	    	for(String file : pairs.get(currentShow)){
	    		System.out.println(file);
	    		setAVScore(file,carrier);
		    	if(!logdir.equals("")){
		    		carrier.dumpAVTable(logdir+"/"+currentShow+"-avasso");	    		
		    	}
	    	}
	    }
	    if(uniqConstraints.containsKey(currentShow)){
			System.out.println(uniqConstraints.get(currentShow));
	    	carrier.setUniqPairs(getUniq(uniqConstraints.get(currentShow)));
	    }
	    carrier.setName(currentShow);
	    //carrier.dumpPTable();
	}
	catch(Exception e){e.printStackTrace();}
	return carrier;
    }
	
private ArrayList<ArrayList<String>> getUniq(String uniqFile) throws IOException {
		String line;
		String pattern="[ ]+";
		String[] tokens;
		BufferedReader in = new BufferedReader(new FileReader(uniqFile));
		ArrayList<ArrayList<String>> uniqPairs = new ArrayList<ArrayList<String>> ();
		while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    String t1=tokens[0];
    	    String t2=tokens[1];
    	    ArrayList<String> a= new ArrayList<String>();
    	    a.add(t1);a.add(t2);
    	    uniqPairs.add(a);
		}
		in.close();
		return uniqPairs;
	}
		private HashMap<String, double[][]> getTables(String featFile, HashMap<String,ArrayList<String>> featuresBySegment) throws NumberFormatException, IOException {
		HashMap<String, double[][]> tables =new HashMap<String, double[][]>();
		String line;
    	String pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(featFile));
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    String segment=tokens[0];
    	    String cluster=tokens[1];
    	    int segidx=segmentNames.indexOf(segment);
    	    if(segidx==-1)
    	    	continue;
    	    int clusidx=labelIdx.get(cluster);
    	    for(int i=2; i<tokens.length;i++){
    	    	ArrayList<String> features;
    	    	if(featuresBySegment.containsKey(segment))
    	    		features=featuresBySegment.get(segment);
    	    	else{
    	    		features=new ArrayList<String>();
    	    		featuresBySegment.put(segment, features);
    	    	}
    	    	String[] namevalue=tokens[i].split(":");
    	    	if(!features.contains(namevalue[0]))
    	    		features.add(namevalue[0]);
    	    	if(!tables.containsKey(namevalue[0])){
    	    		double[][] table = new double[segmentNames.size()][labelIdx.size()];
    	    		table[segidx][clusidx]= (double)Double.parseDouble(namevalue[1]);
    	    		tables.put(namevalue[0], table);
    	    	}
    	    	else{
    	    		double[][] table= tables.get(namevalue[0]);
    	    		table[segidx][clusidx]=(double)Double.parseDouble(namevalue[1]);
    	    	}
    	    }
    	}
    	in.close();
		return tables;
	}
    public double[][] getAtable(){return aCostTable;}
    public static void main(String args[]){
	double[][] q={{1,1,2,2},{4,5,6,7}};
    }
	public String getoutputDir() {
		return outputDir;
	}
}
