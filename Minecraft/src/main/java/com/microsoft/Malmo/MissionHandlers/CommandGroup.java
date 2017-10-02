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
import java.util.HashMap;

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.Schemas.MissionInit;

/** Composite class that manages a set of ICommandHandler objects.<br>
 */
public class CommandGroup extends CommandBase
{
    private ArrayList<ICommandHandler> handlers;
    private boolean isOverriding = false;
    private boolean shareParametersWithChildren = false;

    public CommandGroup()
    {
        this.handlers = new ArrayList<ICommandHandler>();
    }

    /** Call this to give a copy of the group's parameter block to all children.<br>
     * Useful for composite handlers like CommandForStandardRobot etc, where the children
     * aren't specified in the XML directly and so have no other opportunity to be parameterised.
     * This must be set before parseParameters is called in order for it to take effect.
     * (parseParameters is called shortly after construction, so the constructor is the best place to set this.)
     * @param share true if the children should get a copy of the parent's parameter block.
     */
    protected void setShareParametersWithChildren(boolean share)
    {
        this.shareParametersWithChildren = share;
    }
    
    void addCommandHandler(ICommandHandler handler)
    {
        if (handler != null)
        {
            this.handlers.add(handler);
            handler.setOverriding(this.isOverriding);
        }
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
    {
        for (ICommandHandler han : this.handlers)
        {
            if (han.execute(verb + " " + parameter, missionInit))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void install(MissionInit missionInit)
    {
        for (ICommandHandler han : this.handlers)
        {
            han.install(missionInit);
        }
    }

    @Override
    public void deinstall(MissionInit missionInit)
    {
        for (ICommandHandler han : this.handlers)
        {
            han.deinstall(missionInit);
        }
    }

    @Override
    public boolean isOverriding()
    {
        return this.isOverriding;
    }

    @Override
    public void setOverriding(boolean b)
    {
        this.isOverriding = b;
        for (ICommandHandler han : this.handlers)
        {
            han.setOverriding(b);
        }
    }

    @Override
    public void setParentBehaviour(MissionBehaviour mb)
    {
        super.setParentBehaviour(mb);
        for (ICommandHandler han : this.handlers)
            ((HandlerBase)han).setParentBehaviour(mb);
    }

    @Override
    public void appendExtraServerInformation(HashMap<String, String> map)
    {
        for (ICommandHandler han : this.handlers)
        {
            if (han instanceof HandlerBase)
                ((HandlerBase)han).appendExtraServerInformation(map);
        }
    }

    @Override
    public boolean parseParameters(Object params)
    {
        // Normal handling:
        boolean ok = super.parseParameters(params);

        // Now, pass the params to each child handler, if that was requested:
        if (this.shareParametersWithChildren)
        {
            // AND the results, but without short-circuit evaluation.
            for (ICommandHandler han : this.handlers)
            {
                if (han instanceof HandlerBase)
                {
                    ok &= ((HandlerBase) han).parseParameters(params);
                }
            }
        }
        return ok;
    }

    public boolean isFixed()
    {
        return false;   // Return true to stop MissionBehaviour from adding new handlers to this group.
    }
}
