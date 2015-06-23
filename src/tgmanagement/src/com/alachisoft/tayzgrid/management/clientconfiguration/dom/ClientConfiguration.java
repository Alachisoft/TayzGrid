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

package com.alachisoft.tayzgrid.management.clientconfiguration.dom;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@ConfigurationRootAnnotation(value = "configuration")
public class ClientConfiguration implements Cloneable, InternalCompactSerializable
{

    private NodeConfiguration _nodeConfig;
    private java.util.HashMap<String, CacheConfiguration> _cacheConfigsMap;
    private String _bindIp;

    @ConfigurationSectionAnnotation(value = "cache-server")
    public final NodeConfiguration getNodeConfiguration()
    {
        return _nodeConfig;
    }

    @ConfigurationSectionAnnotation(value = "cache-server")
    public final void setNodeConfiguration(Object value)
    {
        _nodeConfig = (NodeConfiguration) value;
    }

    @ConfigurationSectionAnnotation(value = "cache")
    public final CacheConfiguration[] getCacheConfigurations()
    {
        CacheConfiguration[] configs = null;

        if (_cacheConfigsMap != null)
        {
            configs = new CacheConfiguration[_cacheConfigsMap.size()];
            if (_cacheConfigsMap.size() > 0)
            {
                Iterator<com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration> iterator = _cacheConfigsMap.values().iterator();
                int index = 0;
                while (iterator.hasNext())
                {
                    configs[index] = iterator.next();
                    index++;
                }
            }
        }

        return configs;
    }

    @ConfigurationSectionAnnotation(value = "cache")
    public final void setCacheConfigurations(Object[] value)
    {
        CacheConfiguration config;
        if (_cacheConfigsMap != null && _cacheConfigsMap.size() > 0)
        {
            _cacheConfigsMap.clear();
        }
        else
        {
            _cacheConfigsMap = new java.util.HashMap<String, CacheConfiguration>();
        }

        for (Object temp : value)
        {
            config = (CacheConfiguration) temp;
            _cacheConfigsMap.put(config.getCacheId().toLowerCase(), config);
        }
    }

    public final String getBindIp()
    {
        return _bindIp;
    }

    public final void setBindIp(String value)
    {
        _bindIp = value;

        if (_cacheConfigsMap != null)
        {
            for (CacheConfiguration config : _cacheConfigsMap.values())
            {
                if (config != null)
                {
                    config.setBindIp(value);
                }
            }
        }
    }

    public final java.util.HashMap<String, CacheConfiguration> getCacheConfigurationsMap()
    {
        return _cacheConfigsMap;
    }

    public final void setCacheConfigurationsMap(java.util.HashMap<String, CacheConfiguration> value)
    {
        _cacheConfigsMap = value;
    }

    public final Object clone()
    {
        ClientConfiguration configuration = new ClientConfiguration();
        Object tempVar = getCacheConfigurations().clone();
        configuration.setCacheConfigurations(getCacheConfigurations() != null ? (CacheConfiguration[]) ((tempVar instanceof CacheConfiguration[]) ? tempVar : null) : null);
        Object tempVar2 = _nodeConfig.clone();
        configuration._nodeConfig = _nodeConfig != null ? (NodeConfiguration) ((tempVar2 instanceof NodeConfiguration) ? tempVar2 : null) : null;
        configuration.setBindIp(_bindIp);

        return configuration;
    }

    //<editor-fold defaultstate="collapsed" desc="ISerailizable">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        this._nodeConfig = Common.as(reader.ReadObject(), NodeConfiguration.class);
        this._cacheConfigsMap = new HashMap<String, CacheConfiguration>();
        int length = reader.ReadInt32();
        for (int i = 0; i < length; i++)
        {
            _cacheConfigsMap.put((String) Common.as(reader.ReadObject(), String.class), Common.as(reader.ReadObject(), CacheConfiguration.class));
        }
        this._bindIp = Common.as(reader.ReadObject(), String.class);
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_nodeConfig);
        writer.Write(this._cacheConfigsMap.size());
        Map.Entry current = null;
        Iterator ite = this._cacheConfigsMap.entrySet().iterator();
        while (ite.hasNext())
        {
            current = (Map.Entry) ite.next();
            writer.WriteObject(current.getKey().toString());
            writer.WriteObject(current.getValue());
        }
        writer.WriteObject(_bindIp);
    }
    //</editor-fold>
}
