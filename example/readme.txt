each directory show the commands to train a CRF with unary, pairwise and uniq potential

In each case the features are assumed to be continuous and parameters are tied over all the labels


The command to train a CRF model looks like: 
java -classpath ../../class/:../../lib/bsh.jar tool4Gmms.Train --model-file model-file --pathsfile pathsfile --inferencer LoopyBP --max-inferencer LoopyBP --model CRFmodel.ser.gz

--model-file contains the java code creating the required potentials
--inferencer is the option specifying the type of inference algorithm which will be used. Have a look at edu.umass.cs.mallet.grmm.inference to see what is available
--max-inferencer is probably useless here
--model the filename where the CRF model will be saved
--pathsfile contains the location of the required file and follow this syntax: 
0 showname
1 listFile 
2 unaryFile
3 pairWiseFile
4 UniqFile
5 outputDir


The file have space separated column formats:
listFile:
label segmentID

unary feauture file:
feature_type segmentID label value

pairwise feature file:
feature_type segment1 segment2 value

unicity constraint file (This potential enforce two segments not to have the same label e.g. to faces in the same image cannot have the same name ):
seg1 seg2


command to do inference
java -classpath ../../class/:../../lib/bsh.jar tool4Gmms.Main --pathsfile pathtests --max-inferencer LoopyBP --model unary.ser.gz
--max-inferencer the inference algorithm that will be used

The results are two files which will be located depending on the value 0 and 5 in pathtests
The file .mapping has the following structure:
inferedLabel segmentID originalLabel
The file .margin:
segmentID label1:probabilty_4_Label1 label2:probabilty_4_Label2 ....


**** Pathfile syntax ***
The pathfiles follow this syntax
0 showname1
1 listFile
2 unaryFile
3 pairWiseFile
4 UniqFile
5 outputDir
0 showname2
1 listFile
2 unaryFile
3 pairWiseFile
4 UniqFile
5 outputDir
....


Note that for each several feature files can be used and others can be omitted:
0 showname1
1 listFile
2 unaryFile1
2 unaryFile2
3 pairWiseFile
5 outputDir


The files have space separated column formats:
listFile:
label segmentID

unary feauture file:
feature_type segmentID label value

pairwise feature file:
feature_type segment1 segment2 value

unicity constraint file (This potential enforce two segments not to have the same label e.g. to faces in the same image cannot have the same name ):
seg1 seg2


