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

package com.alachisoft.tayzgrid.serialization.standard;

import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateSelector;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UnsignedByteArraySerializationSurrogate;
import javassist.CannotCompileException;

/**
 *
 * @author  
 */
public class SurrogateHelper
{

    public static Object CreateGenericTypeInstance(TypeSurrogateSelector mSurrogateTypeSelector, Class genericType, HashMap attributeOrder, String cacheContext, boolean portable, short subHandle,HashMap nonCompactFieldsMap) throws Exception
    {

        if (genericType == null)
        {
            throw new NullPointerException("genericType");
        }
        try
        {
            ClassPool pool = ClassPool.getDefault();
            CtClass dynamicSurrugate = null;

            Class classLoader = null;

            try
            {
                classLoader = Class.forName("com.alachisoft.tayzgrid.serialization.standard.DynamicSurrogate" + genericType.getName().toLowerCase() + cacheContext);

            }
            catch (ClassNotFoundException classNotFoundException)
            {
            }

            int i = 0;
            while (classLoader == null)
            {
                try
                {
                    if (classLoader == null)
                    {
                        dynamicSurrugate = pool.get("com.alachisoft.tayzgrid.serialization.standard.DynamicSurrogate");
                        dynamicSurrugate.defrost();

                        try
                        {
                            dynamicSurrugate = pool.get("com.alachisoft.tayzgrid.serialization.standard.DynamicSurrogate" + genericType.getName().toLowerCase() + cacheContext);

                            dynamicSurrugate.defrost();
                        }
                        catch (NotFoundException notFoundException)
                        {
                        }


                        dynamicSurrugate.replaceClassName("com.alachisoft.tayzgrid.serialization.standard.DynamicSurrogate", "com.alachisoft.tayzgrid.serialization.standard.DynamicSurrogate"
                                + genericType.getName().toLowerCase() + cacheContext);

                        //If Ever Ever Class.forName fails to detect a class already present in jvm and we move on to commit the sin of modifying it
                        //just to make sure that never happens we deforst it first (Safety first)
                        //This function is always called once when initializing a cache
                        dynamicSurrugate.defrost();

                        CtMethod readObject = dynamicSurrugate.getDeclaredMethod("readObject");
                        CtMethod writeObject = dynamicSurrugate.getDeclaredMethod("writeObject");

                        StringBuffer serializationCode = new StringBuffer();
                        StringBuffer deserializationCode = new StringBuffer();

                        GenerateDynamicCode.generateCode(genericType, serializationCode, deserializationCode, attributeOrder, portable, subHandle, mSurrogateTypeSelector, cacheContext,nonCompactFieldsMap);



                        writeObject.insertAt(1, serializationCode.toString());
                        readObject.insertAt(1, deserializationCode.toString());

                        try
                        {
                            classLoader = dynamicSurrugate.toClass();
                        }
                        catch (Exception exception)
                        {
                            i++;
                            if (i == 5)
                            {
                                throw new Exception("Please restart Service; a null exception has been thrown by javassist while generationg code at runtime: "
                                        + "'sun.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)'");
                            }
                            classLoader = null;
                        }
                    }
                }
                catch (CannotCompileException cce)
                {
                    if(cce.getMessage().contains("no such constructor"))
                    {
                        throw new CannotCompileException("No default constructor found in " + genericType.getName(), cce);
                    }
                    else
                    {
                        throw cce;
                    }
                }
                catch (Exception exception)
                {
                    
                    throw exception;
                }

                
            }

            SerializationSurrogate h;
            Constructor[] ctorlist = null;
            try
            {
                ctorlist = classLoader.getDeclaredConstructors();
            }
            catch (SecurityException ex)
            {
                throw new Exception(ex.getMessage());
            }
            catch (Exception exc)
            {
                throw exc;
            }
            h = (SerializationSurrogate) ctorlist[0].newInstance(genericType);

            h.setSubTypeHandle(subHandle);
            
            return h;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }
}
