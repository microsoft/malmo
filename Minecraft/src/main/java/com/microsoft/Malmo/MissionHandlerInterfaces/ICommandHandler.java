package com.microsoft.Malmo.MissionHandlerInterfaces;

import com.microsoft.Malmo.Schemas.MissionInit;

/** Interface for command handlers.<br>
 * These objects will take string commands from an agent (eg AI), and translate them into Minecraft actions (eg jump, crouch, etc.)
 */
public interface ICommandHandler
{
    /** Perform the requested command, if it makes sense to do so.<br>
     * Commands are received via Sockets, and sent to all command handlers in turn, until one returns true.
     * @param command the string command received from the agent.
     * @param missionInit details of the current mission
     * @return true if this command has been handled; false if the Mod should continue passing it to other handlers.<br>
     * (It is up to the handler whether or not to "swallow" the command - it depends on whether we want multiple handlers to handle
     * the same command.)
     */
    public boolean execute(String command, MissionInit missionInit);

    /** Install this handler, in whatever way is necessary.<br>
     * @param missionInit details of the current mission
     */
    public void install(MissionInit missionInit);
    
    
    /** Extricate this handler from the Minecraft code.<br>
     * @param missionInit details of the current mission
     */
    public void deinstall(MissionInit missionInit);
    
    /**
     * @return true if this object is overriding the default Minecraft control method.
     */
    public boolean isOverriding();
    
    /** Switch this command handler on/off. If on, it will be overriding the default Minecraft control method.
     * @param b set this control on/off.
     */
    public void setOverriding(boolean b);
}
