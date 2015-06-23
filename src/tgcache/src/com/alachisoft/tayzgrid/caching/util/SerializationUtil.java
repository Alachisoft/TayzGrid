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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSet;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSetConstant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author 
 */


public class SerializationUtil
{
    public static boolean Deserialize(CacheEntry entry, String serializationContext) throws GeneralFailureException
    {
        boolean isDeserialized = false;
        if (entry != null) //saftey check maybe redundant
        {
            SerializationBitSet tempFlag=new SerializationBitSet();
            if(entry.getFlag()!=null)
            {
                tempFlag=new SerializationBitSet((byte)entry.getFlag().getData());
            }
            if(!tempFlag.IsBitSet((byte)SerializationBitSetConstant.Flattened))
            {
                return true;
            }
            
            //byte[] entryValAsByteArr = null;
            Object newValue = null;
            if (entry.getValue() instanceof CallbackEntry)
            {
                newValue = Deserialize(((CallbackEntry) entry.getValue()).getValue(), entry.getFlag(), serializationContext);
                
                ((CallbackEntry) entry.getValue()).setValue(newValue);
                //entry.setValue(newValue);
                isDeserialized = true;
                    //entryValAsByteArr = ((UserBinaryObject) ((CallbackEntry) entry.getValue()).getValue()).GetFullObject();
            }
            else if (entry.getValue() instanceof UserBinaryObject)
            {
                newValue = Deserialize(entry.getValue(), entry.getFlag(), serializationContext);
                entry.setValue(newValue);
                isDeserialized = true;
                    //entryValAsByteArr = ((UserBinaryObject) entry.getValue()).GetFullObject();
            } 
            else
            {
                    throw new GeneralFailureException("DeSerialization Failed. Cache Entry value is neither user binary object nor callback entry.");
            }
        }
        return isDeserialized;
    }
    
    //Object must be user binary object for deserialization
    public static Object Deserialize(Object value, BitSet flag, String serializationContext) throws GeneralFailureException
    {
        try {
            if(!flag.IsBitSet((byte)BitSetConstants.Flattened))
                return value;
            byte[] serialized = null;
            if(value instanceof UserBinaryObject)
            {
                serialized = ((UserBinaryObject)value).GetFullObject();
            }
            else
                return value;
            
            SerializationBitSet tempFlag = new SerializationBitSet(flag.getData());
            Object deserialized = com.alachisoft.tayzgrid.serialization.util.SerializationUtil.safeDeserialize(serialized, serializationContext, tempFlag);
            flag.setData(tempFlag.getData());
            return deserialized;
        } catch (Exception ex) {
             GeneralFailureException e = new GeneralFailureException(ex.getMessage(), ex);
             throw e;
        }
    }
    
    public static boolean Serialize(CacheEntry entry, String serializationContext) throws GeneralFailureException
    {
        boolean isSerialized = false;
        if (entry != null) // just for saftey maybe redundant
        {
            BitSet Flag = entry.getFlag();
            if (Flag.IsBitSet((byte) BitSetConstants.Flattened))
            {
                isSerialized = true;
                return isSerialized;
            } 
            else
            {
                byte[] newValue = null;
                if (entry.getValue() instanceof CallbackEntry)
                {
                    newValue = Serialize((Object)((CallbackEntry) entry.getValue()).getValue(),entry.getFlag(), serializationContext);
                    ((CallbackEntry) entry.getValue()).setValue(UserBinaryObject.CreateUserBinaryObject(newValue));
                    isSerialized=true;
                } 
                else
                {
                    newValue = Serialize((Object)entry.getValue(),entry.getFlag(), serializationContext);
                    entry.setValue(UserBinaryObject.CreateUserBinaryObject(newValue));
                    isSerialized = true;
                }
            }
        }
        return isSerialized;
    }

    public static byte[] Serialize(Object value, BitSet flag, String serializationContext) throws GeneralFailureException
    {
        try 
        {
            if(flag.IsBitSet((byte)BitSetConstants.Flattened))  //redundant 
                return (byte[])value;
            byte[] serialized = null;
            SerializationBitSet tempFlag=new SerializationBitSet();
            if(flag!=null)
            {
                tempFlag.setData(flag.getData());
            }
            //Serialize
            serialized = com.alachisoft.tayzgrid.serialization.util.SerializationUtil.safeSerialize(value, serializationContext, tempFlag);    //flattened set inside
            flag.setData(tempFlag.getData());
            //Encrypt if cconfigured
            //No need to set bit as it is cache level stuff
            
            return serialized;
        } 
        catch (Exception ex) 
        {
             GeneralFailureException e = new GeneralFailureException(ex.getMessage(), ex);
             throw e;
        }
    }
    
    public static void SerializeHashMap(HashMap hMap, String serializationContext)  throws  GeneralFailureException
    {
        if(hMap!=null && hMap.entrySet()!=null)
        {
            Iterator ide = hMap.entrySet().iterator();
            CacheEntry entry;

            Map.Entry KeyValue;
            while (ide.hasNext()) 
            {
                KeyValue = (Map.Entry) ide.next();
                Object Value = KeyValue.getValue();
                if(Value instanceof CacheEntry)
                {
                    entry = (CacheEntry)((CacheEntry)Value).clone();
                    Serialize(entry, serializationContext);
                    KeyValue.setValue(entry);
                }
                else if(Value instanceof CompressedValueEntry)
                {
                    CompressedValueEntry cvEntry = (CompressedValueEntry)Value;
                    byte[] data =Serialize(cvEntry.Value,cvEntry.Flag,serializationContext);
                    cvEntry.setValue(UserBinaryObject.CreateUserBinaryObject(data));
                }
            }
        }
    }
    

    
}
