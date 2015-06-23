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
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.math.BigDecimal;

public class EvictionPolicy implements Cloneable, InternalCompactSerializable
{

    private boolean enabled;
    private String defaultPriority;
    private java.math.BigDecimal evictionRatio = new java.math.BigDecimal(0);
    private String policy;

    public EvictionPolicy()
    {
    }


    @ConfigurationAttributeAnnotation(value="enable-eviction",appendText="")//Changes for Dom enabled
    public final boolean getEnabled()
    {
        return enabled;
    }

    @ConfigurationAttributeAnnotation(value="enable-eviction",appendText="")//Changes for Dom enabled
    public final void setEnabled(boolean value)
    {
        enabled = value;
    }

    @ConfigurationAttributeAnnotation(value="default-priority",appendText="")
    public final String getDefaultPriority()
    {
        return defaultPriority;
    }

    @ConfigurationAttributeAnnotation(value="default-priority",appendText="")
    public final void setDefaultPriority(String value)
    {
        defaultPriority = value;
    }

    @ConfigurationAttributeAnnotation(value="policy",appendText="")
    public final String getPolicy()
    {
        return policy;
    }

    @ConfigurationAttributeAnnotation(value="policy",appendText="")
    public final void setPolicy(String value)
    {
        policy = value;
    }


    @ConfigurationAttributeAnnotation(value="eviction-ratio",appendText="%")
    public final java.math.BigDecimal getEvictionRatio()
    {
        return evictionRatio;
    }

    @ConfigurationAttributeAnnotation(value="eviction-ratio",appendText="%")
    public final void setEvictionRatio(java.math.BigDecimal value)
    {
        evictionRatio = value;
    }

    @Override
    public final Object clone()
    {
        EvictionPolicy policy = new EvictionPolicy();
        policy.setEnabled(getEnabled());
        policy.setDefaultPriority(getDefaultPriority() != null ? new String(getDefaultPriority()) : null);
        policy.setEvictionRatio(getEvictionRatio());
        policy.setPolicy(getPolicy());
        return policy;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        enabled = reader.ReadBoolean();
        defaultPriority = (String) Common.readAs(reader.ReadObject(), String.class);
        evictionRatio = (BigDecimal) reader.ReadObject();
        policy = (String) Common.readAs(reader.ReadObject(), String.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(enabled);
        writer.WriteObject(defaultPriority);
        writer.WriteObject(evictionRatio);
        writer.WriteObject(policy);
    }

}
