java -classpath ../../class/:../../lib/bsh.jar tool4Gmms.Train --model-file model-file --pathsfile pathstrain --inferencer LoopyBP --max-inferencer LoopyBP --model pairwise.ser.gz
java -classpath ../../class/:../../lib/bsh.jar tool4Gmms.Main --pathsfile pathtests --max-inferencer LoopyBP --model pairwise.ser.gz
