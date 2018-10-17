package com.microsoft.Malmo.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class TCPSocketChannel
{
    private AsynchronousSocketChannel channel;
    private String address;
    private int port;
    private String logname;

    /**
     * Create a TCPSocketChannel that is blocking but times out connects and writes.
     * @param address The address to connect to.
     * @param port The port to connect to. 0 value means don't open.
     * @param logname A name to use for logging.
     */
    public TCPSocketChannel(String address, int port, String logname) {
        this.address = address;
        this.port = port;
        this.logname = logname;

        try {
            connectWithTimeout();
        } catch (IOException e) {
            Log(Level.SEVERE, "Failed to connectWithTimeout AsynchronousSocketChannel: " + e);
        } catch (ExecutionException e) {
            Log(Level.SEVERE, "Failed to connectWithTimeout AsynchronousSocketChannel: " + e);
        } catch (InterruptedException e) {
            Log(Level.SEVERE, "Failed to connectWithTimeout AsynchronousSocketChannel: " + e);
        } catch (TimeoutException e) {
            Log(Level.SEVERE, "AsynchronousSocketChannel connectWithTimeout timed out: " + e);
        }
    }

    public int getPort() { return port; }

    public String getAddress() { return address; }

    public boolean isValid() { return channel != null; }

    public boolean isOpen() { return channel.isOpen(); }

    private void Log(Level level, String message)
    {
        TCPUtils.Log(level, "<-" + this.logname + "(" + this.address + ":" + this.port + ") " + message);
    }

    private void SysLog(Level level, String message)
    {
        TCPUtils.SysLog(level, "<-" + this.logname + "(" + this.address + ":" + this.port + ") " + message);
    }

    private void connectWithTimeout() throws  IOException, ExecutionException, InterruptedException, TimeoutException {
        if (port == 0)
            return;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(address, port);
        Log(Level.INFO, "Attempting to open SocketChannel with InetSocketAddress: " + inetSocketAddress);
        this.channel = AsynchronousSocketChannel.open();
        Future<Void> connected = this.channel.connect(inetSocketAddress);
        connected.get(TCPUtils.DEFAULT_SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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
     * Send string over TCP to the specified address via the specified port, including a header.
     *
     * @param message string to be sent over TCP
     * @return true if message was successfully sent
     */
    public boolean sendTCPString(String message)
    {
        return sendTCPString(message, 0);
    }

    /**
     * Send string over TCP to the specified address via the specified port, including a header.
     *
     * @param message string to be sent over TCP
     * @param retries number of times to retry in event of failure
     * @return true if message was successfully sent
     */
    public boolean sendTCPString(String message, int retries)
    {
        Log(Level.FINE, "About to send: " + message);
        byte[] bytes = message.getBytes();
        return sendTCPBytes(bytes, retries);
    }

    /**
     * Send byte buffer over TCP, including a length header.
     *
     * @param buffer the bytes to send
     * @return true if the message was sent successfully
     */
    public boolean sendTCPBytes(byte[] buffer)
    {
        return sendTCPBytes(buffer, 0);
    }

    /**
     * Send byte buffer over TCP, including a length header.
     *
     * @param bytes the bytes to send
     * @param retries number of times to retry in event of failure
     * @return true if the message was sent successfully
     */
    public boolean sendTCPBytes(byte[] bytes, int retries) {
        try {
            ByteBuffer header = createHeader(bytes.length);

            safeWrite(header);

            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            safeWrite(buffer);

        } catch (Exception e) {
            SysLog(Level.SEVERE, "Failed to send TCP bytes" + (retries > 0 ? " -- retrying " : "") + ": " + e);

            try {
                channel.close();
            } catch (IOException ioe) {
            }

            if (retries > 0) {
                try {
                    connectWithTimeout();
                } catch (Exception connectException) {
                    SysLog(Level.SEVERE, "Failed to reconnect: " + connectException);
                    return false;
                }
                return sendTCPBytes(bytes, retries - 1);
            }

            return false;
        }
        return true;
    }

    /**
     * Send byte buffer over TCP, including a length header.
     * 
     * @param srcbuffers the bytes to send
     * @return true if the message was sent successfully
     */
    public boolean sendTCPBytes(ByteBuffer[] srcbuffers, int length)
    {
        boolean success = false;
        try
        {
            ByteBuffer header = createHeader(length);
            ByteBuffer[] buffers = new ByteBuffer[1 + srcbuffers.length];
            buffers[0] = header;
            for (int i = 0; i < srcbuffers.length; i++)
                buffers[i + 1] = srcbuffers[i];
            if (TCPUtils.isLogging())
            {
                long t1 = System.nanoTime();
                long bytesWritten = write(buffers);
                long t2 = System.nanoTime();
                double rate = 1000.0 * 1000.0 * 1000.0 * (double) (bytesWritten) / (1024.0 * (double) (t2 - t1));
                Log(Level.INFO, "Sent " + bytesWritten + " bytes at " + rate + " Kb/s");
            }
            else
            {
                write(buffers);
            }
            success = true;
        }
        catch (Exception e)
        {
            SysLog(Level.SEVERE, "Failed to send TCP bytes: " + e);
            try { channel.close(); } catch (IOException ioe) {}
        }
        return success;
    }

    private ByteBuffer createHeader(int length) {
        ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(length);
        header.flip();
        return header;
    }

    private void safeWrite(ByteBuffer buffer) throws InterruptedException, TimeoutException, ExecutionException, IOException {
        while (buffer.remaining() > 0) {
            Future<Integer>  future = this.channel.write(buffer);
            int bytesWritten = future.get(TCPUtils.DEFAULT_SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (bytesWritten == 0) {
                throw new IOException("async write failed to send any bytes.");
            }
        }
    }

    private long write(ByteBuffer[] buffers) throws InterruptedException, TimeoutException, ExecutionException, IOException {
        long bytesWritten = 0;
        for (ByteBuffer b : buffers) {
            bytesWritten += b.remaining();
            safeWrite(b);
        }
        return bytesWritten;
    }
}
