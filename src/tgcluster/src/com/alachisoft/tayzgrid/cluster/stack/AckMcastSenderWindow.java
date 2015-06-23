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

import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.GenericCopier;
import java.util.ArrayList;
import java.util.Collections;

// $Id: AckMcastSenderWindow.java,v 1.5 2004/07/05 14:17:32 belaban Exp $
/**
 * Keeps track of ACKs from receivers for each message. When a new message is sent, it is tagged with a sequence number and the receiver set (set of members to which the message is
 * sent) and added to a hashtable (key = sequence number, val = message + receiver set). Each incoming ACK is noted and when all ACKs for a specific sequence number haven been
 * received, the corresponding entry is removed from the hashtable. A retransmission thread periodically re-sends the message point-to-point to all receivers from which no ACKs
 * have been received yet. A view change or suspect message causes the corresponding non-existing receivers to be removed from the hashtable. <p> This class may need flow control
 * in order to avoid needless retransmissions because of timeouts.
 *
 *
 * <author> Bela Ban June 9 1999 </author> <author> John Georgiadis May 8 2001 </author> <version> $Revision: 1.5 $ </version>
 */
public class AckMcastSenderWindow
{

     
    private abstract static class Task implements TimeScheduler.Task
    {

        private Interval intervals;
        private boolean cancelled_Renamed_Field;

        protected Task(long[] intervals)
        {
            this.intervals = new Interval(intervals);
            this.cancelled_Renamed_Field = false;
        }

        public long GetNextInterval()
        {
            return (intervals.next());
        }

        public void cancel()
        {
            cancelled_Renamed_Field = true;
        }

        public boolean IsCancelled()
        {
            return (cancelled_Renamed_Field);
        }

        public void Run()
        {
        }

        public final String getName()
        {
            return "AckMcastSenderWindow.Task";
        }
    }

     
    private static class Entry extends Task
    {

        private void InitBlock(AckMcastSenderWindow enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private AckMcastSenderWindow enclosingInstance;

        public final AckMcastSenderWindow getEnclosing_Instance()
        {
            return enclosingInstance;
        }
        /**
         * The msg sequence number
         */
        public long seqno;
        /**
         * The msg to retransmit
         */
        public Message msg = null;
        /**
         * destination addr -> boolean (true = received, false = not)
         */
        //: no need to sync HashMap; it's synced by default
        public java.util.HashMap senders = new java.util.HashMap();
        /**
         * How many destinations have received the msg
         */
        public int num_received = 0;

        public Entry(AckMcastSenderWindow enclosingInstance, long seqno, Message msg, java.util.ArrayList dests, long[] intervals)
        {
            super(intervals);
            InitBlock(enclosingInstance);
            this.seqno = seqno;
            this.msg = msg;
            for (int i = 0; i < dests.size(); i++)
            {
                senders.put(dests.get(i), false);
            }
        }

        public boolean allReceived()
        {
            return (num_received >= senders.size());
        }

        /**
         * Retransmit this entry
         */
        @Override
        public void Run()
        {
            getEnclosing_Instance()._retransmit(this);
        }

        @Override
        public String toString()
        {
            StringBuilder buff = new StringBuilder();
            buff.append("num_received = " + num_received + ", received msgs = " + Global.CollectionToString(senders));
            return (buff.toString());
        }
    }

    /**
     * @return a copy of stable messages, or null (if non available). Removes all stable messages afterwards
     *
     */
    public java.util.List getStableMessages()
    {
        java.util.List retval;

        synchronized (stable_msgs)
        {
            retval = (stable_msgs.size() > 0) ? (java.util.List) GenericCopier.DeepCopy(stable_msgs)/*.clone()*/ : null;
            if (stable_msgs.size() > 0)
            {
                stable_msgs.clear();
            }
        }

        return (retval);
    }

    /**
     * Called by retransmitter thread whenever a message needs to be re-sent to a destination.
     * <code>dest</code> has to be set in the
     * <code>dst</code> field of
     * <code>msg</code>, as the latter was sent multicast, but now we are sending a unicast message. Message has to be copied before sending it (as headers will be appended and
     * therefore the message changed!).
     */
    public interface RetransmitCommand
    {

        /**
         * Retranmit the given msg
         *
         *
         * @param seqno the sequence number associated with the message
         *
         * @param msg the msg to retransmit (it should be a copy!)
         *
         * @param dest the msg destination
         *
         */
        void retransmit(long seqno, Message msg, Address dest);
    }
    private static final long SEC = 1000;
    /**
     * Default retransmit intervals (ms) - exponential approx.
     */
    private static final long[] RETRANSMIT_TIMEOUTS = new long[]
    {
        2 * SEC, 3 * SEC, 5 * SEC, 8 * SEC
    };
    /**
     * Default retransmit thread suspend timeout (ms)
     */
    private static final long SUSPEND_TIMEOUT = 30 * 1000;
    // Msg tables related
    /**
     * Table of pending msgs: seqno -> Entry
     */
    //no need to sync HashMap; it's synced by default
    private java.util.HashMap msgs = new java.util.HashMap();
    /**
     * List of recently suspected members. Used to cease retransmission to suspected members
     */
    private java.util.ArrayList suspects = new java.util.ArrayList();
    /**
     * Max number in suspects list
     */
    private int max_suspects = 20;
    /**
     * List of acknowledged msgs since the last call to
     * <code>getStableMessages()</code>
     */
    private java.util.List stable_msgs = Collections.synchronizedList(new java.util.ArrayList(10));
    /**
     * Whether a call to
     * <code>waitUntilAcksReceived()</code> is still active
     */
    private boolean waiting = false;
    // Retransmission thread related
    /**
     * Whether retransmitter is externally provided or owned by this object
     */
    private boolean retransmitter_owned;
    /**
     * The retransmission scheduler
     */
    private TimeScheduler retransmitter = null;
    /**
     * Retransmission intervals
     */
    private long[] retransmit_intervals;
    /**
     * The callback object for retransmission
     */
    private AckMcastSenderWindow.RetransmitCommand cmd = null;
 
    private ILogger _ncacheLog;

    private ILogger getCacheLog()
    {
        return _ncacheLog;
    }

    /**
     * @param entry the record associated with the msg to retransmit. It contains the list of receivers that haven't yet ack reception
     *
     */
    private void _retransmit(Entry entry)
    {
        Address sender;
        boolean received;

        synchronized (entry)
        {
            for (java.util.Iterator e = entry.senders.keySet().iterator(); e.hasNext();)
            {
                sender = (Address) e.next();
                received = ((Boolean) (entry.senders.get(sender))).booleanValue();
                if (!received)
                {
                    if (suspects.contains(sender))
                    {
                        getCacheLog().Warn("AckMcastSenderWindow", "removing " + sender + " from retransmit list as it is in the suspect list");

                        remove(sender);
                        continue;
                    }

                    getCacheLog().Warn("AckMcastSenderWindow", "--> retransmitting msg #" + entry.seqno + " to " + sender);

                    cmd.retransmit(entry.seqno, entry.msg.copy(), sender);
                }
            }
        }
    }

    /**
     * Setup this object's state
     *
     *
     * @param cmd the callback object for retranmissions
     *
     * @param retransmit_timeout the interval between two consecutive retransmission attempts
     *
     * @param sched the external scheduler to use to schedule retransmissions
     *
     * @param sched_owned if true, the scheduler is owned by this object and can be started/stopped/destroyed. If false, the scheduler is shared among multiple objects and
     * start()/stop() should not be called from within this object
     *
     *
     * <throws> IllegalArgumentException if
     * <code>cmd</code> is null </throws>
     */
    private void init(AckMcastSenderWindow.RetransmitCommand cmd, long[] retransmit_intervals, TimeScheduler sched, boolean sched_owned)
    {
        if (cmd == null)
        {
            getCacheLog().Error("AckMcastSenderWindow.init", "command is null. Cannot retransmit " + "messages !");
            throw new IllegalArgumentException("Command is null.");
        }

        retransmitter_owned = sched_owned;
        retransmitter = sched;
        this.retransmit_intervals = retransmit_intervals;
        this.cmd = cmd;

        start();
    }

 
    /**
     * Create and <b>start</b> the retransmitter
     *
     *
     * @param cmd the callback object for retranmissions
     *
     * @param sched the external scheduler to use to schedule retransmissions
     *
     *
     * <throws> IllegalArgumentException if
     * <code>cmd</code> is null </throws>
     */
    public AckMcastSenderWindow(AckMcastSenderWindow.RetransmitCommand cmd, TimeScheduler sched, ILogger NCacheLog)
    {
        this._ncacheLog = NCacheLog;
        init(cmd, RETRANSMIT_TIMEOUTS, sched, false);
    }

    /**
     * Create and <b>start</b> the retransmitter
     *
     *
     * @param cmd the callback object for retranmissions
     *
     * @param retransmit_timeout the interval between two consecutive retransmission attempts
     *
     *
     * <throws> IllegalArgumentException if
     * <code>cmd</code> is null </throws>
     */
    public AckMcastSenderWindow(AckMcastSenderWindow.RetransmitCommand cmd, long[] retransmit_intervals, ILogger NCacheLog)
    {
        this._ncacheLog = NCacheLog;
        init(cmd, retransmit_intervals, new TimeScheduler(SUSPEND_TIMEOUT), true);
    }
 
    /**
     * Adds a new message to the hash table.
     *
     *
     * @param seqno The sequence number associated with the message
     *
     * @param msg The message (should be a copy!)
     *
     * @param receivers The set of addresses to which the message was sent and from which consequently an ACK is expected
     *
     */
    public void add(long seqno, Message msg, java.util.ArrayList receivers)
    {
        Entry e;

        if (waiting)
        {
            return;
        }
        if (receivers.isEmpty())
        {
            return;
        }

        synchronized (msgs)
        {
            if (msgs.get((long) seqno) != null)
            {
                return;
            }
            e = new Entry(this, seqno, msg, receivers, retransmit_intervals);
            msgs.put((long) seqno, e);
            retransmitter.AddTask(e);
        }
    }

    /**
     * An ACK has been received from
     * <code>sender</code>. Tag the sender in the hash table as 'received'. If all ACKs have been received, remove the entry all together.
     *
     *
     * @param seqno The sequence number of the message for which an ACK has been received.
     *
     * @param sender The sender which sent the ACK
     *
     */
    public void ack(long seqno, Address sender)
    {
        Entry entry;

        synchronized (msgs)
        {
            entry = (Entry) msgs.get((long) seqno);
            if (entry == null)
            {
                return;
            }

            synchronized (entry)
            {
                Object temp = entry.senders.get(sender);
                if (temp == null)
                {
                    return;
                }

                boolean received = (Boolean) temp;
                if (received)
                {
                    return;
                }

                // If not yet received
                entry.senders.put(sender, true);
                entry.num_received++;
                if (!entry.allReceived())
                {
                    return;
                }
            }

            synchronized (stable_msgs)
            {
                entry.cancel();
                msgs.remove((long) seqno);
                stable_msgs.add((long) seqno);
            }

            
            Monitor.pulse(msgs); 
        }
    }

    /**
     * Remove
     * <code>obj</code> from all receiver sets and wake up retransmission thread.
     *
     *
     * @param obj the sender to remove
     *
     */
    public void remove(Address obj)
    {
        long key;
        Entry entry;

        synchronized (msgs)
        {
            for (java.util.Iterator e = msgs.keySet().iterator(); e.hasNext();)
            {
                key = (Long) e.next();
                entry = (Entry) msgs.get(key);
                synchronized (entry)
                {
                  
                    Object tempObject;
                    tempObject = entry.senders.get(obj);
                    entry.senders.remove(obj);
                    if (tempObject == null)
                    {
                        continue; // suspected member not in entry.senders ?
                    }

                    boolean received = (Boolean) tempObject;
                    if (received)
                    {
                        entry.num_received--;
                    }
                    if (!entry.allReceived())
                    {
                        continue;
                    }
                }
                synchronized (stable_msgs)
                {
                    entry.cancel();
                    msgs.remove(key);
                    stable_msgs.add(key);
                    e = msgs.keySet().iterator();
                }
                
                Monitor.pulse(msgs); 
            }
        }
    }

    /**
     * Process with address
     * <code>suspected</code> is suspected: remove it from all receiver sets. This means that no ACKs are expected from this process anymore.
     *
     *
     * @param suspected The suspected process
     *
     */
    public void suspect(Address suspected)
    {
        getCacheLog().Warn("AckMcastSenderWindow", "suspect is " + suspected);

        remove(suspected);
        suspects.add(suspected);
        if (suspects.size() >= max_suspects)
        {
            suspects.remove(0);
        }
    }

    public void clearStableMessages()
    {
        synchronized (stable_msgs)
        {
            stable_msgs.clear();
        }
    }

    /**
     * @return the number of currently pending msgs
     *
     */
    public long size()
    {
        synchronized (msgs)
        {
            return (msgs.size());
        }
    }

    /**
     * Returns the number of members for a given entry for which acks have to be received
     */
    public final long getNumberOfResponsesExpected(long seqno)
    {
        Entry entry = (Entry) msgs.get((long) seqno);
        if (entry != null)
        {
            return entry.senders.size();
        }
        else
        {
            return - 1;
        }
    }

    /**
     * Returns the number of members for a given entry for which acks have been received
     */
    public final long getNumberOfResponsesReceived(long seqno)
    {
        Entry entry = (Entry) msgs.get((long) seqno);
        if (entry != null)
        {
            return entry.num_received;
        }
        else
        {
            return - 1;
        }
    }

    /**
     * Prints all members plus whether an ack has been received from those members for a given seqno
     */
    public final String printDetails(long seqno)
    {
        Entry entry = (Entry) msgs.get((long) seqno);
        if (entry != null)
        {
            return entry.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * Waits until all outstanding messages have been ACKed by all receivers. Takes into account suspicions and view changes. Returns when there are no entries left in the
     * hashtable. <b>While waiting, no entries can be added to the hashtable (they will be discarded).</b>
     *
     *
     * @param timeout Miliseconds to wait. 0 means wait indefinitely.
     *
     */
    public final void waitUntilAllAcksReceived(long timeout)
    {
        long time_to_wait, start_time, current_time;
        Address suspect;

        // remove all suspected members from retransmission
        for (java.util.Iterator it = suspects.iterator(); it.hasNext();)
        {
            suspect = (Address) it.next();
            remove(suspect);
        }

        time_to_wait = timeout;
        waiting = true;
        if (timeout <= 0)
        {
            synchronized (msgs)
            {
                while (msgs.size() > 0)
                {
                    try
                    {
                        //Changing pulse to notify
                        Monitor.wait(msgs); 
                       
                    }
                    catch (InterruptedException e)
                    {
                        getCacheLog().Error("AckMcastSenderWindow.waitUntilAllAcksReceived()", e.toString());
                    }
                }
            }
        }
        else
        {
            //changing to System.currentTimeMillis
         
            start_time = System.currentTimeMillis();
            synchronized (msgs)
            {
                while (msgs.size() > 0)
                {
                    //changing to System.currentTimeinMillis
                    current_time = System.currentTimeMillis();
                    //current_time = (new java.util.Date().Ticks - 621355968000000000) / 10000;
                    time_to_wait = timeout - (current_time - start_time);
                    if (time_to_wait <= 0)
                    {
                        break;
                    }

                    try
                    {
                         
                        Monitor.wait(msgs,time_to_wait); 
                    }
                    catch (InterruptedException ex)
                    {
                        getCacheLog().Error("AckMcastSenderWindow.waitUntilAllAcksReceived", ex.toString());
                    }
                }
            }
        }

       
        if (time_to_wait < 0)
        {
            getCacheLog().Fatal("[Timeout]AckMcastSenderWindow.waitUntillAllAcksReceived:" + time_to_wait);
        }

        waiting = false;
    }

    /**
     * Start the retransmitter. This has no effect, if the retransmitter was externally provided
     */
    public final void start()
    {
        if (retransmitter_owned)
        {
            retransmitter.Start();
        }
    }

    /**
     * Stop the rentransmition and clear all pending msgs. <p> If this retransmitter has been provided an externally managed scheduler, then just clear all msgs and the associated
     * tasks, else stop the scheduler. In this case the method blocks until the scheduler's thread is dead. Only the owner of the scheduler should stop it.
     */
    public final void stop()
    {
        Entry entry;

        // i. If retransmitter is owned, stop it else cancel all tasks
        // ii. Clear all pending msgs and notify anyone waiting
        synchronized (msgs)
        {
            if (retransmitter_owned)
            {
                try
                {
                    retransmitter.Stop();
                    retransmitter.dispose();
                }
                catch (InterruptedException ex)
                {
                    getCacheLog().Error("AckMcastSenderWindow.stop()", ex.toString());
                }
            }
            else
            {
                for (java.util.Iterator e = msgs.values().iterator(); e.hasNext();)
                {
                    entry = (Entry) e.next();
                    entry.cancel();
                }
            }
            msgs.clear();
            // wake up waitUntilAllAcksReceived() method
 
            Monitor.pulse(msgs);// msgs.notify();
        }
    }

    /**
     * Remove all pending msgs from the hashtable. Cancel all associated tasks in the retransmission scheduler
     */
    public final void reset()
    {
        Entry entry;

        if (waiting)
        {
            return;
        }

        synchronized (msgs)
        {
            for (java.util.Iterator e = msgs.values().iterator(); e.hasNext();)
            {
                entry = (Entry) e.next();
                entry.cancel();
            }
            msgs.clear();

     
            Monitor.pulse(msgs); 
        }
    }

    @Override
    public String toString()
    {
        StringBuilder ret;
        Entry entry;
        long key;

        ret = new StringBuilder();
        synchronized (msgs)
        {
            ret.append("msgs: (" + msgs.size() + ')');
            for (java.util.Iterator e = msgs.keySet().iterator(); e.hasNext();)
            {
                key = ((Long) e.next()).longValue();
                entry = (Entry) msgs.get(key);
                ret.append("key = " + key + ", value = " + entry + '\n');
            }
            synchronized (stable_msgs)
            {
                ret.append("\nstable_msgs: " + Global.CollectionToString(stable_msgs));
            }
        }

        return (ret.toString());
    }
}
