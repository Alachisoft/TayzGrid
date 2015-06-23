
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

import com.alachisoft.tayzgrid.common.*;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAttribute;
import java.lang.reflect.Method;

public class ConfigurationValidator
{
    public ConfigurationValidator()
    {
    }

    public final boolean ValidateConfiguration(Object[] configuration) throws Exception
    {

        if (configuration != null)
        {
            for (Object cfgObject : configuration)
            {
                ValidateSingleCacheConfiguration(cfgObject);
            }
        }
        return true;
    }

    public final boolean ValidateSingleCacheConfiguration(Object cfgObject) throws Exception
    {
        String rootXmlStr = null;
        java.lang.Class type = cfgObject.getClass();
        Object[] cfgObjCustomAttribs = type.getAnnotations();

        if (cfgObjCustomAttribs != null && cfgObjCustomAttribs.length > 0)
        {
            for (int i = 0; i < cfgObjCustomAttribs.length; i++)
            {
                ConfigurationRootAttribute rootAttrib = (ConfigurationRootAttribute) ((cfgObjCustomAttribs[i] instanceof ConfigurationRootAttribute) ? cfgObjCustomAttribs[i] : null);
                if (rootAttrib != null)
                {
                    rootXmlStr = rootAttrib.getRootSectionName();
                }
            }
        }
        return ValidateConfigurationSection(cfgObject, rootXmlStr, 1);
    }

    private boolean ValidateConfigurationSection(Object configSection, String sectionName, int indent) throws Exception
    {
        java.lang.Class type = configSection.getClass();
        for (Method method : type.getMethods())
        {

            ToolsConfigurationAttributeAnnotation attribAnnotation = method.getAnnotation(ToolsConfigurationAttributeAnnotation.class);
            if (attribAnnotation != null)
            {
                Object methodValue = method.invoke(configSection);
                if (methodValue == null && Boolean.parseBoolean(attribAnnotation.isRequired()))
                {
                    throw new Exception("Error: " + attribAnnotation.attributeName() + " attribute is missing in the specified configuration.");
                }
            }

            ToolsConfigurationSectionAnnotation sectionAnnotation = method.getAnnotation(ToolsConfigurationSectionAnnotation.class);
            if (sectionAnnotation != null)
            {
                Object methodValue = method.invoke(configSection);
                if (methodValue != null)
                {
                    if (methodValue.getClass().isArray())
                    {
                        Object[] array = Common.as(methodValue, Object[].class);
                        if (array != null)
                        {
                            for (Object obj : array)
                            {
                                if (obj != null)
                                {
                                    ValidateConfigurationSection(obj, sectionAnnotation.sectionName(), indent + 1);
                                }
                            }
                        }
                    }
                    else
                    {
                        ValidateConfigurationSection(methodValue, sectionAnnotation.sectionName(), indent + 1);
                    }
                }
                else if (methodValue == null && Boolean.parseBoolean(attribAnnotation.isRequired()))
                {
                    throw new Exception("Error: " + sectionAnnotation.sectionName() + " section is missing in the specified configuration.");
                }
            }
        }
        return true;
    }
}
