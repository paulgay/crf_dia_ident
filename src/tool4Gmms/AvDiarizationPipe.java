package tool4Gmms;

import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.LabelAlphabet;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Label;
import edu.umass.cs.mallet.base.types.Labels;
import edu.umass.cs.mallet.base.types.LabelsSequence;
import edu.umass.cs.mallet.base.types.AugmentableFeatureVector;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.util.PropertyList;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
/**
 * Convert the token sequence in the data field of each instance to a feature vector sequence.
 @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
*/

public class AvDiarizationPipe extends Pipe implements Serializable
{
    boolean augmentable;									// Create AugmentableFeatureVector's in the sequence
    boolean binary;												// Create binary (Augmentable)FeatureVector's in the sequence
    boolean growAlphabet = true;
    private ArrayList labelDicts;
    public AvDiarizationPipe (Alphabet dataDict,
			      boolean binary, boolean augmentable)
    {
	super (dataDict, null);
	this.augmentable = augmentable;
	this.binary = binary;
    }

    public AvDiarizationPipe (Alphabet dataDict)
    {
	this (dataDict, false, false);
    }
    public AvDiarizationPipe (boolean binary, boolean augmentable)
    {
	super (Alphabet.class, null);
	this.augmentable = augmentable;
	this.binary = binary;
    }

    public AvDiarizationPipe ()
    {
	this (false, false);
    }
    public Instance pipe (Instance inst)
    {
	InstanceRepere carrier = (InstanceRepere)inst;
	ArrayList<String> segmentNames = (ArrayList<String>)carrier.getTarget();
	FeatureVector[] featureSequence = new FeatureVector[segmentNames.size()];
	Labels[] lbls = new Labels[segmentNames.size()];
	this.labelDicts = new ArrayList ();// reinitialisation  added for the case several instances do not share their vocabulary
	Alphabet alpha;
	//The feature are continuous, so we're tying the parameters over all the possible feature values. the dictionnary is usually used to have an overview of the possible features values and to decide how many weitghs are used. But in our case, there is only 1 weight for all the possible values, so there is just one "fake entry" in the dictionnary.
	String fakeEntry="noDictionnary";
	for (int i = 0; i < segmentNames.size(); i++){
	    ArrayList thisLabels = new ArrayList ();
	    //Integer number =  segmentNumbers.get(i);
	    double[] values = {0};//{number.doubleValue() };
	    int[] indices = { 0 };
	    alpha = (Alphabet)getDataAlphabet();
	    alpha.lookupIndex(fakeEntry);// An entry is added so that the size of the feature's alphabet is one. I do not use alphabet with continuous feature but the size corresponds to the number of parameters.
	    featureSequence[i] = new AugmentableFeatureVector (alpha, null,values,values.length,values.length,false,false,false);
	    thisLabels.add (labelForTok (segmentNames.get(i), 0));//call to labelForTok adds new label to the dictionnary 0 if not present
	    lbls[i] = new Labels ((Label[]) thisLabels.toArray (new Label[thisLabels.size ()]));
	}
	
	//we add some noisy labels which are not associated to the segments in the training set
	//LabelAlphabet labelofthisInstance=((Labels) ((LabelsSequence) carrier.getTarget()).get(0)).get(0).getLabelAlphabet ();
	
	LabelAlphabet dictlabelofthisInstance= (LabelAlphabet) labelDicts.get (0);
    HashMap<String,Integer> labelIdx = carrier.getLabelIdx();
    for (String key : labelIdx.keySet()) 
    	dictlabelofthisInstance.lookupLabel (key);
    
	carrier.setData(new FeatureVectorSequence (featureSequence));
	carrier.setTarget (new LabelsSequence (lbls));
	return (Instance)carrier;
    }
    //trying to use continuous feature with one weight for all the features values, so the size of the alphabet is limited to 1
    private Label labelForTok (String tok, int lvl)
    /*
     * there can be severals type of label. Each one must have it's own alphabet.
     * So if there is more label types than alphabet, new alphabets are added. this probably assumes
     * that the labels should be always written in the data file in the same order (in the same column).
     * Because lvl (which corresponds to the token number) as an index to choose the alphabet from labelDicts.
     */
    {
	while (labelDicts.size() <= lvl) {
	    labelDicts.add (new LabelAlphabet ());
	}
	LabelAlphabet dict = (LabelAlphabet) labelDicts.get (lvl);//get the dictionnary for the corresponding column of label
	return dict.lookupLabel (tok);//if "tok" is not in "dict" it will be added, the label will have this dictionnary. This dictionnary will be store in the variable "labelDicts" and will be reused for other data points. Since I'm dealing with instances that do not share their labels (persons names for each show), "labelDicts" is reinitialised for each new instance.
    }
    public void setGrowAlphabet(boolean growAlphabet) {
	this.growAlphabet = growAlphabet;
    }

    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;
    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeInt (CURRENT_SERIAL_VERSION);
	out.writeBoolean(augmentable);
	out.writeBoolean(binary);
	out.writeBoolean(growAlphabet);
    }
    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
	int version = in.readInt ();
	augmentable = in.readBoolean();
	binary = in.readBoolean();
	//		growAlphabet = true;
	growAlphabet = in.readBoolean();
    }
}
