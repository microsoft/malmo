package com.microsoft.Malmo.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class TCPSocketChannel
{
    SocketChannel channel;
    String address;
    String logname;
    int port;

    public TCPSocketChannel(String address, int port, String logname)
    {
        this.address = address;
        this.port = port;
        this.logname = logname;

        try
        {
            InetSocketAddress insockad = new InetSocketAddress(address, port);
            Log(Level.INFO, "Attempting to open SocketChannel with InetSocketAddress: " + insockad);
            this.channel = SocketChannel.open(insockad);
        }
        catch (IOException e)
        {
            Log(Level.SEVERE, "Failed to open SocketChannel: " + e);
        }
    }

    private void Log(Level level, String message)
    {
        TCPUtils.Log(level, "<-" + this.logname + "(" + this.address + ":" + this.port + ") " + message);
    }

    private void SysLog(Level level, String message)
    {
        TCPUtils.SysLog(level, "<-" + this.logname + "(" + this.address + ":" + this.port + ") " + message);
    }

    public void close()
    {
        Log(Level.INFO, "Attempting to close channel.");
        if (this.channel != null)
        {
            try
            {
                this.channel.close();
            }
            catch (IOException e)
            {
                SysLog(Level.SEVERE, "Failed to close channel: " + e);
            }
        }
    }

    /**
     * Send byte buffer over TCP, including a length header.
     * 
     * @param buffer    the bytes to send
     * @return true if the message was sent successfully
     */
    public boolean sendTCPBytes(ByteBuffer[] srcbuffers, int length)
    {
        boolean success = false;
        try
        {
            ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(length);
            header.flip();
            ByteBuffer[] buffers = new ByteBuffer[1 + srcbuffers.length];
            buffers[0] = header;
            for (int i = 0; i < srcbuffers.length; i++)
                buffers[i + 1] = srcbuffers[i];
            if (TCPUtils.isLogging())
            {
                long t1 = System.nanoTime();
                long bytesWritten = this.channel.write(buffers);
                long t2 = System.nanoTime();
                double rate = 1000.0 * 1000.0 * 1000.0 * (double) (bytesWritten) / (1024.0 * (double) (t2 - t1));
                Log(Level.INFO, "Sent " + bytesWritten + " bytes at " + rate + " Kb/s");
            }
            else
            {
                this.channel.write(buffers);
            }
            success = true;
        }
        catch (Exception e)
        {
            SysLog(Level.SEVERE, "Failed to send TCP bytes: " + e);
        }
        return success;
    }
}