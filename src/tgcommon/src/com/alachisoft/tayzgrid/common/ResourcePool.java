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

package com.alachisoft.tayzgrid.common;

import java.util.Iterator;

/**
 Contains the list of open sql connections. Sql7CacheDependency
 asks for the connection from the connection pool whenever required.
 The connection is added to the pool at the time of creation of the dependency and
 is removed from the connection pool when no dependency object is using it.
 For every interim call for a connection from the pool, its referrence count is
 incremented and referrence count is decremented when a dependency object, using it,
 disposes.
*/
public class ResourcePool implements IDisposable {
	public static class ResourceInfo {
		private Object _resource;
		private int _refCount;

		public ResourceInfo(Object resource) {
			_resource = resource;
		}

		public final int AddRef() {
			return ++_refCount;
		}
		public final int Release() {
			if (_refCount > 0) {
				--_refCount;
			}
			return _refCount;
		}

		public final Object getObject() {
			return _resource;
		}
		public final void setObject(Object value) {
			_resource = value;
		}
	}

	private java.util.HashMap resourceTable;

	/**
	 Static constructor. Initializes the static _connectionTable.
	*/
	public ResourcePool() {
		resourceTable = new java.util.HashMap();
	}


	/**
	 Performs application-defined tasks associated with freeing, releasing, or
	 resetting unmanaged resources.
	*/
	public final void dispose() {
		synchronized (this) {
			Iterator em = resourceTable.entrySet().iterator();
			while (em.hasNext()) {
				ResourceInfo res = (ResourceInfo)em.next();
				DisposeResource(res.getObject());
			}
		}
	}


	public final java.util.Collection getKeys() {
		return resourceTable.keySet();
	}

	public final int getCount() {
		return resourceTable.size();
	}

	/**
	 If available, returns the requested connection from the _connectionTable.
	 Otherwise, returns null.

	 @param key
	 @return
	*/
	public final Object GetResource(Object key) {
		ResourceInfo resourceInfo = (ResourceInfo)((resourceTable.get(key) instanceof ResourceInfo) ? resourceTable.get(key) : null);
		if (resourceInfo != null) {
			return resourceInfo.getObject();
		}
		return null;
	}

	/**
	 Add the resource to resource pool, and increase its reference count

	 @param key
	 @return
	*/
	public final void AddResource(Object key, Object value) {
		ResourceInfo resourceInfo = (ResourceInfo)((resourceTable.get(key) instanceof ResourceInfo) ? resourceTable.get(key) : null);

		if (resourceInfo != null) {
			if (value != null) {
				resourceInfo.setObject(value);
			}
		} else {
			resourceInfo = new ResourceInfo(value);
		}

		resourceTable.put(key, resourceInfo);
		resourceInfo.AddRef();
	}

	public final void AddResource(Object key, Object value, int numberOfCallbacks) {
		ResourceInfo resourceInfo = (ResourceInfo)((resourceTable.get(key) instanceof ResourceInfo) ? resourceTable.get(key) : null);

		if (resourceInfo != null) {
			if (value != null) {
				resourceInfo.setObject(value);
			}
		} else {
			resourceInfo = new ResourceInfo(value);
		}

		resourceTable.put(key, resourceInfo);

		for (int i = 0; i < numberOfCallbacks; i++) {
			resourceInfo.AddRef();
		}
	}


	/**
	 If available, returns the requested connection from the _connectionTable.
	 Otherwise, returns null.

	 @param key
	 @return
	*/
	public final Object RemoveResource(Object key) {
		ResourceInfo resourceInfo = (ResourceInfo)((resourceTable.get(key) instanceof ResourceInfo) ? resourceTable.get(key) : null);
		if (resourceInfo != null) {
			if (resourceInfo.Release() == 0) {
				resourceTable.remove(key);
				DisposeResource(resourceInfo.getObject());
			}
			return resourceInfo.getObject();
		}
		return null;
	}

	public final Object RemoveResource(Object key, int numberOfCallbacks) {
		ResourceInfo resourceInfo = (ResourceInfo)((resourceTable.get(key) instanceof ResourceInfo) ? resourceTable.get(key) : null);
		if (resourceInfo != null) {
			for (int i = 0; i < numberOfCallbacks; i++) {
				if (resourceInfo.Release() == 0) {
					resourceTable.remove(key);
					DisposeResource(resourceInfo.getObject());
				}
			}
			return resourceInfo.getObject();
		}
		return null;
	}

	/**
	 Removes the Severed Resource from the pool.

	 @param key
	*/
	public final void RemoveSeveredResource(Object key) {
		ResourceInfo resourceInfo = (ResourceInfo)((resourceTable.get(key) instanceof ResourceInfo) ? resourceTable.get(key) : null);
		if (resourceInfo != null) {
			resourceTable.remove(key);
			DisposeResource(resourceInfo.getObject());
		}
	}

	/**
	 Remove all the resources from resource table
	*/
	public final void RemoveAllResources() {
		synchronized (this) {
			java.util.Collection keys = resourceTable.keySet();
			java.util.Iterator ie = keys.iterator();

			while (ie.hasNext()) {
                            Object tempObj=ie.next();
				ResourceInfo resourceInfo = (ResourceInfo)((resourceTable.get(tempObj) instanceof ResourceInfo) ? resourceTable.get(tempObj) : null);
				if (resourceInfo != null) {
					DisposeResource(resourceInfo.getObject());
				}
			}

			resourceTable.clear();
		}
	}

	/**
	 If available, returns the requested connection from the _connectionTable.
	 Otherwise, returns null.

	 @param key
	 @return
	*/
	private void DisposeResource(Object res) {
		if (res instanceof IDisposable) {
			try {
				((IDisposable)res).dispose();
			} catch(RuntimeException e) {
			}
		}
	}
        
        public Object[] GetAllResourceKeys()
        {
            synchronized(this) {
                return resourceTable.keySet().toArray();
            }
        }
        
        public Object[] GetAllResourceValues()
        {
            synchronized(this) {
                return resourceTable.values().toArray();
            }                
        }        
}
