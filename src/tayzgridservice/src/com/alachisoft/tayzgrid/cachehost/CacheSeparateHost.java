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
package com.alachisoft.tayzgrid.cachehost;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.logger.JLogger;
import com.alachisoft.tayzgrid.common.logger.LoggerNames;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.CacheConfigManager;
import com.alachisoft.tayzgrid.management.CacheHost;
import com.alachisoft.tayzgrid.management.CacheServer;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.event.CacheStopped;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.net.Helper;
import com.alachisoft.tayzgrid.common.net.NetworkData;
import com.alachisoft.tayzgrid.common.util.PortCalculator;
import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import com.alachisoft.tayzgrid.socketserver.CommandManagerType;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;

public class CacheSeparateHost implements CacheStopped {

    // Cluster and client IP used by cache separate process.
    private static String bindClusterIP, bindServerIP;
    private static CacheHost cacheHost;
    private static SocketServer socketServer;
    private static SocketServer managementSocketServer;
    private static String cacheName;
    private static int managementPort, socketServerPort;
    private static ILogger logger;
    private static int autoStartDelay = 0;
    private static CacheHostParam cParam = new CacheHostParam();
    private static boolean isLoggerInitilized = false;

    private static final CacheSeparateHost cacheSeparateHost = new CacheSeparateHost();

    public static void main(String[] args) {
        boolean loggerInit;
        try {

            if (args != null && args.length > 0) {
                Object param = new CacheHostParam();
                tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
                CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
                param = tempRef_param.argvalue;
                cParam = (CacheHostParam) param;
                 if (cParam.getIsUsage()) {
                    AssemblyUsage.printUsage();
                    return ;
                }
                readServerProperty(cParam.getServerPropertiesPath());
                try {
                    readCacheName();
                } catch (Exception e) {
                    logger = new JLogger();
                    logger.Initialize(LoggerNames.CacheHostLogs);
                    logger.SetLevel("ALL");
                    isLoggerInitilized = true;
                    throw e;
                }
            } else {
                throw new Exception("Arguments not specified");
            }
            logger = new JLogger();
            logger.Initialize(LoggerNames.CacheHostLogs, cacheName);
            logger.SetLevel("ALL");
            isLoggerInitilized = true;
            if (populateValues(args)) {

                NetworkData.registerIPToMonitor(ServicePropValues.BIND_ToCLUSTER_IP);
                NetworkData.registerIPToMonitor(ServicePropValues.BIND_toClient_IP);
                
                startCacheHost();
                System.out.println("Started");
            }

        } catch (Throwable ex) {

            close(isLoggerInitilized);
            if (logger != null) {
                if(isLoggerInitilized)
                {
                    logger.Error(ex.toString());
                    logger.Info("Separate Cache Process is stopped successfully.");
                    logger.Close();
                }
                
            }
            ex.printStackTrace();
            System.err.println("Failed");
            
        }
    }

    private static void close(boolean isLoggerInitilized) {

        try {

            if (isLoggerInitilized) {
                logger.Info("Separate Cache Process is stopping ...");
            }
            
            if(cacheHost.getHostServer()!=null)
            {
                cacheHost.getHostServer().dispose();
                if (isLoggerInitilized) {
                    logger.Info("Management Server stopped successfully. ");
                }
            }
                                
            if (socketServer != null) {
                socketServer.Stop();
                if (isLoggerInitilized) {
                    logger.Info("Client listener stopped successfully. ");
                }
            }
            if (managementSocketServer != null) {
                managementSocketServer.Stop();
                if (isLoggerInitilized) {
                    logger.Info("Management listener stopped successfully. ");
                }
            }
            if (cacheHost != null) {
                cacheHost.StopHosting();
                cacheHost=null;
            }
            

            

        } catch (Exception ex) {
            if (isLoggerInitilized) {
                logger.Error(ex.toString());
            }
        }
    }

    private static void startCacheHost() throws Exception {
        try {
            if (logger != null) {
                logger.Info("Starting Separate Cache Process....");
            }

           
            if (logger != null) {
                logger.Info("Edition : OPEN SOURCE");
            }
        } catch (Exception e) {
            throwError(e.toString(), false);
        }
        try {

            cacheHost = new CacheHost();
            PortPool.setServerType("CacheServer");
            int sendBuffer = SocketServer.DEFAULT_SOCK_BUFFER_SIZE;
            int receiveBuffer = SocketServer.DEFAULT_SOCK_BUFFER_SIZE;

            String managementAddress = "" + bindClusterIP + ":" + managementPort;
            if (logger != null) {
                logger.Info("Starting management listener on " + managementAddress + " .");
            }
            InetAddress managementIP = parseIP(bindClusterIP);
            if (!Helper.isPortFree(managementPort, managementIP)) {
                throw new Exception("Management listener can not be started on the given " + managementPort + " port. The port might be already in use.");
            }
            managementSocketServer = new SocketServer(managementPort, sendBuffer, receiveBuffer);
            managementSocketServer.Start(managementIP, LoggerNames.CacheManagementSocketServer, "TayzGrid Management", CommandManagerType.TayzGridManagement);

            if (logger != null) {
                logger.Info("Management listener started successfully. ");
            }
            String clientServerAddress = "" + bindServerIP + ":" + socketServerPort;
            if (logger != null) {
                logger.Info("Starting client listener on " + clientServerAddress);
            }
            InetAddress socketServerIP = parseIP(bindServerIP);
            if (!Helper.isPortFree(socketServerPort, socketServerIP)) {
                throw new Exception("Client listener can not be started on the given " + socketServerPort + " port. The port might be already in use.");
            }

            socketServer = new SocketServer(socketServerPort, sendBuffer, receiveBuffer);
            socketServer.Start(socketServerIP, LoggerNames.SocketServerLogs, "Cache Host", CommandManagerType.TayzGridClient);

            if (logger != null) {
                logger.Info("Client listener started successfully. ");
            }

            CacheServer.setSocketServerPort(socketServerPort);

            cacheHost.getHostServer().SynchronizeClientConfig();
            cacheHost.getHostServer().setClusterIP(bindClusterIP);
            cacheHost.getHostServer().setRenderer(socketServer);

            cacheHost.getHostServer().RegisterCacheStopCallBack(cacheSeparateHost);

            com.alachisoft.tayzgrid.socketserver.CacheProvider.setProvider(cacheHost.getHostServer());

            if (autoStartDelay > 0) {
                Thread.sleep(autoStartDelay * 1000);
            }

            if (logger != null) {
                logger.Info("Starting cache on its separate process.");
            }

            cacheHost.getHostServer().StartCache(cacheName, null, null);

            if (logger != null) {
                logger.Info("Cache: " + cacheName + " is started successfully. ");
                logger.Info("Separate Cache Process is started successfully.");
            }

        } catch (Exception ex) {
            throwError(ex.toString(), false);
        }
    }

    private static boolean populateValues(String args[]) throws Exception {

        boolean isValid = false;
        String errorMessage = "";
        try {
            if (args != null && args.length > 0) {
                Object param = new CacheHostParam();
                tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
                CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
                param = tempRef_param.argvalue;
                cParam = (CacheHostParam) param;

                if (cParam.getIsUsage()) {
                    AssemblyUsage.printUsage();
                    return false;
                }

                if (!Common.isNullorEmpty(cParam.getCacheName())) {
                    cacheName = cParam.getCacheName();
                    isValid = true;
                } else {
                    if (Common.isFileExist(cParam.getCacheConfigPath())) {
                        try {
                            if (isCacheExist(cParam.getCacheConfigPath())) {
                                String cName = getCacheName(cParam.getCacheConfigPath());
                                if (!Common.isNullorEmpty(cName)) {
                                    cacheName = cName;
                                    isValid = true;
                                } else {
                                    errorMessage = "Provided config.conf is not valid cache configurations.";
                                    isValid = false;
                                }
                            } else {
                                errorMessage = "Multiple cache configurations provided in config.conf.";
                                isValid = false;
                            }
                        } catch (Exception ex) {
                            errorMessage = ex.getMessage();
                            isValid = false;
                        }
                    } else {
                        errorMessage = "Cache configuration [config.conf] path is not provided or File does not exist.";
                        isValid = false;
                    }
                }

                throwError(errorMessage, isValid);

//                if (cParam.getManagementPort() != -1) {
                    //managementPort = cParam.getManagementPort();
                    managementPort = PortCalculator.getManagementPort(cParam.getSocketServerPort());
//                    isValid = true;
//                } else {
//                    if (Common.isFileExist(cParam.getCacheConfigPath())) {
//                        int[] portarray = getPorts(cParam.getCacheConfigPath(), cacheName);
//                        if (portarray[1] <= 0) {
//                            errorMessage = "Cache configuration [cache.conf] path does not existed. ";
//                            errorMessage = "provided cache is not specified in cache.conf.";
//                            isValid = false;
//                        } else {
//                            managementPort = portarray[1];
//                            isValid = true;
//                        }
//                    } else {
//                        errorMessage = "Cache configuration [cache.conf] path is not provided or file does not exist.";
//                        isValid = false;
//                    }
//                }
//                throwError(errorMessage, isValid);

                if (cParam.getSocketServerPort() != -1) {
                    socketServerPort = cParam.getSocketServerPort();
                    isValid = true;
                } else {
                    if (Common.isFileExist(cParam.getCacheConfigPath())) {
                        int[] portarray = getPorts(cParam.getCacheConfigPath(), cacheName);
                        if (portarray[0] <= 0) {
                            errorMessage = "provided cache is not specified in cache.conf.";
                            isValid = false;
                        } else {
                            socketServerPort = portarray[0];
                            isValid = true;
                        }
                    } else {
                        errorMessage = "Cache configuration [cache.conf] path is not provided or file does not exist.";
                        isValid = false;
                    }
                }
                throwError(errorMessage, isValid);
                readServerPropertyFile(cParam.getServerPropertiesPath());
            } else {
                errorMessage = "Arguments not specified";
                isValid = false;
                throwError(errorMessage, isValid);
            }

        } catch (Exception ex) {
            throwError(ex.toString(), false);
        }
        return true;
    }

    private static boolean isCacheExist(String fileName) throws Exception {
        boolean isCacheExist = false;
        try {
            CacheServerConfig[] configCaches = CacheConfigManager.GetConfiguredCaches(fileName);
            if (configCaches != null && configCaches.length == 1) {
                isCacheExist = true;
            }
        } catch (ManagementException ex) {
            throwError(ex.toString(), false);
        }
        return isCacheExist;
    }

    private static String getCacheName(String fileName) throws Exception {
        String name = "";
        try {
            CacheServerConfig[] configCaches = CacheConfigManager.GetConfiguredCaches(fileName);
            if (configCaches != null && configCaches.length == 1) {
                name = configCaches[0].getName();
            }
        } catch (ManagementException ex) {
            throwError(ex.toString(), false);
        }
        return name;
    }

    private static void readServerPropertyFile(String filePath) throws Exception {

        String serverPropertiesPath = "";
        String errorMessage = "";
        Boolean isValid = false;

        if (!Common.isNullorEmpty(filePath)) {
            serverPropertiesPath = filePath;
        } else {
            String tgHome = Common.getTGHome();
            if (!Common.isNullorEmpty(tgHome)) {
                RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();
                if (currentOS == RuntimeUtil.OS.Linux) {
                    serverPropertiesPath = Common.combinePath(tgHome, "/config/server.properties");
                } else {
                    serverPropertiesPath = Common.combinePath(tgHome, "\\config\\server.properties");
                }
            }
        }

        if (Common.isFileExist(serverPropertiesPath)) {
            if (logger != null) {
                logger.Info("Loading service configuration........");
            }
            isValid = true;
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(serverPropertiesPath));
            } catch (IOException iOException) {
                isValid = false;
                errorMessage = "An error occurred while loading server.properties.";
            }

            Enumeration enu = props.keys();
            while (enu.hasMoreElements()) {
                String key = (String) enu.nextElement();
                System.setProperty(key, props.getProperty(key).trim());

            }
            ServicePropValues.initialize();
            bindClusterIP = ServicePropValues.BIND_ToCLUSTER_IP;
            bindServerIP = ServicePropValues.BIND_toClient_IP;
            String autostart = ServicePropValues.AUTO_START_DELAY;
            if (autostart != null && autostart.length() > 0) {
                try {
                    autoStartDelay = Integer.parseInt(autostart);
                } catch (NumberFormatException formatException) {
                    isValid = false;
                    errorMessage = "Invalid Auto Start Delay Interval is specified in service configuration. ";
                }
            }
            if (logger != null) {
                logger.Info("Service configuration loaded successfully.");
            }
        } else {
            isValid = false;
            errorMessage = "Service property File path is not provided or path does not exist.";
        }
        throwError(errorMessage, isValid);
    }

    private static int[] getPorts(String fileName, String cacheName) throws Exception {
        int[] portarray = new int[]{0, 0};
        try {
            CacheServerConfig[] configCaches = null;
            if (Common.isNullorEmpty(fileName)) {
                configCaches = CacheConfigManager.GetConfiguredCaches();
            } else {
                configCaches = CacheConfigManager.GetConfiguredCaches(fileName);
            }
            if (configCaches != null && configCaches.length > 0 && !Common.isNullorEmpty(cacheName)) {
                for (CacheServerConfig cacheServerConfig : configCaches) {
                    if (cacheServerConfig.getName() != null && !cacheServerConfig.getName().isEmpty() && cacheName.toLowerCase().equals(cacheServerConfig.getName().toLowerCase())) {
                        portarray[0] = cacheServerConfig.getClientPort();
                        //portarray[1] = cacheServerConfig.getManagementPort(); Waleed
                        portarray[1] = PortCalculator.getManagementPort(portarray[0]);
                        break;
                    }
                }
            }
        } catch (ManagementException ex) {
            throwError(ex.toString(), false);
        }
        return portarray;
    }

    private static InetAddress parseIP(String bindIp) throws Exception {
        InetAddress bindingIP = null;
        String[] str = bindIp.split("\\.");
        byte[] bite = new byte[4];
        bite[0] = new Integer(str[0]).byteValue();
        bite[1] = new Integer(str[1]).byteValue();
        bite[2] = new Integer(str[2]).byteValue();
        bite[3] = new Integer(str[3]).byteValue();
        try {
            bindingIP = InetAddress.getByAddress(bite);
        } catch (UnknownHostException ex) {
            throwError(ex.toString(), false);
        }
        return bindingIP;
    }

    private static void throwError(String errorMessage, boolean isValid) throws Exception {
        if (!isValid) {
            throw new Exception(errorMessage);
        }
    }

    @Override
    public void onCacheStopped() {

        Thread stopcacheThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    close(true);
                    System.exit(0);
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.Error(ex.toString());
                        logger.Close();
                    }
                }
            }
        });
        stopcacheThread.start();

    }

    private static void readServerProperty(String filePath) throws Exception {

        try {
            String serverPropertiesPath = "";
            if (!Common.isNullorEmpty(filePath)) {
                serverPropertiesPath = filePath;
            }
            if (Common.isFileExist(serverPropertiesPath)) {
                Properties props = new Properties();
                try {
                    props.load(new FileInputStream(serverPropertiesPath));
                } catch (IOException iOException) {
                    throw iOException;
                }
                Enumeration enu = props.keys();
                while (enu.hasMoreElements()) {
                    String key = (String) enu.nextElement();
                    System.setProperty(key, props.getProperty(key).trim());

                }
                ServicePropValues.initialize();
            } else {
                ServicePropValues.loadServiceProp();
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    private static void readCacheName() throws Exception {
        boolean isValid = false;
        String errorMessage = null;
        if (!Common.isNullorEmpty(cParam.getCacheName())) {
            cacheName = cParam.getCacheName();
            isValid = true;
        } else {
            if (Common.isFileExist(cParam.getCacheConfigPath())) {
                try {
                    if (isCacheExist(cParam.getCacheConfigPath())) {
                        String cName = getCacheName(cParam.getCacheConfigPath());
                        if (!Common.isNullorEmpty(cName)) {
                            cacheName = cName;
                            isValid = true;
                        } else {
                            errorMessage = "Provided config.conf is not valid cache configurations.";
                            isValid = false;
                        }
                    } else {
                        errorMessage = "Multiple cache configurations provided in cache.conf.";
                        isValid = false;
                    }
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    isValid = false;
                }
            } else {
                errorMessage = "Cache configuration [cache.conf] path is not provided or File does not exist.";
                isValid = false;
            }
        }
        throwError(errorMessage, isValid);

    }

}
