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

package com.alachisoft.tayzgrid.services;

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.logger.JLogger;
import com.alachisoft.tayzgrid.common.logger.LoggerNames;
import com.alachisoft.tayzgrid.integrations.memcached.provider.CacheFactory;
import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.InvalidArgumentsException;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.MemConfiguration;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.ProtocolType;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.TcpNetworkGateway;
import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import org.boris.winrun4j.AbstractService;
import org.boris.winrun4j.ServiceException;

public class MemcachedGatewayService extends AbstractService {

    private static TcpNetworkGateway _tcpTextGateway;
    private static TcpNetworkGateway _tcpBinaryGateway;
    private static String _configPath = "../../config/memcachegateway.properties";
    private static ILogger _logger = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ServiceException {
        MemcachedGatewayService memcachedProxySvc = new MemcachedGatewayService();
        memcachedProxySvc.serviceMain(args);
    }

    private static boolean loadServiceConfiguration() {
        try {
            if (_logger != null) {
                _logger.Info("Loading service configuration........");
            }
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(_configPath));
            } catch (IOException iOException) {
                if (_logger != null) {
                    _logger.Error("An error occured while loading memcachegateway.properties");
                    _logger.Error(iOException.toString());
                }
                return false;
            }

            Enumeration enu = props.keys();
            while (enu.hasMoreElements()) {
                String key = (String) enu.nextElement();
                System.setProperty(key, props.getProperty(key).trim());
            }
            if (_logger != null) {
                _logger.Info("service configuration loaded");
            }
        } catch (Exception e) {
            if (_logger != null) {
                _logger.Error(e.toString());
            }
            return false;
        } finally {
        }
        return true;
    }

    @Override
    public int serviceMain(String[] strings) throws ServiceException {

        try {
            loadServiceConfiguration();
            _logger = new JLogger();
            _logger.Initialize(LoggerNames.MemcacheGatewayServiceLogs);
            _logger.SetLevel("ALL");
        } catch (Exception e) {
        }

        try {

            if (_logger != null) {
                _logger.Info("**********************************************************************");
                _logger.Info("Memcache Gateway Service");
                _logger.Info("**********************************************************************");
            }

            if (!loadServiceConfiguration()) {
                throw new Exception("An error occured while loading memcachegateway.properties.");
            }
        } catch (Exception e) {
            if (_logger != null) {
                _logger.Fatal("Fatal error occured. Error: " + e.toString());
            }
        }
        RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();

        boolean started = startMemcachedProxyServer();

        if (!started) {
            System.exit(1);
        }

        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stopService();
                } catch (Exception exception) {
                }
            }
        }));

        if (currentOS == RuntimeUtil.OS.Linux) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                }
            }).start();

            return 0;

        } else if (currentOS == RuntimeUtil.OS.Windows) {
            waitWindows();
            //Only on Stop
            try {
                stopService();
            } catch (Exception exception) {
                System.exit(1);
                return 1;
            }
        } else {
            if (_logger != null) {
                _logger.Error("OS detection failure");
            }
        }

        System.exit(0);
        return 0;
    }

    private static boolean startMemcachedProxyServer() {
        try {

            if (MemConfiguration.getTextProtocolPort() == MemConfiguration.getBinaryProtocolPort()) {
                throw new InvalidArgumentsException("Ports cannot be same for text and binary protocol.");
            }

            _tcpTextGateway = new TcpNetworkGateway(MemConfiguration.getTextProtocolIP(), MemConfiguration.getTextProtocolPort(), ProtocolType.Text);
            _tcpTextGateway.StartListenForClients();
            LogManager.getLogger().Info("Proxy server for text protocol started at IP: " + MemConfiguration.getTextProtocolIP() + " port: " + MemConfiguration.getTextProtocolPort());

            _tcpBinaryGateway = new TcpNetworkGateway(MemConfiguration.getBinaryProtocolIP(), MemConfiguration.getBinaryProtocolPort(), ProtocolType.Binary);
            _tcpBinaryGateway.StartListenForClients();
            LogManager.getLogger().Info("Proxy server for binary protocol started at IP: " + MemConfiguration.getBinaryProtocolIP() + " port: " + MemConfiguration.getBinaryProtocolPort());
            if (_logger != null) {
                _logger.Info("MemcacheGateway Started...........");
            }
            //Initialize cacheprovider's instance
            if (CacheFactory.createCacheProvider(MemConfiguration.getCacheName()) == null) {
                LogManager.getLogger().Error("TcpNetworkGateway", "Unable to initialize specified cache : " + MemConfiguration.getCacheName());
            }
        } catch (Exception e) {
            if (_logger != null) {
                _logger.Fatal("Unable to start service due to following error:  " + e.getMessage());
            }
            return false;
        } finally {
            if (_logger != null) {
                _logger.Flush();
            }
        }
        return true;
    }

    private static void stopService() throws Exception {
        try {
            if (_logger != null) {
                _logger.Info("Stoping MemcacheGateway Service...");
            }
            try {
                _tcpTextGateway.StopListenForClients();
                _tcpTextGateway.dispose();
            } catch (Exception e) {
                if (_logger != null) {
                    _logger.Error("An error occured while stopping TextProtocol Gateway. Error: " + e.toString());
                }
            }
            try {
                _tcpBinaryGateway.StopListenForClients();
                _tcpBinaryGateway.dispose();
            } catch (Exception e) {
                if (_logger != null) {
                    _logger.Error("An error occured while stopping BinaryProtocol Gateway. Error: " + e.toString());
                }
            }
            CacheFactory.disposeCacheProvider();

        } catch (Exception e) {
            if (_logger != null) {
                _logger.Error("An error occured while stopping Memcache Gateway. Error: " + e.toString());
            }
        } finally {
            if (_logger != null) {
                _logger.Info("MemcacheGateway Service stopped.");
                _logger.Flush();
                _logger.Close();
            }
        }
    }

    /**
     * Wait The MemcacgeGateway Service Thread until Stop command called by
     * window services
     */
    private void waitWindows() {
        while (!shutdown) {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
            }
        }
    }
}
