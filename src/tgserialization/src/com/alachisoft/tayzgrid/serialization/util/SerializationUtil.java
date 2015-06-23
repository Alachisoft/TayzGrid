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


package com.alachisoft.tayzgrid.serialization.util;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectInputStream;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

public class SerializationUtil
{

    static HashMap _attributeOrder = new HashMap();
    static HashMap _portibilaty = new HashMap();
    static HashMap _subTypeHandle = new HashMap();
    private static FormatterServices impl = null;
    //contains CacheContext just to mae sure when unregistering cache, we don't attempt to unrigester a never existing cacheContext
    private static List cacheList;


    /**
     * Simply serializes any object unless one of the supported default types or Dynamically registered with respect to CacheContext
     * @param value object to serialize
     * @param cacheContext Name of the Cache that object is registered to else null if default type
     * @return returns serialized byte array of the object
     * @throws IOException
     */
    public static byte[] safeSerialize(Object value, String cacheContext,SerializationBitSet flag) throws IOException
    {
        
         if((value instanceof byte[]) && flag!=null)
         {                
             flag.SetBit((byte)SerializationBitSetConstant.BinaryData);
             return (byte[])value;                
         }                  
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream, cacheContext);
         objectOutput.writeObject(value);
         objectOutput.flush();
         flag.SetBit((byte)SerializationBitSetConstant.Flattened);
         
         return byteArrayOutputStream.toByteArray();
    }

    /**
     * Deserializes byte[] into an object castable into the one actually expected if it is  one of the supported default types or Dynamically registered with respect to CacheContext
     * @param value byte[] to deserialize
     * @param cacheContext Name of the cache that object is registered to else null if default type
     * @return expected Object boxed
     * @throws IOException
     * @throws ClassNotFoundException if Class was not dynamically registered or the value is not one of the default types
     */
    public static Object safeDeserialize(Object value, String cacheContext,SerializationBitSet flag) throws IOException, ClassNotFoundException
    {
        if (value != null && value instanceof byte[])
        {
            if(flag!=null && flag.IsBitSet((byte)SerializationBitSetConstant.BinaryData)) return value;                                    
            
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[]) value);
            ObjectInput objectInput = new ObjectInputStream(byteArrayInputStream, cacheContext);
            flag.UnsetBit((byte)SerializationBitSetConstant.Flattened);
            return objectInput.readObject();
            
        }
        return value;
    }
    public static Object compactSerialize(Object value, String cacheContext) throws IOException
    {
        if (value != null && value instanceof ICompactSerializable)
        {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream, cacheContext);
        objectOutput.writeObject(value);
        objectOutput.flush();

        return byteArrayOutputStream.toByteArray();
        }
        return value;
    }

    /**
     * Deletes only the stored cacheContext, dynamically created classes are not destroyed
     * since once a class/file has been loaded into JVM, its instance can be deleted but the class/file cannot
     * @param cacheContext CacheName
     */
    public static void unRegisterCache(String cacheContext)
    {
        if (cacheList != null && cacheList.contains(cacheContext))
        {
            impl.unregisterKnownType(null, cacheContext, true);
            cacheList.remove(cacheContext);
        }
        else
        {
            if (impl == null)
            {
                impl = FormatterServices.getDefault();
            }
            impl.unregisterKnownType(null, cacheContext, true);
        }
    }

    /**
     * Get Type Map of Dynamic Compact Serialization From Protocol String (Recursion Logic inside to parse whole string)
     * @param protocolString String returned by Server containing complete information of which Class(es) to Dynamically Serialize
     * @param startIndex Used for String manipulation, For start of Recursion pass 0
     * @param endIndex Used for String manipulation, For start of Recursion pass 0
     * @return HasMap containing Class name, ID and other data related to the class to be initialized
     */
    public static HashMap GetTypeMapFromProtocolString(String protocolString, int[] startIndex, int[] endIndex)
    {
        endIndex[0] = protocolString.indexOf("\"", startIndex[0] + 1);
        HashMap tbl = new HashMap();
        String token = protocolString.substring(startIndex[0], (endIndex[0]));

        if (token.equals("__dictionary"))
        {
            startIndex[0] = endIndex[0] + 1;
            endIndex[0] = protocolString.indexOf("\"", endIndex[0] + 1);
            int dicCount = Integer.parseInt(protocolString.substring(startIndex[0], (endIndex[0])));

            for (int i = 0; i < dicCount; i++)
            {
                startIndex[0] = endIndex[0] + 1;
                endIndex[0] = protocolString.indexOf("\"", endIndex[0] + 1);
                String key = protocolString.substring(startIndex[0], (endIndex[0]));

                startIndex[0] = endIndex[0] + 1;
                endIndex[0] = protocolString.indexOf("\"", endIndex[0] + 1);

                // If value to be stored is _dictionary then call the function recursively other wise its an attribute
                String value = protocolString.substring(startIndex[0], (endIndex[0]));

                if (value.equals("__dictionary"))
                {
                    HashMap temp = new HashMap();
                    temp = GetTypeMapFromProtocolString(protocolString, startIndex, endIndex);
                    tbl.put(key, temp);
                }
                else
                {
                    tbl.put(key, value);
                }
            }
        }
        return tbl;
    }


    public static String GetProtocolStringFromTypeMap(HashMap typeMap)
        {
            Stack st = new Stack();
            StringBuilder protocolString = new StringBuilder();
            protocolString.append("__dictionary").append("\"");
            protocolString.append(typeMap.size()).append("\"");

            Iterator ide = typeMap.entrySet().iterator();
            while (ide.hasNext())
            {
                Map.Entry mapDic = (Map.Entry)ide.next();
                if (mapDic.getValue() instanceof  HashMap)
                {
                    st.push(mapDic.getValue());
                    st.push(mapDic.getKey());
                }
                else
                {
                    protocolString.append(mapDic.getKey()).append("\"");
                    protocolString.append(mapDic.getValue()).append("\"");
                }
            }

            while (st.size() != 0 && st.size() % 2 == 0)
            {
                Object obj1=st.pop();
                protocolString.append((obj1 instanceof String)? obj1.toString():"").append("\"");
                Object obj2 = st.pop() ;
                protocolString.append(GetProtocolStringFromTypeMap(obj2 instanceof HashMap ? (HashMap) obj2 : null));
            }
            return protocolString.toString();
        }


    public static HashMap getAttributeOrder(String cacheContext)
    {
        return (HashMap) _attributeOrder.get(cacheContext);
    }

    public static HashMap getPortability(String cacheContext)
    {
        return ((HashMap) _portibilaty.get(cacheContext));
    }

    public static boolean getPortability(short type, String cacheContext)
    {
        return (Boolean)((HashMap) _portibilaty.get(cacheContext)).get(type);
    }

    public static void PopulateSubHandle(boolean portable, String cacheContext, String handle, String subHandle, Class type)
    {
        if (portable)
        {
            if (!_subTypeHandle.containsKey(cacheContext))
            {
                _subTypeHandle.put(cacheContext, new HashMap());
            }

            if (!((HashMap) _subTypeHandle.get(cacheContext)).containsKey(handle))
            {
                ((HashMap) _subTypeHandle.get(cacheContext)).put(handle, new HashMap());
            }

            if (!((HashMap) ((HashMap) _subTypeHandle.get(cacheContext)).get(handle)).containsKey(type))
            {
                ((HashMap) ((HashMap) _subTypeHandle.get(cacheContext)).get(handle)).put(type, subHandle);
            }
            else
            {
                throw new IllegalArgumentException("Sub-Handle '" + subHandle + "' already present in " + cacheContext + " in class " + type.getName() + " with Handle " + handle);
            }
        }
    }

    public static short GetSubTypeHandle(String cacheContext, String handle, Class type)
    {
        if (!_subTypeHandle.containsKey(cacheContext))
        {
            return 0;
        }
        if (!(((HashMap) _subTypeHandle.get(cacheContext)).containsKey(handle)))
        {
            return 0;
        }
        return Short.parseShort((String) ((HashMap) ((HashMap) _subTypeHandle.get(cacheContext)).get(handle)).get(type));
    }
    ////*****************
    private static HashMap typeInfoMapTable = new HashMap();
    private static ClassInfoPool classInfoPool;

    public static void registerTypeInfoMap(String cacheContext, TypeInfoMap typeInfoMap)
    {
        classInfoPool = new ClassInfoPool();
        if (!typeInfoMapTable.containsKey(cacheContext))
        {
            typeInfoMapTable.put(cacheContext, typeInfoMap);
        }

    }

    public static void UnRegisterTypeInfoMap(String cacheContext)
    {
        if (!typeInfoMapTable.containsKey(cacheContext))
        {
            typeInfoMapTable.remove(cacheContext);
        }

    }

    public static HashMap getQueryInfo(Object value, String cacheContext) throws Exception
    {
        TypeInfoMap typeInfoMap = (TypeInfoMap) typeInfoMapTable.get(cacheContext);
        if (typeInfoMap == null)
        {
            return null;
        }
        return getQueryInfo(value, typeInfoMap);
    }

    private static HashMap getQueryInfo(Object value, TypeInfoMap typeMap)
            throws Exception
    {
        HashMap queryInfo = null;

        if (typeMap == null)
        {
            return null;
        }

        try
        {
            Class valueClass = value.getClass();
            // int handleId = typeMap.getHandleId(value.getClass().getCanonicalName());
            ClassInfo classInfo = classInfoPool.getClassInf(valueClass);
            int handleId = typeMap.getHandleId(classInfo.getCanonicalName());

            if (handleId != -1)
            {
                queryInfo = new HashMap();
                ArrayList attribValues = new ArrayList();
                ArrayList attributes = typeMap.getAttribList(handleId);

                for (int i = 0; i < attributes.size(); i++)
                {
                    Field fieldAttrib = classInfoPool.getField(valueClass, (String) attributes.get(i));
                    //                    Field fieldAttrib = value.getClass().getField((String) attributes.get(i));
                    if (fieldAttrib != null)
                    {
                        Object attribValue = fieldAttrib.get(value);

                        if (attribValue instanceof java.lang.String) //add all strings as lower case in index tree
                        {
                            attribValue = (Object) (attribValue.toString()).toLowerCase();

                        }
                        attribValues.add(attribValue);
                    }
                    else
                    {
                        throw new Exception("Unable to extract query information from user object.");
                    }

                }
                queryInfo.put("" + handleId, attribValues);
            }

        }
        catch (Exception e)
        {
            throw new Exception("Unable to extract query information from user object.");
        }

        return queryInfo;
    }
}

class ClassInfoPool
{

    HashMap<Class, ClassInfo> pool = new HashMap<Class, ClassInfo>();
    static int i = 1;

    public ClassInfo getClassInf(Class cls)
    {

        ClassInfo classInfo = pool.get(cls);
        if (classInfo != null)
        {
            return classInfo;
        }
        classInfo = new ClassInfo(cls.getCanonicalName());
        pool.put(cls, classInfo);
        return classInfo;
    }

    public Field getField(Class cls, String fieldName) throws NoSuchFieldException
    {
        ClassInfo clsInfo = pool.get(cls);
        Field field = clsInfo.getField(fieldName);
        if (field != null)
        {
            return field;
        }
        field = cls.getField(fieldName);
        clsInfo.addField(fieldName, field);
        return field;
    }
}

class ClassInfo
{

    String canonicalName;
    HashMap<String, Field> fieldMap;

    ClassInfo(String canonicalName)
    {
        this.canonicalName = canonicalName;
        fieldMap = new HashMap<String, Field>();
    }

    public void setCanonicalName(String canonicalName)
    {
        this.canonicalName = canonicalName;
    }

    public String getCanonicalName()
    {
        return this.canonicalName;
    }

    public Field getField(String fieldName)
    {
        return fieldMap.get(fieldName);

    }

    public void addField(String fieldName, Field field)
    {
        fieldMap.put(fieldName, field);
    }
}
