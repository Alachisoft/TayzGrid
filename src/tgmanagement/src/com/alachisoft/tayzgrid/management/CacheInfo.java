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

import com.alachisoft.tayzgrid.caching.LeasedCache;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.config.ConfigReader;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.dom.ConfigConverter;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class CacheInfo implements InternalCompactSerializable {

    private LeasedCache _cache;
    private int CacheProcessId;
    private int[] ports;

    public int getCacheProcessId() {
        return CacheProcessId;
    }

    public void setCacheProcessId(int CacheProcessId) {
        this.CacheProcessId = CacheProcessId;
    }

    public int getManagementPort() {
        return ports[1];
    }

    public int getSocketServerPort() {
        return ports[0];
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public final CacheServerConfig getCacheProps() {
        if (this._cache != null) {
            return this._cache.getConfiguration();
        }
        return null;
    }

    public final void setCacheProps(CacheServerConfig value) {
        if (this._cache != null) {
            this._cache.setConfiguration(value);
        }
    }

    public final LeasedCache getCache() {
        return _cache;
    }

    public final void setCache(LeasedCache value) {
        _cache = value;
    }

    public final void SyncConfiguration() {
        if (this._cache != null) {
            java.util.HashMap config = ConfigConverter.ToHashMap(this.getCacheProps());
            this._cache.setConfigString(ConfigReader.ToPropertiesString(config));
        }
    }

  
    public final void dispose() {
    }

    /**
     *
     * @param reader
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        this._cache.setConfiguration((CacheServerConfig) Common.readAs(reader.ReadObject(), CacheServerConfig.class));
    }

    /**
     *
     * @param writer
     * @throws IOException
     */
    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(this._cache.getConfiguration());
    }
}
