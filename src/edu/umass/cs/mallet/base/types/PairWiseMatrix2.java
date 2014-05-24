/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.umass.cs.mallet.base.types; // Generated package name


/**
 *  Implementation of Matrix that allows arbitrary
 *   number of dimensions.  This implementation
 *   simply uses a flat array.
 *
 *  This also provides static utilities for doing
 *   arbitrary-dimensional array indexing (see
 *   {@link #singleIndex}, {@link #singleToIndices}).
 *
 * Created: Tue Sep 16 14:52:37 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: Matrixn.java,v 1.4 2004/08/31 18:39:51 casutton Exp $
 */
public class PairWiseMatrix2 implements Matrix, Cloneable {
	double[] values;
	int numDimensions;
	int[] sizes;
	/**
	 *  Create a matrix with the given dimensions.
	 *
	 *  @param szs An array containing the maximum for
	 *      each dimension.
	 */
	public PairWiseMatrix2 (int szs[]) {
		try{
			if(szs.length!=2)
				throw new Exception("the pair wise matrix should have only 2 dimensions, found "+szs.length);
			numDimensions = szs.length;
			sizes = (int[])szs.clone();
		/*int total = 1;
		for (int j = 0; j < numDimensions; j++) {
	    total *= sizes [j];
		}*/
			values = new double [2];
		}
		catch(Exception e){ System.err.println("Exception: " + e.getMessage());}
	}
	
	public int getNumDimensions () { return numDimensions; };
	
	public int getDimensions (int [] szs) {
		for ( int i = 0; i < numDimensions; i++ ) {
	    szs [i] = this.sizes [i];
		} 
		return numDimensions;
	}
	
	public double value (int[] indices) {
		return values [singleIndex (indices)];
	}
	
	public void setValue (int[] indices, double value) {
		values [singleIndex (indices)] = value;
	}

	public ConstantMatrix cloneMatrix () {
		/* The  constructor will clone the arrays. */
		return new PairWiseMatrix (sizes, values);
	}

	public Object clone () {
		return cloneMatrix(); 
	}

	public int singleIndex (int[] indices) 
	{
		return singleIndex (sizes, indices);
	}

	// This is public static so it will be useful as a general
	// dereferencing utility for multidimensional arrays.
	public static int singleIndex (int[] szs, int[] indices)
	{
		int row=indices[0]/szs[0];
		int col=indices[0]%szs[0];
		if(row==col)
			return 0;
		else
			return 1;
	}

	// NOTE: Cut-n-pasted to other singleToIndices method!!
	public void singleToIndices (int single, int[] indices) {
		/* must be a better way to do this... */
		int size = 1;
		for (int i = 0; i < numDimensions; i++) {
	    size *= sizes[i];
		}
		for ( int dim = 0; dim < numDimensions; dim++) {
	    size /= sizes [dim];
	    indices [dim] = single / size;
	    single = single % size;
		} 
	}

	/** Just a utility function for arbitrary-dimensional matrix
	 * dereferencing. 
	 */
	// NOTE: Cut-n-paste from other singleToIndices method!!
	public static void singleToIndices (int single, int[] indices, int[] szs) {
		int numd = indices.length;
		assert numd == szs.length;
		/* must be a better way to do this... */
		int size = 1;
		for (int i = 0; i < numd; i++) {
	    size *= szs[i];
		}
		for ( int dim = 0; dim < numd; dim++) {
	    size /= szs [dim];
	    indices [dim] = single / size;
	    single = single % size;
		} 
	}

	public boolean equals (Object o) {
		if (o instanceof PairWiseMatrix) {
			/* This could be extended to work for all Matrixes. */
			PairWiseMatrix m2 = (PairWiseMatrix) o;  
			return 
				(numDimensions == m2.numDimensions) &&
				(sizes.equals (m2.sizes)) &&
				(values.equals (m2.values));
		} else {
			return false;
		}
	}
	public void setSingleValue (int i, double value){
		int row=i/sizes[0];
		int col=i%sizes[0];
		if(row==col)
			values[0]=value;
		else
			values[1]=value;
	}
	public int indexAtLocation (int location) {return location;	}
	public int numLocations () { return sizes[0]*sizes[1]; }
	public int singleSize() { return numLocations (); }
	public double valueAtLocation (int location) {
		int row=location/sizes[0];
		int col=location%sizes[0];
		if(row==col)
			return values[0];
		else
			return values[1];
	}
	public int location (int index){
		return index; 
	}
	public void incrementSingleValue (int i, double delta){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}

  public void setValueAtLocation (int loc, double value){
	  // indices == locations
	  setSingleValue (loc, value);
	}

  public double singleValue(int loc){
	  return valueAtLocation(loc);
  }
	public void setAll (double v){
		values[0]=v;
		values[1]=v;
	}
	
	public double dotProduct (ConstantMatrix m) {
		System.out.println("Warning");
		double ret = 0;
		for (int i = m.numLocations()-1; i >= 0; i--)
			if(m.indexAtLocation(i) < values.length)//fix problem
				ret += valueAtLocation(i) * m.valueAtLocation(i);
			else{
//				System.out.println(m.indexAtLocation(i) + ":" + values.length);
//				throw new ArrayIndexOutOfBoundsException(m.indexAtLocation(i));
			}
		return ret;
	}
	
	public void set (ConstantMatrix m){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void setWithAddend (ConstantMatrix m, double addend){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void setWithFactor (ConstantMatrix m, double factor){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void plusEquals (ConstantMatrix m){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void plusEquals (ConstantMatrix m, double factor){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void equalsPlus (double factor, ConstantMatrix m){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void timesEquals (double factor){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void elementwiseTimesEquals (ConstantMatrix m){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void elementwiseTimesEquals (ConstantMatrix m, double factor){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void divideEquals (double factor){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void elementwiseDivideEquals (ConstantMatrix m){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}
	public void elementwiseDivideEquals (ConstantMatrix m, double factor){
		System.err.println("Warning, the function incrementSingleValue is not implemented");
		new Throwable().printStackTrace();
	}

	public double oneNorm (){
		System.out.println("Warning");
		return values[0]*sizes[0]+values[1]*sizes[0]*(sizes[0]-1);
	}	
	public double twoNorm (){
		System.out.println("Warning");
		return 	values[0]*values[0]*sizes[0]*sizes[0];
	}
	public double absNorm(){
		System.out.println("Warning");
		return Math.abs(values[1])*sizes[0]*sizes[0];
	}
	public double infinityNorm (){
		System.out.println("Warning");
		return Math.abs(values[0]);
	}
	public double oneNormalize ()	{
		System.out.println("Warning");
		double norm = oneNorm();
		for (int i = 0; i < values.length; i++)
			values[i] /= norm;
		return norm;
	}

	public double twoNormalize ()	{
		System.out.println("Warning");
		double norm = twoNorm();
		for (int i = 0; i < values.length; i++)
			values[i] /= norm;
		return norm;
	}

	public double absNormalize ()	{
		System.out.println("Warning");
		double norm = absNorm();
		if (norm > 0)
			for (int i = 0; i < values.length; i++)
				values[i] /= norm;
		return norm;
	}

	public double infinityNormalize () {
		System.out.println("Warning");
		double norm = infinityNorm();
		for (int i = 0; i < values.length; i++)
			values[i] /= norm;
		return norm;
	}

	public boolean isNaN() {
		System.out.println("Warning");
		for (int i = 0; i < values.length; i++)
			if (Double.isNaN(values[i]))
				return true;
		return false;
	}
	public void print(){
		System.out.println(values[0]+" "+values[1]);
	}
	
  /**
   * Returns a one-dimensional array representation of the matrix.
   *   Caller must not modify the return value.
   * @return An array of the values where index 0 is the major index, etc.
   */
  public double[] toArray () {
	 System.out.println("Warning");
    return values;
  }
  
		/* Test array referencing and dereferencing */
		public static void main(String[] args) {
			double m1[] = new double[] { 1.0, 2.0 };
			int idx[] = {10,10};
			PairWiseMatrix m = new PairWiseMatrix(idx);
			int idi[]={4,5};
			m.setValueAtLocation(45,19);
			System.out.println(m.singleValue(45));
			System.out.println(m.value(idi));
		} 
    
	} 
