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

package com.alachisoft.tayzgrid.common.monitoring;

import java.io.Serializable;

public class ClientActivity implements Cloneable, Serializable {
	private java.util.ArrayList _activities = new java.util.ArrayList();
	public java.util.Date _startTime = new java.util.Date(0);
	public java.util.Date _endTime = new java.util.Date(0);
	public Thread _thread;

	public ClientActivity() {
		_startTime = new java.util.Date();
	}

	public final void LogActivity(String method, String log) {
		MethodActivity mActivity = new MethodActivity(method, log);
		synchronized (this) {
			_activities.add(mActivity);
		}
	}

	public final java.util.Date getStartTime() {
		return _startTime;
	}

	public final java.util.Date getEndTime() {
		return _endTime;
	}

	public final java.util.ArrayList getActivities() {
		return _activities;
	}
	public final void Clear() {
		synchronized (this) {
			_activities.clear();
		}
	}

	public final void StartActivity() {
	}

	public final void StopActivity() {
		_endTime = new java.util.Date();
	}



	public final Object clone() {
		ClientActivity clone = new ClientActivity();
		synchronized (this) {
			clone._startTime = _startTime;
			clone._endTime = _endTime;
			Object tempVar = _activities.clone();
			clone._activities = (java.util.ArrayList)((tempVar instanceof java.util.ArrayList) ? tempVar : null);
		}
		return clone;
	}

}
