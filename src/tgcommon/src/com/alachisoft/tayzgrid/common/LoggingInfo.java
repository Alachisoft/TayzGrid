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

package com.alachisoft.tayzgrid.common;


import java.io.Serializable;


/** 
 Provide information about client or server logging.
*/
public final class LoggingInfo implements Serializable {
	private java.util.HashMap<LoggingType, LogsStatus> _logMap;

	/** 
	 Defines the subsystem
	*/
	public enum LoggingSubsystem {
		/** 
		 Socket server
		*/
		Server,

		/** 
		 Remote clients
		*/
		Client;

		public int getValue() {
			return this.ordinal();
		}

		public static LoggingSubsystem forValue(int value) {
			return values()[value];
		}
	}

	/** 
	 Defines status of client and server side logging
	*/
	public enum LogsStatus {
		/** 
		 Disable logging
		*/
		Disable(0),

		/** 
		 Enable logging
		*/
		Enable(1);

		/** 
		 Keep the current value
		*/
		//Unchanged

		private int intValue;
		private static java.util.HashMap<Integer, LogsStatus> mappings;
		private static java.util.HashMap<Integer, LogsStatus> getMappings() {
			if (mappings == null) {
				synchronized (LogsStatus.class) {
					if (mappings == null) {
						mappings = new java.util.HashMap<Integer, LogsStatus>();
					}
				}
			}
			return mappings;
		}

		private LogsStatus(int value) {
			intValue = value;
			LogsStatus.getMappings().put(value, this);
		}

		public int getValue() {
			return intValue;
		}

		public static LogsStatus forValue(int value) {
			return getMappings().get(value);
		}
	}

	/** 
	 Type of logging
	*/
	public enum LoggingType {
		/** 
		 Log only exception and unexpected behaviours
		*/
		Error(0x1),

		/** 
		 Log all information related to important operations
		*/
		Detailed(0x2);

		private int intValue;
		private static java.util.HashMap<Integer, LoggingType> mappings;
		private static java.util.HashMap<Integer, LoggingType> getMappings() {
			if (mappings == null) {
				synchronized (LoggingType.class) {
					if (mappings == null) {
						mappings = new java.util.HashMap<Integer, LoggingType>();
					}
				}
			}
			return mappings;
		}

		private LoggingType(int value) {
			intValue = value;
			LoggingType.getMappings().put(value, this);
		}

		public int getValue() {
			return intValue;
		}

		public static LoggingType forValue(int value) {
			return getMappings().get(value);
		}
	}

	/** 
	 Create a new logging information
	*/
	public LoggingInfo() {
		this._logMap = new java.util.HashMap<LoggingType, LogsStatus>(2);

		this._logMap.put(LoggingType.Error, LogsStatus.Disable);
		this._logMap.put(LoggingType.Detailed, LogsStatus.Disable);
	}

	/** 
	 Set logging status
	 
	 @param type Type of logging to set status for
	 @param status Status of logging
	*/
	public void SetStatus(LoggingType type, LogsStatus status) {
			this._logMap.put(type, status);
	}

	/** 
	 Get logging status for a specified type
	 
	 @param type Type of logging
	 @return Logging status for that type
	*/
	public LogsStatus GetStatus(LoggingType type) {
		return LogsStatus.Disable;

	}
}
