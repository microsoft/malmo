package com.microsoft.Malmo.MissionHandlers;

import java.util.List;

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.AgentQuitFromReachingCommandQuota;
import com.microsoft.Malmo.Schemas.CommandQuota;
import com.microsoft.Malmo.Schemas.MissionInit;

public class AgentQuitFromReachingCommandQuotaImplementation extends HandlerBase implements IWantToQuit
{
    AgentQuitFromReachingCommandQuota quotaparams;
    String quitCode = "";
    int[] quotas;
    Integer totalQuota = 0;
    boolean quotaExceeded = false;
    
    @Override
    public boolean doIWantToQuit(MissionInit missionInit)
    {
        return this.quotaExceeded;
    }

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof AgentQuitFromReachingCommandQuota))
            return false;
        
        this.quotaparams = (AgentQuitFromReachingCommandQuota)params;
        initialiseQuotas();
        return true;
    }

    private void initialiseQuotas()
    {
        this.totalQuota = this.quotaparams.getTotal();
        this.quotas = new int[this.quotaparams.getQuota().size()];
        int i = 0;
        for (CommandQuota cq : this.quotaparams.getQuota())
        {
            this.quotas[i] = cq.getQuota();
            i++;
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        // We need to see the commands as they come in, so we can calculate the quota usage.
        // To do this we create our own command handler and insert it at the root of the command chain.
        // This is also how the ObservationFromRecentCommands and RewardForSendingCommands handlers work.
        // It's slightly dirty behaviour, but it's cleaner than the other options!
        MissionBehaviour mb = parentBehaviour();
        ICommandHandler oldch = mb.commandHandler;
        CommandGroup newch = new CommandGroup() {
            protected boolean onExecute(String verb, String parameter, MissionInit missionInit)
            {
                // See if this command gets handled by the legitimate handlers:
                boolean handled = super.onExecute(verb, parameter, missionInit);
                if (handled)    // Yes, so record it:
                {
                    checkQuotas(verb, parameter);
                }
                return handled;
            }
        };
        
        newch.setOverriding((oldch != null) ? oldch.isOverriding() : true);
        if (oldch != null)
            newch.addCommandHandler(oldch);
        mb.commandHandler = newch;
    }

    private void checkQuotas(String verb, String parameter)
    {
        // Has the total been exceeded?
        if (this.totalQuota != null)
        {
            this.totalQuota--;
            if (this.totalQuota == 0)
            {
                this.quotaExceeded = true;
                this.quitCode = this.quotaparams.getDescription();
                return; // Total takes precedence - don't bother with the rest.
            }
        }
        // See which quotas are matched by this command:
        int i = 0;
        for (CommandQuota cq : this.quotaparams.getQuota())
        {
            List<String> comms = cq.getCommands();
            if (comms != null && comms.contains(verb))
            {
                this.quotas[i]--;
                if (this.quotas[i] == 0)
                {
                    this.quitCode = cq.getDescription();
                    this.quotaExceeded = true;
                    return; // Don't bother examining the rest.
                }
            }
            i++;
        }
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public String getOutcome()
    {
        return this.quitCode;
    }
}
