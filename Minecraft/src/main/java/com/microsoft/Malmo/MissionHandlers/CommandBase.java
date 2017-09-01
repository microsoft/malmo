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

package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
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

        // We expect the first word to be the command, and the rest of the string to be parameters, if present.
        String[] parms = command.split(" ", 2);
        String verb = parms[0].toLowerCase();
        String parameter = (parms.length > 1) ? parms[1] : "";

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