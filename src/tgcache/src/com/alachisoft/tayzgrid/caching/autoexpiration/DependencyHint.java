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

package com.alachisoft.tayzgrid.caching.autoexpiration;

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.AppUtil;

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * Base class for dependency based item evictions.
 */
public abstract class DependencyHint extends ExpirationHint implements ICompactSerializable, java.io.Serializable {

    /**
     * The datetime to start monitoring after.
     */
    protected int _startAfter;

    /**
     * Constructor.
     */
    protected DependencyHint() {
 
        try {
            _startAfter = AppUtil.DiffSeconds(new java.util.Date());
        } catch (ArgumentException argumentException) {
        }
    }

    /**
     * Constructor.
     */
    protected DependencyHint(java.util.Date startAfter) {
        try {
            _startAfter = AppUtil.DiffSeconds(startAfter);
        } catch (ArgumentException argumentException) {
        }
    }

    /**
     * key to compare expiration hints.
     */
    @Override
    public final int getSortKey() {
        return _startAfter;
    }

    /**
     * virtual method that returns true when the expiration has taken place,
     * returns false otherwise.
     */
    @Override
    public boolean DetermineExpiration(CacheRuntimeContext context) {
        try {
            if ((new Integer(_startAfter)).compareTo(AppUtil.DiffSeconds(new java.util.Date())) > 0) {
                return false;
            }
        } catch (ArgumentException argumentException) {
        }

        if (!getHasExpired()) {
            if (getHasChanged()) {
                this.NotifyExpiration(this, null);
            }
        }
        return getHasExpired();
    }

    /**
     * method that returns true when the expiration has taken place, returns
     * false otherwise. Used only for those hints that are validated at the time
     * of Get operation on the cache.
     *
     * @param context
     * @return
     */
    @Override
    public boolean CheckExpired(CacheRuntimeContext context) {
        return DetermineExpiration(context);
    }


    /**
     * Gets a value indicating whether the CacheDependency object has changed.
     */
    public abstract boolean getHasChanged();

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        super.deserialize(reader);
        _startAfter = reader.readInt();
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        super.serialize(writer);
        writer.writeInt(_startAfter);
    }
}
