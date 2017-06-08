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

import net.minecraft.world.storage.WorldInfo;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ServerInitialConditions;
import com.microsoft.Malmo.Schemas.ServerSection;

public class EnvironmentHelper
{
    public static void setMissionWeather(MissionInit minit, WorldInfo worldinfo)
    {
        ServerSection ss = minit.getMission().getServerSection();
        ServerInitialConditions sic = (ss != null) ? ss.getServerInitialConditions() : null;
        if (sic != null && sic.getWeather() != null && !sic.getWeather().equalsIgnoreCase("normal"))
        {
            int maxtime = 1000000 * 20; // Max allowed by Minecraft's own Weather Command.
            int cleartime = (sic.getWeather().equalsIgnoreCase("clear")) ? maxtime : 0;
            int raintime = (sic.getWeather().equalsIgnoreCase("rain")) ? maxtime : 0;
            int thundertime = (sic.getWeather().equalsIgnoreCase("thunder")) ? maxtime : 0;

            worldinfo.setCleanWeatherTime(cleartime);
            worldinfo.setRainTime(raintime);
            worldinfo.setThunderTime(thundertime);
            worldinfo.setRaining(raintime + thundertime > 0);
            worldinfo.setThundering(thundertime > 0);
        }
    }
}
