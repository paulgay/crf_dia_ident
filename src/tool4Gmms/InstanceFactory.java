package tool4Gmms;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import edu.umass.cs.mallet.base.pipe.iterator.AbstractPipeInputIterator;
import edu.umass.cs.mallet.grmm.learning.ACRF;
import edu.umass.cs.mallet.grmm.learning.templates.UnaryTemplate;
import edu.umass.cs.mallet.grmm.learning.templates.UniquenessTemplate;
public class InstanceFactory  extends AbstractPipeInputIterator{

    private	ArrayList<String> segmentNames=new ArrayList<String> ();
    private	ArrayList<String> labels= new ArrayList<String> ();
    private	ArrayList<Integer> segmentIdx =new ArrayList<Integer> ();
    private	ArrayList<String> types= new ArrayList<String> ();
    private	HashMap<String,Integer> labelIdx;
    private double[][] aCostTable;
    private     HashMap<String,String> lists,hungFiles; //show listFile
    private     HashMap<String,String> uniqConstraints; //show listFile
    private     HashMap<String,ArrayList<String>> unaries, pairs; //show, set of filename with unary features
	private Iterator<Entry<String, String>> it;
	private String logdir="";
	private String outputDir;
    public InstanceFactory(String configfile)throws Exception{
    	String line, pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(configfile));
    	String show="";
    	lists=new HashMap<String,String>();
    	hungFiles=new HashMap<String,String>();
    	unaries= new HashMap<String,ArrayList<String>>();
    	pairs = new HashMap<String,ArrayList<String>>();
    	uniqConstraints=new HashMap<String,String>(); 
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
    	    case 7:hungFiles.put(show,tokens[1]);
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
    	int lIdx=0;
    	while((line = in.readLine())!=null){
    		tokens=line.split(pattern);
    		labels.add(tokens[0]);
    		
    		segmentNames.add(tokens[1]);
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
    	    		//segmentNames.add(tokens[1]);
    	    		if(!labelIdx.containsKey(tokens[2])){
    	    			labelIdx.put(tokens[2],new Integer(lIdx));
    	    			lIdx+=1;
    	    		}
    	    	}   
    	    	in.close();
    		}
    	}
    	return new InstanceRepere(segmentNames, labels,labelIdx);
    }
    public HashMap<String, double[][]>  parsePairWise(String featFile)throws Exception{
		HashMap<String, double[][]> tables =new HashMap<String, double[][]>();
		String line;
    	String pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(featFile));
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    String type=tokens[0];
		    String utterName=tokens[1];
		    String trackName=tokens[2];
		    if(!segmentNames.contains(trackName)){
		    	System.out.println("Skipping segment "+trackName+" not appearing in segment list");
		    	continue;
		    }
		    if(!segmentNames.contains(utterName)){
		    	System.out.println("Skipping segment "+utterName+" not appearing in segment list");
		    	continue;
		    }
		    int uttIdx=segmentNames.indexOf(utterName);
		    int trackIdx = segmentNames.indexOf(trackName);
	    	double value = Double.parseDouble(tokens[3]);
    	    if(!tables.containsKey(type)){
    	    	double[][] table = new double[segmentNames.size()][segmentNames.size()];
    	    	for(int j=0;j<table.length;j++)
    	    		for(int k=0;k<table[0].length;k++){
    	    		    table[j][k]=0;
    	    		}
    	    	table[uttIdx][trackIdx]= value;
    	    	tables.put(type, table);
    	    }
    	    else{
    	    	double[][] table= tables.get(type);
    	    	table[uttIdx][trackIdx]=value;
    	    }
		    
		}
		in.close();
		return tables;
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
    private HashSet<String> getHungAsso(String hungFile) throws IOException {
    	HashSet<String> hungLabels=new HashSet<String>();
		String line;
		String pattern="[ ]+";
		String[] tokens;
		BufferedReader in = new BufferedReader(new FileReader(hungFile));
		while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    hungLabels.add(tokens[0]);
		}
		in.close();
    	
		return hungLabels;
	}

    private HashMap<String, double[][]> parseUnaries(String featFile) throws NumberFormatException, IOException {
		HashMap<String, double[][]> tables =new HashMap<String, double[][]>();
		String line;
    	String pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(featFile));
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    String type=tokens[0];
    	    String segment=tokens[1];
    	    String cluster=tokens[2];
    	    double value=(double)Double.parseDouble(tokens[3]);
    	    int segidx=segmentNames.indexOf(segment);
    	    if(segidx==-1)
    	    	continue;
    	    int clusidx=labelIdx.get(cluster);
    	    if(!tables.containsKey(type)){
    	    	double[][] table = new double[segmentNames.size()][labelIdx.size()];
    	    	for(int j=0;j<table.length;j++)
    	    		for(int k=0;k<table[0].length;k++){
    	    		    table[j][k]=0;
    	    		}
    	    	table[segidx][clusidx]= value;
    	    	tables.put(type, table);
    	    }
    	    else{
    	    	double[][] table= tables.get(type);
    	    	table[segidx][clusidx]=value;
    	    }
    	}
    	in.close();
		return tables;
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
	    		System.out.println("parsing file: "+file);
	    		HashMap<String,ArrayList<String>> features = new HashMap<String,ArrayList<String>>();
	    		carrier.addUnaries(parseUnaries(file));
	    		carrier.setFeatures(features);
	    	}
	    	if(!logdir.equals("")){
	    		carrier.dumpUnaries(logdir+"/"+currentShow+"-tables");	    		
	    	}
	    }
	    if(pairs.containsKey(currentShow)){
	    	for(String file : pairs.get(currentShow)){
	    		System.out.println("parsing file: "+file);
	    		carrier.addPairWise(parsePairWise(file));
		    	if(!logdir.equals("")){
		    		carrier.dumpPairWise(logdir+"/"+currentShow+"-avasso");	    		
		    	}
	    	}
	    }
	    if(uniqConstraints.containsKey(currentShow)){
			System.out.println("parsing file: "+uniqConstraints.get(currentShow));
	    	carrier.setUniqPairs(getUniq(uniqConstraints.get(currentShow)));
	    	if(!logdir.equals("")){
	    		carrier.dumpUniq(logdir+"/"+currentShow+"-uniq");	    		
	    	}
	    if(hungFiles.containsKey(currentShow)){
	    	System.out.println("parsing file: "+hungFiles.get(currentShow));
	    	carrier.setHungLabels(getHungAsso(hungFiles.get(currentShow)));
	    }

	    }
	    carrier.setName(currentShow);
	    //carrier.dumpPTable();
	}
	catch(Exception e){e.printStackTrace();}
	return carrier;
    }
	
	public double[][] getAtable(){return aCostTable;}
    public static void main(String args[]){
    }
	public String getoutputDir() {
		return outputDir;
	}
}
