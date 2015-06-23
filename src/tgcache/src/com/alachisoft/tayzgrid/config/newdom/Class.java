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
import java.util.HashMap;

public class Class implements Cloneable, InternalCompactSerializable
{

    private String id, name;
    private java.util.HashMap<String, Attrib> attributesTable;

    public Class()
    {
        attributesTable = new java.util.HashMap<String, Attrib>();
    }

    @ConfigurationAttributeAnnotation(value = "id", appendText = "")
    public final String getID()
    {
        return id;
    }

    @ConfigurationAttributeAnnotation(value = "id", appendText = "")
    public final void setID(String value)
    {
        id = value;
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

    @ConfigurationSectionAnnotation(value = "query-attributes") //Changes for Dom attrib
    public final Attrib[] getAttributes()
    {
        Attrib[] attribs = new Attrib[attributesTable.size()];
        attributesTable.values().toArray(attribs);
        return attribs;
    }

    @ConfigurationSectionAnnotation(value = "query-attributes") //Changes for Dom attrib
    public final void setAttributes(Object value)
    {
        attributesTable.clear();
        Object[] objs = (Object[]) value;
        Attrib attrib;
        for (int i = 0; i < objs.length; i++)
        {
            attrib = (Attrib) objs[i];
            attributesTable.put(attrib.getName(), attrib);
        }
    }

    public final java.util.HashMap<String, Attrib> getAttributesTable()
    {
        return attributesTable;
    }

    public final void setAttributesTable(java.util.HashMap<String, Attrib> value)
    {
        attributesTable = value;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException
    {
        Class cls = new Class();
        cls.setID(getID() != null ? new String(getID()) : null);
        cls.setName(getName() != null ? new String(getName()) : null);
        cls.setAttributes(getAttributes() != null ? (Attrib[]) getAttributes().clone() : null);
        return cls;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        id = Common.as(reader.ReadObject(), String.class);
        name = Common.as(reader.ReadObject(), String.class);
        attributesTable = Common.as(reader.ReadObject(), new HashMap<java.lang.String, Attrib>().getClass());
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(id);
        writer.WriteObject(name);
        writer.WriteObject(attributesTable);
    }
}
