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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.common.net.Address;

public class WeightIdPair implements java.lang.Comparable {
	private int _bucketId;
	private long _weight;
	private Address _address;

	public WeightIdPair(int buckId, long weight, Address address) {
		_bucketId = buckId;
		_weight = weight;
		_address = address;
	}

	public final int getBucketId() {
		return _bucketId;
	}
	public final long getWeight() {
		return _weight;
	}

	public final Address getAddress() {
		return _address;
	}

	public final int compareTo(Object obj) {
		WeightIdPair wiPair = (WeightIdPair)obj;
		return (new Long(this._weight)).compareTo(wiPair.getWeight());
	}
}
