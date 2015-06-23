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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alachisoft.tayzgrid.services;


import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.management.ServerHost;
import com.alachisoft.tayzgrid.socketserver.CacheProvider;
import java.io.File; 

import com.alachisoft.tayzgrid.common.EncryptionUtil;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.logger.JLogger;
import com.alachisoft.tayzgrid.common.logger.LoggerNames;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.CacheConfigManager;
import com.alachisoft.tayzgrid.management.CacheServer;
import com.alachisoft.tayzgrid.socketserver.CommandManagerType;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import com.google.protobuf.ServiceException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Properties;
import org.boris.winrun4j.AbstractService;


public class TayzGrid extends AbstractService {

    private static String TCP_CHANNEL_NAME = "host-stcp";
    private static String HTTP_CHANNEL_NAME = "host-shttp";
    private static long tcpPort = 0;
    private static long httpPort = 0;
    private static int serverPort = 9800;
    private static String bindClusterIP = "";
    private static String bindServerIP = "";
    private static int autoStartDelay = 0;
    private static String cacheAutoStartUser = "";
    private static String cacheAutoStartPassword = "";
    private static int managementServerPort = 10000;
    private static ServerHost cacheHost = null;
    private static SocketServer _socketServer;
    public static SocketServer _managementSocketServer;
    public static Object waitObj = new Object();
    public static boolean waitstart = false;
    private static ILogger _logger;
    private static String _configPath = "config"+File.separator+"server.properties";

    private static  boolean started = true;

    static {
        try {
            loadServiceConfiguration();
            _logger = new JLogger();
            _logger.Initialize(LoggerNames.CacheServiceLogs);
            _logger.SetLevel("ALL");
        } catch (Exception e) {
 
        }

        try {

            if (_logger != null) {
                _logger.Info("**********************************************************************");
                _logger.Info("TayzGrid Service");
                _logger.Info("**********************************************************************");
            }

            if (!loadServiceConfiguration()) {
                throw new Exception("An error occurred while loading server.properties.");
            }

            try {

                if (_logger != null) {
                    _logger.Info("Edition : OPEN SOURCE");
                }
            } catch (Exception e) {
                if (_logger != null) {
                    _logger.Info("Edition : Unknown.");
                }
            }

        } catch (Exception e) {
            if (_logger != null) {
                _logger.Error("Fatal error occurred.");
                _logger.equals(e.toString());
            }

        } finally {

        }

       boolean started = startCacheServer();
		if (!started) {
            if (_logger != null) {
                _logger.Flush();
                _logger.Close();
                _logger = null;
            }

            System.exit(1);
        }
    }

    public static void main(String[] args) throws org.boris.winrun4j.ServiceException {
        TayzGrid cacheService = new TayzGrid();
        cacheService.serviceMain(args);
    }

    private static boolean loadServiceConfiguration() {
        try {
            if (_logger != null) {
                _logger.Info("Loading service configuration........");
            }
            Properties props = new Properties();
            try {
                //System.out.println("Loading server.properties");
                String path = Common.combinePath(Common.getTGHome(), _configPath);
                props.load(new FileInputStream(path));
            } catch (IOException iOException) {
                if (_logger != null) {
                    _logger.Error("An error occurred while loading server.properties.");
                    _logger.Error(iOException.toString());
                    return false;
                }
            }

            Enumeration enu = props.keys();
            while (enu.hasMoreElements()) {
                String key = (String) enu.nextElement();
                System.setProperty(key, props.getProperty(key).trim());
            }

            ServicePropValues.initialize();
            EventLogger.Initialize();
            String configPort = ServicePropValues.CACHE_MANAGEMENT_PORT;
            if (configPort != null && configPort.length() > 0) {
                try {
                    tcpPort = Integer.parseInt(configPort);
                } catch (NumberFormatException formatException) {
                    throw new Exception("Invalid CacheManagementServer.Port is specified in service configuration. ");
                }
            }
            configPort = ServicePropValues.HTTP_PORT;//System.getProperty("Http.Port");
            if (configPort != null && configPort.length() > 0) {
                httpPort = Integer.parseInt(configPort);
            }
            String socketPort = ServicePropValues.CACHE_SERVER_PORT;
            if (socketPort != null && socketPort.length() > 0) {
                try {
                    serverPort = Integer.parseInt(socketPort);
                } catch (NumberFormatException formatException) {
                    throw new Exception("Invalid CacheServer.Port is specified in service configuration. ");
                }
            }
            bindClusterIP = ServicePropValues.BIND_ToCLUSTER_IP;//System.getProperty("JvCacheServer.BindToClusterIP");
            bindServerIP = ServicePropValues.BIND_toClient_IP;//System.getProperty("JvCacheServer.BindToClientServerIP");
            String mgtConfigPort = ServicePropValues.CACHE_MANAGEMENT_PORT;
            if (mgtConfigPort != null && mgtConfigPort.length() > 0) {
                try {
                    managementServerPort = Integer.parseInt(mgtConfigPort);
                } catch (NumberFormatException formatException) {
                    throw new Exception("Invalid CacheManagementServer.Port is specified in service configuration. ");
                }
            }
            String autostart = ServicePropValues.AUTO_START_DELAY;
            if (autostart != null && autostart.length() > 0) {
                try {
                    autoStartDelay = Integer.parseInt(autostart);
                } catch (NumberFormatException formatException) {
                    if (_logger != null) {
                        _logger.Warn("Invalid AutoStartDelay interval is specified in service configuration. ");
                    }
                }
            }

            if (ServicePropValues.CACHE_AUTOSTART_USER != null) {
                cacheAutoStartUser = ServicePropValues.CACHE_AUTOSTART_USER;
            }
            if (ServicePropValues.CACHE_AUTOSTART_PASSWORD != null) {
                cacheAutoStartPassword = ServicePropValues.CACHE_AUTOSTART_PASSWORD;
            }
            if (_logger != null) {
                _logger.Info("Service configuration loaded.");
            }
        } catch (Exception e) {
            if (_logger != null) {
                // _logger.Error("an error occured while loading server.properties");
                _logger.Error(e.toString());
            }
            return false;
        } finally {

            if (tcpPort < 1) {
                tcpPort = CacheConfigManager.DEF_TCP_PORT;
            }
            if (httpPort < 1) {
                httpPort = CacheConfigManager.DEF_HTTP_PORT;
            }
        }
        return true;
    }

    @Override
    public int serviceMain(String[] strings) throws org.boris.winrun4j.ServiceException {

        RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();

//        if(_logger != null) 
//                _logger.Flush();
        //Register jvm shutdown hook.....
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (_logger != null) {
                    _logger.Info("Received stop signal ... ");
                }
                try {
                    stopCacheServer();
                } catch (Exception exception) {
                }
            }
        }));

        if (currentOS == RuntimeUtil.OS.Linux) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AutoStartCaches();
                    // CacheServer.SetWaitOnServiceObject();
                    //System.exit(0);
                }
            }).start();

            return 0;

        } else if (currentOS == RuntimeUtil.OS.Windows) {
            AutoStartCaches();
            waitWindows();
            //Only on Stop
            try {
                stopCacheServer();
            } catch (Exception exception) {
                //exception.printStackTrace();
                System.exit(1);
                return 1;
            }
        } else {
            if (_logger != null) {
                _logger.Error("OS detection failure.");
            }
        }

        System.exit(0);
        return 0;
    }

    private static void stopCacheServer() { 
        try {
            //Kill caches on Service Stop
            loadServiceConfiguration(); //MUST
            if(ServicePropValues.UninstallInProgress.equalsIgnoreCase("true"))
            {
                CacheServer cacheServer = cacheHost.getCacheServer();
                cacheServer.KillAllCaches();
            }
            //
            if (_logger != null) {
                _logger.Info("Stopping management listener ... ");
            }

            cacheHost.StopHosting();

            try {
                _managementSocketServer.Stop();
            } catch (Exception ex) {
                if (_logger != null) {
                    _logger.Error("Unable to stop management listener due to following error ");
                    _logger.Error(ex.toString());
                }
            }

            if (_logger != null) {
                _logger.Info("Stopping client listener ... ");
            }
            try {
                _socketServer.Stop();
            } catch (Exception ex) {
                if (_logger != null) {
                    _logger.Error("Unable to stop client listener due to following error ");
                    _logger.Error(ex.toString());
                }
            }
        } catch (Exception e) {
            if (_logger != null) {
                _logger.Error("An error occurred while stopping cache server. ");
                _logger.Error(e.toString());
            }
        } finally {
            if (_logger != null) {
                _logger.Info("TayzGrid service stopped");
                _logger.Flush();
                _logger.Close();
            }
        }

    }

    public static boolean startCacheServer() {
        try {
            int sendBuffer = SocketServer.DEFAULT_SOCK_BUFFER_SIZE;
            int receiveBuffer = SocketServer.DEFAULT_SOCK_BUFFER_SIZE;
            InetAddress bindingClusterIP = null;
            InetAddress bindingServerIP = null;

            if (bindClusterIP == null || bindClusterIP == "") {
                throw new Exception("Invalid CacheServer.BindToClusterIP is specified in service configuration. ");
            } else {
                try {
                    bindingClusterIP = parseIP(bindClusterIP);
                } catch (Exception e) {
                    throw new Exception("Invalid CacheServer.BindToClusterIP is specified in service configuration. ");
                }
            }

            if (bindServerIP == null || bindServerIP == "") {
                throw new Exception("Invalid CacheServer.BindToClientServerIP is specified in service configuration. ");
            } else {
                try {
                    bindingServerIP = parseIP(bindServerIP);
                } catch (Exception e) {
                    throw new Exception("Invalid CacheServer.BindToClientServerIP is specified in service configuration. ");
                }
            }
            cacheHost = new ServerHost();
            PortPool.setServerType("CacheServer");
            cacheHost.StartHosting(TCP_CHANNEL_NAME, (int) tcpPort, HTTP_CHANNEL_NAME, (int) httpPort, bindServerIP);
            //System.out.println("Management Port          : " + managementServerPort);
            String managementAddress = "" + bindClusterIP + ":" + managementServerPort;
            if (_logger != null) {
                _logger.Info("Starting management listener on " + managementAddress + " .");
            }
            _managementSocketServer = new SocketServer(managementServerPort, sendBuffer, receiveBuffer);
            _managementSocketServer.Start(bindingClusterIP, LoggerNames.CacheManagementSocketServer, "Cache Management", CommandManagerType.TayzGridManagement);

            if (_logger != null) {
                _logger.Info("Management listener started successfully. ");
            }

            String clientServerAddress = "" + bindServerIP + ":" + serverPort;
            if (_logger != null) {
                _logger.Info("Starting client listener on " + clientServerAddress);
            }

            //System.out.println("Socket Server Port       : " + serverPort);
            _socketServer = new SocketServer(serverPort, sendBuffer, receiveBuffer);

            _socketServer.Start(bindingServerIP, LoggerNames.SocketServerLogs, "Cache Host", CommandManagerType.TayzGridClient);
            if (_logger != null) {
                _logger.Info("Client listener started successfully. ");
            }

            CacheServer.setSocketServerPort(serverPort);
           
            cacheHost.getCacheServer().SynchronizeClientConfig();
            cacheHost.getCacheServer().setClusterIP(bindClusterIP);
            CacheProvider.setProvider(cacheHost.getCacheServer());
 


            if (_logger != null) {
                _logger.Info("TayzGrid Server Started");
            }
        } catch (Exception exception) {
            if (_logger != null) {
                _logger.Error("Unable to start service due to following error  " + exception.getMessage());
            }
            return false;
        } finally {
            if (_logger != null) {
                _logger.Flush();
            }
        }

        return true;
    }

    private static InetAddress parseIP(String bindIp) {
        InetAddress bindingIP = null;
        String[] str = bindIp.split("\\.");
        byte[] bite = new byte[4];
        bite[0] = new Integer(str[0]).byteValue();
        bite[1] = new Integer(str[1]).byteValue();
        bite[2] = new Integer(str[2]).byteValue();
        bite[3] = new Integer(str[3]).byteValue();
        try {
            bindingIP = InetAddress.getByAddress(bite);
        } catch (Exception e) {
            //System.out.println(e.getMessage());
        }
        return bindingIP;
    }

    private void waitWindows() {
        int count = 0;
        while (!shutdown) {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
            }
        }
    }

    private static void AutoStartCaches() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (autoStartDelay > 0) {
                        Thread.sleep(autoStartDelay * 1000);
                    }
                    CacheServerConfig[] configCaches = CacheConfigManager.GetConfiguredCaches();
                    if (configCaches != null && configCaches.length > 0) {
                        for (CacheServerConfig cacheServerConfig : configCaches) {
                            if (cacheServerConfig.getAutoStartCacheOnServiceStartup() && !cacheServerConfig.getIsRegistered() && cacheServerConfig.getInProc() == false) {
                                try {
                                    cacheHost.getCacheServer().StartCache(cacheServerConfig.getName(), EncryptionUtil.Encrypt(cacheAutoStartUser), EncryptionUtil.Encrypt(cacheAutoStartPassword));
 
                                } catch (Exception e) {
                                }// all exceptions are logged in event logs. and are ignored here.
                            }
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }).start();
    }

 
 
}
