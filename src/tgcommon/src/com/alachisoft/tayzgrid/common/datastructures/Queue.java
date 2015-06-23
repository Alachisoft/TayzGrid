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

package com.alachisoft.tayzgrid.common.datastructures;

import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.threading.Monitor;

/**
 * Elements are added at the tail and removed from the head. Class is thread-safe in that 1 producer and 1 consumer may add/remove elements concurrently. The class is not
 * explicitely designed for multiple producers or consumers.
 */
public class Queue
{

    private java.util.LinkedList[] _queues = new java.util.LinkedList[3];

    /*
     * flag to determine the state of the queue
     */
    private boolean closed = false;

    /*
     * current size of the queue
     */
    private int size = 0;

    /*
     * Lock object for synchronization. Is notified when element is added
     */
    private Object mutex = new Object();
    /**
     * Lock object for syncing on removes. It is notified when an object is removed
     */
    // Object  remove_mutex=new Object();

    /*
     * the number of end markers that have been added
     */
    private int num_markers = 0;
    /**
     * if the queue closes during the runtime an endMarker object is added to the end of the queue to indicate that the queue will close automatically when the end marker is
     * encountered This allows for a "soft" close.
     *
     * @see Queue#close
     *
     */
    private static final Object endMarker = new Object();

    /**
     * creates an empty queue
     */
    public Queue()
    {
        _queues[Priority.Critical.getValue()] = new java.util.LinkedList();
        _queues[Priority.Normal.getValue()] = new java.util.LinkedList();
        _queues[Priority.Low.getValue()] = new java.util.LinkedList();
    }

    /**
     * Number of Objects in the queue
     */
    public final int getCount()
    {
        return size - num_markers;
    }

    /**
     * returns true if the Queue has been closed however, this method will return false if the queue has been closed using the close(true) method and the last element has yet not
     * been received.
     *
     * @return true if the queue has been closed
     *
     */
    public final boolean getClosed()
    {
        return closed;
    }

    /**
     * adds an object to the tail of this queue If the queue has been closed with close(true) no exception will be thrown if the queue has not been flushed yet.
     *
     * @param obj - the object to be added to the queue
     *
     * @exception QueueClosedException exception if closed() returns true
     *
     */
    public final void add(Object obj)
    {
        add(obj, Priority.Normal);
    }

    /**
     * adds an object to the tail of this queue If the queue has been closed with close(true) no exception will be thrown if the queue has not been flushed yet.
     *
     * @param obj - the object to be added to the queue
     *
     * @param priority - the priority of the object
     *
     * @exception QueueClosedException exception if closed() returns true
     *
     */
    public final void add(Object obj, Priority priority)
    {
        if (obj == null)
        {
            return;
        }
        synchronized (mutex)
        {
            if (closed)
            {
                throw new QueueClosedException();
            }
            if (this.num_markers > 0)
            {
                throw new QueueClosedException("Queue.add(): queue has been closed. You can not add more elements. " + "Waiting for removal of remaining elements.");
            }

            _queues[priority.getValue()].offer(obj);
            size++;
            Monitor.pulse(mutex);
        }
    }

    /**
     * Removes 1 element from head or <B>blocks</B> until next element has been added or until queue has been closed
     *
     * @return the first element to be taken of the queue
     *
     */
    public final Object remove() throws QueueClosedException, InterruptedException
    {
        Object retval = null;
        retval = remove(Long.MAX_VALUE);

        return retval;
    }

    /**
     * Removes 1 element from the head. If the queue is empty the operation will wait for timeout ms. if no object is added during the timeout time, a Timout exception is thrown
     *
     * @param timeout - the number of milli seconds this operation will wait before it times out
     *
     * @return the first object in the queue
     * @throws QueueClosedException
     *
     */
    public final Object remove(long timeout) throws QueueClosedException, InterruptedException
    {
        Object retval = null;

        /*
         * lock the queue
         */
        synchronized (mutex)
        {
            /*
             * if the queue size is zero, we want to wait until a new object is added
             */
            if (size == 0)
            {
                if (closed)
                {
                    throw new QueueClosedException();
                }

                /*
                 * release the add_mutex lock and wait no more than timeout ms
                 */
                boolean wait = false;
                try
                {
                   Monitor.wait(mutex, timeout); // mutex.wait(timeout);
                }
                catch (InterruptedException ex)
                {
                    wait = true;
                }
                int x = 0;
            }
            /*
             * check to see if the object closed
             */
            if (closed)
            {
                throw new QueueClosedException();
            }


            /*
             * get the next value
             */
            if (_queues[Priority.Critical.getValue()].size() > 0)
            {
                retval = removeInternal(_queues[Priority.Critical.getValue()]);
            }
            else if (_queues[Priority.Normal.getValue()].size() > 0)
            {
                retval = removeInternal(_queues[Priority.Normal.getValue()]);
            }
            else if (_queues[Priority.Low.getValue()].size() > 0)
            {
                retval = removeInternal(_queues[Priority.Low.getValue()]);
            }

            /*
             * if we reached an end marker we are going to close the queue
             */
            if (retval == endMarker)
            {
                close(false);
                throw new QueueClosedException();
            }

            /*
             * at this point we actually did receive a value from the queue, return it
             */
            return retval;
        }
    }

    /**
     * returns the first object on the queue, without removing it. If the queue is empty this object blocks until the first queue object has been added
     *
     * @return the first object on the queue
     *
     */
    public final Object peek()
    {
        Object retval = null;
        boolean success = false;
        tangible.RefObject<Boolean> tempRef_success = new tangible.RefObject<Boolean>(success);
        retval = peek(Long.MAX_VALUE, tempRef_success);
        success = tempRef_success.argvalue;
        return retval;
    }

    /**
     * returns the first object on the queue, without removing it. If the queue is empty this object blocks until the first queue object has been added or the operation times out
     *
     * @param timeout how long in milli seconds will this operation wait for an object to be added to the queue before it times out
     *
     * @param error this param is set to false if timeout occurs
     * @return the first object on the queue
     *
     */
    public final Object peek(long timeout, tangible.RefObject<Boolean> success)
    {
        Object retval = null;
        success.argvalue = true;
        synchronized (mutex)
        {
            if (size == 0)
            {
                if (closed)
                {
                    throw new QueueClosedException();
                }
                try
                {
                    long millis = System.currentTimeMillis();
                    if (timeout != 0)
                    {
                        Monitor.wait(mutex, timeout *1000);//mutex.wait(timeout * 1000);
                        if((System.currentTimeMillis() - millis) > timeout * 1000)
                        {
                            success.argvalue = false;
                            return null;
                        }
                    }
                    else
                    {
                        success.argvalue = false;
                        return null;
                    }
                }
                catch (InterruptedException ex)
                {
                    success.argvalue = false;
                    return null;
                }


            }

            if (closed)
            {
                throw new QueueClosedException();
            }

            retval = peekInternal();

            if (retval == endMarker)
            {
                close(false);
                throw new QueueClosedException();
            }
        }
        return retval;
    }

    private Object peekInternal()
    {
        Object retval = null;
        if (_queues[Priority.Critical.getValue()].size() > 0)
        {
            retval = _queues[Priority.Critical.getValue()].peek();
        }
        else if (_queues[Priority.Normal.getValue()].size() > 0)
        {
            retval = _queues[Priority.Normal.getValue()].peek();
        }
        else if (_queues[Priority.Low.getValue()].size() > 0)
        {
            retval = _queues[Priority.Low.getValue()].peek();
        }
        return retval;
    }

    /**
     * Marks the queues as closed. When an
     * <code>add</code> or
     * <code>remove</code> operation is attempted on a closed queue, an exception is thrown.
     *
     * @param flush_entries When true, a end-of-entries marker is added to the end of the queue. Entries may be added and removed, but when the end-of-entries marker is
     * encountered, the queue is marked as closed. This allows to flush pending messages before closing the queue.
     *
     */
    public void close(boolean flush_entries)
    {
        synchronized (mutex)
        {
            if (flush_entries)
            {
                try
                {
                    add(endMarker, Priority.Low);
                    num_markers++;
                }
                catch (QueueClosedException ex)
                {
                }
                return;
            }

            closed = true;
            Monitor.pulse(mutex);
        }
    }

    /**
     * resets the queue. This operation removes all the objects in the queue and marks the queue open
     */
    public void reset()
    {
        synchronized (mutex)
        {
            num_markers = 0;
            if (!closed)
            {
                close(false);
            }

            size = 0;

            _queues[Priority.Critical.getValue()].clear();
            _queues[Priority.Normal.getValue()].clear();
            _queues[Priority.Low.getValue()].clear();

            closed = false;
            Monitor.pulse(mutex);
        }
    }

    /**
     * prints the size of the queue
     */
    @Override
    public String toString()
    {
        return "Queue (" + getCount() + ") messages";
    }


    /*
     * ------------------------------------- Private Methods -----------------------------------
     */
    /**
     * Removes the first element. Returns null if no elements in queue. Always called with add_mutex locked (we don't have to lock add_mutex ourselves)
     */
    private Object removeInternal(java.util.LinkedList queue)
    {
        Object obj = null;
        synchronized (mutex)
        {
            int count = queue.size();
            if (count > 0)
            {
                obj = queue.poll();
            }
            else
            {
                return null;
            }

            size--;
            if (size < 0)
            {
                size = 0;
            }

            if (peekInternal() == endMarker)
            {
                closed = true;
            }
        }

        return obj;
    }
}
