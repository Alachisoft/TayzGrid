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

package com.alachisoft.tayzgrid.socketserver;

public final class MappingTable {
	private java.util.HashMap _mappingTable;
	private java.util.ArrayList _socketList;

	public MappingTable() {
		_mappingTable = new java.util.HashMap(25, 0.75f);
	}

	/**
	 Add cacheId in a list against cacheId in table

	 @param cacheId The cacheId
	 @param cacheId The cacheId to be stored
	*/
	public void Add(String cacheId, String socketId) {
		synchronized (this) { //: multiple threads can add cacheId to mappingTable.
			if (_mappingTable.containsKey(cacheId)) {
				_socketList = (java.util.ArrayList)_mappingTable.get(cacheId);
				_socketList.add(socketId);

				_mappingTable.put(cacheId, _socketList);
			} else {
				_socketList = new java.util.ArrayList(10);
				_socketList.add(socketId);

				_mappingTable.put(cacheId, _socketList);
			}
		}
	}

	/**
	 Get arraylist containing the cacheId's.

	 @param cacheId The cacheId
	 @return List of cacheId's
	*/
	public java.util.ArrayList Get(String cacheId) {
		return (java.util.ArrayList)_mappingTable.get(cacheId);
	}

	/**
	 Removes the cacheId from list, removes cacheId from table if there in no cacheId left

	 @param cacheId The cacheId
	 @param cacheId The cacheId to be removed
	*/
	public void Remove(String cacheId, String socketId) {
		if (_mappingTable == null) {
			return;
		}

		if (_mappingTable.containsKey(cacheId)) {
			_socketList = (java.util.ArrayList)_mappingTable.get(cacheId);
			_socketList.remove(socketId);

			if (_socketList.isEmpty()) {
				_mappingTable.remove(cacheId);
			} else {
				_mappingTable.put(cacheId, _socketList);
			}
		}
	}

	/**
	 Dispose the table object
	*/
	public void dispose() {
		if (_mappingTable == null) {
			return;
		}

		_mappingTable.clear();
		_mappingTable = null;
	}
}
