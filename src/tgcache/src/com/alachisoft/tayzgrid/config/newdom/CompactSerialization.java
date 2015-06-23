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

public class CompactSerialization implements Cloneable, InternalCompactSerializable
{

    private CompactClass[] _compactClass;
    private java.util.ArrayList<CompactClass> _compactClassList;

    @ConfigurationSectionAnnotation(value = "compact-class")
    public final CompactClass[] getCompactClassListAsArray()
    {
        if (_compactClassList != null)
        {
            return _compactClassList.toArray(new CompactClass[0]);
        }
        return null;
    }

    @ConfigurationSectionAnnotation(value = "compact-class")
    public final void setCompactClassListAsArray(Object value)
    {
        if (_compactClassList == null)
        {
            _compactClassList = new java.util.ArrayList<CompactClass>();
        }

        _compactClassList.clear();
        if (value != null)
        {
            Object[] objs = (Object[]) value;
            for (int i = 0; i < objs.length; i++)
            {
                _compactClassList.add((CompactClass) objs[i]);
            }
        }
    }

    public final java.util.ArrayList<CompactClass> getCompactClassList()
    {
        return _compactClassList;
    }

    public final void setCompactClassList(java.util.ArrayList<CompactClass> value)
    {
        _compactClassList = value;
    }

    @Override
    public final Object clone()
    {
        CompactSerialization serialization = new CompactSerialization();
        serialization.setCompactClassListAsArray(getCompactClassListAsArray() != null ? (CompactClass[]) getCompactClassListAsArray().clone() : null);
        return serialization;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _compactClass = Common.as(reader.ReadObject(), CompactClass[].class);
        setCompactClassListAsArray(Common.as(reader.ReadObject(), CompactClass[].class));
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_compactClass);
        writer.WriteObject(getCompactClassListAsArray());
    }
}
