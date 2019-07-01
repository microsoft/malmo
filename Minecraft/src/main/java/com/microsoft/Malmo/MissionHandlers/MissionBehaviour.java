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
import java.util.List;
import java.util.logging.Logger;

import com.microsoft.Malmo.MissionHandlerInterfaces.IAudioProducer;
import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.MissionHandlerInterfaces.IPerformanceProducer;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.MissionHandlerInterfaces.IVideoProducer;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldGenerator;
import com.microsoft.Malmo.Schemas.AgentHandlers;
import com.microsoft.Malmo.Schemas.AgentSection;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ServerHandlers;
import com.microsoft.Malmo.Utils.TimeHelper;

/** Holder class for the various MissionHandler interfaces that together define the behaviour of the mission.<br>
 */
public class MissionBehaviour
{
    public List<IVideoProducer> videoProducers = new ArrayList<IVideoProducer>();
    public IAudioProducer audioProducer = null;
    public ICommandHandler commandHandler = null;
    public IObservationProducer observationProducer = null;
    public IRewardProducer rewardProducer = null;
    public IWorldDecorator worldDecorator = null;
    public IWorldGenerator worldGenerator = null;
    public IPerformanceProducer performanceProducer = null;
    public IWantToQuit quitProducer = null;

    private String failedHandlers = "";
    
    /** Create instances of the various mission handlers, according to the specifications in the MissionInit object.<br>
     * The Mission object (inside MissionInit) contains an optional string for each type of handler, which specifies the class-name of the handler required.<br>
     * This method will attempt to instantiate all the requested objects.<br>
     * Any objects that are left unspecified by the MissionInit, or are unable to be created, are left as null.
     * @param missionInit the MissionInit object for the current Mission, containing information about which handler objects to instantiate.
     * @return a MissionBehaviour object that holds all the requested handlers (assuming they could be created).
     */
    public static MissionBehaviour createAgentHandlersFromMissionInit(MissionInit missionInit) throws Exception
    {
    	MissionBehaviour behaviour = new MissionBehaviour();
    	behaviour.initAgent(missionInit);
    	// TODO - can't throw and return a behaviour!!
        if (behaviour.getErrorReport() != null && behaviour.getErrorReport().length() > 0)
            System.out.println("[ERROR] " + behaviour.getErrorReport());
    	
        //    throw new Exception(behaviour.getErrorReport());
        
    	return behaviour;
    }

    public static MissionBehaviour createServerHandlersFromMissionInit(MissionInit missionInit) throws Exception
    {
    	MissionBehaviour behaviour = new MissionBehaviour();
    	behaviour.initServer(missionInit);
    	// TODO - can't throw and return a behaviour!!
        if (behaviour.getErrorReport() != null && behaviour.getErrorReport().length() > 0)
            System.out.println("[ERROR] " + behaviour.getErrorReport());
    	
    	return behaviour;
    }

    public String getErrorReport()
    {
        return this.failedHandlers;
    }

    private void reset()
    {
        this.videoProducers = new ArrayList<IVideoProducer>();
        this.audioProducer = null;
        this.commandHandler = null;
        this.observationProducer = null;
        this.rewardProducer = null;
        this.worldDecorator = null;
        this.quitProducer = null;
        this.performanceProducer = null;
    }

    private void initAgent(MissionInit missionInit)
    {
        reset();
        AgentHandlers handlerset = missionInit.getMission().getAgentSection().get(missionInit.getClientRole()).getAgentHandlers();
        // Instantiate the various handlers:
        for (Object handler : handlerset.getAgentMissionHandlers())
            createAndAddHandler(handler);

        // If this is a multi-agent mission, need to ensure we have a team reward handler
        // to receive rewards from other agents.
        List<AgentSection> agents = missionInit.getMission().getAgentSection();
        if (agents != null && agents.size() > 1)
            addHandler(new RewardFromTeamImplementation());
    }

    public boolean addExtraHandlers(List<Object> handlers)
    {
        for (Object handler : handlers)
            createAndAddHandler(handler);
        return true;
    }

    private void initServer(MissionInit missionInit)
    {
        reset();
        ServerHandlers handlerset = missionInit.getMission().getServerSection().getServerHandlers();

        // Instantiate the various handlers:
        createAndAddHandler(handlerset.getWorldGenerator());
        for (Object handler : handlerset.getWorldDecorators())
            createAndAddHandler(handler);
        for (Object handler : handlerset.getServerQuitProducers())
            createAndAddHandler(handler);
    }

    private void createAndAddHandler(Object xmlObj)
    {
        if (xmlObj == null)
            return;

        Object handler = createHandlerFromParams(xmlObj);
        if (handler != null)
        {
        	if (handler instanceof HandlerBase)
        		((HandlerBase)(handler)).setParentBehaviour(this);
            addHandler(handler);
        }
    }

    /** Add this handler to our set, creating containers as needs be.
     * @param handler The handler to add.
     */
    private void addHandler(Object handler)
    {
        // Would be nice to have a better way to do this,
        // but the type information isn't preserved in the XML format anymore -
        // and the number of types of handler is pretty unlikely to change, so this list
        // won't have to be added to often, if at all.
        if (handler == null)
            return;

        if (handler instanceof IVideoProducer)
            addVideoProducer((IVideoProducer)handler);
        else if (handler instanceof IAudioProducer)
            addAudioProducer((IAudioProducer)handler);
        else if (handler instanceof IPerformanceProducer)
            addPerformanceProducer((IPerformanceProducer)handler);
        else if (handler instanceof ICommandHandler)
            addCommandHandler((ICommandHandler)handler);
        else if (handler instanceof IObservationProducer)
            addObservationProducer((IObservationProducer)handler);
        else if (handler instanceof IRewardProducer)
            addRewardProducer((IRewardProducer)handler);
        else if (handler instanceof IWorldGenerator)
            addWorldGenerator((IWorldGenerator)handler);
        else if (handler instanceof IWorldDecorator)
            addWorldDecorator((IWorldDecorator)handler);
        else if (handler instanceof IWantToQuit)
            addQuitProducer((IWantToQuit)handler);
        else
            this.failedHandlers += handler.getClass().getSimpleName() + " isn't of a recognised handler type.\n";
    }
    
    private void addVideoProducer(IVideoProducer handler)
    {
        if (this.videoProducers.size() > 0 && (this.videoProducers.get(0).getHeight() != handler.getHeight() || this.videoProducers.get(0).getWidth() != handler.getWidth()))
            this.failedHandlers += "If multiple video producers are specified, they must all share the same dimensions.\n";
        else
            this.videoProducers.add(handler);
    }
    
    private void addPerformanceProducer(IPerformanceProducer handler){
        if (this.performanceProducer != null)
            this.failedHandlers += "Too many audio producers specified - only one allowed at present.\n";
        else
            this.performanceProducer = handler;
    }

    private void addAudioProducer(IAudioProducer handler)
    {
        if (this.audioProducer != null)
            this.failedHandlers += "Too many audio producers specified - only one allowed at present.\n";
        else
            this.audioProducer = handler;
    }

    private void addWorldGenerator(IWorldGenerator handler)
    {
        if (this.worldGenerator != null)
            this.failedHandlers += "Too many world generators specified - only one allowed.\n";
        else
            this.worldGenerator = handler;
    }
    
    public void addRewardProducer(IRewardProducer handler)
    {
        if (this.rewardProducer == null)
            this.rewardProducer = handler;
        else
        {
            if (!(this.rewardProducer instanceof RewardGroup) || ((RewardGroup) this.rewardProducer).isFixed())
            {
                // We have multiple reward handlers - group them.
                RewardGroup group = new RewardGroup();
                group.addRewardProducer(this.rewardProducer);
                this.rewardProducer = group;
            }
            ((RewardGroup) this.rewardProducer).addRewardProducer(handler);
        }
    }

    public void addCommandHandler(ICommandHandler handler)
    {
        if (this.commandHandler == null)
            this.commandHandler = handler;
        else
        {
            if (!(this.commandHandler instanceof CommandGroup) || ((CommandGroup)this.commandHandler).isFixed())
            {
                // We have multiple command handlers - group them.
                CommandGroup group = new CommandGroup();
                group.addCommandHandler(this.commandHandler);
                this.commandHandler = group;
            }
            ((CommandGroup)this.commandHandler).addCommandHandler(handler);
        }
    }
    
    public void addObservationProducer(IObservationProducer handler)
    {
        if (this.observationProducer == null)
            this.observationProducer = handler;
        else
        {
            if (!(this.observationProducer instanceof ObservationFromComposite) || ((ObservationFromComposite)this.observationProducer).isFixed())
            {
                ObservationFromComposite group = new ObservationFromComposite();
                group.addObservationProducer(this.observationProducer);
                this.observationProducer = group;
            }
            ((ObservationFromComposite)this.observationProducer).addObservationProducer(handler);
        }
    }
 
    public void addWorldDecorator(IWorldDecorator handler)
    {
        if (this.worldDecorator == null)
            this.worldDecorator = handler;
        else
        {
            if (!(this.worldDecorator instanceof WorldFromComposite) || ((WorldFromComposite)this.worldDecorator).isFixed())
            {
                WorldFromComposite group = new WorldFromComposite();
                group.addBuilder(this.worldDecorator);
                this.worldDecorator = group;
            }
            ((WorldFromComposite)this.worldDecorator).addBuilder(handler);
        }
    }
 
    public void addQuitProducer(IWantToQuit handler)
    {
        if (this.quitProducer == null)
            this.quitProducer = handler;
        else
        {
            if (!(this.quitProducer instanceof QuitFromComposite) || ((QuitFromComposite)this.quitProducer).isFixed())
            {
                QuitFromComposite group = new QuitFromComposite();
                group.addQuitter(this.quitProducer);
                this.quitProducer = group;
            }
            ((QuitFromComposite)this.quitProducer).addQuitter(handler);
        }
    }
   
    /** Attempt to create an instance of the specified handler class, using reflection.
     * @param xmlHandler the object which specifies both the name and the parameters of the requested handler.
     * @return an instance of the requested class, if possible, or null if the class wasn't found.
     */
    private Object createHandlerFromParams(Object xmlHandler)
    {
        if (xmlHandler == null)
            return null;
        
        Object handler = null;
        String handlerClass = xmlHandler.getClass().getSimpleName();
        if (handlerClass == null || handlerClass.length() == 0)
        {
            return null;
        }
        try
        {
            // To avoid name collisions, the java class will have the suffix "Implementation".
            Class<?> c = Class.forName("com.microsoft.Malmo.MissionHandlers." + handlerClass + "Implementation");
            handler = c.newInstance();
            if (!((HandlerBase)handler).parseParameters(xmlHandler))
                this.failedHandlers += handlerClass + " failed to parse parameters.\n";
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Duff MissionHandler requested: "+handlerClass);
            this.failedHandlers += "Failed to find " + handlerClass + "\n";
        }
        catch (InstantiationException e)
        {
            System.out.println("Could not instantiate specified MissionHandler.");
            this.failedHandlers += "Failed to create " + handlerClass + "\n";
        }
        catch (IllegalAccessException e)
        {
            System.out.println("Could not instantiate specified MissionHandler.");
            this.failedHandlers += "Failed to access " + handlerClass + "\n";
        }
        return handler;
    }

    /** This method gives our handlers a chance to add any information to the ping message
     * which the client sends (repeatedly) to the server while the agents are assembling.
     * This message is guaranteed to get through to the server, so it is a good place to
     * communicate.
     * (NOTE this is called BEFORE addExtraHandlers - but that mechanism is provided to allow
     * the *server* to add extra handlers on the *client* - so the server should already know
     * whatever the extra handlers might want to tell it!)
     * @param map the map of data passed to the server
     */
    public void appendExtraServerInformation(HashMap<String, String> map)
    {
        List<HandlerBase> handlers = getClientHandlerList();
        for (HandlerBase handler : handlers)
            handler.appendExtraServerInformation(map);
    }

    protected List<HandlerBase> getClientHandlerList()
    {
        List<HandlerBase> handlers = new ArrayList<HandlerBase>();
        for (IVideoProducer vp : this.videoProducers)
        {
            if (vp != null && vp instanceof HandlerBase)
                handlers.add((HandlerBase)vp);
        } 
        if (this.audioProducer != null && this.audioProducer instanceof HandlerBase)
            handlers.add((HandlerBase)this.audioProducer);
        if (this.commandHandler != null && this.commandHandler instanceof HandlerBase)
            handlers.add((HandlerBase)this.commandHandler);
        if (this.observationProducer != null && this.observationProducer instanceof HandlerBase)
            handlers.add((HandlerBase)this.observationProducer);
        if (this.rewardProducer != null && this.rewardProducer instanceof HandlerBase)
            handlers.add((HandlerBase)this.rewardProducer);
        if (this.quitProducer != null && this.quitProducer instanceof HandlerBase)
            handlers.add((HandlerBase)this.quitProducer);
        if (this.performanceProducer != null && this.performanceProducer instanceof HandlerBase)
            handlers.add((HandlerBase)this.performanceProducer);
        return handlers;
    }
}
