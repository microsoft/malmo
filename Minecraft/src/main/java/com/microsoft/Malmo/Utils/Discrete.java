// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

package com.microsoft.Malmo.Utils;

import java.util.Random;

/**
 * This class provides a discrete distribution that can be initialized in a
 * variety of ways and sampled fairly easily.
 */
public class Discrete {
    private Random rand;
    private double[] likelihoods;
    private double sum;
    
    /**
     * Constructor. 
     * @param rand A random number generator
     * @param counts The counts per dimension, used to form the distribution. If the sum of the counts is zero, it will set the count for the first dimension to 1.
     */
    public Discrete(Random rand, int[] counts)
    {
        this.likelihoods = new double[counts.length];
        this.rand = rand;		
        this.sum = 0;
        for(int i=0; i<counts.length; i++){
            this.sum += counts[i];
            this.likelihoods[i] = counts[i];
        }
        
        if(this.sum == 0){
            this.likelihoods[0] = 1;
            this.sum = 1;
        }
    }
    
    /**
     * Constructor.
     * @param rand A random number generator
     * @param count The number of dimensions
     * @param complexity The complexity of the distribution. Acts like an inverse entropy, where a complexity of 0 results in a peaked distribution on the first dimension and a complexity of 1 will be a uniform distribution.
     */
    public Discrete(Random rand, int count, double complexity)
    {
        double weight = ((1 - complexity) / count) + complexity;
        this.rand = rand;
        this.likelihoods = new double[count];
        this.sum = weight;
        double part = 1.0 / count;
        for(int i=0; i<count; i++){
            double likelihood = Math.min(part, weight);
            this.likelihoods[i] = likelihood;
            weight -= likelihood;
        }		
    }
    
    /**
     * Sample a value from the distribution.
     * @return The sampled dimension
     */
    public int sample()
    {
        double val = this.rand.nextDouble() * this.sum;
        int sample = 0;		
        while(sample < this.likelihoods.length){
            val -= this.likelihoods[sample];
            if(val < 0){
                return sample;
            }
            
            sample++;			
        }
        
        return this.rand.nextInt(this.likelihoods.length);
    }
    
    public int take()
    {
    	int result = sample();
    	if(this.likelihoods[result] >= 1){
    		this.likelihoods[result] -= 1;
    		this.sum -= 1;
    	}
    	
    	return result;
    }
}
