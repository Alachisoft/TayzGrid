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
package com.alachisoft.tayzgrid.common.event;

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.threading.ThreadPool;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Attempts to Replaces the Event type of .Net
 */
public final class NEvent
{

    private Map<NEventStart, NEventEnd> _eventListners = Collections.synchronizedMap(new HashMap<NEventStart, NEventEnd>());
    private List<NEventStart> _eventListnersSync = Collections.synchronizedList(new ArrayList<NEventStart>());
    private int _count;
    private ILogger _ncacheLogger;
    private String _name = "";
    private boolean _isSync;

    //<editor-fold defaultstate="collapsed" desc="Gets">
    /**
     * Updates the count of the number of subscribers to this event. When providing both the parameters for {@link NEvent#addNEventListners(Alachisoft.NCache.Common.Event.NEventStart, Alachisoft.NCache.Common.Event.NEventEnd)
     * } the count is incremented by 1
     *
     * @return returns the number of subscribers
     */
    public int getCount()
    {
        return _count;
    }

    public String getName()
    {
        return _name;
    }

    public boolean getIsSync()
    {
        return _isSync;
    }
    //</editor-fold>

    /**
     * {@link NEventStart} is the interface that is called first when an event is fired, {@link NEventEnd} is fired when {@link NEventStart} has completed its operation. If an of
     * the Interface is a keepAlive task, it will be executed as a separate thread.
     *
     * @param _ncacheLogger If Any Interface throws an exception , it will be logged in this logger.
     */
    public NEvent(String name, ILogger _ncacheLogger)
    {
        _name = name;
        this._ncacheLogger = _ncacheLogger;
    }

    /**
     * Places the NEvents in an HashMap to be executed when {@link NEvent#fireEvents(java.lang.Object[]) } will be executed
     *
     * @param start First called interface
     * @param end When {@link NEventStart} is finished, this interface will be called signaling the end of the event
     * @throws IllegalArgumentException Throws when Start is null, {@link NEventEnd} can be null
     */
    public synchronized void addNEventListners(NEventStart start, NEventEnd end) throws IllegalArgumentException
    {
        if (start == null)
        {
            throw new IllegalArgumentException("Event Function 'start' cannot be null");
        }

        _eventListners.put(start, end);
        _eventListnersSync.add(start);
        _count++;
    }

    /**
     * Removes the NEvents from execution HashMap
     *
     * @param start First called interface
     * @param end When {@link NEventStart} is finished, this interface will be called signaling the end of the event
     * @throws IllegalArgumentException Throws when Start is null, {@link NEventEnd} can be null
     */
    public synchronized void removeNEventListners(NEventStart start) throws IllegalArgumentException
    {
        if (start == null)
        {
            throw new IllegalArgumentException("Event Function 'start' cannot be null");
        }

        _eventListners.remove(start);
        _eventListnersSync.remove(start);
        _count--;
    }

    /**
     * Clears all the Listners
     */
    public synchronized void unsubscribeAllListners()
    {
        _eventListners.clear();
        _eventListnersSync.clear();
        _count = 0;
    }

    @Deprecated
    public synchronized void addNEventListnersSync(NEventStart start, NEventEnd end) throws IllegalArgumentException
    {
        this.addNEventListners(start, end);
    }

    @Deprecated
    public synchronized void removeNEventListnersSync(NEventStart start) throws IllegalArgumentException
    {

        //remove last subscribed event
        if(start == null && this._eventListnersSync.size() > 0)
        {
            start = this._eventListnersSync.get(this._eventListnersSync.size() - 1);
        }
        this.removeNEventListners(start);
    }

    @Deprecated
    public synchronized void unsubscribeAllSyncListners()
    {
        this.unsubscribeAllListners();
    }

    /**
     * Start a separate thread for every event subscribed
     *
     * @param removeSubscriberOnException inCase of SocketException, the subscriber will be removed
     * @param obj arguments provided to {@link NEventStart} and {@link NEventEnd}
     */
    public synchronized void fireEvents(boolean removeSubscriberOnException, Object... obj)
    {
        EventFire[] eventFire = new EventFire[_count];
        Iterator ide = _eventListners.entrySet().iterator();
        for (int i = 0; i < _count; i++)
        {
            Map.Entry<NEventStart, NEventEnd> pair = (Map.Entry<NEventStart, NEventEnd>) ide.next();
            eventFire[i] = new EventFire(pair.getKey(), pair.getValue(), this._ncacheLogger, this, obj);
            eventFire[i].setRemoveSubscriberOnException(removeSubscriberOnException);
            ThreadPool.executeTask(eventFire[i]);
        }
    }

    public synchronized Object fireEventsSynchronous(boolean removeSubscriberOnException, Object... obj)
    {
        Object val = null;
        Iterator ide = _eventListnersSync.iterator();
        while(ide.hasNext())
        {
            NEventStart start = (NEventStart) ide.next();
            try
            {
                  val = start.hanleEvent(obj);
            }
            catch (SocketException e)
            {
                if (_ncacheLogger != null)
                {
                    _ncacheLogger.Error("FireEvent.run.caller: " + _name, "Event# " + Thread.currentThread().getName() + "with Exception: \t" + e.getMessage());
                }

                if (removeSubscriberOnException)
                {
                    this.removeNEventListnersSync(start);
                }
            }
            catch (Exception e)
            {
                if (_ncacheLogger != null)
                {
                    _ncacheLogger.Error("FireEvent.run.caller: " + _name, "Event# " + Thread.currentThread().getName() + "with Exception: \t" + e.getMessage());
                }
            }
        }
        return val;
    }

    /**
     * Returns a Deep copy of HashMap, Override copy function of the interface class if cloning is very important
     *
     * @return Subscribers list
     */
    public synchronized HashMap<NEventStart, NEventEnd> getInvocationList()
    {
        return new HashMap<NEventStart, NEventEnd>(_eventListners);
    }

    /**
     * Starts the Event interface provided as a new thread following the same principle of {@link NEvent#fireEvents(java.lang.Object[])}
     *
     * @param start First called interface
     * @param end When {@link NEventStart} is finished, this interface will be called signaling the end of the event
     * @param ncacheLogger If Any Interface throws an exception , it will be logged in this logger.
     * @param obj arguments provided to {@link NEventStart} and {@link NEventEnd}
     */
    public static void beginInvoke(NEventStart start, NEventEnd end, ILogger ncacheLogger, boolean removeSubscriberOnException, NEvent source, Object... obj)
    {
        EventFire even = new EventFire(start, end, ncacheLogger, source, obj);
        even.setRemoveSubscriberOnException(removeSubscriberOnException);
        Thread thread = new Thread(even, "EventFire beginInvoke");
        thread.start();
    }
}
