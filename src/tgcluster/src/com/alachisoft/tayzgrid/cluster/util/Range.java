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

package com.alachisoft.tayzgrid.cluster.util;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

 
/**
 * Represents a range of messages that need retransmission. Contains the first and last seqeunce numbers. <p><b>Author:</b> Chris Koiak, Bela Ban</p> <p><b>Date:</b> 12/03/2003</p>
 */
public class Range implements ICompactSerializable, Serializable
{

    public long low = - 1; // first msg to be retransmitted
    public long high = - 1; // last msg to be retransmitted

    /**
     * For externalization
     */
    public Range()
    {
    }

    public Range(long low, long high)
    {
        this.low = low;
        this.high = high;
    }

    @Override
    public String toString()
    {
        return "[" + low + " : " + high + ']';
    }

    //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
    @Override
    public void deserialize(CacheObjectInput reader) throws IOException
    {
        low = reader.readLong();
        high = reader.readLong();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeLong(low);
        writer.writeLong(high);
    }
    //</editor-fold>
}
