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

package com.alachisoft.tayzgrid.caching.datagrouping;

import com.alachisoft.tayzgrid.common.ISizable;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * This class contains data group information for an object
 */
public class GroupInfo implements Cloneable, ICompactSerializable,java.io.Serializable, ISizable
{

    private String _group = null;
    private String _subGroup = null;

    public GroupInfo()
    {
    }

    public GroupInfo(String group, String subGroup)
    {
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(group))
        {
            _group = group;
        }
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(subGroup))
        {
            _subGroup = subGroup;
        }
    }

    public final String getGroup()
    {
        return _group;
    }

    public final void setGroup(String value)
    {
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(value))
        {
            _group = value;
        }
    }

    public final String getSubGroup()
    {
        return _subGroup;
    }

    public final void setSubGroup(String value)
    {
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(value))
        {
            _subGroup = value;
        }
    }

    public final Object clone()
    {
        return new GroupInfo(_group, _subGroup);
    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _group = (String) reader.readObject();
        _subGroup = (String) reader.readObject();
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(_group);
        writer.writeObject(_subGroup);
    }

    public static GroupInfo ReadGrpInfo(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {        
        Boolean isNull = reader.readBoolean();
        
        if (isNull == true)
            return null;
        
        GroupInfo newInfo = new GroupInfo();
        newInfo.deserialize(reader);
        
        return newInfo;
    }

    public static void WriteGrpInfo(CacheObjectOutput writer, GroupInfo grpInfo) throws IOException
    {
        Boolean isNull = true;
         
        if (grpInfo == null)
            writer.writeBoolean(isNull);
        else
        {
            isNull = false;
            writer.writeBoolean(isNull);
            grpInfo.serialize(writer);
        }
        
        //return;
        //grpInfo.serialize(writer);
    }

    @Override
    public int getSize() {
        int temp = 0;
        temp += MemoryUtil.NetReferenceSize;
        temp += MemoryUtil.NetReferenceSize;
        temp += MemoryUtil.GetInMemoryInstanceSize(temp);
        
        temp += MemoryUtil.getStringSize(this._group);
        temp += MemoryUtil.getStringSize(this._subGroup);
        
        return temp;
    }

    @Override
    public int getInMemorySize() {
        return this.getSize();
    }
}
