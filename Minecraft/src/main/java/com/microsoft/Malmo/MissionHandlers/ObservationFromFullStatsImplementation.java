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

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.Utils.JSONWorldDataHelper;

/**
 * Simple IObservationProducer object that pings out a whole bunch of data.<br>
 */
public class ObservationFromFullStatsImplementation extends ObservationFromServer
{
    public static class FullStatsRequestMessage extends ObservationFromServer.ObservationRequestMessage
    {
        @Override
        void restoreState(ByteBuf buf)
        {
            // Nothing to do - no context needed.
        }

        @Override
        void persistState(ByteBuf buf)
        {
            // Nothing to do - no context needed.
        }
    }

    public static class FullStatsRequestMessageHandler extends ObservationFromServer.ObservationRequestMessageHandler implements IMessageHandler<FullStatsRequestMessage, IMessage>
    {
        @Override
        void buildJson(JsonObject json, EntityPlayerMP player, ObservationRequestMessage message)
        {
            JSONWorldDataHelper.buildAchievementStats(json, player);
            JSONWorldDataHelper.buildLifeStats(json, player);
            JSONWorldDataHelper.buildPositionStats(json, player);
            JSONWorldDataHelper.buildEnvironmentStats(json, player);
        }

        @Override
        public IMessage onMessage(FullStatsRequestMessage message, MessageContext ctx)
        {
            return processMessage(message, ctx);
        }
    }

    @Override
    public ObservationRequestMessage createObservationRequestMessage()
    {
        return new FullStatsRequestMessage();
    }
}
