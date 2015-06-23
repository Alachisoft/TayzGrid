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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

public class OperationID implements ICompactSerializable, Serializable
{
    private String _opID;
    private long _opCounter;
    public OperationID()
    {
    }
    public OperationID(String opID, long opCounter)
    {
            _opID = opID;
            _opCounter = opCounter;
    }
    public final String getOperationId()
    {
            return _opID;
    }
    public final void setOperationId(String value)
    {
            _opID = value;
    }
    public final long getOpCounter()
    {
            return _opCounter;
    }
    public final void setOpCounter(long value)
    {
            _opCounter = value;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException 
    {
            writer.writeLong(this._opCounter);
            writer.writeObject(this._opID);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException 
    {
            this._opCounter = reader.readLong();
            this._opID = (String)reader.readObject();
    }
}
