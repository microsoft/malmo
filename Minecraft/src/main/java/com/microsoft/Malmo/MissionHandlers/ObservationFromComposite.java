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

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

/**
 * Composite class that concatenates the results from multiple ObservationProducer objects.<br>
 */
public class ObservationFromComposite extends HandlerBase implements IObservationProducer
{
    private ArrayList<IObservationProducer> producers;

    /**
     * Add another ObservationProducer object.<br>
     * 
     * @param producer the observation producing object to add to the mix.
     */
    public void addObservationProducer(IObservationProducer producer)
    {
        if (this.producers == null)
        {
            this.producers = new ArrayList<IObservationProducer>();
        }
        this.producers.add(producer);
    }

    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        if (this.producers == null)
            return;

        for (IObservationProducer producer : this.producers)
        {
            producer.writeObservationsToJSON(json, missionInit);
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        for (IObservationProducer producer : this.producers)
        {
            producer.prepare(missionInit);
        }
    }

    @Override
    public void cleanup()
    {
        for (IObservationProducer producer : this.producers)
        {
            producer.cleanup();
        }
    }

    @Override
    public void appendExtraServerInformation(HashMap<String, String> map)
    {
        for (IObservationProducer producer : this.producers)
        {
            if (producer instanceof HandlerBase)
                ((HandlerBase)producer).appendExtraServerInformation(map);
        }
    }

    public boolean isFixed()
    {
        return false; // Return true to stop MissionBehaviour from adding new handlers to this group.
    }
}