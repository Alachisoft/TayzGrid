
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

package com.alachisoft.tayzgrid.tools.common;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import java.lang.reflect.Method;

public class CommandLineArgumentParser {

    public static void CommandLineParser(tangible.RefObject<Object> obj, String[] args) throws Exception {
        ConfigurationBuilder configbuilder = new ConfigurationBuilder();
        java.lang.Class type = obj.argvalue.getClass();
        Method[] objMethods = type.getMethods();
        ArgumentAttributeAnnontation orphanAnnotation = null;
        Method orphanMethod = null;
        if (objMethods != null) {
            for (int i = 0; i < args.length; i++) {
                Boolean isAssinged = false;
                for (Method objMethod : objMethods) {
                    ArgumentAttributeAnnontation customAnnotation = objMethod.getAnnotation(ArgumentAttributeAnnontation.class);
                    if (customAnnotation != null) {
                        try {
                           if (customAnnotation.shortNotation().equals(args[i])
                                    || customAnnotation.fullNotation().toLowerCase().equals(args[i].toLowerCase()))
                           {
                                Class[] parameters = objMethod.getParameterTypes();
                                if (parameters.length == 0) {
                                    continue;
                                }
                                if (parameters[0].equals(boolean.class) || parameters[0].equals(Boolean.class)) {
                                    Boolean defaultValue = Boolean.parseBoolean(customAnnotation.defaultValue());
                                    objMethod.invoke(obj.argvalue, !defaultValue);
                                    isAssinged = true;
                                    break;
                                } else {
                                    int index = i + 1;
                                    if (index <= args.length - 1) {
                                        Object value = configbuilder.ConvertToPrimitive(parameters[0], args[++i], null);
                                        objMethod.invoke(obj.argvalue, value);
                                        isAssinged = true;
                                        break;
                                    }
                                }
                            } 
                           else if (customAnnotation.shortNotation() != null && customAnnotation.shortNotation().equals("")) 
                           {
                                Class[] parameters = objMethod.getParameterTypes();
                               
                                if (orphanAnnotation == null && !isAssinged&&parameters.length>0) {
                                    
                                    orphanAnnotation = customAnnotation;
                                    orphanMethod = objMethod;
                                }
                            }
                        } catch (Exception e) {
                            throw new Exception("Can not set the value for attribute " + customAnnotation.shortNotation() + " Errror :" + e.getMessage());
                        }
                    }
                }
                if (!isAssinged) {
                    if (orphanAnnotation != null && orphanMethod != null) {
                        if(args[i].toString().startsWith("-"))
                            throw new Exception("Invalid argument: '"+args[i].toString()+"'");
                        Class parameter = orphanMethod.getParameterTypes()[0];
                        Object value = configbuilder.ConvertToPrimitive(parameter, args[i], null);
                        orphanMethod.invoke(obj.argvalue, value);
                    }
                }
            }
        }
    }
}
