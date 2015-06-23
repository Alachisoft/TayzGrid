/*
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

package com.alachisoft.tayzgrid.cluster.protocols;

import com.alachisoft.tayzgrid.cluster.util.Util;
import com.alachisoft.tayzgrid.cluster.stack.Protocol;
import com.alachisoft.tayzgrid.cluster.stack.ProtocolStackType;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// $Id: QUEUE.java,v 1.6 2004/07/23 02:28:01 belaban Exp $

/**
 * Queuing layer. Upon reception of event START_QUEUEING, all events traveling through this layer upwards/downwards (depending on direction of event) will be queued. Upon reception
 * of a STOP_QUEUEING event, all events will be released. Finally, the queueing flag is reset. When queueing, only event STOP_QUEUEING (received up or downwards) will be allowed to
 * release queueing.
 *
 * <author> Bela Ban </author>
 */
public class QUEUE extends Protocol
{

    public QUEUE()
    {
    }

    public java.util.List getUpVector()
    {
        return up_vec;
    }

    public java.util.List getDownVector()
    {
        return dn_vec;
    }

    public boolean getQueueingUp()
    {
        return queueing_up;
    }

    public boolean getQueueingDown()
    {
        return queueing_dn;
    }

    /**
     * All protocol names have to be unique !
     */
    @Override
    public String getName()
    {
        return "QUEUE";
    }
    public java.util.List up_vec = Collections.synchronizedList(new java.util.ArrayList(10));
    public java.util.List dn_vec = Collections.synchronizedList(new java.util.ArrayList(10));
    public boolean queueing_up = false, queueing_dn = false;
    private ReentrantReadWriteLock queingLock = new ReentrantReadWriteLock();
    private Lock readLock = queingLock.readLock();
    private Lock writeLock = queingLock.writeLock();


    @Override
    public java.util.List providedUpServices()
    {
        java.util.List ret = Collections.synchronizedList(new java.util.ArrayList(10));
        ret.add((int) Event.START_QUEUEING);
        ret.add((int) Event.STOP_QUEUEING);
        return ret;
    }

    @Override
    public java.util.List providedDownServices()
    {
        java.util.List ret = Collections.synchronizedList(new java.util.ArrayList(10));
        ret.add((int) Event.START_QUEUEING);
        ret.add((int) Event.STOP_QUEUEING);
        return ret;
    }

    @Override
    public boolean setProperties(java.util.HashMap props)
    {
        if (stack.getStackType() == ProtocolStackType.TCP)
        {
            this.up_thread = false;
            this.down_thread = false;
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info(getName() + ".setProperties", "part of TCP stack");
            }
        }
        return true;
    }

    /**
     * Queues or passes up events. No queue sync. necessary, as this method is never called concurrently.
     */
    @Override
    public void up(Event evt)
    {
        switch (evt.getType())
        {
            case Event.MSG:
                Message msg = (Message) evt.getArg();
                Object obj = msg.getHeader(HeaderType.GMS);
                if (obj != null && obj instanceof com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS.HDR)
                {
                    com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS.HDR hdr = (com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS.HDR) obj;
                    if (hdr.type == com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS.HDR.VIEW || hdr.type == com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS.HDR.JOIN_RSP)
                    {
                       writeLock.lock();
                        try
                        {
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("Queue.Up()", "Received VIEW event, so we start up_queuing");
                            }
                            queueing_up = true; // starts up queuing
                        }
                        finally
                        {
                           writeLock.unlock();
                        }
                    }
                    if (getStack().getCacheLog().getIsInfoEnabled())
                    {
                        getStack().getCacheLog().Info("Queue.up()", "Message Headers = " + Global.CollectionToString(msg.getHeaders()));
                    }
                    passUp(evt);
                    return;
                }

               readLock.lock();
                try
                {
                    if (queueing_up)
                    {
                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("queued up event " + evt.toString());
                        }
                        up_vec.add(evt);
                        return;
                    }
                }
                finally
                {
                   readLock.unlock();
                }

                break;


        }

        passUp(evt); // Pass up to the layer above us
    }

    private void deliverUpQueuedEvts(Event evt)
    {

        Event e;

        if (getStack().getCacheLog().getIsInfoEnabled())
        {
            getStack().getCacheLog().Info("QUEUE.deliverUpQueuedEvts","replaying up events");
        }

        writeLock.lock();
        try
        {
            for (int i = 0; i < up_vec.size(); i++)
            {
                e = (Event) up_vec.get(i);
                passUp(e);
            }
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("Queue.deliverUpQueuedEvts()", "delivered up queued msg count = " + up_vec.size());
            }
            up_vec.clear();
            queueing_up = false;
        }
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public void down(Event evt)
    {
       

        switch (evt.getType())
        {
            case Event.VIEW_CHANGE_OK:
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("Queue.down()", "VIEW_CHANGE : lets stop queuing");
                }
                deliverUpQueuedEvts(evt);
                break;

        }

        if (queueing_dn)
        {
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info("queued down event: " + Util.printEvent(evt));
            }
            dn_vec.add(evt);
            return;
        }

        passDown(evt); // Pass up to the layer below us
    }
}
