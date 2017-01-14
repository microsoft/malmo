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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class TCPSocketHelper
{
	public static final int DEFAULT_SOCKET_TIMEOUT_MS = 1000;
	private static Logger logger = Logger.getLogger("com.microsoft.Malmo.TCPSocketHelper");
	private static FileHandler filehandler = null;
	private static boolean logging = false;

	public String address;
	public int port;
	Socket socket = null;
	
	/** Create a new socket helper, bound to a specific endpoint, which will keep a connection alive for repeated use.
     * @param address address to send the message to
     * @param port port number to use
	 */
	public TCPSocketHelper(String address, int port)
	{
		this.address = address;
		this.port = port;
		createSocket();
	}
	
    static void setLogging(boolean log)
    {
        logging = log;
        if (log == true && filehandler == null)
        {
            try
            {
                filehandler = new FileHandler("TCPLog.txt");
                filehandler.setFormatter(new SimpleFormatter());
            }
            catch (SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            logger.addHandler(filehandler);
        }
    }
	
	public void close()
	{
		if (this.socket != null)
		{
			try
			{
				this.socket.close();
			}
			catch (IOException e)
			{
				System.out.println("WARNING: error closing socket: " + e);
			}
		}
	}
	
	private void createSocket()
	{
		this.socket = new Socket();
		try
		{
			this.socket.connect(new InetSocketAddress(this.address, this.port), DEFAULT_SOCKET_TIMEOUT_MS);
		}
		catch (IOException e)
		{
			this.socket = null;
			System.out.println("WARNING: Failed to create socket: " + e);
		}
	}
    
    /** Send string over TCP to the specified address via the specified port, including a header.
     * @param message string to be sent over TCP
     * @return true if message was successfully sent
     */
    public boolean sendTCPString(String message)
    {
        byte[] bytes = message.getBytes();
        return sendTCPBytes(bytes);
    }
    
    /** Send byte buffer over TCP, including a length header.
     * @param buffer the bytes to send
     * @return true if the message was sent successfully
     */
    public boolean sendTCPBytes(byte[] buffer)
    {
        if (this.socket == null)
            return false;	// No socket, nothing will work.

        boolean success = false;
        try
        {
            DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
            if (logging)
            {
                long t1 = System.nanoTime();
                dos.writeInt(buffer.length);
                dos.write(buffer, 0, buffer.length);
                dos.flush();
                long t2 = System.nanoTime();
                double rate = 1000.0 * 1000.0 * 1000.0 * (double)(buffer.length) / (1024.0 * (double)(t2 - t1));
                logger.log(Level.INFO, "Sent " + buffer.length + " bytes to " + this.address + ":" + this.port + " at " + rate + " Kb/s");
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
            System.out.println(String.format("Failed to send TCP bytes to %s:%d.", this.address, this.port));
            System.out.println(e);
        }
        return success;
    }

    /** Choose a port from the specified range - either sequentially, or at random.
     * @param minPort minimum (inclusive) value for port.
     * @param maxPort max (inclusive) possible port value.
     * @param random true to allocate based on a random sample; false to allocate sequentially, starting from minPort.
     * @return a ServerSocket.
     */
    public static ServerSocket getSocketInRange(int minPort, int maxPort, boolean random)
    {
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
    			s = new ServerSocket(port);
    			return s;      // Created okay, so this port is available.
    		}
    		catch (IOException e)
    		{
    			// Try the next port.
    		}
    	}
    	return null;   // No port found in the allowed range.
    }
    
    public static class SocketChannelHelper
    {
    	SocketChannel channel;
    	String address;
    	int port;

    	public SocketChannelHelper(String address, int port)
    	{
    		this.address = address;
    		this.port = port;

    		try
    		{
				this.channel = SocketChannel.open(new InetSocketAddress(address, port));
			}
    		catch (IOException e)
    		{
				e.printStackTrace();
			}
    	}
    	
    	public void close()
    	{
    		if (this.channel != null)
    		{
    			try
    			{
    				this.channel.close();
    			}
    			catch (IOException e)
    			{
    				System.out.println("WARNING: error closing socket: " + e);
    			}
    		}
    	}
    	
	    /** Send byte buffer over TCP, including a length header.
	     * @param buffer the bytes to send
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
                    buffers[i+1] = srcbuffers[i];
                if (logging)
                {
                    long t1 = System.nanoTime();
                    long bytesWritten = this.channel.write(buffers);
                    long t2 = System.nanoTime();
                    double rate = 1000.0 * 1000.0 * 1000.0 * (double)(bytesWritten) / (1024.0 * (double)(t2 - t1));
                    logger.log(Level.INFO, "Sent " + bytesWritten + " bytes to " + this.address + ":" + this.port + " at " + rate + " Kb/s");
                }
                else
                {
                    this.channel.write(buffers);
                }
                success = true;
            }
            catch (Exception e)
            {
                System.out.println(String.format("Failed to send TCP bytes to %s:%d.", this.address, this.port));
                System.out.println(e);
            }
            return success;
        }
    }
    
    /** Simple utility class for providing and tracking tokens.<br>
     * Tokens can be requested and used; unused tokens will be removed after a timeout period has passed.
     */
    public static class TokenMap
    {
    	public final static long TOKEN_SHELF_LIFE_MS = 10000;	// Tokens last ten seconds by default.
    	public final static int CONCURRENT_TOKEN_LIMIT = 1;		// By default only one token allowed at a time.

        private class Token
        {
        	private String ip;
        	private long creationtime;
        	
        	public Token(String ipOfClient)
        	{
        		this.creationtime = System.currentTimeMillis();
        		this.ip = ipOfClient;
        	}
        	
        	public boolean isValid(String ipOfClient)
        	{
        		return this.ip.equals(ipOfClient) && !isExpired();
        	}
        	
        	public boolean isExpired()
        	{
        		return System.currentTimeMillis() - this.creationtime > TOKEN_SHELF_LIFE_MS;
        	}
        }

        private HashMap<String, Token> tokens = new HashMap<String, Token>();
        
        /** Generate a token for this client ip address.
         * @param ipOfClient The client's ip address.
         * @return a hash string which can be returned via the ip address, and subsequently used to retrieve the token, or null if no tokens are available.
         */
        public String createToken(String ipOfClient)
        {
        	cleanExpiredTokens();

        	if (this.tokens.size() < CONCURRENT_TOKEN_LIMIT)
        	{
	        	Token tkn = new Token(ipOfClient);
	    		String tokenuid = UUID.randomUUID().toString();
	    		this.tokens.put(tokenuid,  tkn);
	    		return tokenuid;
        	}
        	return null;
        }

        /** Use the token referred to by the tokenid and ip address. This is a one-shot operation.
         * @param tokenuid the identifier returned by createToken
         * @param ip the ip address from which the token request came
         * @return true if the token is valid, false if there is no token, or it doesn't match the client ip address, or it has expired.
         */
        public boolean useToken(String tokenuid, String ip)
        {
        	cleanExpiredTokens();
        	return true;	// TODO - for now, disable token validation - assume everything is valid (when you're part of a team).
        	
//        	Token tkn = this.tokens.remove(tokenuid);
//        	if (tkn == null)
//        		return false;
//        	return tkn.isValid(ip);
        }

        /** Used internally to keep things tidy - will remove expired tokens from the map.
         */
        private void cleanExpiredTokens()
        {
        	Iterator<Entry<String, Token>> it = this.tokens.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<String, Token> pair = (Map.Entry<String, Token>)it.next();
                if (pair.getValue().isExpired())
                	it.remove();
            }
        }
    }
}