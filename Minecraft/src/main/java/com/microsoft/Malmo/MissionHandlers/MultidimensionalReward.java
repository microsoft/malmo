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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.microsoft.Malmo.Schemas.Reward;
import com.microsoft.Malmo.Schemas.Reward.Value;
import com.microsoft.Malmo.Utils.SchemaHelper;

/**
 * Stores a float reward on multiple dimensions. 
 */
public class MultidimensionalReward {

    private Map<Integer, Float> map = new HashMap<Integer, Float>();
    private boolean isFinalReward = false;

    public MultidimensionalReward()
    {
    }

    public MultidimensionalReward(boolean isFinalReward)
    {
        this.isFinalReward = isFinalReward;
    }

    /**
     * True if no rewards have been received.
     * 
     * @return whether the reward is empty.
     */
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean isFinalReward() {
        return this.isFinalReward;
    }

    /**
     * Add a given reward value on a specified dimension.
     * 
     * @param dimension
     *            the dimension to add the reward on.
     * @param value
     *            the value of the reward.
     */
    public void add(int dimension, float value) {
        if(this.map.containsKey(dimension))
            this.map.put(dimension, this.map.get(dimension) + value);
        else
            this.map.put(dimension, value);
    }

    /**
     * Merge in another multidimensional reward structure.
     * 
     * @param other
     *            the other multidimensional reward structure.
     */
    public void add(MultidimensionalReward other) {
        for (Map.Entry<Integer, Float> entry : other.map.entrySet()) {
            Integer dimension = entry.getKey();
            Float reward_value = entry.getValue();
            this.add(dimension.intValue(), reward_value.floatValue());
        }
    }

    /**
     * Retrieve the reward structure as defined by the schema.
     * 
     * @return the reward structure as defined by the schema.
     */
    public Reward getAsReward() {
        Reward reward = new Reward();
        for (Map.Entry<Integer, Float> entry : this.map.entrySet()) {
            Integer dimension = entry.getKey();
            Float reward_value = entry.getValue();
            Value reward_entry = new Value();
            reward_entry.setDimension(dimension);
            reward_entry.setValue(new BigDecimal(reward_value));
            reward.getValue().add(reward_entry);
        }
        return reward;
    }

    /**
     * Gets the reward structure as an XML string as defined by the schema.
     * 
     * @return the XML string.
     */
    public String getAsXMLString() {
        // Create a string XML representation:
        String rewardString = null;
        try {
            rewardString = SchemaHelper.serialiseObject(this.getAsReward(), Reward.class);
        } catch (JAXBException e) {
            System.out.println("Caught reward serialization exception: " + e);
        }
        return rewardString;
    }

    /**
     * Gets the reward structure as a simple, easily parsed string<br>
     * Format: <dimension>:<value>, comma delimited.
     * eg "0:45.6,1:32.2,12:-1.0" etc
     * 
     * @return the string.
     */
    public String getAsSimpleString() {
        String rewardString = "";
        for (Map.Entry<Integer, Float> entry : this.map.entrySet()) {
            Integer dimension = entry.getKey();
            Float reward_value = entry.getValue();
            if (!rewardString.isEmpty())
                rewardString += ",";
            rewardString += dimension + ":" + reward_value;
        }
        return rewardString;
    }

    /**
     * Get the total rewards from all dimensions, each of which may be positive or negative.
     * @return The total rewards.
     */
    public double getRewardTotal() {
        double rewards = 0.0;
        for (Map.Entry<Integer, Float> entry : this.map.entrySet()) {
            rewards += entry.getValue();
        }
        return rewards;
    }

    /**
     * Resets the storage to empty.
     */
    public void clear() {
        this.map.clear();
    }
}
