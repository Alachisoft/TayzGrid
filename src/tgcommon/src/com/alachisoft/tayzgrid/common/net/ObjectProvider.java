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

package com.alachisoft.tayzgrid.common.net;
public abstract class ObjectProvider {
	private java.util.HashMap _available = new java.util.HashMap();
	private java.util.HashMap _rented = new java.util.HashMap();
	private java.util.ArrayList _availableRentIds = new java.util.ArrayList();
	protected int _initialSize = 30;
	protected java.lang.Class _objectType;
	private Object _sync = new Object();
	private int _rentid = 1;

	private java.util.ArrayList list = new java.util.ArrayList();

	public ObjectProvider() {
		Initialize();
	}
	public ObjectProvider(int initialSize) {
		_initialSize = initialSize;
		Initialize();
	}
	public final void Initialize() {
		IRentableObject obj = null;

		synchronized (_sync) {
			for (int i = 0; i < _initialSize; i++) {
				obj = CreateObject();
				if (obj != null) {
					ResetObject(obj);
					list.add(obj);
				}
			}
		}
	}

	public final IRentableObject RentAnObject() {
		IRentableObject obj = null;
		synchronized (_sync) {
				if (_available.size() > 0) {
					obj = (IRentableObject)_available.get(_availableRentIds.get(0));
					_available.remove(obj.getRentId());
					_availableRentIds.remove(obj.getRentId());
					_rented.put(obj.getRentId(), obj);
				} else {
					obj = (IRentableObject)CreateObject();
					obj.setRentId(_rentid++);
					if (obj != null) {
						_rented.put(obj.getRentId(),obj);
					}
				}
		}

		return obj;
	}

	public final void SubmittObject(IRentableObject obj) {
		synchronized (_sync) {
		{
				if (_rented.containsKey(obj.getRentId())) {
					_rented.remove(obj.getRentId());
					ResetObject(obj);
					_available.put(obj.getRentId(),obj);
					_availableRentIds.add(obj.getRentId());
				}
		}
		}
	}

	protected abstract IRentableObject CreateObject();
	protected abstract void ResetObject(Object obj);
	public abstract java.lang.Class getObjectType();
	public abstract String getName();
	public final int getTotalObjects() {
		return _rented.size() + _available.size();
	}

	public final int getAvailableCount() {
		return _available.size();
	}

	public final int getRentCount() {
		return _rented.size();
	}

	public final int getInitialSize() {
		return _initialSize;
	}
	public final void setInitialSize(int value) {
		_initialSize = value;
	}
}
