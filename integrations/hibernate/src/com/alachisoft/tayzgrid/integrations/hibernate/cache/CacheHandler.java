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

package com.alachisoft.tayzgrid.integrations.hibernate.cache;

import com.alachisoft.tayzgrid.web.caching.Cache;

public class CacheHandler {
    private com.alachisoft.tayzgrid.web.caching.Cache _cache=null;
    private int _refCount=0;
    
    public CacheHandler(String cacheName, boolean exceptionEnabled) throws Exception
    {
        _cache=com.alachisoft.tayzgrid.web.caching.TayzGrid.initializeCache(cacheName);
        _cache.setExceptionsEnabled(exceptionEnabled);
        _refCount++;
    }
    
    public Cache getCache()
    {
        return _cache;
    }
    
    public void incrementRefCount()
    {
        _refCount++;
    }
    
    public int decrementRefCount()
    {
        return --_refCount;
    }
    
    public void disposeCache() throws Exception
    {
        _cache.dispose();
        _cache=null;
    }
}
