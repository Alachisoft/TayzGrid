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
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class ProviderAssembly implements Cloneable, InternalCompactSerializable
{

    private String assemblyName, className, _fullProviderName;

    public ProviderAssembly()
    {
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

    public final Object clone() throws CloneNotSupportedException
    {
        ProviderAssembly provider = new ProviderAssembly();

        provider.setAssemblyName(getAssemblyName() != null ? new String(getAssemblyName()) : null);
        provider.setClassName(getClassName() != null ? new String(getClassName()) : null);
        provider.setFullProviderName(getFullProviderName() != null ? new String(getFullProviderName()) : null);
        return provider;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
         assemblyName=(String) Common.readAs(reader.ReadObject(), String.class);
          className= (String) Common.readAs(reader.ReadObject(), String.class);
        _fullProviderName= (String) Common.readAs(reader.ReadObject(), String.class);

    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(assemblyName);
        writer.WriteObject(className);
        writer.WriteObject(_fullProviderName);

    }
}
