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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class CacheLoader implements Cloneable, InternalCompactSerializable
{

    private boolean enabled;
    private int retries, retryInterval;
    private ProviderAssembly provider;
    private Parameter[] parameters;

    public CacheLoader()
    {
    }

    @ConfigurationAttributeAnnotation(value = "retries", appendText = "")
    public final int getRetries()
    {
        return retries;
    }

    @ConfigurationAttributeAnnotation(value = "retries", appendText = "")
    public final void setRetries(int value)
    {
        retries = value;
    }

    @ConfigurationAttributeAnnotation(value = "retry-interval", appendText = "")
    public final int getRetryInterval()
    {
        return retryInterval;
    }

    @ConfigurationAttributeAnnotation(value = "retry-interval", appendText = "")
    public final void setRetryInterval(int value)
    {
        retryInterval = value;
    }

    @ConfigurationAttributeAnnotation(value = "enable-loader", appendText = "")//Changes for Dom enabled
    public final boolean getEnabled()
    {
        return enabled;
    }

    @ConfigurationAttributeAnnotation(value = "enable-loader", appendText = "")//Changes for Dom enabled
    public final void setEnabled(boolean value)
    {
        enabled = value;
    }

    @ConfigurationSectionAnnotation(value = "provider")
    public final ProviderAssembly getProvider()
    {
        return provider;
    }

     @ConfigurationSectionAnnotation(value = "provider")
    public final void setProvider(ProviderAssembly value)
    {
        provider = value;
    }

    @ConfigurationSectionAnnotation(value = "parameters")//Changes for Dom from param
    public final Parameter[] getParameters()
    {
        return parameters;
    }

    @ConfigurationSectionAnnotation(value = "parameters")//Changes for Dom param
    public final void setParameters(Object value)
    {
        Object[] objs = (Object[])value;
        parameters=new Parameter[objs.length];
            for (int i = 0; i < objs.length; i++)
            {
               parameters[i]=(Parameter)objs[i];
            }
        //parameters = value;
    }

    @Override
    public final Object clone() throws java.lang.CloneNotSupportedException
    {
        CacheLoader loader = new CacheLoader();
        loader.enabled = enabled;
        loader.retries = retries;
        loader.retryInterval = retryInterval;
        loader.provider = provider != null ? (ProviderAssembly) provider.clone() : null;
        loader.parameters = parameters != null ? (Parameter[]) parameters.clone() : null;
        return loader;
    }
    
    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        enabled = reader.ReadBoolean();
        retries = reader.ReadInt32();
        retryInterval = reader.ReadInt32();
        provider = (ProviderAssembly) Common.readAs(reader.ReadObject(), ProviderAssembly.class);
        parameters = (Parameter[]) Common.readAs(reader.ReadObject(), Parameter[].class);

    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(enabled);
        writer.Write(retries);
        writer.Write(retryInterval);
        writer.WriteObject(provider);
        writer.WriteObject(parameters);

    }
}
