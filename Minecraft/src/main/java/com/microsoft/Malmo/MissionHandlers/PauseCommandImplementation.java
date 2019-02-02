// ---------------------------------------------------------
// Author: William Guss 2019
// ---------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.TimeHelper;

public class PauseCommandImplementation extends CommandBase {
    public PauseCommandImplementation(){
    }

    @Override
    public void install(MissionInit missionInit) {  }

    @Override
    public void deinstall(MissionInit missionInit) {  }

    @Override
    public boolean isOverriding() {
        return false;
    }

    @Override
    public void setOverriding(boolean b) {
    }

    @Override
    protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
        if (verb.equals("pause")){
            if(parameter.equals("1")){
                TimeHelper.pause();
            } else if(parameter.equals("0")){
                TimeHelper.unpause();
            }
        }
        return true;
    }
}
