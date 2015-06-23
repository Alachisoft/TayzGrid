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
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.util.HtmlEncoding;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Parameter implements Cloneable, InternalCompactSerializable
{

    private String name, paramValue;

    public Parameter()
    {
    }

    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public final String getName()
    {
        return name;
    }
    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public final void setName(String value)
    {
        name = value;
    }

    @ConfigurationAttributeAnnotation(value = "value", appendText = "")
    public final String getParamValue()
    {
        return  HtmlEncoding.encode(paramValue);
    }
    @ConfigurationAttributeAnnotation(value = "value", appendText = "")
    public final void setParamValue(String value)
    {
        paramValue = HtmlEncoding.decode(value);
    }

    @Override
    public final Object clone() throws CloneNotSupportedException
    {
        Parameter parameters = new Parameter();

        parameters.setName(getName() != null ? new String(getName()) : null);

        parameters.setParamValue(getParamValue() != null ? new String(getParamValue()) : null);
        return parameters;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        name = HtmlEncoding.encode((String) Common.readAs(reader.ReadObject(), String.class));
        paramValue = HtmlEncoding.encode((String) Common.readAs(reader.ReadObject(), String.class));
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(HtmlEncoding.decode(name));
        writer.WriteObject(HtmlEncoding.decode(paramValue));
    }
}
