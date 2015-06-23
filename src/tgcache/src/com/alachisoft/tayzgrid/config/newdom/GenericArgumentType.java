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
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;

public class GenericArgumentType implements Cloneable, java.io.Serializable
{
    private String id;
    private CompactClass[] _genericArgsCompactTypes;
    private java.util.ArrayList<CompactClass> _genericArgsCompactTypeList;

    public GenericArgumentType()
    {
    }

    @ConfigurationAttributeAnnotation(value="id",appendText="")
    public final String getID()
    {
        return id;
    }
   @ConfigurationAttributeAnnotation(value="id",appendText="")
    public final void setID(String value)
    {
        id = value;
    }

    @ConfigurationSectionAnnotation(value="compact-class")
    public final CompactClass[] getGenericArgsCompactTypes()
    {
        if (_genericArgsCompactTypeList != null)
        {
            return _genericArgsCompactTypeList.toArray(new CompactClass[0]);
        }
        return null;
    }
    @ConfigurationSectionAnnotation(value="compact-class")
    public final void setGenericArgsCompactTypes(Object value)
    {
        if (_genericArgsCompactTypeList == null)
        {
            _genericArgsCompactTypeList = new java.util.ArrayList<CompactClass>();
        }

        _genericArgsCompactTypeList.clear();
        if (value != null)
        {
            Object[] objs = (Object[])value;
            for (int i = 0; i < objs.length; i++)
            {
               _genericArgsCompactTypeList.add((CompactClass)objs[i]);
            }
        }
    }

    public final java.util.ArrayList<CompactClass> getGenericArgsCompactTypeList()
    {
        return _genericArgsCompactTypeList;
    }

    public final void setGenericArgsCompactTypeList(java.util.ArrayList<CompactClass> value)
    {
        _genericArgsCompactTypeList = value;
    }

    @Override
    public final Object clone()
    {
        GenericArgumentType genericArgType = new GenericArgumentType();
        genericArgType.setID(getID() != null ? new String(getID()) : null);
        genericArgType.setGenericArgsCompactTypes(getGenericArgsCompactTypes() != null ? (CompactClass[]) getGenericArgsCompactTypes().clone() : null);
        return genericArgType;
    }
}
