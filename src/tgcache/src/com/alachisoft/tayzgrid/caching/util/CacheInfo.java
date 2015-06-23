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

import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;

/**
 A class used to hold cache information
*/
public class CacheInfo {
	/**  The name of the cache instance.
	*/
	private String _name = "";
	/**  The scheme of the cache.
	*/
	private String _class = "";
	/**  The property string used to create the cache.
	*/
	private String _configString = "";

	private String _currentPartId;

	private CacheServerConfig _config;

	public final String getCurrentPartitionId() {
		return _currentPartId;
	}
	public final void setCurrentPartitionId(String value) {
		_currentPartId = value;
	}

	public final String getName() {
		return _name;
	}
	public final void setName(String value) {
		_name = value;
	}

	public final String getClassName() {
		return _class;
	}
	public final void setClassName(String value) {
		_class = value;
	}

	public final String getConfigString() {
		return _configString;
	}
	public final void setConfigString(String value) {
		_configString = value;
	}

	public final CacheServerConfig getConfiguration() {
		return this._config;
	}
	public final void setConfiguration(CacheServerConfig value) {
		this._config = value;
	}

	/**
	 Gets the value which indicates whether the cache is clustered cache or not.
	*/
	public final boolean getIsClusteredCache() {
		boolean isClustered = false;
		if (_class != null) {
			if (_class.compareTo("replicated-server") == 0 || _class.compareTo("partitioned-server") == 0) {
				isClustered = true;
			}
		}
		return isClustered;
	}
}
