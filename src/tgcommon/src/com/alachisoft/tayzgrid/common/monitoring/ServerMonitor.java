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

import java.util.Iterator;
import java.util.Map;

public class ServerMonitor
{

    private static boolean _monitor;
    private static java.util.HashMap _clientActivity = new java.util.HashMap();
    private static java.util.HashMap _threadClientMap = new java.util.HashMap();
    private static java.util.HashMap _dataStructures = new java.util.HashMap();
    private static java.util.HashMap _dedicatedThreadsActivity = new java.util.HashMap();
    private static java.util.Date _startTime = new java.util.Date(0);
    private static java.util.Date _endTime = new java.util.Date(0);
    private static Object _sync = new Object();

    public static java.util.Date getStartTime()
    {
        return _startTime;
    }

    public static java.util.Date getEndTime()
    {
        return _endTime;
    }

    public static boolean getMonitorActivity()
    {
        return _monitor;
    }

    public static void StartMonitoring()
    {
        synchronized (_sync)
        {
            if (_monitor)
            { 
                return;
            }
            _monitor = true;
            _startTime = new java.util.Date();
        }
    }

    public static void StopMonitoring()
    {
        synchronized (_sync)
        {
            if (_monitor)
            {
                _monitor = false;
                _endTime = new java.util.Date();
            }
        }
    }

    public static void RegisterClient(String clientID, String address)
    {
        if (clientID == null)
        {
            return;
        }
        synchronized (_clientActivity)
        {
            if (!_clientActivity.containsKey(clientID))
            {
                ClientMonitor clientActivity = new ClientMonitor(clientID, address);
                _clientActivity.put(clientID, clientActivity);
            }
        }
    }

    public static void UnregisterClient(String clientId)
    {
        if (clientId == null)
        {
            return;
        }
        synchronized (_clientActivity)
        {
            if (_clientActivity.containsKey(clientId))
            {
                _clientActivity.remove(clientId);
            }
        }
    }

    public static void StartClientActivity(String clientId)
    {
        if (clientId == null)
        {
            return;
        }
        if (_monitor)
        {
            ClientMonitor cMonitor = (ClientMonitor) ((_clientActivity.get(clientId) instanceof ClientMonitor) ? _clientActivity.get(clientId) : null);
            if (cMonitor != null)
            {
                cMonitor.StartActivity();
            }
            long tId = Thread.currentThread().getId();

            synchronized (_threadClientMap)
            {
                _threadClientMap.put(tId, cMonitor);
            }
        }
    }

    public static void StopClientActivity(String clientId)
    {

        if (_monitor)
        {
            ClientMonitor cMonitor = (ClientMonitor) ((_clientActivity.get(clientId) instanceof ClientMonitor) ? _clientActivity.get(clientId) : null);
            if (cMonitor != null)
            {
                cMonitor.StopActivity();
            }
            long tId = Thread.currentThread().getId();

            synchronized (_threadClientMap)
            {
                _threadClientMap.remove(tId);
            }
        }
    }

    public static void LogClientActivity(String method, String activity)
    {

        if (_monitor)
        {
            ClientMonitor cMonitor = (ClientMonitor) ((_threadClientMap.get(Thread.currentThread().getId()) instanceof ClientMonitor) ? _threadClientMap.get(Thread.currentThread().getId()) : null);
            if (cMonitor != null)
            {
                cMonitor.LogActivity(method, activity);
            }
        }
    }

    public static java.util.HashMap GetCompletedClientActivity()
    {
        java.util.HashMap activityTable = new java.util.HashMap();
        synchronized (_clientActivity)
        {
            Iterator ide = _clientActivity.entrySet().iterator();

            while (ide.hasNext())
            {
                Map.Entry pair = (Map.Entry)ide.next();
                ClientMonitor cMonitor = (ClientMonitor) ((pair.getValue() instanceof ClientMonitor) ? pair.getValue() : null);
                activityTable.put(cMonitor.getInfo(), cMonitor.GetCompletedClientActivities());
            }
        }
        return activityTable;
    }

    public static java.util.HashMap GetCurrentClientActivity() throws CloneNotSupportedException
    {
        java.util.HashMap activityTable = new java.util.HashMap();
        synchronized (_clientActivity)
        {
            Iterator ide = _clientActivity.entrySet().iterator();

            while (ide.hasNext())
            {
                Map.Entry pair = (Map.Entry)ide.next();
                ClientMonitor cMonitor = (ClientMonitor) ((pair.getValue() instanceof ClientMonitor) ? pair.getValue() : null);
                activityTable.put(cMonitor.getInfo(), cMonitor.GetCurrentClientActivities());
            }
        }
        return activityTable;
    }

    public static void Reset()
    {
        if (_clientActivity != null)
        {
            synchronized (_clientActivity)
            {
                _clientActivity.clear();
            }
        }
        if (_dedicatedThreadsActivity != null)
        {
            synchronized (_dedicatedThreadsActivity)
            {
                _dedicatedThreadsActivity.clear();
            }
        }
    }
}
