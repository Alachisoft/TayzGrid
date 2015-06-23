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

import com.alachisoft.tayzgrid.tools.common.ArgumentAttributeAnnontation;

public class CacheHostParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private String cacheName, cacheConfigPath, serverPropertiesPath;
    private int socketServerPort = -1, managementPort = 1;

    public CacheHostParam() {
    }

    @ArgumentAttributeAnnontation(shortNotation = "-i", fullNotation = "--cacheid", appendText = "", defaultValue = "")
    public String getCacheName() {
        return cacheName;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-i", fullNotation = "--cacheid", appendText = "", defaultValue = "")
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--configfile", appendText = "", defaultValue = "")
    public String getCacheConfigPath() {
        return cacheConfigPath;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--configfile", appendText = "", defaultValue = "")
    public void setCacheConfigPath(String cacheConfigPath) {
        this.cacheConfigPath = cacheConfigPath;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-F", fullNotation = "--propertyfile", appendText = "", defaultValue = "")
    public String getServerPropertiesPath() {
        return serverPropertiesPath;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-F", fullNotation = "--propertyfile", appendText = "", defaultValue = "")
    public void setServerPropertiesPath(String serverPropertiesPath) {
        this.serverPropertiesPath = serverPropertiesPath;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--clientport", appendText = "", defaultValue = "")
    public int getSocketServerPort() {
        return socketServerPort;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--clientport", appendText = "", defaultValue = "")
    public void setSocketServerPort(int socketServerPort) {
        this.socketServerPort = socketServerPort;
    }

   // @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--managementport", appendText = "", defaultValue = "")
    public int getManagementPort() {
        return managementPort;
    }

   // @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--managementport", appendText = "", defaultValue = "")
    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

}
