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

/**
 * Enumeration that defines the update operation on cache can update data source
 */
public enum DataSourceUpdateOptions
{

    /**
     * Do not update data source
     */
    None(0),
    /**
     * Update data source synchronously
     */
    WriteThru(1),
    /**
     * Update data source asynchronously
     */
    WriteBehind(2);
    private int intValue;
    private static java.util.HashMap<Integer, DataSourceUpdateOptions> mappings;

    private static java.util.HashMap<Integer, DataSourceUpdateOptions> getMappings()
    {
        if (mappings == null)
        {
            synchronized (DataSourceUpdateOptions.class)
            {
                if (mappings == null)
                {
                    mappings = new java.util.HashMap<Integer, DataSourceUpdateOptions>();
                }
            }
        }
        return mappings;
    }

    private DataSourceUpdateOptions(int value)
    {
        intValue = value;
        DataSourceUpdateOptions.getMappings().put(value, this);
    }

    public int getValue()
    {
        return intValue;
    }

    public static DataSourceUpdateOptions forValue(int value)
    {
        return getMappings().get(value);
    }
}
