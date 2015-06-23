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

package com.alachisoft.tayzgrid.runtime.util;



import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
/**
 *
 * 
 */
public class HelperFxn {

    /** Creates a new instance of HelperFxn */

    private static final long TICKS_AT_EPOCH = 621355968000000000L;
    private static final long TICKS_PER_MILLISECOND = 10000;


    public HelperFxn() {
    }



    /**
     *
     * @param src byte array
     * @param srcStart
     * @param dest char array
     * @param destStart
     * @param length
     */
    public static void copyTo(byte[] src, int srcStart, char[] dest, int destStart, int length) {
        for (int i = srcStart, j=destStart; i < srcStart + length|| j< destStart+length; i++, j++)
            dest[i] = (char)src[j];
    }

    /**
     *
     * @param src
     * @param srcStart
     * @param dest
     * @param destStart
     * @param length
     */
    public static void copyTo(char[] src, int srcStart, byte[] dest, int destStart, int length) {
        for (int i = srcStart, j=destStart; i < srcStart + length|| j< destStart+length; i++, j++)
            dest[i] = (byte)src[j];
    }

    /**
     *
     * @param src
     * @param srcStart
     * @param dest
     * @param destStart
     * @param length
     */
    public static void copyTo(byte[] src, int srcStart, byte[] dest, int destStart, int length) {
        for (int i = srcStart, j=destStart; i < srcStart + length|| j< destStart+length; i++, j++)
            dest[i] = (byte)src[j];
    }

    /**
     *
     * @param data
     * @throws java.io.UnsupportedEncodingException
     * @return
     */
    public static byte[] getUTF8Bytes(String data) throws UnsupportedEncodingException{
        return data.getBytes("UTF-8");
    }

    /**
     *
     * @param data
     * @throws java.io.UnsupportedEncodingException
     * @return
     */
    public static byte[] getUTF8Bytes(byte[] data) throws UnsupportedEncodingException{
        return String.valueOf(data).getBytes("UTF-8");
    }

    /**
     *
     * @param data
     * @throws java.io.UnsupportedEncodingException
     * @return
     */
    public static byte[] getUTF8Bytes(int data) throws UnsupportedEncodingException{
        return String.valueOf(data).getBytes("UTF-8");
    }

    public static void BlockCopy(byte[] src, int srcOffset, byte[] dst, int dstOffset, int count)
    {
        for(int i = srcOffset; i < count; i++)
        {
            dst[dstOffset+i] = src[i];
        }
    }

    public static long getTicks(Date date)
    {
        long ticks = 0;
         if (date.getTime() != 0) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            cal.setTime(date);
            try {
                NCDateTime ncDate = new NCDateTime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND));
                ticks = ncDate.getTicks();
            }
            catch(ArgumentException ex) {
            }
         }
         return ticks;
    }

    public static long getUTCTicks(Date date)
    {
        long ticks = 0;
         if (date.getTime() != 0) {
             TimeZone utc = TimeZone.getTimeZone("UTC");
            Calendar cal = Calendar.getInstance(utc);
            cal.set(Calendar.MILLISECOND, 0);
            cal.setTime(date);
            try {
                NCDateTime ncDate = new NCDateTime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND));
                ticks = ncDate.getTicks();
            }
            catch(ArgumentException ex) {
            }
         }
         return ticks;
    }

    public static Date getDateFromTicks(long ticks)
    {
          return new NCDateTime(ticks).getDate();
    }


    public static Date getDateFromUTCTicks(long ticks)
    {
        return new NCDateTime(ticks).getLocalizedDate();
    }

    public static Date getCurrentTime()
    {
        return Calendar.getInstance().getTime();
    }

}
