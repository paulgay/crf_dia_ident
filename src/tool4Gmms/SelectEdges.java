package tool4Gmms;

import org.jgrapht.*;
import org.jgraph.JGraph;
import org.jgrapht.graph.*;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.ext.JGraphModelAdapter;
import java.util.*;
import java.io.*;
public class SelectEdges {
    private double[][] pCostTable;
    private double[] flatTable;
    private int N;
    public SelectEdges(double[][] pCostTable){
	this.pCostTable=pCostTable;
	this.N=pCostTable.length;
	setFlatTable();
    }
    public void setFlatTable(){
	this.flatTable=new double[(N-1)*N/2];
	//	System.out.println("la flat tabvkle");
	for(int j=0;j<N;j++)
	    for(int i=j+1;i<N;i++){
		//System.out.println(i+" "+j+" "+matIndicesToFlatIndex(j,i)+" "+pCostTable[i][j]);
		flatTable[matIndicesToFlatIndex(j,i)]=pCostTable[i][j];
	    }
    }
    public SimpleGraph selectEdges(){
	SimpleGraph<Integer,DefaultEdge> graph = new SimpleGraph<Integer,DefaultEdge>(DefaultEdge.class);
	for(int i=0;i<N;i++)
	    graph.addVertex(new Integer(i));
	for(int i=0;i<flatTable.length;i++){
	    int[] vertices=flatIndexToMatIndices(i);
	    if(null==DijkstraShortestPath.findPathBetween(graph,new Integer(vertices[0]), new Integer(vertices[1]))){
		graph.addEdge(new Integer(vertices[0]), new Integer(vertices[1]));
	    }
	}
	return graph;
    }
    public int matIndicesToFlatIndex(int col, int row){
	int flatIndex=0;
	for(int i=0;i<col;i++)
	    flatIndex=flatIndex+(N-(i+1));
	return flatIndex + row-col-1;
    }
    public int[] flatIndexToMatIndices(int flatIndex){
	int col=1;
	while(flatIndex>(N-col)){
	    flatIndex=flatIndex-(N-col);
	    col=col+1;
	}
	return new int[]{col-1,flatIndex+col-1};
    }
    public static void main(String args[]){
	double[][] table = new double[5][5];
	for(int i=0;i<5;i++)
	    for(int j=0;j<i;j++)
		table[i][j]=i*j+i;
	for(int i=0;i<5;i++)
	    for(int j=i+1;j<5;j++)
		table[i][j]=j*i+j;
	for(int i=0;i<5;i++)
	    for(int j=0;j<5;j++)
		System.out.println(i+" "+j+" "+table[i][j]);
	SelectEdges s = new SelectEdges(table);
	SimpleGraph g = s.selectEdges();
	System.out.println("le graph");
	System.out.println(g);
	System.out.println(g.containsEdge(new Integer(0), new Integer(1)));
	System.out.println(g.containsEdge(new Integer(0), new Integer(0)));

    }
}