package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Composite class that concatenates the results from multiple ObservationProducer objects.<br>
 */
public class ObservationFromComposite extends HandlerBase implements IObservationProducer
{
    private ArrayList<IObservationProducer> producers;
	
	/** Add another ObservationProducer object.<br>
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
}