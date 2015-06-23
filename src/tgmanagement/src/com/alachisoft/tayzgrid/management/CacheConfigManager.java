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

package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.XmlConfigReader;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.io.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helps locate the configuration information file.
 */
public class CacheConfigManager {

    /**
     * Default tcp channel port.
     */
    public static final int DEF_TCP_PORT = Integer.parseInt(ServicePropValues.CACHE_MANAGEMENT_PORT);
    /**
     * Default http channel port.
     */
    public static final int DEF_HTTP_PORT = 8251;
    /**
     * Default IPC channel port.
     */
    public static final String DEF_IPC_PORT_NAME = "jvcCacheHost";
    /**
     * Configuration file folder name
     */
    private static final String DIRNAME = "config";
    /**
     * Configuration file name
     */
    private static final String FILENAME = "cache.conf";
    /**
     * Path of the configuration file.
     */
    private static String s_configFileName = "";
    /**
     * Path of the configuration folder.
     */
    private static String s_configDir = "";
    /**
     * Default tcp channel port.
     */
    protected static int s_tcpPort = DEF_TCP_PORT;
    /**
     * Default http channel port.
     */
    protected static int s_httpPort = DEF_HTTP_PORT;
    /**
     * Default IPC channel port.
     */
    protected static String s_ipcPortName = DEF_IPC_PORT_NAME;

    protected CacheConfigManager() {
    }

    /**
     * static constructor
     */
    static {
        try {
            CacheConfigManager.ScanConfiguration();
        } catch (ManagementException managementException) {
        } catch (Exception e) {
        }

    }

    /**
     * Configuration files folder.
     */
    public static String getDirName() {
        return s_configDir;
    }

    /**
     * Configuration file name.
     */
    public static String getFileName() {
        return s_configFileName;
    }

    /**
     * Configuration file name.
     */
    public static int getTcpPort() {
        return s_tcpPort;
    }

    /**
     * Configuration file name.
     */
    public static int getHttpPort() {
        return s_httpPort;
    }

    /**
     * Configuration file name.
     */
    public static String getIPCPortName() {
        return s_ipcPortName;
    }

    /**
     * Scans the registry and locates the configuration file.
     */
    public static void ScanConfiguration() throws ManagementException, Exception {

        try {
            s_configDir = AppUtil.getInstallDir();
            if (s_configDir == null || s_configDir.length() == 0) {
                throw new ManagementException("Missing installation folder information");
            }
            s_configDir = Common.combinePath(s_configDir, DIRNAME);
            if (!(new java.io.File(s_configDir)).isDirectory()) {
                (new java.io.File(s_configDir)).mkdir();
            }
            s_configFileName = Common.combinePath(s_configDir, FILENAME);

            if (!(new java.io.File(s_configFileName)).isFile() ||(new java.io.File(s_configFileName)).length()==0 ) {
                /**
                 * Save a dummy configuration.
                 */
                SaveConfiguration(null);
            }
        } catch (ManagementException e) {
            s_configFileName = "";
            throw e;
        } catch (Exception e) {
            s_configFileName = "";
            throw new ManagementException(e.getMessage(), e);
        }

        try {
            String v = ServicePropValues.CACHE_MANAGEMENT_PORT;
            if (v != null) {
                int port = Integer.parseInt(v);
                s_tcpPort = port;
            }
        } catch (Exception e) {
            throw e;
        }
        try {
            String v = ServicePropValues.HTTP_PORT;
            if (v != null) {
                int port = Integer.parseInt(v);
                s_httpPort = port;
            }
        } catch (Exception e) {
            throw e;
        }
        try {
            String v = ServicePropValues.IPC_PortName;
            if (v != null) {
                String portName = String.valueOf(v);
                if (portName != null) {
                    s_ipcPortName = portName;
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Initialize a registered cache given by the ID.
     *
     * @param cacheId A string identifier of configuration.
     */
    public static java.util.ArrayList GetCacheConfig(String cacheId, String userId, String password, boolean inProc) throws ManagementException {
        if (getFileName().length() == 0) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt");
        }
        try {
            XmlConfigReader configReader = new XmlConfigReader(getFileName(), cacheId);
            java.util.ArrayList propsList = configReader.getPropertiesList();
            java.util.ArrayList configsList = CacheConfig.GetConfigs(propsList);
            for (Iterator it = configsList.iterator(); it.hasNext();) {
                CacheConfig config = (CacheConfig) it.next();
                if (!inProc) {
                    inProc = config.getUseInProc();
                }
                break;
            }

            if (inProc) {
                return configsList;
            }
            return null;
        } catch (ManagementException e2) {
            throw e2;
        } catch (Exception e) {
            throw new ManagementException(e.getMessage(), e);
        }
    }

    /**
     * Initialize a registered cache given by the ID.
     *
     * @param cacheId A string identifier of configuration.
     */
    public static CacheConfig GetCacheConfig(String cacheId) throws ManagementException {
        if (getFileName().length() == 0) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt");
        }
        try {
            XmlConfigReader configReader = new XmlConfigReader(getFileName(), cacheId);
            return CacheConfig.FromProperties(configReader.getProperties());
        } catch (Exception e) {
            throw new ManagementException(e.getMessage(), e);
        }
    }

    public static CacheServerConfig GetUpdatedCacheConfig(String cacheId, String partId, String newNode, tangible.RefObject<java.util.ArrayList> affectedNodes, boolean isJoining) throws ManagementException {
        if (getFileName().length() == 0) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt");
        }

        try {
            XmlConfigReader configReader = new XmlConfigReader(getFileName(), cacheId);
            CacheServerConfig config = configReader.GetConfigDom();

            String list = config.getCluster().getChannel().getInitialHosts().toLowerCase();
            String[] nodes = list.split("[,]", -1);

            if (isJoining) {
                for (String node : nodes) {
                    String[] nodename = node.split("[\\[]", -1);
                    affectedNodes.argvalue.add(nodename[0]);
                }

                if (list.indexOf(newNode) == -1) {
                    list = list + "," + newNode + "[" + config.getCluster().getChannel().getTcpPort() + "]";
                }
            } else {
                for (String node : nodes) {
                    String[] nodename = node.split("[\\[]", -1);
                    if (!newNode.equals(nodename[0])) {
                        affectedNodes.argvalue.add(nodename[0]);
                    }
                }

                list = "";
                for (Iterator it = affectedNodes.argvalue.iterator(); it.hasNext();) {
                    String node = (String) it.next();
                    if (list.length() == 0) {
                        list = node + "[" + config.getCluster().getChannel().getTcpPort() + "]";
                    } else {
                        list = list + "," + node + "[" + config.getCluster().getChannel().getTcpPort() + "]";
                    }
                }
            }

            config.getCluster().getChannel().setInitialHosts(list);

            return config;
        } catch (Exception e) {
            throw new ManagementException(e.getMessage(), e);
        }
    }

    /**
     * Loads and returns all cache configurations from the configuration file.
     */
    public static CacheServerConfig[] GetConfiguredCaches() throws ManagementException {
        if (FILENAME.length() == 0) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt.");
        }
        try {
            ConfigurationBuilder builder = new ConfigurationBuilder(getFileName());
            builder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class);
            builder.ReadConfiguration();
            com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[] caches = new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[builder.getConfiguration().length];
            System.arraycopy(builder.getConfiguration(), 0, caches, 0, builder.getConfiguration().length);

            return convertToOldDom(caches);
        } catch (Exception e) {
            throw new ManagementException(e.getMessage(), e);
        }
    }
    
      /**
     * Loads and returns all cache configurations from the configuration file.
     */
    public static CacheServerConfig[] GetConfiguredCaches(String filePath) throws ManagementException {
        if (Common.isNullorEmpty(filePath)) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt.");
        }
        try {
            ConfigurationBuilder builder = new ConfigurationBuilder(filePath);
            builder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class);
            builder.ReadConfiguration();
            com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[] caches = new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[builder.getConfiguration().length];
            System.arraycopy(builder.getConfiguration(), 0, caches, 0, builder.getConfiguration().length);

            return convertToOldDom(caches);
        } catch (Exception e) {
            throw new ManagementException(e.getMessage(), e);
        }
    }
    

    //Method for converting New Dom into Old Dom for Passing to back to LoadConfig Method .. .. .. [Numan Hanif]
    private static com.alachisoft.tayzgrid.config.dom.CacheServerConfig[] convertToOldDom(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[] newCacheConfigsList) throws Exception {
        //Alachisoft.NCache.Config.Dom.CacheServerConfig[] oldCacheConfigsList = new CacheServerConfig[newCacheConfigsList.length];
        List<com.alachisoft.tayzgrid.config.dom.CacheServerConfig> oldCacheConfigsList = new ArrayList<com.alachisoft.tayzgrid.config.dom.CacheServerConfig>();
        for (int index = 0; index < newCacheConfigsList.length; index++)
        {
            try{
                oldCacheConfigsList.add(com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToOldDom(newCacheConfigsList[index]));
            }catch(Exception e){
                
            }
        }
        com.alachisoft.tayzgrid.config.dom.CacheServerConfig[] oldCacheConfigsArray = new CacheServerConfig[oldCacheConfigsList.size()];
        return oldCacheConfigsList.toArray(oldCacheConfigsArray);
    }

    /**
     * Loads and returns all cache configurations from the configuration file.
     */
    public static CacheConfig[] GetConfiguredCaches2() throws ManagementException, ConfigurationException, UnknownHostException {
        if (getFileName().length() == 0) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt.");
        }
        try {
            XmlConfigReader xcr = new XmlConfigReader("", "");
            java.util.Map propMap = null;

            propMap = xcr.GetProperties2(CacheConfigManager.getFileName());

            java.util.ArrayList configList = new java.util.ArrayList();

            Iterator ide = propMap.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry current = (Map.Entry) ide.next();
                java.util.Map properties = (java.util.Map) current.getValue();
                configList.add(CacheConfig.FromProperties2(properties));
            }

            CacheConfig[] configs = new CacheConfig[configList.size()];
            for (int i = 0; i < configList.size(); i++) {
                configs[i] = (CacheConfig) ((configList.get(i) instanceof CacheConfig) ? configList.get(i) : null);
            }

            return configs;
        } catch (ManagementException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ManagementException(e.getMessage(), e);
        }
    }
    /**
     * Save caches to configuration
     */
    public static void SaveConfiguration(java.util.HashMap caches, java.util.HashMap partitionedCaches) throws ManagementException, IllegalArgumentException, IllegalAccessException, FileNotFoundException, UnsupportedEncodingException, IOException, Exception {
        if (getFileName().length() == 0) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt.");
        }

        java.util.ArrayList<CacheServerConfig> configurations = new java.util.ArrayList<CacheServerConfig>();
        if (caches != null) {
            Iterator ide = caches.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry current = (Map.Entry) ide.next();
                try {
                    CacheInfo cacheInfo = (CacheInfo) current.getValue();
                    configurations.add(cacheInfo.getCacheProps());
                } catch (RuntimeException e) {
                }
            }
        }

        //Change for New Dom Convert Old Dom Config to New Dom Config before Saving it on File ncconf[Numan Hanif]
        SaveConfiguration(convertToNewDom(configurations).toArray(new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[0]));

    }

    //Method for converting Old Dom into New Dom for Saving it on the nconf File .. .. .. [Numan Hanif]
    private static java.util.ArrayList<com.alachisoft.tayzgrid.config.newdom.CacheServerConfig> convertToNewDom(java.util.ArrayList<CacheServerConfig> oldCacheConfigsList) throws Exception {
        java.util.ArrayList<com.alachisoft.tayzgrid.config.newdom.CacheServerConfig> newCacheConfigsList = new ArrayList<com.alachisoft.tayzgrid.config.newdom.CacheServerConfig>();

        Iterator itr = oldCacheConfigsList.listIterator();
        while (itr.hasNext()) {
            com.alachisoft.tayzgrid.config.dom.CacheServerConfig tempOldCacheConfig = (com.alachisoft.tayzgrid.config.dom.CacheServerConfig) itr.next();
            try{
                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig tempNewCacheConfig = com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToNewDom(tempOldCacheConfig);
                newCacheConfigsList.add(tempNewCacheConfig);
            }catch(Exception e){
                
            }
        }
        return newCacheConfigsList;


    }

    /**
     * Save the configuration
     *
     * @param configuration
     */
    public static void SaveConfiguration(Object[] configuration) throws IllegalArgumentException, IllegalAccessException, ManagementException, FileNotFoundException, Exception {
        StringBuilder xml = new StringBuilder();
        xml.append("<configuration>\r\n");
        if (configuration != null && configuration.length > 0) {
            ConfigurationBuilder builder = new ConfigurationBuilder(configuration);
            builder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class);
            xml.append(builder.GetXmlString());
        }
        xml.append("\r\n</configuration>");
        WriteXmlToFile(xml.toString());
    }

    /**
     * Write the xml configuration string to c
     *
     * @param xml
     */
    private static void WriteXmlToFile(String xml) throws ManagementException, FileNotFoundException, UnsupportedEncodingException, IOException {
        if (getFileName().length() == 0) {
            throw new ManagementException("Can not locate cache configuration file. Installation might be corrupt.");
        }
        OutputStream os = null;
        Writer writer = null;
        try {
            os = new FileOutputStream(getFileName());
            writer = new OutputStreamWriter(os);
            writer.write(xml);
            writer.flush();
        } catch (RuntimeException e) {
            throw new ManagementException(e.getMessage(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (RuntimeException e) {
                }
                writer = null;
            }
            if (os != null) {
                try {
                    os.close();
                } catch (RuntimeException e2) {
                }
                os = null;
            }
        }
    }
}
