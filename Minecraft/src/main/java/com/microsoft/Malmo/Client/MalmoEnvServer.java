package com.microsoft.Malmo.Client;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Utils.TCPUtils;
import net.minecraftforge.common.config.Configuration;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Hashtable;
import com.microsoft.Malmo.Utils.TCPInputPoller;
import java.util.logging.Level;

import java.util.LinkedList;


/**
 * MalmoEnvServer - multi-agent supporting OpenAI gym "environment" server.
 */
public class MalmoEnvServer {

    private class MissionState {

        // Mission parameters:
        String missionInit = null;
        String token = null;
        String experimentId = null;
        int agentCount = 0;
        int reset = 0;

        // Env state:
        boolean done = false;
        double reward = 0.0;
        byte[] ops = null;

        LinkedList<String> commands = new LinkedList<String>();
    }

    private static boolean envPolicy = false;

    private Lock lock = new ReentrantLock();
    // private Condition cond = lock.newCondition();

    private MissionState missionState = new MissionState();

    private Hashtable<String, Integer> initTokens = new Hashtable<String, Integer>();

    static final int BYTES_INT = 4;
    static final int BYTES_DOUBLE = 8;
    private static final Charset utf8 = Charset.forName("UTF-8");

    private int port;
    private TCPInputPoller missionPoller; // Used for command parsing and not actual communication.

    /***
     * Malmo "Env" service.
     * @param port the port the service listens on.
     * @param missionPoller for plugging into existing comms handling.
     */
    public MalmoEnvServer(int port, TCPInputPoller missionPoller) {
        this.missionPoller = missionPoller;
        this.port = port;
    }

    /** Initialize malmo env configuration. For now either on or "legacy" AgentHost protocol.*/
    static public void update(Configuration configs)
    {
        envPolicy = configs.get(MalmoMod.ENV_CONFIGS, "env", "false").getBoolean();
        System.out.println("read env policy " + envPolicy);
    }

    public static boolean isEnv() {
        return envPolicy;
    }

    /**
     * Start servicing the MalmoEnv protocol.
     * @throws IOException
     */
    public void serve() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            try {
                final Socket socket = serverSocket.accept();

                Thread thread = new Thread("EnvServerSocketHandler") {
                    public void run() {
                        try {
                            while (true) {
                                DataInputStream din = new DataInputStream(socket.getInputStream());
                                int hdr = din.readInt();
                                // System.out.println("Hdr " + hdr);
                                byte[] data = new byte[hdr];
                                din.readFully(data);

                                String command = new String(data, utf8);

                                if (command.startsWith("<Init")) {

                                    init(command, socket);

                                }
                                if (command.startsWith("<Find")) {

                                    find(command, socket);

                                } else if (command.startsWith("<MissionInit")) {

                                    missionInit(din, command, socket);

                                } else if (command.startsWith("<Step")) {

                                    step(command, socket, din);

                                } else if (command.startsWith("<Exit")) {

                                    exit(command, socket);

                                } else if (command.startsWith("<Echo")) {
                                    command = "<Echo>" + command + "</Echo>";
                                    data = command.getBytes(utf8);
                                    hdr = data.length;

                                    DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                                    dout.writeInt(hdr);
                                    dout.write(data, 0, hdr);
                                    dout.flush();
                                } else {
                                    throw new IOException("Unknown command");
                                }
                            }
                        } catch (IOException ioe) {
                            System.out.println("MalmoEnv socket error: " + ioe + " (can be on disconnect)");
                            try {
                                socket.close();
                            } catch (IOException ioe2) {
                            }
                        }
                    }
                };
                thread.start();
            } catch (IOException ioe) {
                TCPUtils.Log(Level.SEVERE, "MalmoEnv service exits on " + ioe);
            }
        }
    }

    // Handler for <MissionInit> messages.
    private void missionInit(DataInputStream din, String command, Socket socket) throws IOException {

        String ipOriginator = socket.getInetAddress().getHostName();

        int hdr;
        byte[] data;// Read the token.
        hdr = din.readInt();
        System.out.println("Hdr2 " + hdr);
        data = new byte[hdr];
        din.readFully(data);
        String id = new String(data, utf8);
        System.out.println(id);

        String[] token = id.split(":");

        String experimentId = token[0];
        int role = Integer.parseInt(token[1]);
        int reset = Integer.parseInt(token[2]);
        int agentCount = Integer.parseInt(token[3]);

        // System.out.println(experimentId + ":" + role + ":" + reset);

        port = -1;
        boolean allTokensConsumed = true;
        boolean started = false;

        lock.lock();
        try {
            if (role == 0) {

                String previousToken = experimentId + ":0:" + (reset - 1);
                // System.out.println("Purge own " + previousToken);
                initTokens.remove(previousToken);

                String myToken = experimentId + ":0:" + reset;
                if (!initTokens.containsKey(myToken)) {
                    System.out.println("(Pre)Start " + role + " reset " + reset);

                    initTokens.put(myToken, 0);
                    started = startUp(command, ipOriginator, experimentId, reset, agentCount, myToken);
                }

                // Check that all previous tokens have been consumed. If not don't proceed to mission.
                for (int i = 1; i < agentCount; i++) {
                    String tokenForAgent = experimentId + ":" + i + ":" + (reset - 1);
                    System.out.println("check " + tokenForAgent);
                    if (initTokens.containsKey(tokenForAgent)) {
                        System.out.println("Mission init blocked on Unconsumed " + tokenForAgent);
                        allTokensConsumed = false;
                    }
                }

                // TODO could wait locally while not allTokensConsumed.

                if (allTokensConsumed && !initTokens.containsKey(myToken)) {
                    // TODO if start after all consumed (move pre-start to here):
                    // initTokens.put(myToken, 0);
                    // started = startUp(command, ipOriginator, experimentId, reset, agentCount, myToken);
                }
            } else {
                System.out.println("Start " + role + " reset " + reset);

                started = startUp(command, ipOriginator, experimentId, reset, agentCount, experimentId + ":" + role + ":" + reset);
            }
        } finally {
            lock.unlock();
        }

        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        dout.writeInt(BYTES_INT);
        dout.writeInt(allTokensConsumed && started ? 1 : 0);
        dout.flush();

        byte[] turnKey = "".getBytes(); // TODO get initial turn key
        dout.writeInt(turnKey.length);
        dout.write(turnKey);
        dout.flush();
    }

    private boolean startUp(String command, String ipOriginator, String experimentId, int reset, int agentCount, String myToken) {

        // Clear out mission state
        missionState.reward = 0.0;
        missionState.commands.clear();
        missionState.ops = null;

        missionState.missionInit = command;
        missionState.done = false;
        missionState.token = myToken;
        missionState.experimentId = experimentId;
        missionState.agentCount = agentCount;
        missionState.reset = reset;

        return startUpMission(command, ipOriginator);
    }

    private boolean startUpMission(String command, String ipOriginator) {
        System.out.println("start up mission");
        if (missionPoller == null)
            return false;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        missionPoller.commandReceived(command, ipOriginator, dos);
        try {
            dos.flush();
            byte[] reply = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(reply);
            DataInputStream dis = new DataInputStream(bais);
            int hdr = dis.readInt();
            byte[] replyBytes = new byte[hdr];
            dis.readFully(replyBytes);
            System.out.println("start up reply " + new String(replyBytes));
            if (new String(replyBytes).equals("MALMOOK")) {
                System.out.println("MalmoEnvServer Mission starting ...");
                return true;
            }
        } catch (IOException ioe) {
        }
        return false;
    }

    // Handler for <Step> messages.
    private void step(String command, Socket socket, DataInputStream din) throws IOException {
        String actionCommand = command.substring(6, command.length() - 7);

        System.out.println("Command (step action): " + actionCommand);

        int hdr;
        byte[] turnKey;// Read the turn key.
        hdr = din.readInt();
        turnKey = new byte[hdr];
        din.readFully(turnKey);

        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        double reward;
        boolean done;
        byte[] obs;

        lock.lock();
        try {
            if (missionState.ops != null) {
                obs = missionState.ops;
            } else {
                obs = new byte[0];
            }

            reward = missionState.reward;
            missionState.reward = 0.0;
            missionState.ops = null;

            done = missionState.done;
            if (!done && obs.length > 0) {
                missionState.commands.add(actionCommand);
            }
        } finally {
            lock.unlock();
        }

        dout.writeInt(obs.length);
        dout.write(obs);

        dout.writeInt(BYTES_INT + BYTES_DOUBLE);
        dout.writeDouble(reward);
        dout.writeInt(done ? 1 : 0);

        turnKey = "".getBytes(); // TODO get turn key
        dout.writeInt(turnKey.length);
        dout.write(turnKey);
        dout.flush();
    }

    // Handler for <Find> messages - used by non-zero roles to discover integrated server port from primary (role 0) service.
    private void find(String command, Socket socket) throws IOException {

        Integer port;
        lock.lock();
        try {
            String token = command.substring(6, command.length() - 7);

            // System.out.println("Find? " + token);

            // Purge previous token.
            String[] tokenSplits = token.split(":");
            String experimentId = tokenSplits[0];
            int role = Integer.parseInt(tokenSplits[1]);
            int reset = Integer.parseInt(tokenSplits[2]);

            String previousToken = experimentId + ":" + role + ":" + (reset - 1);
            // System.out.println("Purge " + previousToken);
            initTokens.remove(previousToken);

            // Check for next token.
            port = initTokens.get(token);
            if (port == null) {
                port = 0;
                // TODO could wait a while but not forever.
                System.out.println("Role " + role + " reset " + reset + " waiting for token.");
            } else {
                System.out.println("Found " + port);
            }
        } finally {
            lock.unlock();
        }

        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        dout.writeInt(BYTES_INT);
        dout.writeInt(port);
        dout.flush();
    }

    // Handler for <Init> messages. These reset the service so use with care!
    private void init(String command, Socket socket) throws IOException {
        lock.lock();
        try {
            initTokens = new Hashtable<String, Integer>();

            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
            dout.writeInt(BYTES_INT);
            dout.writeInt(1);
            dout.flush();
        } finally {
            lock.unlock();
        }
    }

    // Handler for <Exit> messages. These "kill the service" temporarily so use with care!
    private void exit(String command, Socket socket) throws IOException {
        lock.lock();
        try {
             // We may exit before we get a chance to reply.

            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
            dout.writeInt(BYTES_INT);
            dout.writeInt(1);
            dout.flush();

            ClientStateMachine.exitJava();

        } finally {
            lock.unlock();
        }
    }

    // Malmo client methods:

    public String getCommand() {
        lock.lock();
        try {
            String command = missionState.commands.poll();
            if (command == null)
                return "";
            else
                return command;
        } finally {
            lock.unlock();
        }
    }

    public void endMission() {
        lock.lock();
        try {
            missionState.done = true;
            missionState.missionInit = null;

            if (missionState.token != null) {
                initTokens.remove(missionState.token);
                missionState.token = null;
                missionState.experimentId = null;
                missionState.agentCount = 0;
                missionState.reset = 0;
            }
        } finally {
            lock.unlock();
        }
    }

    public void addRewards(double rewards) {
        lock.lock();
        try {
            missionState.reward += rewards;
        } finally {
            lock.unlock();
        }
    }

    public void addFrame(byte[] frame) {
        lock.lock();
        try {
            missionState.ops = frame; // Replaces current. TODO track skipped frames.
        } finally {
            lock.unlock();
        }
    }

    public void notifyIntegrationServerStarted(int integrationServerPort) {
        lock.lock();
        try {
            if (missionState.token != null) {
                System.out.println("Integration server start up - token: " + missionState.token);
                addTokens(integrationServerPort, missionState.token, missionState.experimentId, missionState.agentCount, missionState.reset);
            } else {
                System.out.println("No mission token on integration server start up!");
            }
        } finally {
            lock.unlock();
        }
    }

    private void addTokens(int integratedServerPort, String myToken, String experimentId, int agentCount, int reset) {
        initTokens.put(myToken, integratedServerPort);
        // Place tokens for other agents to find.
        for (int i = 1; i < agentCount; i++) {
            String tokenForAgent = experimentId + ":" + i + ":" + reset;
            System.out.println("add token " + tokenForAgent);
            initTokens.put(tokenForAgent, integratedServerPort);
        }
    }
}
