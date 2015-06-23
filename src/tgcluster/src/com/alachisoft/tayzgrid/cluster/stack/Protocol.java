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

package com.alachisoft.tayzgrid.cluster.stack;

import com.alachisoft.tayzgrid.cluster.ThreadClass;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.blocks.ExtSocketException;
import com.alachisoft.tayzgrid.common.datastructures.QueueClosedException;
import com.alachisoft.tayzgrid.common.ServicePropValues;

// $Id: Protocol.java,v 1.18 2004/07/05 14:17:33 belaban Exp $
/**
 * The Protocol class provides a set of common services for protocol layers. Each layer has to be a subclass of Protocol and override a number of methods (typically just
 * <code>up()</code>,
 * <code>Down</code> and
 * <code>getName</code>. Layers are stacked in a certain order to form a protocol stack. <a href=org.jgroups.Event.html>Events</a> are passed from lower layers to upper ones and
 * vice versa. E.g. a Message received by the UDP layer at the bottom will be passed to its higher layer as an Event. That layer will in turn pass the Event to its layer and so on,
 * until a layer handles the Message and sends a response or discards it, the former resulting in another Event being passed down the stack.<p> Each layer has 2 FIFO queues, one
 * for up Events and one for down Events. When an Event is received by a layer (calling the internal upcall
 * <code>ReceiveUpEvent</code>), it is placed in the up-queue where it will be retrieved by the up-handler thread which will invoke method
 * <code>Up</code> of the layer. The same applies for Events traveling down the stack. Handling of the up-handler and down-handler threads and the 2 FIFO queues is donw by the
 * Protocol class, subclasses will almost never have to override this behavior.<p> The important thing to bear in mind is that Events have to passed on between layers in FIFO order
 * which is guaranteed by the Protocol implementation and must be guranteed by subclasses implementing their on Event queuing.<p> <b>Note that each class implementing interface
 * Protocol MUST provide an empty, public constructor !</b>
 */
public abstract class Protocol
{

    public static class UpHandler extends ThreadClass
    {

        private com.alachisoft.tayzgrid.common.datastructures.Queue mq;
        private Protocol handler;
        private int id;

        public UpHandler(com.alachisoft.tayzgrid.common.datastructures.Queue mq, Protocol handler)
        {
            this.mq = mq;
            this.handler = handler;
            if (handler != null)
            {
                setName("UpHandler (" + handler.getName() + ')');
            }
            else
            {
                setName("UpHandler");
            }
            setIsBackground(true);
        }

        public UpHandler(com.alachisoft.tayzgrid.common.datastructures.Queue mq, Protocol handler, String name, int id)
        {
            this.mq = mq;
            this.handler = handler;
            if (name != null)
            {
                setName(name);
            }
            setIsBackground(true);
            this.id = id;
        }

        /**
         * Removes events from mq and calls handler.up(evt)
         */
        @Override
        public void run()
        {
            if (handler.getStack().getCacheLog().getIsInfoEnabled())
            {
                handler.getStack().getCacheLog().Info(getName(), "---> Started!");
            }
 
            while (!mq.getClosed())
            {
                try
                {
                    Event evt = (Event) mq.remove();
                    if (evt == null)
                    {
                        handler.getStack().getCacheLog().Warn("Protocol", "removed null event");
                        continue;
                    }
                    if (handler.enableMonitoring)
                    {
                        handler.PublishUpQueueStats(mq.getCount(), id);
                    }
 
                    handler.up(evt);
 
                }
                catch (QueueClosedException e)
                {
                    handler.getStack().getCacheLog().Error(getName(), e.toString());
                    break;
                }
 
                catch (Exception e)
                {
                    handler.getStack().getCacheLog().Error(getName(), " exception: " + e.toString());
                }
            }
 
            if (handler.getStack().getCacheLog().getIsInfoEnabled())
            {
                handler.getStack().getCacheLog().Info(getName() + "    ---> Stopped!");
            }
        }
    }

    public static class DownHandler extends ThreadClass
    {

        private com.alachisoft.tayzgrid.common.datastructures.Queue mq;
        private Protocol handler;
        private int id;

        public DownHandler(com.alachisoft.tayzgrid.common.datastructures.Queue mq, Protocol handler)
        {
            this.mq = mq;
            this.handler = handler;
            String name = null;
            if (handler != null)
            {
                setName("DownHandler (" + handler.getName() + ')');
            }
            else
            {
                setName("DownHandler");
            }

            setIsBackground(true);
        }

        public DownHandler(com.alachisoft.tayzgrid.common.datastructures.Queue mq, Protocol handler, String name, int id)
        {
            this.mq = mq;
            this.handler = handler;
            setName(name);
            setIsBackground(true);
            this.id = id;
        }

        /**
         * Removes events from mq and calls handler.down(evt)
         */
        @Override
        public void run()
        {
            try
            {
                while (!mq.getClosed())
                {
                    try
                    {
                        Event evt = (Event) mq.remove();
                        if (evt == null)
                        {
                            handler.getStack().getCacheLog().Warn("Protocol", "removed null event");
                            continue;
                        }

                        int type = evt.getType();
                        if (type == Event.ACK || type == Event.START || type == Event.STOP)
                        {
                            if (handler.handleSpecialDownEvent(evt) == false)
                            {
                                continue;
                            }
                        }

                        if (handler.enableMonitoring)
                        {
                            handler.PublishDownQueueStats(mq.getCount(), id);
                        }
                        handler.down(evt);
                    }
                    catch (QueueClosedException e)
                    {
                        handler.getStack().getCacheLog().Error(getName(), e.toString());
                        break;
                    }
 
                    catch (Exception e)
                    {
                        handler.getStack().getCacheLog().Warn(getName(), " exception is " + e.toString());
                    }
                }
            }
            catch (Exception e)
            {
                handler.getStack().getCacheLog().Error("DownHandler.Run():3", "exception=" + e.toString());
            }
        }
    }

    public abstract String getName();

    public final com.alachisoft.tayzgrid.common.datastructures.Queue getUpQueue()
    {
        return up_queue;
    }

    public final com.alachisoft.tayzgrid.common.datastructures.Queue getDownQueue()
    {
        return down_queue;
    }

    public final ProtocolStack getStack()
    {
        return this.stack;
    }

    public final void setStack(ProtocolStack value)
    {
        this.stack = value;
    }

    public final Protocol getUpProtocol()
    {
        return up_prot;
    }

    public final void setUpProtocol(Protocol value)
    {
        this.up_prot = value;
    }

    public final Protocol getDownProtocol()
    {
        return down_prot;
    }

    public final void setDownProtocol(Protocol value)
    {
        this.down_prot = value;
    }
    protected long THREAD_JOIN_TIMEOUT = 1000;
    protected java.util.HashMap props = new java.util.HashMap();
    protected Protocol up_prot, down_prot;
    protected ProtocolStack stack;
    protected com.alachisoft.tayzgrid.common.datastructures.Queue up_queue, down_queue;
    protected int up_thread_prio = - 1;
    protected int down_thread_prio = - 1;
    protected boolean down_thread = false; // determines whether the down_handler thread should be started
    protected boolean up_thread = true; // determines whether the up_handler thread should be started
    protected UpHandler up_handler;
    protected DownHandler down_handler;
    protected boolean _printMsgHdrs = false;
    public boolean enableMonitoring;
    protected boolean useAvgStats = false;

    /**
     * Configures the protocol initially. A configuration string consists of name=value items, separated by a ';' (semicolon), e.g.:
     * <pre>
     * "loopback=false;unicast_inport=4444"
     * </pre>
     */
    public boolean setProperties(java.util.HashMap props) throws Exception
    {
        if (props != null)
        {
            this.props = (java.util.HashMap) props.clone();
        }
        return true;
    }

    /**
     * Called by Configurator. Removes 2 properties which are used by the Protocol directly and then calls setProperties(), which might invoke the setProperties() method of the
     * actual protocol instance.
     */
    public boolean setPropertiesInternal(java.util.HashMap props) throws Exception
    {
        this.props = (java.util.HashMap) props.clone();

        if (props.containsKey("down_thread"))
        {
            down_thread = Boolean.parseBoolean((String)props.get("down_thread"));
            props.remove("down_thread");
        }
        if (props.containsKey("down_thread_prio"))
        {
            down_thread_prio = Integer.decode((String)props.get("down_thread_prio"));
            props.remove("down_thread_prio");
        }
        if (props.containsKey("up_thread"))
        {
            up_thread = Boolean.parseBoolean((String)props.get("up_thread"));
            props.remove("up_thread");
        }
        if (props.containsKey("up_thread_prio"))
        {
            up_thread_prio = Integer.decode((String)props.get("up_thread_prio"));
            props.remove("up_thread_prio");
        }

        if (ServicePropValues.CacheServer_EnableDebuggingCounters != null)
        {
            enableMonitoring = Boolean.parseBoolean((ServicePropValues.CacheServer_EnableDebuggingCounters));
        }

        if (ServicePropValues.useAvgStats != null)
        {
            useAvgStats = Boolean.parseBoolean(((ServicePropValues.useAvgStats)));
        }

        return setProperties(props);
    }

    public java.util.HashMap getProperties()
    {
        return props;
    }
 

    public void PublishUpQueueStats(long count, int queueId)
    {
    }

    public void PublishDownQueueStats(long count, int queueId)
    {
    }
 

    /**
     * Called after instance has been created (null constructor) and before protocol is started. Properties are already set. Other protocols are not yet connected and events cannot
     * yet be sent.
     *
     * @exception Exception Thrown if protocol cannot be initialized successfully. This will cause the ProtocolStack to fail, so the channel constructor will throw an exception
     *
     */
    public void init() throws Exception
    {
    }

    /**
     * This method is called on a {@link org.jgroups.Channel#connect(String)}. Starts work. Protocols are connected and queues are ready to receive events. Will be called <em>from
     * bottom to top</em>. This call will replace the <b>START</b> and <b>START_OK</b> events.
     *
     * @throws ExtSocketException  Exception Thrown if protocol cannot be started successfully. This will cause the ProtocolStack to fail, so {@link org.jgroups.Channel#connect(String)} will throw
     * an exception
     *
     */
    public void start() throws ExtSocketException
    {
    }

    /**
     * This method is called on a {@link org.jgroups.Channel#disconnect()}. Stops work (e.g. by closing multicast socket). Will be called <em>from top to bottom</em>. This means
     * that at the time of the method invocation the neighbor protocol below is still working. This method will replace the <b>STOP</b>, <b>STOP_OK</b>, <b>CLEANUP</b> and
     * <b>CLEANUP_OK</b> events. The ProtocolStack guarantees that when this method is called all messages in the down queue will have been flushed
     */
    public void stop()
    {
    }

    /**
     * This method is called on a {@link org.jgroups.Channel#close()}. Does some cleanup; after the call the VM will terminate
     */
    public void destroy()
    {
    }

    /**
     * List of events that are required to be answered by some layer above.
     *
     * @return Vector (of Integers)
     *
     */
    public java.util.List requiredUpServices()
    {
        return null;
    }

    /**
     * List of events that are required to be answered by some layer below.
     *
     * @return Vector (of Integers)
     *
     */
    public java.util.List requiredDownServices()
    {
        return null;
    }

    /**
     * List of events that are provided to layers above (they will be handled when sent down from above).
     *
     * @return Vector (of Integers)
     *
     */
    public java.util.List providedUpServices()
    {
        return null;
    }

    /**
     * List of events that are provided to layers below (they will be handled when sent down from below).
     *
     * @return Vector (of Integers)
     *
     */
    public java.util.List providedDownServices()
    {
        return null;
    }

    /**
     * Used internally. If overridden, call this method first. Only creates the up_handler thread if down_thread is true
     */
    public void startUpHandler()
    {
        if (up_thread)
        {
            if (up_handler == null)
            {
                up_queue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                up_handler = new UpHandler(up_queue, this);
                if (up_thread_prio >= 0)
                {
                    try
                    {
                         
                    }
                    catch (Exception t)
                    {
                        stack.getCacheLog().Error("Protocol", "priority " + up_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                up_handler.Start();
            }
        }
    }

    /**
     * Used internally. If overridden, call this method first. Only creates the down_handler thread if down_thread is true
     */
    public void startDownHandler()
    {
        if (down_thread)
        {
            if (down_handler == null)
            {
                down_queue = new com.alachisoft.tayzgrid.common.datastructures.Queue();
                down_handler = new DownHandler(down_queue, this);
                if (down_thread_prio >= 0)
                {
                    try
                    {
                         
                    }
                    catch (Exception t)
                    {
                        stack.getCacheLog().Error("Protocol.startDownHandler()", "priority " + down_thread_prio + " could not be set for thread: " + t.getStackTrace());
                    }
                }
                down_handler.Start();
            }
        }
    }

    /**
     * Used internally. If overridden, call parent's method first
     */
    public void stopInternal()
    {
        if (up_queue != null)
        {
            up_queue.close(false); // this should terminate up_handler thread
        }

        if (up_handler != null && up_handler.getIsAlive())
        {
            try
            {
                up_handler.Join(THREAD_JOIN_TIMEOUT);
            }
            catch (Exception e)
            {
                stack.getCacheLog().Error("Protocol.stopInternal()", "up_handler.Join " + e.getMessage());
            }
            if (up_handler != null && up_handler.getIsAlive())
            {
                up_handler.Interrupt(); // still alive ? let's just kill it without mercy...
                try
                {
                    up_handler.Join(THREAD_JOIN_TIMEOUT);
                }
                catch (Exception e)
                {
                    stack.getCacheLog().Error("Protocol.stopInternal()", "up_handler.Join " + e.getMessage());
                }
                if (up_handler != null && up_handler.getIsAlive())
                {
                    stack.getCacheLog().Error("Protocol", "up_handler thread for " + getName() + " was interrupted (in order to be terminated), but is still alive");
                }
            }
        }
        up_handler = null;

        if (down_queue != null)
        {
            down_queue.close(false); // this should terminate down_handler thread
        }
        if (down_handler != null && down_handler.getIsAlive())
        {
            try
            {
                down_handler.Join(THREAD_JOIN_TIMEOUT);
            }
            catch (Exception e)
            {
                stack.getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
            }
            if (down_handler != null && down_handler.getIsAlive())
            {
                down_handler.Interrupt(); // still alive ? let's just kill it without mercy...
                try
                {
                    down_handler.Join(THREAD_JOIN_TIMEOUT);
                }
                catch (Exception e)
                {
                    stack.getCacheLog().Error("Protocol.stopInternal()", "down_handler.Join " + e.getMessage());
                }
                if (down_handler != null && down_handler.getIsAlive())
                {
                    stack.getCacheLog().Error("Protocol", "down_handler thread for " + getName() + " was interrupted (in order to be terminated), but is is still alive");
                }
            }
        }
        down_handler = null;
    }

    /**
     * Internal method, should not be called by clients. Used by ProtocolStack. I would have used the 'friends' modifier, but this is available only in C++ ... If the up_handler
     * thread is not available (down_thread == false), then directly call the up() method: we will run on the caller's thread (e.g. the protocol layer below us).
     */
    public void receiveUpEvent(Event evt)
    {
        int type = evt.getType();

        if (_printMsgHdrs && type == Event.MSG)
        {
            printMsgHeaders(evt, "up()");
        }
 
        if (up_handler == null)
        {
            up(evt);
            return;
        }
        try
        {
 
 
            if (stack.getCacheLog().getIsInfoEnabled())
            {
                stack.getCacheLog().Info(getName() + ".receiveUpEvent()", "RentId :" + evt.getRentId() + "up queue count : " + up_queue.getCount());
            }
            up_queue.add(evt, evt.getPriority());
        }
        catch (Exception e)
        {
            stack.getCacheLog().Warn("Protocol.receiveUpEvent()", e.toString());
        }
    }

    /**
     * Prints the header of a message. Used for debugging purpose.
     *
     * @param evt
     */
    protected final void printMsgHeaders(Event evt, String extra)
    {
        Message m = (Message) evt.getArg();
        try
        {
            if (m != null)
            {
                if (stack.getCacheLog().getIsInfoEnabled())
                {
                    stack.getCacheLog().Info(this.getName() + "." + extra + ".printMsgHeaders()", Global.CollectionToString(m.getHeaders()));
                }
            }
        }
        catch (Exception e)
        {
            stack.getCacheLog().Error(this.getName() + ".printMsgHeaders()", e.toString());
        }
    }

    /**
     * Internal method, should not be called by clients. Used by ProtocolStack. I would have used the 'friends' modifier, but this is available only in C++ ... If the down_handler
     * thread is not available (down_thread == false), then directly call the down() method: we will run on the caller's thread (e.g. the protocol layer above us).
     */
    public void receiveDownEvent(Event evt)
    {
        int type = evt.getType();
 
        if (down_handler == null)
        {
            if (type == Event.ACK || type == Event.START || type == Event.STOP)
            {
                if (handleSpecialDownEvent(evt) == false)
                {
                    return;
                }
            }
            if (_printMsgHdrs && type == Event.MSG)
            {
                printMsgHeaders(evt, "down()");
            }
            down(evt);
            return;
        }
        try
        {
            if (type == Event.STOP || type == Event.VIEW_BCAST_MSG)
            {
                if (handleSpecialDownEvent(evt) == false)
                {
                    return;
                }
                if (down_prot != null)
                {
                    down_prot.receiveDownEvent(evt);
                }
                return;
            }
            down_queue.add(evt, evt.getPriority());
        }
        catch (Exception e)
        {
            stack.getCacheLog().Warn("Protocol.receiveDownEvent():2", e.toString());
        }
    }

    /**
     * Causes the event to be forwarded to the next layer up in the hierarchy. Typically called by the implementation of
     * <code>Up</code> (when done).
     */
    public void passUp(Event evt)
    {
        if (up_prot != null)
        {
      
            up_prot.receiveUpEvent(evt);
        }
        else
        {
            stack.getCacheLog().Error("Protocol", "no upper layer available");
        }
    }

    /**
     * Causes the event to be forwarded to the next layer down in the hierarchy.Typically called by the implementation of
     * <code>Down</code> (when done).
     */
    public void passDown(Event evt)
    {
        if (down_prot != null)
        {
 
            down_prot.receiveDownEvent(evt);
        }
        else
        {
            stack.getCacheLog().Error("Protocol", "no lower layer available");
        }
    }

    /**
     * An event was received from the layer below. Usually the current layer will want to examine the event type and - depending on its type - perform some computation (e.g.
     * removing headers from a MSG event type, or updating the internal membership list when receiving a VIEW_CHANGE event). Finally the event is either a) discarded, or b) an
     * event is sent down the stack using
     * <code>passDown()</code> or c) the event (or another event) is sent up the stack using
     * <code>passUp()</code>.
     */
    public void up(Event evt)
    {
        passUp(evt);
    }

    /**
     * An event is to be sent down the stack. The layer may want to examine its type and perform some action on it, depending on the event's type. If the event is a message MSG,
     * then the layer may need to add a header to it (or do nothing at all) before sending it down the stack using
     * <code>passDown()</code>. In case of a GET_ADDRESS event (which tries to retrieve the stack's address from one of the bottom layers), the layer may need to send a new
     * response event back up the stack using
     * <code>passUp()</code>.
     */
    public void down(Event evt)
    {
        passDown(evt);
    }

    /**
     * These are special internal events that should not be handled by protocols
     *
     * @return boolean True: the event should be passed further down the stack. False: the event should be discarded (not passed down the stack)
     *
     */
    protected boolean handleSpecialDownEvent(Event evt)
    {
        switch (evt.getType())
        {
            case Event.ACK:
                if (down_prot == null)
                {
                    passUp(new Event(Event.ACK_OK));
                    return false; // don't pass down the stack
                }
            
                Event ev = new Event(Event.START);
                return this.handleSpecialDownEvent(ev);

            case Event.START:
                try
                {
                    start();

                    // if we're the transport protocol, reply with a START_OK up the stack
                    if (down_prot == null)
                    {
                        passUp(new Event(Event.START_OK, (Object) true));
                        return false; // don't pass down the stack
                    }
                    return true; // pass down the stack
                }
                catch (Exception e)
                {
                    stack.getCacheLog().Error("Protocol.handleSpecialDownEvent", e.toString());
                    passUp(new Event(Event.START_OK, new Exception(e.getMessage(), e)));
                }
                return false;

            case Event.STOP:
                try
                {
                    stop();
                }
                catch (Exception e)
                {
                    stack.getCacheLog().Error("Protocol.handleSpecialDownEvent()", e.toString());
                }
                if (down_prot == null)
                {
                    passUp(new Event(Event.STOP_OK, (Object) true));
                    return false; // don't pass down the stack
                }
                return true; // pass down the stack

            default:
                return true; // pass down by default

        }
    }
}
