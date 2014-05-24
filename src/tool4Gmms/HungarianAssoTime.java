package tool4Gmms;
import java.util.*;
import java.io.*;

import www.spatial.maine.edu.assignment.HungarianAlgorithm;

public class HungarianAssoTime extends HungarianAsso{
/*
	private HashMap<String,String> association;
	private	ArrayList<String> segmentNames;
	private	ArrayList<String> labels;
	private	ArrayList<Integer> segmentIdx;
	private	ArrayList<String> types;
	private	HashMap<String,Integer> segmentOrder;
	private	HashMap<String,Integer> labelIdx;
	private double[][] pCostTable;
	private double[][] aVCostTable;
*/
    public void setAVScore (String avcostFile)throws Exception{
		String line;
		String pattern="[ ]+";
		String[] tokens;
		double[][] aVCostTable=new double[getNumUtterances()][getNumFaceTracks()];
		HashMap<String,Integer> segmentOrder=getSegmentOrder();
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
		    double value = Double.parseDouble(tokens[14]); //Math.min(Double.parseDouble(tokens[5]),Double.parseDouble(tokens[7]))-Math.max(Double.parseDouble(tokens[4]),Double.parseDouble(tokens[6]));
		    aVCostTable[uttIdx][trackIdx]=value;
		}
		super.setAvCostTable(aVCostTable);
    }
    public void writeScoreClus (String outfile)throws Exception{
    	HashMap<String,String> association = getAssociation();
    	ArrayList<String> types = getTypes();
    	ArrayList<String> labels = getLabels();
    	association=new HashMap<String,String>();
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
    	// Start by assigning a great value to each entry (worst value)
    	BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
    	for (int i = 0; i < speakerList.size(); i++) {
    	    for (int j = 0; j < headList.size(); j++) {
    		    String speakerName = speakerList.get(i);
    		    String headName = headList.get(j);
    		    double value = match(speakerName,headName);
    		    out.write(speakerName+" "+headName+" "+value+"\n");
    		}
    	}
    	out.close();
    }
	public static void main(String args[]){
		String mapping =args[0];
		String avcostFile= args[1];
		String outfile = args[2];
		HungarianAssoTime ha = new HungarianAssoTime();
		try{
			ha.loadMapping(mapping);
			ha.setAVScore(avcostFile);
			ha.writeScoreClus(outfile);
			//ha.makeAssociation();
			//ha.writeMapping(outfile);
		}
		catch (Exception e) {
		    e.printStackTrace();
		    System.exit(0);
		}
	}
}
