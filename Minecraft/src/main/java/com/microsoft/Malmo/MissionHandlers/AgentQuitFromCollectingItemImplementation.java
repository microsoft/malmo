package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.AgentQuitFromCollectingItem;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithDescription;
import com.microsoft.Malmo.Schemas.BlockSpec;
import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.MissionInit;

import net.minecraft.block.Block;

public class AgentQuitFromCollectingItemImplementation extends HandlerBase implements IWantToQuit
{
    AgentQuitFromCollectingItem params;
    List<String> itemTypeNames;
    String quitCode = "";

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof AgentQuitFromCollectingItem))
            return false;
        
        this.params = (AgentQuitFromCollectingItem)params;
        // Flatten all the possible block type names for ease of matching later:
        this.itemTypeNames = new ArrayList<String>();
        for (BlockOrItemSpecWithDescription bs : this.params.getBlock())
        {
            for (String s : bs.getType())
            {
//                Block b = Block.getBlockFromName(bt.value());
//                this.blockTypeNames.add(b.getUnlocalizedName().toLowerCase());
            }
        }
        return true;
    }

    @Override
    public boolean doIWantToQuit(MissionInit missionInit)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanup()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getOutcome()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
