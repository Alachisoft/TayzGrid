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

package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.event.CacheListener;

/**
 * For New Changes in Events (Introduced in JvCache 4.1)
 * Client will maintain events registered against every 'Type' of notifications
 * registered
 * This wrapper is registered for the sole purpose that if the user registers
 * same instance of CacheEvent listeners multiple times then we have the ability to add them
 * in the 'LIST'
 * @author  
 */
class CacheListenerWrapper
{
    CacheListener _cacheEventListener = null;

    public CacheListenerWrapper(CacheListener listener)
    {
        this._cacheEventListener = listener;
    }



    /**
     * Holds one instance only, calling it multiple times overrides it
     * @param _cacheEventListener
     */
    public void setCacheEvent(CacheListener _cacheEvent)
    {
        this._cacheEventListener = _cacheEvent;
    }


    /**
     * Does what the name says
     * @return CacheListener it holds
     */
    public CacheListener getCacheEvent()
    {
        return _cacheEventListener;
    }


    /**
     * Verifies that the instance it holds is the same or not
     * @param listener Instance to check against
     * @return True if the instance is same, otherwise false.
     */
    public boolean verifyListenerInstance(CacheListener listener)
    {
        return _cacheEventListener == listener;
    }

    @Override
    public boolean equals(Object obj)
    {

        return ((CacheListenerWrapper)obj).getCacheEvent() == _cacheEventListener;
    }

    public boolean equals(CacheListenerWrapper obj)
    {
        return obj == _cacheEventListener;
    }

    @Override
    public int hashCode()
    {
        return _cacheEventListener.hashCode();
    }






}
