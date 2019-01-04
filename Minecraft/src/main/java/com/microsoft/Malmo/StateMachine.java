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

package com.microsoft.Malmo;

import java.util.ArrayList;
import java.util.logging.Level;

import net.minecraftforge.common.MinecraftForge;

import com.microsoft.Malmo.Utils.TCPUtils;

/**
 * Class designed to track and control the state of the mod, especially regarding mission launching/running.<br>
 * States are defined by the MissionState enum, and control is handled by MissionStateEpisode subclasses.
 * The ability to set the state directly is restricted, but hooks such as onPlayerReadyForMission etc are exposed to allow
 * subclasses to react to certain state changes.<br>
 * The ProjectMalmo mod app class inherits from this and uses these hooks to run missions.
 */
abstract public class StateMachine
{
    private IState state;

    private EpisodeEventWrapper eventWrapper = null;
    private String errorDetails = "";
	private Thread homeThread;

    public void clearErrorDetails()
    {
        synchronized (this.errorDetails)
        {
            this.errorDetails = "";
        }
    }
    
    public void saveErrorDetails(String error)
    {
        synchronized (this.errorDetails)
        {
            this.errorDetails += error + "\n";
        }
    }
    
    public String getErrorDetails()
    {
        String ret = "";
        synchronized (this.errorDetails)
        {
            ret = this.errorDetails;
        }
        return ret;
    }
    
    /** A queue of the next episodes to advance to.<br>
     * Should only be required for occasions where a thread other than the home thread instigated the change of state,
     * so we need to queue the state change and allow the home thread to act on it instead.
     */
    private ArrayList<IState> stateQueue = new ArrayList<IState>();
    
    public StateMachine(IState initialState)
    {
        // Create an EventWrapper to handle the forwarding of events to the mission episodes.
        this.eventWrapper = new EpisodeEventWrapper();
        setState(initialState);

        // Save the current thread as our "home" thread - state changes will only be allowed to happen on this thread.
        this.homeThread = Thread.currentThread();
        
        // Register the EventWrapper on the event busses:
        MinecraftForge.EVENT_BUS.register(this.eventWrapper);
    }
    
    /** Private method to set the state - not available to subclasses.<br>
     * Restricted to ensure mod never gets into an illegal state.
     * @param toState state we are transitioning to.
     */
    private void setState(IState toState)
    {
        // We want to make sure state changes happen purely on the "home" thread,
        // for the sake of sanity and avoiding many obscure race-condition bugs.
        if (Thread.currentThread() == this.homeThread)
        {
            // We are on the home thread, so safe to proceed:
            if (this.state != toState)
            {
                System.out.println(getName() + " enter state: " + toState);
                TCPUtils.Log(Level.INFO, "======== " + getName() + " enter state: " + toState + " ========");
                this.state = toState;
                onPreStateChange(toState);
                onStateChange();
            }
        }
        else
        {
            // We are not on the home thread; queue this state transition
            // so that the home thread can act on it.
            queueStateChange(toState);
        }
    }
    
    /** Get the state this machine is currently in. Returns null if the state machine is about to change state (ie a state change has been requested).
     * @return The state which the machine is currently in.
     */
    public IState getStableState()
    {
        synchronized(this.stateQueue)
        {
            if (this.stateQueue.size() == 0)
            	return this.state;
        }
        return null;
    }
    
    /** Add this state to the queue so that the client thread can process the state transition when it is next in control.<br>
     * We only allow the client thread to transition the state.
     * @param state the state to transition to.
     */
    public void queueStateChange(IState state)
    {
        synchronized(this.stateQueue)
        {
            if (this.stateQueue.size() != 0)
            {
                // The queue is only a method for ensuring the transition is carried out on the correct thread - transitions should
                // never happen so quickly that we end up with more than one state in the queue.
                System.out.println("STATE ERROR - multiple states in the queue.");
            }
            this.stateQueue.add(state);
            System.out.println(getName() + " request state: " + state);
            TCPUtils.Log(Level.INFO, "-------- " + getName() + " request state: " + state + " --------");
        }
    }

    /** Call this regularly to give the state machine a chance to transition to the next state.<br>
     * Must be called from the home thread.
     */
    public void updateState() {
        if (Thread.currentThread() == this.homeThread) {
            IState state = null;
            // Check the state queue to see if we need to carry out a transition:
            synchronized (this.stateQueue) {
                if (this.stateQueue.size() > 0) {
                    state = this.stateQueue.remove(0);
                }
            }
            if (state != null) {
                setState(state);   // Transition to the next state.
            }
        }
    }

    /** Used mainly for diagnostics - override to return a human-readable name for your state machine.
     */
    protected abstract String getName();
 
    /** Used mainly for diagnostics - called just before the state change happens - override to print debugging info etc.
     */
    protected abstract void onPreStateChange(IState toState);
    
    /** For each state change, kick off the next required action.<br>
     * This was implemented initially to allow us to handle the loading of maps in multiple stages,<br>
     * giving Minecraft a chance to respond in between each stage. This theoretically minimises race conditions etc.<br>
     * For each new state, we create a MissionStateEpisode, which contains all the code relating to that event:<br>
     * what action to take, how to tell when the state can move on, and what state it should move on into.
     */
    private void onStateChange()
    {
        StateEpisode stateEpisode = getStateEpisodeForState(this.state);
        StateEpisode lastEpisode = this.eventWrapper.setStateEpisode(stateEpisode);
        if (lastEpisode != null)
        	lastEpisode.cleanup();
        
        if (stateEpisode != null)
            stateEpisode.start();
    }
    
    /** Create the episode object for the requested state.
     * @param state the state the mod is entering
     * @return a MissionStateEpisode that localises all the logic required to run this state
     */
    abstract protected StateEpisode getStateEpisodeForState(IState state);
}
