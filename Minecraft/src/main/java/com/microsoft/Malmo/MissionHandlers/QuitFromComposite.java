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
import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.MissionInit;

/** A composite object that allows multiple IWantToQuit objects to be chained together.<br>
 * This is useful if, for example, a mission can be ended by either reaching a goal, or running out of time.
 * Quitters can be combined using AND or OR; in AND mode, the mission only ends when <i>all</i> the IWantToQuit objects are returning true.<br>
 * (The default mode is OR, which seems more useful. There is currently no easy way to specify in the MissionInit that you want otherwise.)
 */
public class QuitFromComposite extends HandlerBase implements IWantToQuit
{
String quitCode = "";

    public enum CombineMode
    {
        /**
         * doIWantToQuit only returns true if all the child IWantToQuit objects return true.
         */
        Combine_AND,
        /**
         * doIWantToQuit returns true if any of the child IWantToQuit objects return true.
         */
        Combine_OR
    }

    private ArrayList<IWantToQuit> quitters;
    private CombineMode mode = CombineMode.Combine_OR; // OR by default.

    /**
     * Add another IWantToQuit object to the children.
     * 
     * @param quitter the IWantToQuit object.
     */
    public void addQuitter(IWantToQuit quitter)
    {
        if (this.quitters == null)
        {
            this.quitters = new ArrayList<IWantToQuit>();
        }
        this.quitters.add(quitter);
    }

    /**
     * Change the way the child quitters impact the overall decision on whether or not to quit.
     * 
     * @param mode either AND or OR.
     */
    public void setCombineMode(CombineMode mode)
    {
        this.mode = mode;
    }

    @Override
    public boolean doIWantToQuit(MissionInit missionInit)
    {
        if (this.quitters == null)
        {
            return false;
        }
        boolean result = (this.mode == CombineMode.Combine_AND) ? true : false;
        for (IWantToQuit quitter : this.quitters)
        {
            boolean wantsToQuit = quitter.doIWantToQuit(missionInit);
            if (wantsToQuit)
                addQuitCode(quitter.getOutcome());
            if (this.mode == CombineMode.Combine_AND)
            {
                result &= wantsToQuit;
            }
            else
            {
                result |= wantsToQuit;
            }
        }
        return result;
    }

    private void addQuitCode(String qc)
    {
        if (qc == null || qc.isEmpty())
            return;
        if (this.quitCode == null || this.quitCode.isEmpty())
            this.quitCode = qc;
        else
            this.quitCode += ";" + qc;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        for (IWantToQuit quitter : this.quitters)
            quitter.prepare(missionInit);
    }

    @Override
    public void cleanup()
    {
        for (IWantToQuit quitter : this.quitters)
            quitter.cleanup();
    }

    @Override
    public void appendExtraServerInformation(HashMap<String, String> map)
    {
        for (IWantToQuit quitter : this.quitters)
        {
            if (quitter instanceof HandlerBase)
                ((HandlerBase)quitter).appendExtraServerInformation(map);
        }
    }

    @Override
    public String getOutcome()
    {
        return this.quitCode;
    }

    public boolean isFixed()
    {
        return false;   // Return true to stop MissionBehaviour from adding new handlers to this group.
    }
}
