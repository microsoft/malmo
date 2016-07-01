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

package com.microsoft.Malmo.MissionHandlers;

import java.util.HashMap;

import com.microsoft.Malmo.Schemas.Reward;

public class MultidimensionalReward {

    private HashMap<Integer, Float> map = new HashMap<Integer, Float>();

    /**
     * Add a given reward value on a specified dimension.
     * 
     * @param dimension
     *            the dimension to add the reward on.
     * @param value
     *            the value of the reward.
     */
    public void add(int dimension, float value) {
        // TODO: check if dimension key exists
        // if exists then sum value
        // else create and set to value
    }

    /**
     * Merge in another multidimensional reward structure.
     * 
     * @param other
     *            the other multidimensional reward structure.
     */
    public void add(MultidimensionalReward other) {
        // TODO: call this.add(key,value) on each entry in other
    }

    /**
     * Retrieve the reward structure as defined by the schema, and reset the
     * storage.
     * 
     * @return the reward structure as defined by the schema.
     */
    public Reward getAndClear() {
        Reward reward = new Reward();
        // TODO: add each entry from the hashmap
        this.clear();
        return reward;
    }

    /**
     * Resets the storage to empty.
     */
    public void clear() {
        this.map.clear();
    }
}
