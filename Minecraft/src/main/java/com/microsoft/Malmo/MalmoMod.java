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

package com.microsoft.Malmo;

import com.microsoft.Malmo.MissionHandlers.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import com.microsoft.Malmo.Client.MalmoModClient;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Server.MalmoModServer;
import com.microsoft.Malmo.Utils.AddressHelper;
import com.microsoft.Malmo.Utils.PerformanceHelper;
import com.microsoft.Malmo.Utils.ScoreHelper;
import com.microsoft.Malmo.Utils.SchemaHelper;
import com.microsoft.Malmo.Utils.ScreenHelper;
import com.microsoft.Malmo.Utils.SeedHelper;
import com.microsoft.Malmo.Utils.TCPUtils;
import com.microsoft.Malmo.Client.MalmoEnvServer;


@Mod(modid = MalmoMod.MODID, guiFactory = "com.microsoft.Malmo.MalmoModGuiOptions")
public class MalmoMod
{
    public static final String MODID = "malmomod";
    public static final String SOCKET_CONFIGS = "malmoports";
    public static final String ENV_CONFIGS = "envtype";
    public static final String DIAGNOSTIC_CONFIGS = "malmodiags";
    public static final String AUTHENTICATION_CONFIGS = "malmologins";
    public static final String SCORING_CONFIGS = "malmoscore";
    public static final String PERFORMANCE_CONFIGS = "malmoperformance";
    public static final String SEED_CONFIGS = "malmoseed";
    public static final String AGENT_DEAD_QUIT_CODE = "MALMO_AGENT_DIED";
    public static final String AGENT_UNRESPONSIVE_CODE = "MALMO_AGENT_NOT_RESPONDING";
    public static final String VIDEO_UNRESPONSIVE_CODE = "MALMO_VIDEO_NOT_RESPONDING";

    protected static Hashtable<String, Object> clientProperties = new Hashtable<String, Object>();
    protected static Hashtable<String, Object> serverProperties = new Hashtable<String, Object>();

    MalmoModClient client = null;
    MalmoModServer server = null;
    Configuration sessionConfig = null;		// Configs just for this session - used in place of command-line arguments, overwritten by LaunchClient.bat
    Configuration permanentConfig = null;	// Configs that persist - not overwritten by LaunchClient.bat

    @Instance(value = MalmoMod.MODID) //Tell Forge what instance to use.
    public static MalmoMod instance;

    public static SimpleNetworkWrapper network;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
         if (!SchemaHelper.testSchemaVersionNumbers(Loader.instance().activeModContainer().getVersion()))
            throw new RuntimeException("This mod has been incorrectly built; check schema version numbers.");

        if (event.getModMetadata().version.equals("${version}"))
        {
            // The mcmod.info version number is populated by gradle; if we've been built without gradle,
            // via eclipse say, then we can just use the internal version number instead, which comes to us from the version.properties file.
            // (There's no real benefit to doing this; it just looks nicer in the Mod GUI if the version number is filled in.)
            event.getModMetadata().version = Loader.instance().activeModContainer().getVersion();
        }
        // Load the correct configs (client or server)
        File configDir = event.getModConfigurationDirectory();
        File sessionConfigFile = new File(configDir, MODID + event.getSide().toString() + ".cfg");
        File permanentConfigFile = new File(configDir, MODID + event.getSide().toString() + "Permanent.cfg");
        this.sessionConfig = new Configuration(sessionConfigFile);
        this.sessionConfig.load();
        this.permanentConfig = new Configuration(permanentConfigFile);
        this.permanentConfig.load();

        AddressHelper.update(this.sessionConfig);
        ScoreHelper.update(this.sessionConfig);
        ScreenHelper.update(this.permanentConfig);
        TCPUtils.update(this.permanentConfig);
        MalmoEnvServer.update(this.sessionConfig);
        PerformanceHelper.update(this.sessionConfig);
        SeedHelper.update(this.sessionConfig);

        network = NetworkRegistry.INSTANCE.newSimpleChannel("Malmo");

        // Until we can do tighter message passing and syncing in sync ticking, we want to keep
        // things client side.
        network.registerMessage(ObservationFromGridImplementation.GridRequestMessageHandler.class, ObservationFromGridImplementation.GridRequestMessage.class, 2, Side.SERVER);
        network.registerMessage(MalmoMessageHandler.class, MalmoMessage.class, 3, Side.CLIENT);	// Malmo messages from server to client
        network.registerMessage(SimpleCraftCommandsImplementation.CraftMessageHandler.class, SimpleCraftCommandsImplementation.CraftMessage.class, 4, Side.SERVER);
        network.registerMessage(NearbyCraftCommandsImplementation.CraftNearbyMessageHandler.class, NearbyCraftCommandsImplementation.CraftNearbyMessage.class, 13, Side.SERVER);
        network.registerMessage(NearbySmeltCommandsImplementation.SmeltNearbyMessageHandler.class, NearbySmeltCommandsImplementation.SmeltNearbyMessage.class, 14, Side.SERVER);
        network.registerMessage(EquipCommandsImplementation.EquipMessageHandler.class, EquipCommandsImplementation.EquipMessage.class, 15, Side.SERVER);
        network.registerMessage(PlaceCommandsImplementation.PlaceMessageHandler.class, PlaceCommandsImplementation.PlaceMessage.class, 16, Side.SERVER);
        network.registerMessage(AbsoluteMovementCommandsImplementation.TeleportMessageHandler.class, AbsoluteMovementCommandsImplementation.TeleportMessage.class, 5, Side.SERVER);
        network.registerMessage(MalmoMessageHandler.class, MalmoMessage.class, 6, Side.SERVER);	// Malmo messages from client to server
        network.registerMessage(InventoryCommandsImplementation.InventoryMessageHandler.class, InventoryCommandsImplementation.InventoryMessage.class, 7, Side.SERVER);
        network.registerMessage(DiscreteMovementCommandsImplementation.UseActionMessageHandler.class, DiscreteMovementCommandsImplementation.UseActionMessage.class, 8, Side.SERVER);
        network.registerMessage(DiscreteMovementCommandsImplementation.AttackActionMessageHandler.class, DiscreteMovementCommandsImplementation.AttackActionMessage.class, 9, Side.SERVER);
        network.registerMessage(ObservationFromFullInventoryImplementation.InventoryRequestMessageHandler.class, ObservationFromFullInventoryImplementation.InventoryRequestMessage.class, 10, Side.SERVER);
        network.registerMessage(InventoryCommandsImplementation.InventoryChangeMessageHandler.class, InventoryCommandsImplementation.InventoryChangeMessage.class, 11, Side.CLIENT);
        network.registerMessage(ObservationFromSystemImplementation.SystemRequestMessageHandler.class, ObservationFromSystemImplementation.SystemRequestMessage.class, 12, Side.SERVER);
    }

    @EventHandler
    public void onMissingMappingsEvent(FMLMissingMappingsEvent event)
    {
        // The lit_furnace item was removed in Minecraft 1.9, so pre-1.9 files will produce a warning when
        // loaded. This is harmless for a human user, but it breaks Malmo's FileWorldGenerator handler, since
        // it will bring up a GUI and wait for the user to click a button before continuing.
        // To avoid this, we specifically ignore lit_furnace item mapping.
        for (MissingMapping mapping : event.getAll())
        {
            if (mapping.type == GameRegistry.Type.ITEM && mapping.name.equals("minecraft:lit_furnace"))
                mapping.ignore();
        }
    }

    public Configuration getModSessionConfigFile() { return this.sessionConfig; }
    public Configuration getModPermanentConfigFile() { return this.permanentConfig; }

    public static Hashtable<String, Object> getPropertiesForCurrentThread() throws Exception
    {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread())
            return clientProperties;
        
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null && server.isCallingFromMinecraftThread())
            return serverProperties;
        else throw new Exception("Request for properties made from unrecognised thread.");
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (event.getSide().isClient())
        {
            this.client = new MalmoModClient();
            this.client.init(event);
        }
        if (event.getSide().isServer())
        {
            this.server = new MalmoModServer();
            this.server.init(event);
        }
    }

    public void initIntegratedServer(MissionInit minit)
    {
        // Will replace any existing server objects.
        this.server = new MalmoModServer();
        this.server.init(minit);
    }

    public void sendMissionInitDirectToServer(MissionInit minit) throws Exception
    {
        if (this.server == null)
            throw new Exception("Trying to send a mission request directly when no server has been created!");

        this.server.sendMissionInitDirectToServer(minit);
    }

    public float getServerTickRate() throws Exception
    {
        if (this.server == null)
            throw new Exception("Trying to get the server tick rate when no server has been created!");

        return this.server.getServerTickRate();
    }

    public enum MalmoMessageType
    {
        SERVER_NULLMESSASGE,
        SERVER_ALLPLAYERSJOINED,
        SERVER_GO,                  // All clients are running, server is running - GO!
        SERVER_STOPAGENTS,			// Server request for all agents to stop what they are doing (mission is over)
        SERVER_MISSIONOVER,			// Server informing that all agents have stopped, and the mission is now over.
        SERVER_OBSERVATIONSREADY,
        SERVER_TEXT,
        SERVER_ABORT,
        SERVER_COLLECTITEM,
        SERVER_DISCARDITEM,
        SERVER_BUILDBATTLEREWARD,   // Server has detected a reward from the build battle
        SERVER_SHARE_REWARD,        // Server has received a reward from a client and is distributing it to the other agents
        SERVER_YOUR_TURN,           // Server turn scheduler is telling client that it is their go next
        SERVER_SOMEOTHERMESSAGE,
        CLIENT_AGENTREADY,			// Client response to server's ready request
        CLIENT_AGENTRUNNING,		// Client has just started running
        CLIENT_AGENTSTOPPED,		// Client response to server's stop request
        CLIENT_AGENTFINISHEDMISSION,// Individual agent has finished a mission
        CLIENT_BAILED,				// Client has hit an error and been forced to enter error state
        CLIENT_SHARE_REWARD,        // Client has received a reward and needs to share it with other agents
        CLIENT_TURN_TAKEN,          // Client is telling the server turn scheduler that they have just taken their turn
        CLIENT_SOMEOTHERMESSAGE
    }

    /** General purpose messaging class<br>
     * Used to pass messages from the server to the client.
     */
    static public class MalmoMessage implements IMessage
    {
        private MalmoMessageType messageType = MalmoMessageType.SERVER_NULLMESSASGE;
        private int uid = 0;
        private Map<String, String> data = new HashMap<String, String>();

        public MalmoMessage()
        {
        }

        /** Construct a message for all listeners of that messageType
         * @param messageType
         * @param message
         */
        public MalmoMessage(MalmoMessageType messageType, String message)
        {
            this.messageType = messageType;
            this.uid = 0;
            this.data.put("message",  message);
        }

        /** Construct a message for the (hopefully) single listener that matches the uid
         * @param messageType
         * @param uid a hash code that (more or less) uniquely identifies the targeted listener
         * @param message
         */
        public MalmoMessage(MalmoMessageType messageType, int uid, Map<String, String> data)
        {
            this.messageType = messageType;
            this.uid = uid;
            this.data = data;
        }

        /** Read a UTF8 string that could potentially be larger than 64k<br>
         * The ByteBufInputStream.readUTF() and writeUTF() calls use the first two bytes of the message
         * to encode the length of the string, which limits the string length to 64k.
         * This method gets around that limitation by using a four byte header.
         * @param bbis ByteBufInputStream we are reading from
         * @return the (potentially large) string we read
         * @throws IOException
         */
        private String readLargeUTF(ByteBufInputStream bbis) throws IOException
        {
            int length = bbis.readInt();
            if (length == 0)
                return "";

            byte[] data = new byte[length];
            int length_read = bbis.read(data, 0, length);
            if (length_read != length)
                throw new IOException("Failed to read whole message");

            return new String(data, "utf-8");
        }

        /** Write a potentially long string as UTF8<br>
         * The ByteBufInputStream.readUTF() and writeUTF() calls use the first two bytes of the message
         * to encode the length of the string, which limits the string length to 64k.
         * This method gets around that limitation by using a four byte header.
         * @param s The string we are sending
         * @param bbos The ByteBufOutputStream we are writing to
         * @throws IOException
         */
        private void writeLargeUTF(String s, ByteBufOutputStream bbos) throws IOException
        {
            byte[] data = s.getBytes("utf-8");
            bbos.writeInt(data.length);
            bbos.write(data);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            int i = ByteBufUtils.readVarInt(buf, 1);	// Read message type from first byte.
            if (i >= 0 && i <= MalmoMessageType.values().length)
                this.messageType = MalmoMessageType.values()[i];
            else
                this.messageType = MalmoMessageType.SERVER_NULLMESSASGE;

            // Now read the uid:
            this.uid = buf.readInt();

            // And the actual message content:
            // First, the number of entries in the map:
            int length = buf.readInt();
            this.data = new HashMap<String, String>();
            // Now read each key/value pair:
            ByteBufInputStream bbis = new ByteBufInputStream(buf);
            for (i = 0; i < length; i++)
            {
                String key;
                String value;
                try
                {
                    key = bbis.readUTF();
                    value = readLargeUTF(bbis);
                    this.data.put(key, value);
                }
                catch (IOException e)
                {
                    System.out.println("Warning - failed to read message data");
                }
            }
            try
            {
                bbis.close();
            }
            catch (IOException e)
            {
                System.out.println("Warning - failed to read message data");
            }
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            ByteBufUtils.writeVarInt(buf, this.messageType.ordinal(), 1);	// First byte is the message type.
            buf.writeInt(this.uid);
            // Now write the data as a set of string pairs:
            ByteBufOutputStream bbos = new ByteBufOutputStream(buf);
            buf.writeInt(this.data.size());
            for (Entry<String, String> e : this.data.entrySet())
            {
                try
                {
                    bbos.writeUTF(e.getKey());
                    writeLargeUTF(e.getValue(), bbos);
                }
                catch (IOException e1)
                {
                    System.out.println("Warning - failed to write message data");
                }
            }
            try
            {
                bbos.close();
            }
            catch (IOException e1)
            {
                System.out.println("Warning - failed to write message data");
            }
        }
    }

    public interface IMalmoMessageListener
    {
        void onMessage(MalmoMessageType messageType, Map<String, String> data);
    }

    /** Handler for messages from the server to the clients. Register with this to receive specific messages.
    */
    public static class MalmoMessageHandler implements IMessageHandler<MalmoMessage, IMessage>
    {
        static private Map<MalmoMessageType, List<IMalmoMessageListener>> listeners = new HashMap<MalmoMessageType, List<IMalmoMessageListener>>();
        public MalmoMessageHandler()
        {
        }

        public static boolean registerForMessage(IMalmoMessageListener listener, MalmoMessageType messageType)
        {
            if (!listeners.containsKey(messageType))
                listeners.put(messageType,  new ArrayList<IMalmoMessageListener>());

            if (listeners.get(messageType).contains(listener))
                return false;	// Already registered.

            listeners.get(messageType).add(listener);
            return true;
        }

        public static boolean deregisterForMessage(IMalmoMessageListener listener, MalmoMessageType messageType)
        {
            if (!listeners.containsKey(messageType))
                return false;	// Not registered.

            return listeners.get(messageType).remove(listener);	// Will return false if not present.
        }

        @Override
        public IMessage onMessage(final MalmoMessage message, final MessageContext ctx)
        {
            final List<IMalmoMessageListener> interestedParties = listeners.get(message.messageType);
            if (interestedParties != null && interestedParties.size() > 0)
            {
                IThreadListener mainThread = null;
                if (ctx.side == Side.CLIENT)
                    mainThread = Minecraft.getMinecraft();
                else
                    mainThread = (WorldServer)ctx.getServerHandler().playerEntity.world;

                mainThread.addScheduledTask(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (IMalmoMessageListener l : interestedParties)
                        {
                            // If the message's uid is set (ie non-zero), then use it to ensure that only the matching listener receives this message.
                            // Otherwise, let all listeners who are interested get a look.
                            if (message.uid == 0 || System.identityHashCode(l) == message.uid)
                                l.onMessage(message.messageType,  message.data);
                        }
                    }
                });
            }
            return null; // no response in this case
        }
    }

    public static void safeSendToAll(MalmoMessageType malmoMessage)
    {
        // network.sendToAll() is buggy - race conditions result in the message getting trashed if there is more than one client.
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (Object player : server.getPlayerList().getPlayers())
        {
            if (player != null && player instanceof EntityPlayerMP)
            {
                // Must construct a new message for each client:
                network.sendTo(new MalmoMod.MalmoMessage(malmoMessage, ""), (EntityPlayerMP)player);
            }
        }
    }

    public static void safeSendToAll(MalmoMessageType malmoMessage, Map<String, String> data)
    {
        // network.sendToAll() is buggy - race conditions result in the message getting trashed if there is more than one client.
        if (data == null)
        {
            safeSendToAll(malmoMessage);
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (Object player : server.getPlayerList().getPlayers())
        {
            if (player != null && player instanceof EntityPlayerMP)
            {
                // Must construct a new message for each client:
                Map<String, String> dataCopy = new HashMap<String, String>();
                dataCopy.putAll(data);
                network.sendTo(new MalmoMod.MalmoMessage(malmoMessage, 0, dataCopy), (EntityPlayerMP)player);
            }
        }
    }
}
