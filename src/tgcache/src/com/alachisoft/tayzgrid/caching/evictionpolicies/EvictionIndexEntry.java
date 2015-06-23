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

package com.alachisoft.tayzgrid.caching.evictionpolicies;

import com.alachisoft.tayzgrid.common.ISizable;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;

public class EvictionIndexEntry implements ISizable {
    
	private java.util.HashMap _hintIndex = new java.util.HashMap();
	private long _previous = -1;
	private long _next = -1;
        
        private int _hintIndexMaxSize = 0;

	public final long getPrevious() {
		return _previous;
	}
	public final void setPrevious(long value) {
		_previous = value;
	}

	public final long getNext() {
		return _next;
	}
	public final void setNext(long value) {
		_next = value;
	}

	public final void Insert(Object key) {
            _hintIndex.put(key, null);
            
            if(_hintIndex.size() > _hintIndexMaxSize)
                _hintIndexMaxSize = _hintIndex.size();
	}

	public final boolean Remove(Object key) {
		if (_hintIndex != null) {
			_hintIndex.remove(key);
		}
		return _hintIndex.isEmpty();
	}

	public final java.util.Collection GetAllKeys() {
		return _hintIndex.keySet();
	}

	public final boolean Contains(Object key) {
		return _hintIndex.containsKey(key);
	}

    @Override
    public int getSize() {
        return this.getEvictionIndexEntrySize();
    }

    @Override
    public int getInMemorySize() {
        return this.getSize();
    }
    
    private int getEvictionIndexEntrySize()
    {
        int temp = 0;
        temp += MemoryUtil.NetReferenceSize;
        temp += MemoryUtil.NetLongSize;
        temp += MemoryUtil.NetLongSize;
        temp += MemoryUtil.NetIntSize;
        
        temp += _hintIndexMaxSize * MemoryUtil.NetHashtableOverHead;
        return temp;
    }
}
