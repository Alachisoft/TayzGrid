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

package com.alachisoft.tayzgrid.cluster.blocks;

public class BinaryMessage
{


    private byte[] buffer;
    private Object[] userPayLoad;
    private java.util.Date _time = new java.util.Date();

    public BinaryMessage(byte[] buf, Object[] userpayLoad)
    {
        buffer = buf;
        userPayLoad = userpayLoad;
    }


    public final byte[] getBuffer()
    {
        return buffer;
    }

    public final Object[] getUserPayLoad()
    {
        return userPayLoad;
    }

    public final int getSize()
    {
        int size = 0;
        if (buffer != null)
        {
            size += buffer.length;
        }
        if (userPayLoad != null)
        {
            for (int i = 0; i < userPayLoad.length; i++)
            {
                Object tempVar = userPayLoad[i];

                byte[] tmp = (byte[]) ((tempVar instanceof byte[]) ? tempVar : null);
                if (tmp != null)
                {
                    size += tmp.length;
                }
            }
        }
        return size;
    }
}
