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

package com.alachisoft.tayzgrid.caching.topologies;

/**
 Enumeration that defines the result of a Put operation.
*/
public enum CacheAddResult
{
	/**  The item was added.
	*/
	Success,

	/**  The item was added but cache is near to full
	*/
	SuccessNearEviction,

	/**  The item already exists.
	*/
	KeyExists,
	/**  The operation failed, since there is not enough space.
	*/
	NeedsEviction,
	/**  General failure.
	*/
	Failure,
	/**
	 Apply only in case of partitioned caches.
	 This result is sent when a bucket has been transfered to another node
	 but it is not fully functionaly yet.
	 The operations must wait untile they get an indication that the bucket
	 has become fully functional on the new node.
	*/
	BucketTransfered,
	/**  Operation timedout on all of the nodes.
	*/
	FullTimeout,
	/**  Operation timedout on some of the nodes.
	*/
	PartialTimeout;

	public int getValue() {
		return this.ordinal();
	}

	public static CacheAddResult forValue(int value) {
		return values()[value];
	}
}