package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;

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
        			ok &= ((HandlerBase)han).parseParameters(params);
        		}
        	}
		}
    	return ok;
    }
}
