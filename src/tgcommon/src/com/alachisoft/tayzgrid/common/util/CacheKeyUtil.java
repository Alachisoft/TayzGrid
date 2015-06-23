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

import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.standard.io.SizeableOutputStream;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CacheKeyUtil {
    
    public static ByteString toByteString(Object key, String serializationContext) throws IOException
    {
        return ByteString.copyFrom(Serialize(key, serializationContext));
    }
    
     public static List<ByteString> toByteStrings(Object[] keys, String serializationContext) throws IOException
    {
        List<ByteString> keysList=new ArrayList();
        for(Object key:keys)
        {
            keysList.add(ByteString.copyFrom(Serialize(key, serializationContext)));
        }
        return keysList;        
    }
    
    public static byte[] Serialize(Object key, String serializationContext) throws IOException
    {
            if(serializationContext == null)
                serializationContext = "";
            return CompactBinaryFormatter.toByteBuffer(key, serializationContext);
    }
    
    
    public static Object Deserialize(ByteString serializedKey, String serializationContext) throws IOException, ClassNotFoundException
    {
        return Deserialize(serializedKey.toByteArray(), serializationContext);
    }
    
    public static Object SafeDeserialize(ByteString serializedKey, String serializationContext) throws IOException, ClassNotFoundException
    {
        return serializedKey != null? SafeDeserialize(serializedKey.toByteArray(), serializationContext): null;
    }
    
    public static Object Deserialize(byte[] serializedKey, String serializationContext) throws IOException, ClassNotFoundException
    {
        if(serializedKey == null || serializedKey.length == 0)
            return null;
            return CompactBinaryFormatter.fromByteBuffer(serializedKey, serializationContext);
    }
    
    public static Object SafeDeserialize(byte[] serializedKey, String serializationContext) throws IOException, ClassNotFoundException
    {
        try{
        return Deserialize(serializedKey, serializationContext);
        }catch(Exception e){}
        
        return serializedKey ;
    }
    
    public static long getKeySize(Object key, String cacheContext)
    {
        try {
            SizeableOutputStream sizableStream = new SizeableOutputStream();
            CompactBinaryFormatter.writeToStream(key, sizableStream, cacheContext);
            return sizableStream.getSize();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
