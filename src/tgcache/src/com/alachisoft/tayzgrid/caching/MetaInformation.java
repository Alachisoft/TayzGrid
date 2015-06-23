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

import java.util.Iterator;
import java.util.Map;

/**
 Contains an object's attribute names and corresponding values. This class is used to retrieve attribute
 values if required during query execution because we cannot retrieve these values from actual object that
 that is stored in cache in binary form.
*/
public class MetaInformation {
	private java.util.HashMap _attributeValues;
	private Object _cacheKey;
	private String _type;

	public MetaInformation(java.util.HashMap attributeValues) {
		_attributeValues = attributeValues;
	}

	public final Object getCacheKey() {
		return _cacheKey;
	}
	public final void setCacheKey(Object value) {
		_cacheKey = value;
	}

	public final String getType() {
		return _type;
	}
	public final void setType(String value) {
		_type = value;
	}

	public final Object getItem(String key) {
		return _attributeValues == null ? null : _attributeValues.get(key);
        }

	public final void setItem(String key, Object value) {
		if (_attributeValues == null) {
			_attributeValues = new java.util.HashMap();
		}

		_attributeValues.put(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		MetaInformation other = (MetaInformation)((obj instanceof MetaInformation) ? obj : null);

		if (other != null) {
			result = this.getCacheKey().equals(other.getCacheKey());
		}

		return result;
	}

	@Override
	public int hashCode() {
		return this.getCacheKey().hashCode();
	}

	public final boolean IsAttributeIndexed(String attribName) {
		if (_attributeValues.containsKey(attribName))
		{
			return true;
		}
		return false;
	}

    public final void Add(java.util.HashMap attributeValues)
    {
        Iterator it = attributeValues.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry pair = (Map.Entry) it.next();
            _attributeValues.put(pair.getKey(), pair.getValue());
        }
    }

    public final java.util.HashMap getAttributeValues()
    {
        if (_attributeValues != null)
        {
            java.util.HashMap result = new java.util.HashMap();
            Iterator it = _attributeValues.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry) it.next();
                if (entry.getValue() instanceof String)
                {
                    result.put(entry.getKey(), ((String) entry.getValue()).toLowerCase());
                }
                else
                {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
            return result;
        }
        return _attributeValues;
    }
}
