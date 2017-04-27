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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;

public class TCPSocket
{
    public String address;
    public String logname;
    public int port;
    Socket socket = null;

    /**
     * Create a new socket helper, bound to a specific endpoint, which will keep a connection alive for repeated use.
     * 
     * @param address   address to send the message to
     * @param port      port number to use
     * @param logname      for logging purposes - name describing the purpose of this socket
     */
    public TCPSocket(String address, int port, String logname)
    {
        this.address = address;
        this.port = port;
        this.logname = logname;
        createSocket();
    }

    private void SysLog(Level level, String message)
    {
        TCPUtils.SysLog(level, "<-" + this.logname + " (" + this.address + ":" + this.port + "): " + message);
    }

    private void Log(Level level, String message)
    {
        TCPUtils.Log(level, "<-" + this.logname + " (" + this.address + ":" + this.port + "): " + message);
    }

    public void close()
    {
        if (this.socket != null)
        {
            try
            {
                Log(Level.INFO, "Attempting to close socket... ");
                this.socket.close();
                Log(Level.INFO, "...succeeded.");
            }
            catch (IOException e)
            {
                SysLog(Level.WARNING, "Error closing socket!");
            }
        }
    }

    private void createSocket()
    {
        this.socket = new Socket();
        InetSocketAddress sockaddr = new InetSocketAddress(this.address, this.port);
        Log(Level.INFO, "Attempting to create socket with InetSocketAddress: " + sockaddr + "...");
        try
        {
            this.socket.connect(sockaddr, TCPUtils.DEFAULT_SOCKET_TIMEOUT_MS);
            Log(Level.INFO, "...socket created successfully.");
            return;
        }
        catch (IOException e)
        {
            SysLog(Level.SEVERE, "Failed to create socket: " + e);
        }
        /*
        InetAddress address = sockaddr.getAddress();
        if (address.isLoopbackAddress() && address instanceof Inet6Address)
        {
            System.out.println("Trying again with IPv4 loopback address:");
            try
            {
                this.socket.connect(new InetSocketAddress("127.0.0.1", this.port));
                return;
            }
            catch (IOException e)
            {
                System.out.println("WARNING: Giving up after failed second attempt to create socket: " + e);
            }
        }*/
        this.socket = null;
    }

    /**
     * Send string over TCP to the specified address via the specified port, including a header.
     * 
     * @param message   string to be sent over TCP
     * @return true if message was successfully sent
     */
    public boolean sendTCPString(String message)
    {
        Log(Level.FINE, "About to send: " + message);
        byte[] bytes = message.getBytes();
        return sendTCPBytes(bytes);
    }

    /**
     * Send byte buffer over TCP, including a length header.
     * 
     * @param buffer    the bytes to send
     * @return true if the message was sent successfully
     */
    public boolean sendTCPBytes(byte[] buffer)
    {
        if (this.socket == null)
        {
            Log(Level.WARNING, "Asked to send bytes over null socket!");
            return false; // No socket, nothing will work.
        }

        boolean success = false;
        try
        {
            DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
            if (TCPUtils.isLogging())
            {
                long t1 = System.nanoTime();
                dos.writeInt(buffer.length);
                dos.write(buffer, 0, buffer.length);
                dos.flush();
                long t2 = System.nanoTime();
                double rate = 1000.0 * 1000.0 * 1000.0 * (double) (buffer.length) / (1024.0 * (double) (t2 - t1));
                Log(Level.INFO, "Sent " + buffer.length + " bytes at " + rate + " Kb/s");
            }
            else
            {
                dos.writeInt(buffer.length);
                dos.write(buffer, 0, buffer.length);
            }
            success = true;
        }
        catch (IOException e)
        {
            SysLog(Level.SEVERE, "Failed to send TCP bytes: " + e);
        }
        return success;
    }

    /**
     * Choose a port from the specified range - either sequentially, or at random.
     * 
     * @param minPort     minimum (inclusive) value for port.
     * @param maxPort     max (inclusive) possible port value.
     * @param random      true to allocate based on a random sample; false to allocate sequentially, starting from minPort.
     * @return a ServerSocket.
     */
    public static ServerSocket getSocketInRange(int minPort, int maxPort, boolean random)
    {
        TCPUtils.Log(Level.INFO, "Attempting to create a ServerSocket in range (" + minPort + "-" + maxPort + (random ? ") at random..." : ") sequentially..."));
        ServerSocket s = null;
        int port = minPort - 1;
        Random r = new Random(System.currentTimeMillis());
        while (s == null && port <= maxPort)
        {
            if (random)
                port = minPort + r.nextInt(maxPort - minPort);
            else
                port++;
            try
            {
                TCPUtils.Log(Level.INFO, "    - trying " + port + "...");
                s = new ServerSocket(port);
                TCPUtils.Log(Level.INFO, "Succeeded!");
                return s; // Created okay, so this port is available.
            }
            catch (IOException e)
            {
                // Try the next port.
                TCPUtils.Log(Level.INFO, "    - failed: " + e);
            }
        }
        TCPUtils.Log(Level.SEVERE, "Could find no available port!");
        return null; // No port found in the allowed range.
    }
}
