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
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.ArrayList;

public class PortableClass implements Cloneable, java.io.Serializable, InternalCompactSerializable
{

    private String id;
    private String name;
    private String assembly;
    private String type;
    private java.util.ArrayList<PortableAttribute> portableAttributeList;

    public PortableClass()
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

    @ConfigurationAttributeAnnotation(value = "class-handle-id", appendText = "")//Changes for Dom handle-id
    public final String getID()
    {
        return id;
    }

    @ConfigurationAttributeAnnotation(value = "class-handle-id", appendText = "")//Changes for Dom handle-id
    public final void setID(String value)
    {
        id = value;
    }

    @ConfigurationAttributeAnnotation(value = "assembly-name", appendText = "")//Changes for Dom assembly
    public final String getAssembly()
    {
        return assembly;
    }

    @ConfigurationAttributeAnnotation(value = "assembly-name", appendText = "")//Changes for Dom assembly
    public final void setAssembly(String value)
    {
        assembly = value;
    }

    @ConfigurationAttributeAnnotation(value = "language-platform", appendText = "")//Changes for Dom type
    public final String getType()
    {
        return type;
    }

    @ConfigurationAttributeAnnotation(value = "language-platform", appendText = "")//Changes for Dom type
    public final void setType(String value)
    {
        type = value;
    }

    @ConfigurationSectionAnnotation(value = "attribute")
    public final PortableAttribute[] getPortableAttributes()
    {
        if (portableAttributeList != null)
        {
            return portableAttributeList.toArray(new PortableAttribute[0]);
        }
        return null;
    }

    @ConfigurationSectionAnnotation(value = "attribute")
    public final void setPortableAttributes(Object value)
    {
        if (portableAttributeList == null)
        {
            portableAttributeList = new java.util.ArrayList<PortableAttribute>();
        }

        portableAttributeList.clear();
        if (value != null)
        {
            Object[] objs = (Object[]) value;
            for (int i = 0; i < objs.length; i++)
            {
                portableAttributeList.add((PortableAttribute) objs[i]);
            }
        }
    }

    public final java.util.ArrayList<PortableAttribute> getPortableAttributeList()
    {
        return portableAttributeList;
    }

    public final void setPortableAttributeList(java.util.ArrayList<PortableAttribute> value)
    {
        portableAttributeList = value;
    }

    @Override
    public final Object clone()
    {
        PortableClass portableClass = new PortableClass();
        return portableClass;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        id = Common.as(reader.ReadObject(), String.class);
        name = Common.as(reader.ReadObject(), String.class);
        assembly = Common.as(reader.ReadObject(), String.class);
        type = Common.as(reader.ReadObject(), String.class);
        setPortableAttributes(Common.as(reader.ReadObject(), PortableAttribute[].class));
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(id);
        writer.WriteObject(name);
        writer.WriteObject(assembly);
        writer.WriteObject(type);
        writer.WriteObject(getPortableAttributes());
    }
}
