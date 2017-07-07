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

package com.microsoft.Malmo.Utils;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.Session;

public class AuthenticationHelper
{
    public static boolean setPlayerName(Session currentSession, String newPlayerName)
    {
        if (currentSession.getUsername().equals(newPlayerName))
            return true;

        // Create new session object:
        Session newSession = new Session(newPlayerName, currentSession.getPlayerID(), currentSession.getToken(), "mojang"/*currentSession.getSessionType().toString()*/);
        newSession.setProperties(new com.mojang.authlib.properties.PropertyMap());  // Prevents calls to the session service to get profile properties
        return setSession(newSession);
    }

    private static boolean setSession(Session newSession)
    {
        // Are we in the dev environment or deployed?
        boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        // We need to know, because the member name will either be obfuscated or not.
        String sessionMemberName = devEnv ? "session" : "field_71449_j";
        // NOTE: obfuscated name may need updating if Forge changes - search for "session" in Malmo\Minecraft\build\tasklogs\retromapSources.log
        Field session;
        try
        {
            session = Minecraft.class.getDeclaredField(sessionMemberName);
            session.setAccessible(true);
            session.set(Minecraft.getMinecraft(), newSession);
            return true;
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        return false;
    }
}
