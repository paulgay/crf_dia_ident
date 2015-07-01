unicity constraint is about saying that two data points cannot have the same label

training
java -classpath ../../lib/crf_dia_ident.jar:../../lib/bsh.jar tool4Gmms.Train --model-file model-file --pathsfile pathstrain --inferencer LoopyBP --max-inferencer LoopyBP --model uniq.ser.gz

testing
java -classpath ../../lib/crf_dia_ident.jar:../../lib/bsh.jar tool4Gmms.Main --pathsfile pathtests --max-inferencer LoopyBP --model uniq.ser.gz

thanks to the unicity constraint between s1 and s4, s4 get the correct classification label
Note that the inference might not convergence if there is too many pairwise links (it's depend on the size of the clique in the graph). In this case, the programm will remove some links to get smaller cliques and will try again
