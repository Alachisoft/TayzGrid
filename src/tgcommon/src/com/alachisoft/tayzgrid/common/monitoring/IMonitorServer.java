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

public interface IMonitorServer {
	void Initialize(String cacheId);

	java.util.ArrayList<CacheNodeStatistics> GetCacheStatistics();

	void RegisterEventViewerEvents(String[] sources);
	void UnRegisterEventViewerEvents();

	java.util.ArrayList<EventViewerEvent> GetLatestEvents();

	java.util.ArrayList<Node> GetCacheServers();

	java.util.ArrayList<ServerNode> GetRunningCacheServers();

	java.util.ArrayList<ClientNode> GetCacheClients();

	java.util.ArrayList<ConfiguredCacheInfo> GetAllConfiguredCaches();

	ConfiguredCacheInfo GetCacheConfigurationInfo();

	java.util.ArrayList<Node> GetUpdatedCacheServers();

	String GetClusterNIC();

	String GetNICForIP(String ip);

	String GetSocketServerNIC();

	java.util.ArrayList<ServerNode> GetUpdatedRunningCacheServers();

	java.util.ArrayList<ClientNode> GetUpdatedCacheClients();

	java.util.ArrayList<ClientProcessStats> GetClientProcessStats();

	int GetPercentageCPUUsage();
}
