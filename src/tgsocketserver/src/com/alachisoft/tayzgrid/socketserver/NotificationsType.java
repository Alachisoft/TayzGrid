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

public enum NotificationsType {
	RegAddNotif(1),
	RegUpdateNotif(2),
	RegRemoveNotif(3),
	RegClearNotif(4),
	RegCustomNotif(5),
	RegNodeJoinedNotif(6),
	RegNodeLeftNotif(7),
	RegCacheStoppedNotif(8),
	RegHashmapChangedNotif(9),
	UnregAddNotif(100),
	UnregUpdateNotif(101),
	UnregRemoveNotif(102),
	UnregClearNotif(103),
	UnregCustomNotif(104),
	UnregNodeJoinedNotif(105),
	UnregNodeLeftNotif(106),
	UnregCacheStoppedNotif(107),
	UnregHashmapChangedNotif(108),
        MapReduceTaskNotif(109);

	private int intValue;
	private static java.util.HashMap<Integer, NotificationsType> mappings;
	private static java.util.HashMap<Integer, NotificationsType> getMappings() {
		if (mappings == null) {
			synchronized (NotificationsType.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, NotificationsType>();
				}
			}
		}
		return mappings;
	}

	private NotificationsType(int value) {
		intValue = value;
		NotificationsType.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static NotificationsType forValue(int value) {
		return getMappings().get(value);
	}
}
