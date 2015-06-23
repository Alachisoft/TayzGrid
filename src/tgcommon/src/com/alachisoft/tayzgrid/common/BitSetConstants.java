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

public class BitSetConstants
{

    public static final int Flattened = 1;
    public static final int Compressed = 2;
    public static final int WriteThru = 4;
    public static final int WriteBehind = 8;
    public static final int ReadThru = 16;
    public static final int LockItem = 64;
    public static final int OptionalDSOperation = 32;
    public static final int BinaryData = 128;

    public static int getBitSetData(BitSet bits)
    {
        return (int) bits.getData();
    }

    public static BitSet setBitSetData(String bits)
    {
        BitSet value = new BitSet();
        int maxIndex = bits.length() - 1;
        for (int i = maxIndex; i >= 0; i--)
        {
            if (bits.charAt(i) == '1')
            {
                value.SetBit((byte) (int) Math.pow(2, maxIndex - i));
            }
        }

        return value;
    }

    public static BitSet setBitSetData(int bits)
    {

        BitSet value = new BitSet();
        value.SetBit((byte) bits);
        return value;
    }
}