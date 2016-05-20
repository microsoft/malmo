package com.microsoft.Malmo.Utils;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;

import com.microsoft.Malmo.MalmoMod;

/** Class that helps to track and centralise our various IP address and port requirements.<br>
 */
public class AddressHelper
{
	static public final int MIN_MISSION_CONTROL_PORT = 10000;
	static public final int MIN_FREE_PORT = 10100;
	static public final int MAX_FREE_PORT = 11000;
	static private int missionControlPortOverride = 0;
	static private int missionControlPort = 0;
	
	static public void update(Configuration configs)
	{
		// Read the MCP override from our configs file - if it's not present, use 0 as a default.
		missionControlPortOverride = configs.get(MalmoMod.SOCKET_CONFIGS, "portOverride", 0).getInt();
	}
	
	/** Get the MissionControl port override - set via the configs.<br>
	 * If this is 0, the system will automatically allocate a port based on the available range.
	 */
	static public int getMissionControlPortOverride()
	{
		return AddressHelper.missionControlPortOverride;
	}

	/** Get the actual port used for mission control, assuming it has been set. (Will return 0 if not.)<br>
	 * @return the port in use for mission control.
	 */
	static public int getMissionControlPort()
	{
		return AddressHelper.missionControlPort;
	}

	/** Set the actual port used for mission control - not persisted, could be different each time the Mod is run.
	 * @param port the port currently in use for mission control.
	 */
	static public void setMissionControlPort(int port)
	{
		if (port != AddressHelper.missionControlPort)
		{
			AddressHelper.missionControlPort = port;
			// Also update our metadata, for displaying to the user:
			ModMetadata md = Loader.instance().activeModContainer().getMetadata();
			if (port != -1)
				md.description = "Talk to this Mod using port " + EnumChatFormatting.GREEN + port;
			else
				md.description = EnumChatFormatting.RED + "ERROR: No mission control port - check configuration";

			// See if changing the port should lead to changing the login details:
			AuthenticationHelper.update(MalmoMod.instance.getModPermanentConfigFile());
		}
	}
}