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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.common.ResetableIterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Enumerator that provides single enumration over multiple containers.
 */
public class AggregateEnumerator implements ResetableIterator {

    /**
     * list of enumerators.
     */
    private ResetableIterator[] _enums = null;
    /**
     * index of the current enumerator.
     */
    private int _currId = 0;
    private Map.Entry _current;

    /**
     * Constructor.
     *
     * @param enums
     * @param first first enumerator.
     * @param second second enumerator.
     */
    public AggregateEnumerator(ResetableIterator[] enums) {
        _enums = enums;
        this.reset();
    }

    /**
     * Sets the enumerator to its initial position, which is before the first
     * element in the collection.
     */
    public void reset() {
        _currId = 0;
        _enums[_currId].reset();
    }

    /**
     * Advances the enumerator to the next element of the collection.
     *
     * @return true if the enumerator was successfully advanced to the next
     * element; false if the enumerator has passed the end of the collection.
     */
    private boolean MoveNext() {
        boolean result = _enums[_currId].hasNext();
        if (!result && _currId < _enums.length - 1) {
            _enums[++_currId].reset();
            result = _enums[_currId].hasNext();
            _current = (Entry) _enums[_currId].next();
        }
        return result;
    }

    /**
     * Gets the current element in the collection.
     */
    private Object getCurrent() {
        return _current;
    }

    /**
     * gets both the key and the value of the current dictionary entry.
     */
    private Map.Entry getEntry() {
        return _current;
    }

    /**
     * gets the key of the current dictionary entry.
     */
    private Object getKey() {
        return _current.getKey();
    }

    /**
     * gets the value of the current dictionary entry.
     */
    private Object getValue() {
        return _current.getValue();
    }

    @Override
    public boolean hasNext() {
        boolean result = _enums[_currId].hasNext();
        if (!result && _currId < _enums.length - 1) {
            _enums[++_currId].reset();
            result = _enums[_currId].hasNext();
        }
        return result;
    }

    @Override
    public Object next() {
        return _enums[_currId].next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
