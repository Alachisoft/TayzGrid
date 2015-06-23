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
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.logger.ILogger;

/**
 * ACK-based sliding window for a sender. Messages are added to the window keyed by seqno When an ACK is received, the corresponding message is removed. The Retransmitter
 * continously iterates over the entries in the hashmap, retransmitting messages based on their creation time and an (increasing) timeout. When there are no more messages in the
 * retransmission table left, the thread terminates. It will be re-activated when a new entry is added to the retransmission table.
 *
 * <author> Bela Ban </author>
 */
public class AckSenderWindow implements Retransmitter.RetransmitCommand
{

    private void InitBlock()
    {
        retransmitter = new Retransmitter(null, this);
    }
    public AckSenderWindow.RetransmitCommand retransmit_command = null; // called to request XMIT of msg
    public java.util.HashMap msgs = new java.util.HashMap(); // keys: seqnos (Long), values: Messages
    public long[] interval = new long[]
    {
        1000, 2000, 3000, 4000
    };
    public Retransmitter retransmitter;
    public com.alachisoft.tayzgrid.common.datastructures.Queue msg_queue = new com.alachisoft.tayzgrid.common.datastructures.Queue(); // for storing messages if msgs is full
    public int window_size = - 1; // the max size of msgs, when exceeded messages will be queued
    /**
     * when queueing, after msgs size falls below this value, msgs are added again (queueing stops)
     */
    public int min_threshold = - 1;
    public boolean use_sliding_window = false, queueing = false;
    public Protocol transport = null; // used to send messages
    //private NewTrace nTrace = null;
    //string _cacheName;
    private ILogger _ncacheLog;

    private ILogger getCacheLog()
    {
        return _ncacheLog;
    }

    public interface RetransmitCommand
    {

        void retransmit(long seqno, Message msg);
    }

    /**
     * Creates a new instance. Thre retransmission thread has to be started separately with
     * <code>start()</code>.
     *
     * @param com If not null, its method
     * <code>retransmit()</code> will be called when a message needs to be retransmitted (called by the Retransmitter).
     *
     */
    public AckSenderWindow(AckSenderWindow.RetransmitCommand com, ILogger NCacheLog)
    {
         
        _ncacheLog = NCacheLog;

        InitBlock();
        retransmit_command = com;
        retransmitter.setRetransmitTimeouts(interval);
    }

    public AckSenderWindow(AckSenderWindow.RetransmitCommand com, long[] interval, ILogger NCacheLog)
    {
        //this.nTrace = nTrace;
        _ncacheLog = NCacheLog;
        InitBlock();
        retransmit_command = com;
        this.interval = interval;
        retransmitter.setRetransmitTimeouts(interval);
    }

    /**
     * This constructor whould be used when we want AckSenderWindow to send the message added by add(), rather then ourselves.
     */
    public AckSenderWindow(AckSenderWindow.RetransmitCommand com, long[] interval, Protocol transport, ILogger NCacheLog)
    {
 
        _ncacheLog = NCacheLog;
        InitBlock();
        retransmit_command = com;
        this.interval = interval;
        this.transport = transport;
        retransmitter.setRetransmitTimeouts(interval);
    }

    public void setWindowSize(int window_size, int min_threshold)
    {
        this.window_size = window_size;
        this.min_threshold = min_threshold;

        // sanity tests for the 2 values:
        if (min_threshold > window_size)
        {
            this.min_threshold = window_size;
            this.window_size = min_threshold;
            getCacheLog().Warn("min_threshold (" + min_threshold + ") has to be less than window_size ( " + window_size + "). Values are swapped");
        }
        if (this.window_size <= 0)
        {
            this.window_size = this.min_threshold > 0 ? (int) (this.min_threshold * 1.5) : 500;
            getCacheLog().Warn("window_size is <= 0, setting it to " + this.window_size);
        }
        if (this.min_threshold <= 0)
        {
            this.min_threshold = this.window_size > 0 ? (int) (this.window_size * 0.5) : 250;
            getCacheLog().Warn("min_threshold is <= 0, setting it to " + this.min_threshold);
        }


        getCacheLog().Debug("window_size=" + this.window_size + ", min_threshold=" + this.min_threshold);
        use_sliding_window = true;
    }

    public void reset()
    {
        synchronized (msgs)
        {
            msgs.clear();
        }
 
        retransmitter.reset();
    }

    /**
     * Adds a new message to the retransmission table. If the message won't have received an ack within a certain time frame, the retransmission thread will retransmit the message
     * to the receiver. If a sliding window protocol is used, we only add up to
     * <code>window_size</code> messages. If the table is full, we add all new messages to a queue. Those will only be added once the table drains below a certain threshold (
     * <code>min_threshold</code>)
     */
    public void add(long seqno, Message msg)
    {
        synchronized (msgs)
        {
            if (msgs.containsKey(seqno))
            {
                return;
            }

           
            if (!use_sliding_window)
            {
                addMessage(seqno, msg);
            }
            else
            {
                // we use a sliding window
                if (queueing)
                {
                    addToQueue(seqno, msg);
                }
                else
                {
                    if (msgs.size() + 1 > window_size)
                    {
                        queueing = true;
                        addToQueue(seqno, msg);
                        if (getCacheLog().getIsInfoEnabled())
                        {
                            getCacheLog().Info("window_size (" + window_size + ") was exceeded, " + "starting to queue messages until window size falls under " + min_threshold);
                        }
                    }
                    else
                    {
                        addMessage(seqno, msg);
                    }
                }
            }
        }
    }

    /**
     * Removes the message from
     * <code>msgs</code>, removing them also from retransmission. If sliding window protocol is used, and was queueing, check whether we can resume adding elements. Add all
     * elements. If this goes above window_size, stop adding and back to queueing. Else set queueing to false.
     */
    public void ack(long seqno)
    {
        Entry entry;

        synchronized (msgs)
        {
            msgs.remove(seqno);
            retransmitter.remove(seqno);

            if (use_sliding_window && queueing)
            {
                if (msgs.size() < min_threshold)
                {
                    // we fell below threshold, now we can resume adding msgs
                    if (getCacheLog().getIsInfoEnabled())
                    {
                        getCacheLog().Info("number of messages in table fell under min_threshold (" + min_threshold + "): adding " + msg_queue.getCount() + " messages on queue");
                    }

                    while (msgs.size() < window_size)
                    {
                        if ((entry = removeFromQueue()) != null)
                        {
                            addMessage(entry.seqno, entry.msg);
                        }
                        else
                        {
                            break;
                        }
                    }

                    if (msgs.size() + 1 > window_size)
                    {
                        if (getCacheLog().getIsInfoEnabled())
                        {
                            getCacheLog().Info("exceeded window_size (" + window_size + ") again, will still queue");
                        }
                        return; // still queueing
                    }
                    else
                    {
                        queueing = false; // allows add() to add messages again
                    }

                    if (getCacheLog().getIsInfoEnabled())
                    {
                        getCacheLog().Info("set queueing to false (table size=" + msgs.size() + ')');
                    }
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return Global.CollectionToString(msgs.keySet()) + " (retransmitter: " + retransmitter.toString() + ')';
    }

    /*
     * -------------------------------- Retransmitter.RetransmitCommand interface -------------------
     */
    public void retransmit(long first_seqno, long last_seqno, Address sender)
    {
        Message msg;

        if (retransmit_command != null)
        {
            for (long i = first_seqno; i <= last_seqno; i++)
            {
                if ((msg = (Message) msgs.get((long) i)) != null)
                {
                    // find the message to retransmit
                    retransmit_command.retransmit(i, msg);
                    
                }
            }
        }
    }
    /*
     * ----------------------------- End of Retransmitter.RetransmitCommand interface ----------------
     */

    /*
     * ---------------------------------- Private methods ---------------------------------------
     */
    public void addMessage(long seqno, Message msg)
    {
        if (transport != null)
        {
            transport.passDown(new Event(Event.MSG, msg));
        }
        msgs.put(seqno, msg);
        retransmitter.add(seqno, seqno);
    }

    public void addToQueue(long seqno, Message msg)
    {
        try
        {
            msg_queue.add(new Entry(this, seqno, msg));
        }
        catch (Exception ex)
        {
            getCacheLog().Error("AckSenderWindow.add()", ex.toString());
        }
    }

    public Entry removeFromQueue()
    {
        try
        {
            return msg_queue.getCount() == 0 ? null : (Entry) msg_queue.remove();
        }
        catch (Exception ex)
        {
            getCacheLog().Error("AckSenderWindow.removeFromQueue()", ex.toString());

            return null;
        }
    }
    /*
     * ------------------------------ End of Private methods ------------------------------------
     */

    /**
     * Struct used to store message alongside with its seqno in the message queue
     */
    public static class Entry
    {

        private void InitBlock(AckSenderWindow enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private AckSenderWindow enclosingInstance;

        public final AckSenderWindow getEnclosing_Instance()
        {
            return enclosingInstance;
        }
        public long seqno;
        public Message msg;

        public Entry(AckSenderWindow enclosingInstance, long seqno, Message msg)
        {
            InitBlock(enclosingInstance);
            this.seqno = seqno;
            this.msg = msg;
        }
    }
}
