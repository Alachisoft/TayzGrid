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

import java.io.Serializable;

/**
 * A class that encapsulates a bit set.
 */
public class BitSet implements Cloneable, Serializable
{

    private byte _bitset;

    public BitSet()
    {
    }

    public BitSet(byte bitset)
    {
        this._bitset = bitset;
    }

    public byte getData()
    {
        return _bitset;
    }

    public void setData(byte bitset)
    {
        _bitset = bitset;
    }

    /// <summary> Bit set specific functions. </summary>
    public boolean IsAnyBitSet(byte bit)
    {
        return ((_bitset & bit) != 0);
    }

    public boolean IsBitSet(byte bit)
    {
        return ((_bitset & bit) == bit);
    }

    public void SetBit(byte bit)
    {
        _bitset |= bit;
    }

    public void UnsetBit(byte bit)
    {
        _bitset &= (~bit & 0xff);
    }

    public void Set(byte bitsToSet, byte bitsToUnset)
    {
        SetBit(bitsToSet);
        UnsetBit(bitsToUnset);
    }

    public Object Clone()
    {
        BitSet other = new BitSet();
        other._bitset = _bitset;
        return other;
    }

    public void Dispose()
    {
    }
}