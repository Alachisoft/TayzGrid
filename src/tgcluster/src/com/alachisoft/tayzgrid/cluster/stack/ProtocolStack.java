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

import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.GroupChannel;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Transport;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.threading.Promise;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A ProtocolStack manages a number of protocols layered above each other. It creates all protocol classes, initializes them and, when ready, starts all of them, beginning with the
 * bottom most protocol. It also dispatches messages received from the stack to registered objects (e.g. channel, GMP) and sends messages sent by those objects down the stack.<p>
 * The ProtocolStack makes use of the Configurator to setup and initialize stacks, and to destroy them again when not needed anymore
 *
 * <author> Bela Ban </author>
 */
public class ProtocolStack extends Protocol implements Transport
{

    /**
     * Returns all protocols in a list, from top to bottom. <em>These are not copies of protocols, so modifications will affect the actual instances !</em>
     */
    public java.util.List getProtocols()
    {
        Protocol p;
        java.util.List v = Collections.synchronizedList(new java.util.ArrayList(10));

        p = top_prot;
        while (p != null)
        {
            v.add(p);
            p = p.getDownProtocol();
        }
        return v;
    }

    @Override
    public String getName()
    {
        return "ProtocolStack";
    }
    private Protocol top_prot = null;
    private Protocol bottom_prot = null;
    private Configurator conf = new Configurator();
    private GroupChannel channel = null;
    public TimeScheduler timer = new TimeScheduler(30 * 1000);
    private String setup_string;
    private boolean operational = false;
    private boolean stopped = true;
    public Promise ack_promise = new Promise();
    /**
     * Used to sync on START/START_OK events for start()
     */
    public Promise start_promise;
    /**
     * used to sync on STOP/STOP_OK events for stop()
     */
    public Promise stop_promise;
    public static final int ABOVE = 1; // used by insertProtocol()
    public static final int BELOW = 2; // used by insertProtocol()
    private ProtocolStackType stackType = ProtocolStackType.values()[0];

    public PerfStatsCollector perfStatsColl = new PerfStatsCollector("Empty");

 
    private ILogger _ncacheLog;

    public final ILogger getCacheLog()
    {
        return _ncacheLog;
    }
    
    
     
     

    public final void setNCacheLog(ILogger value)
    {
        _ncacheLog = value;
    }

   
    public ProtocolStack(GroupChannel channel, String setup_string)
    {
        this.setup_string = setup_string;
        this.channel = channel;
    }
 

    public final void InitializePerfCounters(String instance, Monitor monitor)
    {
		perfStatsColl.setInstanceName(instance);
		boolean enableDebuggingCounters = false;
		perfStatsColl.InitializePerfCounters(enableDebuggingCounters, monitor);

    }
 

    public final ProtocolStackType getStackType()
    {
        return stackType;
    }

    public final void setStackType(ProtocolStackType value)
    {
        stackType = value;
    }

    public final boolean getIsOperational()
    {
        return operational;
    }

    public final void setIsOperational(boolean value)
    {
        operational = value;
    }

    /**
     * Prints the names of the protocols, from the bottom to top. If include_properties is true, the properties for each protocol will also be printed.
     */
    public String printProtocolSpec(boolean include_properties)
    {
        StringBuilder sb = new StringBuilder();
        Protocol prot = top_prot;
        String name;

        while (prot != null)
        {
            name = prot.getName();
            if (name != null)
            {
                if ((new String("ProtocolStack")).equals(name))
                {
                    break;
                }
                sb.append(name);
                if (include_properties)
                {
                    java.util.HashMap props = prot.getProperties();
                    java.util.Map.Entry entry;
                    if (props != null)
                    {
                        sb.append('\n');
                        for (java.util.Iterator it = props.entrySet().iterator(); it.hasNext();)
                        {
                            entry = (java.util.Map.Entry) it.next();
                            sb.append(entry + "\n");
                        }
                    }
                }
                sb.append('\n');

                prot = prot.getDownProtocol();
            }
        }

        return sb.toString();
    }

    public void setup() throws Exception
    {
        if (top_prot == null)
        {
            top_prot = conf.setupProtocolStack(setup_string, this);
            if (top_prot == null)
            {
                throw new Exception("ProtocolStack.setup(): couldn't create protocol stack");
            }
            top_prot.setUpProtocol(this);
            bottom_prot = conf.getBottommostProtocol(top_prot);
            conf.startProtocolStack(bottom_prot); // sets up queues and threads
        }
    }

    /**
     * Creates a new protocol given the protocol specification.
     *
     * @param prot_spec The specification of the protocol. Same convention as for specifying a protocol stack. An exception will be thrown if the class cannot be created. Example:
     * <pre>"VERIFY_SUSPECT(timeout=1500)"</pre> Note that no colons (:) have to be specified
     *
     * @return Protocol The newly created protocol
     *
     * @exception Exception Will be thrown when the new protocol cannot be created
     *
     */
    public Protocol createProtocol(String prot_spec) throws Exception
    {
        return conf.createProtocol(prot_spec, this);
    }

    /**
     * Inserts an already created (and initialized) protocol into the protocol list. Sets the links to the protocols above and below correctly and adjusts the linked list of
     * protocols accordingly. Note that this method may change the value of top_prot or bottom_prot.
     *
     * @param prot The protocol to be inserted. Before insertion, a sanity check will ensure that none of the existing protocols have the same name as the new protocol.
     *
     * @param position Where to place the protocol with respect to the neighbor_prot (ABOVE, BELOW)
     *
     * @param neighbor_prot The name of the neighbor protocol. An exception will be thrown if this name is not found
     *
     * @exception Exception Will be thrown when the new protocol cannot be created, or inserted.
     *
     */
    public void insertProtocol(Protocol prot, int position, String neighbor_prot) throws Exception
    {
        conf.insertProtocol(prot, position, neighbor_prot, this);
    }

    /**
     * Removes a protocol from the stack. Stops the protocol and readjusts the linked lists of protocols.
     *
     * @param prot_name The name of the protocol. Since all protocol names in a stack have to be unique (otherwise the stack won't be created), the name refers to just 1 protocol.
     *
     * @exception Exception Thrown if the protocol cannot be stopped correctly.
     *
     */
    public void removeProtocol(String prot_name)
    {
        conf.removeProtocol(prot_name);
    }

    /**
     * Returns a given protocol or null if not found
     */
    public Protocol findProtocol(String name)
    {
        Protocol tmp = top_prot;
        String prot_name;
        while (tmp != null)
        {
            prot_name = tmp.getName();
            if (prot_name != null && prot_name.equals(name))
            {
                return tmp;
            }
            tmp = tmp.getDownProtocol();
        }
        return null; // conf.findProtocol(this, name);
    }

    @Override
    public void destroy()
    {
        if (top_prot != null)
        {
            conf.stopProtocolStack(top_prot); // destroys msg queues and threads
 
            top_prot = null;
        }
    }

    /**
     * Start all layers. The {@link Protocol#start()} method is called in each protocol, <em>from top to bottom</em>. Each layer can perform some initialization, e.g. create a
     * multicast socket
     */
    public void startStack() throws Exception
    {
        Object start_result = null;
        if (stopped == false)
        {
            return;
        }

        timer.Start();

        if (start_promise == null)
        {
            start_promise = new Promise();
        }
        else
        {
            start_promise.Reset();
        }

        down(new Event(Event.START));
        start_result = start_promise.WaitResult(0);
        if (start_result != null && start_result instanceof Exception)
        {
            if (start_result instanceof Exception)
            {
                throw (Exception) start_result;
            }
            else
            {
                throw new Exception("ProtocolStack.start(): exception is " + start_result);
            }
        }
        stopped = false;
    }

    @Override
    public void startUpHandler()
    {
         
    }

    @Override
    public void startDownHandler()
    {
        
    }

    /**
     * Iterates through all the protocols <em>from top to bottom</em> and does the following: <ol> <li>Waits until all messages in the down queue have been flushed (ie., size is 0)
     * <li>Calls stop() on the protocol </ol>
     */
    public void stopStack() throws InterruptedException
    {
        if (timer != null)
        {
            try
            {
                timer.dispose();
            }
            catch (Exception ex)
            {
                getCacheLog().Error("ProtocolStack.stopStack", "exception=" + ex);
            }
        }

        if (stopped)
        {
            return;
        }

        if (stop_promise == null)
        {
            stop_promise = new Promise();
        }
        else
        {
            stop_promise.Reset();
        }

        down(new Event(Event.STOP));
        stop_promise.WaitResult(5000);

        operational = false;
        stopped = true;
    }

    @Override
    public void stopInternal()
    {
      
    }

    /**
     * Flushes all events currently in the <em>down</em> queues and returns when done. This guarantees that all events sent <em>before</em> this call will have been handled.
     */
    public void flushEvents()
    {
        long start, stop;
        ack_promise.Reset();

         
        start = System.currentTimeMillis();
        
        down(new Event(Event.ACK));
        ack_promise.WaitResult(0);
        stop = System.currentTimeMillis();
        
    }

    /*
     * --------------------------- Transport interface ------------------------------
     */
    public void send(Message msg)
    {
        down(new Event(Event.MSG, msg));
    }

    @Override
    public Object receive(long timeout) throws Exception
    {
        throw new Exception("ProtocolStack.receive(): not implemented !");
    }
    /*
     * ------------------------- End of Transport interface ---------------------------
     */

    @Override
    public void up(Event evt)
    {
        switch (evt.getType())
        {

            case Event.ACK_OK:
                ack_promise.SetResult((Object) true);
                return;

            case Event.START_OK:
                if (start_promise != null)
                {
                    start_promise.SetResult(evt.getArg());
                }
                return;

            case Event.STOP_OK:
                if (stop_promise != null)
                {
                    stop_promise.SetResult(evt.getArg());
                }
                return;
        }

        if (channel != null)
        {
            channel.up(evt);
        }
    }

    @Override
    public void down(Event evt)
    {
        if (top_prot != null)
        {
            top_prot.receiveDownEvent(evt);
        }
        else
        {
            getCacheLog().Error("ProtocolStack", "no down protocol available !");
        }
    }

    @Override
    public void receiveUpEvent(Event evt)
    {
        up(evt);
    }

    /**
     * Override with null functionality: we don't need any threads to be started !
     */
    public void startWork()
    {
    }

    /**
     * Override with null functionality: we don't need any threads to be started !
     */
    public void stopWork()
    {
    }
    /*
     * ----------------------- End of Protocol functionality ---------------------------
     */
}
