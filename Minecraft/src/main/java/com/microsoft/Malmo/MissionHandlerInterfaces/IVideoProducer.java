package com.microsoft.Malmo.MissionHandlerInterfaces;

import java.nio.ByteBuffer;

import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for objects which are responsible for providing Minecraft video data.
 */
public interface IVideoProducer
{
    /** Get a frame of video from Minecraft.
     * @param missionInit the MissionInit object for the currently running mission, which may contain parameters for the video requirements.
     * @return an array of bytes representing this frame.<br>
     * (The format is unspecified; it is up to the IVideoProducer implementation and the agent to agree on how the data is formatted.)
     */
    public void getFrame(MissionInit missionInit, ByteBuffer buffer);
    
    /** Get the requested width of the video frames returned.*/
    public int getWidth(MissionInit missionInit);

    /** Get the requested height of the video frames returned.*/
    public int getHeight(MissionInit missionInit);
    
    /** Get the number of bytes required to store a frame.*/
    public int getRequiredBufferSize();
    
    /** Called once before the mission starts - use for any necessary initialisation.*/
    public void prepare(MissionInit missionInit);
    
    /** Called once after the mission ends - use for any necessary cleanup.*/
    public void cleanup();
}
