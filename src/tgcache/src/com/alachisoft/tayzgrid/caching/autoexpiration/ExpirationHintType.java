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

package com.alachisoft.tayzgrid.caching.autoexpiration;

public enum ExpirationHintType {
	NULL(-1),
	Parent(0),
	FixedExpiration(1),
	TTLExpiration(2),
	TTLIdleExpiration(3),
	FixedIdleExpiration(4),
	NodeExpiration(7),
	IdleExpiration(12),
	AggregateExpirationHint(13),	
	ExtensibleDependency(17);

	private int intValue;
	private static java.util.HashMap<Integer, ExpirationHintType> mappings;
	private static java.util.HashMap<Integer, ExpirationHintType> getMappings() {
		if (mappings == null) {
			synchronized (ExpirationHintType.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, ExpirationHintType>();
				}
			}
		}
		return mappings;
	}

	private ExpirationHintType(int value) {
		intValue = value;
		ExpirationHintType.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static ExpirationHintType forValue(int value) {
		return getMappings().get(value);
	}
}
