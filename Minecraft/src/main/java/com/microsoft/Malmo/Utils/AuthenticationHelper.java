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
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.Session;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.microsoft.Malmo.MalmoMod;
import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

public class AuthenticationHelper
{
    public static final String PROP_USERNAMES = "usernames";
    public static final String PROP_PORT_TO_USER_MAPPINGS = "portToUserMappings";
    public static final String PROP_USER_TO_PASSWORD_MAPPINGS = "usernameToPasswordMappings";
    protected static final String UNAUTH = "unauthenticated";

    public static String username = UNAUTH;
    public static String password = "";
    public static boolean isAuthenticated = true;

    public static class LoginDetails
    {
        public static final int nullPortMapping = -1;

        public String username;
        public int port;
        public String password;

        public LoginDetails(String username, int port, String password)
        {
            this.username = username;
            this.port = port;
            this.password = password;
        }

        public boolean hasPassword()
        {
            return this.password != null && !this.password.isEmpty();
        }

        public boolean hasPortMapping()
        {
            return (this.port != LoginDetails.nullPortMapping);
        }
    }

    protected static List<LoginDetails> logins = new ArrayList<LoginDetails>();

    /** Get a copy of the list of available user names
     * @return a copy of the username list
     */
    public static List<LoginDetails> getCopyOfLoginDetails()
    {
        List<LoginDetails> logins = new ArrayList<LoginDetails>();
        for (LoginDetails ld : AuthenticationHelper.logins)
        {
            logins.add(new LoginDetails(ld.username, ld.port, ld.password));
        }
        return logins;
    }

    public static void setLoginDetails(List<LoginDetails> logins)
    {
        // Take a copy:
        AuthenticationHelper.logins.clear();
        for (LoginDetails ld : logins)
        {
            AuthenticationHelper.logins.add(new LoginDetails(ld.username, ld.port, ld.password));
        }
    }

    public static void update(Configuration config)
    {
        // There is a chance we authenticated over the commandline / through the launcher.
        // Forge sets token to FML by default if *not* authenticating, so check for that now:
        if (Minecraft.getMinecraft().getSession().getToken().equals("FML"))
            AuthenticationHelper.isAuthenticated = false;

        String[] validUsernames = config.getStringList(PROP_USERNAMES, MalmoMod.AUTHENTICATION_CONFIGS, new String[0], I18n.format("auth."+PROP_USERNAMES, new Object[0]));
        String[] portToUserMappings = config.getStringList(PROP_PORT_TO_USER_MAPPINGS, MalmoMod.AUTHENTICATION_CONFIGS, new String[0], I18n.format("auth."+PROP_PORT_TO_USER_MAPPINGS, new Object[0]));
        String[] usernameToPasswordMappings = config.getStringList(PROP_USER_TO_PASSWORD_MAPPINGS, MalmoMod.AUTHENTICATION_CONFIGS, new String[0], I18n.format("auth."+PROP_USER_TO_PASSWORD_MAPPINGS,  new Object[0]));
        // Decrypt:
        for (int i = 0; i < usernameToPasswordMappings.length; i++)
            usernameToPasswordMappings[i] = decode(usernameToPasswordMappings[i]);

        // Store in a sensible format:
        AuthenticationHelper.logins.clear();
        for (int i = 0; i < validUsernames.length; i++)
        {
            String user = validUsernames[i];
            AuthenticationHelper.logins.add(new LoginDetails(user, getPortForUser(user, portToUserMappings), getPasswordForUsername(user, usernameToPasswordMappings)));
        }

        String username = getUserForPort(AddressHelper.getMissionControlPort(), portToUserMappings);
        String password = getPasswordForUsername(username, usernameToPasswordMappings);
        // If we can't find login details for this port, don't try to log in.
        // This means the user can login via the commandline/launcher, without us overriding that.
        if (!username.equals(UNAUTH))
            setUsernameAndPassword(username, password);
    }

    public static void setUsernameAndPassword(String username, String password)
    {
        String lastusername = AuthenticationHelper.username;
        String lastpassword = AuthenticationHelper.password;
        AuthenticationHelper.username = username;
        AuthenticationHelper.password = password;
        if (!lastusername.equals(AuthenticationHelper.username) || !lastpassword.equals(AuthenticationHelper.password))
        {
            AuthenticationHelper.isAuthenticated = switchUser();
            if (!AuthenticationHelper.isAuthenticated)
                System.out.println("ERROR: Could not authenticate user " + AuthenticationHelper.username + "; multi-agent missions will not be possible.");
        }
    }

    public static void save()
    {
        // Write our LoginDetails back out as seperate string lists:
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<String> portToUserMappings = new ArrayList<String>();
        ArrayList<String> usernameToPasswordMappings = new ArrayList<String>();
        for (LoginDetails ld : AuthenticationHelper.logins)
        {
            names.add(ld.username);
            if (ld.hasPassword())
                usernameToPasswordMappings.add(AuthenticationHelper.encode(ld.username + ":" + ld.password));
            if (ld.hasPortMapping())
                portToUserMappings.add(String.valueOf(ld.port) + ":" + ld.username);
        }

        Configuration config = MalmoMod.instance.getModPermanentConfigFile();
        Property propUsernames = config.get(MalmoMod.AUTHENTICATION_CONFIGS, PROP_USERNAMES, new String[0], I18n.format("auth."+PROP_USERNAMES, new Object[0]));
        Property propPortToUserMappings = config.get(MalmoMod.AUTHENTICATION_CONFIGS, PROP_PORT_TO_USER_MAPPINGS, new String[0], I18n.format("auth."+PROP_PORT_TO_USER_MAPPINGS, new Object[0]));
        Property propUsernameToPasswordMappings = config.get(MalmoMod.AUTHENTICATION_CONFIGS, PROP_USER_TO_PASSWORD_MAPPINGS, new String[0], I18n.format("auth."+PROP_USER_TO_PASSWORD_MAPPINGS,  new Object[0]));

        propUsernames.set(names.toArray(new String[0]));
        propPortToUserMappings.set(portToUserMappings.toArray(new String[0]));
        propUsernameToPasswordMappings.set(usernameToPasswordMappings.toArray(new String[0]));
        config.save();
    }

    public static String getUserForPort(int port, String[] portToUserMappings)
    {
        String portstring = String.valueOf(port) + ":";
        for (int i = 0; i < portToUserMappings.length; i++)
        {
            if (portToUserMappings[i].startsWith(portstring))
                return portToUserMappings[i].substring(portstring.length());
        }
        return UNAUTH;
    }

    /** Get the port number mapped to this user name
     * @param user
     * @return nullPortMapping if no mapping was found
     */
    public static int getPortForUser(String user, String[] portToUserMappings)
    {
        for (int i = 0; i < portToUserMappings.length; i++)
        {
            String s = portToUserMappings[i];
            if (s.endsWith(user))
            {
                String strport = s.substring(0, s.length() - (user.length() + 1));	// +1 for the colon - portnumber:username
                Integer port = null;
                try
                {
                    port = Integer.valueOf(strport);
                    return port;
                }
                catch (Exception e)
                {
                }
            }
        }
        return LoginDetails.nullPortMapping;
    }

    public static String getPasswordForUsername(String username, String[] usernameToPasswordMappings)
    {
        for (int i = 0; i < usernameToPasswordMappings.length; i++)
        {
            if (usernameToPasswordMappings[i].startsWith(username+":"))
                return usernameToPasswordMappings[i].substring(username.length() + 1);
        }
        return "";
    }

    public static String encode(String source)
    {
        // To avoid having passwords in plain text, add encryption here.
        return source;
    }

    public static String decode(String dest)
    {
        // To avoid having passwords in plain text, add decryption here.
        return dest;
    }

    private static YggdrasilUserAuthentication getAuthenticator()
    {
        // Create an authentication service using Minecraft's session service:
        MinecraftSessionService service = Minecraft.getMinecraft().getSessionService();
        if (!(service instanceof YggdrasilMinecraftSessionService))
            return null;

        UserAuthentication uauth = ((YggdrasilMinecraftSessionService)service).getAuthenticationService().createUserAuthentication(Agent.MINECRAFT);
        if (!(uauth instanceof YggdrasilUserAuthentication))
            return null;

        return (YggdrasilUserAuthentication)uauth;
    }

    private static boolean switchUser()
    {
        YggdrasilUserAuthentication auth = getAuthenticator();
        if (auth == null)
            return false;

        // Now use it to logout and login with the current user:
        auth.logOut();
        auth.setUsername(AuthenticationHelper.username);
        auth.setPassword(AuthenticationHelper.password);
        try
        {
            if (!AuthenticationHelper.username.equals(UNAUTH) && !AuthenticationHelper.password.isEmpty())
            {
                auth.logIn();
                if (!forceSessionUpdate(auth))
                    return false;
            }
        }
        catch (AuthenticationException e)
        {
            return false;
        }
        return true;
    }

    private static boolean forceSessionUpdate(YggdrasilUserAuthentication auth)
    {
        // Create new session object:
        Session newSession = new Session(auth.getSelectedProfile().getName(), auth.getSelectedProfile().getId().toString(), auth.getAuthenticatedToken(), auth.getUserType().getName());
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
