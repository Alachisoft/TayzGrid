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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.util.WildcardEnabledRegex;
import java.util.Iterator;

/**
 * special purpose single key store. this store is used in ActiveQueryIndex, an
 * extension of AttributeIndex, that is passed to re-evaluate an active query.
 */
public class HashStore implements IIndexStore, java.io.Serializable {

    private java.util.HashMap _store;

    public HashStore() {
        _store = new java.util.HashMap();
    }

    public final Object Add(Object key, Object value) {
        if (_store != null) {
            _store.put(key, value);
        }
        return null;
    }

    public final boolean Remove(Object key, Object value) {
        if (_store != null && _store.containsKey(key)) {
            _store.remove(key);
        }
        return true;
    }

    public final void Clear() {
        if (_store != null) {
            _store.clear();
        }
    }

    public final Iterator GetEnumerator() {
        if (_store != null) {
            return _store.entrySet().iterator();
        }

        return (new java.util.HashMap()).entrySet().iterator();
    }

    public final int getCount() {
        return _store != null ? _store.size() : 0;
    }

    public final java.util.ArrayList GetData(Object key, ComparisonType comparisonType) {
        java.util.ArrayList result = new java.util.ArrayList();
        java.lang.Comparable keyToCompare = (java.lang.Comparable) ((key instanceof java.lang.Comparable) ? key : null);

        if (_store != null) {
            switch (comparisonType) {
                case EQUALS:
                    if (_store.containsKey(key)) {
                        result.add(_store.get(key));
                    }

                    break;

                case NOT_EQUALS:
                    for (Object storedKey : _store.keySet()) {
                        if (((java.lang.Comparable) storedKey).compareTo(keyToCompare) != 0) {
                            result.add(_store.get(storedKey));
                        }
                    }

                    break;

                case LESS_THAN:
                    for (Object storedKey : _store.keySet()) {
                        if (((java.lang.Comparable) storedKey).compareTo(keyToCompare) < 0) {
                            result.add(_store.get(storedKey));
                        }
                    }
                    break;

                case GREATER_THAN:
                    for (Object storedKey : _store.keySet()) {
                        if (((java.lang.Comparable) storedKey).compareTo(keyToCompare) > 0) {
                            result.add(_store.get(storedKey));
                        }
                    }
                    break;

                case LESS_THAN_EQUALS:
                    for (Object storedKey : _store.keySet()) {
                        if (((java.lang.Comparable) storedKey).compareTo(keyToCompare) <= 0) {
                            result.add(_store.get(storedKey));
                        }
                    }
                    break;

                case GREATER_THAN_EQUALS:
                    for (Object storedKey : _store.keySet()) {
                        if (((java.lang.Comparable) storedKey).compareTo(keyToCompare) >= 0) {
                            result.add(_store.get(storedKey));
                        }
                    }
                    break;

                case LIKE:
                    for (Object storedKey : _store.keySet()) {
                        String pattern = (String) ((key instanceof String) ? key : null);
                        WildcardEnabledRegex regex = new WildcardEnabledRegex(pattern);

                        if (storedKey instanceof String) {
                            if (regex.IsMatch((String) storedKey)) {
                                result.add(_store.get(key));
                            }
                        }
                    }

                    break;

                case NOT_LIKE:
                    for (Object storedKey : _store.keySet()) {
                        String pattern = (String) ((key instanceof String) ? key : null);
                        WildcardEnabledRegex regex = new WildcardEnabledRegex(pattern);

                        if (storedKey instanceof String) {
                            if (!regex.IsMatch((String) storedKey)) {
                                result.add(_store.get(key));
                            }
                        }
                    }
                    break;
            }
        }
        return result;
    }

    @Override
    public long getIndexInMemorySize() {
        throw new UnsupportedOperationException("HashStore.getIndexInMemorySize()");
    }
}
