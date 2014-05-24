package tool4Gmms;
import bsh.EvalError;
import edu.umass.cs.mallet.base.util.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.grmm.inference.Inferencer;
import edu.umass.cs.mallet.grmm.learning.GenericAcrfTui;
import edu.umass.cs.mallet.grmm.learning.ACRF;
import edu.umass.cs.mallet.grmm.learning.ACRFTrainer;
import edu.umass.cs.mallet.grmm.learning.ACRFEvaluator;
import edu.umass.cs.mallet.grmm.learning.AcrfSerialEvaluator;
import edu.umass.cs.mallet.grmm.learning.MultiSegmentationEvaluatorACRF;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.Label;
import edu.umass.cs.mallet.base.types.Labels;
import edu.umass.cs.mallet.base.types.LabelsSequence;

public class Train2{

  private static CommandOption.File modelFile = new CommandOption.File
          (GenericAcrfTui.class, "model-file", "FILENAME", true, null, "Text file describing model structure.", null);


  private static CommandOption.File pathsfile = new CommandOption.File
          (GenericAcrfTui.class, "pathsfile", "FILENAME", true, null, "File containing paths to the different files", null);

  private static CommandOption.Integer numLabelsOption = new CommandOption.Integer
  (GenericAcrfTui.class, "num-labels", "INT", true, -1,
          "If supplied, number of labels on each line of input file." +
                  "  Otherwise, the token ---- must separate labels from features.", null);

  private static CommandOption.String inferencerOption = new CommandOption.String
          (GenericAcrfTui.class, "inferencer", "STRING", true, "TRP",
                  "Specification of inferencer.", null);

  private static CommandOption.String maxInferencerOption = new CommandOption.String
          (GenericAcrfTui.class, "max-inferencer", "STRING", true, "TRP.createForMaxProduct()",
                  "Specification of inferencer.", null);

  private static CommandOption.String trainingshows = new CommandOption.String
          (GenericAcrfTui.class, "trainingshows", "STRING", true, "/idiap/temp/pgay/association/showsTrain",
                  "Specification of the shows used for training.", null);

  private static CommandOption.String testshows = new CommandOption.String
          (GenericAcrfTui.class, "testshows", "STRING", true, "/idiap/temp/pgay/association/showsTest",
                  "Specification of the shows used for training.", null);

  private static CommandOption.String model = new CommandOption.String
          (GenericAcrfTui.class, "model", "STRING", true, "noModel",
                  "Specification of the model to be used for testing.", null);

  private static CommandOption.String iterNumber = new CommandOption.String
          (GenericAcrfTui.class, "iterNumber", "STRING", true, "none",
                  "iter number.", null);

  private static CommandOption.String evalOption = new CommandOption.String
          (GenericAcrfTui.class, "eval", "STRING", true, "LOG",
                  "Evaluator to use.  Java code grokking performed.", null);

  static CommandOption.Boolean cacheUnrolledGraph = new CommandOption.Boolean
          (GenericAcrfTui.class, "cache-graphs", "true|false", true, false,
                  "Whether to use memory-intensive caching.", null);
    
  static CommandOption.Boolean useTokenText = new CommandOption.Boolean
          (GenericAcrfTui.class, "use-token-text", "true|false", true, false,
                  "Set this to true if first feature in every list is should be considered the text of the " +
                          "current token.  This is used for NLP-specific debugging and error analysis.", null);

  static CommandOption.Integer randomSeedOption = new CommandOption.Integer
	(GenericAcrfTui.class, "random-seed", "INTEGER", true, 0,
	 "The random seed for randomly selecting a proportion of the instance list for training", null);


  private static BshInterpreter interpreter = setupInterpreter ();

    public static void main(String args[]){
	doProcessOptions (GenericAcrfTui.class, args);
	Timing timing = new Timing ();
	try {
	    //int iteNumber=(int)(new Integer (iterNumber.value));
	    AvDiarizationPipe pipe = new AvDiarizationPipe();
	    String showsTrainFile = trainingshows.value;
        ArrayList<String> showTrain = getShows(showsTrainFile);
	    String pathsFile = pathsfile.value.getAbsolutePath();
	    ACRF.Template[] tmpls = parseModelFile (modelFile.value);
	    //MakeTableFrValues inputTrainingPipe = new MakeTableFrValues(trainingDataDir,-1,showTrain,tmpls);
	    int itNumber=(int)(new Integer (iterNumber.value));
	    InstanceFactory2 inputTrainingPipe = new InstanceFactory2(pathsFile,itNumber,showTrain,tmpls);
	    InstanceListRepere training = new InstanceListRepere(pipe);
	    training.add(inputTrainingPipe);
	    
	    
	    ACRFEvaluator eval = createEvaluator (evalOption.value);
	    Inferencer inf = createInferencer (inferencerOption.value);
	    Inferencer maxInf = createInferencer (maxInferencerOption.value);
	    ACRF acrf = new ACRF (pipe, tmpls);//ACRF.java l64
	    acrf.setInferencer (inf);
	    acrf.setViterbiInferencer (maxInf);
	    ACRFTrainer trainer = new ACRFTrainer ();//empty constructor
	    trainer.train (acrf, training, null, null, eval, 9999);
	    //trainer.train (acrf, training, null, testing, eval, 9999);//ACRFTrainer l84
	    FileUtils.writeGzippedObject (new File (model.value), acrf);
	    timing.tick ("Training");
	    System.out.println ("Total time (ms) = " + timing.elapsedTime ());
/*    
	    String testingDataDir = testFile.value.getAbsolutePath();
	    String showsTestFile = testshows.value;
	    ArrayList<String> showTest = getShows(showsTestFile);
	    MakeTableFrValues inputTestingPipe = new MakeTableFrValues(testingDataDir,-1,showTest,tmpls);
	    InstanceListRepere testing = new InstanceListRepere(pipe);
	    testing.add(inputTestingPipe);
	    AVDiarizationEval.writeResults(acrf,testing,itNumber);
*/
	}
	catch(Exception e){System.out.println(e); e.printStackTrace();}
    }

    public static ArrayList<String> getShows(String listShow) throws Exception{
        ArrayList<String> shows = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new FileReader(listShow));
        String line;
        while((line = in.readLine())!=null){
            shows.add(line);
        }
        return shows;
    }
    private static BshInterpreter setupInterpreter ()
    {
	BshInterpreter interpreter = CommandOption.getInterpreter ();
	try {
      interpreter.eval ("import edu.umass.cs.mallet.base.extract.*");
      interpreter.eval ("import edu.umass.cs.mallet.grmm.inference.*");
      interpreter.eval ("import edu.umass.cs.mallet.grmm.learning.*");
      interpreter.eval ("import edu.umass.cs.mallet.grmm.learning.templates.*");
    } catch (EvalError e) {
      throw new RuntimeException (e);
    }

    return interpreter;
  }

  public static ACRFEvaluator createEvaluator (String spec) throws EvalError
  {
    if (spec.indexOf ('(') >= 0) {
      // assume it's Java code, and don't screw with it.
      return (ACRFEvaluator) interpreter.eval (spec);
    } else {
      LinkedList toks = new LinkedList (Arrays.asList (spec.split ("\\s+")));
      return createEvaluator (toks);
    }
  }

  private static ACRFEvaluator createEvaluator (LinkedList toks)
  {
    String type = (String) toks.removeFirst ();

    if (type.equalsIgnoreCase ("SEGMENT")) {
      int slice = Integer.parseInt ((String) toks.removeFirst ());
      if (toks.size() % 2 != 0)
        throw new RuntimeException ("Error in --eval "+evalOption.value+": Every start tag must have a continue.");
      int numTags = toks.size () / 2;
      String[] startTags = new String [numTags];
      String[] continueTags = new String [numTags];

      for (int i = 0; i < numTags; i++) {
        startTags[i] = (String) toks.removeFirst ();
        continueTags[i] = (String) toks.removeFirst ();
      }

      return new MultiSegmentationEvaluatorACRF (startTags, continueTags, slice);

    } else if (type.equalsIgnoreCase ("LOG")) {
      return new ACRFTrainer.LogEvaluator ();

    } else if (type.equalsIgnoreCase ("SERIAL")) {
      List evals = new ArrayList ();
      while (!toks.isEmpty ()) {
        evals.add (createEvaluator (toks));
      }
      return new AcrfSerialEvaluator (evals);

    } else {
      throw new RuntimeException ("Error in --eval "+evalOption.value+": illegal evaluator "+type);
    }
  }

  private static Inferencer createInferencer (String spec) throws EvalError
  {
    String cmd;
    if (spec.indexOf ('(') >= 0) {
      // assume it's Java code, and don't screw with it.
      cmd = spec;
    } else {
      cmd = "new "+spec+"()";
    }

    // Return whatever the Java code says to
    Object inf = interpreter.eval (cmd);

    if (inf instanceof Inferencer)
      return (Inferencer) inf;

    else throw new RuntimeException ("Don't know what to do with inferencer "+inf);
  }


  public static void doProcessOptions (Class childClass, String[] args)
  {
    CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
    options.add (childClass);
    options.process (args);
    options.logOptions (Logger.getLogger (""));
  }

  private static ACRF.Template[] parseModelFile (File mdlFile) throws IOException, EvalError
  {
    BufferedReader in = new BufferedReader (new FileReader (mdlFile));

    List tmpls = new ArrayList ();
    String line = in.readLine ();
    while (line != null) {
      Object tmpl = interpreter.eval (line);
      if (!(tmpl instanceof ACRF.Template)) {
        throw new RuntimeException ("Error in "+mdlFile+" line "+in.toString ()+":\n  Object "+tmpl+" not a template");
      }
      tmpls.add (tmpl);
      line = in.readLine ();
    }

    return (ACRF.Template[]) tmpls.toArray (new ACRF.Template [0]);
  }
}
