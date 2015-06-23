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

package com.alachisoft.tayzgrid.common;

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class to help with common tasks.
 */
public class AppUtil
{
    public final static int TotalBuckets=1000;
    public final static String DeployedAssemblyDir = "deploy/";

    public static String getInstallDir()
    {
        String installPath = ServicePropValues.INSTALLDIR_DIR;
        if (installPath != null && !installPath.equals(""))
        {
            return installPath;
        }

        String path = ServicePropValues.getTGHome();//GetAppSetting("InstallDir");
        if (path == null || path.length() == 0)
        {
            return null;
        }
        return path;
    }
    
    //@24March2015 --TODO-- key must be ISizeable
    public static int getKeySize(Object key){
        if(key != null && key instanceof String){
            return (2*((String)key).length()) + 24;
        }
        return 0;
    }

    public static String GetAppSetting(String key)
    {
        return GetAppSetting("", key);
    }

    public static String GetAppSetting(String section, String key)
    {
        return "";
    }
    private static java.util.Date START_DT;//; = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();

    static
    {
        try
        {
            int x = 0;
            NCDateTime time = new NCDateTime(2004, 11, 30, 0, 0, 0, 0);
            START_DT = time.getLocalizedDate();

            int y = 0;
        }
        catch (Exception e)
        {
        }
    }

    public static int DiffSeconds(java.util.Date dt) throws ArgumentException
    {

        long timespan = HelperFxn.getUTCTicks(dt) - new NCDateTime(START_DT).getTicks();
        int interval = (int) new TimeSpan(timespan).getTotalSeconds();
        return interval;
    }
    //without converting date in UTC

    public static int fixedExpDiffSeconds(java.util.Date dt) throws ArgumentException
    {
        long timespan = new NCDateTime(dt).getTicks() - new NCDateTime(START_DT).getTicks();
        int interval = (int) new TimeSpan(timespan).getTotalSeconds();
        return interval;
    }

    public static int DiffMilliseconds(java.util.Date dt) throws ArgumentException
    {

        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar c = Calendar.getInstance(utc);
        c.clear();
        c.set(Calendar.MILLISECOND, 0);
        c.setTime((Date) dt);
        NCDateTime ncdt = null;
        ncdt = new NCDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));


        NCDateTime date = null;
        NCDateTime start_ncd = null;
        start_ncd = new NCDateTime(START_DT);
        date = new NCDateTime(dt);

        int interval = date.getMilliseconds() - start_ncd.getMilliseconds();
        return interval;

    }

    public static long DiffTicks(java.util.Date dt) throws ArgumentException
    {
        return (HelperFxn.getUTCTicks(dt) - new NCDateTime(START_DT).getTicks());
    }

    public static java.util.Date GetDateTime(int absoluteTime)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(START_DT);
        cal.add(Calendar.SECOND, absoluteTime);
       return cal.getTime();
    }

    public static int hashCode(Object obj)
    {
        if (obj instanceof String){
        char[] array = ((String)obj).toCharArray();
        int[] intArray = new int[(int) Math.ceil((double) (array.length) / 2)];

        for (int i = 0, j = 0; i < intArray.length; i++)
        {
            char[] toInt = new char[2];
            if (j < array.length)
            {
                toInt[0] = array[j++];
            }
            if (j < array.length)
            {
                toInt[1] = array[j++];
            }
            intArray[i] = charToInt(toInt);
        }

        int num = 0x15051505;
        int num2 = num;

        for (int i = array.length, j = 0; i > 0; i -= 4, j += 2)
        {

            num = (((num << 5) + num) + (num >> 0x1b)) ^ intArray[j];
            if (i <= 2)
            {
                break;
            }
            num2 = (((num2 << 5) + num2) + (num2 >> 0x1b)) ^ intArray[j + 1];
        }
        return (num + (num2 * 0x5d588b65));}
        else
            return obj.hashCode();
    }

    private static int charToInt(char[] array)
    {
        return (array[0] | (array[1] << 16));
    }
}