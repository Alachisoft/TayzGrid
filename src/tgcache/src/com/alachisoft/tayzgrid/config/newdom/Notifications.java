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
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Notifications implements Cloneable, InternalCompactSerializable
{

    private boolean itemRemove, itemAdd, itemUpdate ;
    private boolean cacheClear= true;

    public Notifications()
    {
    }

    @ConfigurationAttributeAnnotation(value = "item-remove", appendText = "")
    public final boolean getItemRemove()
    {
        return itemRemove;
    }

    @ConfigurationAttributeAnnotation(value = "item-remove", appendText = "")
    public final void setItemRemove(boolean value)
    {
        itemRemove = value;
    }

    @ConfigurationAttributeAnnotation(value = "item-add", appendText = "")
    public final boolean getItemAdd()
    {
        return itemAdd;
    }

    @ConfigurationAttributeAnnotation(value = "item-add", appendText = "")
    public final void setItemAdd(boolean value)
    {
        itemAdd = value;
    }

    @ConfigurationAttributeAnnotation(value = "item-update", appendText = "")
    public final boolean getItemUpdate()
    {
        return itemUpdate;
    }

    @ConfigurationAttributeAnnotation(value = "item-update", appendText = "")
    public final void setItemUpdate(boolean value)
    {
        itemUpdate = value;
    }

    public final boolean getCacheClear()
    {
        return cacheClear;
    }

    public final void setCacheClear(boolean value)
    {
        cacheClear = value;
    }

    @Override
    public final Object clone()
    {
        Notifications notifications = new Notifications();
        notifications.setItemAdd(getItemAdd());
        notifications.setItemRemove(getItemRemove());
        notifications.setItemUpdate(getItemUpdate());
        notifications.setCacheClear(getCacheClear());
        return notifications;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException
    {
        itemRemove = reader.ReadBoolean();
        itemAdd= reader.ReadBoolean();
        itemUpdate= reader.ReadBoolean();
        cacheClear = reader.ReadBoolean();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(itemRemove);
        writer.Write(itemAdd);
        writer.Write(itemUpdate);
        writer.Write(cacheClear);
    }
}
