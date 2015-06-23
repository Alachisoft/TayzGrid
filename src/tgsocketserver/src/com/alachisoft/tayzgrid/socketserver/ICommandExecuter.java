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

import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.caching.SocketServerStats;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

public interface ICommandExecuter //extends IDisposable
{
	String getID();
	void OnClientConnected(String clientID, String cacheId)throws OperationFailedException;
	void OnClientDisconnected(String clientID, String cacheId)throws OperationFailedException;
	void UpdateSocketServerStats(SocketServerStats stats);
	void RegisterNotification(NotificationsType type);
	boolean IsCoordinator(String srcCacheUniqueID);
	void DisposeEnumerator(EnumerationPointer pointer) throws OperationFailedException;
        void dispose();
}
