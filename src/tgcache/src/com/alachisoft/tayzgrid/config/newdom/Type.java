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

public class Type implements Cloneable, java.io.Serializable, InternalCompactSerializable
{

    private String id, name;
    private boolean portable = false;
    private PortableClass[] portableClasses;
    private java.util.ArrayList<PortableClass> portableClassList;
    private AttributeListUnion attrbiuteList;

    public Type()
    {
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

    @ConfigurationAttributeAnnotation(value = "handle", appendText = "")
    public final String getName()
    {
        return name;
    }

    @ConfigurationAttributeAnnotation(value = "handle", appendText = "")
    public final void setName(String value)
    {
        name = value;
    }

    @ConfigurationAttributeAnnotation(value = "portable", appendText = "")
    public final boolean getPortable()
    {
        return portable;
    }

    @ConfigurationAttributeAnnotation(value = "portable", appendText = "")
    public final void setPortable(boolean value)
    {
        portable = value;
    }

    @ConfigurationSectionAnnotation(value = "all-attribute-list") //Changes for Dom attribute-list
    public final AttributeListUnion getAttributeList()
    {
        return attrbiuteList;
    }

    @ConfigurationSectionAnnotation(value = "all-attribute-list")//Changes for Dom attribute-list
    public final void setAttributeList(AttributeListUnion value)
    {
        attrbiuteList = value;
    }

    @ConfigurationSectionAnnotation(value = "sharing-class")//Changes for Dom class
    public final PortableClass[] getPortableClasses()
    {
        if (portableClassList != null)
        {
            return portableClassList.toArray(new PortableClass[0]);
        }
        return null;
    }

    @ConfigurationSectionAnnotation(value = "sharing-class")//Changes for Dom class
    public final void setPortableClasses(Object[] portableClasses)
    {
        if (portableClassList == null)
        {
            portableClassList = new java.util.ArrayList<PortableClass>();
        }

        portableClassList.clear();
        if (portableClasses != null)
        {
            for (Object portableClass : portableClasses)
            {
                portableClassList.add((PortableClass) portableClass);
            }
        }
    }

    public final java.util.ArrayList<PortableClass> getPortableClassList()
    {
        return portableClassList;
    }

    public final void setPortableClassList(java.util.ArrayList<PortableClass> value)
    {
        portableClassList = value;
    }

    @Override
    public final Object clone()
    {
        Type type = new Type();
        type.setID(getID() != null ? new String(getID()) : null);
        type.setName(getName() != null ? new String(getName()) : null);
        type.setPortable(this.getPortable());
        type.setPortableClasses(getPortableClasses() != null ? (PortableClass[]) getPortableClasses().clone() : null);
        type.setAttributeList(getAttributeList() != null ? (AttributeListUnion) getAttributeList().clone() : null);
        return type;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        id = Common.as(reader.ReadObject(), String.class);
        name = Common.as(reader.ReadObject(), String.class);
        portable = reader.ReadBoolean();
        portableClasses = Common.as(reader.ReadObject(), PortableClass[].class);
        setPortableClasses(Common.as(reader.ReadObject(), PortableClass[].class));
        attrbiuteList = Common.as(reader.ReadObject(), AttributeListUnion.class);

    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(id);
        writer.WriteObject(name);
        writer.Write(portable);
        writer.WriteObject(portableClasses);
        writer.WriteObject(getPortableClasses());
        writer.WriteObject(attrbiuteList);
    }
}
