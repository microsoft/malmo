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

import net.minecraftforge.common.config.Configuration;

import com.microsoft.Malmo.MalmoMod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

public class SeedHelper
{
    private static ArrayDeque<Long> seeds = new ArrayDeque<Long>();
    private static Random seedGenerator;

    /** Initialize seeding. */
    static public void update(Configuration configs)
    {

        String sSeed = configs.get(MalmoMod.SEED_CONFIGS, "seed", "NONE").getString();
        if(!sSeed.isEmpty() && sSeed != "NONE" ){
            sSeed = sSeed.replaceAll("\\s+","");
            String[] sSeedList =  sSeed.split(",");
                for(int i = 0; i < sSeedList.length; i++){
                try{
                    long seed = Long.parseLong(sSeedList[i]);
                    seeds.push(seed);

                } catch(NumberFormatException e){
                    System.out.println("[ERROR] Seed specified was " + sSeedList[i] + ". Expected a long (integer).");
                }
            } 
        }
    }

    static public Random getRandom(){
        if(seedGenerator != null){
            return new Random(seedGenerator.nextLong());
        } else{
            return new Random();
        }
    }

    /**
     * Advances the seed manager to the next seed.
     */
    static public boolean advanceNextSeed(Long nextSeed){
        if(seeds.size() > 0){
            seedGenerator = new Random(seeds.pop());
            if(nextSeed != null){
                System.out.println("[LOGTOPY] Tried to set seed for environment, but overriden by initial seed list.");
                return false;
            } 
        }
        else{

            if(nextSeed != null){
                seedGenerator = new Random(nextSeed);
            }
            else{
                seedGenerator = null;
            }
        }
        return true;
    }
}