Train and test a model with unary features

Command to train a model with unary feauture
java -classpath ../../class/:../../lib/bsh.jar tool4Gmms.Train --model-file model-file --pathsfile pathstrain --inferencer LoopyBP --max-inferencer LoopyBP --model unary.ser.gz
command to do inference
java -classpath ../../class/:../../lib/bsh.jar tool4Gmms.Main --pathsfile pathtests --max-inferencer LoopyBP --model unary.ser.gz


