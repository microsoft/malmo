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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Helper object for sending UDP packets.<br>
 */
public class UDPSocketHelper
{
    /** Send string over UDP to the specified address via the specified port.
     * @param message string to be sent over UDP
     * @param address address to send the message to
     * @param port port number to use
     * @return true if message was successfully sent
     */
    public static boolean sendUDPString(String message, InetAddress address, int port)
    {
        DatagramPacket packet = null;
        boolean succeeded = true;
        packet = new DatagramPacket(message.getBytes(), message.length(), address, port);

        DatagramSocket socket = null;
        try
        {
            socket = new DatagramSocket();
        }
        catch (SocketException e1)
        {
            System.out.println("Error creating UDP socket");
            succeeded = false;
        }
        
        if (socket != null)
        {
            try
            {
                socket.send(packet);
            }
            catch (IOException e)
            {
                System.out.println("Error sending UDP packet");
                succeeded = false;
            }
            socket.close();
        }
        
        return succeeded;
    }
    
    /** Helper function to return a heart-beat object for the Minecraft client.<br>
     * @param address address to send beat signals to
     * @param port port to send beat signals on
     * @param intervalMs interval between beat signals, in milliseconds
     * @return the newly created UDPHeartBeat object
     */
    public static UDPHeartBeat createClientHeartBeat(InetAddress address, int port, int intervalMs)
    {
        return new UDPClientHeartBeat(address, port, intervalMs);
    }

    /** Helper function to return a heart-beat object for the Minecraft server.<br>
     * @param address address to send beat signals to
     * @param port port to send beat signals on
     * @param intervalMs interval between beat signals, in milliseconds
     * @return the newly created UDPHeartBeat object
     */
    public static UDPHeartBeat createServerHeartBeat(InetAddress address, int port, int intervalMs)
    {
        return new UDPSocketHelper.UDPServerHeartBeat(address, port, intervalMs);
    }

    /** Abstract base class for a UDP "heart beat".<br>
     * This is a UDP pulse that is intended to demonstrate the healthy operation of the client code. A monitor can track these pulses
     * and, if the next pulse doesn't arrive within an expected period, can use this to trigger whatever error conditions are implied.<br>
     * The abstract base class contains the logic for timing and sending the beat,
     * but subclasses are responsible for actually calling this code.<br>
     */
    static abstract public class UDPHeartBeat
    {
        private InetAddress address;
        private int port;
        private int intervalMs;
        private long timeOfLastBeat;
        
        /** Create a UDP heart beat object.
         * @param address address of the monitor to which to send the signal 
         * @param port port number the monitor will be listening on
         * @param intervalMs preferred time, in milliseconds, between signals.
         */
        public UDPHeartBeat(InetAddress address, int port, int intervalMs)
        {
            this.address = address;
            this.port = port;
            this.intervalMs = intervalMs;
        }

        /** Send a simple beat signal via UDP.
         */
        protected void beat()
        {
            sendUDPString(getBeatSignal(), this.address, this.port);
            this.timeOfLastBeat = System.currentTimeMillis();
        }
        
        /** Determine whether or not a new heart-beat signal is required.
         * @return true if the time since the last signal is greater than the beat interval requested.
         */
        protected boolean isTimeForBeat()
        {
            long timeNow = System.currentTimeMillis();
            return (timeNow - this.timeOfLastBeat) > this.intervalMs;
        }
        
        /** Override this to return a string to be sent as the heartbeat signal.<br>
         * This allows multiple heartbeats to use the same port, but still be distinguishable.
         * @return the string to be sent as a UDP message each heartbeat 
         */
        abstract public String getBeatSignal();
    }
    
    /** A heart beat that is launched from the client thread.<br>
     * This uses the Minecraft Forge client tick event as an opportunity to send the beat signal.<br>
     * The client heart will beat once the Minecraft instance is up and running, whether or not a game is active.<br>
     * NOTE:the heartbeat interval can not be shorter than the interval between client ticks (normally 50ms in non-overclocked games).
     */
    static public class UDPClientHeartBeat extends UDPHeartBeat
    {
        public UDPClientHeartBeat(InetAddress address, int port, int intervalMs)
        {
            super(address, port, intervalMs);
            FMLCommonHandler.instance().bus().register(this);
        }
        
        /** Tick event called on the Client.<br>
         * Used to send the heart-beat at regular intervals.
         * @param ev ClientTickEvent for this tick.
         */
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent ev)
        {
            if (isTimeForBeat())
            {
                beat();
            }
        }
        
        @Override
		public String getBeatSignal() { return "<client beat>"; }
    }
    
    /** A heart beat that is launched from the server thread.<br>
     * This uses the Minecraft Forge server tick event as an opportunity to send the beat signal.<br>
     * The server heart will only beat while the server is ticking - eg while a game is active (not paused).<br>
     * NOTE:the heartbeat interval can not be shorter than the interval between server ticks (normally 50ms in non-overclocked games).
     */
    static public class UDPServerHeartBeat extends UDPHeartBeat
    {
        public UDPServerHeartBeat(InetAddress address, int port, int intervalMs)
        {
            super(address, port, intervalMs);
            FMLCommonHandler.instance().bus().register(this);
        }
        
        /** Tick event called on the Server.<br>
         * Used to send the heart-beat at regular intervals.
         * @param ev ClientTickEvent for this tick.
         */
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent ev)
        {
            if (isTimeForBeat())
            {
                beat();
            }
        }
        
        @Override
        public String getBeatSignal() { return "<server beat>"; }
    }
}
