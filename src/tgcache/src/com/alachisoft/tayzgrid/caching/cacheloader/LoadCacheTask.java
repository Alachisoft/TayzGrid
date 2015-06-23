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
package com.alachisoft.tayzgrid.caching.cacheloader;

import com.alachisoft.tayzgrid.common.IDisposable;

public class LoadCacheTask implements Runnable, IDisposable
{

    private CacheStartupLoader _cacheLoader;

    private boolean _isRunnung;

    public boolean isIsRunnung() {
        return _isRunnung;
    }

    public LoadCacheTask(CacheStartupLoader cacheLoader) {
        _cacheLoader = cacheLoader;
    }

    @Override
    public void run() {
        _isRunnung = true;
        try {
            _cacheLoader.LoadCache();

        } catch (Exception e2) {
            _isRunnung = false;
        }
        _isRunnung = false;
    }

    public final void dispose() {
        _isRunnung = false;
        ((IDisposable) _cacheLoader).dispose();
    }
}
