each example directory show the commands to train a CRF with unary, pairwise and uniq potential

The following comments are about the general syntax of the files and the commands. I hope that put in parallel with the examples, this is enough to get you started

The features are assumed to be continuous and parameters are tied over all the labels, i.e. there is one CRF parameter for each feature (note other techniques such as [1] could be applied, but I did not implemented them)

**** train a CRF model *****
The command to train a CRF model looks like: 
java -classpath ../../class/:../../lib/bsh.jar tool4Gmms.Train --model-file model-file --pathsfile pathsfile --inferencer LoopyBP --max-inferencer LoopyBP --model CRFmodel.ser.gz

--model-file contains the java code creating the required potentials
--inferencer is the option specifying the type of inference algorithm which will be used. Have a look at edu.umass.cs.mallet.grmm.inference to see what is available
--max-inferencer is for testing and so it is useless here
--model the filename where the CRF model will be saved
--pathsfile contains the location of the required files and follow this syntax: 
0 showname
1 listFile 
2 unaryFile
3 pairWiseFile
4 UniqFile
5 outputDir

***syntax of the different files***

The file have space separated column formats:
*listFile:
label segmentID

During training the CRF model, the label is used as ground true. During inference and testing, you can put whatever label you want, it doesn't matter.


*unaryFile (if a couple segmentID/feature_type is missing for a label, the value will be filled with 0 by the program):
feature_type segmentID label value

*pairWiseFile (The output will be "value" if the CRF give the same label to segment1 and segment2 and "-value" otherwise" ):
feature_type segment1 segment2 value

*UniqFile ( unicity constraint file: This potential enforce two segments not to have the same label e.g. to faces in the same image cannot have the same name ):
seg1 seg2


**** pathsfile syntax ***
The pathsfile file follow this syntax
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


Note that several feature files can be used and others can be omitted:
0 showname1
1 listFile
2 unaryFile1
2 unaryFile2
3 pairWiseFile
5 outputDir


***** command to do inference ******
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


Note that several feature files can be used and others can be omitted:
0 showname1
1 listFile
2 unaryFile1
2 unaryFile2
3 pairWiseFile
5 outputDir

***** command to do inference ******
* decoding
java -classpath crf_dia_ident.jar:bsh.jar tool4Gmms.Main --pathsfile pathtests --max-inferencer LoopyBP --model unary.ser.gz
--max-inferencer the inference algorithm that will be used

The results is a .mapping file which will be located depending on the value 0 and 5 in pathtests
The file .mapping has the following structure:
inferedLabel segmentID originalLabel


*getting marginal
java -classpath crf_dia_ident.jar:bsh.jar tool4Gmms.GetMargin --pathsfile pathtests --max-inferencer LoopyBP --model pairwise.ser.gz
This will produce a .margin file which contains the probability of each label given a segment.



[1]: Using continuous features in the maximum entropy model, Yu, Dong and Deng, Li and Acero, Alex, Pattern Recognition Letters, 2009


