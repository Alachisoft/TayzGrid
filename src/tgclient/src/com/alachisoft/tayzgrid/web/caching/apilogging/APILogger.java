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

package com.alachisoft.tayzgrid.web.caching.apilogging;

import com.alachisoft.tayzgrid.runtime.caching.Tag;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class APILogger  implements Runnable{

    private String _fileName;
    private String _cacheName;
    private int _iterationNumber = 0;

    private final Queue _logQueue=new LinkedList();
    private Thread _thread;
    private int _logToFileInterval;
    
    private DebugAPIConfigurations _debugAPIConfigurations;
    private boolean _disposed=false;
    
    public APILogger(String cacheName, DebugAPIConfigurations debugAPIConfigurations) {
        _debugAPIConfigurations=debugAPIConfigurations;
       _logToFileInterval=1000*DebugAPIConfigurations.getLoggerThreadLoggingInterval();
        _cacheName = cacheName;
        _thread=new Thread(this);
        _thread.start();
    }

    public final void Log(APILogItem logItem) throws IOException {
        logItem.setLoggingTime(new Date());
        synchronized(_logQueue)
        {
            _logQueue.add(logItem);
            _logQueue.notify();
        }
    }
    private final void LogInternal(APILogItem logItem) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(getFileName(logItem.getLoggingTime()), true)));
            writer.println(logItem.getLoggingTime().toString(/*"MM/dd/yyyy hh:mm:ss.fff tt"*/) + "\t" + logItem.getSignature());
            if (logItem.getNoOfKeys() > 0) {
                writer.println(String.format("%1$-30s\t Number of Keys = %2$s", "", (new Integer(logItem.getNoOfKeys())).toString()));
            }

            if (logItem.getKey()!=null) {
                writer.println(String.format("%1$-30s\t Key = %2$s", " ", logItem.getKey()));
            }

            if (logItem.getAbsolueExpiration() != null) {
                writer.println(String.format("%1$-30s\t Absolute Expiration = %2$s", " ", logItem.getAbsolueExpiration().toString()));
            }
            if (logItem.getSlidingExpiration() != null) {
                writer.println(String.format("%1$-30s\t Sliding Expiration = %2$s milliseconds", " ", logItem.getSlidingExpiration().getTotalMiliSeconds()));
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(logItem.getGroup())) {
                writer.println(String.format("%1$-30s\t Group = %2$s", " ", logItem.getGroup()));
            }
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(logItem.getSubGroup())) {
                writer.println(String.format("%1$-30s\t SubGroup = %2$s", " ", logItem.getSubGroup()));
            }

            if (logItem.getTags() != null) {
                writer.println(String.format("%1$-30s\t Tags:", " "));
                for (Tag t : logItem.getTags()) {
                    writer.println(String.format("%1$-30s\t\tValue = %2$s", " ", t.getTagName()));
                }
            }

            if (logItem.getNamedTags() != null) {
                writer.println(String.format("%1$-30s\t NamedTags:", " "));
                java.util.Iterator ie = logItem.getNamedTags().getIterator();
                while (ie.hasNext()) {
                    Map.Entry entry = (Map.Entry) ie.next();
                    writer.println(String.format("{0,-30}\t\t Key = " + entry.getKey() + "\tValue = " + entry.getValue(), " "));
                }
            }

            if (logItem.getPriority() != null) {
                writer.println(String.format("%1$-30s\t Priority = %2$s", " ", logItem.getPriority().toString()));
            }

   
        
            if (logItem.getIsResyncRequired() != null) {
                writer.println(String.format("%1$-30s\t IsResyncRequired = %2$s", " ", logItem.getIsResyncRequired().toString()));
            }


            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(logItem.getProviderName())) {
                writer.println(String.format("%1$-30s\t ProviderName = %2$s", " ", logItem.getProviderName()));
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(logItem.getResyncProviderName())) {
                writer.println(String.format("%1$-30s\t ResyncProviderName = %2$s", " ", logItem.getResyncProviderName()));
            }

            if (logItem.getDSWriteOption() != null) {
                writer.println(String.format("%1$-30s\t DSWriteOption = %2$s", " ", logItem.getDSWriteOption().toString()));
            }

            if (logItem.getDSReadOption() != null) {
                writer.println(String.format("%1$-30s\t DSReadOption = %2$s", " ", logItem.getDSReadOption().toString()));
            }

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(logItem.getQuery())) {
                writer.println(String.format("%1$-30s\t Query = %2$s", " ", logItem.getQuery().toString()));
            }
            if (logItem.getQueryValues() != null) {
                writer.println(String.format("%1$-30s\t Values:", " "));
                Iterator ide = logItem.getQueryValues().entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry entry = (Map.Entry) ide.next();
                    writer.println(String.format("{0,-30}\t\t Key = " + entry.getKey() + "\tValue = " + entry.getValue(), " "));
                }
            }

            if (logItem.getLockTimeout() != null) {
                writer.println(String.format("%1$-30s\t LockTimeout = %2$s milliseconds", " ", logItem.getLockTimeout().getTotalMiliSeconds()));
            }
            if (logItem.getAcquireLock() != null) {
                writer.println(String.format("%1$-30s\t AcquireLock = %2$s", " ", logItem.getAcquireLock().toString()));
            }
            if (logItem.getReleaseLock() != null) {
                writer.println(String.format("%1$-30s\t ReleaseLock = %2$s", " ", logItem.getReleaseLock().toString()));
            }
            if (logItem.getCacheItemVersion() != null) {
                writer.println(String.format("%1$-30s\t CacheItemVersion = %2$s", " ", logItem.getCacheItemVersion().toString()));
            }
            if (logItem.getRuntimeAPILogItem() != null) {
                RuntimeAPILogItem rtLogItem = logItem.getRuntimeAPILogItem();
                String avg = rtLogItem.getIsBulk() ? "Average " : "";
                if (rtLogItem.getIsBulk()) {
                    writer.println(String.format("%1$-30s\t Number of Objects = %2$s", " ", rtLogItem.getNoOfObjects()));
                }
                writer.println(String.format("%1$-30s\t %2$sObject Size (bytes) = %3$s", " ", avg, rtLogItem.getSizeOfObject()));
                
            }

            if (logItem.getCacheNotificationTypes() != null) {
                writer.println(String.format("%1$-30s\t CacheNotificationTypes:", " "));
                Iterator ie = logItem.getCacheNotificationTypes().iterator();
                while (ie.hasNext()) {
                    writer.println(String.format("%1$-30s\t\t Value = %2$s", " ", ie.next()));
                }
            }
            if (logItem.getCacheStatusNotificationTypes() != null) {
                writer.println(String.format("%1$-30s\t CacheStatusNotificationTypes:", " "));
                Iterator ie = logItem.getCacheStatusNotificationTypes().iterator();
                while (ie.hasNext()) {
                    writer.println(String.format("%1$-30s\t\t Value = %2$s", " ", ie.next()));
                }
            }
            if (logItem.getExceptionMessage() != null) {
                writer.println(String.format("%1$-30s\t Exception = %2$s", " ", logItem.getExceptionMessage().replace('\n', ' ')));
            }


            writer.println();

        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private String getFileName(Date loggingTime) {

        int iterationNumber = _debugAPIConfigurations.GetIterationNumber(loggingTime);
        if (_fileName != null && _iterationNumber == iterationNumber) {
            return _fileName;
        }
        _iterationNumber = iterationNumber;
        String [] pid=ManagementFactory.getRuntimeMXBean().getName().split("@", 2);
        _fileName = getPath() + File.separator + _cacheName + "_" + loggingTime.toString().replace(':', '-') + pid[0] + ".log";
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(_fileName, true)));
            writer.println("TIMESTAMP                      \t API CALL" + "\t");
        } catch (Exception ex) {
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return _fileName;
    }

    private String getPath() {
        String _path = System.getenv("TG_HOME");
        String dirName = "log" + File.separator + "apiusage";
        try {
            File file = new File(_path);
            file = new File(file, dirName);
            _path = file.getPath();
        } catch (java.lang.Exception e) {
        }

        if (_path == null) {
            _path = System.getProperty("user.dir");
            File file = new File(_path);
            file = new File(file, dirName);
            _path = file.getPath();
        }
        if (!(new java.io.File(_path)).isDirectory()) {
            (new java.io.File(_path)).mkdir();
        }
        return _path;
    }

    @Override
    public void run() {
        LogInBackground();
        }
    private void LogInBackground()
    {
        while(!_disposed && !_debugAPIConfigurations.isLoggingExpired())
        {
        try
        {
            while(!_disposed)
            {
                APILogItem apiLogItem = null;
                synchronized(_logQueue)
                {
                    if(_logQueue.size()>0)
                        apiLogItem=(APILogItem)_logQueue.poll();
                }
                if(apiLogItem!=null)
                    LogInternal(apiLogItem);
            }
            if(_logToFileInterval==0)
            {
                synchronized(_logQueue)
                {
                    _logQueue.wait();
                }
            }
            else
                Thread.sleep(_logToFileInterval);
        }
        catch(ThreadDeath e)
        {
            break;
        }
        catch(Exception e)
        {
            try {
                APILogItem logItem = new APILogItem(null, "An error occured while logging" + e.toString());
                LogInternal(logItem);
            } 
            catch (Exception ex) {
            }
        }
        
        }
    }
    
    public void Dispose()
    {
        _disposed=true;
    }
}
