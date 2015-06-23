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

package com.alachisoft.tayzgrid.common.stats;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * HPTime represents the time based on the ticks of High Performance coutners. It is a relative time not synchronized with system time. The time accuracy is upto micro seconds.
 *
 */
public class HPTime implements java.lang.Comparable, InternalCompactSerializable
{
	private int _hr;
	private int _min;
	private int _sec;
	private int _mlSec;
	private int _micSec;

	private static long _frequency;
	private static long _baseTimeInNanoSeconds;
	private static Object _synObj = new Object();
	private static String _col = ":";

	private double _baseTime;
	private double _baseRem;

	static {
		synchronized (_synObj) {
                    _baseTimeInNanoSeconds = System.nanoTime();
		}
	}

	/**
	 Gets the hours component of the time of this instance.
	*/
	public final int getHours() {
		return _hr;
	}

	/**
	 Gets the hours component of the time of this instance.
	*/
	public final int getMinutes() {
		return _min;
	}

	/**
	 Gets the Secnds component of the time of this instance.
	*/
	public final int getSeconds() {
		return _sec;
	}

	/**
	 Gets the MilliSecond component of the time of this instance.
	*/
	public final int getMilliSeconds() {
		return _mlSec;
	}

	/**
	 Gets the MicroSeconds component of the time of this instance.
	*/
	public final int getMicroSeconds() {
		return _micSec;
	}

	public final double getBaseTime() {
		return _baseTime;
	}
	public final void setBaseTime(double value) {
		_baseRem = value;
	}

	public final double getServerTicks() {
		return _baseRem;
	}

	/**
	 Gets current HP time
	*/

    public final HPTime getCurrentTime()
    {
       		try {

			double rem = 0;
			long currentTimeInNano = 0;
			long diff;

			HPTime time = new HPTime();
			currentTimeInNano = System.nanoTime();

			diff = currentTimeInNano - _baseTimeInNanoSeconds;
			rem = ((double)diff /(double) 1000000);

			_baseTime = rem;
			time._baseTime = rem;
			rem += _baseRem;


			time._hr = (int)(rem / 3600000);
			rem = rem - (time._hr * 3600000);

			time._min = (int)rem / 60000;
			rem = rem - (time._min * 60000);

			time._sec = (int)rem / 1000;
			rem = rem - (time._sec * 1000);

			time._mlSec = (int)rem;
			rem = (rem - (double)time._mlSec) * 1000;
			time._micSec = (int)rem;

			return time;
		} finally {
		}
    }

	/**
	 Gets current HP time
	*/

    public static HPTime getNow()
    {
        double rem = 0;
        long currentTimeInNano = 0;
        long diff;

        HPTime time = new HPTime();
        currentTimeInNano = System.nanoTime();

        diff = currentTimeInNano - _baseTimeInNanoSeconds;
        rem = ((double)diff /(double) 1000000);

        time._hr = (int) (rem / 3600000);
        rem = rem - (time._hr * 3600000);

        time._min = (int) rem / 60000;
        rem = rem - (time._min * 60000);

        time._sec = (int) rem / 1000;
        rem = rem - (time._sec * 1000);

        time._mlSec = (int) rem;
        rem = (rem - (double) time._mlSec) * 1000;
        time._micSec = (int) rem;

        return time;
    }
	/**
	 Gets the string representation of the current instance of HP time.

	 @return
	*/
	@Override
	public String toString() {
		return _hr % 24 + _col + _min % 60 + _col + _sec % 60 + _col + (long)_mlSec + _col + _micSec;
	}

	/**
	 Gets the string representation of the current instance of HP time.

	 @return
	*/
	public final String ToAbsoluteTimeString() {
		return _hr + _col + _min + _col + _sec + _col + (long)_mlSec + _col + _micSec;
	}



    public final int compareTo(Object obj)
    {
        if (obj instanceof HPTime)
        {
            HPTime other = (HPTime) obj;
            int result = (new Integer(this.getHours())).compareTo(other.getHours());
            if (result == 0)
            {
                result = (new Integer(this.getMinutes())).compareTo(other.getMinutes());
                if (result == 0)
                {
                    result = (new Integer(this.getSeconds())).compareTo(other.getSeconds());
                    if (result == 0)
                    {
                        result = (new Integer(this.getMilliSeconds())).compareTo(other.getMilliSeconds());
                        if (result == 0)
                        {
                            return (new Integer(this.getMicroSeconds())).compareTo(other.getMicroSeconds());
                        }
                        return result;
                    }
                    return result;
                }
                return result;
            }
            return result;
        }
        throw new IllegalArgumentException("Object is not HPTime");
    }



    @Override
    public final void Deserialize(com.alachisoft.tayzgrid.serialization.standard.io.CompactReader reader) throws IOException,ClassNotFoundException
    {
		_hr = reader.ReadInt32();
		_micSec = reader.ReadInt32();
		_min = reader.ReadInt32();
		_mlSec = reader.ReadInt32();
		_sec = reader.ReadInt32();
    }

    @Override
    public final void Serialize(com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter writer)throws IOException
    {
		writer.Write(_hr);
		writer.Write(_micSec);
		writer.Write(_min);
		writer.Write(_mlSec);
		writer.Write(_sec);
    }

}
