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

package com.alachisoft.tayzgrid.mapreduce;


public enum TaskType  {

    /**
     * Submit Task
     */
    MapReduce(1),

    /**
     *Start Submitted Task
     */
    EntityProcessor(2),

    /**
     *Stop Running Task
     */
    Aggregator(3)
    ;

	private int intValue;
	private static java.util.HashMap<Integer, TaskType> mappings;
	private static java.util.HashMap<Integer, TaskType> getMappings() {
		if (mappings == null) {
			synchronized (MapReduceOpCodes.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, TaskType>();
				}
			}
		}
		return mappings;
	}

	private TaskType(int value) {
		intValue = value;
		TaskType.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static TaskType forValue(int value) {
		return getMappings().get(value);
	}
}
