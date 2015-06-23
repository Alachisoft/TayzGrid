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
public class QueryIdentifier 
{
	private String _query = "";
	private long _refCount = 0;

	public final String getQuery() {
		return _query;
	}
	public final void setQuery(String value) {
		_query = value;
	}

	public final long getReferenceCount() {
		return _refCount;
	}
	public final void setReferenceCount(long value) {
		_refCount = value;
	}

	public QueryIdentifier(String query) {
		_query = query;
		_refCount = 1;
	}

	public final void AddRef() {
		synchronized (this) {
			_refCount++;
		}
	}

	@Override
	public int hashCode() {
		return _query.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else {
			QueryIdentifier other = (QueryIdentifier)((obj instanceof QueryIdentifier) ? obj : null);
			if (other == null) {
				return _query.equals(obj.toString().toLowerCase());
			} else {
				return this.getQuery().equals(other.getQuery());
			}
		}
	}

	@Override
	public String toString() {
		return _query;
	}

	public final int compareTo(Object obj) {
		int result = 0;
		if (obj != null && obj instanceof QueryIdentifier) {
			QueryIdentifier other = (QueryIdentifier)obj;
			if (other._refCount > _refCount) {
				result = -1;
			} else if (other._refCount < _refCount) {
				result = 1;
			}
		}
		return result;
	}

}
