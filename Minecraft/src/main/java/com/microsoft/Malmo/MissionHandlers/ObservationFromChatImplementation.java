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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.Malmo.MissionHandlerInterfaces.IObservationProducer;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Utils.ScreenHelper;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.text.TextFormatting;

public class ObservationFromChatImplementation extends HandlerBase implements IObservationProducer
{
    private static final String TITLE_TYPE = "Title";
    private static final String SUBTITLE_TYPE = "Subtitle";
    private static final String CHAT_TYPE = "Chat";

    private class ChatMessage
    {
        public String messageType;
        public String messageContent;
        public ChatMessage(String messageType, String messageContent)
        {
            this.messageType = messageType;
            this.messageContent = messageContent;
        }
    }

    private ArrayList<ChatMessage> chatMessagesReceived = new ArrayList<ChatMessage>();

    @Override
    public void writeObservationsToJSON(JsonObject json, MissionInit missionInit)
    {
        if (!this.chatMessagesReceived.isEmpty())
        {
            HashMap<String, ArrayList<String>> lists = new HashMap<String, ArrayList<String>>();
            for (ChatMessage message : this.chatMessagesReceived)
            {
                ArrayList<String> arr = lists.get(message.messageType);
                if (arr == null)
                {
                    arr = new ArrayList<String>();
                    lists.put(message.messageType, arr);
                }
                arr.add(message.messageContent);
            }
            for (String key : lists.keySet())
            {
                JsonArray jarr = new JsonArray();
                for (String message : lists.get(key))
                {
                    jarr.add(new JsonPrimitive(message));
                }
                json.add(key, jarr);
            }
            this.chatMessagesReceived.clear();
        }
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void cleanup()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onTitleChange(ScreenHelper.TitleChangeEvent event)
    {
        if (event.title != null)
            this.chatMessagesReceived.add(new ChatMessage(TITLE_TYPE, TextFormatting.getTextWithoutFormattingCodes(event.title)));
        if (event.subtitle != null)
            this.chatMessagesReceived.add(new ChatMessage(SUBTITLE_TYPE, TextFormatting.getTextWithoutFormattingCodes(event.subtitle)));
    }

    @SubscribeEvent
    public void onEvent(ClientChatReceivedEvent event)
    {
        this.chatMessagesReceived.add(new ChatMessage(CHAT_TYPE, event.getMessage().getUnformattedText()));
    }
}
