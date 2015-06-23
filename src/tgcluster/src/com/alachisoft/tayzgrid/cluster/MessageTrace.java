/*
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

package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;


public class MessageTrace implements ICompactSerializable
{

    private String _trace;
    
    private long _timeStamp;


    public MessageTrace(String trace)
    {
        _trace = trace;
 
        _timeStamp = System.currentTimeMillis();
    }

    @Override
    public String toString()
    {
        String toString = "";
        if (_trace != null)
        {
           
            toString = _trace + " : " + _timeStamp;
        }
        return toString;
    }

    @Override
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        Object tempVar = reader.readObject();
        _trace = (String) ((tempVar instanceof String) ? tempVar : null);

       
        _timeStamp = reader.readLong();
     
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(_trace);

   
        writer.writeLong(_timeStamp);
    }

}
