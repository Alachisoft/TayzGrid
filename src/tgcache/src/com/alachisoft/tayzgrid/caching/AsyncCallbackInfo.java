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

import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class AsyncCallbackInfo extends CallbackInfo implements InternalCompactSerializable
{

    private int _requestId;

    public AsyncCallbackInfo()
    {
    }

    public AsyncCallbackInfo(int reqid, String clietnid, Object asyncCallback)
    {
        super(clietnid, asyncCallback);
        _requestId = reqid;
    }

    public final int getRequestID()
    {
        return _requestId;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CallbackInfo)
        {
            CallbackInfo other = (CallbackInfo) ((obj instanceof CallbackInfo) ? obj : null);
            if (!getClient().equals(other.getClient()))
            {
                return false;
            }
            if (other.getCallback() instanceof Short && theCallback instanceof Short)
            {
                if ((Short) other.getCallback() != (Short) theCallback)
                {
                    return false;
                }
            }
            else if (other.getCallback() != theCallback)
            {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString()
    {
        String cnt = theClient != null ? theClient : "NULL";
        String cback = theCallback != null ? theCallback.toString() : "NULL";
        return cnt + ":" + cback;
    }
    
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException 
    {
        super.Deserialize(reader);
        _requestId = reader.ReadInt32();
    }
    
    public void Serialize(CompactWriter writer) throws IOException, IOException
    {
        super.Serialize(writer);
        writer.Write(_requestId);
    }
}
