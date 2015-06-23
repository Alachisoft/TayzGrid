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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class ClientMonitor implements Serializable {
	private String _clientID;
	private String _address;
	private java.util.ArrayList _activities = new java.util.ArrayList();
	private java.util.HashMap _currentActivities = new HashMap();


	public ClientMonitor(String id, String address) {
		_clientID = id;
		_address = address;
	}

	public final String getID() {
		return _clientID;
	}

	public final String getAddress() {
		return _address;
	}

	public final ClienfInfo getInfo() {
		return new ClienfInfo(_clientID, _address);
	}

	public final void StartActivity() {
		ClientActivity acitvity = new ClientActivity();
		acitvity._thread = Thread.currentThread();
		long tId = Thread.currentThread().getId();


			_currentActivities.put(tId, acitvity);

	}
	public final void StopActivity() {
		long tId = Thread.currentThread().getId();
		ClientActivity activity = null;

			 activity = (ClientActivity)((_currentActivities.get(tId) instanceof ClientActivity) ? _currentActivities.get(tId) : null);
			_currentActivities.remove(tId);

		if (activity != null) {
			activity.StopActivity();

				_activities.add(activity);

		}
	}

	public final void LogActivity(String method, String log) {
		ClientActivity activity = (ClientActivity)((_currentActivities.get(Thread.currentThread().getId()) instanceof ClientActivity) ? _currentActivities.get(Thread.currentThread().getId()) : null);
		if (activity != null) {
			activity.LogActivity(method, log);
		}
	}

	public final void Clear() {

			_activities.clear();


			_currentActivities.clear();

	}

	public final java.util.ArrayList GetCompletedClientActivities() {
		java.util.ArrayList completedActivities = null;

			Object tempVar = _activities.clone();
			completedActivities = (java.util.ArrayList)((tempVar instanceof java.util.ArrayList) ? tempVar : null);


		return completedActivities;
	}

	public final java.util.ArrayList GetCurrentClientActivities() throws CloneNotSupportedException {
		java.util.ArrayList completedActivities = new java.util.ArrayList();

			Iterator ide = _currentActivities.entrySet().iterator();
			while (ide.hasNext()) {
                            Object obj = ide.next();
				if (obj != null) {
					completedActivities.add(((ClientActivity)obj).clone());
				}
			}

		return completedActivities;
	}
}
