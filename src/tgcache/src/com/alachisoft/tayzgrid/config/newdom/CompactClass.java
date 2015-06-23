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
import com.alachisoft.tayzgrid.serialization.util.ConfigAttributes;
import java.util.ArrayList;
import java.util.HashMap;

public class CompactClass implements Cloneable, InternalCompactSerializable
{

    private String id;
    private String name;
    private String assembly;
    private String type;
    private boolean isGeneric = false;
    private String genericId;
    private String noOfArgs;
    private boolean portable = false;
    private java.util.HashMap<String,Attrib> nonCompactFields;

    public CompactClass()
    {
        nonCompactFields=new HashMap<String, Attrib>();
    }
    @ConfigurationSectionAnnotation(value = ConfigAttributes.CompactSerialization.nonCompactFields)
    public final void setNonCompactFields(Object value)
    {
    
        nonCompactFields.clear();
        Object[] objs=(Object[]) value;
        Attrib attrib;
        for(int index=0;index<objs.length;index++)        
        {
            attrib=(Attrib)objs[index];
            nonCompactFields.put(attrib.getName(), attrib);
        }        
    }
    
    @ConfigurationSectionAnnotation(value = ConfigAttributes.CompactSerialization.nonCompactFields)
    public final Attrib[] getNonCompactFields()
    {
        Attrib[] attribs=new Attrib[nonCompactFields.size()];
        nonCompactFields.values().toArray(attribs);        
        
        return attribs;
    }
    
    public final java.util.HashMap<String, Attrib> getNonCompactFieldsTable()
    {
        return nonCompactFields;
    }

    public final void setNonCompactFieldsTable(java.util.HashMap<String, Attrib> value)
    {
        nonCompactFields = value;
    }
    
    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.id, appendText = "")
    public final String getID()
    {
        return id;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.id, appendText = "")
    public final void setID(String value)
    {
        id = value;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.name, appendText = "") //Changes for Dom name
    public final String getName()
    {
        return name;
    }

    @ConfigurationAttributeAnnotation(value =  ConfigAttributes.CompactSerialization.name, appendText = "") //Changes for Dom name
    public final void setName(String value)
    {
        name = value;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.assembly, appendText = "") //Changes for Dom assembly
    public final String getAssembly()
    {
        return assembly;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.assembly, appendText = "") //Changes for Dom assembly
    public final void setAssembly(String value)
    {
        assembly = value;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.type, appendText = "") //Changes for Dom type
    public final String getType()
    {
        return type;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.type, appendText = "") //Changes for Dom type
    public final void setType(String value)
    {
        type = value;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.isGeneric, appendText = "")
    public final boolean getIsGeneric()
    {
        return isGeneric;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.isGeneric, appendText = "")
    public final void setIsGeneric(boolean value)
    {
        isGeneric = value;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.genericId, appendText = "")
    public final String getGenericId()
    {
        return genericId;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.genericId, appendText = "")
    public final void setGenericId(String value)
    {
        genericId = value;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.noOfArgs, appendText = "")
    public final String getNumberOfArgs()
    {
        return noOfArgs;
    }

    @ConfigurationAttributeAnnotation(value = ConfigAttributes.CompactSerialization.noOfArgs, appendText = "")
    public final void setNumberOfArgs(String value)
    {
        noOfArgs = value;
    }
    private GenericArgumentType[] _genericArgumentType;
    private java.util.ArrayList<GenericArgumentType> _genericArgumentTypeList;

    @ConfigurationSectionAnnotation(value = ConfigAttributes.CompactSerialization.argumentsType)
    public final GenericArgumentType[] getGenericArgumentTypes()
    {
        if (_genericArgumentTypeList != null)
        {
            return _genericArgumentTypeList.toArray(new GenericArgumentType[0]);
        }
        return null;
    }

    @ConfigurationSectionAnnotation(value = ConfigAttributes.CompactSerialization.argumentsType)
    public final void setGenericArgumentTypes(Object[] value)
    {
        if (_genericArgumentTypeList == null)
        {
            _genericArgumentTypeList = new java.util.ArrayList<GenericArgumentType>();
        }

        _genericArgumentTypeList.clear();
        if (value != null)
        {
            for (Object obj : value)
            {
                _genericArgumentTypeList.add((GenericArgumentType) obj);
            }
        }
    }

    public final java.util.ArrayList<GenericArgumentType> getGenericArgumentTypeList()
    {
        return _genericArgumentTypeList;
    }

    public final void setGenericArgumentTypeList(java.util.ArrayList<GenericArgumentType> value)
    {
        _genericArgumentTypeList = value;
    }

    public final boolean getPortable()
    {
        return portable;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException
    {
        CompactClass compactClass = new CompactClass();
        compactClass.setName(getName() != null ? (String) new String(getName()) : null);
        compactClass.setID(getID() != null ? (String) new String(getID()) : null);
        compactClass.setType(getType() != null ? (String) new String(getType()) : null);
        compactClass.setIsGeneric(getIsGeneric());
        compactClass.setGenericId(getGenericId() != null ? (String) new String(getGenericId()) : null);
        compactClass.setNumberOfArgs(getNumberOfArgs() != null ? (String) new String(getNumberOfArgs()) : null);
        compactClass.setGenericArgumentTypes(getGenericArgumentTypes() != null ? (GenericArgumentType[]) getGenericArgumentTypes().clone() : null);
        compactClass.setNonCompactFields(getNonCompactFields()!=null?(Attrib[])getNonCompactFields().clone():null);
        return compactClass;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        id = (String) Common.readAs(reader.ReadObject(), String.class);
        name = (String) Common.readAs(reader.ReadObject(), String.class);
        assembly = (String) Common.readAs(reader.ReadObject(), String.class);
        type = (String) Common.readAs(reader.ReadObject(), String.class);
        isGeneric = reader.ReadBoolean();
        genericId = (String) Common.readAs(reader.ReadObject(), String.class);
        noOfArgs = (String) Common.readAs(reader.ReadObject(), String.class);
        portable = reader.ReadBoolean();
        nonCompactFields=Common.as(reader.ReadObject(),new HashMap<java.lang.String,Attrib>().getClass());
        _genericArgumentTypeList=Common.as(reader.ReadObject(),new ArrayList<GenericArgumentType>().getClass());
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(id);
        writer.WriteObject(name);
        writer.WriteObject(assembly);
        writer.WriteObject(type);
        writer.Write(isGeneric);
        writer.WriteObject(genericId);
        writer.WriteObject(noOfArgs);
        writer.Write(portable);
        writer.WriteObject(nonCompactFields);
        writer.WriteObject(_genericArgumentTypeList);
    }
}
