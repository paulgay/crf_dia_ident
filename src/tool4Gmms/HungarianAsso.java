package tool4Gmms;
import java.util.*;
import java.io.*;
import www.spatial.maine.edu.assignment.HungarianAlgorithm;

public class HungarianAsso {

	private HashMap<String,String> association;
	private	ArrayList<String> segmentNames;
	private	ArrayList<String> labels;
	private	ArrayList<Integer> segmentIdx;
	private	ArrayList<String> types;
	private	HashMap<String,Integer> segmentOrder;
	private	HashMap<String,Integer> labelIdx;
	private double[][] aVCostTable;

	public void loadMapping(String mapFileName)throws Exception{
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
    public void setAVScore (String avcostFile)throws Exception{
		String line;
		String pattern="[ ]+";
		String[] tokens;
		aVCostTable=new double[getNumUtterances()][getNumFaceTracks()];
		for(int i=0;i<aVCostTable.length;i++)
		    for(int j=0;j<aVCostTable[0].length;j++){
		    	aVCostTable[i][j]=-9999;
		    }
		BufferedReader in = new BufferedReader(new FileReader(avcostFile));
		while((line = in.readLine())!=null){
		    tokens=line.split(pattern);
		    String utterName=tokens[2];
		    int uttIdx=(int)segmentOrder.get(utterName);
		    String trackName=tokens[3];
		    int trackIdx = (int)segmentOrder.get(trackName);
		    double value = Double.parseDouble(tokens[14]);
		    aVCostTable[uttIdx][trackIdx]=value;
		}
    }
    public void makeAssociation ()throws Exception{
    	this.association=new HashMap<String,String>();
    	int nbSet = 0;
    	ArrayList<String> speakerList = new ArrayList<String>();
    	ArrayList<String> headList = new ArrayList<String>();
    	for(int i=0;i<types.size();i++){
    	    if(types.get(i).equals("faceTrack")){
    		if(!headList.contains(labels.get(i)))
    		    headList.add(labels.get(i));
    	    }
    	    else
    		if(!speakerList.contains(labels.get(i)))
    		    speakerList.add(labels.get(i));
    	}
    	int max = Math.max(speakerList.size(),headList.size());
    	double[][] costMatrix = new double[max][max];
    	// Start by assigning a great value to each entry (worst value)
    	for (int i = 0; i < max; i++) {
    	    for (int j = 0; j < max; j++) {
    		if(i<speakerList.size()&&j<headList.size()){
    		    String speakerName = speakerList.get(i);
    		    String headName = headList.get(j);
    		    costMatrix[i][j] = match(speakerName,headName);
    		}
    		else
    		    costMatrix[i][j]=0;//fake value to have a square matrix
    	    }
    	}
    	//dumpAssocMatrix(speakerList, headList, costMatrix);
    	if (costMatrix.length > 0) {
    	    String sumType = "max";
    	    int[][] assignment = new int[costMatrix.length][2];
    	    String speaker,head;
    	    assignment = HungarianAlgorithm.hgAlgorithm(costMatrix, sumType); // Call Hungarian algorithm.
    	    for (int i = 0; i < assignment.length; i++) {
	    		if (costMatrix[assignment[i][0]][assignment[i][1]] > 0) {
	    		    speaker = speakerList.get(assignment[i][0]);
	    		    head = headList.get(assignment[i][1]);
	    		    association.put(speaker,head+"_"+speaker);
	    		    association.put(head,head+"_"+speaker);
	    		}
    	    }
    	}
    }
    public double match(String speaker, String head)throws Exception{
    	double score =0;
    	Boolean overlap=false;
    	for (int i=0;i<segmentNames.size();i++) {// I could loop less
    	    if (speaker.equals(labels.get(i))) {
    		if(types.get(i).equals("faceTrack"))
    		    throw new Exception("face track in a speaker");
    		int uttIdx = (int)segmentOrder.get(segmentNames.get(i));
    		for (int j=i;j<segmentNames.size();j++) {
    		    if(head.equals(labels.get(j))){
	    			if(types.get(j).equals("utter"))
	    			    throw new Exception("utterance in a head");
	    			int trackIdx=segmentOrder.get(segmentNames.get(j));
	    			double localScore=aVCostTable[uttIdx][trackIdx];
	    			if(localScore!=-9999){
	    				overlap=true;
	    				score=score+localScore;
//	    				if(speaker.equals("S12")){
//	    					System.out.println(head+" "+segmentNames.get(j)+" "+segmentNames.get(i)+" "+localScore+" "+score);
//	    				}
	    			}
    		    }
    		}
    	    }
    	}
    	if(overlap)
    	    return score;
    	else
    	    return 0;//We forbid the association if there is no temporal overlap.
        }
	public void writeMapping(String mapFileName)throws Exception{
    	BufferedWriter out = new BufferedWriter(new FileWriter(mapFileName));
    	for(int i=0;i<labels.size();i++)
    		if(association.containsKey(labels.get(i)))
    			out.write(association.get(labels.get(i))+" "+segmentNames.get(i)+" "+segmentOrder.get(segmentNames.get(i))+" "+types.get(i)+" "+labels.get(i)+"\n");
    		else
    			out.write(labels.get(i)+" "+segmentNames.get(i)+" "+segmentOrder.get(segmentNames.get(i))+" "+types.get(i)+" "+labels.get(i)+"\n");
    	out.close();
	}	
    public double[][] getAVCostTable(){return aVCostTable;}
    public void setAvCostTable(double[][] aVCostTable){this.aVCostTable=aVCostTable;}
    public ArrayList<String> getTypes(){return types;}
    public ArrayList<String> getSegmentNames(){return segmentNames;}
    public HashMap<String,Integer> getSegmentOrder(){return segmentOrder;}
    public void setLabelIdx(HashMap<String,Integer> labelIdx){this.labelIdx=labelIdx;}
    public HashMap<String,Integer> getLabelIdx(){return labelIdx;}
    public HashMap<String,String> getAssociation(){return association;}
    public void setAssociation(HashMap<String,String> asso){association=asso;}
    public ArrayList<String> getLabels(){ return labels;}
	public static void main(String args[]){
		String mapping =args[0];
		String avcostFile= args[1];
		String outfile = args[2];
		HungarianAsso ha = new HungarianAsso();
		try{
			ha.loadMapping(mapping);
			ha.setAVScore(avcostFile);
			ha.makeAssociation();
			ha.writeMapping(outfile);
		}
		catch (Exception e) {
		    e.printStackTrace();
		    System.exit(0);
		}
	}
}
