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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

public class DeleteQueryResultSet implements ICompactSerializable, Serializable
{
    /**  List of keys which are dependiong on this item. 
    */
   
    private java.util.HashMap _keysEffected = new java.util.HashMap();
    private int _keysEffectedCount;

   

    public final java.util.HashMap getKeysEffected()
    {
        return _keysEffected;
    }
    public final void setKeysEffected(java.util.HashMap value)
    {
        synchronized (this)
        {
                _keysEffected = value;
        }
    }

    public final int getKeysEffectedCount()
    {
        return _keysEffectedCount;
    }
    public final void setKeysEffectedCount(int value)
    {
        _keysEffectedCount += value;
    }

    public final Object[] getRemoveKeys()
    {
       
        
        Object[] keys = new Object[_keysEffected.size()];
        System.arraycopy(_keysEffected.keySet().toArray(new Object[0]), 0, keys, 0, _keysEffected.size());
        
        return keys;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
     
        writer.writeObject(_keysEffected);
        writer.write(_keysEffectedCount);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
        
        _keysEffected = (java.util.HashMap)reader.readObject();
        _keysEffectedCount = reader.readInt();
    }
}
