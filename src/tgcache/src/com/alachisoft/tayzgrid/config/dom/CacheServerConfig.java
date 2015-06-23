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

package com.alachisoft.tayzgrid.config.dom;



import com.alachisoft.tayzgrid.common.enums.ClientNodeStatus;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.common.enums.CacheStatus;
import com.alachisoft.tayzgrid.common.StatusInfo;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;

import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.common.enums.CacheStatus;
import com.alachisoft.tayzgrid.common.enums.ClientNodeStatus;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;

import com.alachisoft.tayzgrid.config.newdom.TaskConfiguration;

import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import javax.cache.configuration.Factory;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheWriter;

@ConfigurationAttributeAnnotation(value = "cache-config", appendText = "")
@ConfigurationRootAnnotation(value = "cache-config")
public class CacheServerConfig implements Cloneable, InternalCompactSerializable
{

    private boolean cacheIsRunning = false;
    private boolean cacheIsRegistered = false;
    private boolean licenseIsExpired = false;
    private boolean autoStartCacheOnServiceStartup = false;
    private String name;
    private boolean inproc;
    private double configID;
    private String lastModified;
    private int managementPort = 0;
    private int socketPort = 0;
    
    /**
     * This helps to differentiate between a local-cache, client-cache and clustered-cache
     */
    private String cacheType;
    private com.alachisoft.tayzgrid.config.newdom.Log log;
    private com.alachisoft.tayzgrid.config.newdom.PerfCounters perfCounters;

    private com.alachisoft.tayzgrid.config.newdom.QueryIndex indexes;
    private com.alachisoft.tayzgrid.config.newdom.BackingSource backingSource;
    private com.alachisoft.tayzgrid.config.newdom.CacheLoader cacheloader;
    private com.alachisoft.tayzgrid.config.newdom.Notifications notifications;
    private com.alachisoft.tayzgrid.config.newdom.Cleanup cleanup;
    private com.alachisoft.tayzgrid.config.newdom.Storage storage;
    private com.alachisoft.tayzgrid.config.newdom.EvictionPolicy evictionPolicy;
    private Cluster cluster;
    private com.alachisoft.tayzgrid.config.newdom.AutoLoadBalancing autoBalancing;
    private com.alachisoft.tayzgrid.config.newdom.ClientNodes clientNodes;
    private com.alachisoft.tayzgrid.config.newdom.AlertsNotifications _alertsNotifications;
    private com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig _sqlDependencyConfig;
    private com.alachisoft.tayzgrid.common.enums.RtContextValue _runtimeContextValue;

    private com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy _expirationPolicy;


    private DataFormat _dataformat=DataFormat.Binary;

    private Factory<CacheLoader> cacheLoaderFactory;
    private Factory<CacheWriter> cacheWriterFactory;
    private boolean isLoaderOnly = false;

    private com.alachisoft.tayzgrid.config.newdom.TaskConfiguration _taskConfiguration;



    public CacheServerConfig()
    {
        log = new com.alachisoft.tayzgrid.config.newdom.Log();
        notifications = new com.alachisoft.tayzgrid.config.newdom.Notifications();
        _taskConfiguration = new com.alachisoft.tayzgrid.config.newdom.TaskConfiguration();
        _runtimeContextValue = RtContextValue.JVCACHE;
    }
    
    public final RtContextValue getRuntimeContext()
    {
        return _runtimeContextValue;
    }
    
    public final void setRuntimeContext(RtContextValue contextValue)
    {        
        _runtimeContextValue = contextValue;
    }

    public final boolean getIsRegistered()
    {
        return cacheIsRegistered;
    }

    public final void setIsRegistered(boolean value)
    {
        cacheIsRegistered = value;
    }

    public final boolean getIsRunning()
    {
        boolean isRunning = cacheIsRunning;

        if (this.getCacheType().equals("clustered-cache"))
        {
            for (StatusInfo cacheStatus : cluster.getNodes().values())
            {
                if (cacheStatus.Status == CacheStatus.Running)
                {
                    isRunning = true;
                    break;
                }
            }
        }

        return isRunning;
    }

    public final void setIsRunning(boolean value)
    {
        if (this.getCacheType().equals("local-cache") || this.getCacheType().equals("client-cache"))
        {
            cacheIsRunning = value;
        }
    }

    public final boolean getIsExpired()
    {
        return licenseIsExpired;
    }

    public final void setIsExpired(boolean value)
    {
        licenseIsExpired = value;
    }
    
        @ConfigurationAttributeAnnotation(value = "data-format", appendText = "")
    public final String getDataFormat()
    {
         if(_dataformat==DataFormat.Binary)
             return "Binary";
         else if (_dataformat==DataFormat.Object)
             return "Object";
         return "";
    }
    
    @ConfigurationAttributeAnnotation(value = "data-format", appendText = "")
    public final void setDataFormat(String value)
    {
        
        if(value.equalsIgnoreCase("Binary")){
            _dataformat = DataFormat.Binary;
        }else if(value.equalsIgnoreCase("Object")){
            _dataformat = DataFormat.Object;
        }
        else
            _dataformat = null;
    }
    
    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public final String getName()
    {
        return name;
    }

    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public final void setName(String value)
    {
        name = value;
    }

    @ConfigurationAttributeAnnotation(value = "inproc", appendText = "")
    public final boolean getInProc()
    {
        return inproc;
    }

    @ConfigurationAttributeAnnotation(value = "inproc", appendText = "")
    public final void setInProc(boolean value)
    {
        inproc = value;
    }
    
     /**
     * @return the managementPort
     */
    //@ConfigurationAttributeAnnotation(value = "management-port", appendText = "")
    public int getManagementPort() {
        return managementPort;
    }

    /**
     * @param managementPort the managementPort to set
     */
    //@ConfigurationAttributeAnnotation(value = "management-port", appendText = "")
    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }
    
     /**
     * @return the socketPort
     */
    @ConfigurationAttributeAnnotation(value = "client-port", appendText = "")
    public int getClientPort() {
        return socketPort;
    }

    /**
     * @param socketPort the socketPort to set
     */
    @ConfigurationAttributeAnnotation(value = "client-port", appendText = "")
    public void setClientPort(int socketPort) {
        this.socketPort = socketPort;
    }

    @ConfigurationAttributeAnnotation(value = "config-id", appendText = "")
    public final double getConfigID()
    {
        return configID;
    }

    @ConfigurationAttributeAnnotation(value = "config-id", appendText = "")
    public final void setConfigID(double value)
    {
        configID = value;
    }

    @ConfigurationAttributeAnnotation(value = "last-modified", appendText = "")
    public final String getLastModified()
    {
        return lastModified;
    }

    @ConfigurationAttributeAnnotation(value = "last-modified", appendText = "")
    public final void setLastModified(String value)
    {
        lastModified = value;
    }

    /**
     * [Ata]Type is part of 3.8 config. This is to be uncommented after development is complete.
     */
    //get { return cacheType; }
    @ConfigurationAttributeAnnotation(value = "type", appendText = "")
    public final String getCacheType()
    {
        String type = this.cacheType;
        if (type == null)
        {
            type = "local-cache";
            if (this.cluster != null)
            {
                type = "clustered-cache";
            }
        }
        return type;
    }

    @ConfigurationAttributeAnnotation(value = "type", appendText = "")
    public final void setCacheType(String value)
    {
        cacheType = value;
    }

    @ConfigurationAttributeAnnotation(value = "auto-start", appendText = "")
    public final boolean getAutoStartCacheOnServiceStartup()
    {
        return this.autoStartCacheOnServiceStartup;
    }

    @ConfigurationAttributeAnnotation(value = "auto-start", appendText = "")
    public final void setAutoStartCacheOnServiceStartup(boolean value)
    {
        autoStartCacheOnServiceStartup = value;
    }

    @ConfigurationSectionAnnotation(value = "log")
    public final com.alachisoft.tayzgrid.config.newdom.Log getLog()
    {
        return log;
    }

    @ConfigurationSectionAnnotation(value = "log")
    public final void setLog(com.alachisoft.tayzgrid.config.newdom.Log value)
    {
        log = value;
    }
    
    /**
     * @return the _expirationPolicy
     */
    @ConfigurationSectionAnnotation(value = "expiration-policy")
    public com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy getExpirationPolicy() {
        return _expirationPolicy;
    }

    /**
     * @param _expirationPolicy the _expirationPolicy to set
     */
    @ConfigurationSectionAnnotation(value = "expiration-policy")
    public void setExpirationPolicy(com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy _expirationPolicy) {
        this._expirationPolicy = _expirationPolicy;
    }

    @ConfigurationSectionAnnotation(value = "perf-counters")
    public final com.alachisoft.tayzgrid.config.newdom.PerfCounters getPerfCounters()
    {
        return perfCounters;
    }

    @ConfigurationSectionAnnotation(value = "perf-counters")
    public final void setPerfCounters(com.alachisoft.tayzgrid.config.newdom.PerfCounters value)
    {
        perfCounters = value;
    }


    @ConfigurationSectionAnnotation(value = "data-load-balancing")
    public final com.alachisoft.tayzgrid.config.newdom.AutoLoadBalancing getAutoLoadBalancing()
    {
        return autoBalancing;
    }

    @ConfigurationSectionAnnotation(value = "data-load-balancing")
    public final void setAutoLoadBalancing(com.alachisoft.tayzgrid.config.newdom.AutoLoadBalancing value)
    {
        autoBalancing = value;
    }

    @ConfigurationSectionAnnotation(value = "client-nodes")
    public final com.alachisoft.tayzgrid.config.newdom.ClientNodes getClientNodes()
    {
        return clientNodes;
    }

    @ConfigurationSectionAnnotation(value = "client-nodes")
    public final void setClientNodes(com.alachisoft.tayzgrid.config.newdom.ClientNodes value)
    {
        clientNodes = value;
    }

    @ConfigurationSectionAnnotation(value = "indexes")
    public final com.alachisoft.tayzgrid.config.newdom.QueryIndex getQueryIndices()
    {
        return indexes;
    }

    @ConfigurationSectionAnnotation(value = "indexes")
    public final void setQueryIndices(com.alachisoft.tayzgrid.config.newdom.QueryIndex value)
    {
        indexes = value;
    }

    @ConfigurationSectionAnnotation(value = "backing-source")
    public final com.alachisoft.tayzgrid.config.newdom.BackingSource getBackingSource()
    {
        return backingSource;
    }

    @ConfigurationSectionAnnotation(value = "backing-source")
    public final void setBackingSource(com.alachisoft.tayzgrid.config.newdom.BackingSource value)
    {
        backingSource = value;
    }

    @ConfigurationSectionAnnotation(value = "cache-loader")
    public final com.alachisoft.tayzgrid.config.newdom.CacheLoader getCacheLoader()
    {
        return cacheloader;
    }

    @ConfigurationSectionAnnotation(value = "cache-loader")
    public final void setCacheLoader(com.alachisoft.tayzgrid.config.newdom.CacheLoader value)
    {
        cacheloader = value;
    }

    @ConfigurationSectionAnnotation(value = "notifications")
    public final com.alachisoft.tayzgrid.config.newdom.Notifications getNotifications()
    {
        return notifications;
    }

    @ConfigurationSectionAnnotation(value = "notifications")
    public final void setNotifications(com.alachisoft.tayzgrid.config.newdom.Notifications value)
    {
        notifications = value;
    }

  

    @ConfigurationSectionAnnotation(value = "cleanup")
    public final com.alachisoft.tayzgrid.config.newdom.Cleanup getCleanup()
    {
        return cleanup;
    }

    @ConfigurationSectionAnnotation(value = "cleanup")
    public final void setCleanup(com.alachisoft.tayzgrid.config.newdom.Cleanup value)
    {
        cleanup = value;
    }

    @ConfigurationSectionAnnotation(value = "storage")
    public final com.alachisoft.tayzgrid.config.newdom.Storage getStorage()
    {
        return storage;
    }

    @ConfigurationSectionAnnotation(value = "storage")
    public final void setStorage(com.alachisoft.tayzgrid.config.newdom.Storage value)
    {
        storage = value;
    }
    

    @ConfigurationSectionAnnotation(value = "eviction-policy")
    public final com.alachisoft.tayzgrid.config.newdom.EvictionPolicy getEvictionPolicy()
    {
        return evictionPolicy;
    }

    @ConfigurationSectionAnnotation(value = "eviction-policy")
    public final void setEvictionPolicy(com.alachisoft.tayzgrid.config.newdom.EvictionPolicy value)
    {
        evictionPolicy = value;
    }

    @ConfigurationSectionAnnotation(value = "cluster")
    public final Cluster getCluster()
    {
        return cluster;
    }

    @ConfigurationSectionAnnotation(value = "cluster")
    public final void setCluster(Cluster value)
    {
        cluster = value;
    }

    @ConfigurationSectionAnnotation(value = "alerts")
    public final com.alachisoft.tayzgrid.config.newdom.AlertsNotifications getAlertsNotifications()
    {
        return _alertsNotifications;
    }

    @ConfigurationSectionAnnotation(value = "alerts")
    public final void setAlertsNotifications(com.alachisoft.tayzgrid.config.newdom.AlertsNotifications value)
    {
        _alertsNotifications = value;
    }

    public final com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig getSQLDependencyConfig()
    {
        return null;
    }

    public final void setSQLDependencyConfig(com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig value)
    {
        _sqlDependencyConfig = value;
    }

    public final Factory<CacheLoader> getCacheLoaderFactory()
    {
        return cacheLoaderFactory;
    }
    
    public final void setCacheLoaderFactory(Factory<CacheLoader> cacheLoaderFactory)
    {
        this.cacheLoaderFactory = cacheLoaderFactory;
    }
    
    public final boolean getIsLoaderOnly() {
        return isLoaderOnly;
    }
    public final void setIsLoaderOnly(boolean isLoaderOnly) {
        this.isLoaderOnly = isLoaderOnly;
    }
    public final Factory<CacheWriter> getCacheWriterFactory()
    {
        return cacheWriterFactory;
    }
    
    public final void setCacheWriterFactory(Factory<CacheWriter> cacheWriterFactory)
    {
        this.cacheWriterFactory = cacheWriterFactory;
    }


    @ConfigurationSectionAnnotation(value = "tasks-config")
    public final com.alachisoft.tayzgrid.config.newdom.TaskConfiguration getTaskConfiguration()
    {
        return _taskConfiguration;
    }
    @ConfigurationSectionAnnotation(value = "tasks-config")
    public final void setTaskConfiguration(com.alachisoft.tayzgrid.config.newdom.TaskConfiguration value)
    {
        this._taskConfiguration = value;
    }
    

    public final Object clone() throws CloneNotSupportedException
    {
        CacheServerConfig config = new CacheServerConfig();
        config.setName(getName() != null ? new String(getName()) : null);
        config.cacheType = this.cacheType;
        config.setAutoStartCacheOnServiceStartup(this.getAutoStartCacheOnServiceStartup());
        config.setDataFormat(this.getDataFormat());
        config.setInProc(getInProc());
        config.setConfigID(getConfigID());
        config.setLastModified(getLastModified() != null ? new String(getLastModified()) : null);

        Object tempVar = clientNodes.clone();
        config.clientNodes = clientNodes != null ? (com.alachisoft.tayzgrid.config.newdom.ClientNodes) ((tempVar instanceof com.alachisoft.tayzgrid.config.newdom.ClientNodes) ? tempVar : null) : null;
        config.setLog(getLog() != null ? (com.alachisoft.tayzgrid.config.newdom.Log) getLog().clone() : null);
        config.setPerfCounters(getPerfCounters() != null ? (com.alachisoft.tayzgrid.config.newdom.PerfCounters) getPerfCounters().clone() : null);


        config.autoBalancing = this.autoBalancing != null ? (com.alachisoft.tayzgrid.config.newdom.AutoLoadBalancing) this.autoBalancing.clone() : null;
        

        config.setCleanup(getCleanup() != null ? (com.alachisoft.tayzgrid.config.newdom.Cleanup) getCleanup().clone() : null);
        config.setStorage(getStorage() != null ? (com.alachisoft.tayzgrid.config.newdom.Storage) getStorage().clone() : null);
        config.setEvictionPolicy(getEvictionPolicy() != null ? (com.alachisoft.tayzgrid.config.newdom.EvictionPolicy) getEvictionPolicy().clone() : null);
        config.setCluster(getCluster() != null ? (Cluster) getCluster().clone() : null);

        config.backingSource = this.backingSource != null ? (com.alachisoft.tayzgrid.config.newdom.BackingSource) this.backingSource.clone() : null;
        config.cacheloader = this.cacheloader != null ? (com.alachisoft.tayzgrid.config.newdom.CacheLoader) this.cacheloader.clone() : null;
        config.setAlertsNotifications(getAlertsNotifications() != null ? (com.alachisoft.tayzgrid.config.newdom.AlertsNotifications) this.getAlertsNotifications().clone() : null);
        config.setQueryIndices(getQueryIndices() != null ? (com.alachisoft.tayzgrid.config.newdom.QueryIndex) getQueryIndices().clone() : null);
        config.setNotifications(getNotifications() != null ? (com.alachisoft.tayzgrid.config.newdom.Notifications) getNotifications().clone() : null);
        config.setSQLDependencyConfig(getSQLDependencyConfig() != null ? (com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig) getSQLDependencyConfig().clone() : null);

        config.setTaskConfiguration(getTaskConfiguration() != null ? (TaskConfiguration) getTaskConfiguration().clone() : null);
        
        config.setIsRegistered(this.getIsRegistered());
        config.setIsRunning(this.getIsRunning());
        config.licenseIsExpired = this.licenseIsExpired;
        config._runtimeContextValue = this._runtimeContextValue;

        config.managementPort = this.managementPort;
        config.socketPort = this.socketPort;
        config._expirationPolicy = this._expirationPolicy;

        return config;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        cacheIsRunning = reader.ReadBoolean();
        cacheIsRegistered = reader.ReadBoolean();
        licenseIsExpired = reader.ReadBoolean();
        autoStartCacheOnServiceStartup = reader.ReadBoolean();
        name = Common.as(reader.ReadObject(), String.class);
        String temp = Common.as(reader.ReadObject(), String.class);
        if(temp.equalsIgnoreCase("Binary"))    
        {_dataformat = DataFormat.Binary;}
        else if(temp.equalsIgnoreCase("Object"))
        {_dataformat = DataFormat.Object;}
        inproc = reader.ReadBoolean();
        configID = reader.ReadDouble();
        lastModified = Common.as(reader.ReadObject(), String.class);
        cacheType = Common.as(reader.ReadObject(), String.class);
        log = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.Log.class);
        perfCounters = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.PerfCounters.class);

        indexes = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.QueryIndex.class);
        backingSource = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.BackingSource.class);
        cacheloader = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.CacheLoader.class);
        notifications = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.Notifications.class);
       
        cleanup = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.Cleanup.class);
        storage = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.Storage.class);
        evictionPolicy = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.EvictionPolicy.class);
        cluster = Common.as(reader.ReadObject(), Cluster.class);
        autoBalancing = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.AutoLoadBalancing.class);
        clientNodes = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.ClientNodes.class);
        _alertsNotifications = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.AlertsNotifications.class);
        
        _taskConfiguration = Common.as(reader.ReadObject(), com.alachisoft.tayzgrid.config.newdom.TaskConfiguration.class);
        _runtimeContextValue = "1".equals(Common.as(reader.ReadObject(), String.class)) ? RtContextValue.JVCACHE : RtContextValue.NCACHE  ;        
        managementPort = reader.ReadInt32();
        socketPort = reader.ReadInt32();
        _expirationPolicy = Common.as(reader.ReadObject(),com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy.class);

    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(cacheIsRunning);
        writer.Write(cacheIsRegistered);
        writer.Write(licenseIsExpired);
        writer.Write(autoStartCacheOnServiceStartup);
        writer.WriteObject(name);
        if(_dataformat == DataFormat.Binary)
        {writer.WriteObject("Binary");}
        else if (_dataformat == DataFormat.Object)
        {writer.WriteObject("Object");}
        writer.Write(inproc);
        writer.Write(configID);
        writer.WriteObject(lastModified);
        writer.WriteObject(cacheType);
        writer.WriteObject(log);
        writer.WriteObject(perfCounters);

        writer.WriteObject(indexes);
        writer.WriteObject(backingSource);
        writer.WriteObject(cacheloader);
        writer.WriteObject(notifications);
        writer.WriteObject(cleanup);
        writer.WriteObject(storage);
        writer.WriteObject(evictionPolicy);
        writer.WriteObject(cluster);

        writer.WriteObject(autoBalancing);
        writer.WriteObject(clientNodes);
        writer.WriteObject(_alertsNotifications);
        
        writer.WriteObject(_taskConfiguration);
        writer.WriteObject(_runtimeContextValue == RtContextValue.JVCACHE ? "1" : "0");
        writer.Write(managementPort);
        writer.Write(socketPort);
        writer.WriteObject(_expirationPolicy);

    }



}
