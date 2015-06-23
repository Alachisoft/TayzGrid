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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Provider implements InternalCompactSerializable, Cloneable
{

    private String providerName, assemblyName, className, _fullProviderName;
    private boolean isDefaultProvider;
    private boolean asyncMode;
    private boolean loaderOnly;
    private Parameter[] parameters;

    public Provider()
    {
    }

     @ConfigurationAttributeAnnotation(value = "loader-only", appendText = "")
    public final boolean getIsLoaderOnly() {
        return loaderOnly;
    }
     @ConfigurationAttributeAnnotation(value = "loader-only", appendText = "")
    public final void setIsLoaderOnly(boolean value) {
        loaderOnly = value;
    }
    
    @ConfigurationAttributeAnnotation(value = "provider-name", appendText = "")
    public final String getProviderName()
    {
        return providerName;
    }
    @ConfigurationAttributeAnnotation(value = "provider-name", appendText = "")
    public final void setProviderName(String value)
    {
        providerName = value;
    }

    @ConfigurationAttributeAnnotation(value = "assembly-name", appendText = "")
    public final String getAssemblyName()
    {
        return assemblyName;
    }
    @ConfigurationAttributeAnnotation(value = "assembly-name", appendText = "")
    public final void setAssemblyName(String value)
    {
        assemblyName = value;
    }

     @ConfigurationAttributeAnnotation(value = "class-name", appendText = "")
    public final String getClassName()
    {
        return className;
    }
    @ConfigurationAttributeAnnotation(value = "class-name", appendText = "")
    public final void setClassName(String value)
    {
        className = value;
    }

    @ConfigurationAttributeAnnotation(value = "full-name", appendText = "")
    public final String getFullProviderName()
    {
        return _fullProviderName;
    }
    @ConfigurationAttributeAnnotation(value = "full-name", appendText = "")
    public final void setFullProviderName(String value)
    {
        _fullProviderName = value;
    }

    @ConfigurationAttributeAnnotation(value = "default-provider", appendText = "")
    public final boolean getIsDefaultProvider()
    {
        return isDefaultProvider;
    }
    @ConfigurationAttributeAnnotation(value = "default-provider", appendText = "")
    public final void setIsDefaultProvider(boolean value)
    {
        isDefaultProvider = value;
    }

     @ConfigurationSectionAnnotation(value="parameters")//Changes for Dom param
    public final Parameter[] getParameters()
    {
        return parameters;
    }
     @ConfigurationSectionAnnotation(value="parameters")//Changes for Dom param
    public final void setParameters(Object value)
    {
        Object[] objs = (Object[])value;
        parameters=new Parameter[objs.length];
            for (int i = 0; i < objs.length; i++)
            {
               parameters[i]=(Parameter)objs[i];
            }
    }

    public final boolean getAsyncMode()
    {
        return asyncMode;
    }
    public final void setAsyncMode(boolean value)
    {
        asyncMode = value;
    }

    @Override
    public final Object clone()
    {
        Provider provider = new Provider();
        provider.setAsyncMode(getAsyncMode());
        provider.setProviderName(getProviderName() != null ? new String(getProviderName()) : null);
        provider.setAssemblyName(getAssemblyName() != null ? new String(getAssemblyName()) : null);
        provider.setClassName(getClassName() != null ? new String(getClassName()) : null);
        provider.setFullProviderName(getFullProviderName() != null ? new String(getFullProviderName()) : null);
        provider.setParameters(getParameters() != null ? (Parameter[]) ((getParameters().clone() instanceof Parameter[]) ? getParameters().clone() : null) : null);
        provider.setIsLoaderOnly(getIsLoaderOnly());
        return provider;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        providerName = (String) Common.readAs(reader.ReadObject(), String.class);
        assemblyName = (String) Common.readAs(reader.ReadObject(), String.class);
        className = (String) Common.readAs(reader.ReadObject(),String.class);
        _fullProviderName = (String) Common.readAs(reader.ReadObject(), String.class);
        isDefaultProvider = reader.ReadBoolean();
        asyncMode = reader.ReadBoolean();
        parameters = (Parameter[]) Common.readAs(reader.ReadObject(),Parameter[].class);
        loaderOnly = reader.ReadBoolean();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(providerName);
        writer.WriteObject(assemblyName);
        writer.WriteObject(className);
        writer.WriteObject(_fullProviderName);
        writer.Write(isDefaultProvider);
        writer.Write(asyncMode);
        writer.WriteObject(parameters);
        writer.Write(loaderOnly);
    }
}
