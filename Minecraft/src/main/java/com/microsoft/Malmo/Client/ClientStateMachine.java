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

package com.microsoft.Malmo.Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

import org.xml.sax.SAXException;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.IState;
import com.microsoft.Malmo.MalmoMod;
import com.microsoft.Malmo.MalmoMod.IMalmoMessageListener;
import com.microsoft.Malmo.MalmoMod.MalmoMessageType;
import com.microsoft.Malmo.StateEpisode;
import com.microsoft.Malmo.StateMachine;
import com.microsoft.Malmo.Client.MalmoModClient.InputType;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.MissionHandlers.MissionBehaviour;
import com.microsoft.Malmo.MissionHandlers.MultidimensionalReward;
import com.microsoft.Malmo.Schemas.AgentHandlers;
import com.microsoft.Malmo.Schemas.AgentSection;
import com.microsoft.Malmo.Schemas.ClientAgentConnection;
import com.microsoft.Malmo.Schemas.MinecraftServerConnection;
import com.microsoft.Malmo.Schemas.Mission;
import com.microsoft.Malmo.Schemas.MissionEnded;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.MissionResult;
import com.microsoft.Malmo.Schemas.ModSettings;
import com.microsoft.Malmo.Utils.AddressHelper;
import com.microsoft.Malmo.Utils.AuthenticationHelper;
import com.microsoft.Malmo.Utils.SchemaHelper;
import com.microsoft.Malmo.Utils.ScreenHelper;
import com.microsoft.Malmo.Utils.ScreenHelper.TextCategory;
import com.microsoft.Malmo.Utils.TCPInputPoller;
import com.microsoft.Malmo.Utils.TCPInputPoller.CommandAndIPAddress;
import com.microsoft.Malmo.Utils.TCPSocketHelper;
import com.microsoft.Malmo.Utils.TimeHelper;

/**
 * Class designed to track and control the state of the mod, especially regarding mission launching/running.<br>
 * States are defined by the MissionState enum, and control is handled by
 * MissionStateEpisode subclasses. The ability to set the state directly is
 * restricted, but hooks such as onPlayerReadyForMission etc are exposed to
 * allow subclasses to react to certain state changes.<br>
 * The ProjectMalmo mod app class inherits from this and uses these hooks to run missions.
 */
public class ClientStateMachine extends StateMachine implements IMalmoMessageListener
{
    private static final String MISSING_MCP_PORT_ERROR = "no_mcp";
    private static final String INFO_MCP_PORT = "info_mcp";

    private MissionInit currentMissionInit = null; // The MissionInit object for the mission currently being loaded/run.
    private MissionBehaviour missionBehaviour = new MissionBehaviour();
    private String missionQuitCode = ""; // The reason why this mission ended.
    private MultidimensionalReward finalReward = new MultidimensionalReward(); // The reward at the end of the mission, sent separately to ensure timely delivery.
    private ScreenHelper screenHelper = new ScreenHelper();
    protected MalmoModClient inputController;

    // Socket stuff:
    protected TCPInputPoller missionPoller;
    protected TCPInputPoller controlInputPoller;
    protected int integratedServerPort;

    public ClientStateMachine(ClientState initialState, MalmoModClient inputController)
    {
        super(initialState);
        this.inputController = inputController;

        // Register ourself on the event busses, so we can harness the client tick:
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_TEXT);
    }

    @Override
    public void clearErrorDetails()
    {
        super.clearErrorDetails();
        this.missionQuitCode = "";
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev)
    {
        // Use the client tick to ensure we regularly update our state (from the client thread)
        updateState();
    }

    public ScreenHelper getScreenHelper()
    {
        return screenHelper;
    }

    @Override
    public void onMessage(MalmoMessageType messageType, Map<String, String> data)
    {
        if (messageType == MalmoMessageType.SERVER_TEXT)
        {
            String chat = data.get("chat");
            if (chat != null)
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(chat), 1);
            else
            {
                String text = data.get("text");
                ScreenHelper.TextCategory category = ScreenHelper.TextCategory.valueOf(data.get("category"));
                String strtime = data.get("displayTime");
                Integer time = (strtime != null) ? Integer.valueOf(strtime) : null;
                this.getScreenHelper().addFragment(text, category, time);
            }
        }
    }

    @Override
    protected String getName()
    {
        return "CLIENT";
    }

    @Override
    protected void onPreStateChange(IState toState)
    {
        this.getScreenHelper().addFragment("CLIENT: " + toState, ScreenHelper.TextCategory.TXT_CLIENT_STATE, "");
    }

    /**
     * Create the episode object for the requested state.
     * 
     * @param state the state the mod is entering
     * @return a MissionStateEpisode that localises all the logic required to run this state
     */
    @Override
    protected StateEpisode getStateEpisodeForState(IState state)
    {
        if (!(state instanceof ClientState))
            return null;

        ClientState cs = (ClientState) state;
        switch (cs)
        {
        case WAITING_FOR_MOD_READY:
            return new InitialiseClientModEpisode(this);
        case DORMANT:
            return new DormantEpisode(this);
        case CREATING_HANDLERS:
            return new CreateHandlersEpisode(this);
        case EVALUATING_WORLD_REQUIREMENTS:
            return new EvaluateWorldRequirementsEpisode(this);
        case PAUSING_OLD_SERVER:
            return new PauseOldServerEpisode(this);
        case CLOSING_OLD_SERVER:
            return new CloseOldServerEpisode(this);
        case CREATING_NEW_WORLD:
            return new CreateWorldEpisode(this);
        case WAITING_FOR_SERVER_READY:
            return new WaitingForServerEpisode(this);
        case RUNNING:
            return new MissionRunningEpisode(this);
        case IDLING:
            return new MissionIdlingEpisode(this);
        case MISSION_ENDED:
            return new MissionEndedEpisode(this, MissionResult.ENDED, false, false, true);
        case ERROR_DUFF_HANDLERS:
            return new MissionEndedEpisode(this, MissionResult.MOD_FAILED_TO_INSTANTIATE_HANDLERS, true, true, true);
        case ERROR_INTEGRATED_SERVER_UNREACHABLE:
            return new MissionEndedEpisode(this, MissionResult.MOD_SERVER_UNREACHABLE, true, true, true);
        case ERROR_NO_WORLD:
            return new MissionEndedEpisode(this, MissionResult.MOD_HAS_NO_WORLD_LOADED, true, true, true);
        case ERROR_CANNOT_CREATE_WORLD:
            return new MissionEndedEpisode(this, MissionResult.MOD_FAILED_TO_CREATE_WORLD, true, true, true);
        case ERROR_CANNOT_START_AGENT: // run-on deliberate
        case ERROR_LOST_AGENT:
            return new MissionEndedEpisode(this, MissionResult.MOD_HAS_NO_AGENT_AVAILABLE, true, true, false);
        case ERROR_LOST_NETWORK_CONNECTION: // run-on deliberate
        case ERROR_CANNOT_CONNECT_TO_SERVER:
            return new MissionEndedEpisode(this, MissionResult.MOD_CONNECTION_FAILED, true, false, true); // No point trying to inform the server - we can't reach it anyway!
        case MISSION_ABORTED:
            return new MissionEndedEpisode(this, MissionResult.MOD_SERVER_ABORTED_MISSION, true, false, true);  // Don't inform the server - it already knows (we're acting on its notification)
        case WAITING_FOR_SERVER_MISSION_END:
            return new WaitingForServerMissionEndEpisode(this);
        default:
            break;
        }
        return null;
    }

    protected MissionInit currentMissionInit()
    {
        return this.currentMissionInit;
    }

    protected MissionBehaviour currentMissionBehaviour()
    {
        return this.missionBehaviour;
    }

    protected class MissionInitResult
    {
        public MissionInit missionInit = null;
        public boolean wasMissionInit = false;
        public String error = null;
    }

    protected MissionInitResult decodeMissionInit(String command)
    {
        MissionInitResult result = new MissionInitResult();
        if (command == null)
        {
            result.error = "Null command passed.";
            return result;
        }

        String rootNodeName = SchemaHelper.getRootNodeName(command);
        if (rootNodeName != null && rootNodeName.equals("MissionInit"))
        {
            result.wasMissionInit = true;
            // Attempt to decode the MissionInit XML string.
            try
            {
                result.missionInit = (MissionInit) SchemaHelper.deserialiseObject(command, "MissionInit.xsd", MissionInit.class);
            }
            catch (JAXBException e)
            {
                System.out.println("JAXB exception: " + e);
                if (e.getMessage() != null)
                    result.error = e.getMessage();
                else if (e.getLinkedException() != null && e.getLinkedException().getMessage() != null)
                    result.error = e.getLinkedException().getMessage();
                else
                    result.error = "Unspecified problem parsing MissionInit - check your Mission xml.";
            }
            catch (SAXException e)
            {
                System.out.println("SAX exception: " + e);
                result.error = e.getMessage();
            }
            catch (XMLStreamException e)
            {
                System.out.println("XMLStreamException: " + e);
                result.error = e.getMessage();
            }
        }
        return result;
    }

    protected boolean areMissionsEqual(Mission m1, Mission m2)
    {
        // TODO - compare missions.
        // A simple string comparison on the serialisations won't necessarily work, since there are
        // variations in the way JAXB, CodeSynthesis etc perform the serialisation/deserialisation.
        // For the time being, we just return true here.
        return true;
    }

    /**
     * Set up the mission poller.<br>
     * This is called during the initialisation episode, but also needs to be
     * available for other episodes in case the configuration changes, resulting
     * in changes to the ports.
     * 
     * @throws UnknownHostException
     */
    protected void initialiseComms() throws UnknownHostException
    {
        // Start polling for missions:
        if (this.missionPoller != null)
        {
            this.missionPoller.stopServer();
        }

        this.missionPoller = new TCPInputPoller(AddressHelper.getMissionControlPortOverride(), AddressHelper.MIN_MISSION_CONTROL_PORT, AddressHelper.MAX_FREE_PORT)
        {
            @Override
            public void onError(String error, DataOutputStream dos)
            {
                System.out.println("SENDING ERROR: " + error);
                try
                {
                    dos.writeInt(error.length());
                    dos.writeBytes(error);
                }
                catch (IOException e)
                {
                }
            }

            @Override
            public boolean onCommand(String command, String ipFrom, DataOutputStream dos)
            {
                System.out.println("Received from " + ipFrom + ":");
                System.out.println(command);

                String reply = null;
                boolean keepProcessing = true;

                // See if we've been sent a MissionInit message:
                MissionInitResult missionInitResult = decodeMissionInit(command);
                if (missionInitResult.wasMissionInit && missionInitResult.missionInit == null)
                {
                    reply = "MALMOERROR" + missionInitResult.error;
                    keepProcessing = false; // Got sent a duff MissionInit xml - pass back the JAXB/SAXB errors.
                }
                else if (!missionInitResult.wasMissionInit)
                {
                    reply = "MALMOERRORNOTUNDERSTOOD";
                    keepProcessing = false; // Don't understand this message - not a MissionInit.
                }
                else if (missionInitResult.missionInit != null)
                {
                    MissionInit missionInit = missionInitResult.missionInit;
                    String rootNodeName = SchemaHelper.getRootNodeName(command);
                    if (rootNodeName != null && rootNodeName.equals("MissionInit"))
                    {
                        // We've been sent a MissionInit message.
                        // If the server information is missing, we interpret this as a request to find it.
                        // (Unless the requested role is 0, in which case the server information *should* be missing.)
                        if (missionInit.getMinecraftServerConnection() == null && missionInit.getClientRole() != 0)
                        {
                            if (currentMissionInit() == null)
                            {
                                // We don't have a MissionInit ourselves, so we can't help:
                                reply = "MALMONOMISSION";
                                keepProcessing = false; // No further action required.
                            }
                            else if (currentMissionInit().getExperimentUID().equals(missionInit.getExperimentUID()) && areMissionsEqual(currentMissionInit().getMission(), missionInit.getMission()))
                            {
                                // Our Missions and Experiment IDs match, so we are running the same experiment.
                                // Return the port and server IP address to the caller:
                                MinecraftServerConnection msc = currentMissionInit().getMinecraftServerConnection();
                                if (msc == null)
                                    reply = "MALMOERRORMinecraftServerConnection missing!";
                                else
                                    reply = "MALMOS" + msc.getAddress() + ":" + msc.getPort();

                                keepProcessing = false;
                            }
                            else
                            {
                                // We have a MissionInit, but it doesn't match theirs, or we're not the server:
                                reply = "MALMOBUSY";
                                keepProcessing = false; // No further action required.
                            }
                        }
                        else if (missionInit.getMinecraftServerConnection() == null && missionInit.getClientRole() == 0)
                        {
                            // MissionInit passed to us, no server details, and role 0 specified -
                            // this means WE should become the server, and launch this mission.
                            IState currentState = getStableState();
                            if (currentState != null && currentState.equals(ClientState.DORMANT))
                            {
                                reply = "MALMOOK";
                                keepProcessing = true; // State machine will now process this MissionInit and start the mission.
                            }
                            else
                            {
                                // We're busy - we can't run this mission.
                                reply = "MALMOBUSY";
                                keepProcessing = false; // Ignore the message.
                            }
                        }
                        else if (missionInit.getMinecraftServerConnection() != null && missionInit.getClientRole() != 0)
                        {
                            // We have server details filled in - this is a equest for US to run the mission.
                            IState currentState = getStableState();
                            if (currentState != null && currentState.equals(ClientState.DORMANT))
                            {
                                reply = "MALMOOK";
                                keepProcessing = true; // State machine will now process this MissionInit and start the mission.
                            }
                            else
                            {
                                // We're busy - we can't run this mission.
                                reply = "MALMOBUSY";
                                keepProcessing = false; // Ignore the message.
                            }
                        }
                        else if (missionInit.getMinecraftServerConnection() != null && missionInit.getClientRole() == 0)
                        {
                            // Error condition - currently makes no sense to fill in the server details for role 0.
                            reply = "MALMOERRORServer details provided for server role";
                            keepProcessing = false; // Ditch this message.
                        }
                    }
                }

                if (reply != null)
                {
                    System.out.println("REPLYING WITH: " + reply);
                    try
                    {
                        dos.writeInt(reply.length());
                        dos.writeBytes(reply);
                    }
                    catch (IOException e)
                    {
                    }
                }
                return keepProcessing;
            }
        };

        this.missionPoller.start();

        // Tell the address helper what the actual port is:
        AddressHelper.setMissionControlPort(ClientStateMachine.this.missionPoller.getPortBlocking());
        if (AddressHelper.getMissionControlPort() == -1)
        {
            // Failed to create a mission control port - nothing will work!
            System.out.println("**** NO MISSION CONTROL SOCKET CREATED - WAS THE PORT IN USE? (Check Mod GUI options) ****");
            ClientStateMachine.this.getScreenHelper().addFragment("ERROR: Could not open a Mission Control Port - check the Mod GUI options.", TextCategory.TXT_CLIENT_WARNING, MISSING_MCP_PORT_ERROR);
        }
        else
        {
            // Clear the error string, if there was one:
            ClientStateMachine.this.getScreenHelper().clearFragment(MISSING_MCP_PORT_ERROR);
        }
        // Display the port number:
        ClientStateMachine.this.getScreenHelper().clearFragment(INFO_MCP_PORT);
        if (AddressHelper.getMissionControlPort() != -1)
            ClientStateMachine.this.getScreenHelper().addFragment("MCP: " + AddressHelper.getMissionControlPort(), TextCategory.TXT_INFO, INFO_MCP_PORT);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Episode helpers - each extends a MissionStateEpisode to encapsulate a certain state
    // ---------------------------------------------------------------------------------------------------------

    public abstract class ErrorAwareEpisode extends StateEpisode implements IMalmoMessageListener
    {
        protected Boolean errorFlag = false;
        protected Map<String, String> errorData = null;

        public ErrorAwareEpisode(ClientStateMachine machine)
        {
            super(machine);
            MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_ABORT);
        }

        @Override
        public void onMessage(MalmoMessageType messageType, Map<String, String> data)
        {
            if (messageType == MalmoMod.MalmoMessageType.SERVER_ABORT)
            {
                synchronized (this.errorFlag)
                {
                    this.errorFlag = true;
                    this.errorData = data;
                    // Save the error message, if there is one:
                    if (data != null)
                    {
                        String err = data.get("message");
                        if (err != null)
                            ClientStateMachine.this.saveErrorDetails(err);
                    }
                    onAbort(data);
                }
            }
        }

        @Override
        public void cleanup()
        {
            super.cleanup();
            MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_ABORT);
        }

        protected boolean inAbortState()
        {
            synchronized (this.errorFlag)
            {
                return this.errorFlag;
            }
        }

        protected Map<String, String> getErrorData()
        {
            synchronized (this.errorFlag)
            {
                return this.errorData;
            }
        }

        protected void onAbort(Map<String, String> errorData)
        {
            // Default does nothing, but can be overridden.
        }
    }

    /**
     * Helper base class that responds to the config change and updates our AddressHelper.<br>
     * This will also reset the mission poller. Depending on the state, more
     * work may be needed (eg to recreate the command handler, etc) - it's up to
     * the individual state episodes to do whatever else needs doing.
     */
    abstract public class ConfigAwareStateEpisode extends ErrorAwareEpisode
    {
        ConfigAwareStateEpisode(ClientStateMachine machine)
        {
            super(machine);
        }

        @Override
        public void onConfigChanged(OnConfigChangedEvent ev)
        {
            if (ev.configID.equals(MalmoMod.SOCKET_CONFIGS))
            {
                AddressHelper.update(MalmoMod.instance.getModSessionConfigFile());
                try
                {
                    ClientStateMachine.this.initialiseComms();
                }
                catch (UnknownHostException e)
                {
                    // TODO What to do here?
                    e.printStackTrace();
                }
                ScreenHelper.update(MalmoMod.instance.getModPermanentConfigFile());
            }
            else if (ev.configID.equals(MalmoMod.AUTHENTICATION_CONFIGS))
            {
                AuthenticationHelper.update(MalmoMod.instance.getModPermanentConfigFile());
            }
        }
    }

    /** Initial episode - perform client setup */
    public class InitialiseClientModEpisode extends ConfigAwareStateEpisode
    {
        InitialiseClientModEpisode(ClientStateMachine machine)
        {
            super(machine);
        }

        @Override
        protected void execute() throws Exception
        {
            ClientStateMachine.this.initialiseComms();

            // This is necessary in order to allow user to exit the Minecraft window without halting the experiment:
            GameSettings settings = Minecraft.getMinecraft().gameSettings;
            settings.pauseOnLostFocus = false;
        }

        @Override
        public void onRenderTick(TickEvent.RenderTickEvent ev)
        {
            // We wait until we start to get render ticks, at which point we assume Minecraft has finished starting up.
            episodeHasCompleted(ClientState.DORMANT);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    /** Dormant state - receptive to new missions */
    public class DormantEpisode extends ConfigAwareStateEpisode
    {
        private ClientStateMachine csMachine;

        protected DormantEpisode(ClientStateMachine machine)
        {
            super(machine);
            this.csMachine = machine;
        }

        @Override
        protected void execute()
        {
            // Clear our current MissionInit state:
            csMachine.currentMissionInit = null;
            // Clear our current error state:
            clearErrorDetails();
            // And clear out any stale commands left over from recent missions:
            if (ClientStateMachine.this.controlInputPoller != null)
                ClientStateMachine.this.controlInputPoller.clearCommands();
        }

        @Override
        public void onClientTick(TickEvent.ClientTickEvent ev) throws Exception
        {
            checkForMissionCommand();
        }

        private void checkForMissionCommand() throws Exception
        {
            if (ClientStateMachine.this.missionPoller == null)
                return;

            CommandAndIPAddress comip = missionPoller.getCommandAndIPAddress();
            if (comip == null)
                return;
            String missionMessage = comip.command;
            if (missionMessage == null || missionMessage.length() == 0)
                return;

            MissionInitResult missionInitResult = decodeMissionInit(missionMessage);
            MissionInit missionInit = missionInitResult.missionInit;
            if (missionInit != null)
            {
                missionInit.getClientAgentConnection().setAgentIPAddress(comip.ipAddress);
                System.out.println("Mission received: " + missionInit.getMission().getAbout().getSummary());
                csMachine.currentMissionInit = missionInit;
                // Move on to next state:
                episodeHasCompleted(ClientState.CREATING_HANDLERS);
            }
            else
            {
                throw new Exception("Failed to get valid MissionInit object from SchemaHelper.");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    /**
     * Now the MissionInit XML has been decoded, the client needs to create the
     * Mission Handlers.
     */
    public class CreateHandlersEpisode extends ConfigAwareStateEpisode
    {
        protected CreateHandlersEpisode(ClientStateMachine machine)
        {
            super(machine);
        }

        @Override
        protected void execute() throws Exception
        {
            // Now try creating the handlers:
            try
            {
                ClientStateMachine.this.missionBehaviour = MissionBehaviour.createAgentHandlersFromMissionInit(currentMissionInit());
            }
            catch (Exception e)
            {
                // TODO
            }
            // Set up our command input poller. This is only checked during the MissionRunning episode, but
            // it needs to be started now, so we can report the port it's using back to the agent.
            ClientAgentConnection cac = currentMissionInit().getClientAgentConnection();
            int requestedPort = cac.getClientCommandsPort();
            // If the requested port is 0, we dynamically allocate our own port, and feed that back to the agent.
            // If the requested port is non-zero, we have to use it.
            if (requestedPort != 0 && ClientStateMachine.this.controlInputPoller != null && ClientStateMachine.this.controlInputPoller.getPort() != requestedPort)
            {
                // A specific port has been requested, and it's not the one we are currently using,
                // so we need to recreate our poller.
                ClientStateMachine.this.controlInputPoller.stopServer();
                ClientStateMachine.this.controlInputPoller = null;
            }
            if (ClientStateMachine.this.controlInputPoller == null)
            {
                if (requestedPort == 0)
                    ClientStateMachine.this.controlInputPoller = new TCPInputPoller(AddressHelper.MIN_FREE_PORT, AddressHelper.MAX_FREE_PORT, true);
                else
                    ClientStateMachine.this.controlInputPoller = new TCPInputPoller(requestedPort);
                ClientStateMachine.this.controlInputPoller.start();
            }
            // Make sure the cac is up-to-date:
            cac.setClientCommandsPort(ClientStateMachine.this.controlInputPoller.getPortBlocking());

            // Check to see whether anything has caused us to abort - if so, go to the abort state.
            if (inAbortState())
                episodeHasCompleted(ClientState.MISSION_ABORTED);

            // Handlers and poller created successfully; proceed to next stage of loading.
            // We will either need to connect to an existing server, or to start
            // a new integrated server ourselves, depending on our role.
            // For now, assume that the mod with role 0 is responsible for the server.
            if (currentMissionInit().getClientRole() == 0)
            {
                // We are responsible for the server - investigate what needs to happen next:
                episodeHasCompleted(ClientState.EVALUATING_WORLD_REQUIREMENTS);
            }
            else
            {
                // We may need to connect to a server.
                episodeHasCompleted(ClientState.WAITING_FOR_SERVER_READY);
            }
        }
    };

    // ---------------------------------------------------------------------------------------------------------
    /**
     * Attempt to connect to a server. Wait until connection is established.
     */
    public class WaitingForServerEpisode extends ConfigAwareStateEpisode
    {
        String agentName;
        int ticksUntilNextPing = 0;
        TCPSocketHelper sender;

        protected WaitingForServerEpisode(ClientStateMachine machine)
        {
            super(machine);
            MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_ALLPLAYERSJOINED);
        }

        private TCPSocketHelper sender()
        {
            if (this.sender == null)
            {
                // Set up a TCP connection to the agent:
                ClientAgentConnection cac = currentMissionInit().getClientAgentConnection();
                this.sender = new TCPSocketHelper(cac.getAgentIPAddress(), cac.getAgentMissionControlPort());
            }
            return this.sender;
        }

        @Override
        protected void onClientTick(ClientTickEvent ev)
        {
            // Check to see whether anything has caused us to abort - if so, go to the abort state.
            if (inAbortState())
                episodeHasCompleted(ClientState.MISSION_ABORTED);

            if (ticksUntilNextPing == 0)
            {
                // Tell the server what our agent name is.
                // We do this repeatedly, because the server might not yet be listening.
                if (Minecraft.getMinecraft().thePlayer != null)
                {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("agentname", agentName);
                    map.put("username", Minecraft.getMinecraft().thePlayer.getName());
                    System.out.println("***Telling server we are ready - " + agentName);
                    MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_AGENTREADY, 0, map));
                }

                // We also ping our agent, just to check it is still available:
                boolean sentOkay = sender().sendTCPString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ping/>");
                if (!sentOkay)
                {
                    // It's not available - bail.
                    ClientStateMachine.this.getScreenHelper().addFragment("ERROR: Lost contact with agent - aborting mission", TextCategory.TXT_CLIENT_WARNING, 10000);
                    episodeHasCompletedWithErrors(ClientState.ERROR_LOST_AGENT, "Lost contact with the agent");
                }

                ticksUntilNextPing = 10; // Try again in ten ticks.
            }
            else
            {
                ticksUntilNextPing--;
            }

            List<AgentSection> agents = currentMissionInit().getMission().getAgentSection();
            if (agents.size() > 1 && currentMissionInit().getClientRole() != 0)
            {
                // We are waiting to join a out-of-process server. Need to pay attention to what happens -
                // if we can't join, for any reason, we should abort the mission.
                GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                if (screen != null && screen instanceof GuiDisconnected)
                {
                    // Disconnected screen appears when something has gone wrong.
                    // Would be nice to grab the reason from the screen, but it's a private member.
                    // (Can always use reflection, but it's so inelegant.)
                    episodeHasCompletedWithErrors(ClientState.ERROR_CANNOT_CONNECT_TO_SERVER, "Unable to connect to Minecraft server in multi-agent mission.");
                }
            }
        }

        @Override
        protected void execute() throws Exception
        {
            Minecraft.getMinecraft().displayGuiScreen(null); // Clear any menu screen that might confuse things.

            // Get our name from the Mission:
            List<AgentSection> agents = currentMissionInit().getMission().getAgentSection();
            if (agents == null || agents.size() <= currentMissionInit().getClientRole())
                throw new Exception("No agent section for us!"); // TODO
            this.agentName = agents.get(currentMissionInit().getClientRole()).getName();

            if (agents.size() > 1 && currentMissionInit().getClientRole() == 0) // Multi-agent mission - make sure the server is open to the LAN:
            {
                MinecraftServerConnection msc = new MinecraftServerConnection();
                String address = currentMissionInit().getClientAgentConnection().getClientIPAddress();
                // Do we need to open to LAN?
                if (Minecraft.getMinecraft().isSingleplayer() && !Minecraft.getMinecraft().getIntegratedServer().getPublic())
                {
                    String portstr = Minecraft.getMinecraft().getIntegratedServer().shareToLAN(GameType.SURVIVAL, false); // Set to true to stop spam kicks?
                    ClientStateMachine.this.integratedServerPort = Integer.valueOf(portstr);
                }
                msc.setPort(ClientStateMachine.this.integratedServerPort);
                msc.setAddress(address);
                currentMissionInit().setMinecraftServerConnection(msc);
            }
            else if (agents.size() > 1)
            {
                // Multi-agent mission, we should be joining a server.
                // (Unless we are already on the correct server.)
                String address = currentMissionInit().getMinecraftServerConnection().getAddress();
                int port = currentMissionInit().getMinecraftServerConnection().getPort();
                String targetIP = address + ":" + port;
                System.out.println("We should be joining " + targetIP);
                // Always connect, even if we're already on the same server -
                // otherwise things get out of step.
                net.minecraftforge.fml.client.FMLClientHandler.instance().connectToServerAtStartup(address, port);
            }
        }

        @Override
        public void onMessage(MalmoMessageType messageType, Map<String, String> data)
        {
            super.onMessage(messageType, data);

            if (messageType != MalmoMessageType.SERVER_ALLPLAYERSJOINED)
                return;

            String extraHandlers = data.get("extra_handlers");
            if (extraHandlers != null && extraHandlers.length() > 0)
                attemptToAddExtraHandlers(extraHandlers);

            // The server is ready, so send our MissionInit back to the agent and go!
            // We launch the agent by sending it the MissionInit message we were sent
            // (but with the Launcher's IP address included)
            String xml = null;
            boolean sentOkay = false;
            String errorReport = "";
            try
            {
                xml = SchemaHelper.serialiseObject(currentMissionInit(), MissionInit.class);
                sentOkay = this.sender().sendTCPString(xml);
            }
            catch (JAXBException e)
            {
                errorReport = e.getMessage();
            }
            if (sentOkay)
                episodeHasCompleted(ClientState.RUNNING);
            else
            {
                ClientStateMachine.this.getScreenHelper().addFragment("ERROR: Could not contact agent to start mission - mission will abort.", TextCategory.TXT_CLIENT_WARNING, 10000);
                if (!errorReport.isEmpty())
                {
                    ClientStateMachine.this.getScreenHelper().addFragment("ERROR DETAILS: " + errorReport, TextCategory.TXT_CLIENT_WARNING, 10000);
                    errorReport = ": " + errorReport;
                }
                episodeHasCompletedWithErrors(ClientState.ERROR_CANNOT_START_AGENT, "Failed to send MissionInit back to agent" + errorReport);
            }
        }

        private void attemptToAddExtraHandlers(String extraHandlers)
        {
            try
            {
                AgentHandlers handlers = (AgentHandlers) SchemaHelper.deserialiseObject(extraHandlers, "MissionInit.xsd", AgentHandlers.class);
                currentMissionBehaviour().addExtraHandlers(handlers);
            }
            catch (Exception e)
            {
                // Do something... like episodeHasCompletedWithErrors(nextState, error)?
            }
        }

        @Override
        public void cleanup()
        {
            super.cleanup();
            MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_ALLPLAYERSJOINED);
            if (this.sender != null)
                this.sender.close();
        }
    }

    /**
     * Wait for the server to decide the mission has ended.<br>
     * We're not allowed to return to dormant until the server decides everyone can.
     */
    public class WaitingForServerMissionEndEpisode extends ConfigAwareStateEpisode
    {
        protected WaitingForServerMissionEndEpisode(ClientStateMachine machine)
        {
            super(machine);
            MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_MISSIONOVER);
        }

        @Override
        protected void execute() throws Exception
        {
            // Get our name from the Mission:
            List<AgentSection> agents = currentMissionInit().getMission().getAgentSection();
            if (agents == null || agents.size() <= currentMissionInit().getClientRole())
                throw new Exception("No agent section for us!"); // TODO
            String agentName = agents.get(currentMissionInit().getClientRole()).getName();

            // Now send a message to the server saying that we are ready:
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("agentname", agentName);
            MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_AGENTSTOPPED, 0, map));
        }

        @Override
        public void onMessage(MalmoMessageType messageType, Map<String, String> data)
        {
            super.onMessage(messageType, data);
            if (messageType == MalmoMessageType.SERVER_MISSIONOVER)
                episodeHasCompleted(ClientState.DORMANT);
        }

        @Override
        public void cleanup()
        {
            super.cleanup();
            MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_MISSIONOVER);
        }

        @Override
        protected void onAbort(Map<String, String> errorData)
        {
            episodeHasCompleted(ClientState.MISSION_ABORTED);
        }
    };

    // ---------------------------------------------------------------------------------------------------------
    /**
     * Depending on the basemap provided, either begin to perform a full world
     * load, or reset the current world
     */
    public class EvaluateWorldRequirementsEpisode extends ConfigAwareStateEpisode
    {
        EvaluateWorldRequirementsEpisode(ClientStateMachine machine)
        {
            super(machine);
        }

        @Override
        protected void execute()
        {
            // We are responsible for creating the server, if required.
            // This means we need access to the server's MissionHandlers:
            MissionBehaviour serverHandlers = null;
            try
            {
                serverHandlers = MissionBehaviour.createServerHandlersFromMissionInit(currentMissionInit());
            }
            catch (Exception e)
            {
                episodeHasCompletedWithErrors(ClientState.ERROR_DUFF_HANDLERS, "Could not create server mission handlers: " + e.getMessage());
            }

            boolean needsNewWorld = serverHandlers != null && serverHandlers.worldGenerator != null && serverHandlers.worldGenerator.shouldCreateWorld(currentMissionInit());
            boolean worldCurrentlyExists = Minecraft.getMinecraft().getIntegratedServer() != null && Minecraft.getMinecraft().theWorld != null;
            if (needsNewWorld && worldCurrentlyExists)
            {
                // We want a new world, and there is currently a world running,
                // so we need to kill the current world.
                episodeHasCompleted(ClientState.PAUSING_OLD_SERVER);
            }
            else if (needsNewWorld && !worldCurrentlyExists)
            {
                // We want a new world, and there is currently nothing running,
                // so jump to world creation:
                episodeHasCompleted(ClientState.CREATING_NEW_WORLD);
            }
            else if (!needsNewWorld && worldCurrentlyExists)
            {
                // We don't want a new world, and we can use the current one -
                // but we own the server, so we need to pass it the new mission init:
                Minecraft.getMinecraft().getIntegratedServer().addScheduledTask(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            MalmoMod.instance.sendMissionInitDirectToServer(currentMissionInit);
                        }
                        catch (Exception e)
                        {
                            episodeHasCompletedWithErrors(ClientState.ERROR_INTEGRATED_SERVER_UNREACHABLE, "Could not send MissionInit to our integrated server: " + e.getMessage());
                        }
                    }
                });
                // Skip all the map loading stuff and go straight to waiting for the server:
                episodeHasCompleted(ClientState.WAITING_FOR_SERVER_READY);
            }
            else if (!needsNewWorld && !worldCurrentlyExists)
            {
                // Mission has requested no new world, but there is no current world to play in - this is an error:
                episodeHasCompletedWithErrors(ClientState.ERROR_NO_WORLD, "We have no world to play in - check that your ServerHandlers section contains a world generator");
            }
        }
    };

    // ---------------------------------------------------------------------------------------------------------
    /**
     * Pause the old server. It's vital that we do this, otherwise it will
     * respond to the quit disconnect package straight away and kill the server
     * thread, which means there will be no server to respond to the loadWorld
     * code. (This was the cause of the infamous "Holder Lookups" hang.)
     */
    public class PauseOldServerEpisode extends ConfigAwareStateEpisode
    {
        int serverTickCount = 0;
        int clientTickCount = 0;

        PauseOldServerEpisode(ClientStateMachine machine)
        {
            super(machine);
        }

        @Override
        protected void execute()
        {
            if (Minecraft.getMinecraft().getIntegratedServer() != null && Minecraft.getMinecraft().theWorld != null)
            {
                // If the integrated server has been opened to the LAN, we won't be able to pause it.
                // To get around this, we need to make it think it's not open, by modifying its isPublic flag.
                if (Minecraft.getMinecraft().getIntegratedServer().getPublic())
                {
                    if (!killPublicFlag(Minecraft.getMinecraft().getIntegratedServer()))
                    {
                        // Can't pause, don't want to risk the hang - so bail.
                        episodeHasCompletedWithErrors(ClientState.ERROR_CANNOT_CREATE_WORLD, "Can not pause the old server since it's open to LAN; no way to safely create new world.");
                    }
                }

                Minecraft.getMinecraft().displayGuiScreen(new GuiIngameMenu());
            }
        }

        private boolean killPublicFlag(IntegratedServer server)
        {
            // Are we in a dev environment?
            boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
            // We need to know, because the member name will either be obfuscated or not.
            String isPublicMemberName = devEnv ? "isPublic" : "field_71346_p";
            // NOTE: obfuscated name may need updating if Forge changes.
            Field isPublic;
            try
            {
                isPublic = IntegratedServer.class.getDeclaredField(isPublicMemberName);
                isPublic.setAccessible(true);
                isPublic.set(server, false);
                return true;
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void onClientTick(TickEvent.ClientTickEvent ev)
        {
            // Check to see whether anything has caused us to abort - if so, go to the abort state.
            if (inAbortState())
                episodeHasCompleted(ClientState.MISSION_ABORTED);

            // We need to make sure that both the client and server have paused,
            // otherwise we are still susceptible to the "Holder Lookups" hang.
            
            // Since the server sets its pause state in response to the client's pause state,
            // and it only performs this check once, at the top of its tick method,
            // to be sure that the server has had time to set the flag correctly we need to make sure
            // that at least one server tick method has *started* since the flag was set.
            // We can't do this by catching the onServerTick events, since we don't receive them when the game is paused.
            
            // The following code makes use of the fact that the server both locks and empties the server's futureQueue,
            // every time through the server tick method.
            // This locking means that if the client - which needs to wait on the lock -
            // tries to add an event to the queue in response to an event on the queue being executed,
            // the newly added event will have to happen in a subsequent tick.
            if (Minecraft.getMinecraft().isGamePaused() && ev != null && ev.phase == Phase.END && this.clientTickCount == this.serverTickCount && this.clientTickCount <= 2)
            {
                this.clientTickCount++; // Increment our count, and wait for the server to catch up.
                Minecraft.getMinecraft().getIntegratedServer().addScheduledTask(new Runnable()
                {
                    public void run()
                    {
                        // Increment the server count.
                        PauseOldServerEpisode.this.serverTickCount++;
                    }
                });
            }

            if (this.serverTickCount > 2)
                episodeHasCompleted(ClientState.CLOSING_OLD_SERVER);
        }
    };

    // ---------------------------------------------------------------------------------------------------------
    /**
     * Send a disconnecting message to the current server - sent before attempting to load a new world.
     */
    public class CloseOldServerEpisode extends ConfigAwareStateEpisode
    {
        CloseOldServerEpisode(ClientStateMachine machine)
        {
            super(machine);
        }

        @Override
        protected void execute()
        {
            if (Minecraft.getMinecraft().theWorld != null)
            {
                // If the Minecraft server isn't paused at this point,
                // then the following line will cause the server thread to exit...
                Minecraft.getMinecraft().theWorld.sendQuittingDisconnectingPacket();
                // ...in which case the next line will hang.
                Minecraft.getMinecraft().loadWorld((WorldClient) null);
                // Must display the GUI or Minecraft will attempt to access a non-existent player in the client tick.
                Minecraft.getMinecraft().displayGuiScreen(new GuiMainMenu());
            }
        }

        @Override
        public void onClientTick(TickEvent.ClientTickEvent ev)
        {
            // Check to see whether anything has caused us to abort - if so, go to the abort state.
            if (inAbortState())
                episodeHasCompleted(ClientState.MISSION_ABORTED);

            if (ev.phase == Phase.END)
                episodeHasCompleted(ClientState.CREATING_NEW_WORLD);
        }
    };

    // ---------------------------------------------------------------------------------------------------------
    /**
     * Attempt to create a world.
     */
    public class CreateWorldEpisode extends ConfigAwareStateEpisode
    {
        boolean serverStarted = false;

        CreateWorldEpisode(ClientStateMachine machine)
        {
            super(machine);
        }

        @Override
        protected void execute()
        {
            try
            {
                // We need to use the server's MissionHandlers here:
                MissionBehaviour serverHandlers = MissionBehaviour.createServerHandlersFromMissionInit(currentMissionInit());
                if (serverHandlers != null && serverHandlers.worldGenerator != null)
                {
                    if (!serverHandlers.worldGenerator.createWorld(currentMissionInit()))
                    {
                        // World has not been created.
                        episodeHasCompletedWithErrors(ClientState.ERROR_CANNOT_CREATE_WORLD, "Server world-creation handler failed to create a world: " + serverHandlers.worldGenerator.getErrorDetails());
                    }
                }
            }
            catch (Exception e)
            {
                episodeHasCompletedWithErrors(ClientState.ERROR_CANNOT_CREATE_WORLD, "Server world-creation handler failed to create a world: " + e.getMessage());
            }
        }

        @Override
        protected void onServerTick(ServerTickEvent ev)
        {
            if (!this.serverStarted)
            {
                // The server has started ticking - we can set up its state machine,
                // and move on to the next state in our own machine.
                this.serverStarted = true;
                MalmoMod.instance.initIntegratedServer(currentMissionInit()); // Needs to be done from the server thread.
                episodeHasCompleted(ClientState.WAITING_FOR_SERVER_READY);
            }
        }
    };

    // ---------------------------------------------------------------------------------------------------------
    /**
     * State in which an agent has finished the mission, but is waiting for the server to draw stumps.
     */
    public class MissionIdlingEpisode extends ConfigAwareStateEpisode
    {
        protected MissionIdlingEpisode(ClientStateMachine machine)
        {
            super(machine);
            MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_STOPAGENTS);
        }

        @Override
        protected void execute()
        {
        }

        @Override
        public void onMessage(MalmoMessageType messageType, Map<String, String> data)
        {
            super.onMessage(messageType, data);
            // This message will be sent to us once the server has decided the mission is over.
            if (messageType == MalmoMessageType.SERVER_STOPAGENTS)
                episodeHasCompleted(ClientState.MISSION_ENDED);
        }

        @Override
        public void cleanup()
        {
            super.cleanup();
            MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_STOPAGENTS);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    /**
     * State in which a mission is running.<br>
     * This state is ended by the death of the player or by the IWantToQuit
     * handler, or by the server declaring the mission is over.
     */
    public class MissionRunningEpisode extends ConfigAwareStateEpisode
    {
        public static final int FailedTCPSendCountTolerance = 3; // Number of TCP timeouts before we cancel the mission

        protected MissionRunningEpisode(ClientStateMachine machine)
        {
            super(machine);
            MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_STOPAGENTS);
            MalmoMod.MalmoMessageHandler.registerForMessage(this, MalmoMessageType.SERVER_GO);
        }

        boolean serverHasFiredStartingPistol = false;
        boolean playerDied = false;
        private int failedTCPRewardSendCount = 0;
        private int failedTCPObservationSendCount = 0;
        private boolean wantsToQuit = false; // We have decided our mission is at an end
        private VideoHook videoHook = new VideoHook();
        private String quitCode = "";
        private TCPSocketHelper observationSocket = null;
        private TCPSocketHelper rewardSocket = null;

        protected void onMissionStarted()
        {
            // Open our communication channels:
            openSockets();

            // Tell the server we have started:
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("username", Minecraft.getMinecraft().thePlayer.getName());
            MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_AGENTRUNNING, 0, map));

            // Set up our mission handlers:
            if (currentMissionBehaviour().commandHandler != null)
            {
                currentMissionBehaviour().commandHandler.install(currentMissionInit());
                currentMissionBehaviour().commandHandler.setOverriding(true);
            }

            if (currentMissionBehaviour().observationProducer != null)
                currentMissionBehaviour().observationProducer.prepare(currentMissionInit());

            if (currentMissionBehaviour().quitProducer != null)
                currentMissionBehaviour().quitProducer.prepare(currentMissionInit());

            if (currentMissionBehaviour().rewardProducer != null)
                currentMissionBehaviour().rewardProducer.prepare(currentMissionInit());

            this.videoHook.start(currentMissionInit(), currentMissionBehaviour().videoProducer);

            // Make sure we have mouse control:
            ClientStateMachine.this.inputController.setInputType(InputType.AI);
            Minecraft.getMinecraft().inGameHasFocus = true; // Otherwise auto-repeat won't work for mouse clicks.

            // Overclocking:
            ModSettings modsettings = currentMissionInit().getMission().getModSettings();
            if (modsettings != null && modsettings.getMsPerTick() != null)
                TimeHelper.setMinecraftClientClockSpeed(1000 / modsettings.getMsPerTick());
            if (modsettings != null && modsettings.isPrioritiseOffscreenRendering() == Boolean.TRUE)
                TimeHelper.displayGranularityMs = 1000;
        }

        protected void onMissionEnded(IState nextState, String errorReport)
        {
            // Tidy up our mission handlers:
            if (currentMissionBehaviour().rewardProducer != null)
                currentMissionBehaviour().rewardProducer.cleanup();

            if (currentMissionBehaviour().quitProducer != null)
                currentMissionBehaviour().quitProducer.cleanup();

            if (currentMissionBehaviour().observationProducer != null)
                currentMissionBehaviour().observationProducer.cleanup();

            if (currentMissionBehaviour().commandHandler != null)
            {
                currentMissionBehaviour().commandHandler.setOverriding(false);
                currentMissionBehaviour().commandHandler.deinstall(currentMissionInit());
            }

            // Close our communication channels:
            closeSockets();

            this.videoHook.stop();

            // Return Minecraft speed to "normal":
            TimeHelper.setMinecraftClientClockSpeed(20);
            TimeHelper.displayGranularityMs = 0;

            ClientStateMachine.this.missionQuitCode = this.quitCode;
            if (errorReport != null)
                episodeHasCompletedWithErrors(nextState, errorReport);
            else
                episodeHasCompleted(nextState);
        }

        @Override
        protected void execute()
        {
            onMissionStarted();
        }

        @Override
        public void onPlayerDies(LivingDeathEvent event)
        {
            if (event.entity instanceof EntityPlayerMP)
            {
                this.playerDied = true;
                this.quitCode = MalmoMod.AGENT_DEAD_QUIT_CODE;
            }
        }

        @Override
        public void onClientTick(ClientTickEvent event)
        {
            // Check to see whether anything has caused us to abort - if so, go to the abort state.
            if (inAbortState())
                onMissionEnded(ClientState.MISSION_ABORTED, "Mission was aborted by server: " + ClientStateMachine.this.getErrorDetails());

            // Check to see whether we've been kicked from the server.
            NetworkManager netman = Minecraft.getMinecraft().getNetHandler().getNetworkManager();
            if (netman != null && !netman.hasNoChannel() && !netman.isChannelOpen())
            {
                // Connection has been lost.
                onMissionEnded(ClientState.ERROR_LOST_NETWORK_CONNECTION, "Client was kicked from server - " + netman.getExitMessage().getUnformattedText());
            }

            // Although we only arrive in this episode once the server has determined that all clients are ready to go,
            // the server itself waits for all clients to begin running before it enters the running state itself.
            // This creates a small vulnerability, since a running client could theoretically *finish* its mission
            // before the server manages to *start*.
            // (This has potentially disastrous effects for the state machine, and is easy to reproduce by,
            // for example, setting the start point and goal of the mission to the same coordinates.)
            
            // To guard against this happening, although we are running, we don't act on anything -
            // we don't check for commands, or send observations or rewards - until we get the SERVER_GO signal,
            // which is sent once the server's running episode has started.
            if (!this.serverHasFiredStartingPistol)
                return;

            if (event.phase == Phase.END)
            {
                // Check whether or not we want to quit:
                IWantToQuit quitHandler = (currentMissionBehaviour() != null) ? currentMissionBehaviour().quitProducer : null;
                boolean quitHandlerFired = (quitHandler != null && quitHandler.doIWantToQuit(currentMissionInit()));
                if (quitHandlerFired || this.wantsToQuit || this.playerDied)
                {
                    if (quitHandlerFired)
                    {
                        this.quitCode = quitHandler.getOutcome();
                    }
                    try
                    {
                        // Save the quit code for anything that needs it:
                        MalmoMod.getPropertiesForCurrentThread().put("QuitCode", this.quitCode);
                    }
                    catch (Exception e)
                    {
                        System.out.println("Failed to get properties - final reward may go missing.");
                    }

                    // Get the final reward data:
                    ClientAgentConnection cac = currentMissionInit().getClientAgentConnection();
                    if (currentMissionBehaviour() != null && currentMissionBehaviour().rewardProducer != null && cac != null)
                        currentMissionBehaviour().rewardProducer.getReward(currentMissionInit(), ClientStateMachine.this.finalReward);

                    // Now send a message to the server saying that we have finished our mission:
                    List<AgentSection> agents = currentMissionInit().getMission().getAgentSection();
                    String agentName = agents.get(currentMissionInit().getClientRole()).getName();
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("agentname", agentName);
                    map.put("username", Minecraft.getMinecraft().thePlayer.getName());
                    MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_AGENTFINISHEDMISSION, 0, map));
                    onMissionEnded(ClientState.IDLING, null);
                }
                else
                {
                    // Send off observation and reward data:
                    sendData();
                    // And see if we have any incoming commands to act upon:
                    checkForControlCommand();
                }
            }
        }

        private void openSockets()
        {
            ClientAgentConnection cac = currentMissionInit().getClientAgentConnection();
            this.observationSocket = new TCPSocketHelper(cac.getAgentIPAddress(), cac.getAgentObservationsPort());
            this.rewardSocket = new TCPSocketHelper(cac.getAgentIPAddress(), cac.getAgentRewardsPort());
        }

        private void closeSockets()
        {
            this.observationSocket.close();
            this.rewardSocket.close();
        }

        private void sendData()
        {
            // Create the observation data:
            String data = "";
            if (currentMissionBehaviour() != null && currentMissionBehaviour().observationProducer != null)
            {
                JsonObject json = new JsonObject();
                currentMissionBehaviour().observationProducer.writeObservationsToJSON(json, currentMissionInit());
                data = json.toString();
            }

            ClientAgentConnection cac = currentMissionInit().getClientAgentConnection();

            if (data != null && data.length() > 2 && cac != null) // An empty json string will be "{}" (length 2) - don't send these.
            {
                // Bung the whole shebang off via TCP:
                if (this.observationSocket.sendTCPString(data))
                {
                    this.failedTCPObservationSendCount = 0;
                }
                else
                {
                    // Failed to send observation message.
                    this.failedTCPObservationSendCount++;
                    ClientStateMachine.this.getScreenHelper().addFragment("ERROR: Agent missed observation signal", TextCategory.TXT_CLIENT_WARNING, 5000);
                }
            }

            // Now create the reward signal:
            if (currentMissionBehaviour() != null && currentMissionBehaviour().rewardProducer != null && cac != null)
            {
                MultidimensionalReward reward = new MultidimensionalReward();
                currentMissionBehaviour().rewardProducer.getReward(currentMissionInit(), reward);
                if (!reward.isEmpty())
                {
                    String strReward = reward.getAsSimpleString();
                    if (this.rewardSocket.sendTCPString(strReward))
                    {
                        this.failedTCPRewardSendCount = 0; // Reset the count of consecutive TCP failures.
                    }
                    else
                    {
                        // Failed to send TCP message - probably because the agent has quit under our feet.
                        // (This happens a lot when developing a Python agent - the developer has no easy way to quit
                        // the agent cleanly, so tends to kill the process.)
                        this.failedTCPRewardSendCount++;
                        ClientStateMachine.this.getScreenHelper().addFragment("ERROR: Agent missed reward signal", TextCategory.TXT_CLIENT_WARNING, 5000);
                    }
                }
            }

            // Check that our messages are getting through:
            int maxFailed = Math.max(this.failedTCPRewardSendCount, this.videoHook.failedTCPSendCount);
            maxFailed = Math.max(maxFailed, this.failedTCPObservationSendCount);
            if (maxFailed > FailedTCPSendCountTolerance)
            {
                // They're not - and we've exceeded the count of allowed TCP failures.
                System.out.println("ERROR: TCP video frames are not getting through - quitting mission.");
                this.wantsToQuit = true;
                this.quitCode = MalmoMod.AGENT_UNRESPONSIVE_CODE;
            }
        }

        /**
         * Check to see if any control instructions have been received and act on them if so.
         */
        private void checkForControlCommand()
        {
            String command = "";
            boolean quitHandlerFired = false;
            IWantToQuit quitHandler = (currentMissionBehaviour() != null) ? currentMissionBehaviour().quitProducer : null;

            command = ClientStateMachine.this.controlInputPoller.getCommand();
            while (command != null && command.length() > 0 && !quitHandlerFired)
            {
                // Pass the command to our various control overrides:
                boolean handled = handleCommand(command);
                // Get the next command:
                command = ClientStateMachine.this.controlInputPoller.getCommand();
                // If there *is* another command (commands came in faster than one per client tick),
                // then we should check our quit producer before deciding whether to execute it.
                if (command != null && command.length() > 0 && handled)
                    quitHandlerFired = (quitHandler != null && quitHandler.doIWantToQuit(currentMissionInit()));
            }
        }

        /**
         * Attempt to handle a command string by passing it to our various external controllers in turn.
         * 
         * @param command the command string to be handled.
         * @return true if the command was handled.
         */
        private boolean handleCommand(String command)
        {
            if (currentMissionBehaviour() != null && currentMissionBehaviour().commandHandler != null)
            {
                return currentMissionBehaviour().commandHandler.execute(command, currentMissionInit());
            }
            return false;
        }

        @Override
        public void onMessage(MalmoMessageType messageType, Map<String, String> data)
        {
            super.onMessage(messageType, data);
            // This message will be sent to us once the server has decided the mission is over.
            if (messageType == MalmoMessageType.SERVER_STOPAGENTS)
            {
                this.quitCode = data.containsKey("QuitCode") ? data.get("QuitCode") : "";
                try
                {
                    // Save the quit code for anything that needs it:
                    MalmoMod.getPropertiesForCurrentThread().put("QuitCode", this.quitCode);
                }
                catch (Exception e)
                {
                    System.out.println("Failed to get properties - final reward may go missing.");
                }
                // Get the final reward data:
                ClientAgentConnection cac = currentMissionInit().getClientAgentConnection();
                if (currentMissionBehaviour() != null && currentMissionBehaviour().rewardProducer != null && cac != null)
                    currentMissionBehaviour().rewardProducer.getReward(currentMissionInit(), ClientStateMachine.this.finalReward);

                onMissionEnded(ClientState.MISSION_ENDED, null);
            }
            else if (messageType == MalmoMessageType.SERVER_GO)
            {
                this.serverHasFiredStartingPistol = true; // GO GO GO!
            }
        }

        @Override
        public void cleanup()
        {
            super.cleanup();
            MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_STOPAGENTS);
            MalmoMod.MalmoMessageHandler.deregisterForMessage(this, MalmoMessageType.SERVER_GO);
        }
    };

    // ---------------------------------------------------------------------------------------------------------
    /**
     * State that occurs at the end of the mission, whether due to death,
     * failure, success, error, or whatever.
     */
    public class MissionEndedEpisode extends ConfigAwareStateEpisode
    {
        private MissionResult result;
        private boolean aborting;
        private boolean informServer;
        private boolean informAgent;

        public MissionEndedEpisode(ClientStateMachine machine, MissionResult mr, boolean aborting, boolean informServer, boolean informAgent)
        {
            super(machine);
            this.result = mr;
            this.aborting = aborting;
            this.informServer = informServer;
            this.informAgent = informAgent;
        }

        @Override
        protected void execute()
        {
            // Get a text report:
            String errorFeedback = ClientStateMachine.this.getErrorDetails();
            String quitFeedback = ClientStateMachine.this.missionQuitCode;
            String concatenation = (errorFeedback != null && !errorFeedback.isEmpty() && quitFeedback != null && !quitFeedback.isEmpty()) ? ";\n" : "";
            String report = quitFeedback + concatenation + errorFeedback;

            if (this.informServer)
            {
                // Inform the server of what has happened.
                HashMap<String, String> map = new HashMap<String, String>();
                if (Minecraft.getMinecraft().thePlayer != null) // Might not be a player yet.
                    map.put("username", Minecraft.getMinecraft().thePlayer.getName());
                map.put("error", ClientStateMachine.this.getErrorDetails());
                MalmoMod.network.sendToServer(new MalmoMod.MalmoMessage(MalmoMessageType.CLIENT_BAILED, 0, map));
            }

            if (this.informAgent)
            {
                // Create a MissionEnded instance for this result:
                MissionEnded missionEnded = new MissionEnded();
                missionEnded.setStatus(this.result);
                if (ClientStateMachine.this.missionQuitCode != null && ClientStateMachine.this.missionQuitCode.equals(MalmoMod.AGENT_DEAD_QUIT_CODE))
                    missionEnded.setStatus(MissionResult.PLAYER_DIED); // Need to do this manually.
                missionEnded.setHumanReadableStatus(report);
                if (!ClientStateMachine.this.finalReward.isEmpty())
                {
                    missionEnded.setReward(ClientStateMachine.this.finalReward.getAsReward());
                    ClientStateMachine.this.finalReward.clear();
                }
                // And send it to the agent to inform it that the mission has ended:
                sendMissionEnded(missionEnded);
            }

            if (this.aborting) // Take the shortest path back to dormant.
                episodeHasCompleted(ClientState.DORMANT);
        }

        private void sendMissionEnded(MissionEnded missionEnded)
        {
            // Send a MissionEnded message to the agent to inform it that the mission has ended.
            ClientAgentConnection conn = currentMissionInit().getClientAgentConnection();
            String address = conn.getAgentIPAddress();
            int port = conn.getAgentMissionControlPort();

            // Create a string XML representation:
            String missionEndedString = null;
            try
            {
                missionEndedString = SchemaHelper.serialiseObject(missionEnded, MissionEnded.class);
            }
            catch (JAXBException e)
            {
            }

            boolean sentOkay = false;
            if (missionEndedString != null)
            {
                System.out.println(String.format("Sending mission ended message to %s:%d.", address, port));
                TCPSocketHelper sender = new TCPSocketHelper(address, port);
                sentOkay = sender.sendTCPString(missionEndedString);
                sender.close();
            }

            if (!sentOkay)
            {
                // Couldn't formulate a reply to the agent - bit of a problem.
                // Can't do much to alert the agent itself,
                // will have to settle for alerting anyone who is watching the mod:
                ClientStateMachine.this.getScreenHelper().addFragment("ERROR: Could not send mission ended message - agent may need manually resetting.", TextCategory.TXT_CLIENT_WARNING, 10000);
            }
        }

        @Override
        public void onClientTick(ClientTickEvent event)
        {
            if (!this.aborting)
                episodeHasCompleted(ClientState.WAITING_FOR_SERVER_MISSION_END);
        }
    };
}
