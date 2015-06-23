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

import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.DirectoryUtil;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.enums.EventType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.util.logging.*;
import tangible.RefObject;

public class JLogger implements ILogger {

    private Logger logger;
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
    private static String _nodeIP = "";
    private boolean isLoggingEnabled = true;
    
    /**
     * File size limit of log files.
     */
    private int _limit = 5000000; // 5 Mb;
    /**
     * Number of max log files that can be written. Currently set to 1000
     * increasing it any further is causing extreme delays in reply as
     * Filehandler is calling new File() doing some IO. In short logging needs
     * to be reviewed particularly configurelogger() .
     */
    private int _numLogFiles = 1000;
    private String _logFileNamePattern = "log";
    private boolean _append = true;

    public int getLimit() {
        return this._limit;
    }

    public void setLimit(int limit) {
        this._limit = limit;
    }

    public int getNumLogFiles() {
        return this._numLogFiles;
    }

    public void setNumLogFiles() {
        this._limit = this._numLogFiles;
    }

    public String getLogFileNamePattern() {
        return this._logFileNamePattern;
    }

    public void setLogFileNamePattern(String pattern) {
        this._logFileNamePattern = pattern;
    }

    public boolean getAppend() {
        return this._append;
    }

    public void setAppend(boolean append) {
        this._append = append;
    }

    /**
     * Scans the registry and locates the configuration file.
     */
    static {
        s_configDir = AppUtil.getInstallDir();
        try {

            if (!Common.isNullorEmpty(ServicePropValues.BIND_ToCLUSTER_IP)) {
                _nodeIP = ServicePropValues.BIND_ToCLUSTER_IP;
            } else {
                String strHostName = InetAddress.getLocalHost().getHostName();
                InetAddress addr = InetAddress.getByName(strHostName);
                _nodeIP = addr.getHostAddress();
            }

        } catch (Exception ex) {

        }

        try {
            s_configDir = new File(s_configDir, DIRNAME).getPath();
        } catch (java.lang.Exception e) {
        }

    }

    public void loadInfo() {
        s_configDir = AppUtil.getInstallDir();
        try {

            if (!Common.isNullorEmpty(ServicePropValues.BIND_ToCLUSTER_IP)) {
                _nodeIP = ServicePropValues.BIND_ToCLUSTER_IP;
            } else {
                String strHostName = InetAddress.getLocalHost().getHostName();
                InetAddress addr = InetAddress.getByName(strHostName);
                _nodeIP = addr.getHostAddress();
            }

        } catch (Exception ex) {

        }

        try {
            s_configDir = new File(s_configDir, DIRNAME).getPath();
        } catch (java.lang.Exception e) {
        }
    }

    @Override
    public String Initialize(java.util.Map properties, String partitionID, String cacheName) throws Exception {
        return Initialize(properties, partitionID, cacheName, false, false);
    }

    @Override
    public final String Initialize(java.util.Map properties, String partitionID, String cacheName, boolean isStartedAsMirror, boolean inproc) throws Exception {
        if((((String)properties.get("enabled")).equalsIgnoreCase("false")))
        {
            isLoggingEnabled = false;
        }
        loadInfo();
        if (this.logger != null) {
            throw new Exception("Multiple Initialize calls for same logger");
        }

        if (_loggerName != null) {
            return _loggerName;
        }
        try {
            _loggerName = cacheName;

            if (partitionID != null && partitionID.length() > 0) {
                _loggerName += "-" + partitionID;
            }

            if (isStartedAsMirror) {
                _loggerName += "-" + "replica";
            }

            if (inproc && !isStartedAsMirror) {
                _loggerName += "." + ManagementFactory.getRuntimeMXBean().getName();
            }

            String LogExceptions = "";

            if (_loggerName.equals("LogExceptions")) {
                LogExceptions = "/LogExceptions";
            }
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            String currTime = dateFormat.format(date).replace('/', '-').replace(':', '-').replace(' ', '-');
            boolean defaultPath = true;
            String initialPath = "";

            if (properties != null) {
                if (properties.containsKey("log-path")) {
                    initialPath = (String) (properties.get("log-path"));
                    if (!initialPath.equals("")) {
                        defaultPath = !isValidLocation(initialPath, cacheName);
                    }
                }
            }

            if (defaultPath) {
                initialPath = GetLogPath();
            }

            String fileName = initialPath + LogExceptions + "/" + _loggerName + "_" + currTime + "_" + _nodeIP + ".%g.txt";
            
            if(isLoggingEnabled)
            {
                //append should b true
                PrintStream out = new PrintStream(fileName.replace("%g", "0"));
                out.print(String.format("%-28s", "TIMESTAMP")
                        + String.format("%-45s", "LOGGERNAME")
                        + String.format("%-35s", "THREAD-ID")
                        + String.format("%-20s", "LEVEL")
                        + "MESSAGE\r\n");
                out.close();
            }
            this.ConfigureLogger(fileName);

            if (properties != null) {
                if (properties.containsKey("trace-errors")) {
                    if (Boolean.parseBoolean(properties.get("trace-errors").toString())) {
                        this.SetLevel(JLevel.Error);
                    }
                }

                if (properties.containsKey("trace-notices")) {
                    if (Boolean.parseBoolean(properties.get("trace-notices").toString())) {
                        this.SetLevel(JLevel.INFO);
                    }
                }

                if (properties.containsKey("trace-warnings")) {
                    if (Boolean.parseBoolean(properties.get("trace-warnings").toString())) {
                        this.SetLevel(JLevel.WARNING);
                    }
                }

                if (properties.containsKey("trace-debug")) {
                    if (Boolean.parseBoolean(properties.get("trace-debug").toString())) {
                        this.SetLevel(JLevel.ALL);
                    }
                }

                if (properties.containsKey("enabled")) {
                    if (!Boolean.parseBoolean(properties.get("trace-errors").toString())) {
                        this.SetLevel(JLevel.OFF);
                    }
                }
            } else {
                this.SetLevel(JLevel.WARNING);
            }

        } catch (Exception e) {
            throw e;
        }

        return _loggerName;

    }

    public final void Initialize(LoggerNames loggerNameEnum, boolean isEventLogs) throws Exception {
        if (logger != null) {
            throw new Exception("Multiple Initialize calls for same logger");
        }

        _loggerName = loggerNameEnum.toString();

        String filepath = "";

        tangible.RefObject<String> tempRef_filepath = new tangible.RefObject<String>(filepath);
        boolean tempVar = !DirectoryUtil.SearchGlobalDirectory(ServicePropValues.LOGS_FOLDER, false, tempRef_filepath);
        filepath = tempRef_filepath.argvalue;
        if (tempVar) {
            try {
                tangible.RefObject<String> tempRef_filepath2 = new tangible.RefObject<String>(filepath);
                DirectoryUtil.SearchLocalDirectory(ServicePropValues.LOGS_FOLDER, true, tempRef_filepath2);
                filepath = tempRef_filepath2.argvalue;
            } catch (Exception ex) {
                throw new Exception("Unable to initialize the log file", ex);
            }
        }

        try {
            filepath = Common.combinePath(filepath, loggerNameEnum.toString().toLowerCase());

            if (!(new java.io.File(filepath)).isDirectory()) {
                (new java.io.File(filepath)).mkdir();
            }

            filepath = Common.combinePath(filepath, loggerNameEnum.toString().toLowerCase());

            this.ConfigureLogger(filepath);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public final void Initialize(LoggerNames loggerName) throws Exception {
        if (loggerName != LoggerNames.Licence) {
            Initialize(loggerName, null);
        }
    }

    @Override
    public final void Initialize(LoggerNames loggerNameEnum, String cacheName) throws Exception {
        if (logger != null) {
            throw new Exception("Multiple Initialize calls for same logger");
        }

        _loggerName = loggerNameEnum.toString();

        String filename = _loggerName;
        if (loggerNameEnum == LoggerNames.ClientLogs) {

            filename = filename + "." + cacheName + "." + ManagementFactory.getRuntimeMXBean().getName();

            _loggerName = cacheName + ManagementFactory.getRuntimeMXBean().getName();
        } else {
            filename = cacheName;
        }

        java.net.InetAddress localMachine = null;
        try {
            localMachine = java.net.InetAddress.getLocalHost();
        } catch (UnknownHostException unknownHostException) {
            throw new Exception(unknownHostException.toString());
        }

        filename = _nodeIP + "_"
                + (filename == null ? "" : filename + ".")
                + localMachine.getHostName().toLowerCase()
                + "."
                + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                .format(new Date())
                .replace('/', '-')
                .replace(':', '-')
                .replace(' ', '-')
                + ".%g.log";

        if (loggerNameEnum == LoggerNames.CacheServiceLogs || loggerNameEnum == LoggerNames.MemcacheGatewayServiceLogs) {
           
            if (loggerNameEnum == LoggerNames.CacheServiceLogs) {
                filename = "cacheservice.%g.log";
            }
            if (loggerNameEnum == LoggerNames.MemcacheGatewayServiceLogs) {
                filename = "memcachegateway.%g.log";
            }

            loggerNameEnum = LoggerNames.ServiceLogs;
        } else if (loggerNameEnum == LoggerNames.CacheHostLogs) {

            if (cacheName != null) {
                filename = cacheName + ".%g.log";
            } else {
                filename = ManagementFactory.getRuntimeMXBean().getName() + ".%g.log";
            }

        }

        String filepath = "";

        tangible.RefObject<String> tempRef_filepath = new tangible.RefObject<String>(filepath);
        boolean tempVar = !DirectoryUtil.SearchGlobalDirectory(ServicePropValues.LOGS_FOLDER, false, tempRef_filepath);
        filepath = tempRef_filepath.argvalue;
        if (tempVar) {
            try {
                tangible.RefObject<String> tempRef_filepath2 = new tangible.RefObject<String>(filepath);
                DirectoryUtil.SearchLocalDirectory(ServicePropValues.LOGS_FOLDER, true, tempRef_filepath2);
                filepath = tempRef_filepath2.argvalue;
            } catch (Exception ex) {
                throw new Exception("Unable to initialize the log file", ex);
            }
        }

        try {

            StringBuilder sb = new StringBuilder(filepath);
            if (!sb.toString().endsWith("\\") || !sb.toString().endsWith("/")) {
                sb.append("/");
            }
            filepath = sb.append(loggerNameEnum.toString().toLowerCase()).toString().toLowerCase();
            if (!(new java.io.File(filepath)).isDirectory()) {
                (new java.io.File(filepath)).mkdir();
            }
            if (!sb.toString().endsWith("\\") || !sb.toString().endsWith("/")) {
                sb.append("/");
            }
            filepath = sb.append(filename).toString();

            //append should b true
            PrintStream out = new PrintStream(filepath.replace("%g", "0"));
            out.print(String.format("%-28s", "TIMESTAMP")
                    + String.format("%-45s", "LOGGERNAME")
                    + String.format("%-35s", "THREAD-ID")
                    + String.format("%-20s", "LEVEL")
                    + "MESSAGE\r\n");
            out.close();

            this.ConfigureLogger(filepath);
        } catch (Exception e) {
            throw e;
        }

    }

    private void ConfigureLogger(String filePath) throws IOException {
        this.logger = Logger.getLogger(_loggerName);
        this.logger.setUseParentHandlers(false);
        this.setLogFileNamePattern(filePath);
        if(isLoggingEnabled)
        {
            FileHandler fh = new FileHandler(this.getLogFileNamePattern(), this.getLimit(), this.getNumLogFiles(), this.getAppend());
            fh.setFormatter(new JFormatter());
            this.logger.addHandler(fh);
        }
    }

    /**
     * Assigns the new level to the logger
     *
     * @param levelName ALL, DEBUG, INFO, ERROR, FATAL
     */
    @Override
    public final void SetLevel(String levelName) {
        logger.setLevel(JLevel.parse(levelName));
    }

    public final void SetLevel(Level level) {
        logger.setLevel(level);
    }

    /**
     * Use {@link #Error(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Error(String message) 
    {
        if(isLoggingEnabled)
            logger.log(JLevel.Error, message);
    }

    /**
     * Use {@link #Fatal(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Fatal(String message) 
    {
        if(isLoggingEnabled)
            logger.log(JLevel.SEVERE, message);
    }

    /**
     * Use {@link #CriticalInfo(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void CriticalInfo(String message) 
    {    
        if(isLoggingEnabled)
            logger.log(JLevel.CriticalInfo, message);
    }

    /**
     * Use {@link #Info(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Info(String message) 
    {
        if(isLoggingEnabled)
            logger.info(message);
    }

    /**
     * Use {@link #DevTrace(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Debug(String message) 
    {
        if(isLoggingEnabled)
            logger.fine((message));
    }

    /**
     * Use {@link #Debug(java.lang.String, java.lang.String) }
     *
     * @param message
     */
    @Override
    public final void Warn(String message) 
    {
        if(isLoggingEnabled)
            logger.log(Level.WARNING, message);
    }

    @Override
    public final boolean getIsInfoEnabled() {
        return logger.isLoggable(JLevel.INFO);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getIsErrorEnabled() {
        return logger.isLoggable(JLevel.Error);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getIsWarnEnabled() {
        return logger.isLoggable(JLevel.WARNING);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getIsDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getIsFatalEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    public final String GetLogPath() {
        if (_path.length() < 1) {
            _path = s_configDir;
        }

        try {
            if (!(new java.io.File(_path)).isDirectory()) {
                (new java.io.File(_path)).mkdir();
            }
        } catch (Exception e) {
        }
        return _path;
    }

    public final boolean isValidLocation(String location, String cache) {
        try {
            Exception ex = new Exception("Invalid Directory Specified");
            if (location.equals("")) {
                throw ex;
            }
            if ((new File(location)).isDirectory()) {
                return true;
            } else {
                throw ex;
            }
        } catch (Exception e) {
            EventLogger.LogEvent("TayzGrid", "The custom path given for logging cache :\"" + cache + "\" info data is not valid", EventType.ERROR, EventCategories.Error, EventID.GeneralError);
        }
        return false;
    }

    private void RemoveAllAppender() {
    }

    /**
     * Creates Buffer Appender, Responsible for storing all logging events in
     * memory buffer and writing them down when by passing the logging events on
     * to the file appender it holds when its buffer becomes full Can also be
     * made a lossy logger which writes only writes down to a file when a
     * specific crieteria/condition is met
     *
     * @param cacheName CacheName used to name the Buffer Appender
     * @param fileName File name to log into
     * @return Returns the created Appender
     */
    public final boolean[] ReadConfig(tangible.RefObject<Integer> bufferAppender) {
        try {

            String EnableLogs = "False";
            String EnableDetailedLogs = "False";
            String BufferSize = "False";

            try {
                if (BufferSize != null) {
                    bufferAppender.argvalue = Integer.parseInt(BufferSize);
                } else {
                    bufferAppender.argvalue = bufferDefaultSize;
                }
            } catch (Exception e) {
                bufferAppender.argvalue = bufferDefaultSize;
            }

            if (EnableDetailedLogs == null && EnableLogs == null) {
                return new boolean[]{
                    false,
                    false
                };
            } else if (EnableDetailedLogs != null && EnableLogs == null) {
                return new boolean[]{
                    false,
                    Boolean.parseBoolean(EnableDetailedLogs)
                };
            } else if (EnableDetailedLogs == null && EnableLogs != null) {
                return new boolean[]{
                    Boolean.parseBoolean(EnableLogs),
                    false
                };
            } else {
                return new boolean[]{
                    Boolean.parseBoolean(EnableLogs),
                    Boolean.parseBoolean(EnableDetailedLogs)
                };
            }
        } catch (Exception ex) {
            bufferAppender.argvalue = bufferDefaultSize;
            return new boolean[]{
                false,
                false
            };
        }

    }

    public final boolean[] ReadClientConfig(tangible.RefObject<Integer> bufferAppender) {
        try {
            String EnableLogs = System.getProperty("ENABLE_CLIENT_LOGS");
            String EnableDetailedLogs = System.getProperty("ENABLE_DETAILED_CLIENT_LOGS");
            String BufferSize = System.getProperty("BufferSize");

            try {
                if (BufferSize != null) {
                    bufferAppender.argvalue = Integer.parseInt(BufferSize);
                } else {
                    bufferAppender.argvalue = bufferDefaultSize;
                }
            } catch (Exception e) {
                bufferAppender.argvalue = bufferDefaultSize;
            }

            if (EnableDetailedLogs == null && EnableLogs == null) {
                return new boolean[]{
                    false,
                    false
                };
            } else if (EnableDetailedLogs != null && EnableLogs == null) {
                return new boolean[]{
                    false,
                    Boolean.parseBoolean(EnableDetailedLogs)
                };
            } else if (EnableDetailedLogs == null && EnableLogs != null) {
                return new boolean[]{
                    Boolean.parseBoolean(EnableLogs),
                    false
                };
            } else {
                return new boolean[]{
                    Boolean.parseBoolean(EnableLogs),
                    Boolean.parseBoolean(EnableDetailedLogs)
                };
            }
        } catch (Exception ex) {
            bufferAppender.argvalue = bufferDefaultSize;
            return new boolean[]{
                false,
                false
            };
        }

    }

    //<editor-fold defaultstate="collapsed" desc="ILogger Members">
    /**
     * <b>No need to call this flush</b>, its automatic there is no force flush
     * available for AsynAppenders, this is by implementation and only close is
     * available which stops and interrupts all the threads and perfmos their
     * operations forcefully
     */
    @Override
    public final void Flush() {
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
    public final void Close() {

        for (Handler handler : this.logger.getHandlers()) {
            handler.close();
            handler.flush();
            logger.removeHandler(handler);
        }

        SetLevel("OFF");
    }

    /**
     * Error logs
     *
     * @param module Module Name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void Error(String module, String message) {
        int space2 = 50;

        if (module.length() == 0) {
            space2 = 4;
        } else {
            space2 = 50 - module.length();
            if (space2 <= 0) {
                space2 = 40;
            }
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
    public final void Fatal(String module, String message) {
        int space2 = 50;

        if (module.length() == 0) {
            space2 = 4;
        } else {
            space2 = 50 - module.length();
            if (space2 <= 0) {
                space2 = 40;
            }
        }

        Fatal(module + String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Critical Information, Level higher than Fatal
     *
     * @param module Module name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void CriticalInfo(String module, String message) {
        int space2 = 50;

        if (module.length() == 0) {
            space2 = 4;
        } else {
            space2 = 50 - module.length();
            if (space2 <= 0) {
                space2 = 40;
            }
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
    public final void Info(String module, String message) {
        int space2 = 50;

        if (module.length() == 0) {
            space2 = 4;
        } else {
            space2 = 50 - module.length();
            if (space2 <= 0) {
                space2 = 40;
            }
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
    public final void Debug(String module, String message) {
        int space2 = 50;

        if (module.length() == 0) {
            space2 = 4;
        } else {
            space2 = 50 - module.length();
            if (space2 <= 0) {
                space2 = 40;
            }
        }

        Debug(module + String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Warning messages
     *
     * @param module Module name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void Warn(String module, String message) {
        int space2 = 50;

        if (module.length() == 0) {
            space2 = 4;
        } else {
            space2 = 50 - module.length();
            if (space2 <= 0) {
                space2 = 40;
            }
        }

        Warn(String.format("%" + space2 + "s", " ") + message);
    }

    /**
     * Used only for Dev tracing <b>To be removed in production</b>
     *
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void DevTrace(String message) {
//        logger.log(JLevel.DevInfo, message);
        logger.log(Level.SEVERE, message);
        //System.err.println(message);
    }

    /**
     * Used only for Dev tracing <b>To be removed in production</b>
     *
     * @param module Module name
     * @param message Message to log, append the stackTrace
     */
    @Override
    public final void DevTrace(String module, String message) {
        int space2 = 50;

        if (module.length() == 0) {
            space2 = 4;
        } else {
            space2 = 50 - module.length();
            if (space2 <= 0) {
                space2 = 40;
            }
        }

        DevTrace(String.format("%" + space2 + "s", " ") + message);
    }
    //</editor-fold>
}
