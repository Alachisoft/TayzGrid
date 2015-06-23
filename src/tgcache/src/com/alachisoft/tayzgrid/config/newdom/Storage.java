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
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Storage implements Cloneable, InternalCompactSerializable
{

    private String type;
    private long size;

    public Storage()
    {
    }

    @ConfigurationAttributeAnnotation(value="type",appendText="")
    public final String getType()
    {
        return type;
    }

    @ConfigurationAttributeAnnotation(value="type",appendText="")
    public final void setType(String value)
    {
        type = value;
    }

    @ConfigurationAttributeAnnotation(value="cache-size",appendText="mb")
    public final long getSize()
    {
        return size;
    }


    @ConfigurationAttributeAnnotation(value="cache-size",appendText="mb")
    public final void setSize(long value)
    {
        size = value;
    }


    @Override
    public final Object clone()
    {
        Storage storage = new Storage();
        storage.setType(getType() != null ? new String(getType()) : null);
        storage.setSize(size);
        return storage;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        type = (String) Common.readAs(reader.ReadObject(), String.class);
        size = reader.ReadInt64();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(type);
        writer.Write(size);
    }
}
