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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.memcachedencoding;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public final class BinaryConverter {
 

    public static short ToUInt16(byte[] buffer, int offset) {
 
        return (short) ((buffer[offset] << 8) | (buffer[offset + 1]));
    }

 
    public static int ToInt32(byte[] data, int offset) {
            return (data[offset] & 0xff) << 24
      | (data[offset + 1] & 0xff) << 16
      | (data[offset + 2] & 0xff) << 8
      | (data[offset + 3] & 0xff);
           
    }
    
    public static long byteAsULong(byte b) {
        return ((long) b) & 0x00000000000000FFL;
    }

 
    public static long ToUInt32(byte[] buffer, int offset) {
 
        long value = (byteAsULong(buffer[offset]) << 24) | (byteAsULong(buffer[offset+1]) << 16) | (byteAsULong(buffer[offset+2]) << 8) | (byteAsULong(buffer[offset+3]));
        return value;
 
    }

 
    public static long ToInt64(byte[] buffer, int offset) {
           return (buffer[offset] & 0xffL) << 56
      | (buffer[offset + 1] & 0xffL) << 48
      | (buffer[offset + 2] & 0xffL) << 40
      | (buffer[offset + 3] & 0xffL) << 32
      | (buffer[offset + 4] & 0xffL) << 24
      | (buffer[offset + 5] & 0xffL) << 16
      | (buffer[offset + 6] & 0xffL) << 8
      | (buffer[offset + 7] & 0xffL);
 
    }

    /**
     * Converts sequence of bytes to string using ASCII encoding.
     *
     * @param buffer Bytes to be converted
     * @param index Starting index in buffer
     * @param count Number of bytes o be decoded
     * @return
     */
 
    public static String GetString(byte[] data, int index, int count) {
        try {
            return new String(data, index, count, "US-ASCII");//Encoding.ASCII.GetString(buffer, index, count);
        } catch (UnsupportedEncodingException ex) {
            return null;
             
        }
    }

    /**
     * Converts sequence of bytes to string using ASCII encoding
     *
     * @param buffer Bytes to be converted
     * @return
     */
 
    public static String GetString(byte[] data) {
        try {
            return new String(data, "US-ASCII");//Encoding.ASCII.GetString(buffer);
        } catch (UnsupportedEncodingException ex) {
            return null;
             
        }
    }

 
    public static byte[] GetBytes(long value) {
 
        byte[] bytes = new byte[8];
 
        bytes[0] = (byte) (value >> 56);
 
        bytes[1] = (byte) (value >> 48);
 
        bytes[2] = (byte) (value >> 40);
 
        bytes[3] = (byte) (value >> 32);
 
        bytes[4] = (byte) (value >> 24);
 
        bytes[5] = (byte) (value >> 16);
 
        bytes[6] = (byte) (value >> 8);
 
        bytes[7] = (byte) (value & 255);

        return bytes;
    }

 
    public static byte[] GetBytes(int value) {
 
        byte[] bytes = new byte[4];
 
        bytes[0] = (byte) (value >> 24);
 
        bytes[1] = (byte) (value >> 16);
 
        bytes[2] = (byte) (value >> 8);

        bytes[3] = (byte) (value & 255);
        return bytes;
    }


    public static byte[] GetBytes(short value) {

        byte[] bytes = new byte[2];

        bytes[0] = (byte) (value >> 8);

        bytes[1] = (byte) (value & 255);
        return bytes;
    }


    public static byte[] GetBytes(String value) {
        try {
            if (value == null) {
                return new byte[]{};
            }
            return value.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            return null;
            
        }
    }
}