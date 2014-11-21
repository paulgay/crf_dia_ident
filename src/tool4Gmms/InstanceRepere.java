package tool4Gmms;

import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.FeatureVector;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Pseudograph;
import org.jgrapht.alg.BronKerboschCliqueFinder;

import java.util.*;
import java.io.*;
public class InstanceRepere extends Instance{
    private String dir;
    private ArrayList<String> segmentNames;
    private HashMap<String,ArrayList<String>> features;
    private HashMap<String,double[][]> unaries, pairWise;
    private double[][] shots;
    private HashMap<String,Integer> labelIdx;
    boolean locked = false;
    Pipe pipe;
    Object name;
	private ArrayList<ArrayList<String>> uniqPairs, uniqPairori;
	private String outputFile;
	private HashSet<String> hungLabels;
	Pseudograph<String,DefaultEdge> G;
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
	public ArrayList<ArrayList<String>> getUniqPairsOri() {	
		if(uniqPairori==null)
			return getUniqPairs();
		else
			return uniqPairori; 
		}
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
	public boolean removePairLinks(int cliqueSizeMax) {
		System.out.println("Avant: "+uniqPairs.size());
		uniqPairori=(ArrayList<ArrayList<String>>) uniqPairs.clone();
		if(G==null){
			G = new Pseudograph<String, DefaultEdge>(DefaultEdge.class);
			for(ArrayList<String> pair: uniqPairs){
				G.addVertex(pair.get(0));
				G.addVertex(pair.get(1));
				G.addEdge(pair.get(0),pair.get(1));				
			}
		}
		BronKerboschCliqueFinder<String,DefaultEdge> bkcf = new BronKerboschCliqueFinder<String,DefaultEdge>(G);
		Collection<Set<String>> cliques =bkcf.getAllMaximalCliques();
		HashSet<String> toremove = new HashSet<String>();
		for(Set<String> clique: cliques){
			int nmbrToRemove=clique.size()-cliqueSizeMax;
			if(nmbrToRemove<0)
				continue;
			java.util.Iterator<String> itr = clique.iterator();
			for(int i=0;i<nmbrToRemove;i++){
				String v=itr.next();//normaly itr.hasNext() should be true because toremove< clique.size()
				G.removeVertex(v);
				toremove.add(v);
				System.out.println("Removing vertex: "+v);
			}
		}
		ArrayList<ArrayList<String>> pairsToRemove = new ArrayList<ArrayList<String>>();
		for(ArrayList<String> pair: uniqPairs)
			for(String v: toremove)
				if(v.equals(pair.get(0)) || v.equals(pair.get(1)))
					pairsToRemove.add(pair);
		for(ArrayList<String> pair: pairsToRemove)
			uniqPairs.remove(pair);
		System.out.println(G);
		System.out.println("Apres: "+uniqPairs.size());
		return true;
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
	public HashSet<String> getHungLabels() {return hungLabels;	}
	public void setHungLabels(HashSet<String> hungLabels) {this.hungLabels=hungLabels;	}
}
	
