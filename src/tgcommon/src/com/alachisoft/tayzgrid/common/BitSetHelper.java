/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alachisoft.tayzgrid.common;

/**
 *
 * @author 
 */
public class BitSetHelper {
    
       public static boolean IsAnyBitSet(byte _bitset, byte bit) { return ((_bitset & bit) != 0); }
       public static boolean IsBitSet( byte _bitset, byte bit) { return ((_bitset & bit) == bit); }
       public static void SetBit( byte _bitset, byte bit) { _bitset |= bit; }
       public static void UnsetBit( byte _bitset, byte bit) { _bitset &= Byte.valueOf(bit); }
       public static void Set( byte _bitset, byte bitsToSet, byte bitsToUnset)
       {
           SetBit(_bitset, bitsToSet);
           UnsetBit(_bitset, bitsToUnset);
       }
}
