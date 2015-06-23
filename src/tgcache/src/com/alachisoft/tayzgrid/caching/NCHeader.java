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

public class NCHeader
{
    public static byte[] version_id = new byte[]
    {
        (byte) 'N', (byte) 'C', 2, 1, 0
    };

    static
    {
        version_id[4] = (byte) (version_id[0] | version_id[1] | version_id[2] | version_id[3]);
    }
    
    public static String printVersionId(byte[] v)
    {
        StringBuilder sb = new StringBuilder();
        if (v != null)
        {
            int len = getLength();
            if (v.length < len)
            {
                len = v.length;
            }

            for (int i = 0; i < len; i++)
            {
                sb.append((char) v[i]);
            }
        }
        return sb.toString();
    }

    public static int getLength()
    {
        return 5;
    }

    public static boolean CompareTo(byte[] v)
    {
        if (v == null || v.length < version_id.length)
        {
            return false;
        }

        return version_id[0] == v[0] && version_id[1] == v[1] && version_id[2] == v[2] && version_id[3] == v[3] && version_id[4] == v[4];
    }

    public static byte[] getVersion()
    {
        return version_id;
    }
}
