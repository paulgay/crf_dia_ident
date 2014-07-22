package tool4Gmms;

import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.FeatureVector;

import java.util.*;
import java.io.*;
public class InstanceRepere extends Instance{
    private String dir;
    private ArrayList<String> segmentNames;
    private HashMap<String,ArrayList<String>> features;
    private HashMap<String,double[][]> unaries, pairWise, uniq;
    private double[][] shots;
    private HashMap<String,Integer> labelIdx;
    boolean locked = false;
    Pipe pipe;
    Object name;
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
    public InstanceRepere(ArrayList<String> segmentNames, Object target,HashMap<String,Integer> labelIdx){
    	super(null, target, null, null);
    	this.segmentNames = segmentNames;
    	this.labelIdx =labelIdx;
    	unaries=null;
    	pairWise=null;
    	uniq=null;
    }
    public void dumpInstance(){
		System.out.println("-----Instance: "+getName()+"---------");
		ArrayList<Integer> segmentNumbers = (ArrayList<Integer>)this.getData();
		ArrayList<String> labels = (ArrayList<String>)this.getTarget();
		for(int i=0;i<segmentNames.size();i++)
		    System.out.println(segmentNames.get(i)+" "+labels.get(i));
    }
    

    public double[][] getShots() {return shots;}
    public ArrayList<String> getSegmentNames(){return segmentNames;}
    public void setLabelIdx(HashMap<String,Integer> labelIdx){this.labelIdx=labelIdx;}
    public HashMap<String,Integer> getLabelIdx(){return labelIdx;}
    public Object getName(){return name;}
    public void setName(Object name){this.name= name;}
    public String getDir(){return dir;}
    public void setDir(String dir){this.dir=dir;}
    public Pipe getPipe(){return pipe;}
	public HashMap<String,double[][]> getUnaries() {return unaries;	}
	public HashMap<String,double[][]> getPairWise() {return pairWise;	}
	public void setUniqPairs(ArrayList<ArrayList<String>> arrayList) { this.uniqPairs=arrayList;	}
	public ArrayList<ArrayList<String>> getUniqPairs() {	return uniqPairs; }
	public String getoutputFile() {	return outputFile;	}	
	public void addUnaries(HashMap<String,double[][]> unaries){
		if(this.unaries==null)
			this.unaries=unaries;
		else{
			for(Map.Entry<String, double[][]> entry : unaries.entrySet()){
				String key = entry.getKey();
				double[][] value = entry.getValue();
				this.unaries.put(key, value);
			}
		}
	}
	public void addPairWise(HashMap<String,double[][]> pairWise){
		if(this.pairWise==null)
			this.pairWise=pairWise;
		else{
			for(Map.Entry<String, double[][]> entry : pairWise.entrySet()){
				String key = entry.getKey();
				double[][] value = entry.getValue();
				pairWise.put(key, value);
			}
		}
	}
	
	public HashMap<String,ArrayList<String>> getFeatures() {
		return features;
	}
	public void setFeatures(HashMap<String,ArrayList<String>> features) {
		this.features = features;
	}
	public boolean useUnary() {
		if(unaries!=null)
			return true;
		else
			return false;
	}
	public boolean usePairWise() {
		if(pairWise!=null)
			return true;
		else
			return false;
	}
	public Collection<String> getUnaryFeaturesSet() {
		HashSet<String> featureset=new HashSet<String>();
		for (Map.Entry<String,double[][]> entry : unaries.entrySet())
			featureset.add(entry.getKey());
		return featureset;
	}
	public Collection<String> getPairWiseFeaturesSet() {
		HashSet<String> featureset=new HashSet<String>();
		for (Map.Entry<String,double[][]> entry : pairWise.entrySet())
			featureset.add(entry.getKey());
		return featureset;
	}
	public void dumpPairWise(String filename) throws IOException {
		File file=new File(filename);
		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
			for(int j=0;j<segmentNames.size();j++){
				for(Map.Entry<String, double[][]> entry : pairWise.entrySet()){
					double[][] table = entry.getValue();
					double value=table[i][j];
					if(value!=0){
						String seg1 = segmentNames.get(i);
						String seg2 = segmentNames.get(j);
						out.write(seg1+" "+seg2+" "+value+"\n");
					}
				}
			}
		}
		out.close();
	}
	public void dumpUnaries(String filename) throws IOException {
		File file=new File(filename);
		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i=0;i<segmentNames.size();i++){
			for (Map.Entry<String, Integer> label : labelIdx.entrySet()){
				for(Map.Entry<String, double[][]> entry : pairWise.entrySet()){
					double[][] table = entry.getValue();
					double value=table[i][label.getValue()];
					if(value!=0){
						String seg1 = segmentNames.get(i);
						out.write(seg1+" "+label.getKey()+" "+value+"\n");
					}
				}
			}
		}
		out.close();		
	}
	public void dumpUniq(String filename) throws IOException {
		File file=new File(filename);
		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		for(ArrayList<String> pair : uniqPairs){
			out.write(pair.get(0)+" "+pair.get(1)+"\n");
		}
	}
}
	
