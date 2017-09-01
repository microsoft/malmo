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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.google.gson.JsonObject;
import com.microsoft.Malmo.Schemas.GridDefinition;
import com.microsoft.Malmo.Schemas.ObservationFromGrid;
import com.microsoft.Malmo.Utils.JSONWorldDataHelper;
import com.microsoft.Malmo.Utils.JSONWorldDataHelper.GridDimensions;

/** IObservationProducer that spits out block types of the cell around the player.<br>
 * The size of the cell can be specified in the MissionInit XML.
 * The default is (3x3x4) - a one block hull around the player.
 */
public class ObservationFromGridImplementation extends ObservationFromServer
{
    public static class SimpleGridDef	// Could use the JAXB-generated GridDefinition class, but this is safer/simpler.
    {
        int xMin;
        int yMin;
        int zMin;
        int xMax;
        int yMax;
        int zMax;
        String name;
        boolean absoluteCoords;
        SimpleGridDef(int xmin, int ymin, int zmin, int xmax, int ymax, int zmax, String name, boolean absoluteCoords)
        {
            this.xMin = xmin;
            this.yMin = ymin;
            this.zMin = zmin;
            this.xMax = xmax;
            this.yMax = ymax;
            this.zMax = zmax;
            this.name = name;
            this.absoluteCoords = absoluteCoords;
        }
        GridDimensions getEnvirons()
        {
            GridDimensions env = new GridDimensions();
            env.xMax = this.xMax;
            env.yMax = this.yMax;
            env.zMax = this.zMax;
            env.xMin = this.xMin;
            env.yMin = this.yMin;
            env.zMin = this.zMin;
            env.absoluteCoords = this.absoluteCoords;
            return env;
        }
    }

    private List<SimpleGridDef> environs = null;

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof ObservationFromGrid))
            return false;

        ObservationFromGrid ogparams = (ObservationFromGrid)params;
        this.environs = new ArrayList<SimpleGridDef>();
        for (GridDefinition gd : ogparams.getGrid())
        {
            SimpleGridDef sgd = new SimpleGridDef(
                    gd.getMin().getX().intValue(),
                    gd.getMin().getY().intValue(),
                    gd.getMin().getZ().intValue(),
                    gd.getMax().getX().intValue(),
                    gd.getMax().getY().intValue(),
                    gd.getMax().getZ().intValue(),
                    gd.getName(),
                    gd.isAbsoluteCoords());
            this.environs.add(sgd);
        }
        return true;
    }

    public static class GridRequestMessage extends ObservationFromServer.ObservationRequestMessage
    {
        private List<SimpleGridDef> environs = null;

        public GridRequestMessage()	// Needed so FML can instantiate our class using reflection.
        {
        }

        public GridRequestMessage(List<SimpleGridDef> environs)
        {
            this.environs = environs;
        }

        @Override
        void restoreState(ByteBuf buf)
        {
            int numGrids = buf.readInt();
            this.environs = new ArrayList<SimpleGridDef>();
            for (int i = 0; i < numGrids; i++)
            {
                SimpleGridDef sgd = new SimpleGridDef(buf.readInt(), buf.readInt(), buf.readInt(),
                                                      buf.readInt(), buf.readInt(), buf.readInt(), 
                                                      ByteBufUtils.readUTF8String(buf),
                                                      buf.readBoolean());
                this.environs.add(sgd);
            }
        }

        @Override
        void persistState(ByteBuf buf)
        {
            buf.writeInt(this.environs.size());
            for (SimpleGridDef sgd : this.environs)
            {
                buf.writeInt(sgd.xMin);
                buf.writeInt(sgd.yMin);
                buf.writeInt(sgd.zMin);
                buf.writeInt(sgd.xMax);
                buf.writeInt(sgd.yMax);
                buf.writeInt(sgd.zMax);
                ByteBufUtils.writeUTF8String(buf, sgd.name);
                buf.writeBoolean(sgd.absoluteCoords);
            }
        }

        List<SimpleGridDef>getEnvirons() { return this.environs; }
    }

    public static class GridRequestMessageHandler extends ObservationFromServer.ObservationRequestMessageHandler implements IMessageHandler<GridRequestMessage, IMessage>
    {
        @Override
        void buildJson(JsonObject json, EntityPlayerMP player, ObservationRequestMessage message)
        {
            if (message instanceof GridRequestMessage)
            {
                List<SimpleGridDef> environs = ((GridRequestMessage)message).getEnvirons();
                if (environs != null)
                {
                    for (SimpleGridDef sgd : environs)
                    {
                        JSONWorldDataHelper.buildGridData(json, sgd.getEnvirons(), player, sgd.name);
                    }
                }
            }
        }

        @Override
        public IMessage onMessage(GridRequestMessage message, MessageContext ctx)
        {
            return processMessage(message, ctx);
        }
    }

    @Override
    public ObservationRequestMessage createObservationRequestMessage()
    {
        return new GridRequestMessage(this.environs);
    }
}
