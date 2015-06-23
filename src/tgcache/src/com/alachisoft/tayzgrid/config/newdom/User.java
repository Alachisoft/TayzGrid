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

public class User implements Cloneable, InternalCompactSerializable
{

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        id = (String)Common.readAs(reader.ReadObject(),String.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(id);
    }

    private String id;

    public User()
    {
    }

    public User(String Name)
    {
        id = Name;
    }

    @ConfigurationAttributeAnnotation(value = "id", appendText = "")
    public final String getId()
    {
        return id;
    }

    @ConfigurationAttributeAnnotation(value = "id", appendText = "")
    public final void setId(String value)
    {
        id = value;
    }

    @Override
    public final Object clone()
    {
        User user = new User();
        user.setId(getId());
        return user;
    }
}
