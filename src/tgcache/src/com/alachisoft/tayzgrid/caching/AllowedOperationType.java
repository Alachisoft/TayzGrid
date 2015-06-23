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

package com.alachisoft.tayzgrid.caching;

public enum AllowedOperationType
{
		AtomicRead(1),
		AtomicWrite(2),
		BulkRead(3),
		BulkWrite(4),
		InternalCommand(5),
		ClusterRead(6);

		private int intValue;
		private static java.util.HashMap<Integer, AllowedOperationType> mappings;
		private static java.util.HashMap<Integer, AllowedOperationType> getMappings()
		{
			if (mappings == null)
			{
				synchronized (AllowedOperationType.class)
				{
					if (mappings == null)
					{
						mappings = new java.util.HashMap<Integer, AllowedOperationType>();
					}
				}
			}
			return mappings;
		}

		private AllowedOperationType(int value)
		{
			intValue = value;
			AllowedOperationType.getMappings().put(value, this);
		}

		public int getValue()
		{
			return intValue;
		}

		public static AllowedOperationType forValue(int value)
		{
			return getMappings().get(value);
		}
}
