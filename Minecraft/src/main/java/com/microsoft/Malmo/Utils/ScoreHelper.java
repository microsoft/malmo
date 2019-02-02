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

import net.minecraftforge.common.config.Configuration;
import com.microsoft.Malmo.MalmoMod;

import java.io.File;
import java.util.List;
import java.util.Date;

import java.util.logging.*;
import java.util.logging.Level;

import org.apache.commons.lang3.time.DateFormatUtils;
import javax.xml.bind.JAXBException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.microsoft.Malmo.Schemas.AgentSection;
import com.microsoft.Malmo.Schemas.Mission;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.Reward;

/** Class that helps to centralise optional logging of mission rewards.<br>
 */
public class ScoreHelper
{
    private final static int DEFAULT_NO_SCORING = 0;
    private final static int LOG_EACH_REWARD = 1;
    private final static int TOTAL_ALL_REWARDS = 2;
    private final static int TOTAL_WITH_MISSION_XML = 3;

    private static int scoringPolicy = DEFAULT_NO_SCORING;

    private static Logger logger = Logger.getLogger("com.microsoft.Malmo.Scoring");
    private static Handler handler = null;
    private static boolean logging = false;
    private static Level loggingLevel;

    private static Double totalRewards = 0.0; // Totalled rewards over all dimensions if policy is 2.

    /** Initialize scoing. */
    static public void update(Configuration configs)
    {
        scoringPolicy = configs.get(MalmoMod.SCORING_CONFIGS, "policy", DEFAULT_NO_SCORING).getInt();
        if (scoringPolicy > 0) {
            String customLogHandler = configs.get(MalmoMod.SCORING_CONFIGS, "handler", "").getString();
            setLogging(Level.INFO, customLogHandler);
        }
        if (logging)
            log("<ScoreInit><Policy>" + scoringPolicy + "</Policy></ScoreInit>");
    }

    public static boolean isScoring() { return scoringPolicy > 0; }

    /** Record a secure hash of the mission init XML - if scoring. */
    public static void logMissionInit(MissionInit missionInit) throws JAXBException {
        if (!logging || !isScoring())
            return;

        totalRewards = 0.0;

        String missionXml = SchemaHelper.serialiseObject(missionInit.getMission(), Mission.class);
        String hash;
        try {
            hash = digest(missionXml);
        }  catch (NoSuchAlgorithmException e) {
            hash = "";
        }
        List<AgentSection> agents = missionInit.getMission().getAgentSection();
        String agentName = agents.get(missionInit.getClientRole()).getName();

        StringBuffer message = new StringBuffer("<MissionInit><MissionDigest>");
        message.append(hash);
        message.append("</MissionDigest>");
        message.append("<AgentName>");
        message.append(agentName);
        message.append("</AgentName></MissionInit>");
        if (scoringPolicy == TOTAL_WITH_MISSION_XML)
            message.append(missionXml);
        log(message.toString());
    }

    /** Log a reward. */
    public static void logReward(String reward) {
        if (!logging || !isScoring())
            return;
        if (scoringPolicy == LOG_EACH_REWARD) {
            log("<Reward>" + reward + "</Reward>");
        } else if (scoringPolicy == TOTAL_ALL_REWARDS || scoringPolicy == TOTAL_WITH_MISSION_XML) {
            int i = reward.indexOf(":");
            totalRewards += Double.parseDouble(reward.substring(i + 1));
        }
    }

    /** Log mission end rewards. */
    public static void logMissionEndRewards(Reward reward) throws JAXBException {
        if (!logging || !isScoring())
            return;
        if (scoringPolicy == LOG_EACH_REWARD) {
            String rewardString = SchemaHelper.serialiseObject(reward, Reward.class);
            log("<MissionEnd>" + rewardString + "</MissionEnd>");
        } else if (scoringPolicy == TOTAL_ALL_REWARDS || scoringPolicy == TOTAL_WITH_MISSION_XML) {
            List<Reward.Value> values = reward.getValue();
            for(Reward.Value v : values) {
                totalRewards += v.getValue().doubleValue();
            }

            log("<MissionTotal>" + totalRewards + "</MissionTotal>");
        }
        totalRewards = 0.0;
    }

    /** Write to scoring log. */
    private static void log(String message) {
        logger.log(Level.INFO, message);
    }

    private static void setLogging(Level level, String customLogHandler)
    {
        logging = true;
        if (handler == null)
        {
            if (customLogHandler != null && customLogHandler != "") {
                // Custom handler.
                System.out.println("Custom score handler " + customLogHandler);
                try {
                    Class handlerClass = Class.forName(customLogHandler);
                    handler = (Handler) handlerClass.newInstance();
                } catch (Exception e) {
                    System.out.println("Failed to create custom score log " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                try {
                    Date d = new Date();
                    String filename = "Score" + DateFormatUtils.format(d, "yyyy-MM-dd HH-mm-ss") + ".log";
                    handler = new FileHandler("logs" + File.separator + filename);
                } catch (Exception e) {
                    System.out.println("Failed to create file score log " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (handler != null) {
                logger.setUseParentHandlers(false); // Don't flood the parent log.
                logger.addHandler(handler);
            }
        }
        logger.setLevel(level);
        loggingLevel = level;
    }

    private static String digest(String message) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(message.getBytes());

        byte byteData[] = md.digest();

        // Convert the bytes to string in hex format.
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }
}