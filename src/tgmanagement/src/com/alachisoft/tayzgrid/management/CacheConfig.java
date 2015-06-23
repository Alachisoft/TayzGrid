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


import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.config.ConfigReader;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.dom.ConfigConverter;
import com.alachisoft.tayzgrid.config.newdom.BatchConfig;
import com.alachisoft.tayzgrid.config.newdom.WriteBehind;
import com.alachisoft.tayzgrid.config.PropsConfigReader;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
/**
 * Inernal class used to contain cofiguration data.
 */
public class CacheConfig
{

    /**
     * Type of the cache. i.e replicated-server, partitioned-server, local-cache.
     */
    private String _cacheType = "";
    /**
     * ID of the cache.
     */
    private String _cacheId = "";
    /**
     * ID of the partition this cache belongs to.
     */
    private String _paritionId = "";
    /**
     * Flag that indicates if we are to use the cache as inproc.
     */
    private boolean _useInProc;
    /**
     * Server name of the machine hosting NCache service.
     */
    private String _serverName;
    /**
     * Use TCP channel for communication.
     */
    private boolean _useTcp = true;
    /**
     * TCP channel port.
     */
    private long _port;
    /**
     * Property string of the cache.
     */
    private String _propertyString = "";
    /**
     * Regisered DataSharing types.
     */
    private java.util.HashMap _dataSharingKnownTypes;
    /**
     * Regisered Backing Source.
     */
    private java.util.HashMap _backingSource;
    /**
     * Regisered compact types.
     */
    private java.util.HashMap _cmptKnownTypes;
    /**
     * Cluster port.
     */
    private int _clusterPort;
    /**
     * Cluster port.
     */
    private int _clusterPortRange;
    /**
     * Fatal and error logs.
     */
    private boolean _errorLogsEnabled;
    /**
     * info, debug and warning logs.
     */
    private boolean _detailedLogsEnabled;
    private long _cacheMaxSize;
    private long _cleanInterval;
    private float _evictRatio;
    
    /**
     * list of all the servers participating in a clustered cache.
     */
    private java.util.ArrayList _servers;

    private boolean _useHeartBeat = false;
    private java.util.HashMap _alertNotifications;
    private static final String NET_TYPE = "net";
    private static final String NET_TYPE_WITH_COLON = ":net";
    private static final String JAVA_TYPE = "java";
    private static final String JAVA_TYPE_WITH_COLON = ":java";
    private static String PLATFORM_TYPE = "";
    
    private int _managementPort;
    private int _socketPort;



    public final java.util.ArrayList getServers()
    {
        return _servers;
    }

    public final java.util.HashMap getAlertNotification()
    {
        return _alertNotifications;
    }

    public final long getCacheMaxSize()
    {
        return _cacheMaxSize;
    }

    public final long getCleanInterval()
    {
        return _cleanInterval;
    }

    public final float getEvictRatio()
    {
        return _evictRatio;
    }

    /**
     * @return the _managementPort
     */
    public int getManagementPort() {
        return _managementPort;
    }

    /**
     * @param _managementPort the _managementPort to set
     */
    public void setManagementPort(int _managementPort) {
        this._managementPort = _managementPort;
    }
    
     /**
     * @return the _socketPort
     */
    public int getClientPort() {
        return _socketPort;
    }

    /**
     * @param _socketPort the _socketPort to set
     */
    public void setClientPort(int _socketPort) {
        this._socketPort = _socketPort;
    }
    
    /**
     * Type of the cache. i.e replicated-server, partitioned-server, partitioned-replica-server, local-cache.
     */
    public final String getCacheType()
    {
        return _cacheType;
    }

    public final void setCacheType(String value)
    {
        _cacheType = value;
    }

    /**
     * ID of the cache.
     */
    public final String getCacheId()
    {
        return _cacheId;
    }

    public final void setCacheId(String value)
    {
        _cacheId = value;
    }

    /**
     * ID of the partition this cache belongs to.
     */
    public final String getPartitionId()
    {
        return _paritionId;
    }

    public final void setPartitionId(String value)
    {
        _paritionId = value;
    }

    /**
     * Flag that indicates if we are to use the cache as inproc.
     */
    public final boolean getUseInProc()
    {
        return _useInProc;
    }

    public final void setUseInProc(boolean value)
    {
        _useInProc = value;
    }

    /**
     * Server name of the machine hosting NCache service.
     */
    public final String getServerName()
    {
        return _serverName;
    }

    public final void setServerName(String value)
    {
        _serverName = value;
    }

    /**
     * Use TCP channel for communication.
     */
    public final boolean getUseTcp()
    {
        return _useTcp;
    }

    public final void setUseTcp(boolean value)
    {
        _useTcp = value;
    }

    /**
     * TCP channel port.
     */
    public final long getPort()
    {
        return _port;
    }

    public final void setPort(long value)
    {
        _port = value;
    }

    /**
     * Property string of the cache.
     */
    public final String getPropertyString()
    {
        return _propertyString;
    }

    public final void setPropertyString(String value)
    {
        _propertyString = value;
    }

    /**
     * Registered DataSharing known types.
     */
    public final java.util.HashMap getDataSharingKnownTypes()
    {
        return _dataSharingKnownTypes;
    }

    public final void setDataSharingKnownTypes(java.util.HashMap value)
    {
        _dataSharingKnownTypes = value;
    }

    /**
     * Registered compact known types.
     */
    public final java.util.HashMap getCompactKnownTypes()
    {
        return _cmptKnownTypes;
    }

    public final void setCompactKnownTypes(java.util.HashMap value)
    {
        _cmptKnownTypes = value;
    }

    /**
     * Registered Backing Source.
     */
    public final java.util.HashMap getBackingSource()
    {
        return _backingSource;
    }

    public final void setBackingSource(java.util.HashMap value)
    {
        _backingSource = value;
    }

    public final boolean getIsUdpCluster()
    {
        if (getPropertyString().indexOf("cluster") > 0)
        {
            if (getPropertyString().indexOf("udp") > 0)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Cluster port of the ndoe.
     */
    public final int getClusterPort()
    {
        return _clusterPort;
    }

    public final void setClusterPort(int value)
    {
        _clusterPort = value;
    }

    /**
     * Cluster port range.
     */
    public final int getClusterPortRange()
    {
        return _clusterPortRange;
    }

    public final void setClusterPortRange(int value)
    {
        _clusterPortRange = value;
    }

    public final boolean getIsErrorLogsEnabled()
    {
        return _errorLogsEnabled;
    }

    public final void setIsErrorLogsEnabled(boolean value)
    {
        _errorLogsEnabled = value;
    }

    public final boolean getIsDetailedLogsEnabled()
    {
        return _detailedLogsEnabled;
    }

    public final void setIsDetailedLogsEnabled(boolean value)
    {
        _detailedLogsEnabled = value;
    }

    public final boolean getUseHeartBeat()
    {
        return _useHeartBeat;
    }

    public final void setUseHeartBeat(boolean value)
    {
        _useHeartBeat = value;
    }
    private void InitializeServerName() throws UnknownHostException
    {
        this._serverName = java.net.InetAddress.getLocalHost().getHostName();
    }
    /**
     * Constructor
     */
    public CacheConfig() throws UnknownHostException
    {
        _port = CacheConfigManager.getTcpPort();
        this._clusterPortRange = 2;
        this._socketPort = 9610;
        this._managementPort = 8280; 
        this.InitializeServerName();
    }

    public CacheConfig(long tcpPort) throws UnknownHostException
    {
        _port = tcpPort;
        this.InitializeServerName();
    }

    /**
     * Populates the object from specified configuration object.
     *
     * @param configuration
     * @return
     */
    public static CacheConfig FromDom(CacheServerConfig config) throws ManagementException, UnknownHostException
    {
        java.util.HashMap props = ConfigConverter.ToHashMap(config);
        return FromProperties(props);

    }

    /**
     * Populates the object from specified configuration object.
     *
     * @param configuration
     * @return
     */
    public static CacheConfig FromConfiguration(CacheServerConfig configuration) throws UnknownHostException
    {
        CacheConfig cConfig = null;
        if (configuration != null)
        {
            cConfig = new CacheConfig();

            cConfig._useInProc = configuration.getInProc();
            cConfig._managementPort = configuration.getManagementPort();
            cConfig._socketPort = configuration.getClientPort();
            cConfig.setCacheId(configuration.getName());

           

            if (configuration.getCluster() != null)
            {
                if (configuration.getCluster().getChannel() != null)
                {
                    cConfig._clusterPort = configuration.getCluster().getChannel().getTcpPort();
                    cConfig._clusterPortRange = configuration.getCluster().getChannel().getPortRange();
                }
                cConfig._useHeartBeat = configuration.getCluster().getUseHeartbeat();
                cConfig._servers = FromHostListToServers(configuration.getCluster().getChannel().getInitialHosts());

                String topology = "";
                String tempVar = configuration.getCluster().getTopology();
                if (tempVar.equals("replicated"))
                {
                    topology = "replicated-server";
                }
                else if (tempVar.equals("partitioned"))
                {
                    topology = "partitioned-server";
                }
                cConfig._cacheType = topology;
            }
            else
            {
                cConfig._cacheType = "local-cache";
            }

            if (configuration.getCleanup() != null)
            {
                cConfig._cleanInterval = configuration.getCleanup().getInterval() * 1000; ///to millisec
            }

            if (configuration.getEvictionPolicy() != null)
            {
                cConfig._evictRatio = configuration.getEvictionPolicy().getEvictionRatio().floatValue();
            }

            if (configuration.getStorage() != null)
            {
                cConfig._cacheMaxSize = configuration.getStorage().getSize() * 1048576; ///from mb to bytes
            }

            if (configuration.getLog() != null)
            {
                cConfig._errorLogsEnabled = configuration.getLog().getTraceErrors();
                cConfig._detailedLogsEnabled = configuration.getLog().getTraceDebug();
            }

            if (configuration.getBackingSource() != null)
            {
                java.util.HashMap settings = new java.util.HashMap();
                settings.put("backing-source", GetBackingSource(configuration.getBackingSource()));
                cConfig._backingSource = settings;
            }

            if (configuration.getAlertsNotifications() != null)
            {
                cConfig._alertNotifications = GetAlerts(configuration.getAlertsNotifications());
            }

        }
        return cConfig;
    }

    private static java.util.HashMap GetAlerts(com.alachisoft.tayzgrid.config.newdom.AlertsNotifications alertsNotifications)
    {
        java.util.HashMap settings = new java.util.HashMap();
        if (alertsNotifications.getEMailNotifications() != null)
        {
            settings.put("email-notification", GetEmailNotifications(alertsNotifications.getEMailNotifications()));
        }
        if (alertsNotifications.getAlertsTypes() != null)
        {
            settings.put("alerts-types", GetAlertsType(alertsNotifications.getAlertsTypes()));
        }
        return settings;
    }

    private static java.util.HashMap GetEmailNotifications(com.alachisoft.tayzgrid.config.newdom.EmailNotifications emailNotifications)
    {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("email-notification-enabled", (new Boolean(emailNotifications.getEmailNotificationEnabled())).toString());
        settings.put("sender", emailNotifications.getSender());
        settings.put("smtp-server", emailNotifications.getSmtpServer());
        settings.put("smtp-port", (new Integer(emailNotifications.getSmtpPort())).toString());
        settings.put("ssl", (new Boolean(emailNotifications.getSSL())).toString());
        settings.put("authentication", (new Boolean(emailNotifications.getAuthentication())).toString());
        settings.put("sender-login", emailNotifications.getLogin());
        settings.put("sender-password", emailNotifications.getPassword());

        if (emailNotifications.getRecipients() != null)
        {
            settings.put("recipients", GetRecipients(emailNotifications.getRecipients()));
        }
        return settings;
    }

    private static java.util.HashMap GetRecipients(com.alachisoft.tayzgrid.config.newdom.NotificationRecipient[] recipients)
    {
        java.util.HashMap settings = new java.util.HashMap();

        for (int i = 0; i < recipients.length; i++)
        {
            settings.put(recipients[i].getID(), recipients[i].getID());
        }

        return settings;
    }

    private static java.util.HashMap GetBackingSource(com.alachisoft.tayzgrid.config.newdom.BackingSource backingSource)
    {
        java.util.HashMap settings = new java.util.HashMap();
        if (backingSource.getReadthru() != null)
        {
            settings.put("read-thru", GetReadThru(backingSource.getReadthru()));
        }
        if (backingSource.getWritethru() != null)
        {
            settings.put("write-thru", GetWriteThru(backingSource.getWritethru()));
        }
        return settings; 
    }

    private static java.util.HashMap GetWriteThru(com.alachisoft.tayzgrid.config.newdom.Writethru writethru)
    {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("enabled", (new Boolean(writethru.getEnabled())).toString());

        if (writethru.getProviders() != null)
        {
            settings.put("write-thru-providers", GetProviders(writethru.getProviders()));
        }
        if (writethru.getWriteBehind() != null)
        {
            settings.put("write-behind", GetWriteBehind(writethru.getWriteBehind()));
        }
        return settings;
    }

    private static java.util.HashMap GetReadThru(com.alachisoft.tayzgrid.config.newdom.Readthru readthru)
    {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("enabled", (new Boolean(readthru.getEnabled())).toString());
        

        if (readthru.getProviders() != null)
        {
            settings.put("read-thru-providers", GetProviders(readthru.getProviders()));
        }

        return settings;
    }
    private static java.util.HashMap GetWriteBehind(WriteBehind writeBehind) {
        java.util.HashMap settings = new java.util.HashMap();

        if (writeBehind != null) {
            settings.put("mode", writeBehind.getMode());
            settings.put("throttling-rate-per-sec", writeBehind.getThrottling());
            settings.put("failed-operations-queue-limit", writeBehind.getRequeueLimit());
            settings.put("failed-operations-eviction-ratio", writeBehind.getEviction());
            if (writeBehind.getBatchConfig() != null) {
                settings.put("batch-mode-config", GetBatchConfig(writeBehind.getBatchConfig()));
            }
        }

        return settings;
    }

    private static java.util.HashMap GetBatchConfig(BatchConfig batchConfig) {
        java.util.HashMap settings = new java.util.HashMap();
        if (batchConfig != null) {
            settings.put("batch-interval", batchConfig.getBatchInterval());
            settings.put("operation-delay", batchConfig.getOperationDelay());
        }
        return settings;
    }
    private static java.util.HashMap GetProviders(com.alachisoft.tayzgrid.config.newdom.Provider[] providers)
    {
        java.util.HashMap settings = new java.util.HashMap();

        if (providers != null && providers.length > 0)
        {
            for (int i = 0; i < providers.length; i++)
            {
                settings.put(providers[i].getProviderName(), GetProvider(providers[i]));
            }
        }

        return settings;
    }

    private static java.util.HashMap GetProvider(com.alachisoft.tayzgrid.config.newdom.Provider provider)
    {
        java.util.HashMap settings = new java.util.HashMap();

        if (provider != null)
        {
            settings.put("provider-name", provider.getProviderName());
            settings.put("assembly-name", provider.getAssemblyName());
            settings.put("class-name", provider.getClassName());
            settings.put("full-name", provider.getFullProviderName());
            settings.put("default-provider", (new Boolean(provider.getIsDefaultProvider())).toString());
            settings.put("loader-only", (new Boolean(provider.getIsLoaderOnly())).toString());
            java.util.HashMap paramss = GetParameters(provider.getParameters());
            if (paramss != null)
            {
                settings.put("parameters", paramss);
            }
        }

        return settings;
    }

    private static java.util.HashMap GetParameters(com.alachisoft.tayzgrid.config.newdom.Parameter[] parameters)
    {
        if (parameters == null)
        {
            return null;
        }

        java.util.HashMap settings = new java.util.HashMap();
        for (int i = 0; i < parameters.length; i++)
        {
            settings.put(parameters[i].getName(), parameters[i].getParamValue());
        }


        return settings;
    }

    /**
     * @param alertTypes
     * @return
     */
    private static java.util.HashMap GetAlertsType(com.alachisoft.tayzgrid.config.newdom.AlertsTypes alertTypes)
    {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("cache-stop", (new Boolean(alertTypes.getCacheStop())).toString());
        settings.put("cache-start", (new Boolean(alertTypes.getCacheStart())).toString());
        settings.put("node-left", (new Boolean(alertTypes.getNodeLeft())).toString());
        settings.put("node-joined", (new Boolean(alertTypes.getNodeJoined())).toString());
        settings.put("state-transfer-started", (new Boolean(alertTypes.getStartTransferStarted())).toString());
        settings.put("state-transfer-stop", (new Boolean(alertTypes.getStartTransferStop())).toString());
        settings.put("state-transfer-error", (new Boolean(alertTypes.getStartTransferError())).toString());
        settings.put("service-start-error", (new Boolean(alertTypes.getServiceStartError())).toString());
        settings.put("cache-size", (new Boolean(alertTypes.getCacheSize())).toString());
        settings.put("general-error", (new Boolean(alertTypes.getGeneralError())).toString());
        settings.put("licensing-error", (new Boolean(alertTypes.getLicensingError())).toString());
        settings.put("configuration-error", (new Boolean(alertTypes.getConfigurationError())).toString());
        settings.put("general-info", (new Boolean(alertTypes.getGeneralInfo())).toString());
        settings.put("unhandled-exceptions", (new Boolean(alertTypes.getUnHandledException())).toString());
        return settings;
    }

    private static java.util.HashMap GetCompactType(com.alachisoft.tayzgrid.config.newdom.Type type)
    {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("id", type.getID());
        settings.put("handle", type.getName());
        settings.put("portable", type.getPortable());
        if (type.getPortableClasses() != null)
        {
            settings.put("known-classes", GetCompactPortableClasses(type.getPortableClasses()));
            settings.put("attribute-union-list", GetCompactAttributeListUnion(type.getAttributeList()));
        }
        return settings;
    }

    private static java.util.HashMap GetCompactPortableClasses(com.alachisoft.tayzgrid.config.newdom.PortableClass[] classes)
    {
        java.util.HashMap settings = new java.util.HashMap();
        for (com.alachisoft.tayzgrid.config.newdom.PortableClass clas : classes)
        {
            settings.put(clas.getName(), GetCompactPortableClass(clas));
        }
        return settings;
    }

    private static java.util.HashMap GetCompactAttributeListUnion(com.alachisoft.tayzgrid.config.newdom.AttributeListUnion attributeList)
    {
        java.util.HashMap settings = new java.util.HashMap();
        if (attributeList != null && attributeList.getPortableAttributes() != null)
        {
            settings.put("attribute", GetCompactPortableAttributes(attributeList.getPortableAttributes()));
        }
        return settings;

    }

    private static java.util.HashMap GetCompactPortableClass(com.alachisoft.tayzgrid.config.newdom.PortableClass clas)
    {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("name", clas.getName());
        settings.put("handle-ID", clas.getID());
        settings.put("assembly", clas.getAssembly());
        settings.put("type", clas.getType());
        if (clas.getPortableAttributes() != null)
        {
            settings.put("attribute", GetCompactPortableAttributes(clas.getPortableAttributes()));
        }
        return settings;
    }

    private static java.util.HashMap GetCompactPortableAttributes(com.alachisoft.tayzgrid.config.newdom.PortableAttribute[] attributes)
    {
        java.util.HashMap settings = new java.util.HashMap();
        for (com.alachisoft.tayzgrid.config.newdom.PortableAttribute attrib : attributes)
        {
            settings.put(attrib.getName() + ":" + attrib.getType(), GetCompactPortableAttribute(attrib));
        }
        return settings;
    }

    private static java.util.HashMap GetCompactPortableAttribute(com.alachisoft.tayzgrid.config.newdom.PortableAttribute attrib)
    {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("name", attrib.getName());
        settings.put("type", attrib.getType());
        settings.put("order", attrib.getOrder());
        return settings;
    }

    /**
     * Populates the object from specified configuration string.
     *
     * @return
     */
    public static CacheConfig FromPropertyString(String props) throws ManagementException, UnknownHostException
    {
        CacheConfig cConfig = null;
        if (props != null)
        {
            PropsConfigReader pcr = new PropsConfigReader(props);
            java.util.Map cacheConfig = pcr.getProperties();
            cConfig = CacheConfig.FromProperties(cacheConfig);

        }
        return cConfig;
    }

    public static java.util.ArrayList GetConfigs(java.util.ArrayList props) throws ManagementException, UnknownHostException
    {
        java.util.ArrayList configList = new java.util.ArrayList();
        for (Iterator it = props.iterator(); it.hasNext();)
        {
            java.util.HashMap properties = (java.util.HashMap) it.next();
            CacheConfig config = null;
            config = FromProperties(properties);
            if (config != null)
            {
                configList.add(config);
            }
        }

        return configList;
    }

    public static java.util.ArrayList GetConfigs(java.util.ArrayList props, long tcpPort) throws Exception
    {
        java.util.ArrayList configList = new java.util.ArrayList();
        for (Iterator it = props.iterator(); it.hasNext();)
        {
            java.util.HashMap properties = (java.util.HashMap) it.next();
            CacheConfig config = null;
            config = FromProperties(properties, tcpPort);
            if (config != null)
            {
                configList.add(config);
            }
        }

        return configList;
    }

    private static java.util.Map ReplaceCacheId(java.util.Map properties, String oldCacheId, String newCacheId)
    {
        oldCacheId = oldCacheId.toLowerCase();
        newCacheId = newCacheId.toLowerCase();

        Object tempVar = ((java.util.HashMap) properties).clone();
        java.util.Map props = (java.util.Map) ((tempVar instanceof java.util.Map) ? tempVar : null);
        Iterator ide = properties.entrySet().iterator();
        while (ide.hasNext())
        {
            Map.Entry current = (Map.Entry) ide.next();
            if (((String) current.getKey()).toLowerCase().equals(oldCacheId))
            {
                if (current.getValue() instanceof java.util.Map)
                {
                    props.remove(oldCacheId);
                    props.put(newCacheId, ReplaceCacheId((java.util.Map) ((current.getValue() instanceof java.util.Map) ? current.getValue() : null), oldCacheId, newCacheId));
                    ;
                }
                else
                {
                    props.remove(oldCacheId);
                    props.put(newCacheId, current.getValue());
                }
            }
            else if (current.getValue() instanceof java.util.Map)
            {
                props.put(current.getKey(), ReplaceCacheId((java.util.Map) ((current.getValue() instanceof java.util.Map) ? current.getValue() : null), oldCacheId, newCacheId));
            }
            else
            {
                if (((String) current.getValue()).toLowerCase().equals(oldCacheId))
                {
                    props.remove(current.getKey());
                    props.put(current.getKey(), newCacheId);
                }
            }
        }
        return props;
    }

    public static CacheConfig GetUpdatedConfig(java.util.Map properties, String partId, String joiningNode, tangible.RefObject<java.util.ArrayList> affectedNodes, tangible.RefObject<java.util.ArrayList> affectedPartitions, String oldCacheId, String newCacheId) throws ManagementException, UnknownHostException
    {

        String list = "";
        int clusterPort = 0;

        if (affectedNodes.argvalue == null)
        {
            affectedNodes.argvalue = new java.util.ArrayList();
        }

        properties = ReplaceCacheId(properties, oldCacheId, newCacheId);

        java.util.Map cacheProps = (java.util.Map) ((properties.get("cache") instanceof java.util.Map) ? properties.get("cache") : null);

        if (cacheProps.containsKey("cache-classes"))
        {
            java.util.Map cacheClassesProps = (java.util.Map) ((cacheProps.get("cache-classes") instanceof java.util.Map) ? cacheProps.get("cache-classes") : null);

            String cacheName = String.valueOf(cacheProps.get("name"));
            cacheName = cacheName.toLowerCase();

            if (cacheClassesProps.containsKey(cacheName))
            {
                java.util.Map topologyProps = (java.util.Map) ((cacheClassesProps.get(cacheName) instanceof java.util.Map) ? cacheClassesProps.get(cacheName) : null);

                if (topologyProps.containsKey("cluster"))
                {
                    java.util.Map clusterProps = (java.util.Map) ((topologyProps.get("cluster") instanceof java.util.Map) ? topologyProps.get("cluster") : null);

                    if (clusterProps.containsKey("channel"))
                    {
                        java.util.Map channelProps = (java.util.Map) ((clusterProps.get("channel") instanceof java.util.Map) ? clusterProps.get("channel") : null);

                        if (channelProps.containsKey("tcp"))
                        {
                            java.util.Map tcpProps = (java.util.Map) ((channelProps.get("tcp") instanceof java.util.Map) ? channelProps.get("tcp") : null);

                            if (tcpProps.containsKey("start_port"))
                            {
                                clusterPort = (Integer) (tcpProps.get("start_port"));
                            }
                        }

                        if (channelProps.containsKey("tcpping"))
                        {
                            java.util.Map tcppingProps = (java.util.Map) ((channelProps.get("tcpping") instanceof java.util.Map) ? channelProps.get("tcpping") : null);

                            if (tcppingProps.containsKey("initial_hosts"))
                            {
                                list = String.valueOf(tcppingProps.get("initial_hosts")).toLowerCase();

                                String[] nodes = list.split("[,]", -1);
                                for (String node : nodes)
                                {
                                    String[] nodename = node.split("[[]", -1);
                                    affectedNodes.argvalue.add(nodename[0]);
                                }

                                if (list.indexOf(joiningNode) == -1)
                                {
                                    list = list + "," + joiningNode + "[" + clusterPort + "]";
                                    tcppingProps.put("initial_hosts", list);
                                }
                            }
                        }

                        if (channelProps.containsKey("partitions"))
                        {
                            if (partId != null && !partId.equals(""))
                            {
                                java.util.HashMap partitionsProps = (java.util.HashMap) ((channelProps.get("partitions") instanceof java.util.HashMap) ? channelProps.get("partitions") : null);
                                if (partitionsProps != null)
                                {
                                    if (partitionsProps.containsKey(partId.toLowerCase()))
                                    {
                                        String nodesList = String.valueOf(partitionsProps.get(partId.toLowerCase())).toLowerCase();
                                        if (nodesList.indexOf(joiningNode) == -1)
                                        {
                                            nodesList = nodesList + ", " + joiningNode;
                                            partitionsProps.put(partId.toLowerCase(), nodesList);
                                        }
                                    }
                                    for (Iterator it = partitionsProps.keySet().iterator(); it.hasNext();)
                                    {
                                        String part = (String) it.next();
                                        if (!affectedPartitions.argvalue.contains(part))
                                        {
                                            affectedPartitions.argvalue.add(part);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //send the updated properties...
        return FromProperties(properties);
    }

    public static CacheConfig GetUpdatedConfig(java.util.Map properties, String partId, String newNode, tangible.RefObject<java.util.ArrayList> affectedNodes, tangible.RefObject<java.util.ArrayList> affectedPartitions, boolean isJoining) throws ManagementException, UnknownHostException
    {

        String list = "";
        int clusterPort = 0;

        if (affectedNodes.argvalue == null)
        {
            affectedNodes.argvalue = new java.util.ArrayList();
        }

        java.util.Map cacheProps = (java.util.Map) ((properties.get("cache") instanceof java.util.Map) ? properties.get("cache") : null);

        if (cacheProps.containsKey("cache-classes"))
        {
            java.util.Map cacheClassesProps = (java.util.Map) ((cacheProps.get("cache-classes") instanceof java.util.Map) ? cacheProps.get("cache-classes") : null);

            String cacheName = String.valueOf(cacheProps.get("name"));
            cacheName = cacheName.toLowerCase();

            if (cacheClassesProps.containsKey(cacheName))
            {
                java.util.Map topologyProps = (java.util.Map) ((cacheClassesProps.get(cacheName) instanceof java.util.Map) ? cacheClassesProps.get(cacheName) : null);

                if (topologyProps.containsKey("cluster"))
                {
                    java.util.Map clusterProps = (java.util.Map) ((topologyProps.get("cluster") instanceof java.util.Map) ? topologyProps.get("cluster") : null);

                    if (clusterProps.containsKey("channel"))
                    {
                        java.util.Map channelProps = (java.util.Map) ((clusterProps.get("channel") instanceof java.util.Map) ? clusterProps.get("channel") : null);

                        if (channelProps.containsKey("tcp"))
                        {
                            java.util.Map tcpProps = (java.util.Map) ((channelProps.get("tcp") instanceof java.util.Map) ? channelProps.get("tcp") : null);

                            if (tcpProps.containsKey("start_port"))
                            {
                                clusterPort = (Integer) (tcpProps.get("start_port"));
                            }
                        }

                        if (channelProps.containsKey("tcpping"))
                        {
                            java.util.Map tcppingProps = (java.util.Map) ((channelProps.get("tcpping") instanceof java.util.Map) ? channelProps.get("tcpping") : null);

                            if (tcppingProps.containsKey("initial_hosts"))
                            {
                                list = String.valueOf(tcppingProps.get("initial_hosts")).toLowerCase();

                                String[] nodes = list.split("[,]", -1);

                                if (isJoining)
                                {
                                    for (String node : nodes)
                                    {
                                        String[] nodename = node.split("[[]", -1);
                                        affectedNodes.argvalue.add(nodename[0]);
                                    }

                                    if (list.indexOf(newNode) == -1)
                                    {
                                        list = list + "," + newNode + "[" + clusterPort + "]";
                                        tcppingProps.put("initial_hosts", list);
                                    }
                                }
                                else
                                {
                                    for (String node : nodes)
                                    {
                                        String[] nodename = node.split("[[]", -1);
                                        if (!newNode.equals(nodename[0]))
                                        {
                                            affectedNodes.argvalue.add(nodename[0]);
                                        }
                                    }

                                    list = "";
                                    for (Iterator it = affectedNodes.argvalue.iterator(); it.hasNext();)
                                    {
                                        String node = (String) it.next();
                                        if (list.length() == 0)
                                        {
                                            list = node + "[" + clusterPort + "]";
                                        }
                                        else
                                        {
                                            list = list + "," + node + "[" + clusterPort + "]";
                                        }
                                    }
                                    tcppingProps.put("initial_hosts", list);
                                }
                            }
                        }

                        if (channelProps.containsKey("partitions"))
                        {
                            if (partId != null && !partId.equals(""))
                            {
                                java.util.HashMap partitionsProps = (java.util.HashMap) ((channelProps.get("partitions") instanceof java.util.HashMap) ? channelProps.get("partitions") : null);
                                if (partitionsProps != null)
                                {
                                    if (partitionsProps.containsKey(partId.toLowerCase()))
                                    {
                                        String nodesList = String.valueOf(partitionsProps.get(partId.toLowerCase())).toLowerCase();
                                        if (nodesList.indexOf(newNode) == -1)
                                        {
                                            nodesList = nodesList + ", " + newNode;
                                            partitionsProps.put(partId.toLowerCase(), nodesList);
                                        }
                                    }
                                    for (Iterator it = partitionsProps.keySet().iterator(); it.hasNext();)
                                    {
                                        String part = (String) it.next();
                                        if (!affectedPartitions.argvalue.contains(part))
                                        {
                                            affectedPartitions.argvalue.add(part);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //send the updated properties...
        return FromProperties(properties);
    }

    public static CacheConfig GetUpdatedConfig2(java.util.Map properties, String partId, String joiningNode, tangible.RefObject<java.util.ArrayList> affectedNodes, tangible.RefObject<java.util.ArrayList> affectedPartitions) throws ManagementException, UnknownHostException
    {
        if (properties.size() == 1)
        {
            Iterator ide = properties.entrySet().iterator();
            while (ide.hasNext())
            {
                Map.Entry current = (Map.Entry) ide.next();
                if (current.getValue() instanceof java.util.Map)
                {
                    properties = (java.util.Map) ((current.getValue() instanceof java.util.Map) ? current.getValue() : null);
                }
                break;
            }
        }

        String list = "";
        int clusterPort = 0;

        if (affectedNodes.argvalue == null)
        {
            affectedNodes.argvalue = new java.util.ArrayList();
        }

        if (properties.containsKey("cluster"))
        {
            java.util.Map clusterProps = (java.util.Map) ((properties.get("cluster") instanceof java.util.Map) ? properties.get("cluster") : null);
            if (clusterProps.containsKey("channel"))
            {
                java.util.Map channelProps = (java.util.Map) ((clusterProps.get("channel") instanceof java.util.Map) ? clusterProps.get("channel") : null);
                if (channelProps.containsKey("tcp-port"))
                {
                    clusterPort = (Integer) (channelProps.get("tcp-port"));
                }

                if (channelProps.containsKey("initial-hosts"))
                {
                    list = String.valueOf(channelProps.get("initial-hosts")).toLowerCase();

                    String[] nodes = list.split("[,]", -1);
                    for (String node : nodes)
                    {
                        String[] nodename = node.split("[[]", -1);
                        affectedNodes.argvalue.add(nodename[0]);
                    }

                    if (list.indexOf(joiningNode) == -1)
                    {
                        list = list + "," + joiningNode + "[" + clusterPort + "]";
                        channelProps.put("initial-hosts", list);
                        channelProps.put("num-initial-hosts", "2");
                    }
                }
            }
        }

        //send the updated properties...
        return FromProperties2(properties);
    }

    /**
     * Populates the object from specified configuration.
     *
     * @return
     */
    public static CacheConfig FromProperties(java.util.Map properties, long tcpPort) throws Exception
    {
        CacheConfig data = new CacheConfig(tcpPort);

        if (properties.containsKey("partitionid"))
        {
            data.setPartitionId(properties.get("partitionid").toString().toLowerCase());
        }

        java.util.Map webprops = (java.util.Map) ((properties.get("web-cache") instanceof java.util.Map) ? properties.get("web-cache") : null);
        java.util.Map cacheprops = (java.util.Map) ((properties.get("cache") instanceof java.util.Map) ? properties.get("cache") : null);

        if (properties == null)
        {
            throw new ManagementException("Invalid configuration; missing 'web-cache' element.");
        }

        try
        {
            // Get start_port (ClusterPort) and port_range (ClusterPortRange) from the config file
            if (cacheprops.containsKey("cache-classes"))
            {
                java.util.Map cacheClassesProps = (java.util.Map) ((cacheprops.get("cache-classes") instanceof java.util.Map) ? cacheprops.get("cache-classes") : null);
                String cacheName = String.valueOf(cacheprops.get("name"));
                cacheName = cacheName.toLowerCase();
                if (cacheClassesProps.containsKey(cacheName))
                {
                    java.util.Map topologyProps = (java.util.Map) ((cacheClassesProps.get(cacheName) instanceof java.util.Map) ? cacheClassesProps.get(cacheName) : null);
                    if (topologyProps.containsKey("cluster"))
                    {
                        java.util.Map clusterProps = (java.util.Map) ((topologyProps.get("cluster") instanceof java.util.Map) ? topologyProps.get("cluster") : null);
                        if (clusterProps.containsKey("channel"))
                        {
                            java.util.Map channelProps = (java.util.Map) ((clusterProps.get("channel") instanceof java.util.Map) ? clusterProps.get("channel") : null);
                            if (channelProps.containsKey("tcp"))
                            {
                                java.util.Map tcpProps = (java.util.Map) ((channelProps.get("tcp") instanceof java.util.Map) ? channelProps.get("tcp") : null);
                                if (tcpProps.containsKey("start_port"))
                                {
                                    data.setClusterPort((Integer) (tcpProps.get("start_port")));
                                }
                                if (tcpProps.containsKey("port_range"))
                                {
                                    data.setClusterPortRange((Integer) (tcpProps.get("port_range")));
                                }
                                else
                                {
                                    data.setClusterPortRange(1);
                                }
                            }
                        }
                    }

                    if (topologyProps.containsKey("type"))
                    {
                        data._cacheType = String.valueOf(topologyProps.get("type"));
                    }
                }
            }

            // Get Error and Detailed logs enable status from the config file
            if (cacheprops.containsKey("log"))
            {
                java.util.Map cacheLogProps = (java.util.Map) ((cacheprops.get("log") instanceof java.util.Map) ? cacheprops.get("log") : null);
                if (cacheLogProps.containsKey("enabled"))
                {
                    boolean logsEnabled = (Boolean) (cacheLogProps.get("enabled"));
                    if (logsEnabled)
                    {
                        if (cacheLogProps.containsKey("trace-errors"))
                        {
                            data.setIsErrorLogsEnabled((Boolean) (cacheLogProps.get("trace-errors")));
                        }
                        if (cacheLogProps.containsKey("trace-debug"))
                        {
                            data.setIsDetailedLogsEnabled((Boolean) (cacheLogProps.get("trace-debug")));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }

        data.setCacheId(String.valueOf(webprops.get("cache-id")));
        if (data.getCacheId() == null || data.getCacheId().length() == 0)
        {
            throw new ManagementException("'cache-id' not specified in configuration.");
        }

        if (webprops.containsKey("channel"))
        {
            String channel = String.valueOf(webprops.get("channel"));
            channel = channel.toLowerCase();
            if (channel.compareTo("http") == 0)
            {
                data.setUseTcp(false);
            }
        }

        if (webprops.containsKey("shared"))
        {
            data.setUseInProc(!(Boolean) (webprops.get("shared")));
        }

        if (webprops.containsKey("port"))
        {
            data.setPort((Integer) (webprops.get("port")));
        }

        if (webprops.containsKey("server"))
        {
            data.setServerName(String.valueOf(webprops.get("server")));
        }

        properties.remove("id");
        properties.remove("type");
        data.setPropertyString(ConfigReader.ToPropertiesString(properties));

        if (properties.containsKey("data-sharing"))
        {
            data.setDataSharingKnownTypes((java.util.HashMap) properties.get("data-sharing"));
        }

        if (properties.containsKey("compact-serialization"))
        {
            data.setCompactKnownTypes((java.util.HashMap) properties.get("compact-serialization"));
        }

        return data;
    }

    /**
     * Populates the object from specified configuration.
     *
     * @return
     */
    public static CacheConfig FromProperties(java.util.Map properties) throws ManagementException,UnknownHostException
    {
        CacheConfig data = new CacheConfig();

        if (properties.containsKey("partitionid"))
        {
            data.setPartitionId(properties.get("partitionid").toString().toLowerCase());
        }

        java.util.Map webprops = (java.util.Map) ((properties.get("web-cache") instanceof java.util.Map) ? properties.get("web-cache") : null);
        java.util.Map cacheprops = (java.util.Map) ((properties.get("cache") instanceof java.util.Map) ? properties.get("cache") : null);

        if (properties == null)
        {
            throw new ManagementException("Invalid configuration; missing 'web-cache' element.");
        }
        if (cacheprops != null)
        {
            if (!(cacheprops.containsKey("class") && cacheprops.containsKey("name")))
            {
                if (properties.containsKey("id"))
                {
                    cacheprops.put("name", properties.get("id").toString());
                    cacheprops.put("class", properties.get("id").toString());
                }
            }
        }

        try
        {

            // Get start_port (ClusterPort) and port_range (ClusterPortRange) from the config file
            if (cacheprops.containsKey("cache-classes"))
            {
                java.util.Map cacheClassesProps = (java.util.Map) ((cacheprops.get("cache-classes") instanceof java.util.Map) ? cacheprops.get("cache-classes") : null);
                String cacheName = String.valueOf(cacheprops.get("name"));
                cacheName = cacheName.toLowerCase();
                if (cacheClassesProps.containsKey(cacheName))
                {
                    java.util.Map topologyProps = (java.util.Map) ((cacheClassesProps.get(cacheName) instanceof java.util.Map) ? cacheClassesProps.get(cacheName) : null);
                    if (topologyProps.containsKey("cluster"))
                    {
                        java.util.Map clusterProps = (java.util.Map) ((topologyProps.get("cluster") instanceof java.util.Map) ? topologyProps.get("cluster") : null);
                        if (clusterProps.containsKey("channel"))
                        {
                            java.util.Map channelProps = (java.util.Map) ((clusterProps.get("channel") instanceof java.util.Map) ? clusterProps.get("channel") : null);
                            if (channelProps.containsKey("tcp"))
                            {
                                java.util.Map tcpProps = (java.util.Map) ((channelProps.get("tcp") instanceof java.util.Map) ? channelProps.get("tcp") : null);
                                if (tcpProps.containsKey("start_port"))
                                {
                                    data.setClusterPort((Integer) (tcpProps.get("start_port")));
                                }
                                if (tcpProps.containsKey("use_heart_beat"))
                                {
                                    data.setUseHeartBeat((Boolean) (tcpProps.get("use_heart_beat")));
                                }
                                if (tcpProps.containsKey("port_range"))
                                {
                                    data.setClusterPortRange((Integer) (tcpProps.get("port_range")));
                                }
                                else
                                {
                                    data.setClusterPortRange(1);
                                }
                            }
                            if (channelProps.containsKey("tcpping"))
                            {
                                java.util.Map tcppingProps = (java.util.Map) ((channelProps.get("tcpping") instanceof java.util.Map) ? channelProps.get("tcpping") : null);
                                if (tcppingProps.containsKey("initial_hosts"))
                                {
                                    String hostString = (String) tcppingProps.get("initial_hosts");
                                    data._servers = FromHostListToServers(hostString);
                                }
                            }
                        }
                    }

                    if (topologyProps.containsKey("type"))
                    {
                        data._cacheType = String.valueOf(topologyProps.get("type"));
                    }

                    if (topologyProps.containsKey("clean-interval"))
                    {
                        data._cleanInterval = (Long) (topologyProps.get("clean-interval")) * 1000; //convert to ms
                    }

                    if (topologyProps.containsKey("scavenging-policy"))
                    {
                        java.util.Map scavengingProps = (java.util.Map) ((topologyProps.get("scavenging-policy") instanceof java.util.Map) ? topologyProps.get("scavenging-policy") : null);
                        if (scavengingProps.containsKey("evict-ratio"))
                        {
                            data._evictRatio = (Float) (scavengingProps.get("evict-ratio"));
                        }
                    }

                    //We need to extract storage information from the properties
                    //because user can now change the cache size at runtime.
                    //hot apply configuration option for cache size.
                    if (topologyProps.containsKey("storage"))
                    {
                        java.util.Map storageProps = (java.util.Map) ((topologyProps.get("storage") instanceof java.util.Map) ? topologyProps.get("storage") : null);
                        if (storageProps.containsKey("class"))
                        {
                            String storageClass = (String) ((storageProps.get("class") instanceof String) ? storageProps.get("class") : null);
                            java.util.Map storageProviderProps = (java.util.Map) ((storageProps.get(storageClass) instanceof java.util.Map) ? storageProps.get(storageClass) : null);
                            if (storageProviderProps.containsKey("max-size"))
                            {
                                data._cacheMaxSize = (Long) (storageProviderProps.get("max-size")) * 1024 * 1024; //from MBs to bytes
                            }
                        }
                    }
                    else if (topologyProps.containsKey("internal-cache"))
                    {
                        java.util.Map internalProps = (java.util.Map) ((topologyProps.get("internal-cache") instanceof java.util.Map) ? topologyProps.get("internal-cache") : null);

                        if (internalProps.containsKey("clean-interval"))
                        {
                            data._cleanInterval = (Long) (internalProps.get("clean-interval")) * 1000; //convert to ms
                        }

                        if (internalProps.containsKey("scavenging-policy"))
                        {
                            java.util.Map scavengingProps = (java.util.Map) ((internalProps.get("scavenging-policy") instanceof java.util.Map) ? internalProps.get("scavenging-policy") : null);
                            if (scavengingProps.containsKey("evict-ratio"))
                            {
                                data._evictRatio = (Float) (scavengingProps.get("evict-ratio"));
                            }
                        }

                        if (internalProps.containsKey("storage"))
                        {
                            java.util.Map storageProps = (java.util.Map) ((internalProps.get("storage") instanceof java.util.Map) ? internalProps.get("storage") : null);
                            if (storageProps.containsKey("class"))
                            {
                                String storageClass = (String) ((storageProps.get("class") instanceof String) ? storageProps.get("class") : null);
                                java.util.Map storageProviderProps = (java.util.Map) ((storageProps.get(storageClass) instanceof java.util.Map) ? storageProps.get(storageClass) : null);
                                if (storageProviderProps.containsKey("max-size"))
                                {
                                    data._cacheMaxSize = (Long) (storageProviderProps.get("max-size")) * 1024 * 1024; //from MBs to bytes
                                }
                            }
                        }
                    }
                }
            }

            // Get Error and Detailed logs enable status from the config file
            if (cacheprops.containsKey("log"))
            {
                java.util.Map cacheLogProps = (java.util.Map) ((cacheprops.get("log") instanceof java.util.Map) ? cacheprops.get("log") : null);
                if (cacheLogProps.containsKey("enabled"))
                {
                    boolean logsEnabled = (Boolean) (cacheLogProps.get("enabled"));
                    if (logsEnabled)
                    {
                        if (cacheLogProps.containsKey("trace-errors"))
                        {
                            data.setIsErrorLogsEnabled((Boolean) (cacheLogProps.get("trace-errors")));
                        }
                        if (cacheLogProps.containsKey("trace-debug"))
                        {
                            data.setIsDetailedLogsEnabled((Boolean) (cacheLogProps.get("trace-debug")));
                        }
                    }
                }
            }

        }
        catch (Exception e)
        {
        }

        data.setCacheId(String.valueOf(webprops.get("cache-id")));
        if (data.getCacheId() == null || data.getCacheId().length() == 0)
        {
            throw new ManagementException("'cache-id' not specified in configuration.");
        }

        if (webprops.containsKey("channel"))
        {
            String channel = String.valueOf(webprops.get("channel"));
            channel = channel.toLowerCase();
            if (channel.compareTo("http") == 0)
            {
                data.setUseTcp(false);
            }
        }

        if (webprops.containsKey("shared"))
        {
            data.setUseInProc(!(Boolean.parseBoolean(webprops.get("shared").toString())));
        }

        if (webprops.containsKey("port"))
        {
            data.setPort((Integer) (webprops.get("port")));
        }
        else
        {
            data.setPort(data.getUseTcp() ? CacheConfigManager.getTcpPort() : CacheConfigManager.getHttpPort());
        }

        if (webprops.containsKey("server"))
        {
            data.setServerName(String.valueOf(webprops.get("server")));
        }

        properties.remove("id");
        properties.remove("type");
        data.setPropertyString(ConfigReader.ToPropertiesString(properties));

        if (properties.containsKey("data-sharing"))
        {
            data.setDataSharingKnownTypes((java.util.HashMap) properties.get("data-sharing"));
        }

        if (properties.containsKey("compact-serialization"))
        {
            data.setCompactKnownTypes((java.util.HashMap) properties.get("compact-serialization"));
        }

        return data;
    }

    private static java.util.ArrayList FromHostListToServers(String hostString)
    {
        java.util.ArrayList servers = new java.util.ArrayList();

        if (hostString.indexOf(',') != -1)
        {
            String[] hosts = hostString.split("[,]", -1);
            if (hosts != null)
            {
                for (int i = 0; i < hosts.length; i++)
                {
                    hosts[i] = hosts[i].trim();
                    servers.add(hosts[i].substring(0, hosts[i].indexOf('[')));
                }
            }
        }
        else
        {
            servers.add(hostString.trim().substring(0, hostString.indexOf('[')));
        }
        return servers;
    }

    /**
     * Populates the object from specified configuration.
     *
     * @return
     */
    public static CacheConfig FromProperties2(java.util.Map properties) throws ManagementException,UnknownHostException
    {
        CacheConfig data = new CacheConfig();

        try
        {
            if (properties.containsKey("name"))
            {
                data._cacheId = (String) ((properties.get("name") instanceof String) ? properties.get("name") : null);
            }
            else
            {
                throw new ManagementException("'name' not specified in configuration.");
            }

            if (properties.containsKey("log"))
            {
                java.util.Map cacheLogProps = (java.util.Map) ((properties.get("log") instanceof java.util.Map) ? properties.get("log") : null);
                if (cacheLogProps.containsKey("enabled"))
                {
                    boolean logsEnabled = (Boolean) (cacheLogProps.get("enabled"));
                    if (logsEnabled)
                    {
                        if (cacheLogProps.containsKey("trace-errors"))
                        {
                            data.setIsErrorLogsEnabled((Boolean) (cacheLogProps.get("trace-errors")));
                        }
                        if (cacheLogProps.containsKey("trace-debug"))
                        {
                            data.setIsDetailedLogsEnabled((Boolean) (cacheLogProps.get("trace-debug")));
                        }
                    }
                }
            }

            // Get start_port (ClusterPort) and port_range (ClusterPortRange) from the config file
            if (properties.containsKey("cluster"))
            {
                java.util.Map clusterProps = (java.util.Map) ((properties.get("cluster") instanceof java.util.Map) ? properties.get("cluster") : null);
                if (clusterProps.containsKey("channel"))
                {
                    java.util.Map channelProps = (java.util.Map) ((clusterProps.get("channel") instanceof java.util.Map) ? clusterProps.get("channel") : null);
                    if (channelProps.containsKey("tcp-port"))
                    {
                        data.setClusterPort((Integer) (channelProps.get("tcp-port")));
                    }
                    data.setClusterPortRange(1);
                }
            }
        }
        catch (RuntimeException e)
        {
        }

        data.setUseInProc((Boolean) (properties.get("inproc")));
        data.setManagementPort((Integer)(properties.get("management-port")));
        data.setClientPort((Integer)(properties.get("client-port")));
        data.setPropertyString(ConfigReader.ToPropertiesString(properties));

        return data;
    }

   

    
}
