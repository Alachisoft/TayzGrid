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
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFDotNetSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFJavaSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt16ArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt16SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt32ArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt32SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt64ArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt64SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UnsignedByteArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UnsignedByteSerializationSurrogate;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class GenerateDynamicCode
{

    private static String IDENTIFIER = "temp";
    private static HashMap _attributeOrder = new HashMap();
    private static HashMap _nonCompactFieldsMap=new HashMap();
    private static TypeSurrogateSelector _mSurrogateTypeSelector;
    private static String _cacheContext;

    public static void generateCode(Class obj, StringBuffer serializeCode, StringBuffer deserializeCode, HashMap attributeOrder, boolean portable, short subHandle,TypeSurrogateSelector mSurrogateTypeSelector, String cacheContext,HashMap nonCompactFieldsMap) throws Exception
    {
        try
        {
            _attributeOrder = attributeOrder;
            if(nonCompactFieldsMap!=null)
            _nonCompactFieldsMap.put(obj,nonCompactFieldsMap);
            _mSurrogateTypeSelector = mSurrogateTypeSelector;
            _cacheContext = cacheContext;

            ArrayList<String> classCookie = new ArrayList<String>();
            classCookie.add(obj.getCanonicalName());

            if (!portable)
            {
                GenerateDynamicCode.generateSerializationAndDeserializationMethods(obj, serializeCode, deserializeCode, IDENTIFIER, null, classCookie, portable);
            }
            else
            {
                GenerateDynamicCode.generatePortableSerializationAndDeserializationMethods(obj, serializeCode, deserializeCode, IDENTIFIER, (String[][]) _attributeOrder.get(obj.getCanonicalName()), classCookie, portable, subHandle);
            }

            deserializeCode.append("\n\treturn " + IDENTIFIER + ";");
            serializeCode.append("\n\treturn;"); //Dont ask just do it
            
            if(nonCompactFieldsMap!=null)
            _nonCompactFieldsMap.remove(obj);
            
        }
        catch (Exception exception)
        {
            throw exception;
        }
    }


    /* This method generates the serialization and deserialization code strings which was later used to
     * generate the runtime code
     */
    private static void generateSerializationAndDeserializationMethods(Class obj, StringBuffer serializeCode, StringBuffer deserializeCode, String identifier, String[][] attribOrder, ArrayList<String> classCookie, boolean portable) throws Exception
    {
        try
        {
            // Initializing obj Class
            serializeCode.append("\n\t" + obj.getCanonicalName() + " " + identifier + "=" + "(" + obj.getCanonicalName() + ")graph;");
            deserializeCode.append("\n\t" + obj.getCanonicalName() + " " + identifier + "=" + "new " + obj.getCanonicalName() + "();");

            // Iterate thrpugh all Public fields only, order dependant
            Field[] fields = obj.getFields();
            Field[] javaFields = null;

            HashMap<String, String> unsignedTypes = new HashMap<String, String>();

            //Order out attributes
            if (attribOrder != null && portable)
            {
                //<editor-fold defaultstate="collapsed" desc="IF Portable">
                javaFields = GetAllFields(obj, attribOrder, fields, unsignedTypes);
                //</editor-fold>
            }
            else
            {
               if(_nonCompactFieldsMap.containsKey(obj))
               {
                HashMap nonCompactFields=(HashMap)_nonCompactFieldsMap.get(obj);
                java.util.ArrayList<Field> fieldsList=new ArrayList<Field>();
                int index=-1;
                for(Field f:fields)
                {
                    if(nonCompactFields.containsKey(f.getName().toLowerCase()))
                        continue;
                    else
                        fieldsList.add(f);                                                                                     
                }
                Field[] temp=new Field[fieldsList.size()];
                javaFields=fieldsList.toArray(temp);
               }
               else
               {
                javaFields = fields;
               }
            }

            for (Field f : javaFields)
            {

                if (f == null)
                {
                    serializeCode.append("\n\toutput.writeObject(new com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFDotNetSerializationSurrogate());");
                    deserializeCode.append("\n\tObject obj = input.readObject();");
                    deserializeCode.append("\n\tif(obj != null){return temp;}");

                    continue;
                }
                if (f.getType().isPrimitive()) // for Primitive types only
                {
                    if (f.getType().getName().toLowerCase().equals("int"))
                    {
                        if (unsignedTypes.containsKey(f.getName()))
                        {
                            serializeCode.append("\n\toutput.writeUInt16(" + identifier + "." + f.getName() + ");");
                            deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readUInt16();");
                        }
                        else
                        {
                            serializeCode.append("\n\toutput.writeInt(" + identifier + "." + f.getName() + ");");
                            deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readInt();");
                        }
                    }
                    else if (f.getType().getName().toLowerCase().equals("double"))
                    {
                        serializeCode.append("\n\toutput.writeDouble(" + identifier + "." + f.getName() + ");");
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readDouble();");
                    }
                    else if (f.getType().getName().toLowerCase().equals("long"))
                    {
                        if (unsignedTypes.containsKey(f.getName()))
                        {
                            serializeCode.append("\n\toutput.writeUInt32(" + identifier + "." + f.getName() + ");");
                            deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readUInt32();");
                        }
                        else
                        {
                            serializeCode.append("\n\toutput.writeLong(" + identifier + "." + f.getName() + ");");
                            deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readLong();");
                        }
                    }
                    else if (f.getType().getName().toLowerCase().equals("short"))
                    {
                        serializeCode.append("\n\toutput.writeShort(" + identifier + "." + f.getName() + ");");
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readShort();");
                    }
                    else if (f.getType().getName().toLowerCase().equals("float"))
                    {
                        serializeCode.append("\n\toutput.writeFloat(" + identifier + "." + f.getName() + ");");
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readFloat();");
                    }
                    else if (f.getType().getName().toLowerCase().equals("char"))
                    {
                        serializeCode.append("\n\toutput.writeChar(" + identifier + "." + f.getName() + ");");
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readChar();");
                    }
                    else if (f.getType().getName().toLowerCase().equals("byte"))
                    {
                        serializeCode.append("\n\toutput.writeByte(" + identifier + "." + f.getName() + ");");
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readByte();");
                    }
                    else if (f.getType().getName().toLowerCase().equals("boolean"))
                    {
                        serializeCode.append("\n\toutput.writeBoolean(" + identifier + "." + f.getName() + ");");
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readBoolean();");
                    }
                }
                else if (!f.getType().isEnum()
                        /*
                         * BUG# 4096
                         * && !f.getType().isInterface()
                         * the bug here being that we were simply skipping interface.
                         * i.e consider the following class
                         *      public class GenericList<T>
                         *      {
                         *          public java.util.List<T> list1 = new java.util.ArrayList<T>();
                         *          public java.util.ArrayList<T> list2 = new java.util.ArrayList<T>();
                         *          public GenericList()
                         *          {
                         *          }
                         *      }
                         *
                         *  if had added a few items in list1 and list2 and then added object of GenereicClass in the cache. the code being generated was skipping the code for
                         *  public java.util.List<T> list1 as java.util.List<T> is an interface the other list, list2 was working with out any issues.
                         *  this fix makes sure that we don't skip interfaces.
                         */
                        && !f.getType().isMemberClass()
                        && !f.getType().isLocalClass())
                {
                    // Problem with double[] , float[] and short[] ... unable to call writObject directly, creating a local copy of the array and then passing
                    // on to writeObject solves the problem, reason not identified, no reply posted onto problem posted in JavaAssist forums or StackOverflow
                    // Throws exception if directly passes: java.lang.VerifyError: (class: testapp1/Dyn, method: processDouble signature: (Lsomething/Output;Ljava/lang/Object;)V) Inconsistent args_size for opc_invokeinterface
                    if (f.getType().getCanonicalName().equals("double[]") || f.getType().getCanonicalName().equals("float[]") || f.getType().getCanonicalName().equals("long[]"))
                    {
                        // creates a local copy
                        serializeCode.append("\n\t" + f.getType().getCanonicalName() + " " + f.getName() + " = " + identifier + "." + f.getName() + ";");
                        // passes
                        serializeCode.append("\n\toutput.writeObject(" + f.getName() + ");");
                    }
                    else
                    {
                        if (f.getType().getName().equals("java.math.BigInteger") && unsignedTypes.containsKey(f.getName()))
                        {
                            serializeCode.append("\n\toutput.writeUInt64(" + identifier + "." + f.getName() + ");");
                        }
                        else
                        {
                            serializeCode.append("\n\toutput.writeObject(" + identifier + "." + f.getName() + ");");
                        }

                    }


                    //Deserialize Code
                    if (f.getType().getName().toLowerCase().equals("java.lang.string"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (String)input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("int[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (int[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("double[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (double[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("float[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (float[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("char[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (char[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("byte[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (byte[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("long[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (long[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("boolean[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (boolean[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().equals("short[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (short[])input.readObject();");
                    }
                    else if (f.getType().getCanonicalName().toLowerCase().equals("java.lang.string[]"))
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (" + f.getType().getCanonicalName() + ")input.readObject();");
                    }
                    else if (f.getType().getName().equals("java.math.BigInteger"))
                    {
                        if (unsignedTypes.containsKey(f.getName()))
                        {
                            deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = input.readUInt64();");
                        }
                        else
                        {
                            deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (" + f.getType().getCanonicalName() + ")input.readObject();");
                        }
                    }
                    else
                    {
                        deserializeCode.append("\n\t" + identifier + "." + f.getName() + " = (" + f.getType().getCanonicalName() + ")input.readObject();");
                    }

                }
                else
                {
                    if (!classCookie.contains(f.getType().getCanonicalName()))
                    {
                        classCookie.add(f.getType().getCanonicalName());
                        generateSerializationAndDeserializationMethods(f.getType(), serializeCode, deserializeCode, identifier + "." + f.getName() + ".", null, classCookie, portable);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw ex;
        }

    }

    private static void generatePortableSerializationAndDeserializationMethods(Class obj, StringBuffer serializeCode, StringBuffer deserializeCode, String IDENTIFIER, String[][] attribOrder, ArrayList<String> classCookie, boolean portable, short subHandle) throws Exception
    {
        try
        {
            // Initializing obj Class
            serializeCode.append("\n\t" + obj.getCanonicalName() + " " + IDENTIFIER + "=" + "(" + obj.getCanonicalName() + ")graph;");
            deserializeCode.append("\n\t" + obj.getCanonicalName() + " " + IDENTIFIER + "=" + "new " + obj.getCanonicalName() + "();");
            serializeCode.append("\n\tshort handle;");
            deserializeCode.append("\n\tjava.lang.Object LOCAL_OBJECT;");

            // Iterate thrpugh all Public fields only, order dependant
            Field[] fields = obj.getFields();
            Field[] javaFields = null;

            HashMap<String, String> unsignedTypes = new HashMap<String, String>();

            //Order out attributes
            if (attribOrder != null && portable)
            {
                //<editor-fold defaultstate="collapsed" desc="IF Portable">
                javaFields = GetAllFields(obj, attribOrder, fields, unsignedTypes);
                //</editor-fold>
            }
            else
            {
                javaFields = fields;
            }

            for (int i = 0; i < javaFields.length; i++)
            {
                Field f = javaFields[i];

                boolean toSkip = false;
                if (i < attribOrder[1].length && attribOrder[1][i].equals("0"))
                {
                    toSkip = true;
                }


                if (!(f == null && toSkip))
                {
                    if (f == null)
                    {
                        serializeCode.append("\n\toutput.writeObject(new com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFJavaSerializationSurrogate());");
                        serializeCode.append("\n\toutput.writeShort(" + subHandle + ");");
                        deserializeCode.append("\n\tObject obj = input.readObject();");
                        deserializeCode.append("\n\tShort eof = (Short)obj ;");
                        deserializeCode.append("\n\tif(this.getSubHandle() != eof.shortValue()){return temp;}");

                        continue;
                    }
                    if (f.getType().isPrimitive())
                    {
                        if (f.getType().getName().toLowerCase().equals("int"))
                        {
                            if (unsignedTypes.containsKey(f.getName()))
                            {
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UInt16SerializationSurrogate.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\toutput.writeUInt16(" + IDENTIFIER + "." + f.getName() + ");");
                                deserializePortableCode(deserializeCode, f, IDENTIFIER);
                            }
                            else
                            {
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Integer.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\toutput.writeInt(" + IDENTIFIER + "." + f.getName() + ");");
                                //deserializeCode.append("\n\t" + IDENTIFIER + "." + f.getName() + " = input.readInt();");
                                deserializePortableCode(deserializeCode, f, IDENTIFIER);
                            }
                        }
                        else if (f.getType().getName().toLowerCase().equals("long"))
                        {
                            if (unsignedTypes.containsKey(f.getName()))
                            {
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UInt32SerializationSurrogate.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\toutput.writeUInt32(" + IDENTIFIER + "." + f.getName() + ");");
                                deserializePortableCode(deserializeCode, f, IDENTIFIER);
                            }
                            else
                            {
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Long.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\toutput.writeLong(" + IDENTIFIER + "." + f.getName() + ");");
                                deserializePortableCode(deserializeCode, f, IDENTIFIER);
                            }
                        }
                        else if (f.getType().getName().toLowerCase().equals("double"))
                        {
                            SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Double.class, _cacheContext);
                            writeClassHandle(serializeCode, surr.getClassHandle());
                            serializeCode.append("\n\toutput.writeDouble(" + IDENTIFIER + "." + f.getName() + ");");
                            deserializePortableCode(deserializeCode, f, IDENTIFIER);
                        }
                        else if (f.getType().getName().toLowerCase().equals("short"))
                        {
                            if(unsignedTypes.containsKey(f.getName()))
                            {
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UnsignedByteSerializationSurrogate.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\toutput.writeUByte(" + IDENTIFIER + "." + f.getName() + ");");
                                deserializePortableCode(deserializeCode, f, IDENTIFIER);
                            }
                            else
                            {
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Short.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\toutput.writeShort(" + IDENTIFIER + "." + f.getName() + ");");
                                deserializePortableCode(deserializeCode, f, IDENTIFIER);
                            }
                        }
                        else if (f.getType().getName().toLowerCase().equals("float"))
                        {
                            SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Float.class, _cacheContext);
                            writeClassHandle(serializeCode, surr.getClassHandle());
                            serializeCode.append("\n\toutput.writeFloat(" + IDENTIFIER + "." + f.getName() + ");");
                            deserializePortableCode(deserializeCode, f, IDENTIFIER);
                        }
                        else if (f.getType().getName().toLowerCase().equals("char"))
                        {
                            SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Character.class, _cacheContext);
                            writeClassHandle(serializeCode, surr.getClassHandle());
                            serializeCode.append("\n\toutput.writeChar(" + IDENTIFIER + "." + f.getName() + ");");
                            deserializePortableCode(deserializeCode, f, IDENTIFIER);
                        }
                        else if (f.getType().getName().toLowerCase().equals("byte"))
                        {
                            SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Byte.class, _cacheContext);
                            writeClassHandle(serializeCode, surr.getClassHandle());
                            serializeCode.append("\n\toutput.writeByte(" + IDENTIFIER + "." + f.getName() + ");");
                            deserializePortableCode(deserializeCode, f, IDENTIFIER);
                        }
                        else if (f.getType().getName().toLowerCase().equals("boolean"))
                        {
                            SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(Boolean.class, _cacheContext);
                            writeClassHandle(serializeCode, surr.getClassHandle());
                            serializeCode.append("\n\toutput.writeBoolean(" + IDENTIFIER + "." + f.getName() + ");");
                            deserializePortableCode(deserializeCode, f, IDENTIFIER);
                        }
                    }
                    else if (!f.getType().isEnum()
                            /*
                         * BUG# 4096
                         * && !f.getType().isInterface()
                         * the bug here being that we were simply skipping interface.
                         * i.e consider the following class
                         *      public class GenericList<T>
                         *      {
                         *          public java.util.List<T> list1 = new java.util.ArrayList<T>();
                         *          public java.util.ArrayList<T> list2 = new java.util.ArrayList<T>();
                         *          public GenericList()
                         *          {
                         *          }
                         *      }
                         *
                         *  if had added a few items in list1 and list2 and then added object of GenereicClass in the cache. the code being generated was skipping the code for
                         *  public java.util.List<T> list1 as java.util.List<T> is an interface the other list, list2 was working with out any issues.
                         *  this fix makes sure that we don't skip interfaces.
                         */
                            && !f.getType().isMemberClass() && !f.getType().isLocalClass())
                    {
                        // Problem with double[] , float[] and short[] ... unable to call writObject directly, creating a local copy of the array and then passing
                        // on to writeObject solves the problem, reason not identified, no reply posted onto problem posted in JavaAssist forums or StackOverflow
                        // Throws exception if directly passes: java.lang.VerifyError: (class: testapp1/Dyn, method: processDouble signature: (Lsomething/Output;Ljava/lang/Object;)V) Inconsistent args_size for opc_invokeinterface
                        if (f.getType().getCanonicalName().equals("double[]") || f.getType().getCanonicalName().equals("float[]"))
                        {
                            // creates a local copy
                            serializeCode.append("\n\t" + f.getType().getCanonicalName() + " " + f.getName() + " = " + IDENTIFIER + "." + f.getName() + ";");
                            // passes
                            serializeCode.append("\n\toutput.writeObject(" + f.getName() + ");");
                        }
                        else if (f.getType().getCanonicalName().equals("short[]"))
                        {
                            //<editor-fold defaultstate="collapsed" desc="Unsigned">
                            if (unsignedTypes.containsKey(f.getName()))
                            {
                                serializeCode.append("\n\tif(" + IDENTIFIER + "." + f.getName() + " == null)\n\t{");
                                serializeCode.append("\n\t\toutput.writeObject(" + IDENTIFIER + "." + f.getName() + "); \n\t}");
                                serializeCode.append("\n\telse\n\t{");
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UnsignedByteArraySerializationSurrogate.class, _cacheContext);
                            writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\t\toutput.writeObject(" + IDENTIFIER + "." + f.getName()
                                        + ", com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UnsignedByteArraySerializationSurrogate.class);");
                                serializeCode.append("\n\t}");
                            }
                                    //</editor-fold>
                            else
                            {
                                serializeCode.append("\n\toutput.writeObject(" + IDENTIFIER + "." + f.getName() + ");");
                            }
                        }
                        else if (f.getType().getCanonicalName().equals("int[]"))
                        {
                            //<editor-fold defaultstate="collapsed" desc="Unsigned">
                            if (unsignedTypes.containsKey(f.getName()))
                            {
                                serializeCode.append("\n\tif(" + IDENTIFIER + "." + f.getName() + " == null)\n\t{");
                                serializeCode.append("\n\t\toutput.writeObject(" + IDENTIFIER + "." + f.getName() + "); \n\t}");
                                serializeCode.append("\n\telse\n\t{");
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UInt16ArraySerializationSurrogate.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\t\toutput.writeObject(" + IDENTIFIER + "." + f.getName()
                                        + ", com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt16ArraySerializationSurrogate.class);");
                                serializeCode.append("\n\t}");
                            }
                            //</editor-fold>
                            else
                            {
                                serializeCode.append("\n\toutput.writeObject(" + IDENTIFIER + "." + f.getName() + ");");
                            }
                        }
                        else if (f.getType().getCanonicalName().equals("long[]"))
                        {
                            // creates a local copy
                            serializeCode.append("\n\t" + f.getType().getCanonicalName() + " " + f.getName() + " = " + IDENTIFIER + "." + f.getName() + ";");
                            //<editor-fold defaultstate="collapsed" desc="Unsigned">
                            if (unsignedTypes.containsKey(f.getName()))
                            {
                                serializeCode.append("\n\tif(" + IDENTIFIER + "." + f.getName() + " == null)\n\t{");

                                // passes
                                serializeCode.append("\n\t\toutput.writeObject(" + f.getName() + ");\n\t}");
                                serializeCode.append("\n\telse\n\t{");
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UInt32ArraySerializationSurrogate.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\t\toutput.writeObject(" + f.getName()
                                        + ", com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt32ArraySerializationSurrogate.class);");
                                serializeCode.append("\n\t}");
                            }
                                    //</editor-fold>
                            else
                            {
                                // passes
                                serializeCode.append("\n\toutput.writeObject(" + f.getName() + ");");
                            }
                        }
                        else if (f.getType().getCanonicalName().equals("java.math.BigInteger[]"))
                        {
                            //<editor-fold defaultstate="collapsed" desc="Unsigned">
                            if (unsignedTypes.containsKey(f.getName()))
                            {
                                serializeCode.append("\n\tif(" + IDENTIFIER + "." + f.getName() + " == null)\n\t{");
                                serializeCode.append("\n\t\toutput.writeObject(" + IDENTIFIER + "." + f.getName() + "); \n\t}");
                                serializeCode.append("\n\telse\n\t{");
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UInt64ArraySerializationSurrogate.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\t\toutput.writeObject(" + IDENTIFIER + "." + f.getName()
                                        + ", com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt64ArraySerializationSurrogate.class);");
                                serializeCode.append("\n\t}");
                            }
                                    //</editor-fold>
                            else
                            {
                                serializeCode.append("\n\toutput.writeObject(" + IDENTIFIER + "." + f.getName() + ");");
                            }
                        }
                        else
                        {
                            if (f.getType().getName().equals("java.math.BigInteger") && unsignedTypes.containsKey(f.getName()))
                            {
                                SerializationSurrogate surr = _mSurrogateTypeSelector.getSurrogateForType(UInt64SerializationSurrogate.class, _cacheContext);
                                writeClassHandle(serializeCode, surr.getClassHandle());
                                serializeCode.append("\n\toutput.writeUInt64(" + IDENTIFIER + "." + f.getName() + ");");
                            }
                            else
                            {
                                serializeCode.append("\n\toutput.writeObject(" + IDENTIFIER + "." + f.getName() + ");");
                            }

                        }

                        //<editor-fold defaultstate="collapsed" desc="BigInteger">
                        if (f.getType().getName().equals("java.math.BigInteger"))
                        {
                            if (unsignedTypes.containsKey(f.getName()))
                            {
                                deserializePortableCode(deserializeCode, f, IDENTIFIER);
                            }
                            else
                            {
                                deserializePortableCode(deserializeCode,f, IDENTIFIER);
                            }
                        }
                        else
                        {
                            deserializePortableCode(deserializeCode,f, IDENTIFIER);
                        }
                        //</editor-fold>

                    }
                    else
                    {
                        if (!classCookie.contains(f.getClass().getCanonicalName()))
                        {
                            classCookie.add(f.getClass().getCanonicalName());
                            generateSerializationAndDeserializationMethods(f.getType(), serializeCode, deserializeCode, IDENTIFIER + "." + f.getName() + ".", /*(String[][]) _attributeOrder.get(obj.getCanonicalName())*/ null, classCookie, portable);
                        }
                    }
                }
                else
                {
                    serializeCode.append("\n\toutput.writeObject(new com.alachisoft.tayzgrid.serialization.standard.io.surrogates.SkipSerializationSurrogate());");
                    deserializeCode.append("\n\tinput.skipObject();");
                }
            }
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    private static Field[] GetAllFields(Class obj, String[][] attribOrder, Field[] fields, HashMap<String, String> unsignedTypes) throws Exception
    {
        if (attribOrder.length != 0)
        {
            Field[] tempField = new Field[attribOrder[0].length + 1];
            boolean EOF = true;
            for (int i = 0; i < (attribOrder[0].length + 1); i++)
            {
                if (i == (attribOrder[0].length) && EOF)
                {
                    break;
                }
                int number = i;
                if (!EOF)
                {
                    number = i - 1;
                }
                if (attribOrder[0][number] != "skip.attribute")
                {
                    if (attribOrder[1][number].equals("-1") && EOF)
                    {
                        tempField[i] = null;
                        EOF = false;
                        continue;
                    }
                    for (int j = 0; j < fields.length; j++)
                    {
                        if (attribOrder[0][number].contains("unsigned16.")
                                || attribOrder[0][number].contains("unsigned32.")
                                || attribOrder[0][number].contains("unsigned64.")
                                || attribOrder[0][number].contains("unsigned16[].")
                                || attribOrder[0][number].contains("unsigned32[].")
                                || attribOrder[0][number].contains("unsigned64[].")
                                || attribOrder[0][number].contains("unsignedbyte.")
                                || attribOrder[0][number].contains("unsignedbyte[]."))
                        {
                            String[] split = attribOrder[0][number].split("\\.");
                            unsignedTypes.put(split[1], split[0]);
                            attribOrder[0][number] = split[1];
                        }

                        if (attribOrder[0][number].equals(fields[j].getName().toString()))
                        {
                            tempField[i] = fields[j];
                        }
                    }
                    if (tempField[i] == null)
                    {
                        throw new Exception("Unable to intialize Compact Serialization: Assembly mismatch, The Assembly provided to NCManager is different to the one used locally: Unable to find Field "
                                + attribOrder[0][number] + " in " + obj.getCanonicalName());
                    }
                }
                else
                {
                    tempField[i] = null;
                }
            }
            return tempField;
        }
        else
        {
            return fields;
        }
    }

    private static void deserializePortableCode(StringBuffer deserializeCode, Field f, String IDENTIFIER)
    {
        deserializeCode.append("\n\tLOCAL_OBJECT = input.readObject();");
        deserializeCode.append("\n\tif(LOCAL_OBJECT instanceof com.alachisoft.tayzgrid.serialization.standard.io.surrogates.SkipSerializationSurrogate)");
        deserializeCode.append("\n\t{");
        deserializeCode.append("\n\t}");
        deserializeCode.append("\n\telse");
        deserializeCode.append("\n\t{");
        String canonicalName = getCanonicalName(f.getType().getCanonicalName());
        String assignmentString = getAssignmentString(f.getType().getCanonicalName());
        deserializeCode.append("\n\t\t" + canonicalName + " tempVariable = (" + canonicalName + ")LOCAL_OBJECT;");
        deserializeCode.append("\n\t\t" + IDENTIFIER + "." + f.getName() + " = " + assignmentString + ";");
        deserializeCode.append("\n\t}");
    }

    private static String getCanonicalName(String canonicalName)
    {

            if(canonicalName.equals("int"))
                return "java.lang.Integer";
            if(canonicalName.equals("short"))
                return "java.lang.Short";
            if(canonicalName.equals("long"))
                return "java.lang.Long";
            if(canonicalName.equals("char"))
                return "java.lang.Character";
            if(canonicalName.equals("double"))
                return "java.lang.Double";
            if(canonicalName.equals("byte"))
                return "java.lang.Byte";
            if(canonicalName.equals("float"))
                return "java.lang.Float";
            if(canonicalName.equals("boolean"))
                return "java.lang.Boolean";
            else
                return canonicalName;


    }

    private static void writeClassHandle(StringBuffer serializeCode, short classHandle)
    {
        serializeCode.append("\n\thandle = " + classHandle + ";");
        serializeCode.append("\n\toutput.writeShort(handle);");
    }

    private static String getAssignmentString(String canonicalName)
    {
        if(canonicalName.equals("int"))
                return "tempVariable.intValue()";
            if(canonicalName.equals("short"))
                return "tempVariable.shortValue()";
            if(canonicalName.equals("long"))
                return "tempVariable.longValue()";
            if(canonicalName.equals("char"))
                return "tempVariable.charValue()";
            if(canonicalName.equals("double"))
                return "tempVariable.doubleValue()";
            if(canonicalName.equals("byte"))
                return "tempVariable.byteValue()";
            if(canonicalName.equals("float"))
                return "tempVariable.floatValue()";
            if(canonicalName.equals("boolean"))
                return "tempVariable.booleanValue()";
            else
                return "tempVariable";
    }

    static enum Types
    {

        INT(0),
        SHORT(1),
        LONG(2),
        DOUBLE(3),
        FLOAT(4),
        BOOLEAN(5),
        CHAR(6),
        CHARS(7),
        BYTE(8),
        BYTES(9),
        UTF(10);

        Types(int v)
        {
        }
    }
}
