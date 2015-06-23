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

import com.alachisoft.tayzgrid.common.net.ObjectProvider;
import java.io.Serializable;

/**
 * Used to append messages with stack information <p><b>Author:</b> Chris Koiak, Bela Ban</p> <p><b>Date:</b> 12/03/2003</p>
 */
public abstract class Header implements Serializable
{ //: ICloneable

    public static final long HDR_OVERHEAD = 255; // estimated size of a header (used to estimate the size of the entire msg)

    /**
     * To be implemented by subclasses. Return the size of this object for the serialized version of it. I.e. how many bytes this object takes when flattened into a buffer. This
     * may be different for each instance, or can be the same. This may also just be an estimation. E.g. FRAG uses it on Message to determine whether or not to fragment the
     * message. Fragmentation itself will be accurate, because the entire message will actually be serialized into a byte buffer, so we can determine the exact size.
     */
    public long size()
    {
        return HDR_OVERHEAD;
    }


    /**
     * Default String representation of the HDR
     *
     * @return String representation of the HDR
     */
    @Override
    public String toString()
    {
        //: getClass().FullName converted ot getName() ... have to check wether getName returns fully qualified name or not
        return '[' + getClass().getName() + " HDR]";
    }

    public Object clone(ObjectProvider provider)
    {
        return null;
    }
}
