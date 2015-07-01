train the model
java -classpath ../../lib/crf_dia_ident.jar:../../lib/bsh.jar tool4Gmms.Train --model-file model-file --pathsfile pathstrain --inferencer LoopyBP --max-inferencer LoopyBP --model pairwise.ser.gz

inference decoding by getting the max probable label
java -classpath ../../lib/crf_dia_ident.jar:../../lib/bsh.jar tool4Gmms.Main --pathsfile pathtests --max-inferencer LoopyBP --model pairwise.ser.gz


or simply get the marginal: 

java -classpath ../../lib/crf_dia_ident.jar:../../lib/bsh.jar tool4Gmms.GetMargin --pathsfile pathtests --max-inferencer LoopyBP --model pairwise.ser.gz



pairwise feature is multiply by: 
+lambda if the segments have same labels
-lambda if segments have different labels

where lambda is the parameter learned for the pairwise feature


When comparing results with the unary example
s6 get the now the good classification label due to the pairwise feature between s5 and s6
