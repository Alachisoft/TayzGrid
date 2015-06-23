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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class QueryIndex implements Cloneable, InternalCompactSerializable
{

    Boolean indexForAll;
    private Class[] classes;

    public QueryIndex()
    {
    }

    @ConfigurationSectionAnnotation(value = "query-class")//Changes for Dom class
    public final Class[] getClasses()
    {
        return classes;
    }

    @ConfigurationSectionAnnotation(value = "query-class")//Changes for Dom class
    public final void setClasses(Object value)
    {
        if(value!=null){
        Object[] objs = (Object[])value;
        classes = new Class[objs.length];
        for (int i = 0; i < objs.length; i++)
        {
            classes[i] = (Class)objs[i];
        }
        }
    }

    @Override
    public final Object clone()
    {
        QueryIndex indexes = new QueryIndex();
        indexes.setClasses(getClasses() != null ? (Class[]) getClasses().clone() : null);
        return indexes;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        classes = (Class[])Common.readAs(reader.ReadObject(),Class[].class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(classes);
    }
}
