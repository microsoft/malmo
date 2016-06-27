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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/** Class which polls for TCP commands in the background, and makes them available via a thread-safe queue.<br>
 * Used for receiving control commands from the Malmo code.
 */
public class TCPInputPoller extends Thread
{
    public class CommandAndIPAddress
    {
        public String command;
        public String ipAddress;
        CommandAndIPAddress(String command, String ipAddress)
        {
            this.command = command;
            this.ipAddress = ipAddress;
        }
    }

    private boolean keepRunning = true;
    private ArrayList<CommandAndIPAddress> commandQueue;
    private int requestedPortNumber;	// Can be 0, meaning allocate one dynamically.
    private int portRangeMin = -1;
    private int portRangeMax = -1;
    private boolean choosePortRandomly = false;
    private ServerSocket serverSocket;
    private boolean failedToCreate = false;

    /**
     * Manually add a command to the command queue.<br>
     * Useful for testing, and for times when the mod might want to add its own commands.
     * @param s	command - ie "strafe 0.5" etc.
     */
    public void addCommand(String s)
    {
        synchronized(this)
        {
            this.commandQueue.add(new CommandAndIPAddress(s, ""));
        }
    }

    /** Create a new TCPInputPoller to sit and await messages on the specified port.
     * @param port port to listen on.
     */
    public TCPInputPoller(int port)
    {
        this.requestedPortNumber = port;
        this.commandQueue = new ArrayList<CommandAndIPAddress>();
    }

    /** Create a new TCPInputPoller to sit and await messages on a port which is dynamically allocated from a range.
     * @param portmin minimum valid port number (inclusive)
     * @param portmax maximum valid port number (inclusive)
     * @param choosePortRandomly if true, choose a free port from the range at random; otherwise choose the next free port in the range.
     */
    public TCPInputPoller(int portmin, int portmax, boolean choosePortRandomly)
    {
        this.requestedPortNumber = 0;	// 0 means allocate dynamically.
        this.portRangeMax = portmax;
        this.portRangeMin = portmin;
        this.choosePortRandomly = choosePortRandomly;
        this.commandQueue = new ArrayList<CommandAndIPAddress>();
    }

    /** Create a new TCPInputPoller to sit and await messages on the port which is either specified, or chosen from the range.
     * @param requestedPort if non-zero, the specific port to use - otherwise choose a port sequentially from the range.
     * @param portmin minimum valid port number (inclusive)
     * @param portmax maximum valid port number (inclusive)
     */
    public TCPInputPoller(int requestedPort, int portmin, int portmax)
    {
        this.requestedPortNumber = requestedPort;
        this.portRangeMax = Math.max(portmin,  portmax);
        this.portRangeMin = Math.min(portmin,  portmax);
        this.commandQueue = new ArrayList<CommandAndIPAddress>();
    }

    /** Pop the oldest command from our list and return it.
     * @return the oldest unhandled command in our list
     */
    public String getCommand()
    {
        String command = "";
        synchronized(this)
        {
            if (commandQueue.size() > 0)
            {
                command = commandQueue.remove(0).command;
            }
        }
        return command;
    }

    /** Remove all commands from the queue.
     */
    public void clearCommands()
    {
        synchronized(this)
        {
            System.out.println("JETTISONING " + commandQueue.size() + " COMMANDS");
            commandQueue.clear();
        }
    }

    /** Pop the oldest command from our list and return it.
     * @return the oldest unhandled command in our list
     */
    public CommandAndIPAddress getCommandAndIPAddress()
    {
        CommandAndIPAddress command = null;
        synchronized(this)
        {
            if (commandQueue.size() > 0)
            {
                command = commandQueue.remove(0);
            }
        }
        return command;
    }

    /** Immediately stop waiting for messages, and close the SocketServer.
     */
    public void stopServer()
    {
        keepRunning = false;
        // Thread will be blocked waiting for input - unblock it by closing the socket underneath it:
        if (this.serverSocket != null)
        {
            try
            {
                this.serverSocket.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.serverSocket = null;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     * Start waiting for TCP messages.
     */
    public void run()
    {
        this.serverSocket = null;
        try
        {
            // If requrestedPortNumber is 0 and we have a range of ports specified, then attempt to allocate a port dynamically from that range.
            if (this.requestedPortNumber == 0 && this.portRangeMax != -1 && this.portRangeMin != -1)
                this.serverSocket = TCPSocketHelper.getSocketInRange(this.portRangeMin, this.portRangeMax, this.choosePortRandomly);
            else	// Attempt to use the requested port - if it's 0, the system will allocate one dynamically.
                this.serverSocket = new ServerSocket(this.requestedPortNumber);	// Use the specified port number
        }
        catch (Exception e)
        {
            System.out.println("Failed to create socket server on port " + requestedPortNumber);
            this.failedToCreate = true;
            return;
        }

        System.out.println("Listening for messages on port " + this.serverSocket.getLocalPort());

        while (keepRunning)
        {
            Socket socket = null;
            try
            {
                socket = this.serverSocket.accept();
            }
            catch (SocketException e)
            {
                System.out.println("No socket found - ServerSocket was probably closed under our feet - normal for stopping polling.");
            }
            catch (IOException e)
            {
                System.out.println("Failed to accept socket request.");
            }

            if (socket != null)
            {
                Runnable connectionHandler = new TCPConnectionHandler(socket, this);
                new Thread(connectionHandler).start();
            }
        }

        if (this.serverSocket != null)
        {
            try
            {
                this.serverSocket.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    protected void commandReceived(String command, String ipOriginator, DataOutputStream dos)
    {
        synchronized(this)
        {
            if (onCommand(command, ipOriginator, dos))
            {
                // Add this command to our list - the calling thread will
                // retrieve it via getCommand().
                commandQueue.add(new CommandAndIPAddress(command, ipOriginator));
            }
        }
    }

    /** Override this if you want instant notification of each command as it comes in.
     * @param command the command just received
     * @param ipFrom the IP Address which sent the command
     * @param dos a stream for sending data back to the originating socket
     * @return true to allow the command to be queued; false to squelch it.
     */
    public boolean onCommand(String command, String ipFrom, DataOutputStream dos)
    {
        return true;
    }

    /** Override this if you want notification of errors in the input stream.
     * @param error the error that occurred
     * @param dos a stream for sending data back to the originating socket
     */
    public void onError(String error, DataOutputStream dos)
    {
    }

    /** Get the port number which is actually being used by the SocketServer<br>
     * ***If the server hasn't yet bound to a port, this will return -1.***
     * @return the port number in use, or -1 if no port has been bound yet.
     */
    public int getPort()
    {
        if (this.serverSocket == null)
            return -1;
        return this.serverSocket.getLocalPort();	// Will return -1 if not bound.
    }

    /** Get the port number which is actually being used by the SocketServer<br>
     * If the server hasn't yet bound to a port, wait until it does.
     * @return the port number in use, or -1 if the socket failed to bind.
     */
    public int getPortBlocking()
    {
        synchronized (this)
        {
            while (getPort() == -1)
            {
                if (this.failedToCreate)
                    return -1;
                try
                {
                    this.wait(10);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return getPort();
    }

    /** Class which handles the socket connection, getting messages from it and forwarding them on to the TCPInputPoller.
     */
    public class TCPConnectionHandler extends Thread
    {
        private Socket socket;
        TCPInputPoller poller;

        public TCPConnectionHandler(Socket socket, TCPInputPoller poller)
        {
            this.socket = socket;
            this.poller = poller;
        }

        public void run()
        {
            final int MAX_STR_LEN = 10000000;
            try
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                StringBuffer sb = new StringBuffer();
                int intC;
                while ((intC = br.read()) != -1)
                {
                    char c = (char) intC;
                    if (c == '\n')
                    {
                        String command = sb.toString();
                        String ipOriginator = this.socket.getInetAddress().getHostAddress();
                        DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
                        poller.commandReceived(command, ipOriginator, dos);
                        sb.setLength(0);
                    }
                    else 
                    {
                        sb.append(c);
                    }
                    if (sb.length() >= MAX_STR_LEN) {
                        DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
                        poller.onError("MALMOERROR Input too long", dos);
                        break; // discard anything else we received
                    }
                }
            }
            catch (IOException e)
            {
                System.out.println("~~~~~~~~ Socket stream error: " + e);
            }
        }
    }
}
