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

public class Attrib implements Cloneable, java.io.Serializable, InternalCompactSerializable
{

    private String id, name, type;

    public Attrib()
    {
    }

    @ConfigurationAttributeAnnotation(value="id",appendText="")
    public final String getID()
    {
        return id;
    }
    @ConfigurationAttributeAnnotation(value="id",appendText="" )
    public final void setID(String value)
    {
        id = value;
    }

    @ConfigurationAttributeAnnotation(value="name",appendText="" )
    public final String getName()
    {
        return name;
    }
    @ConfigurationAttributeAnnotation(value="name",appendText="" )
    public final void setName(String value)
    {
        name = value;
    }

    @ConfigurationAttributeAnnotation(value="data-type",appendText="" )
    public final String getType()
    {
        return type;
    }
    @ConfigurationAttributeAnnotation(value="data-type",appendText="" )
    public final void setType(String value)
    {
        type = value;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException
    {
        Attrib attrib = new Attrib();
        attrib.setID(getID() != null ? (String) new String(getID()) : null);
        attrib.setName(getName() != null ? (String) new String(getName()) : null);
        attrib.setType(getType() != null ? (String) new String(getType()) : null);
        return attrib;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        id = Common.as(reader.ReadObject(), String.class);
        name = Common.as(reader.ReadObject(), String.class);
        type = Common.as(reader.ReadObject(), String.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(id);
        writer.WriteObject(name);
        writer.WriteObject(type);
    }
}
