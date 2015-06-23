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

package com.alachisoft.tayzgrid.common.logger;

public interface ILogger {
	/**
	 Start the cache logging functionality.
	*/
	String Initialize(java.util.Map properties, String partitionID, String cacheName) throws Exception;

	/**
	 Start the cache logging functionality.
	*/
	String Initialize(java.util.Map properties, String partitionID, String cacheName, boolean isStartedAsMirror, boolean inproc)throws Exception;

	/**
	 intitializes Known name based log files (will not log License Logs at service Startup

	 @param loggerName Enum of Known loggerNames
	*/
	void Initialize(LoggerNames loggerName)throws Exception;

	 /**
	 intitializes Known name based log files

	 @param loggerName Enum of Known loggerNames
	 @param cacheName cacheName 
	 */
	void Initialize(LoggerNames loggerNameEnum, String cacheName)throws Exception;

	/**
	 Stop the cache logging functionality.
	*/
	void Close();

	void Flush();

	void SetLevel(String levelName);

	void Error(String message);

	void Error(String module, String message);

	void Fatal(String message);

	void Fatal(String module, String message);

	void CriticalInfo(String message);

	void CriticalInfo(String module, String message);

	void DevTrace(String message);

	void DevTrace(String module, String message);

	void Info(String message);

	void Info(String module, String message);

	void Debug(String message);

	void Debug(String module, String message);

	void Warn(String message);

	void Warn(String module, String message);

	boolean getIsInfoEnabled();

	boolean getIsErrorEnabled();

	boolean getIsWarnEnabled();

	boolean getIsDebugEnabled();

	boolean getIsFatalEnabled();

}
