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

package com.alachisoft.tayzgrid.socketserver.util;

import java.nio.charset.Charset;

public final class HelperFxn
{

    /**
     * Converts the byte into string using UTF8Encoding
     *
     * @param buffer buffer containing value to be converted
     * @return
     */
    public static String ToString(byte[] buffer)
    {
        return new String(buffer, 0, buffer.length, Charset.forName("UTF-8"));
    }

    public static String ToStringUni(byte[] buffer)
    {
        return new String(buffer, 0, buffer.length, Charset.forName("UTF-16"));
    }

    public static String ToString(byte[] buffer, int offset, int size)
    {
        return new String(buffer, offset, size, Charset.forName("UTF-8"));
    }

    /**
     * Converts byte array to string using UTF8Encoding
     *
     * @param value value to be converted to byte
     * @return
     */
    public static byte[] ToBytes(String data)
    {
        return new String(data.getBytes(), Charset.forName("UTF-8")).getBytes();

    }

    public static byte[] ToBytesUni(String data)
    {
        return new String(data.getBytes(), Charset.forName("UTF-16")).getBytes();
    }

    /**
     * Converts the specified byte array to int. It is callers responsibilty to ensure that value can be converted to Int32
     *
     * @param buffer
     * @return
     */
    public static int ToInt32(byte[] buffer) throws Exception
    {
        int cInt = 0;
        try
        {
            cInt=Integer.decode(new String(buffer,Charset.forName("UTF-8")));
        }
        catch (Exception e)
        {
            throw e;
        }

        return cInt;
    }

    /**
     * Convert the selected bytes to int
     *
     * @param buffer buffer containing the value
     * @param offset offset from which the int bytes starts
     * @param size number of bytes
     * @return
     */
    public static int ToInt32(byte[] buffer, int offset, int size, String s)throws Exception
    {
        int cInt = 0;
        try
        {
            int buffsize = 0;
            for (int i = 0; i < buffer.length; i++)
            {
                if(buffer[i] != 0)
                    buffsize++;
                else
                    break;
            }
            cInt=Integer.decode(new String(buffer, offset, buffsize,Charset.forName("UTF-8")));
        }
        catch (Exception e)
        {
            throw e;
        }

        return cInt;
    }

    /**
     * Convert the selected bytes to int
     *
     * @param buffer buffer containing the value
     * @param offset offset from which the int bytes starts
     * @param size number of bytes
     * @return
     */
    public static int ToInt32(byte[] buffer, int offset, int size) throws Exception
    {
        int cInt = 0;
        try
        {
            cInt=Integer.decode(new String(buffer, offset, size,Charset.forName("UTF-8")));
        }
        catch (Exception e)
        {
            throw e;
        }

        return cInt;
    }

    /**
     * Copy block of data from source array
     *
     * @param copyFrom Source array
     * @param startIndex Start index in the source array from where copy begins
     * @param endIndex End index, until which the bytes are copied
     * @return Resultant array
     */
    public static byte[] CopyPartial(byte[] copyFrom, int startIndex, int endIndex)
    {
        byte[] copyIn = new byte[endIndex - startIndex];

        for (int i = startIndex, count = 0; i < endIndex; i++, count++)
        {
            copyIn[count] = copyFrom[i];
        }

        return copyIn;
    }

    /**
     * Copy block of data from source array
     *
     * @param copyFrom Source array
     * @param startIndex Start index in the source array from where copy begins
     * @param endIndex End index, until which the bytes are copied
     * @return Resultant array
     */
    public static void CopyPartial(byte[] copyFrom, byte[] copyTo, int startIndex, int endIndex)
    {
        for (int i = startIndex, count = 0; i < endIndex; i++, count++)
        {
            copyTo[count] = copyFrom[i];
        }
    }

    /**
     * Copy block of data from source array
     *
     * @param copyFrom Source array
     * @param startIndex Start index in the source array from where copy begins
     * @param length Number of bytes to copy
     * @return Resultant array
     */
    public static byte[] CopyTw(byte[] copyFrom, int startIndex, int length)
    {
        byte[] copyIn = new byte[length];
        int loop = length + startIndex;

        for (int i = startIndex, count = 0; i < loop; i++, count++)
        {
            copyIn[count] = copyFrom[i];
        }

        return copyIn;
    }
}
