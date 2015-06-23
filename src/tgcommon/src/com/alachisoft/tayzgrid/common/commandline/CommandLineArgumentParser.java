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
package com.alachisoft.tayzgrid.common.commandline;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CommandLineArgumentParser
{

    public static void Parse(Object obj, String[] args) throws IllegalArgumentException, Exception
    {
        ConfigurationBuilder configbuilder = new ConfigurationBuilder();
        java.lang.Class type = obj.getClass();
        Method[] objMethods = type.getMethods();
        ArgumentAttributeAnnotation orphanAnnotation = null;
        Method orphanMethod = null;
        if (objMethods != null)
        {
            for (int i = 0; i < args.length; i++)
            {
                Boolean isAssinged = false;
                for (Method objMethod : objMethods)
                {
                    ArgumentAttributeAnnotation customAnnotation = objMethod.getAnnotation(ArgumentAttributeAnnotation.class);
                    if (customAnnotation != null)
                    {
                        try
                        {
                            java.util.List parameters = Arrays.asList(objMethod.getParameterTypes());
                            if (customAnnotation.shortNotation().equals(args[i].toLowerCase())
                                    || customAnnotation.fullNotation().toLowerCase().equals(args[i].toLowerCase()))
                            {
                                if (parameters.isEmpty())
                                {
                                    continue;
                                }
                                Class firstParam = (Class) parameters.get(0);
                                if (firstParam.equals(boolean.class) || firstParam.equals(Boolean.class))
                                {
                                    Boolean defaultValue = Boolean.parseBoolean(customAnnotation.defaultValue());
                                    objMethod.invoke(obj, !defaultValue);
                                    isAssinged = true;
                                    break;
                                }
                                else
                                {
                                    int index = i + 1;
                                    if (index <= args.length - 1)
                                    {
                                        Object value = configbuilder.ConvertToPrimitive(firstParam, args[++i], null);
                                        objMethod.invoke(obj, value);
                                        isAssinged = true;
                                        break;
                                    }
                                }
                            }
                            else if (customAnnotation.shortNotation() != null && customAnnotation.shortNotation().equals(""))
                            {
                                if (orphanAnnotation == null && !isAssinged && parameters.size() > 0)
                                {
                                    orphanAnnotation = customAnnotation;
                                    orphanMethod = objMethod;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            throw new Exception("Can not set the value for attribute " + customAnnotation.shortNotation() + " Errror :" + e.getMessage());
                        }
                    }
                }
                if (!isAssinged)
                {
                    if (orphanAnnotation != null && orphanMethod != null)
                    {
                        if (args[i].toString().startsWith("-"))
                        {
                            throw new IllegalArgumentException("Invalid argument: '" + args[i].toString() + "'");
                        }
                        Class parameter = orphanMethod.getParameterTypes()[0];
                        Object value = configbuilder.ConvertToPrimitive(parameter, args[i], null);
                        orphanMethod.invoke(obj, value);
                    }
                    else
                    {
                        if (args[i].toString().startsWith("-"))
                        {
                            throw new IllegalArgumentException("Invalid argument: '" + args[i].toString() + "'");
                        }
                    }
                }
            }
        }
    }
}
