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

import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.event.NEvent;
import com.alachisoft.tayzgrid.common.event.NEventEnd;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.monitoring.EventLogEntryType;
import com.alachisoft.tayzgrid.common.monitoring.EventViewerEvent;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class EventLogger implements NEventStart
{
    private static long _instanceId = 0;
    private static IEventLogger _eventlogger = null;
    private static JLogger _fileLogger = null;
    private static boolean recognizedOS = true;
    private static String lastLoggedEvent;
    private static NEvent _nevent = new NEvent("OnEventLogEntry", _fileLogger);
    private static ArrayList<EventViewerEvent> _events;


    public static String getLastLoggedEvent()
    {
        return lastLoggedEvent;
    }

    public static void setLastLoggedEvent(String msg, String type)
    {
        EventLogger.lastLoggedEvent = type.concat(msg);
    }

    public static void Initialize()
    {
        _events = new ArrayList<EventViewerEvent>();
        _fileLogger = new JLogger();
        lastLoggedEvent = new String();
        if (_eventlogger == null)
        {
            if (RuntimeUtil.getCurrentOS() == RuntimeUtil.OS.Windows)
            {
                _eventlogger = new JWindowsLogger();
                _eventlogger.initialize();
            }
            else if (RuntimeUtil.getCurrentOS() == RuntimeUtil.OS.Linux)
            {
                _eventlogger = new JLinuxLogger();
            }
            else
            {
                _eventlogger = new JLinuxLogger();
                recognizedOS = false;
            }

            try
            {
                _fileLogger.Initialize(LoggerNames.EventLogs, true);
            }
            catch (Exception ex)
            {
            }
            _eventlogger.setLogger(_fileLogger);

            if (!recognizedOS)
            {
                _eventlogger.LogEvent("OS is not recognized: " + System.getProperty("os.name"), EventType.ERROR);
            }
        }
    }

    public static void registerEvents(String name)
    {
        synchronized (_nevent)
        {
            _nevent.addNEventListners(new NEventStart() {

                @Override
                public Object hanleEvent(Object... obj) throws SocketException, Exception {
                    return _events.add((EventViewerEvent)obj[0]);
                }
            }, null);
        }
    }
    public static void unregisterEvents(String name)
    {
        synchronized (_nevent)
        {
            _nevent.removeNEventListners(null);
        }
    }


    public void OnLogEntryWritten(EventViewerEvent event)
    {
            synchronized(_nevent)
            {
                _events.add(event);
            }
    }

    public synchronized static void LogEvent(String msg, EventType type)
    {
        LogEvent("TayzGrid", msg, type, (short) -1, -1);
    }
    
   
    

    public synchronized static void LogEvent(String source, String msg, EventType type, short category, int eventid)
    {
         if (_eventlogger == null)
        {
            return;
        }
        EventLogger.setLastLoggedEvent(msg, type.toString());
        _eventlogger.LogEvent(msg, type);

        EventViewerEvent event = new EventViewerEvent();
        event.setInstanceId(eventid);
        event.setEventType(type);
        event.setMessage(msg);
        event.setSource(source);
        try
        {
            event.setGeneratedTime(new NCDateTime(new Date()));
        }
        catch(Exception ex)
                {
                    LogEvent("TayzGrid", ex.getMessage(), EventType.ERROR, (short) -1, -1);
                }

        _events.add(event);
    }

    @Override
    public Object hanleEvent(Object... obj) throws SocketException, Exception {
        return _events.add((EventViewerEvent)obj[0]);
    }

    public static synchronized EventViewerEvent[] getEventList()
    {
        EventViewerEvent[] eventList = new EventViewerEvent[_events.size()];
        return _events.toArray(eventList);
    }

    public static synchronized void clearEvents()
    {
        _events.clear();
    }

}
