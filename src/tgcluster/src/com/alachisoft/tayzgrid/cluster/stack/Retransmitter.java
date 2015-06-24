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

import com.alachisoft.tayzgrid.cluster.util.Util;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.net.Address;

/**
 * Maintains a pool of sequence numbers of messages that need to be retransmitted. Messages are aged and retransmission requests sent according to age (linear backoff used). If a
 * TimeScheduler instance is given to the constructor, it will be used, otherwise Reransmitter will create its own. The retransmit timeouts have to be set first thing after
 * creating an instance. The
 * <code>add()</code> method adds a range of sequence numbers of messages to be retransmitted. The
 * <code>remove()</code> method removes a sequence number again, cancelling retransmission requests for it. Whenever a message needs to be retransmitted, the
 * <code>RetransmitCommand.retransmit()</code> method is called. It can be used e.g. by an ack-based scheme (e.g. AckSenderWindow) to retransmit a message to the receiver, or by a
 * nak-based scheme to send a retransmission request to the sender of the missing message.
 *
 *
 * <author> John Giorgiadis </author> <author> Bela Ban </author> <version> $Revision: 1.4 $ </version>
 */
public class Retransmitter
{

    public void setRetransmitTimeouts(long[] value)
    {
        if (value != null)
        {
            RETRANSMIT_TIMEOUTS = value;
        }
    }
    private static final long SEC = 1000;
    /**
     * Default retransmit intervals (ms) - exponential approx.
     */
    private static long[] RETRANSMIT_TIMEOUTS = new long[]
    {
        2 * SEC, 3 * SEC, 5 * SEC, 8 * SEC
    };
    /**
     * Default retransmit thread suspend timeout (ms)
     */
    private static final long SUSPEND_TIMEOUT = 2000;
    private Address sender = null;
    private java.util.ArrayList msgs = new java.util.ArrayList();
    private Retransmitter.RetransmitCommand cmd = null;
    private boolean retransmitter_owned;
    private TimeScheduler retransmitter = null;

    /**
     * Retransmit command (see Gamma et al.) used to retrieve missing messages
     */
    public interface RetransmitCommand
    {

        /**
         * Get the missing messages between sequence numbers
         * <code>first_seqno</code> and
         * <code>last_seqno</code>. This can either be done by sending a retransmit message to destination
         * <code>sender</code> (nak-based scheme), or by retransmitting the missing message(s) to
         * <code>sender</code> (ack-based scheme).
         *
         * @param first_seqno The sequence number of the first missing message
         *
         * @param last_seqno The sequence number of the last missing message
         *
         * @param sender The destination of the member to which the retransmit request will be sent (nak-based scheme), or to which the message will be retransmitted (ack-based
         * scheme).
         *
         */
        void retransmit(long first_seqno, long last_seqno, Address sender);
    }

    /**
     * Create a new Retransmitter associated with the given sender address
     *
     * @param sender the address from which retransmissions are expected or to which retransmissions are sent
     *
     * @param cmd the retransmission callback reference
     *
     * @param sched retransmissions scheduler
     *
     */
    public Retransmitter(Address sender, Retransmitter.RetransmitCommand cmd, TimeScheduler sched)
    {
        init(sender, cmd, sched, false);
    }

    /**
     * Create a new Retransmitter associated with the given sender address
     *
     * @param sender the address from which retransmissions are expected or to which retransmissions are sent
     *
     * @param cmd the retransmission callback reference
     *
     */
    public Retransmitter(Address sender, Retransmitter.RetransmitCommand cmd)
    {
        init(sender, cmd, new TimeScheduler(30 * 1000), true);
    }

    /**
     * Add the given range [first_seqno, last_seqno] in the list of entries eligible for retransmission. If first_seqno > last_seqno, then the range [last_seqno, first_seqno] is
     * added instead <p> If retransmitter thread is suspended, wake it up TODO: Does not check for duplicates !
     */
    public void add(long first_seqno, long last_seqno)
    {
        Entry e;

        if (first_seqno > last_seqno)
        {
            long tmp = first_seqno;
            first_seqno = last_seqno;
            last_seqno = tmp;
        }
        synchronized (msgs)
        {
            e = new Entry(this, first_seqno, last_seqno, RETRANSMIT_TIMEOUTS);
            msgs.add(e);
            retransmitter.AddTask(e);
        }
    }

    /**
     * Remove the given sequence number from the list of seqnos eligible for retransmission. If there are no more seqno intervals in the respective entry, cancel the entry from the
     * retransmission scheduler and remove it from the pending entries
     */
    public void remove(long seqno)
    {
        synchronized (msgs)
        {
            for (int index = 0; index < msgs.size(); index++)
            {
                Entry e = (Entry) msgs.get(index);
                synchronized (e)
                {
                    if (seqno < e.low || seqno > e.high)
                    {
                        continue;
                    }
                    e.remove(seqno);
                    if (e.low > e.high)
                    {
                        e.cancel();
                        msgs.remove(index);
                    }
                }
                break;
            }
        }
    }

    /**
     * Reset the retransmitter: clear all msgs and cancel all the respective tasks
     */
    public void reset()
    {
        synchronized (msgs)
        {
            for (int index = 0; index < msgs.size(); index++)
            {
                Entry entry = (Entry) msgs.get(index);
                entry.cancel();
            }
            msgs.clear();
        }
    }

    /**
     * Stop the rentransmition and clear all pending msgs. <p> If this retransmitter has been provided an externally managed scheduler, then just clear all msgs and the associated
     * tasks, else stop the scheduler. In this case the method blocks until the scheduler's thread is dead. Only the owner of the scheduler should stop it.
     */
    public void stop()
    {
        // i. If retransmitter is owned, stop it else cancel all tasks
        // ii. Clear all pending msgs
        synchronized (msgs)
        {
            if (retransmitter_owned)
            {
                try
                {
                    retransmitter.dispose();
                }
                catch (InterruptedException ex)
                {
                    
                }
            }
            else
            {
                for (int index = 0; index < msgs.size(); index++)
                {
                    Entry e = (Entry) msgs.get(index);
                    e.cancel();
                }
            }
            msgs.clear();
        }
    }

    @Override
    public String toString()
    {
        return (msgs.size() + " messages to retransmit: (" + Global.CollectionToString(msgs) + ')');
    }

    /*
     * ------------------------------- Private Methods --------------------------------------
     */
    /**
     * Init this object
     *
     *
     * @param sender the address from which retransmissions are expected
     *
     * @param cmd the retransmission callback reference
     *
     * @param sched retransmissions scheduler
     *
     * @param sched_owned whether the scheduler parameter is owned by this object or is externally provided
     *
     */
    private void init(Address sender, Retransmitter.RetransmitCommand cmd, TimeScheduler sched, boolean sched_owned)
    {
        this.sender = sender;
        this.cmd = cmd;
        retransmitter_owned = sched_owned;
        retransmitter = sched;
    }


    /*
     * ---------------------------- End of Private Methods ------------------------------------
     */
    /**
     * The retransmit task executed by the scheduler in regular intervals
     */
    private abstract static class Task implements TimeScheduler.Task
    {

        private Interval intervals;
        private boolean isCancelled;

        protected Task(long[] intervals)
        {
            this.intervals = new Interval(intervals);
            this.isCancelled = false;
        }

        public long GetNextInterval()
        {
            return (intervals.next());
        }

        public boolean IsCancelled()
        {
            return (isCancelled);
        }

        public void cancel()
        {
            isCancelled = true;
        }

        public void Run()
        {
        }
    }

    /**
     * The entry associated with an initial group of missing messages with contiguous sequence numbers and with all its subgroups.<br> E.g. - initial group: [5-34] - msg 12 is
     * acknowledged, now the groups are: [5-11], [13-34] <p> Groups are stored in a list as long[2] arrays of the each group's bounds. For speed and convenience, the lowest &
     * highest bounds of all the groups in this entry are also stored separately
     */
    private static class Entry extends Task
    {

        private Retransmitter enclosingInstance;
        public java.util.ArrayList list;
        public long low;
        public long high;

        public Entry(Retransmitter enclosingInstance, long low, long high, long[] intervals)
        {
            super(intervals);
            this.enclosingInstance = enclosingInstance;
            this.low = low;
            this.high = high;
            list = new java.util.ArrayList();
            list.add(low);
            list.add(high);
        }

        /**
         * Remove the given seqno and resize or partition groups as necessary. The algorithm is as follows:<br> i. Find the group with low <= seqno <= high ii. If seqno == low, a.
         * if low == high, then remove the group Adjust global low. If global low was pointing to the group deleted in the previous step, set it to point to the next group. If
         * there is no next group, set global low to be higher than global high. This way the entry is invalidated and will be removed all together from the pending msgs and the
         * task scheduler iii. If seqno == high, adjust high, adjust global high if this is the group at the tail of the list iv. Else low < seqno < high, break [low,high] into
         * [low,seqno-1] and [seqno+1,high]
         *
         *
		 @param seqno the sequence number to remove
         *
         */
        public void remove(long seqno)
        {
            int i;
            long loBound = -1;
            long hiBound = -1;

            synchronized (this)
            {
                for (i = 0; i < list.size(); i += 2)
                {
                    loBound = (Long) list.get(i);
                    hiBound = (Long) list.get(i + 1);

                    if (seqno < loBound || seqno > hiBound)
                    {
                        continue;
                    }
                    break;
                }
                if (i == list.size())
                {
                    return;
                }

                if (seqno == loBound)
                {
                    if (loBound == hiBound)
                    {
                        list.remove(i);
                        list.remove(i);
                    }
                    else
                    {
                        list.set(i, ++loBound);
                    }

                    if (i == 0)
                    {
                        low = list.isEmpty() ? high + 1 : loBound;
                    }
                }
                else if (seqno == hiBound)
                {
                    list.set(i + 1, --hiBound);
                    if (i == list.size() - 1)
                    {
                        high = hiBound;
                    }
                }
                else
                {
                    list.set(i + 1, seqno - 1);

                    list.add(i + 2, hiBound);
                    list.add(i + 2, seqno + 1);
                }
            }
        }

        /**
         * Retransmission task:<br> For each interval, call the retransmission callback command
         */
        @Override
        public void Run()
        {
            java.util.ArrayList cloned;
            synchronized (this)
            {
                cloned = (java.util.ArrayList) list.clone();
            }
            for (int i = 0; i < cloned.size(); i += 2)
            {
                long loBound = (Long) cloned.get(i);
                long hiBound = (Long) cloned.get(i + 1);
                enclosingInstance.cmd.retransmit(loBound, hiBound, enclosingInstance.sender);
            }
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            if (low == high)
            {
                sb.append(low);
            }
            else
            {
                sb.append(low).append(':').append(high);
            }
            return sb.toString();
        }
    } // end class Entry

    public static void sleep(long timeout) throws InterruptedException
    {
        Util.sleep(timeout);
    }
}
