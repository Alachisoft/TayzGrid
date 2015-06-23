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
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class AttributeListUnion implements Cloneable, java.io.Serializable, InternalCompactSerializable
{

    private java.util.ArrayList<PortableAttribute> portableAttributeList;

    public AttributeListUnion()
    {
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
    public final void setPortableAttributes(Object[] protableAttributes)
    {
        if (portableAttributeList == null)
        {
            portableAttributeList = new java.util.ArrayList<PortableAttribute>();
        }

        portableAttributeList.clear();
        if (protableAttributes != null)
        {
            for (Object portableAttribute : protableAttributes)
            {
                portableAttributeList.add((PortableAttribute) portableAttribute);
            }
        }
    }

    @Override
    public final Object clone()
    {
        AttributeListUnion attributeList = new AttributeListUnion();
        attributeList.setPortableAttributes(getPortableAttributes() != null ? (PortableAttribute[]) getPortableAttributes().clone() : null);
        return attributeList;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        setPortableAttributes(Common.as(reader.ReadObject(), PortableAttribute[].class));
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(getPortableAttributes());
    }
}
