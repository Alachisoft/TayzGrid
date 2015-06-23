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

import com.alachisoft.tayzgrid.common.net.ObjectProvider;
import com.alachisoft.tayzgrid.common.net.IRentableObject;

public class CompactEntryProvider extends ObjectProvider
{

    public CompactEntryProvider()
    {
        super();
    }

    public CompactEntryProvider(int initialSize)
    {
        super(initialSize);
    }

    @Override
    protected IRentableObject CreateObject()
    {
        return new CompactCacheEntry();
    }

    @Override
    protected void ResetObject(Object obj)
    {
        CompactCacheEntry entry = (CompactCacheEntry) ((obj instanceof CompactCacheEntry) ? obj : null);
        if (entry != null)
        {
            entry.Reset();
        }
    }

    @Override
    public java.lang.Class getObjectType()
    {
        if (_objectType == null)
        {
            _objectType = CompactCacheEntry.class;
        }
        return _objectType;
    }

    @Override
    public String getName()
    {
        return "CompactEntryProvider";
    }
}
