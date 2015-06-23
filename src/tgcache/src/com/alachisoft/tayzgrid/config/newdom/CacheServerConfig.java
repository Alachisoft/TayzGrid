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

import com.alachisoft.tayzgrid.common.enums.CacheStatus;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import javax.cache.configuration.Factory;
import javax.cache.integration.CacheWriter;

@ConfigurationRootAnnotation(value = "cache-config")
public class CacheServerConfig implements Cloneable, InternalCompactSerializable {

    private CacheServerConfigSetting cacheSettings;
    private CacheDeployment cacheDeployment;
    private boolean cacheIsRunning = false;
    private boolean cacheIsRegistered = false;
    private boolean licenseIsExpired = false;
    private double configID;
    
    private Factory<javax.cache.integration.CacheLoader> cacheLoaderFactory;
    private Factory<CacheWriter> cacheWriterFactory;
    private boolean isLoaderOnly = false;
    
    public CacheServerConfig() {
        this.cacheSettings = new CacheServerConfigSetting();
    }

    public final boolean getIsRegistered() {
        return cacheIsRegistered;
    }

    public final void setIsRegistered(boolean value) {
        cacheIsRegistered = value;
    }

    public final boolean getIsRunning() {
        boolean isRunning = cacheIsRunning;

        if (this.cacheSettings.getCacheType() == com.alachisoft.tayzgrid.common.enums.CacheTopologyType.ClusteredCache) {
            for (StatusInfo cacheStatus : this.cacheDeployment.getServers().getNodes().values()) {
                if (cacheStatus.Status == CacheStatus.Running) {
                    isRunning = true;
                    break;
                }
            }
        }

        return isRunning;
    }

    public final void setIsRunning(boolean value) {
        if (this.cacheSettings.getCacheType().equals("local-cache") || this.cacheSettings.getCacheType().equals("client-cache")) {
            cacheIsRunning = value;
        }
    }

    public final boolean getIsExpired() {
        return licenseIsExpired;
    }

    public final void setIsExpired(boolean value) {
        licenseIsExpired = value;
    }

    @ConfigurationSectionAnnotation(value = "cache-settings")
    public final CacheServerConfigSetting getCacheSettings() {
        return cacheSettings;
    }

    @ConfigurationSectionAnnotation(value = "cache-settings")
    public final void setCacheSettings(CacheServerConfigSetting value) {
        cacheSettings = value;
    }

    @ConfigurationSectionAnnotation(value = "cache-deployment")
    public final CacheDeployment getCacheDeployment() {
        return cacheDeployment;
    }

    @ConfigurationSectionAnnotation(value = "cache-deployment")
    public final void setCacheDeployment(CacheDeployment value) {
        cacheDeployment = value;
    }
    //[ConfigurationAttribute("config-id")]

    @ConfigurationAttributeAnnotation(value = "config-id", appendText = "")
    public final double getConfigID() {
        return configID;
    }

    @ConfigurationAttributeAnnotation(value = "config-id", appendText = "")
    public final void setConfigID(double value) {
        configID = value;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException {
        CacheServerConfig config = new CacheServerConfig();
        config.cacheSettings = cacheSettings != null ? (CacheServerConfigSetting) cacheSettings.clone() : null;
        config.cacheDeployment = cacheDeployment != null ? (CacheDeployment) cacheDeployment.clone() : null;
        config.configID = this.configID;
        config.setIsRegistered(this.cacheIsRegistered);
        config.setIsRunning(this.cacheIsRunning);
        config.setIsExpired(this.licenseIsExpired);
        return config;
    }

    /**
     *
     * @param reader
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {

        this.cacheSettings = Common.as(reader.ReadObject(), CacheServerConfigSetting.class);
        this.cacheDeployment = Common.as(reader.ReadObject(), CacheDeployment.class);
        this.configID = reader.ReadDouble();
        cacheIsRunning = reader.ReadBoolean();
        cacheIsRegistered = reader.ReadBoolean();
        licenseIsExpired = reader.ReadBoolean();
    }

    /**
     *
     * @param writer
     * @throws IOException
     */
    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(cacheSettings);
        writer.WriteObject(this.cacheDeployment);
        writer.Write(configID);
        writer.Write(cacheIsRunning);
        writer.Write(cacheIsRegistered);
        writer.Write(licenseIsExpired);

    }
    
    public final Factory<javax.cache.integration.CacheLoader> getCacheLoaderFactory()
    {
        return cacheLoaderFactory;
    }
    
    public final void setCacheLoaderFactory(Factory<javax.cache.integration.CacheLoader> cacheLoaderFactory)
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
}