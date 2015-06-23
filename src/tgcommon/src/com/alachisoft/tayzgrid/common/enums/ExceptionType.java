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

package com.alachisoft.tayzgrid.common.enums;

public enum ExceptionType {
	OPERATIONFAILED(0),
	AGGREGATE(1),
	CONFIGURATION(2),
	GENERALFAILURE(3),
	SECURITY(4),
	NOTSUPPORTED(5),
	MAX_CLIENTS_REACHED(6),
	STREAM_ALREADY_LOCKED(7),
	STREAM_CLOSED(8),
	STREAM_EXC(9),
	STREAM_INVALID_LOCK(10),
	STREAM_NOT_FOUND(11),
	TYPE_INDEX_NOT_FOUND(12),
	ATTRIBUTE_INDEX_NOT_FOUND(13),
	STATE_TRANSFER_EXCEPTION(14);

	private int intValue;
	private static java.util.HashMap<Integer, ExceptionType> mappings;
	private static java.util.HashMap<Integer, ExceptionType> getMappings() {
		if (mappings == null) {
			synchronized (ExceptionType.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, ExceptionType>();
				}
			}
		}
		return mappings;
	}

	private ExceptionType(int value) {
		intValue = value;
		ExceptionType.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static ExceptionType forValue(int value) {
		return getMappings().get(value);
	}
}
