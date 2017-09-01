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

/** Lightweight baseclass for all handlers - provides access to parameters from the XML file.
 */
public class HandlerBase
{
    /** A static list of all classes that implement a handler - used to aid dynamic handler creation.
     */
    static ArrayList<String> handlers;
    
    /** Pointer up to the MissionBehaviour object that created us - useful for certain mission handlers.
     */
    private MissionBehaviour parentBehaviour = null;

    public HandlerBase() {}

    public void setParentBehaviour(MissionBehaviour mb)
    {
    	this.parentBehaviour = mb;
    }

    protected MissionBehaviour parentBehaviour()
    {
    	return this.parentBehaviour;
    }
    
    /** Attempt to parse the given object as a set of parameters for this handler.
     * @param params the parameter block to parse
     * @return true if the object made sense for this handler; false otherwise.
     */
    public boolean parseParameters(Object params)
    {
        return true;
    }

    /** Our chance to add any info the server might need before the mission starts.
     * @param map Map of data sent to server in the client's ping message.
     */
    public void appendExtraServerInformation(HashMap<String, String> map)
    {
        // Mostly does nothing, but, for example, TurnBasedCommandsImplementation uses this
        // in order to register with the server.
    }
}