package tool4Gmms;
import java.util.*;
import java.io.*;
import java.lang.Math;
import edu.umass.cs.mallet.base.pipe.iterator.AbstractPipeInputIterator;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.LabelAlphabet;
import edu.umass.cs.mallet.base.types.Labels;
import edu.umass.cs.mallet.base.types.LabelsSequence;
import edu.umass.cs.mallet.grmm.learning.ACRF;
import edu.umass.cs.mallet.grmm.learning.templates.TableTemplate;
import edu.umass.cs.mallet.grmm.learning.templates.UniquenessTemplate;
public class InstanceFactoryIdent  extends InstanceFactory {

    private HashMap<String,String> association;
    private	ArrayList<String> segmentNames=new ArrayList<String> ();
    private	ArrayList<String> labels= new ArrayList<String> ();
    private	ArrayList<Integer> segmentIdx =new ArrayList<Integer> ();
    private	ArrayList<String> types= new ArrayList<String> ();
    private	HashMap<String,Integer> segmentOrder = new HashMap<String,Integer>();
    private	HashMap<String,Integer> labelIdx;
    private int nAudio;
    private int nVid;
    private int nvModels;
    private double[][] pCostTable;
    private double[][] aVCostTable;
    private double[][] hsCostTable;
    private double[][] ladCostTable;
    private double[][] posCostTable;
    private double[][] aCostTable;
    private double[][] vCostTable;
    private double[][] vCostTableSift;
    private ACRF.Template[] templates;
    private ArrayList<String> shows;
    private String dir;
    private int nextShow;
    private int iterNumber=-1;

    public InstanceFactoryIdent(String dir,int iterNumber, ArrayList<String> shows, ACRF.Template[] templates) throws Exception{
    	super(dir, iterNumber, shows, templates);
		this.iterNumber=iterNumber;
		this.dir=dir;
		this.templates=templates;
		this.shows=shows;
		nextShow=0;
    }
    public InstanceRepere loadMapping(String mapFileName)throws Exception{
	segmentNames=new ArrayList<String> ();
	labels= new ArrayList<String> ();
	segmentIdx =new ArrayList<Integer> ();
	types= new ArrayList<String> ();
	segmentOrder = new HashMap<String,Integer>();
	labelIdx= new HashMap<String,Integer> ();
	String line, pattern="[ ]+";
	String[] tokens;
	BufferedReader in = new BufferedReader(new FileReader(mapFileName));
	int lIdx=0;
	while((line = in.readLine())!=null){
	    tokens=line.split(pattern);
	    labels.add(tokens[0]);
	    segmentNames.add(tokens[1]);
	    segmentIdx.add(new Integer((new Double(tokens[2])).intValue()));
	    segmentOrder.put(tokens[1],new Integer((new Double(tokens[2])).intValue()));
	    types.add(tokens[3]);
	    if(!labelIdx.containsKey(tokens[0])){
			labelIdx.put(tokens[0],new Integer(lIdx));
			lIdx+=1;
	    }
	}
	in.close();
	return new InstanceRepere(segmentNames,labels,segmentIdx,types,segmentOrder,labelIdx);
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
    public void setAVScore (String avcostFile, InstanceRepere carrier, String hungarianFile)throws Exception{
		String line;
		String pattern="[ ]+";
		String[] tokens;
		aVCostTable=new double[carrier.getNumUtterances()][carrier.getNumFaceTracks()];
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
    	return ocrr1;
    }
    private double[][] getShotInfo(String shotInfoFile, ArrayList<String> segmentNames) throws Exception{
    	double[][] shotInfo = new double[segmentNames.size()][1];
    	String line;
    	String pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(shotInfoFile));
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    if(segmentNames.contains(tokens[0]))
    	    	shotInfo[segmentNames.indexOf(tokens[0])][0]=(double)new Double(tokens[1]);
    	}
		return shotInfo;
	}
    
	private HashMap<String, ArrayList<String>> getShotInfo2(String shotInfoFile, ArrayList<String> segmentNames) throws Exception{
		HashMap<String, ArrayList<String>>  shotcarac= new HashMap<String, ArrayList<String>>();
    	String line;
    	String pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(shotInfoFile));
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    ArrayList<String> array = new ArrayList<String>();
    	    for(int i=1; i<tokens.length;i++)
    	    	array.add(tokens[i]);
    	    shotcarac.put(tokens[0], array);
    	}		
		return shotcarac;
	}
	private HashMap<String, HashMap<String, String>> getCaptions( String captionFile) throws Exception{
		HashMap<String, HashMap<String, String>>  captions = new HashMap<String, HashMap<String, String>>();
		String line;
    	String pattern="[ ]+";
    	String[] tokens;
    	BufferedReader in = new BufferedReader(new FileReader(captionFile));
    	while((line = in.readLine())!=null){
    	    tokens=line.split(pattern);
    	    HashMap<String,String> values= new HashMap<String,String>();
    	    for(int i=1; i<tokens.length;i++){
    	    	String[] namevalue=tokens[i].split(":");
    	    	values.put(namevalue[0], namevalue[1]);
    	    }
    	    captions.put(tokens[0], values);
    	    if(tokens[0].equals("S_00075_C_03")){
    	    	int a=0;
    	    }
    	}
		return captions;
	}
    public InstanceRepere nextInstance(){
	assert (nextShow != -1);
	InstanceRepere carrier = getInstanceForShow(nextShow);
	nextShow = getNextShow();
	return carrier;
    }
    public int getNextShow(){
	if(nextShow==(shows.size()-1))
	    return -1;
	else{
	    nextShow=nextShow+1;
	    return nextShow;
	}
    }
    public boolean hasNext (){
	return nextShow!=-1;
    }
    public InstanceRepere getInstanceForShow(int nextShow){
	String currentShow=shows.get(nextShow);
	String aScoreFile,vScoreFile, avScoreFile,pScoreFile,vSiftScoreFile,aScoreFileCohort,vScoreFileCohort,vSiftScoreFileCohort,featFile;
	InstanceRepere carrier=null;
	System.out.println("getting instance for show: "+currentShow);
	aScoreFile = dir+"/iter"+iterNumber+"/ascore/"+currentShow+".atable";
	vScoreFile = dir+"/iter"+iterNumber+"/vscore/"+currentShow+".vtable";
	vSiftScoreFile=dir+"/iter"+iterNumber+"/vscore/"+currentShow+".vsifttable";
	featFile=dir+"/"+currentShow+".feat";
	avScoreFile = dir+"/iter"+iterNumber+"/avscores/"+currentShow+".value4dec";
	pScoreFile = dir+"/video/distSurf/"+currentShow+".score";
	String aCaptionFile=dir+"/init/audio/"+currentShow+".aocr";
	String vCaptionFile=dir+"/init/video/"+currentShow+".vocr";
	String ocrr1File=dir+"/init/list/"+currentShow+".ocrr1";
	String shotInfoFile=dir+"/init/avcues/"+currentShow+".shottype";
	String captionFile=dir+"/init/avcues/"+currentShow+".ocr";
	String hungarianFile=dir+"/init/avcues/"+currentShow+".hung";
	String uniqFile = dir+"/"+currentShow+".uniq";
	try{
		carrier=loadMapping(dir+"/"+currentShow+".list");
		//addNoise(vCaptionFile,carrier);
		addNoise(featFile,carrier); //not really noise, just adding identities from the captions
	    carrier.setDir(this.dir);
	    carrier.setName(currentShow);
	    for(int i=0;i<templates.length;i++){
	    	if(templates[i] instanceof ACRF.FusionTemplate){
	    		System.out.println(avScoreFile);
	    		System.out.println(hungarianFile);
	    	    setAVScore(avScoreFile,carrier,hungarianFile);
	    	    carrier.dumpAVTable(iterNumber);
	    	}
	    }
	    for(int i=0;i<templates.length;i++){
	    	if(templates[i] instanceof ACRF.AcousticTemplate){
	    		System.out.println(aScoreFile);
	    		carrier.setACostTable(getVScore2(aScoreFile,carrier,0.46589,true));
	    		//setAScore2(aScoreFile,aScoreFileCohort, carrier);
		    	carrier.dumpATable(iterNumber);
	    	}
	    	if(templates[i] instanceof ACRF.VisualTemplateSift){
	    		System.out.println(vSiftScoreFile);
	    		carrier.setVSiftCostTable(getVScore2(vSiftScoreFile,carrier,-0.928285019239,false));
	    		carrier.dumpVTableSift(iterNumber);
	    	}
	    	if(templates[i] instanceof TableTemplate){
	    		System.out.println(featFile);
	    		HashMap<String,ArrayList<String>> features = new HashMap<String,ArrayList<String>>();
	    		carrier.setTables(getTables(featFile, features));
	    		carrier.setFeatures(features);
	    		carrier.dumpTables(iterNumber, currentShow+"-tables");
	    	}
	    	if(templates[i] instanceof UniquenessTemplate){	    		
				System.out.println(uniqFile);
	    		carrier.setUniqPairs(getUniq(uniqFile));
	    	}

	    }
	    carrier.setName(currentShow);
	    carrier.setDir(this.dir);

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
	public void dumpAVCostTable(String show)throws Exception{
		FileWriter fstream = new FileWriter(show+".avtable");
		BufferedWriter out = new BufferedWriter(fstream);
		out.close();
    }
    public void dumpAssocMatrix(ArrayList<String> speakerList, ArrayList<String> headList, double[][] costMatrix)throws Exception{
	FileWriter fstream = new FileWriter(dir+"/fusion/iter"+iterNumber+"/"+shows.get(nextShow)+".assocMatrix");
	BufferedWriter out = new BufferedWriter(fstream);
	out.write(speakerList.get(0));
	for (int i = 1; i < speakerList.size(); i++) {
	    out.write(" "+speakerList.get(i));
	}
	out.write("\n");
	out.write(headList.get(0));
	for (int i = 1; i < headList.size(); i++) {
	    out.write(" "+headList.get(i));
	}
	out.write("\n");
	for (int i = 0; i < speakerList.size(); i++) {
	    out.write( Double.toString(costMatrix[i][0]));
	    for (int j = 1; j < headList.size(); j++) {
		out.write( Double.toString(costMatrix[i][j]));
	    }
	    out.write("\n");
	}
	out.close();
    }
    public HashMap<String,String> getAssociation(){return association;}
    public double[][] getAtable(){return aCostTable;}
    public static void main(String args[]){
	double[][] q={{1,1,2,2},{4,5,6,7}};
	double[] a=q[0];
	//	System.out.println(Mean.evaluate(q[1],0,q.length));
    }
}
