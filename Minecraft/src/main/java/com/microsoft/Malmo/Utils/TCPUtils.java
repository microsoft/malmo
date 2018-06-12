package com.microsoft.Malmo.Utils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.microsoft.Malmo.MalmoMod;

import net.minecraftforge.common.config.Configuration;

public class TCPUtils
{
    public enum SeverityLevel
    {
        LOG_NONE("Logging off", Level.OFF),             // Default - log nothing
        LOG_SEVERE("Log errors only", Level.SEVERE),    // Only log errors
        LOG_WARNINGS("Log warnings", Level.WARNING),    // Log warnings and above
        LOG_INFO("Log basic info", Level.INFO),         // Also log basic info
        LOG_DETAILED("Detailed logging", Level.FINE),   // Log detailed info
        LOG_ALL("Log everything", Level.ALL);

        private final String displayName;
        private final Level level;
        SeverityLevel(String displayName, Level level) { this.displayName = displayName; this.level = level; }
        public final String getDisplayName() { return this.displayName; }
        public final Level getLevel() { return this.level; }
    }

    static class UTCFormatter extends Formatter
    {
    	private static final String dateformat = "yyyy-MMM-dd HH:mm:ss.S";
        private static final FastDateFormat DATE_FORMATTER = FastDateFormat.getInstance(dateformat, TimeZone.getTimeZone("UTC"));
        private static final String padding = dateformat + "00000"; // to pad the milliseconds up to 6 spaces - see below.

        @Override
        public String format(LogRecord record)
        {
            StringBuilder builder = new StringBuilder(1000);
            String timestamp = DATE_FORMATTER.format(new Date(record.getMillis()));
            // "000" padding is to match the greater precision available from the C++ side, if combining logs.
            timestamp += padding.substring(timestamp.length());
            builder.append(timestamp).append(" M ")   // 'M' for 'Mod' - useful if combining logs with platform-side.
                .append(String.format("%1$-7s", record.getLevel()))
                .append(formatMessage(record))
                .append("\n");
            return builder.toString();
        }

        public String getHead(Handler h)
        {
            return super.getHead(h);
        }

        public String getTail(Handler h)
        {
            return super.getTail(h);
        }
    }

    public static final int DEFAULT_SOCKET_TIMEOUT_MS = 30000;

    private static Logger logger = Logger.getLogger("com.microsoft.Malmo.TCPUtils");
    private static FileHandler filehandler = null;
    private static boolean logging = false;
    private static int currentIndentation = 0;
    private static SeverityLevel loggingSeverityLevel = SeverityLevel.LOG_NONE;

    public static void setLogging(SeverityLevel slevel)
    {
        logging = slevel != SeverityLevel.LOG_NONE;
        if (logging == true && filehandler == null)
        {
            try
            {
                Date d = new Date();
                String filename = "TCP" + DateFormatUtils.format(d, "yyyy-MM-dd HH-mm-ss") + ".log";
                filehandler = new FileHandler("logs" + File.separator + filename);
                filehandler.setFormatter(new UTCFormatter());
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
            logger.addHandler(filehandler);
        }
        logger.setLevel(slevel.getLevel());
        loggingSeverityLevel = slevel;
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
        String[] values = new String[SeverityLevel.values().length];
        for (SeverityLevel level : SeverityLevel.values())
            values[level.ordinal()] = level.getDisplayName();
        String severityLevel = config.getString("loggingSeverityLevel", MalmoMod.DIAGNOSTIC_CONFIGS, TCPUtils.loggingSeverityLevel.getDisplayName(), "Set the level of socket debugging information to be logged.", values);
        for (SeverityLevel level : SeverityLevel.values())
            if (level.getDisplayName().equals(severityLevel))
                setLogging(level);
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

    /**
     * Choose a port from the specified range - either sequentially, or at random.
     *
     * @param minPort     minimum (inclusive) value for port.
     * @param maxPort     max (inclusive) possible port value.
     * @param random      true to allocate based on a random sample; false to allocate sequentially, starting from minPort.
     * @return a ServerSocket.
     */
    public static ServerSocket getSocketInRange(int minPort, int maxPort, boolean random)
    {
        TCPUtils.Log(Level.INFO, "Attempting to create a ServerSocket in range (" + minPort + "-" + maxPort + (random ? ") at random..." : ") sequentially..."));
        ServerSocket s = null;
        int port = minPort - 1;
        Random r = new Random(System.currentTimeMillis());
        while (s == null && port <= maxPort)
        {
            if (random)
                port = minPort + r.nextInt(maxPort - minPort);
            else
                port++;
            try
            {
                TCPUtils.Log(Level.INFO, "    - trying " + port + "...");
                s = new ServerSocket(port);
                TCPUtils.Log(Level.INFO, "Succeeded!");
                return s; // Created okay, so this port is available.
            }
            catch (IOException e)
            {
                // Try the next port.
                TCPUtils.Log(Level.INFO, "    - failed: " + e);
            }
        }
        TCPUtils.Log(Level.SEVERE, "Could find no available port!");
        return null; // No port found in the allowed range.
    }
}
