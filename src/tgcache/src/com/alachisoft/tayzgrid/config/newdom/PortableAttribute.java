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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class PortableAttribute implements Cloneable, java.io.Serializable, InternalCompactSerializable
{

    private String name;
    private String type;
    private String order;
    private String _mappedTo;

    public PortableAttribute()
    {
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

    @ConfigurationAttributeAnnotation(value = "data-type", appendText = "")//Changes for Dom type
    public final String getType()
    {
        return type;
    }

    @ConfigurationAttributeAnnotation(value = "data-type", appendText = "")//Changes for Dom type
    public final void setType(String value)
    {
        type = value;
    }

    @ConfigurationAttributeAnnotation(value = "serialization-order", appendText = "")//Changes for Dom order
    public final String getOrder()
    {
        return order;
    }

    @ConfigurationAttributeAnnotation(value = "serialization-order", appendText = "")//Changes for Dom order
    public final void setOrder(String value)
    {
        order = value;
    }

    @Override
    public final Object clone()
    {
        PortableAttribute portableAttribute = new PortableAttribute();
        portableAttribute.setName(getName() != null ? new String(getName()) : null);
        portableAttribute.setType(getType() != null ? new String(getType()) : null);
        portableAttribute.setOrder(getOrder() != null ? new String(getOrder()) : null);
        return portableAttribute;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        name = Common.as(reader.ReadObject(), String.class);
        type = Common.as(reader.ReadObject(), String.class);
        order = Common.as(reader.ReadObject(), String.class);
        _mappedTo = Common.as(reader.ReadObject(), String.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(name);
        writer.WriteObject(type);
        writer.WriteObject(order);
        writer.WriteObject(_mappedTo);
    }
}
