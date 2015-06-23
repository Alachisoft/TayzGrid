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

import com.alachisoft.tayzgrid.event.CustomListener;


public class CustomListenerWrapper
{
     CustomListener _cacheEventListener = null;

    public CustomListenerWrapper(CustomListener listener)
    {
        this._cacheEventListener = listener;
    }

    /**
     * Holds one instance only, calling it multiple times overrides it
     *
     * @param _cacheEventListener
     */
    public void setCustomEvent(CustomListener _cacheEvent)
    {
        this._cacheEventListener = _cacheEvent;
    }

    /**
     * Does what the name says
     *
     * @return CacheListener it holds
     */
    public CustomListener getCustomEvent()
    {
        return _cacheEventListener;
    }

    /**
     * Verifies that the instance it holds is the same or not
     *
     * @param listener Instance to check against
     * @return True if the instance is same, otherwise false.
     */
    public boolean verifyListenerInstance(CustomListener listener)
    {
        return _cacheEventListener == listener;
    }

    @Override
    public boolean equals(Object obj)
    {
        return ((CustomListenerWrapper)obj).getCustomEvent() == _cacheEventListener;
    }


}
