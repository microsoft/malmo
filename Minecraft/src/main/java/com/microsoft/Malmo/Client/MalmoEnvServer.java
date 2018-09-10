package com.microsoft.Malmo.Client;

import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.Utils.TCPUtils;
import net.minecraftforge.common.config.Configuration;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
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
        byte[] obs = null;
        String turnKey = "";
        String info = "";

        LinkedList<String> commands = new LinkedList<String>();
    }

    private static boolean envPolicy = false;

    private Lock lock = new ReentrantLock();
    private Condition cond = lock.newCondition();

    private MissionState missionState = new MissionState();

    private Hashtable<String, Integer> initTokens = new Hashtable<String, Integer>();

    static final long COND_WAIT_SECONDS = 3; // Max wait in seconds before timing out (and replying to RPC).
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
        data = new byte[hdr];
        din.readFully(data);
        String id = new String(data, utf8);
        System.out.println("Mission Init" + id);

        String[] token = id.split(":");

        String experimentId = token[0];
        int role = Integer.parseInt(token[1]);
        int reset = Integer.parseInt(token[2]);
        int agentCount = Integer.parseInt(token[3]);

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

                    started = startUp(command, ipOriginator, experimentId, reset, agentCount, myToken);
                    if (started)
                        initTokens.put(myToken, 0);
                } else {
                    started = true; // Pre-started previously.
                }

                // Check that all previous tokens have been consumed. If not don't proceed to mission.

                allTokensConsumed = areAllTokensConsumed(experimentId, reset, agentCount);
                if (!allTokensConsumed) {
                    try {
                        cond.await(COND_WAIT_SECONDS, TimeUnit.SECONDS);
                    } catch (InterruptedException ie) {
                    }
                    allTokensConsumed = areAllTokensConsumed(experimentId, reset, agentCount);
                }

                if (allTokensConsumed && !initTokens.containsKey(myToken)) {
                    // TODO if start after all consumed (move pre-start to here):
                    // started = startUp(command, ipOriginator, experimentId, reset, agentCount, myToken);
                    // if (started) { initTokens.put(myToken, 0); cond.signalAll(); }
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

    private boolean areAllTokensConsumed(String experimentId, int reset, int agentCount) {
        boolean allTokensConsumed = true;
        for (int i = 1; i < agentCount; i++) {
            String tokenForAgent = experimentId + ":" + i + ":" + (reset - 1);
            if (initTokens.containsKey(tokenForAgent)) {
                System.out.println("Mission init - Unconsumed " + tokenForAgent);
                allTokensConsumed = false;
            }
        }
        return allTokensConsumed;
    }

    private boolean startUp(String command, String ipOriginator, String experimentId, int reset, int agentCount, String myToken) {

        // Clear out mission state
        missionState.reward = 0.0;
        missionState.commands.clear();
        missionState.obs = null;
        missionState.info = "";

        missionState.missionInit = command;
        missionState.done = false;
        missionState.turnKey = "";
        missionState.token = myToken;
        missionState.experimentId = experimentId;
        missionState.agentCount = agentCount;
        missionState.reset = reset;

        return startUpMission(command, ipOriginator);
    }

    private boolean startUpMission(String command, String ipOriginator) {
        // System.out.println("Start up mission");
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

    private static final int stepTagLength = "<step>".length();

    // Handler for <Step> messages.
    private void step(String command, Socket socket, DataInputStream din) throws IOException {
        String actionCommand = command.substring(stepTagLength, command.length() - (stepTagLength + 1));
        // System.out.println("Command (step action): " + actionCommand);

        int hdr;
        byte[] stepTurnKey;
        hdr = din.readInt();
        stepTurnKey = new byte[hdr];
        din.readFully(stepTurnKey);

        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        double reward;
        boolean done;
        byte[] obs;
        String info;
        byte[] currentTurnKey;

        lock.lock();
        try {
            // Get the current observation. If none wait for a short time.
            if (missionState.obs != null) {
                obs = missionState.obs;
            } else {
                try {
                    cond.await(COND_WAIT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                }
                obs = missionState.obs;
            }
            if (obs == null) {
                obs = new byte[0];
            }

            reward = missionState.reward;
            missionState.reward = 0.0;
            info = missionState.info;
            missionState.info = "";
            missionState.obs = null;

            done = missionState.done;
            currentTurnKey = missionState.turnKey.getBytes();

            if (!done && obs.length > 0) {
                if (stepTurnKey.length > 0) {
                    // TODO There is no check that stepTurnKey is still current. In any case:
                    // The step turn key may be stale when picked up from the command queue.
                    missionState.commands.add(new String(stepTurnKey) + " " + actionCommand);
                } else {
                    missionState.commands.add(actionCommand);
                }
            }
        } finally {
            lock.unlock();
        }

        dout.writeInt(obs.length);
        dout.write(obs);

        dout.writeInt(BYTES_INT + BYTES_DOUBLE);
        dout.writeDouble(reward);
        dout.writeInt(done ? 1 : 0);

        byte[] infoBytes = info.getBytes(utf8);
        dout.writeInt(infoBytes.length);
        dout.write(infoBytes);

        dout.writeInt(currentTurnKey.length);
        dout.write(currentTurnKey);
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
            initTokens.remove(previousToken);
            cond.signalAll();

            // Check for next token. Wait for a short time if not already produced.
            port = initTokens.get(token);
            if (port == null) {
                try {
                    cond.await(COND_WAIT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                }
                port = initTokens.get(token);
                if (port == null) {
                    port = 0;
                    System.out.println("Role " + role + " reset " + reset + " waiting for token.");
                }
            }

            if (port != 0) {
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
            missionState.turnKey = "";

            if (missionState.token != null) {
                initTokens.remove(missionState.token);
                missionState.token = null;
                missionState.experimentId = null;
                missionState.agentCount = 0;
                missionState.reset = 0;

                cond.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    // Record a Malmo "observation" json - as the env info since an environment "obs" is a video frame.
    public void observation(String info) {
        // Parsing obs as JSON would be slower but less fragile than extracting the turn_key using string search.
        String pattern = "\"turn_key\":\"";
        int i = info.indexOf(pattern);
        String turnKey = "";
        if (i != -1) {
            turnKey = info.substring(i + pattern.length(), info.length() - 1);
            turnKey = turnKey.substring(0, turnKey.indexOf("\""));
            // System.out.println("Observation turn key: " + turnKey);
        }
        lock.lock();
        try {
            missionState.turnKey = turnKey;
            missionState.info = info; // TODO Info could be costly to send as quite long or could contain restricted info. Make recording configurable.
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
            missionState.obs = frame; // Replaces current. TODO track skipped frames.
            cond.signalAll();
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
                cond.signalAll();
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
            // System.out.println("Add token " + tokenForAgent);
            initTokens.put(tokenForAgent, integratedServerPort);
        }
    }
}
