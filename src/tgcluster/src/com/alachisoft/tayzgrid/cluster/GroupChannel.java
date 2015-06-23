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

package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.cluster.stack.ProtocolStack;
import com.alachisoft.tayzgrid.common.datastructures.Queue;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// $Id: GroupChannel.java,v 1.25 2004/08/29 19:35:03 belaban Exp $
//using StateTransferInfo = Alachisoft.NCache.Cluster.Stack.StateTransferInfo;
/**
 * GroupChannel is a pure Java implementation of Channel When a GroupChannel object is instantiated it automatically sets up the protocol stack
 *
 * <author> Bela Ban </author> <author> Filip Hanik </author> <version> $Revision: 1.25 $ </version>
 */
public class GroupChannel extends Channel
{

    public String FORCE_PROPS = "force.properties";

    /*
     * the protocol stack configuration string
     */
    private String props;

    /*
     * the address of this GroupChannel instance
     */
    private Address local_addr = null;

    /*
     * the channel (also know as group) name
     */
    private String channel_name = null; // group id
    private String subGroup_name = null; // subgroup id

    /*
     * the latest view of the group membership
     */
    private View my_view = null;
    /*
     * the queue that is used to receive messages (events) from the protocol stack
     */
    private Queue mq = new Queue();
    /*
     * the protocol stack, used to send and receive messages from the protocol stack
     */
    private ProtocolStack prot_stack = null;
    /**
     * Thread responsible for closing a channel and potentially reconnecting to it (e.g. when shunned)
     */
    public CloserThread closer = null;

    /*
     * lock objects
     */
    private final Object local_addr_mutex = new Object();
    private final Object connect_mutex = new Object();
    private boolean connect_ok_event_received = false;
    private final Object connect_mutex_phase2 = new Object();
    private boolean connect_ok_event_received_phase2 = false;
    private final Object disconnect_mutex = new Object();
    private boolean disconnect_ok_event_received = false;
    private final Object flow_control_mutex = new Object();
    /**
     * wait until we have a non-null local_addr
     */
    private long LOCAL_ADDR_TIMEOUT = 30000; //=Long.parseLong(System.getProperty("local_addr.timeout", "30000"));
	/*
     * flag to indicate whether to receive views from the protocol stack
     */
    private boolean receive_views = true;
    /*
     * flag to indicate whether to receive suspect messages
     */
    private boolean receive_suspects = true;
    /*
     * flag to indicate whether to receive blocks, if this is set to true, receive_views is set to true
     */
    private boolean receive_blocks = false;
    /*
     * flag to indicate whether to receive local messages if this is set to false, the GroupChannel will not receive messages sent by itself
     */
    private boolean receive_local_msgs = true;
    /*
     * flag to indicate whether the channel will reconnect (reopen) when the exit message is received
     */
    private boolean auto_reconnect = false;
    /*
     * channel connected flag
     */
    private boolean connected = false;
    private boolean block_sending = false; // block send()/down() if true (unlocked by UNBLOCK_SEND event)
	/*
     * channel closed flag
     */
    private boolean closed = false; // close() has been called, channel is unusable
    //private NewTrace _nTrace = null;
    private boolean _isStartedAsMirror = false;
    ///// <summary> Trace object.</summary>
    
    
    private ILogger _ncacheLog;
    
   

    public final ILogger getCacheLog()
    {
        return _ncacheLog;
    }

    public final void setNCacheLog(ILogger value)
    {
        _ncacheLog = value;

        if (prot_stack != null)
        {
            prot_stack.setNCacheLog(value);
        }
    }

    /**
     * Used to maintain additional data across channel disconnects/reconnects. This is a kludge and will be remove as soon as JGroups supports logical addresses
     */

    private byte[] additional_data = null;

    static
    {
        try
        {
            Global.registerCompactType();
        }
        catch (CacheArgumentException nCacheArgumentException)
        {
         
        }
    }

    
    public final ProtocolStack getStack()
    {
        return prot_stack;
    }

    /**
     * Constructs a
     * <code>GroupChannel</code> instance with the protocol stack configuration based upon the specified properties parameter.
     *
     *
     * @param properties an old style property string, a string representing a system resource containing a JGroups XML configuration, a string representing a URL pointing to a
     * JGroups XML XML configuration, or a string representing a file name that contains a JGroups XML configuration.
     *
     *
     * <throws> ChannelException if problems occur during the configuration and </throws> initialization of the protocol stack.
     * @param NCacheLog
     * @throws ChannelException
     */
    public GroupChannel(String properties, ILogger NCacheLog) throws ChannelException
    {
        props = properties;
        this._ncacheLog = NCacheLog;

        /*
         * create the new protocol stack
         */
        prot_stack = new ProtocolStack(this, props);
        prot_stack.setNCacheLog(NCacheLog);

        /*
         * Setup protocol stack (create layers, queues between them
         */
        try
        {
            prot_stack.setup();
        }
        catch (Exception e)
        {
            NCacheLog.Error("GroupChannel.GroupChannel()", e.toString());
            throw new ChannelException("GroupChannel(): " + e);
        }
    }


    public final void InitializePerformanceCounter(String instanceName, Monitor monitor)
    {
        if (prot_stack != null)
        {
            prot_stack.InitializePerfCounters(instanceName, monitor);
        }
    }


    /**
     * @return the protocol stack. Currently used by Debugger. Specific to GroupChannel, therefore not visible in Channel
     */
    public final ProtocolStack getProtocolStack()
    {
        return prot_stack;
    }

   
    /**
     * @return the protocol stack configuration in string format. an example of this property is<BR>
     * "UDP:PING:FD:STABLE:NAKACK:UNICAST:FRAG:FLUSH:GMS:VIEW_ENFORCER:STATE_TRANSFER:QUEUE"
     */
    public final String getProperties()
    {
        return props;
    }

    /**
     * @return true if the Open operation has been called successfully
     */
    @Override
    public boolean getIsOpen()
    {
        return !closed;
    }

    /**
     * returns true if the Connect operation has been called successfully
     *
     * @return
     */
    @Override
    public boolean getIsConnected()
    {
        return connected;
    }

    @Override
    public int getNumMessages()
    {
        return mq != null ? mq.getCount() : - 1;
    }

    /**
     * returns the current view.<BR> if the channel is not connected or if it is closed it will return null<BR>
     *
     * @return returns the current group view, or null if the channel is closed or disconnected
     *
     */
    @Override
    public View getView()
    {
        return closed || !connected ? null : my_view;
    }

    /**
     * returns the local address of the channel returns null if the channel is closed
     */
    @Override
    public Address getLocalAddress()
    {
        return closed ? null : local_addr;
    }

    /**
     * returns the name of the channel if the channel is not connected or if it is closed it will return null
     */
    @Override
    public String getChannelName()
    {
        return closed ? null : (!connected ? null : channel_name);
    }

    /**
     * @param include_properties
     * @return a pretty-printed form of all the protocols. If include_properties is set, the properties for each protocol will also be printed.
     */
    public String printProtocolSpec(boolean include_properties)
    {
        return prot_stack != null ? prot_stack.printProtocolSpec(include_properties) : null;
    }

    @Override
    public void connectPhase2() throws ChannelClosedException, ChannelException
    {
        synchronized (this)
        {
            /*
             * make sure the channel is not closed
             */
            checkClosed();

            // only connect if we are not a unicast channel
            if (channel_name != null)
            {

                /*
                 * Wait for notification that the channel has been connected to the group
                 */
                synchronized (connect_mutex_phase2)
                {
                    // wait for CONNECT_OK event
                    Event connect_event = new Event(Event.CONNECT_PHASE_2, _isStartedAsMirror);
                    connect_ok_event_received_phase2 = false; // added patch by Roland Kurman (see history.txt)
                    down(connect_event);

                    try
                    {
                        while (!connect_ok_event_received_phase2)
                        {
                            com.alachisoft.tayzgrid.common.threading.Monitor.wait(connect_mutex_phase2);//connect_mutex_phase2.wait();
                        }
                    }
                    catch (Exception e)
                    {
                        getCacheLog().Error("GroupChannel.connect():2", "exception=" + e);
                    }
                }
            }

        }
    }

    /**
     * Connects the channel to a group.<BR> If the channel is already connected, an error message will be printed to the error log<BR> If the channel is closed a ChannelClosed
     * exception will be thrown<BR> This method starts the protocol stack by calling ProtocolStack.start<BR> then it sends an Event.CONNECT event down the stack and waits to
     * receive a CONNECT_OK event<BR> Once the CONNECT_OK event arrives from the protocol stack, any channel listeners are notified<BR> and the channel is considered connected<BR>
     *
     *
     * @param channel_name A <code>String</code> denoting the group name. Cannot be null.
     *
     * @param subGroup_name
     * @param isStartedAsMirror
     * @param twoPhaseInitialization
     * @exception ChannelException The protocol stack cannot be started
     *
     */
    @Override
    public void connect(String channel_name, String subGroup_name, boolean isStartedAsMirror, boolean twoPhaseInitialization) throws ChannelException
    {
        synchronized (this)
        {
            /*
             * make sure the channel is not closed
             */
            checkClosed();

            _isStartedAsMirror = isStartedAsMirror;
            /*
             * if we already are connected, then ignore this
             */
            if (connected)
            {
                getCacheLog().Error("GroupChannel", "already connected to " + channel_name);
                return;
            }

            /*
             * make sure we have a valid channel name
             */
            if (channel_name == null)
            {
                getCacheLog().Error("GroupChannel", "channel_name is null, assuming unicast channel");
            }
            else
            {
                this.channel_name = channel_name;
            }

            //=============================================
            if (subGroup_name != null)
            {
                this.subGroup_name = subGroup_name;
            }
            //=============================================

            try
            {
                prot_stack.startStack(); // calls start() in all protocols, from top to bottom
            }
            catch (Exception e)
            {
                getCacheLog().Error("GroupChannel.connect()", "exception: " + e);

                throw new ChannelException(e.getMessage(), e);
            }

            /*
             * try to get LOCAL_ADDR_TIMEOUT. Catch SecurityException thrown if called in an untrusted environment (e.g. using JNLP)
             */
            LOCAL_ADDR_TIMEOUT = 30000;

            /*
             * Wait LOCAL_ADDR_TIMEOUT milliseconds for local_addr to have a non-null value (set by SET_LOCAL_ADDRESS)
             */


            synchronized (local_addr_mutex)
            {
                long wait_time = LOCAL_ADDR_TIMEOUT;

                //: Need to re verify the System.currentTimeMillis as it has a accuracy issue of 50 ms or 70ms
                long start = System.currentTimeMillis();
                //long start = (dat.getTicks(); - 621355968000000000L) / 10000L;
                while (local_addr == null && wait_time > 0)
                {
                    try
                    {
                         
                        com.alachisoft.tayzgrid.common.threading.Monitor.wait(local_addr_mutex,wait_time) ;//local_addr_mutex.wait(wait_time);
                    }
                    catch (InterruptedException e)
                    {
                        getCacheLog().Error("GroupChannel.connect():2", "exception=" + e);
                    }
                    
                    wait_time -= System.currentTimeMillis();
                }

                
                if (wait_time < 0)
                {
                    getCacheLog().Fatal("[Timeout]GroupChannel.connect:" + wait_time);
                }
            }

            // ProtocolStack.start() must have given us a valid local address; if not we won't be able to continue
            if (local_addr == null)
            {
                getCacheLog().Error("GroupChannel", "local_addr == null; cannot connect");
                throw new ChannelException("local_addr is null");
            }


            /*
             * create a temporary view, assume this channel is the only member and is the coordinator
             */

            //: java.util.ArrayList t = java.util.ArrayList.Synchronized(new java.util.ArrayList(1));
            List t = Collections.synchronizedList(new ArrayList(1));
            t.add(local_addr);
            my_view = new View(local_addr, 0, t); // create a dummy view

            // only connect if we are not a unicast channel
            if (channel_name != null)
            {

                /*
                 * Wait for notification that the channel has been connected to the group
                 */
                synchronized (connect_mutex)
                {
                    // wait for CONNECT_OK event
                    Event connect_event = new Event(Event.CONNECT, new Object[]
                            {
                                channel_name,
                                subGroup_name,
                                isStartedAsMirror,
                                twoPhaseInitialization
                            });
                    connect_ok_event_received = false; // added patch by Roland Kurman (see history.txt)
                    down(connect_event);

                    try
                    {
                        while (!connect_ok_event_received)
                        {
                            com.alachisoft.tayzgrid.common.threading.Monitor.wait(connect_mutex);// connect_mutex.wait();
                        }
                    }
                    catch (Exception e)
                    {
                        getCacheLog().Error("GroupChannel.connect():2", "exception=" + e);
                    }
                }
            }

            /*
             * notify any channel listeners
             */
            connected = true;
            if (channel_listener != null)
            {
                channel_listener.channelConnected(this);
            }
        }
    }

    /**
     * Disconnects the channel if it is connected. If the channel is closed, this operation is ignored<BR> Otherwise the following actions happen in the listed order<BR> <ol> <li>
     * The GroupChannel sends a DISCONNECT event down the protocol stack<BR> <li> Blocks until the channel to receives a DISCONNECT_OK event<BR> <li> Sends a STOP_QUEING event down
     * the stack<BR> <li> Stops the protocol stack by calling ProtocolStack.stop()<BR> <li> Notifies the listener, if the listener is available<BR> </ol>
     */
    @Override
    public void disconnect()
    {
        synchronized (this)
        {
            if (closed)
            {
                return;
            }

            if (connected)
            {

                if (channel_name != null)
                {

                    /*
                     * Send down a DISCONNECT event. The DISCONNECT event travels down to the GMS, where a DISCONNECT_OK response is generated and sent up the stack. GroupChannel
                     * blocks until a DISCONNECT_OK has been received, or until timeout has elapsed.
                     */
                    Event disconnect_event = new Event(Event.DISCONNECT, local_addr);

                    synchronized (disconnect_mutex)
                    {
                        try
                        {
                            disconnect_ok_event_received = false;
                            down(disconnect_event); // DISCONNECT is handled by each layer
                            while (!disconnect_ok_event_received)
                            {
                                com.alachisoft.tayzgrid.common.threading.Monitor.wait(disconnect_mutex);//disconnect_mutex.wait(); // wait for DISCONNECT_OK event
                            }
                        }
                        catch (Exception e)
                        {
                            getCacheLog().Error("GroupChannel.disconnect", e.toString());
                        }
                    }
                }

                // Just in case we use the QUEUE protocol and it is still blocked...
                down(new Event(Event.STOP_QUEUEING));

                connected = false;
                try
                {
                    prot_stack.stopStack(); // calls stop() in all protocols, from top to bottom
                    prot_stack.destroy();
                }
                catch (Exception e)
                {
                    getCacheLog().Error("GroupChannel.disconnect()", e.toString());
                }

                if (channel_listener != null)
                {
                    channel_listener.channelDisconnected(this);
                }

                init(); // sets local_addr=null; changed March 18 2003 (bela) -- prevented successful rejoining
            }
        }
    }

    /**
     * Destroys the channel.<BR> After this method has been called, the channel us unusable.<BR> This operation will disconnect the channel and close the channel receive queue
     * immediately<BR>
     */
    @Override
    public void close() throws InterruptedException
    {
        synchronized (this)
        {
            _close(true, true); // by default disconnect before closing channel and close mq
        }
    }

    /**
     * Opens the channel.<BR> this does the following actions<BR> 1. Resets the receiver queue by calling Queue.reset<BR> 2. Sets up the protocol stack by calling
     * ProtocolStack.setup<BR> 3. Sets the closed flag to false.<BR>
     *
     * @throws ChannelException
     */
    @Override
    public void open() throws ChannelException
    {
        synchronized (this)
        {
            if (!closed)
            {
                throw new ChannelException("GroupChannel.open(): channel is already open.");
            }

            try
            {
                mq.reset();

                // new stack is created on open() - bela June 12 2003
                prot_stack = new ProtocolStack(this, props);
                prot_stack.setup();
                closed = false;
            }
            catch (Exception e)
            {
                throw new ChannelException("GroupChannel().open(): " + e.getMessage());
            }
        }
    }

    /**
     * implementation of the Transport interface.<BR> Sends a message through the protocol stack<BR>
     *
     * @param msg the message to be sent through the protocol stack, the destination of the message is specified inside the message itself
     *
     *
     */
    @Override
    public void send(Message msg) throws ChannelClosedException, ChannelException
    {
        checkClosed();
        checkNotConnected();

        Event evt = new Event();
        evt.setType(Event.MSG);
        msg.setIsUserMsg(true);
        evt.setArg(msg);
        evt.setPriority(msg.getPriority());

        down(evt);
    }

    /**
     * creates a new message with the destination address, and the source address and the object as the message value
     *
     * @param dst - the destination address of the message, null for all members
     *
     * @param src - the source address of the message
     *
     * @param obj - the value of the message
     *
     * @see GroupChannel#send
     *
     */
    @Override
    public void send(Address dst, Address src, Object obj) throws ChannelClosedException, ChannelException
    {
        try
        {
            send(new Message(dst, src, obj));
        }
        catch (Exception ex)
        {
           if(getCacheLog().getIsErrorEnabled())getCacheLog().Error("GroupChannel.send()", ex.toString());
        }
    }

    /**
     * Blocking receive method. This method returns the object that was first received by this JChannel and that has not been received before. After the object is received, it is
     * removed from the receive queue.<BR> If you only want to inspect the object received without removing it from the queue call JChannel.peek<BR> If no messages are in the
     * receive queue, this method blocks until a message is added or the operation times out<BR> By specifying a timeout of 0, the operation blocks forever, or until a message has
     * been received.
     *
     * @param timeout the number of milliseconds to wait if the receive queue is empty. 0 means wait forever
     *
     * @exception ChannelClosedException
     *
     * @throws ChannelException
     * @see JChannel#peek
     *
     */
    @Override
    public Object receive(long timeout) throws ChannelClosedException, ChannelException
    {
        Object retval;
        Event evt;

        checkClosed();
        checkNotConnected();

        try
        {
            evt = (timeout <= 0) ? (Event) mq.remove() : (Event) mq.remove(timeout);
            retval = getEvent(evt);
            return retval;
        }
        catch (com.alachisoft.tayzgrid.common.datastructures.QueueClosedException e)
        {
            getCacheLog().Error("GroupChannel.receive()", e.toString());
            throw new ChannelClosedException();
        }

        catch (Exception e)
        {
            getCacheLog().Error("GroupChannel.receive()", e.toString());
            return null;
        }
    }

    /**
     * Just peeks at the next message, view or block. Does <em>not</em> install new view if view is received<BR> Does the same thing as GroupChannel.receive but doesn't remove the
     * object from the receiver queue
     *
     * @throws ChannelClosedException
     * @throws ChannelException
     */
    @Override
    public Event peek(long timeout) throws ChannelClosedException, ChannelException
    {
        Event evt;
        checkClosed();
        checkNotConnected();

        try
        {
            boolean success = true;
            tangible.RefObject<Boolean> tempRef_success = new tangible.RefObject<Boolean>(success);
            evt = (timeout <= 0) ? (Event) mq.peek() : (Event) mq.peek(timeout, tempRef_success);
            success = tempRef_success.argvalue;
            if (!success)
            { // timeout eception
                getCacheLog().Fatal("[Timeout]GroupChannel.peek: Timeout exception " + timeout);
                return null;
            }
            return evt;
        }
        catch (com.alachisoft.tayzgrid.common.datastructures.QueueClosedException queue_closed)
        {
            getCacheLog().Error("GroupChannel.peek()", queue_closed.toString());
            return null;
        }
        catch (Exception e)
        {
            getCacheLog().Error("GroupChannel.peek", "exception: " + e.toString());
            return null;
        }
    }

    /**
     * sets a channel option the options can be either
     * <PRE>
     * Channel.BLOCK
     * Channel.VIEW
     * Channel.SUSPECT
     * Channel.LOCAL
     * Channel.GET_STATE_EVENTS
     * Channel.AUTO_RECONNECT
     * Channel.AUTO_GETSTATE
     * </PRE> There are certain dependencies between the options that you can set, I will try to describe them here<BR> Option: Channel.VIEW option<BR> Value: java.lang.Boolean<BR>
     * Result: set to true the GroupChannel will receive VIEW change events<BR> <BR> Option: Channel.SUSPECT<BR> Value: java.lang.Boolean<BR> Result: set to true the GroupChannel
     * will receive SUSPECT events<BR> <BR> Option: Channel.BLOCK<BR> Value: java.lang.Boolean<BR> Result: set to true will set setOpt(VIEW, true) and the GroupChannel will receive
     * BLOCKS and VIEW events<BR> <BR> Option: GET_STATE_EVENTS<BR> Value: java.lang.Boolean<BR> Result: set to true the GroupChannel will receive state events<BR> <BR> Option:
     * LOCAL<BR> Value: java.lang.Boolean<BR> Result: set to true the GroupChannel will receive messages that it self sent out.<BR> <BR> Option: AUTO_RECONNECT<BR> Value:
     * java.lang.Boolean<BR> Result: set to true and the GroupChannel will try to reconnect when it is being closed<BR> <BR> Option: AUTO_GETSTATE<BR> Value: java.lang.Boolean<BR>
     * Result: set to true, the AUTO_RECONNECT will be set to true and the GroupChannel will try to get the state after a close and reconnect happens<BR> <BR>
     *
     *
     * @param option the parameter option Channel.VIEW, Channel.SUSPECT, etc
     *
     * @param value_Renamed the value to set for this option
     *
     *
     */
    @Override
    public void setOpt(int option, Object value_Renamed)
    {
        if (closed)
        {
            getCacheLog().Warn("GroupChannel.setOpt", "channel is closed; option not set!");
            return;
        }

        switch (option)
        {
            case SUSPECT:
                if (value_Renamed instanceof Boolean)
                {
                    receive_suspects = ((Boolean) value_Renamed);
                }
                else
                {
                    getCacheLog().Error("GroupChannel.setOpt", "option " + Channel.option2String(option) + " (" + value_Renamed + "): value has to be Boolean.");
                }
                break;

            case BLOCK:
                if (value_Renamed instanceof Boolean)
                {
                    receive_blocks = ((Boolean) value_Renamed);
                }
                else
                {
                    getCacheLog().Error("GroupChannel.setOpt", "option " + Channel.option2String(option) + " (" + value_Renamed + "): value has to be Boolean.");
                }
                if (receive_blocks)
                {
                    receive_views = true;
                }
                break;


            case LOCAL:
                if (value_Renamed instanceof Boolean)
                {
                    receive_local_msgs = ((Boolean) value_Renamed);
                }
                else
                {
                    getCacheLog().Error("GroupChannel.setOpt", "option " + Channel.option2String(option) + " (" + value_Renamed + "): value has to be Boolean.");
                }
                break;


            case AUTO_RECONNECT:
                if (value_Renamed instanceof Boolean)
                {
                    auto_reconnect = ((Boolean) value_Renamed);
                }
                else
                {
                    getCacheLog().Error("GroupChannel.setOpt", "option " + Channel.option2String(option) + " (" + value_Renamed + "): value has to be Boolean.");
                }
                break;


            default:
                getCacheLog().Error("GroupChannel.setOpt", "option " + Channel.option2String(option) + " not known.");
                break;

        }
    }

    /**
     * returns the value of an option.
     *
     * @param option the option you want to see the value for
     *
     * @return the object value, in most cases java.lang.Boolean
     *
     * @see GroupChannel#setOpt
     *
     */
    @Override
    public Object getOpt(int option)
    {
        switch (option)
        {
            case BLOCK:
                //                return Boolean.valueOf(receive_blocks);
                return receive_blocks ? true : false;

            case SUSPECT:
                //                return Boolean.valueOf(receive_suspects);
                return receive_suspects ? true : false;

            case LOCAL:
                //                return Boolean.valueOf(receive_local_msgs);
                return receive_local_msgs ? true : false;

            default:
                getCacheLog().Error("GroupChannel.get", "option " + Channel.option2String(option) + " not known.");
                return null;

        }
    }

    /**
     * Called to acknowledge a block() (callback in
     * <code>MembershipListener</code> or
     * <code>BlockEvent</code> received from call to
     * <code>receive()</code>). After sending blockOk(), no messages should be sent until a new view has been received. Calling this method on a closed channel has no effect.
     */
    @Override
    public void blockOk()
    {
        down(new Event(Event.BLOCK_OK));
        down(new Event(Event.START_QUEUEING));
    }

    /**
     * Callback method <BR> Called by the ProtocolStack when a message is received. It will be added to the message queue from which subsequent
     * <code>Receive</code>s will dequeue it.
     *
     * @param evt the event carrying the message from the protocol stack
     *
     */
    public void up(Event evt)
    {
        int type = evt.getType();
        Message msg;

        /*
         * if the queue is not available, there is no point in processing the message at all
         */
        if (mq == null)
        {
            getCacheLog().Error("GroupChannel.up", "message queue is null.");
            return;
        }
        switch (type)
        {


            case Event.MSG:
                msg = (Message) evt.getArg();
               
                if (!receive_local_msgs)
                {
                    // discard local messages (sent by myself to me)
                    if (local_addr != null && msg.getSrc() != null)
                    {
                        if (local_addr.equals(msg.getSrc()))
                        {
                            return;
                        }
                    }
                }
                break;


            case Event.VIEW_CHANGE:
                my_view = (View) evt.getArg();

                // crude solution to bug #775120: if we get our first view *before* the CONNECT_OK,
                // we simply set the state to connected
                if (connected == false)
                {
                    connected = true;
                    synchronized (connect_mutex)
                    {
                        // bug fix contributed by Chris Wampler (bug #943881)
                        connect_ok_event_received = true;
                        com.alachisoft.tayzgrid.common.threading.Monitor.pulse(connect_mutex);//connect_mutex.notify();
                    }
                }

                // unblock queueing of messages due to previous BLOCK event:
                down(new Event(Event.STOP_QUEUEING));
                if (!receive_views)
                // discard if client has not set receving views to on
                {
                    return;
                }
            
                break;


            case Event.SUSPECT:
                if (!receive_suspects)
                {
                    return;
                }
                break;


            case Event.CONFIG:
                java.util.HashMap config = (java.util.HashMap) evt.getArg();

                break;


            case Event.BLOCK:
                // If BLOCK is received by application, then we trust the application to not send
                // any more messages until a VIEW_CHANGE is received. Otherwise (BLOCKs are disabled),
                // we queue any messages sent until the next VIEW_CHANGE (they will be sent in the
                // next view)

                if (!receive_blocks)
                {
                    // discard if client has not set 'receiving blocks' to 'on'
                    down(new Event(Event.BLOCK_OK));
                    down(new Event(Event.START_QUEUEING));
                    return;
                }
                break;


            case Event.CONNECT_OK:
                synchronized (connect_mutex)
                {
                    connect_ok_event_received = true;
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(connect_mutex);//connect_mutex.notify();
                }
                break;

            case Event.CONNECT_OK_PHASE_2:
                synchronized (connect_mutex_phase2)
                {
                    connect_ok_event_received_phase2 = true;
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(connect_mutex_phase2);//connect_mutex_phase2.notify();
                }
                break;

            case Event.DISCONNECT_OK:
                synchronized (disconnect_mutex)
                {
                    disconnect_ok_event_received = true;
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(disconnect_mutex); //.notifyAll();
                }
                break;


            case Event.SET_LOCAL_ADDRESS:
                synchronized (local_addr_mutex)
                {
                    local_addr = (Address) evt.getArg();
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(local_addr_mutex);//local_addr_mutex.notifyAll();
                }
                break;


            case Event.EXIT:
                handleExit(evt);
                return; // no need to pass event up; already done in handleExit()


            case Event.BLOCK_SEND:
                synchronized (flow_control_mutex)
                {
                    getCacheLog().Error("GroupChannel.up", "received BLOCK_SEND.");
                    block_sending = true;
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(flow_control_mutex);// flow_control_mutex.notifyAll();
                }
                break;


            case Event.UNBLOCK_SEND:
                synchronized (flow_control_mutex)
                {
                    getCacheLog().Error("GroupChannel.up", "received UNBLOCK_SEND.");
                    block_sending = false;
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(flow_control_mutex);//flow_control_mutex.notifyAll();
                }
                break;


            default:
                break;

        }


        // If UpHandler is installed, pass all events to it and return (UpHandler is e.g. a building block)
        if (up_handler != null)
        {
            up_handler.up(evt);
            return;
        }

        if (type == Event.MSG || type == Event.VIEW_CHANGE || type == Event.SUSPECT || type == Event.BLOCK)
        { 
            try
            {
                mq.add(evt);
            }
            catch (Exception e)
            {
                getCacheLog().Error("GroupChannel.up()", e.toString());
            }
        }
    }

    /**
     * Sends a message through the protocol stack if the stack is available
     *
     * @param evt the message to send down, encapsulated in an event
     *
     */
    @Override
    public void down(Event evt)
    {
        if (evt == null)
        {
            return;
        }

        // only block for messages; all other events are passed through
        if (block_sending && evt.getType() == Event.MSG)
        {
            synchronized (flow_control_mutex)
            {
                while (block_sending)
                {
                    try
                    {
                        getCacheLog().Error("GroupChannel.down", "down() blocks because block_sending == true");
                        com.alachisoft.tayzgrid.common.threading.Monitor.wait(flow_control_mutex);//flow_control_mutex.wait();
                    }
                    catch (Exception e)
                    {
                        getCacheLog().Error("GroupChannel.down()", "exception=" + e);
                    }
                }
            }
        }

        // handle setting of additional data (kludge, will be removed soon)
        if (evt.getType() == Event.CONFIG)
        {
            try
            {
                java.util.Map m = (java.util.Map) evt.getArg();
                if (m != null && m.containsKey("additional_data"))
                {
                    additional_data = (byte[]) m.get("additional_data");
                }
            }
            catch (Exception t)
            {
                getCacheLog().Error("GroupChannel.down()", "CONFIG event did not contain a hashmap: " + t);
            }
        }

        if (prot_stack != null)
        {
            prot_stack.down(evt);
        }
        else
        {
            getCacheLog().Error("GroupChannel.down", "no protocol stack available.");
        }
    }

    public final String ToString(boolean details)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("local_addr=").append(local_addr).append('\n');
        sb.append("channel_name=").append(channel_name).append('\n');
        sb.append("my_view=").append(my_view).append('\n');
        sb.append("connected=").append(connected).append('\n');
        sb.append("closed=").append(closed).append('\n');
        if (mq != null)
        {
            sb.append("incoming queue size=").append(mq.getCount()).append('\n');
        }
        if (details)
        {
            sb.append("block_sending=").append(block_sending).append('\n');
            sb.append("receive_views=").append(receive_views).append('\n');
            sb.append("receive_suspects=").append(receive_suspects).append('\n');
            sb.append("receive_blocks=").append(receive_blocks).append('\n');
            sb.append("receive_local_msgs=").append(receive_local_msgs).append('\n');
            sb.append("auto_reconnect=").append(auto_reconnect).append('\n');
            sb.append("props=").append(props).append('\n');
        }

        return sb.toString();
    }


    /*
     * ----------------------------------- Private Methods -------------------------------------
     */
    /**
     * Initializes all variables. Used after <tt>close()</tt> or <tt>disconnect()</tt>, to be ready for new <tt>connect()</tt>
     */
    private void init()
    {
        local_addr = null;
        channel_name = null;
        my_view = null;

        // changed by Bela Sept 25 2003
        //if(mq != null && mq.closed())
        //  mq.reset();

        connect_ok_event_received = false;
        disconnect_ok_event_received = false;
        connected = false;
        block_sending = false; // block send()/down() if true (unlocked by UNBLOCK_SEND event)
    }

    /**
     * health check.<BR> throws a ChannelNotConnected exception if the channel is not connected
     */
    private void checkNotConnected() throws ChannelNotConnectedException
    {
        if (!connected)
        {
            throw new ChannelNotConnectedException();
        }
    }

    /**
     * health check<BR> throws a ChannelClosed exception if the channel is closed
     */
    private void checkClosed() throws ChannelClosedException
    {
        if (closed)
        {
            throw new ChannelClosedException();
        }
    }

    /**
     * returns the value of the event<BR> These objects will be returned<BR>
     * <PRE>
     * <B>Event Type    - Return Type</B>
     * Event.MSG           - returns a Message object
     * Event.VIEW_CHANGE   - returns a View object
     * Event.SUSPECT       - returns a SuspectEvent object
     * Event.BLOCK         - returns a new BlockEvent object
     * Event.GET_APPLSTATE - returns a GetStateEvent object
     * Event.STATE_RECEIVED- returns a SetStateEvent object
     * Event.Exit          - returns an ExitEvent object
     * All other           - return the actual Event object
     * </PRE>
     *
     * @param evt - the event of which you want to extract the value
     *
     * @return the event value if it matches the select list, returns null if the event is null returns the event itself if a match (See above) can not be made of the event type
     *
     */
    public static Object getEvent(Event evt)
    {
        if (evt == null)
        {
            return null; // correct ?
        }

        switch (evt.getType())
        {

            case Event.MSG:
                return evt.getArg();

            case Event.VIEW_CHANGE:
                return evt.getArg();

            case Event.SUSPECT:
                return new SuspectEvent(evt.getArg());

            case Event.BLOCK:
                return new BlockEvent();

            case Event.EXIT:
                return new ExitEvent();

            default:
                return evt;

        }
    }

    /**
     * Disconnects and closes the channel. This method does the folloing things 1. Calls
     * <code>this.disconnect</code> if the disconnect parameter is true 2. Calls
     * <code>Queue.close</code> on mq if the close_mq parameter is true 3. Calls
     * <code>ProtocolStack.stop</code> on the protocol stack 4. Calls
     * <code>ProtocolStack.destroy</code> on the protocol stack 5. Sets the channel closed and channel connected flags to true and false 6. Notifies any channel listener of the
     * channel close operation
     */
    private void _close(boolean disconect, boolean close_mq) throws InterruptedException
    {
        if (closed)
        {
            return;
        }

        if (disconect)
        {
            disconnect(); // leave group if connected
        }

        if (close_mq)
        {
            try
            {
                if (mq != null)
                {
                    mq.close(false); // closes and removes all messages
                }
            }
            catch (Exception e)
            {
                getCacheLog().Error("GroupChannel._close()", "exception: " + e.toString());
            }
        }

        if (prot_stack != null)
        {
            try
            {
                prot_stack.stopStack();
                prot_stack.destroy();
            }
            catch (Exception e)
            {
                getCacheLog().Error("GroupChannel._close():2", "exception: " + e);
            }
        }
        closed = true;
        connected = false;
        if (channel_listener != null)
        {
            channel_listener.channelClosed(this);
        }
        init(); // sets local_addr=null; changed March 18 2003 (bela) -- prevented successful rejoining
    }

    /**
     * Creates a separate thread to close the protocol stack. This is needed because the thread that called GroupChannel.up() with the EXIT event would hang waiting for up() to
     * return, while up() actually tries to kill that very thread. This way, we return immediately and allow the thread to terminate.
     */
    private void handleExit(Event evt)
    {
        if (channel_listener != null)
        {
            channel_listener.channelShunned();
        }

        if (closer != null && !closer.getIsAlive())
        {
            closer = null;
        }
        if (closer == null)
        {
            getCacheLog().Error("GroupChannel.handleExit", "received an EXIT event, will leave the channel");
            closer = new CloserThread(this, evt);
            closer.Start();
        }
    }

    /*
     * ------------------------------- End of Private Methods ----------------------------------
     */
    public static class CloserThread extends ThreadClass
    {

        private void InitBlock(GroupChannel enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private GroupChannel enclosingInstance;

        public final GroupChannel getEnclosing_Instance()
        {
            return enclosingInstance;
        }
        public Event evt;
        public ThreadClass t = null;

        public CloserThread(GroupChannel enclosingInstance, Event evt)
        {
            InitBlock(enclosingInstance);
            this.evt = evt;
            setName("CloserThread");
            setIsBackground(true);
        }

        @Override
        public void run()
        {
            try
            {
                String old_channel_name = getEnclosing_Instance().channel_name; // remember because close() will null it
                String old_subGroup_name = getEnclosing_Instance().subGroup_name; // remember because for reconnect, it is required
                this.enclosingInstance.getCacheLog().Error("CloserThread.Run", "GroupChannel: " + "closing the channel");
                getEnclosing_Instance()._close(false, false); // do not disconnect before closing channel, do not close mq (yet !)

                if (getEnclosing_Instance().up_handler != null)
                {
                    getEnclosing_Instance().up_handler.up(this.evt);
                }
                else
                {
                    try
                    {
                        getEnclosing_Instance().mq.add(this.evt);
                    }
                    catch (Exception ex)
                    {
                        this.enclosingInstance.getCacheLog().Error("CloserThread.Run()", "exception: " + ex.toString());
                    }
                }

                if (getEnclosing_Instance().mq != null)
                {
                    com.alachisoft.tayzgrid.cluster.util.Util.sleep(500); // give the mq thread a bit of time to deliver EXIT to the application
                    try
                    {
                        getEnclosing_Instance().mq.close(false);
                    }
                    catch (Exception e)
                    {
                        this.enclosingInstance.getCacheLog().Error("CloserThread.Run()", "exception=" + e);
                    }
                }

                if (getEnclosing_Instance().auto_reconnect)
                {
                    try
                    {
                        this.enclosingInstance.getCacheLog().Error("GroupChannel", "reconnecting to group " + old_channel_name);
                        getEnclosing_Instance().open();
                    }
                    catch (Exception ex)
                    {
                        this.enclosingInstance.getCacheLog().Error("CloserThread.Run():2", "failure reopening channel: " + ex.toString());
                        return;
                    }

                    try
                    {
                        if (getEnclosing_Instance().additional_data != null)
                        {
                            // set previously set additional data
                            java.util.Map m = new java.util.HashMap(11);
                            m.put("additional_data", getEnclosing_Instance().additional_data);
                            getEnclosing_Instance().down(new Event(Event.CONFIG, m));
                        }
                        getEnclosing_Instance().connect(old_channel_name, old_subGroup_name, getEnclosing_Instance()._isStartedAsMirror, false);
                        if (getEnclosing_Instance().channel_listener != null)
                        {
                            getEnclosing_Instance().channel_listener.channelReconnected(getEnclosing_Instance().local_addr);
                        }
                    }
                    catch (Exception ex)
                    {
                        this.enclosingInstance.getCacheLog().Error("CloserThread.Run():3", "failure reconnecting to channel: " + ex.getMessage());
                    }
                }
            }
            catch (Exception ex)
            {
                this.enclosingInstance.getCacheLog().Error("CloserThread.Run()", ex.toString());
            }
            finally
            {
                getEnclosing_Instance().closer = null;
            }
        }
    }
}
