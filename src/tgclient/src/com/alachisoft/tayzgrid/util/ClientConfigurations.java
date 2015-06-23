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
package com.alachisoft.tayzgrid.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public class ClientConfigurations {

    static boolean _debugApiLoggingEnabled = false;
    static int _debugApiTimeBeforeLoggingStart = 0;
    static int _debugApiNumberOfIterations = 0;
    static int _debugApiDurationOfEachIteration = 0;
    static int _debugApiIntervalBetweenIterations = 0;
    static int _debugApiLoggerThreadLoggingInterval = 5;
    private static final String _configFileName="clientconfig.properties";

    public static boolean getDebugApiLoggingEnabled()
    {
        return _debugApiLoggingEnabled;
    }
    
    public static int getDebugApiTimeBeforeLoggingStart()
    {
        return _debugApiTimeBeforeLoggingStart;
    }
    
    public static int getDebugApiNumberOfIterations()
    {
        return _debugApiNumberOfIterations;
    }
    
    public static int getDebugApiDurationOfEachIteration()
    {
        return _debugApiDurationOfEachIteration;
    }
    
    public static int getDebugApiIntervalBetweenIterations()
    {
        return _debugApiIntervalBetweenIterations;
    }
    
    public static int getDebugApiLoggerThreadLoggingInterval()
    {
        return _debugApiLoggerThreadLoggingInterval;
    }
    
    private static String getFilePath(String fileName){
        String filePath = "";
        if (new File(fileName).exists()) {
            filePath = fileName;
        } else if (new File(".\\bin\\" + fileName).exists()) {
            filePath = ".\\bin\\" + fileName;
        } else if (new File(System.getenv("TG_HOME") + "\\config\\" + fileName).exists()) {
            filePath = System.getenv("TG_HOME") + "\\config\\" + fileName;
        } else {
            filePath=null;
        }
        return filePath;
    }
    
    static{
        if (System.getProperty("CacheClient.EnableAPILogging") == null) {
            String filePath = null;
            if ((filePath = getFilePath(_configFileName)) != null) {
                Properties props = new Properties();
                try {
                    props.load(new FileInputStream(filePath));
                } catch (IOException iOException) {
                }

                Enumeration enu = props.keys();
                while (enu.hasMoreElements()) {
                    String key = (String) enu.nextElement();
                    System.setProperty(key, props.getProperty(key).trim());
                }
            }
        }
        if (System.getProperty("CacheClient.EnableAPILogging") != null) {
            _debugApiLoggingEnabled = Boolean.parseBoolean(System.getProperty("CacheClient.EnableAPILogging"));
        }
        try {
            if (System.getProperty("CacheClient.TimeBeforeLoggingStart") != null) {
                String time = System.getProperty("CacheClient.TimeBeforeLoggingStart");
                String[] splitted = time.split(":");
                if (splitted.length == 3) {
                    _debugApiTimeBeforeLoggingStart = Integer.parseInt(splitted[0]) * 3600 + Integer.parseInt(splitted[1]) * 60 + Integer.parseInt(splitted[2]);
                }
            }
        } catch (NumberFormatException ex) {
        }
        try {
            if (System.getProperty("CacheClient.APILogIteraions") != null) {
                _debugApiNumberOfIterations = Integer.parseInt(System.getProperty("CacheClient.APILogIteraions"));
            }
        } catch (NumberFormatException ex) {
        }

        try {
            if (System.getProperty("CacheClient.APILogIterationLength") != null) {
                _debugApiDurationOfEachIteration = Integer.parseInt(System.getProperty("CacheClient.APILogIterationLength"));
            }
        } catch (NumberFormatException ex) {
        }

        try {
            if (System.getProperty("CacheClient.APILogDelayBetweenIteration") != null) {
                _debugApiIntervalBetweenIterations = Integer.parseInt(System.getProperty("CacheClient.APILogDelayBetweenIteration"));
            }
        } catch (NumberFormatException ex) {
        }
        
               try {
            if (System.getProperty("CacheClient.LoggerThreadLoggingInterval") != null) {
                _debugApiLoggerThreadLoggingInterval = Integer.parseInt(System.getProperty("CacheClient.LoggerThreadLoggingInterval"));
            }
        } catch (NumberFormatException ex) {
        } 
    }
}
