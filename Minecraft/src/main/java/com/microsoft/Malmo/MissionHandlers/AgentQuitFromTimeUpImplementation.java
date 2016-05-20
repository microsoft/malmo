package com.microsoft.Malmo.MissionHandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import com.microsoft.Malmo.Schemas.AgentQuitFromTimeUp;
import com.microsoft.Malmo.Schemas.MissionInit;

/** IWantToQuit object that returns true when a certain amount of time has elapsed.<br>
 * This object also draws a cheeky countdown on the Minecraft Chat HUD.
 */

public class AgentQuitFromTimeUpImplementation extends QuitFromTimeUpBase
{
	private String quitCode = "";
	
	@Override
	public boolean parseParameters(Object params)
	{
		if (params == null || !(params instanceof AgentQuitFromTimeUp))
			return false;
		
		AgentQuitFromTimeUp qtuparams = (AgentQuitFromTimeUp)params;
		setTimeLimitMs(qtuparams.getTimeLimitMs().intValue());
		this.quitCode = qtuparams.getDescription();
		return true;
	}

	@Override
	protected long getWorldTime()
	{
		return Minecraft.getMinecraft().theWorld.getTotalWorldTime();
	}

	@Override
	protected void drawCountDown(int secondsRemaining)
	{
        ChatComponentText text = new ChatComponentText("" + secondsRemaining + "...");
        ChatStyle style = new ChatStyle();
        style.setBold(true);
        if (secondsRemaining <= 5)
            style.setColor(EnumChatFormatting.RED);

        text.setChatStyle(style);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(text, 1);
	}
	
	@Override
    public void prepare(MissionInit missionInit) {}

	@Override
    public void cleanup() {}
	
	@Override
	public String getOutcome() { return this.quitCode; }
}