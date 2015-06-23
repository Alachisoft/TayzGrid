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

package com.alachisoft.tayzgrid.runtime.events;

import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import java.util.EnumSet;


public enum EventDataFilter
{
    None                (0x0),    
    Metadata            (0x1),
    DataWithMetaData    (0x3);
    
    private int _value;
    
    EventDataFilter(int value)
    {
        this._value = value;        
    }
    
    public int getValue()
    {
        return this._value;
    }
  
    public static EventDataFilter forValue(int value)
    {
        EventDataFilter[] values = EventDataFilter.values();

        for(int i =0; i< values.length; i++){
            EventDataFilter enumVal = values[i];

            if(enumVal._value == value ){
                return enumVal;
            }
        }
        return EventDataFilter.None;
    }
}
