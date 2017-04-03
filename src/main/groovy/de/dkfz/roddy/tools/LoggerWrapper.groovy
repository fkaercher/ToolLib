/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import de.dkfz.roddy.core.InfoObject
import org.apache.commons.io.filefilter.WildcardFileFilter

import java.util.logging.*

/**
 * This class wraps around the Java logger class and implements a verbosity level structure.
 * Created by michael on 28.05.14.
 */
@groovy.transform.CompileStatic
public class LoggerWrapper {
    public static final int VERBOSITY_INFO = 5;
    public static final int VERBOSITY_WARNING = 3;
    public static final int VERBOSITY_SEVERE = 1;
    public static final int VERBOSITY_HIGH = 5;
    public static final int VERBOSITY_MEDIUM = 3;
    public static final int VERBOSITY_LOW = 1;
    public static final int VERBOSITY_RARE = 5;
    public static final int VERBOSITY_SOMETIMES = 3;
    public static final int VERBOSITY_ALWAYS = 1;
    public static final String DEFAULT_APPLICATION_LOG_DIRECTORY = "/tmp/LoggerWrapperOutput"
    public static final String DEFAULT_LOG_FILES_PREFIX = "default"
    public static final boolean DEFAULT_LOG_EXTENSIVELY = true
    public static final int DEFAULT_MAXIMUM_LOGFILES_PER_PREFIX = 32

    private static File logFile
    private static File applicationLogDirectory = new File(DEFAULT_APPLICATION_LOG_DIRECTORY)
    private static String logFilesPrefix = DEFAULT_LOG_FILES_PREFIX
    private static boolean logExtensively = DEFAULT_LOG_EXTENSIVELY
    private static int maximumLogFilesPerPrefix = DEFAULT_MAXIMUM_LOGFILES_PER_PREFIX
    private static int verbosityLevel = VERBOSITY_LOW;

    private static InfoObject applicationStartupTimestamp = new InfoObject();

    private Logger consoleLogger;

    public LoggerWrapper(String name) {
        consoleLogger = Logger.getLogger(name);
    }

    public static LoggerWrapper getLogger(String name) {
        return new LoggerWrapper(name);
    }

    public static LoggerWrapper getLogger(Class cls) {
        return new LoggerWrapper(cls.getName());
    }

    public static void setVerbosityLevel(int level) {
        verbosityLevel = level;
    }

    public static int getVerbosityLevel() {
        return verbosityLevel;
    }

    public static boolean isVerbosityLow() {
        return verbosityLevel >= VERBOSITY_SEVERE;
    }

    public static boolean isVerbosityMedium() {
        return verbosityLevel >= VERBOSITY_WARNING;
    }

    public static boolean isVerbosityHigh() {
        return verbosityLevel >= VERBOSITY_INFO;
    }

    private static int getVerbosityLevelFor(Level lvl) {
        if (lvl == Level.INFO) {
            return VERBOSITY_INFO;
        } else if (lvl == Level.WARNING) {
            return VERBOSITY_WARNING;
        } else if (lvl == Level.SEVERE) {
            return VERBOSITY_SEVERE;
        }
        return 0;
    }

    private static Level getVerbosityLevelObject(int lvl) {
        if (lvl == VERBOSITY_INFO) {
            return Level.INFO;
        } else if (lvl == VERBOSITY_WARNING) {
            return Level.WARNING;
        } else if (lvl == VERBOSITY_SEVERE) {
            return Level.SEVERE;
        }
        return Level.SEVERE;
    }

    /**
     * Erases all the oldest logfiles in the log directory down to a count of 32(by default).
     */
    private static void manageLogFileCount() {
        try {
            File[] files = applicationLogDirectory.listFiles((FilenameFilter) new WildcardFileFilter(logFilesPrefix + "*"))?.sort() as File[];
            if (files && files.length > maximumLogFilesPerPrefix) {
                // Remove old files
                for (int i = 0; i < files.length - maximumLogFilesPerPrefix + 1; i++) {
                    LoggerWrapper.getLogger(LoggerWrapper.class).postSometimesInfo("Deleted old logfile ${files[i]}.")
                    files[i].delete();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static synchronized File getLogFile() {
        if (logFile == null) {
            String timestamp = InfoObject.formatTimestamp(applicationStartupTimestamp.getTimeStamp())
            logFile = new File(applicationLogDirectory, [logFilesPrefix, timestamp].join("_") + ".tsv");
            manageLogFileCount();
        }

        return logFile;
    }

    private synchronized void logToLogFile(Level level, String text, Throwable ex) {
        try {
            if (!logExtensively) return;
            getLogFile() << [this.consoleLogger.getName(), level, text, ex ?: "NoExceptionThrown"].join("\t") << System.getProperty("line.separator");
        } catch (Exception ignored) {
        }
    }

    public void log(int lvl, String text) {
        def level = getVerbosityLevelObject(lvl)
        logToLogFile(level, text, null);
        if (verbosityLevel >= lvl) {
            consoleLogger.log(level, text);
        }
    }

    public void log(Level lvl, String text) {
        logToLogFile(lvl, text, null);
        if (getVerbosityLevelFor(lvl) <= verbosityLevel)
            consoleLogger.log(lvl, text);
    }

    public void log(Level lvl, String text, Throwable ex) {
        logToLogFile(lvl, text, null);
        if (getVerbosityLevelFor(lvl) <= verbosityLevel)
            consoleLogger.log(lvl, text, ex);
    }

    public void severe(String text) {
        log(Level.SEVERE, text);
    }

    public void severe(String text, Exception ex) {
        log(Level.SEVERE, text);
        log(Level.INFO, RoddyIOHelperMethods.getStackTraceAsString(ex));
    }

    public void warning(String text) {
        log(Level.WARNING, text);
    }

    public void info(String text) {
        log(Level.INFO, text);
    }

    public void rare(String text) {
        postRareInfo(text)
    }

    public void postRareInfo(String text) {
        logToLogFile(Level.INFO, text, null);
        if (verbosityLevel >= VERBOSITY_RARE)
            consoleLogger.log(Level.INFO, text);
    }

    public void always(String text) {
        postAlwaysInfo(text)
    }

    public void postAlwaysInfo(String text) {
        logToLogFile(Level.INFO, text, null);
        if (verbosityLevel >= VERBOSITY_ALWAYS)
            consoleLogger.log(Level.INFO, text);
    }

    public void sometimes(String text) {
        postSometimesInfo(text)
    }

    public void postSometimesInfo(String text) {
        logToLogFile(Level.INFO, text, null);
        if (verbosityLevel >= VERBOSITY_SOMETIMES)
            consoleLogger.log(Level.INFO, text);
    }

    /**
     * Set up the applications logger mechanisms
     * This sets i.e. how messages are printed.
     */
    static void setup(AppConfig appConfig) {
        if (!appConfig) {
            setup()
        }

        File ald = new File(appConfig.getProperty("applicationLogDirectory", DEFAULT_APPLICATION_LOG_DIRECTORY))
        String prefix = appConfig.getProperty("logFilesPrefix", DEFAULT_LOG_FILES_PREFIX)
        Boolean logext = RoddyConversionHelperMethods.toBoolean(appConfig.getProperty("logExtensively", "true"), DEFAULT_LOG_EXTENSIVELY)
        int maxFiles = RoddyConversionHelperMethods.toInt(appConfig.getProperty("maximumLogFilesPerPrefix", "32"), DEFAULT_MAXIMUM_LOGFILES_PER_PREFIX)
        setup(ald, prefix, logext, maxFiles)
    }

    /**
     * Set up the applications logger mechanisms
     * This sets i.e. how messages are printed.
     */
    static void setup(File applicationLogDirectory = new File(DEFAULT_APPLICATION_LOG_DIRECTORY)
                      , String logFilesPrefix = DEFAULT_LOG_FILES_PREFIX
                      , boolean logExtensively = DEFAULT_LOG_EXTENSIVELY
                      , int maximumLogFilesPerPrefix = DEFAULT_MAXIMUM_LOGFILES_PER_PREFIX) {

        LoggerWrapper.applicationLogDirectory = applicationLogDirectory
        LoggerWrapper.logFilesPrefix = logFilesPrefix
        LoggerWrapper.logExtensively = logExtensively
        LoggerWrapper.maximumLogFilesPerPrefix = maximumLogFilesPerPrefix

        Logger global = Logger.getLogger("");
        Handler[] handlers = global.getHandlers();
        for (Handler iHandler : handlers) {
            global.removeHandler(iHandler);
        }

        global.setUseParentHandlers(false);
        ConsoleHandler cHandler = new ConsoleHandler();
        cHandler.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord r) {
                if (r.getLoggerName().startsWith("net.schmizz.sshj"))
                    return false;
                return true;
            }
        });
        cHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord r) {
                if (LoggerWrapper.getVerbosityLevel() == 1)
                    return r.getMessage() + System.getProperty("line.separator")
                StringBuilder sb = new StringBuilder();
                sb.append(r.getLevel()).append(" ").append(r.getSourceMethodName()).append(" ").append(r.getLoggerName()).append(":\t").append(formatMessage(r)).append(System.getProperty("line.separator"));
                if (null != r.getThrown()) {
                    sb.append("Throwable occurred: "); //$NON-NLS-1$
                    Throwable t = r.getThrown();
                    PrintWriter pw = null;
                    try {
                        StringWriter sw = new StringWriter();
                        pw = new PrintWriter(sw);
                        t.printStackTrace(pw);
                        sb.append(sw.toString());
                    } finally {
                        if (pw != null) {
                            try {
                                pw.close();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                }
                return sb.toString();
            }
        });
        global.addHandler(cHandler);
    }
}