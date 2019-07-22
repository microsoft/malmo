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
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.microsoft.Malmo.MalmoMod;

import java.util.Random;
/** Class that helps to centralise optional logging of mission rewards.<br>
 */
@Mod.EventBusSubscriber
public class SeedHelper
{
    private static Long seed;
    private static boolean seedingEnabled = false;
    private static Random seedGenerator;

    /** Initialize scoing. */
    static public void update(Configuration configs)
    {

        String sSeed = configs.get(MalmoMod.SEED_CONFIGS, "seed", "NONE").getString();
        if(sSeed.isEmpty() || sSeed == "NONE" ){
            seedingEnabled = false;
        }
        else{
            try{
                seed = Long.parseLong(sSeed);
                seedingEnabled = true;
                seedGenerator = new Random(seed);
                System.out.println("[LOGTOPY] Seed set to " + sSeed);

            } catch(NumberFormatException e){
                System.out.println("[ERROR] Seed specified was " + sSeed + ". Expected a long (integer).");
                seedingEnabled = false;
            } 
        }
    }

    static public Random getRandom(){
        if(seedingEnabled){
            return new Random(seedGenerator.nextLong());
        } else{
            return new Random();
        }
    } 


    @SubscribeEvent
    public static void onWorldCreate(WorldEvent.Load loadEvent){
        loadEvent.getWorld().rand = getRandom();
    }
}