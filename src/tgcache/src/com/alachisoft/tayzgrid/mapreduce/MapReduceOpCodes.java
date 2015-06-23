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


public enum MapReduceOpCodes  {
    
    /**
     * Submit Task
     */
    SubmitMapReduceTask(1),  

    /**
     * Cancel Running Task
     */
    CancelTask(2),

    /**
     * Stop Running as well as waiting tasks.
     */
    CancelAllTasks(3),
    
    /**
     *Send Data for Reducer
     */
    SendReducerData(4),
    
    /**
     *Mapper Completed
     */
    MapperCompleted(5),
    
    /**
     *Reducer Completed
     */
    ReducerCompleted(6),
    
    /**
     * Reducer Failed
     */
    MapperFailed(7),
    
    /**
     * Reducer Failed
     */
    ReducerFailed(8),
    /**
     * Get TaskSequence
     */
    GetTaskSequence(9),
    
    /**
     * Register Task Notification
     */
    RegisterTaskNotification(10),
    
    /**
     * Unregister Task Notification
     */
    UnregisterTaskNotification(11),
    
    /**
     * Get List of taskIDs or Running tasks
     */
    GetRunningTasks(12),
    
    /**
     * Get the Stats and Progress of the task
     */

    GetTaskStats(13),    
    
    /**
     *Get Enumerator for Task Output
     */
    GetTaskEnumerator(14),
    
    /**
     *Get Enumerator for Task Output
     */
    GetNextRecord(15),
    
    /**
     * To start task
     */
    StartTask(16),
    
    RemoveFromSubmittedList(17),
    
    RemoveFromRunningList(18)
    ;


	private int intValue;
	private static java.util.HashMap<Integer, MapReduceOpCodes> mappings;
	private static java.util.HashMap<Integer, MapReduceOpCodes> getMappings() {
		if (mappings == null) {
			synchronized (MapReduceOpCodes.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, MapReduceOpCodes>();
				}
			}
		}
		return mappings;
	}

	private MapReduceOpCodes(int value) {
		intValue = value;
		MapReduceOpCodes.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static MapReduceOpCodes forValue(int value) {
		return getMappings().get(value);
	}
}
