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

public class PrintAcrf{




  private static CommandOption.String model = new CommandOption.String
          (GenericAcrfTui.class, "model", "STRING", true, "noModel",
                  "Specification of the model to be used for testing.", null);


  private static BshInterpreter interpreter = setupInterpreter ();

    public static void main(String args[]){
	doProcessOptions (GenericAcrfTui.class, args);
	Timing timing = new Timing ();
	try {
	    String acrfzipped = model.value;
	    ACRF acrf = (ACRF)FileUtils.readObject(new File(acrfzipped));
	    acrf.print(System.out);
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
