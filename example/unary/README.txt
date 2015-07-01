Train and test a model with unary features

Command to train a model with unary feauture
java -classpath ../../lib/crf_dia_ident.jar:../../lib/bsh.jar tool4Gmms.Train --model-file model-file --pathsfile pathstrain --inferencer LoopyBP --max-inferencer LoopyBP --model unary.ser.gz

You should get lambda value like: 
f1 1.9568925104389
f2 1.6900421495449478E-17
It's because feature f2 is not well correlated with the label in the training data file train.unary, so it's get low value

command to do inference
java -classpath ../../lib/crf_dia_ident.jar:../../lib/bsh.jar tool4Gmms.Main --pathsfile pathtests --max-inferencer LoopyBP --model unary.ser.gz

You should get something like this in the training file: 
l1 s1 l1
l1 s2 l1
l1 s3 l1
l1 s4 l2
l2 s5 l2
l1 s6 l2

s4 and s6 are missclassified
