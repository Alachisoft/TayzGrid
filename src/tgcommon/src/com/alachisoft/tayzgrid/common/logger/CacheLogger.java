/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alachisoft.tayzgrid.common.logger;

import com.alachisoft.tayzgrid.common.DirectoryUtil;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import tangible.RefObject;

public class CacheLogger implements ILogger
{


    public Level criticalInfo = new CustomLevel(5000000, "CRIT", 0);
    public Level devInfo = new CustomLevel(5000001, "DEV", 0);
    private Logger log;
    /**
     * Configuration file folder name
     */
    private static final String DIRNAME = ServicePropValues.LOGS_FOLDER;
    /**
     * Path of the configuration folder.
     */
    private static String s_configDir = "";
    private String _path = "";
    private static int bufferDefaultSize = 1;
    private String _loggerName = null;
    private final String NEW_LINE = "\r\n";

    /**
     * Scans the registry and locates the configuration file.
     */
    static
    {
        s_configDir = System.getProperty("user.dir");

        try
        {
        }
        catch (java.lang.Exception e)
        {
        }

    }



    @Override
    public String Initialize(java.util.Map properties, String partitionID, String cacheName) throws Exception
    {
        return Initialize(properties, partitionID, cacheName, false, false);
    }

    @Override
    public final String Initialize(java.util.Map properties, String partitionID, String cacheName, boolean isStartedAsMirror, boolean inproc) throws Exception
    {

        if (log != null)
        {
            throw new Exception("Multiple Initialize calls for same logger");
        }



        if (_loggerName != null)
        {
            return _loggerName;
        }
        try
        {
            _loggerName = cacheName;
            if (partitionID != null && partitionID.length() > 0)
            {
                _loggerName += "-" + partitionID;
            }

            if (isStartedAsMirror)
            {
                _loggerName += "-" + "replica";
            }

            if (inproc && !isStartedAsMirror)
            {
                _loggerName += "." + ManagementFactory.getRuntimeMXBean().getName();
            }
            //filename = System.IO.Path.Combine(path, filename);

            String LogExceptions = "";

            if (_loggerName.equals("LogExceptions"))
            {
                LogExceptions = "\\LogExceptions";
            }

            String fileName = GetLogPath() + LogExceptions + "\\" + _loggerName + "_" /*+ Calendar.getInstance().toString()*/ + ".txt";


            AddAppender(CreateRollingFileAppender(fileName));


            if (properties != null)
            {
                if (properties.containsKey("trace-errors"))
                {
                    if ((Boolean) (properties.get("trace-errors")))
                    {
                        SetLevel("ERROR");
                    }
                }

                if (properties.containsKey("trace-notices"))
                {
                    if ((Boolean) (properties.get("trace-notices")))
                    {
                        SetLevel("INFO");
                    }
                }

                if (properties.containsKey("trace-warnings"))
                {
                    if ((Boolean) (properties.get("trace-warnings")))
                    {
                        SetLevel("WARN");
                    }
                }

                if (properties.containsKey("trace-debug"))
                {
                    if ((Boolean) (properties.get("trace-debug")))
                    {
                        SetLevel("ALL");
                    }
                }

                if (properties.containsKey("enabled"))
                {
                    if (!(Boolean) (properties.get("trace-errors")))
                    {
                        SetLevel("OFF");
                    }
                }
            }
            else
            {
                SetLevel("WARN");
            }

        }
        catch (Exception e)
        {
            EventLogger.LogEvent("TayzGrid", "Failed to open log. " + e, EventType.ERROR, EventCategories.Error, EventID.GeneralError);
        }

        return _loggerName;

    }

    @Override
    public final void Initialize(LoggerNames loggerName) throws Exception
    {
        if (loggerName != LoggerNames.Licence)
        {
            Initialize(loggerName, null);
        }
    }

    @Override
    public final void Initialize(LoggerNames loggerNameEnum, String cacheName) throws Exception
    {
        if (log != null)
        {
            throw new Exception("Multiple Initialize calls for same logger");
        }

        _loggerName = loggerNameEnum.toString();

        String filename = _loggerName.toLowerCase();
        if (loggerNameEnum == LoggerNames.ClientLogs)
        {

            filename = filename + "." + cacheName + "." + ManagementFactory.getRuntimeMXBean().getName();

            // changing the name here will invalidate static log checks automatically since LoggerName == ClientLogs
            _loggerName = cacheName + ManagementFactory.getRuntimeMXBean().getName();
        }
        else
        {
            filename = cacheName;
        }

        if (loggerNameEnum != LoggerNames.ClientLogs)
        {
            _loggerName = _loggerName + new java.util.Date();
        }

        java.net.InetAddress localMachine = null;
        try
        {
            localMachine = java.net.InetAddress.getLocalHost();
        }
        catch (UnknownHostException unknownHostException)
        {
            throw new Exception(unknownHostException.toString());
        }

        filename = filename + "." + localMachine.getHostName().toLowerCase() + "." + String.format("%d", new java.util.Date()) + ".logs.txt";

        String filepath = "";

        tangible.RefObject<String> tempRef_filepath = new tangible.RefObject<String>(filepath);
        boolean tempVar = !DirectoryUtil.SearchGlobalDirectory(ServicePropValues.LOGS_FOLDER, false, tempRef_filepath);
        filepath = tempRef_filepath.argvalue;
        if (tempVar)
        {
            try
            {
                tangible.RefObject<String> tempRef_filepath2 = new tangible.RefObject<String>(filepath);
                DirectoryUtil.SearchLocalDirectory(ServicePropValues.LOGS_FOLDER, true, tempRef_filepath2);
                filepath = tempRef_filepath2.argvalue;
            }
            catch (Exception ex)
            {
                throw new Exception("Unable to initialize the log file", ex);
            }
        }

        try
        {

            StringBuilder sb = new StringBuilder(filepath);
            filepath = sb.append(loggerNameEnum.toString()).toString();
            if (!(new java.io.File(filepath)).isDirectory())
            {
                (new java.io.File(filepath)).mkdir();
            }


            filepath = sb.append(filename).toString();
            AddAppender(CreateRollingFileAppender(filepath));
        }
        catch (Exception e)
        {
            throw e;
        }

    }

    /**
     * Assigns the new level to the logger
     *
     * @param levelName ALL, DEBUG, INFO, ERROR, FATAL
     */
    @Override
    public final void SetLevel(String levelName)
    {
        log.setLevel(Level.toLevel(levelName));
    }

    /**
     * Use {@link #Error(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Error(String message)
    {
        log.error(message);
    }

    /**
     * Use {@link #Fatal(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Fatal(String message)
    {
        log.fatal(message);
    }

    /**
     * Use {@link #CriticalInfo(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void CriticalInfo(String message)
    {
        log.log(null, criticalInfo, message, null);
    }

    /**
     * Use {@link #Info(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Info(String message)
    {
        log.info(message);
    }

    /**
     * Use {@link #DevTrace(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Debug(String message)
    {
        log.debug(message);
    }

    /**
     * Use {@link #Debug(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Warn(String message)
    {
        log.warn(message);
    }

    @Override
    public final boolean getIsInfoEnabled()
    {
        return log.isInfoEnabled();
    }

    @Override
    public final boolean getIsErrorEnabled()
    {
        return log.isEnabledFor(Level.toLevel("ERROR"));
    }

    @Override
    public final boolean getIsWarnEnabled()
    {
        return log.isEnabledFor(Level.toLevel("WARNING"));
    }

    @Override
    public final boolean getIsDebugEnabled()
    {
        return log.isDebugEnabled();
    }

    @Override
    public final boolean getIsFatalEnabled()
    {
        return log.isEnabledFor(Level.toLevel("FATAL"));
    }


    public final String GetLogPath()
    {
        if (_path.length() < 1)
        {
            _path = s_configDir;
        }

        try
        {
            if (!(new java.io.File(_path)).isDirectory())
            {
                (new java.io.File(_path)).mkdir();
            }
        }
        catch (Exception e)
        {
            EventLogger.LogEvent("TayzGrid", e.toString(), EventType.ERROR, EventCategories.Error, EventID.GeneralError);
        }
        return _path;
    }

    /**
     * Add a desired appender to the logger
     *
     * @param loggerName Name of the logger to which the appender is to be added
     * @param appender Appender to add to the logger
     */
    private void AddAppender(Appender appender)
    {

        log = Logger.getLogger(_loggerName);

        log.addAppender(appender);
    }

    private void RemoveAllAppender()
    {
    }

    /**
     * Creates Buffer Appender, Responsible for storing all logging events in memory buffer and writing them down when by passing the logging events on to the file appender it
     * holds when its buffer becomes full Can also be made a lossy logger which writes only writes down to a file when a specific crieteria/condition is met
     * @param cacheName CacheName used to name the Buffer Appender
     * @param fileName File name to log into
     * @return Returns the created Appender
     */
    private Appender CreateBufferAppender(String fileName)
    {
        AsyncAppender appender = new AsyncAppender();
        appender.setName("BufferingForwardingAppender" + _loggerName);
        //Pick from config
        int bufferSize = bufferDefaultSize;
        RefObject<Integer> tempRef_bufferSize = new RefObject<Integer>(bufferSize);
        ReadConfig(tempRef_bufferSize);
        bufferSize = tempRef_bufferSize.argvalue;
        tangible.RefObject<Integer> tempRef_bufferSize2 = new tangible.RefObject<Integer>(bufferSize);
        if (bufferSize == bufferDefaultSize)
        {
            ReadClientConfig(tempRef_bufferSize2);
        }
        bufferSize = tempRef_bufferSize2.argvalue;

        if (bufferSize < 1)
        {
            bufferSize = bufferDefaultSize;
        }

        appender.setBufferSize(bufferSize);

        //Threshold is maintained by the logger rather than the appenders
        appender.setThreshold(Level.ALL);

        //Adds the appender to which it will pass on all the logging levels upon filling up the buffer
        appender.addAppender(CreateRollingFileAppender(fileName));

        //necessary to apply the appender property changes
        appender.activateOptions();

        return appender;
    }

    /**
     * Create File appender, This appender is responsible to write stream of data when invoked, in our case, this appender is handeled my the Bufferappender
     *
     * @param cacheName Name of the file appender
     * @param fileName Filename to which is to write logs
     * @return returns the created appender
     */
    private Appender CreateRollingFileAppender(String fileName)
    {
        RollingFileAppender appender = new RollingFileAppender();
        appender.setName("RollingFileAppender" + _loggerName);
        appender.setFile(fileName);
        //doesnt matter since all files are created with a new name
        appender.setAppend(false);

        appender.setMaxBackupIndex(Integer.MAX_VALUE);
        appender.setMaxFileSize("5MB");

        String str = "%-27d{ISO8601}" /*+ "\t%-45.42appdomain" + "\t%-45.42l"*/ + "\t%-42t" + "\t%-9p" + "\t%m" + "%n";

        appender.setThreshold(Level.ALL);

        return appender;
    }

    public final boolean[] ReadConfig(tangible.RefObject<Integer> bufferAppender)
    {
        try
        {

            String EnableLogs = "False";
            String EnableDetailedLogs = "False";
            String BufferSize = "False";


            try
            {
                if (BufferSize != null)
                {
                    bufferAppender.argvalue = Integer.parseInt(BufferSize);
                }
                else
                {
                    bufferAppender.argvalue = bufferDefaultSize;
                }
            }
            catch (Exception e)
            {
                bufferAppender.argvalue = bufferDefaultSize;
            }

            if (EnableDetailedLogs == null && EnableLogs == null)
            {
                return new boolean[]
                        {
                            false, false
                        };
            }
            else if (EnableDetailedLogs != null && EnableLogs == null)
            {
                return new boolean[]
                        {
                            false, Boolean.parseBoolean(EnableDetailedLogs)
                        };
            }
            else if (EnableDetailedLogs == null && EnableLogs != null)
            {
                return new boolean[]
                        {
                            Boolean.parseBoolean(EnableLogs), false
                        };
            }
            else
            {
                return new boolean[]
                        {
                            Boolean.parseBoolean(EnableLogs), Boolean.parseBoolean(EnableDetailedLogs)
                        };
            }
        }
        catch (Exception ex)
        {
            bufferAppender.argvalue = bufferDefaultSize;
            return new boolean[]
                    {
                        false, false
                    };
        }

    }

    public final boolean[] ReadClientConfig(tangible.RefObject<Integer> bufferAppender)
    {
        try
        {
            String EnableLogs = "False";
            String EnableDetailedLogs = "False";
            String BufferSize = "False";

            try
            {
                if (BufferSize != null)
                {
                    bufferAppender.argvalue = Integer.parseInt(BufferSize);
                }
                else
                {
                    bufferAppender.argvalue = bufferDefaultSize;
                }
            }
            catch (Exception e)
            {
                bufferAppender.argvalue = bufferDefaultSize;
            }

            if (EnableDetailedLogs == null && EnableLogs == null)
            {
                return new boolean[]
                        {
                            false, false
                        };
            }
            else if (EnableDetailedLogs != null && EnableLogs == null)
            {
                return new boolean[]
                        {
                            false, Boolean.parseBoolean(EnableDetailedLogs)
                        };
            }
            else if (EnableDetailedLogs == null && EnableLogs != null)
            {
                return new boolean[]
                        {
                            Boolean.parseBoolean(EnableLogs), false
                        };
            }
            else
            {
                return new boolean[]
                        {
                            Boolean.parseBoolean(EnableLogs), Boolean.parseBoolean(EnableDetailedLogs)
                        };
            }
        }
        catch (Exception ex)
        {
            bufferAppender.argvalue = bufferDefaultSize;
            return new boolean[]
                    {
                        false, false
                    };
        }

    }

    //<editor-fold defaultstate="collapsed" desc="ILogger Members">
    /**
     * <b>No need to call this flush</b>, its automatic there is no force flush available for AsynAppenders, this is by implementation and only close is available which stops and
     * interrupts all the threads and perfmos their operations forcefully
     */
    @Override
    public final void Flush()
    {
        //<editor-fold defaultstate="collapsed" desc="A comment related to some bugID">
        //bug id- 1431 Problem was occuring because the Appender enumeration was modifying during iterations
        //IAppender[] logAppenders = log4net.LogManager.GetRepository().GetAppenders();

        //foreach (log4net.Appender.IAppender appender in logAppenders)
        //{
        //    if (appender != null)
        //    {
        //        BufferingAppenderSkeleton buffered = appender as BufferingAppenderSkeleton;
        //        if (buffered is BufferingForwardingAppender)
        //        {
        //            ((BufferingForwardingAppender)buffered).Flush();
        //        }
        //    }
        //}
        //</editor-fold>
        //: Logger
//        Enumeration enumer = log.getAllAppenders();
//        if (enumer != null)
//        {
//            while (enumer.hasMoreElements())
//            {
//                Appender app = (Appender) enumer.nextElement();
//                if (app instanceof AsyncAppender)
//                {
//                    //: there is no force flush available for AsynAppenders, this is by implementation and only close is available which stops and interrupts all the threads and perfmos their operations forcefully
////                    AsyncAppender async = (AsyncAppender)app;
////                    async.close();
//                }
//            }
//        }
    }

    @Override
    /**
     * Closes the appenders and removes them
     */
    public final void Close()
    {
        SetLevel("OFF");

        Enumeration enumer = log.getAllAppenders();
        if (enumer != null)
        {
            while (enumer.hasMoreElements())
            {
                Appender app = (Appender) enumer.nextElement();
                if (app instanceof AsyncAppender)
                {
                    //: there is no force flush available for AsynAppenders, this is by implementation and only close is available which stops and interrupts all the threads and perfmos their operations forcefully
                    AsyncAppender async = (AsyncAppender) app;
                    async.close();
                }
            }
        }
        this.RemoveAllAppender();
    }

    /**
     * Error logs
     *
     * @param module Module Name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void Error(String module, String message)
    {
        int space2 = 50;

        if (module.length() == 0)
        {
            space2 = 4;
        }
        else
        {
            space2 = 50 - module.length();
            if(space2 <= 0)
                space2  = 40;
        }

        Error(module + String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Fatal logs
     *
     * @param module Module Name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void Fatal(String module, String message)
    {
        int space2 = 50;

        if (module.length() == 0)
        {
            space2 = 4;
        }
        else
        {
            space2 = 50 - module.length();
            if(space2 <= 0)
                space2  = 40;
        }

        Fatal(String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Critical Information, Level higher than Fatal
     *
     * @param module Module name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void CriticalInfo(String module, String message)
    {
        int space2 = 50;

        if (module.length() == 0)
        {
            space2 = 4;
        }
        else
        {
            space2 = 50 - module.length();
            if(space2 <= 0)
                space2  = 40;
        }


        CriticalInfo(module + String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Information level debugging
     *
     * @param module Module Name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void Info(String module, String message)
    {
        int space2 = 50;

        if (module.length() == 0)
        {
            space2 = 4;
        }
        else
        {
            space2 = 50 - module.length();
            if(space2 <= 0)
                space2  = 40;
        }

        Info(module + String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Debug level information
     *
     * @param module Module Name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void Debug(String module, String message)
    {
        int space2 = 50;

        if (module.length() == 0)
        {
            space2 = 4;
        }
        else
        {
            space2 = 50 - module.length();
            if(space2 <= 0)
                space2  = 40;
        }

        Debug(String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Warning messages
     *
     * @param module Module name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void Warn(String module, String message)
    {
        int space2 = 50;

        if (module.length() == 0)
        {
            space2 = 4;
        }
        else
        {
            space2 = 50 - module.length();
            if(space2 <= 0)
                space2  = 40;
        }

        Warn(String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Used only for Dev tracing <b>To be removed in production</b>
     *
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void DevTrace(String message)
    {
        log.log(null, devInfo, message, null);
    }

    /**
     * Used only for Dev tracing <b>To be removed in production</b>
     *
     * @param module Module name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void DevTrace(String module, String message)
    {
        int space2 = 50;

        if (module.length() == 0)
        {
            space2 = 4;
        }
        else
        {
            space2 = 50 - module.length();
            if(space2 <= 0)
                space2  = 40;
        }

        DevTrace(String.format("%" + space2 + "s", " ") + message);
    }
    //</editor-fold>
}
