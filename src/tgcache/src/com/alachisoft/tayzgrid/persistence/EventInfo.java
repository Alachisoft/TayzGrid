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

package com.alachisoft.tayzgrid.persistence;

import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

public class EventInfo implements ICompactSerializable, Serializable
{
    private Object _key;
    private java.util.ArrayList<String> _clientIds = new java.util.ArrayList<String>();
    private Object _value;
    private BitSet _flag;
    private ItemRemoveReason _reason;
    private java.util.ArrayList _cbInfoList;

    public final java.util.ArrayList getCallBackInfoList()
    {
        return _cbInfoList;
    }
    public final void setCallBackInfoList(java.util.ArrayList value)
    {
        _cbInfoList = value;
    }

    public final Object getKey()
    {
        return _key;
    }
    public final void setKey(Object value)
    {
        _key = value;
    }

    public final java.util.ArrayList<String> getClientIds()
    {
        return _clientIds;
    }
    public final void setClientIds(java.util.ArrayList<String> value)
    {
        _clientIds = value;
    }

    public final Object getValue()
    {
        return _value;
    }
    public final void setValue(Object value)
    {
        _value = value;
    }   

    public final BitSet getFlag()
    {
        return _flag;
    }
    public final void setFlag(BitSet value)
    {
        _flag = value;
    }

    public final ItemRemoveReason getReason()
    {
        return _reason;
    }
    public final void setReason(ItemRemoveReason value)
    {
        _reason = value;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
          writer.writeObject(_cbInfoList);
          writer.writeObject(_clientIds);
          writer.writeObject(_flag);
          writer.writeObject(_key);
          writer.writeInt(_reason.getValue());
          writer.writeObject(_value);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
          
        _cbInfoList = (java.util.ArrayList)reader.readObject();
        _clientIds = (java.util.ArrayList<String>)reader.readObject();
        _flag = (BitSet)reader.readObject();
        _key = reader.readObject();
        _reason = (ItemRemoveReason)reader.readObject();
        _value = (java.util.ArrayList)reader.readObject();
    }
}


