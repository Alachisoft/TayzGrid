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

package com.alachisoft.tayzgrid.config;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerMapping implements Cloneable, InternalCompactSerializable, java.io.Serializable {

    private java.util.ArrayList<Mapping> actualMappingList = new java.util.ArrayList<Mapping>();

    public ServerMapping() {
    }

    public ServerMapping(HashMap mappings) {
        java.util.Iterator itor = mappings.entrySet().iterator();
        while (itor.hasNext()) {
            Map.Entry entry = (Map.Entry) itor.next();
            HashMap values = (HashMap) entry.getValue();

            Mapping _mapping = new Mapping();
            _mapping.setPrivateIP((String) ((values.get("private-ip") instanceof String) ? values.get("private-ip") : null));
            _mapping.setPrivatePort(Integer.parseInt(values.get("private-port").toString()));
            _mapping.setPublicIP((String) ((values.get("public-ip") instanceof String) ? values.get("public-ip") : null));
            _mapping.setPublicPort(Integer.parseInt(values.get("public-port").toString()));
            actualMappingList.add(_mapping);
        }

    }

    @ConfigurationAttributeAnnotation(value = "mapping", appendText = "")
    public final Mapping[] getMappingServers() {
        if (actualMappingList != null) {
            return actualMappingList.toArray(new Mapping[0]);
        }
        return null;
    }

    @ConfigurationAttributeAnnotation(value = "mapping", appendText = "")
    public final void setMappingServers(Object[] mappings) {
        if (actualMappingList == null) {
            actualMappingList = new java.util.ArrayList<Mapping>();
        }
        if (mappings != null) {
            for (Object mapping : mappings) {
                actualMappingList.add((Mapping) mapping);
            }
        }
    }

    @Override
    public final Object clone() {
        ServerMapping mapp = new ServerMapping();
        mapp.actualMappingList = actualMappingList;
        return mapp;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        setMappingServers(Common.as(reader.ReadObject(), Mapping[].class));
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(this.actualMappingList);
    }
}
