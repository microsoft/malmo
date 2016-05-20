package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.Schemas.ChatCommand;
import com.microsoft.Malmo.Schemas.CommandListModifier;
import com.microsoft.Malmo.Schemas.MissionInit;


/** Base class for Command handlers - provides XML parsing to build up an allowed/disallowed list of commands.
 */
public abstract class CommandBase extends HandlerBase implements ICommandHandler
{
    private List<String> commandsAllowList = null;
    private List<String> commandsDenyList = null;
    
    protected boolean isCommandAllowed(String verb)
    {
        if (this.commandsDenyList == null && this.commandsAllowList == null)
            return true;    // Everything is enabled by default
        
        if (this.commandsDenyList != null && this.commandsDenyList.contains(verb.toLowerCase())) {
            System.out.println("command verb on the deny-list: "+verb);
            return false;   // If the verb is on the deny list, disallow it
        }
        
        if (this.commandsAllowList != null && !this.commandsAllowList.contains(verb.toLowerCase())) {
            System.out.println("command verb not on the allow-list: "+verb);
            for(String v : this.commandsAllowList )
                System.out.println("("+v+" is allowed)");
            return false;   // If the command isn't on the allow list, disallow it
        }

        // Otherwise, all is good:
        return true;
    }
    
    public boolean execute(String command, MissionInit missionInit)
    {
        if (command == null || command.length() == 0)
        {
            return false;
        }

        // At the moment we expect commands to be of the form "verb parameter" (eg "move 0.34").
        // Chuck out anything which doesn't fit this pattern, unless a chat command.
        String[] parms = command.split(" ");
        if(parms.length < 2)
        {
            System.out.println("command has too few parameters: "+command);
            return false;
        }
        String verb = parms[0].toLowerCase();
        String parameter = command.substring( verb.length() + 1 );

        // Also chuck out any commands which aren't on our allow list / are on our deny list:
        if (!isCommandAllowed(verb))
        {
            return false;
        }

        // All okay, so pass to subclass for handling:
        return onExecute(verb, parameter, missionInit);
    }

    protected void setUpAllowAndDenyLists(CommandListModifier list)
    {
        this.commandsDenyList = null;
        this.commandsAllowList = null;
        if (list != null && list.getCommand() != null)
        {
        	ArrayList<String> listcopy = new ArrayList<String>();
        	listcopy.addAll(list.getCommand());
            if (list.getType().equalsIgnoreCase("deny-list"))
            	this.commandsDenyList = listcopy;
            else
            	this.commandsAllowList = listcopy;
        }
    }
    
    abstract protected boolean onExecute(String verb, String parameter, MissionInit missionInit);
}