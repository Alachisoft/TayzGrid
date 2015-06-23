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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.math.BigInteger;

public class CacheServerConfigSetting implements Cloneable, InternalCompactSerializable {
    
    private boolean autoStartCacheOnServiceStartup = false;
    private String name;
    private boolean inproc;
    private String lastModified;
    /**
     * This helps to differentiate between a local-cache, client-cache and
     * clustered-cache
     */
    private String cacheType;
    private Log log;
    private PerfCounters perfCounters;

    private QueryIndex indexes;
    private BackingSource backingSource;
    private CacheLoader cacheloader;
    private Notifications notifications;
    private Cleanup cleanup;
    private Storage storage;
    private EvictionPolicy evictionPolicy;
    private AutoLoadBalancing autoBalancing;
    private AlertsNotifications _alertsNotifications;
    private SQLDependencyConfig _sqlDependencyConfig;
    private CacheTopology cacheTopology;
    private TaskConfiguration _taskConfiguration;
    private String _alias = "";
    private int _managementPort;
    private int _socketPort;
	
    private ExpirationPolicy _expirationPolicy;
    private DataFormat _dataformat=DataFormat.Binary;

    public CacheServerConfigSetting() {
        log = new Log();
        notifications = new Notifications();
        cacheTopology = new CacheTopology();//Change for dom 
        _taskConfiguration = new TaskConfiguration();
    }
    
    @ConfigurationAttributeAnnotation(value = "cache-name", appendText = "")
    public final String getName() {
        return name;
    }

    @ConfigurationAttributeAnnotation(value = "cache-name", appendText = "")
    public final void setName(String value) {
        name = value;
    }

    @ConfigurationAttributeAnnotation(value = "alias", appendText = "")
    public final String getAlias() {
        return _alias;
    }

    @ConfigurationAttributeAnnotation(value = "alias", appendText = "")
    public final void setAlias(String value) {
        _alias = value;
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
    
    @ConfigurationAttributeAnnotation(value = "inproc", appendText = "")
    public final boolean getInProc() {
        return inproc;
    }

    @ConfigurationAttributeAnnotation(value = "inproc", appendText = "")
    public final void setInProc(boolean value) {
        inproc = value;
    }

    @ConfigurationAttributeAnnotation(value = "last-modified", appendText = "")
    public final String getLastModified() {
        return lastModified;
    }

    @ConfigurationAttributeAnnotation(value = "last-modified", appendText = "")
    public final void setLastModified(String value) {
        lastModified = value;
    }

    /**
     * [Ata]Type is part of 3.8 config. This is to be uncommented after
     * development is complete.
     */
    public final String getCacheType() {
        String type = this.cacheTopology.getTopology();
        if (type.equals("replicated-server") || type.equals("replicated") || type.equals("partitioned-server") || type.equals("partitioned")) {
            return "clustered-cache";
        } 
        else if (type.equals("local-cache")) {
            return "local-cache";
        }

        return type;
    }

    public final void setCacheType(String value) {
        cacheType = value;
    }
    
     /**
     * @return the _managementPort
     */
    //@ConfigurationAttributeAnnotation(value = "management-port",appendText = "")
    public final int getManagementPort() {
        return _managementPort;
    }

    /**
     * @param _managementPort the _managementPort to set
     */
    //@ConfigurationAttributeAnnotation(value = "management-port", appendText = "")
    public final void setManagementPort(int _managementPort) {
        this._managementPort = _managementPort;
    }
    
     /**
     * @return the _socketPort
     */
    @ConfigurationAttributeAnnotation(value = "client-port", appendText = "")
    public int getClientPort() {
        return _socketPort;
    }

    /**
     * @param _socketPort the _socketPort to set
     */
    @ConfigurationAttributeAnnotation(value = "client-port", appendText = "")
    public void setClientPort(int _socketPort) {
        this._socketPort = _socketPort;
    }

    @ConfigurationAttributeAnnotation(value = "auto-start", appendText = "")
    public final boolean getAutoStartCacheOnServiceStartup() {
        return this.autoStartCacheOnServiceStartup;
    }

    @ConfigurationAttributeAnnotation(value = "auto-start", appendText = "")
    public final void setAutoStartCacheOnServiceStartup(boolean value) {
        autoStartCacheOnServiceStartup = value;
    }

    @ConfigurationSectionAnnotation(value = "logging")
    public final Log getLog() {
        return log;
    }

    @ConfigurationSectionAnnotation(value = "logging")
    public final void setLog(Log value) {
        log = value;
    }

    @ConfigurationSectionAnnotation(value = "performance-counters")
    public final PerfCounters getPerfCounters() {
        return perfCounters;
    }

    @ConfigurationSectionAnnotation(value = "performance-counters")
    public final void setPerfCounters(PerfCounters value) {
        perfCounters = value;
    }

 

    @ConfigurationSectionAnnotation(value = "data-load-balancing")
    public final AutoLoadBalancing getAutoLoadBalancing() {
        return autoBalancing;
    }

    @ConfigurationSectionAnnotation(value = "data-load-balancing")
    public final void setAutoLoadBalancing(AutoLoadBalancing value) {
        autoBalancing = value;
    }

    @ConfigurationSectionAnnotation(value = "query-indexes")
    public final QueryIndex getQueryIndices() {
        return indexes;
    }

    @ConfigurationSectionAnnotation(value = "query-indexes")
    public final void setQueryIndices(QueryIndex value) {
        indexes = value;
    }

    @ConfigurationSectionAnnotation(value = "backing-source")
    public final BackingSource getBackingSource() {
        return backingSource;
    }

    @ConfigurationSectionAnnotation(value = "backing-source")
    public final void setBackingSource(BackingSource value) {
        backingSource = value;
    }

    @ConfigurationSectionAnnotation(value = "cache-loader")
    public final CacheLoader getCacheLoader() {
        return cacheloader;
    }

    @ConfigurationSectionAnnotation(value = "cache-loader")
    public final void setCacheLoader(CacheLoader value) {
        cacheloader = value;
    }

    @ConfigurationSectionAnnotation(value = "cache-notifications")
    public final Notifications getNotifications() {
        return notifications;
    }

    @ConfigurationSectionAnnotation(value = "cache-notifications")
    public final void setNotifications(Notifications value) {
        notifications = value;
    }

  

    @ConfigurationSectionAnnotation(value = "cleanup")
    public final Cleanup getCleanup() {
        return cleanup;
    }

    @ConfigurationSectionAnnotation(value = "cleanup")
    public final void setCleanup(Cleanup value) {
        cleanup = value;
    }

    @ConfigurationSectionAnnotation(value = "storage")
    public final Storage getStorage() {
        return storage;
    }

    @ConfigurationSectionAnnotation(value = "storage")
    public final void setStorage(Storage value) {
        storage = value;
    }

    @ConfigurationSectionAnnotation(value = "eviction-policy")
    public final EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    @ConfigurationSectionAnnotation(value = "eviction-policy")
    public final void setEvictionPolicy(EvictionPolicy value) {
        evictionPolicy = value;
    }

    @ConfigurationSectionAnnotation(value = "alerts")
    public final AlertsNotifications getAlertsNotifications() {
        return _alertsNotifications;
    }

    @ConfigurationSectionAnnotation(value = "alerts")
    public final void setAlertsNotifications(AlertsNotifications value) {
        _alertsNotifications = value;
    }

    @ConfigurationSectionAnnotation(value = "sql-dependency")
    public final SQLDependencyConfig getSQLDependencyConfig() {
        return _sqlDependencyConfig;
    }

    @ConfigurationSectionAnnotation(value = "sql-dependency")
    public final void setSQLDependencyConfig(SQLDependencyConfig value) {
        _sqlDependencyConfig = value;
    }

    @ConfigurationSectionAnnotation(value = "cache-topology")
    public final CacheTopology getCacheTopology() {
        return cacheTopology;
    }

    @ConfigurationSectionAnnotation(value = "cache-topology")
    public final void setCacheTopology(CacheTopology value) {
        cacheTopology = value;
    }

    @ConfigurationSectionAnnotation(value = "tasks-config")
    public final TaskConfiguration getTaskConfiguration() {
        return _taskConfiguration;
    }

    @ConfigurationSectionAnnotation(value = "tasks-config")
    public final void setTaskConfiguration(TaskConfiguration value) {
        _taskConfiguration = value;
    }
    
    public final String getUniqueId() {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(_alias)) {
            return name;
        }
        return name + "[" + _alias + "]";
    }
    
    @Override
    public final Object clone() throws CloneNotSupportedException {
        CacheServerConfigSetting config = new CacheServerConfigSetting();
        config.setName(getName() != null ? (String) getName() : null);
        config.cacheType = this.cacheType;
        config.setAutoStartCacheOnServiceStartup(this.getAutoStartCacheOnServiceStartup());
        config.setDataFormat(getDataFormat());
        config.setInProc(getInProc());
        config.setAlias(getAlias());
        config.setLastModified(getLastModified() != null ? (String) getLastModified() : null);

        config.setLog(getLog() != null ? (Log) getLog().clone() : null);
        config.setPerfCounters(getPerfCounters() != null ? (PerfCounters) getPerfCounters().clone() : null);
        
        config.autoBalancing = this.autoBalancing != null ? (AutoLoadBalancing) this.autoBalancing.clone() : null;
        
        config.setCleanup(getCleanup() != null ? (Cleanup) getCleanup().clone() : null);
        config.setStorage(getStorage() != null ? (Storage) getStorage().clone() : null);
        config.setEvictionPolicy(getEvictionPolicy() != null ? (EvictionPolicy) getEvictionPolicy().clone() : null);

        config.backingSource = this.backingSource != null ? (BackingSource) this.backingSource.clone() : null;
        config.cacheloader = this.cacheloader != null ? (CacheLoader) this.cacheloader.clone() : null;
        config.setAlertsNotifications(getAlertsNotifications() != null ? (AlertsNotifications) this.getAlertsNotifications().clone() : null);
        config.setQueryIndices(getQueryIndices() != null ? (QueryIndex) getQueryIndices().clone() : null);
        config.setNotifications(getNotifications() != null ? (Notifications) getNotifications().clone() : null);
        //config.setSQLDependencyConfig(getSQLDependencyConfig() != null ? (SQLDependencyConfig) getSQLDependencyConfig().clone() : null);
        config.cacheTopology = this.cacheTopology;
        config._managementPort = this._managementPort;
        config._socketPort = this._socketPort;
        config._expirationPolicy = this._expirationPolicy;
        config.setTaskConfiguration(getTaskConfiguration() != null ? (TaskConfiguration) getTaskConfiguration().clone() : null);
        return config;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        autoStartCacheOnServiceStartup = reader.ReadBoolean();
        name = Common.as(reader.ReadObject(), String.class);
        String temp = Common.as(reader.ReadObject(), String.class);
        if(temp.equalsIgnoreCase("Binary"))    
        {_dataformat = DataFormat.Binary;}
        else if(temp.equalsIgnoreCase("Object"))
        {_dataformat = DataFormat.Object;}
        inproc = reader.ReadBoolean();
        lastModified = Common.as(reader.ReadObject(), String.class);
        cacheType = Common.as(reader.ReadObject(), String.class);
        log = Common.as(reader.ReadObject(), Log.class);
        perfCounters = Common.as(reader.ReadObject(), PerfCounters.class);
        indexes = Common.as(reader.ReadObject(), QueryIndex.class);
        backingSource = Common.as(reader.ReadObject(), BackingSource.class);
        cacheloader = Common.as(reader.ReadObject(), CacheLoader.class);
        notifications = Common.as(reader.ReadObject(), Notifications.class);
     
        cleanup = Common.as(reader.ReadObject(), Cleanup.class);
        storage = Common.as(reader.ReadObject(), Storage.class);
        evictionPolicy = Common.as(reader.ReadObject(), EvictionPolicy.class);

        autoBalancing = Common.as(reader.ReadObject(), AutoLoadBalancing.class);
        //Change for Dom Code Comment
        _alertsNotifications = Common.as(reader.ReadObject(), AlertsNotifications.class);
        //_sqlDependencyConfig = Common.as(reader.ReadObject(), SQLDependencyConfig.class);
        cacheTopology = Common.as(reader.ReadObject(), CacheTopology.class);
        
        //Change for Dom Code Insert
        _alias = Common.as(reader.ReadObject(), String.class);


        _managementPort = reader.ReadInt32();
        _socketPort = reader.ReadInt32();

        _expirationPolicy = Common.as(reader.ReadObject(), ExpirationPolicy.class);


        _taskConfiguration = Common.as(reader.ReadObject(), TaskConfiguration.class);


    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.Write(autoStartCacheOnServiceStartup);
        writer.WriteObject(name);
        if(_dataformat == DataFormat.Binary)
        {writer.WriteObject("Binary");}
        else if (_dataformat == DataFormat.Object)
        {writer.WriteObject("Object");}
        //writer.WriteObject(_dataformat.toString());
        writer.Write(inproc);
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
        writer.WriteObject(autoBalancing);
        writer.WriteObject(_alertsNotifications);
        //writer.WriteObject(_sqlDependencyConfig);
        writer.WriteObject(cacheTopology);
        //Change for Dom Code Insert
        writer.WriteObject(_alias);
        writer.Write(_managementPort);
        writer.Write(_socketPort);
        writer.WriteObject(_expirationPolicy);
        writer.WriteObject(_taskConfiguration);
    }

    /**
     * @return the _expirationPolicy
     */
    @ConfigurationSectionAnnotation(value = "expiration-policy")
    public ExpirationPolicy getExpirationPolicy() {
        return _expirationPolicy;
    }

    /**
     * @param _expirationPolicy the _expirationPolicy to set
     */
    @ConfigurationSectionAnnotation(value = "expiration-policy")
    public void setExpirationPolicy(ExpirationPolicy _expirationPolicy) {
        this._expirationPolicy = _expirationPolicy;

    }

   

   
}
