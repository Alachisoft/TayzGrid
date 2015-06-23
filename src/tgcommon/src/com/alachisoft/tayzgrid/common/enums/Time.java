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
package com.alachisoft.tayzgrid.common.enums;

public enum Time
{

    SEC, //second
    mSEC, //milliSecond
    uSEC, //microSecond
    nSEC;   //nanoSecond

    /**
     *
     * @param time value to be converted
     * @param unit current unit of time
     * @return
     */
    public static double toNanoSeconds(long time, Time unit)
    {
        if (unit.equals(Time.SEC))
        {
            return time * 1000000000.0;
        }
        else if (unit.equals(Time.mSEC))
        {
            return time * 1000000.0;
        }
        else if (unit.equals(Time.uSEC))
        {
            return time * 1000.0;
        }        
        else
        {
            return time * 1.0;
        }
    }
    
        /**
     *
     * @param time value to be converted
     * @param unit current unit of time
     * @return
     */
    public static double toMicroSeconds(long time, Time unit)
    {
        if (unit.equals(Time.SEC))
        {
            return time * 1000000.0;
        }    
        else if(unit.equals(Time.mSEC))
        {
            return time * 1000.0;
        }
        else if(unit.equals(Time.nSEC))
        {
            return time / 1000.0;
        }
        else
        {
            return time * 1.0;
        }
    }

    /**
     *
     * @param time value to be converted
     * @param unit current unit of time
     * @return
     */
    public static double toMilliSeconds(long time, Time unit)
    {
        if (unit.equals(Time.SEC))
        {
            return time * 1000.0;
        }
        else if (unit.equals(Time.uSEC))
        {
            return time / 1000.0;
        }
        else if (unit.equals(Time.nSEC))
        {
            return time / 1000000.0;
        }        
        else
        {
            return time * 1.0;
        }
    }

    /**
     *
     * @param time value to be converted
     * @param unit current unit of time
     * @return
     */
    public static double toSeconds(long time, Time unit)
    {
        if (unit.equals(Time.mSEC))
        {
            return time / 1000.0;
        }
        else if (unit.equals(Time.nSEC))
        {
            return time / 1000000000.0;
        }
        else if (unit.equals(Time.uSEC))
        {
            return time / 1000000.0;
        }
        else
        {
            return time * 1.0;
        }
    }
}
