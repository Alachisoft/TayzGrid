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

package com.alachisoft.tayzgrid.common.util;

import com.alachisoft.tayzgrid.common.datastructures.RedBlack.AttributeTypeSize;

public class MemoryUtil {

    //24 Bytes overhead for every .net class
    public static final int NetOverHead = 24;
    public static final int NetHashtableOverHead = 45;
    public static final int NetListOverHead = 8;

    public static final int NetClassOverHead = 16;
    
    public static final int NetIntSize = 4;
    public static final int NetEnumSize = 4;
    public static final int NetByteSize = 1;
    public static final int NetShortSize = 2;
    
    public static final int NetLongSize = 8;
    public static final int NetDateTimeSize = 8;
    public static final int NetReferenceSize = 8;
    
    /**
     * Dot Net Primitive Types String constants
     */
    public static final String Net_bool = "bool";
    public static final String Net_System_Boolean = "System.Boolean";
    public static final String Net_char = "char";
    public static final String Net_System_Char = "System.Char";
    public static final String Net_string = "string";
    public static final String Net_System_String = "System.String";
    public static final String Net_float = "float";
    public static final String Net_System_Single = "System.Single";
    public static final String Net_double = "double";
    public static final String Net_System_Double = "System.Double";
    public static final String Net_short = "short";
    public static final String Net_ushort = "ushort";
    public static final String Net_System_Int16 = "System.Int16";
    public static final String Net_System_UInt16 = "System.UInt16";
    public static final String Net_int = "int";
    public static final String Net_System_Int32 = "System.Int32";
    public static final String Net_uint = "uint";
    public static final String Net_System_UInt32 = "System.UInt32";
    public static final String Net_long = "long";
    public static final String Net_System_Int64 = "System.Int64";
    public static final String Net_ulong = "ulong";
    public static final String Net_SystemUInt64 = "System.UInt64";
    public static final String Net_byte = "byte";
    public static final String Net_System_Byte = "System.Byte";
    public static final String Net_sbyte = "sbyte";
    public static final String Net_System_SByte = "System.SByte";
    public static final String Net_System_Object = "System.Object";
    public static final String Net_System_DateTime = "System.DateTime";
    public static final String Net_decimal = "decimal";
    public static final String Net_System_Decimal = "System.Decimal";
    
    /**
     * Java Primitive Types String constants
     */
    public static final String Java_Lang_Boolean = "java.lang.Boolean";// True/false value
    public static final String Java_Lang_Character = "java.lang.Character";// Unicode character (16 bit;
    public static final String Java_Lang_String = "java.lang.String";// Unicode String
    public static final String Java_Lang_Float = "java.lang.Float";// IEEE 32-bit float
    public static final String Java_Lang_Double = "java.lang.Double";// IEEE 64-bit float
    public static final String Java_Lang_Short = "java.lang.Short";// Signed 16-bit integer
    public static final String Java_Lang_Integer = "java.lang.Integer";// Signed 32-bit integer
    public static final String Java_Lang_Long = "java.lang.Long";// Signed 32-bit integer
    public static final String Java_Lang_Byte = "java.lang.Byte";// Unsigned 8-bit integer
    public static final String Java_Lang_Object = "java.lang.Object";// Base class for all objects            
    public static final String Java_Util_Date = "java.util.Date";// Dates will always be serialized (passed by value); according to .NET Remoting
    public static final String Java_Match_BigDecimal = "java.math.BigDecimal";// Will always be serialized (passed by value); according to .NET Remoting           


    public static int getStringSize(Object key)
    {
        if(key != null && key instanceof String)
        {
            //size of .net charater is 2 bytes so multiply by 2 length of key, 24 bytes are extra overhead(header) of each instance
            return (2* ((String)key).length()) + MemoryUtil.NetOverHead;
        }
        return 0;
    }
    
    public static int getStringSize(Object keys[])
    {
        int totalSize = 0;
        if(keys != null)
        {
            for (Object key : keys) {
                //size of .net charater is 2 bytes so multiply by 2 length of key, 24 bytes are extra overhead(header) of each instance
                totalSize += (2 * ((String)key).length()) + MemoryUtil.NetOverHead;
            }
        }
        return totalSize;
    }
    
    /**
     * Used to get DataType Size for provided AttributeSize
     * @param type
     * @return 
     */
    public static int getTypeSize(AttributeTypeSize type) 
    {
        switch(type)
        {
            case Byte1:
                return 1;

            case Byte2:
                return 2;

            case Byte4:
                return 4;

            case Byte8:
                return 8;

            case Byte16:
                return 16;
        }
        return 0;
    }
    
    public static AttributeTypeSize GetAttributeTypeSize(String type)
        {
            if(type.equals(Net_bool) || type.equals(Net_byte) || 
                type.equals(Net_System_Byte) || type.equals(Net_sbyte) || 
                type.equals(Net_System_SByte) || type.equals(Net_System_Boolean) ||
                type.equals(Java_Lang_Boolean) || type.equals(Java_Lang_Byte)) {
                
                return AttributeTypeSize.Byte1;
            }
            else if(type.equals(Net_char) || type.equals(Net_short) || 
                    type.equals(Net_ushort) || type.equals(Net_System_Int16) || 
                    type.equals(Net_System_UInt16) || type.equals(Net_System_Char) ||
                    type.equals(Java_Lang_Character) || type.equals(Java_Lang_Float) ||
                    type.equals(Java_Lang_Short)) {
                
                return AttributeTypeSize.Byte2;
            }
            else if(type.equals(Net_float) || type.equals(Net_int) || 
                    type.equals(Net_System_Int32) || type.equals(Net_uint) || 
                    type.equals(Net_System_UInt32) || type.equals(Net_System_Single) ||
                    type.equals(Java_Lang_Integer)) {
                
                return AttributeTypeSize.Byte4;
            }
            else if(type.equals(Net_double) || type.equals(Net_System_Double) || 
                    type.equals(Net_long) || type.equals(Net_System_Int64) || 
                    type.equals(Net_System_DateTime) || type.equals(Net_SystemUInt64) ||
                    type.equals(Java_Lang_Double) || type.equals(Java_Lang_Long) ||
                    type.equals(Java_Util_Date) || type.equals(Net_ulong)) {
                
                return AttributeTypeSize.Byte8;
            }
            else if(type.equals(Net_decimal) || type.equals(Net_System_Decimal) || 
                    type.equals(Java_Match_BigDecimal)) {
                
                return AttributeTypeSize.Byte16;
            }
            else
                return AttributeTypeSize.Variable ;
        }
    
    public static int GetInMemoryInstanceSize(int actualDataBytes) 
    {
        int temp = MemoryUtil.NetClassOverHead;
        short remainder = (short)(actualDataBytes & 7);
        if (remainder != 0)
            remainder = (short)(8 - remainder);

        temp += actualDataBytes + remainder;
        return temp;
    }

    public static long GetInMemoryInstanceSize(long actualDataBytes)
    {
        long temp = MemoryUtil.NetClassOverHead;
        short remainder = (short)(actualDataBytes & 7);
        if (remainder != 0)
            remainder = (short)(8 - remainder);

        temp += actualDataBytes + remainder;
        return temp;           
    }
}
