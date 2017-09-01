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

import com.microsoft.Malmo.MissionHandlerInterfaces.ICommandHandler;
import com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer;
import com.microsoft.Malmo.Schemas.ChatCommand;
import com.microsoft.Malmo.Schemas.ChatMatchSpec;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.RewardForSendingMatchingChatMessage;

import java.util.*;
import java.util.regex.*;

public class RewardForSendingMatchingChatMessageImplementation extends RewardBase implements IRewardProducer {

    private RewardForSendingMatchingChatMessage params;
    private HashMap<Pattern, Float> patternMap = new HashMap<Pattern, Float>();
    private HashMap<Pattern, String> distributionMap = new HashMap<Pattern, String>();

    /**
     * Attempt to parse the given object as a set of parameters for this handler.
     *
     * @param params the parameter block to parse
     * @return true if the object made sense for this handler; false otherwise.
     */
    @Override
    public boolean parseParameters(Object params) {
        super.parseParameters(params);
        if (params == null || !(params instanceof RewardForSendingMatchingChatMessage))
            return false;

        this.params = (RewardForSendingMatchingChatMessage) params;
        for (ChatMatchSpec cm : this.params.getChatMatch())
            addChatMatchSpecToRewardStructure(cm);

        return true;
    }

    /**
     * Helper function for adding a chat match specification to the pattern map.
     * @param c the chat message specification that contains the pattern and reward.
     */
    private void addChatMatchSpecToRewardStructure(ChatMatchSpec c) {
        Float reward = c.getReward().floatValue();
        Pattern pattern = Pattern.compile(c.getRegex(), Pattern.CASE_INSENSITIVE);
        patternMap.put(pattern, reward);
        distributionMap.put(pattern, c.getDistribution());
    }

    /**
     * Get the reward value for the current Minecraft state.
     *
     * @param missionInit the MissionInit object for the currently running mission,
     *                    which may contain parameters for the reward requirements.
     * @param reward
     */
    @Override
    public void getReward(MissionInit missionInit, MultidimensionalReward reward) {
        super.getReward(missionInit, reward);
    }

    /**
     * Called once before the mission starts - use for any necessary
     * initialisation.
     *
     * @param missionInit
     */
    @Override
    public void prepare(MissionInit missionInit) {
        super.prepare(missionInit);
        // We need to see chat commands as they come in.
        // Following the example of RewardForSendingCommandImplementation.
        MissionBehaviour mb = parentBehaviour();
        ICommandHandler oldch = mb.commandHandler;
        CommandGroup newch = new CommandGroup() {
            protected boolean onExecute(String verb, String parameter, MissionInit missionInit) {
                // See if this command gets handled by the legitimate handlers:
                boolean handled = super.onExecute(verb, parameter, missionInit);
                if (handled && verb.equalsIgnoreCase(ChatCommand.CHAT.value())) // Yes, so check if we need to produce a reward
                {
                    Iterator<Map.Entry<Pattern, Float>> patternIt = patternMap.entrySet().iterator();
                    while (patternIt.hasNext()) {
                        Map.Entry<Pattern, Float> entry = patternIt.next();
                        Matcher m = entry.getKey().matcher(parameter);
                        if (m.matches()) {
                            String distribution = distributionMap.get(entry.getKey());
                            addAndShareCachedReward(RewardForSendingMatchingChatMessageImplementation.this.params.getDimension(), entry.getValue(), distribution);
                        }
                    }
                }
                return handled;
            }
        };

        newch.setOverriding((oldch != null) ? oldch.isOverriding() : true);
        if (oldch != null)
            newch.addCommandHandler(oldch);
        mb.commandHandler = newch;
    }

    /**
     * Called once after the mission ends - use for any necessary cleanup.
     */
    @Override
    public void cleanup() {
        super.cleanup();
    }
}
