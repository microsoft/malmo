package com.microsoft.Malmo.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.minecraftforge.common.config.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.microsoft.Malmo.MalmoMod;

public class TCPUtils
{
    public static final int DEFAULT_SOCKET_TIMEOUT_MS = 1000;
    private static Logger logger = Logger.getLogger("com.microsoft.Malmo.TCPUtils");
    private static FileHandler filehandler = null;
    private static boolean logging = false;
    private static int currentIndentation = 0;

    public static void initTCPUtils()
    {
        System.setProperty("java.util.logging.SimpleFormatter.format","%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tL %4$-7s %5$s%n");
    }

    public static void setLogging(boolean log)
    {
        logging = log;
        if (log == true && filehandler == null)
        {
            try
            {
                Date d = new Date();
                String filename = "TCP" + DateFormatUtils.format(d, "yyyy-MM-dd HH-mm-ss") + ".log";
                filehandler = new FileHandler("logs" + File.separator + filename);
                filehandler.setFormatter(new SimpleFormatter());
            }
            catch (SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            logger.setUseParentHandlers(false); // Don't flood the parent log.
            logger.setLevel(Level.FINE);
            logger.addHandler(filehandler);
        }
    }

    public static boolean isLogging() { return logging; }

    public static void Log(Level level, String message)
    {
        if (logging)
            logger.log(level, getIndented(message));
    }

    public static void SysLog(Level level, String message)
    {
        if (logging)
            logger.log(level, getIndented(message));
        System.out.println(level + ": " + message);
    }

    private static String getIndented(String message)
    {
        return (currentIndentation == 0) ? message : StringUtils.repeat("    ", currentIndentation) + message;
    }

    public static void update(Configuration config)
    {
        setLogging(config.getBoolean("generateSocketLogs", MalmoMod.DIAGNOSTIC_CONFIGS, true, "Log all socket activity to aid troubleshooting."));
    }

    private static void indent()
    {
        currentIndentation++;
    }

    private static void unindent()
    {
        currentIndentation--;
    }

    public static class LogSection
    {
        public LogSection(String header)
        {
            TCPUtils.Log(Level.INFO, header);
            TCPUtils.Log(Level.INFO, "{");
            indent();
        }

        public void close()
        {
            unindent();
            TCPUtils.Log(Level.INFO, "}");
        }
    }
}
