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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class EventStatus implements  InternalCompactSerializable, java.io.Serializable
{
    private boolean _cacheCleared = false;
    private boolean _itemAdded = false;
    private boolean _itemUpdated = false;
    private boolean _itemRemoved = false;

    public EventStatus()
    { }

    public final boolean getIsCacheClearedEvent()
    {
            return _cacheCleared;
    }

    public final void setIsCacheClearedEvent(boolean value)
    {
            _cacheCleared = value;
    }

    public final boolean getIsItemAddedEvent()
    {
            return _itemAdded;
    }

    public final void setIsItemAddedEvent(boolean value)
    {
            _itemAdded = value;
    }

    public final boolean getIsItemUpdatedEvent()
    {
            return _itemUpdated;
    }

    public final void setIsItemUpdatedEvent(boolean value)
    {
            _itemUpdated = value;
    }

    public final boolean getIsItemRemovedEvent()
    {
            return _itemRemoved;
    }

    public final void setIsItemRemovedEvent(boolean value)
    {
            _itemRemoved = value;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
            _cacheCleared = reader.ReadBoolean();
            _itemAdded = reader.ReadBoolean();
            _itemRemoved = reader.ReadBoolean();
            _itemUpdated = reader.ReadBoolean();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
            writer.Write(_cacheCleared);
            writer.Write(_itemAdded);
            writer.Write(_itemRemoved);
            writer.Write(_itemUpdated);
    }
 }
