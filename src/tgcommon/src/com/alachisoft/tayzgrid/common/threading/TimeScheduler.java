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

package com.alachisoft.tayzgrid.common.threading;

import com.alachisoft.tayzgrid.common.logger.ILogger;

/**
 * The scheduler supports varying scheduling intervals by asking the task every time for its next preferred scheduling interval. Scheduling can either be <i>fixed-delay</i> or
 * <i>fixed-rate</i>. In fixed-delay scheduling, the task's new schedule is calculated as:<br></br> new_schedule = time_task_starts + scheduling_interval <p> In fixed-rate
 * scheduling, the next schedule is calculated as:<br></br> new_schedule = time_task_was_supposed_to_start + scheduling_interval</p> <p> The scheduler internally holds a queue of
 * tasks sorted in ascending order according to their next execution time. A task is removed from the queue if it is cancelled, i.e. if <tt>TimeScheduler.Task.isCancelled()</tt>
 * returns true. </p> <p> Initially, the scheduler is in <tt>SUSPEND</tt>ed mode, <tt>start()</tt> need not be called: if a task is added, the scheduler gets started automatically.
 * Calling <tt>start()</tt> starts the scheduler if it's suspended or stopped else has no effect. Once <tt>stop()</tt> is called, added tasks will not restart it: <tt>start()</tt>
 * has to be called to restart the scheduler. </p>
 *
 *
 * Fixed-delay and fixed-rate single thread scheduler <p><b>Author:</b> Chris Koiak, Bela Ban</p> <p><b>Date:</b> 12/03/2003</p>
 */
public class TimeScheduler implements Runnable
{

    /**
     * The interface that submitted tasks must implement
     */
    public interface Task
    {

        /**
         * Returns true if task is cancelled and shouldn't be scheduled again
         *
         * @return
         */
        boolean IsCancelled();

        /**
         * The next schedule interval
         *
         * @return The next schedule interval
         */
        long GetNextInterval();

        /**
         * Execute the task
         */
        void Run();
    }
    /**
     * Needed in case all tasks have been cancelled and we are still waiting on the schedule time of the task at the top
     *
     * Regular wake-up intervals for scheduler
     */
    private static final long TICK_INTERVAL = 1000;

    private enum State
    {

        /**
         * State Constant
         */
        RUN(0),
        /**
         * State Constant
         */
        SUSPEND(1),
        /**
         * State Constant
         */
        STOPPING(2),
        /**
         * State Constant
         */
        STOP(3),
        /**
         * State Constant
         */
        DISPOSED(4);
        private int intValue;
        private static java.util.HashMap<Integer, State> mappings;

        private static java.util.HashMap<Integer, State> getMappings()
        {
            if (mappings == null)
            {
                synchronized (State.class)
                {
                    if (mappings == null)
                    {
                        mappings = new java.util.HashMap<Integer, State>();
                    }
                }
            }
            return mappings;
        }

        private State(int value)
        {
            intValue = value;
            State.getMappings().put(value, this);
        }

        public int getValue()
        {
            return intValue;
        }

        public static State forValue(int value)
        {
            return getMappings().get(value);
        }
    }
    /**
     * TimeScheduler thread name
     */
    private static final String THREAD_NAME = "TimeScheduler.Thread";
    private static ILogger _logger = null;
    /**
     * The scheduler thread
     */
    private Thread thread = null;
    /**
     * The thread's running state
     */
    private State thread_state = State.SUSPEND;
    /**
     * Time that task queue is empty before suspending the scheduling thread
     */
    private long suspend_interval;
    /**
     * Sorted list of
     * <code>IntTask</code>s
     */
    private EventQueue queue;

    /**
     * Set the thread state to running, create and start the thread
     */
    private void _start()
    {
        synchronized (this)
        {
            if (thread_state != State.DISPOSED)
            {
                thread_state = State.RUN;
                thread = new Thread(this);
                thread.setName(THREAD_NAME);
                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    /**
     * Restart the suspended thread
     */
    private void _unsuspend()
    {
        _start();
    }

    /**
     * Set the thread state to suspended
     */
    private void _suspend()
    {
        synchronized (this)
        {
            if (thread_state != State.DISPOSED)
            {
                thread_state = State.SUSPEND;
                thread = null;
            }
        }
    }

    /**
     * Set the thread state to stopping
     */
    private void _stopping()
    {
        synchronized (this)
        {
            if (thread_state != State.DISPOSED)
            {
                thread_state = State.STOPPING;
            }
        }
    }

    /**
     * Set the thread state to stopped
     */
    private void _stop()
    {
        synchronized (this)
        {
            if (thread_state != State.DISPOSED)
            {
                thread_state = State.STOP;
                thread.interrupt();
                thread = null;
            }
        }
    }

    /**
     * Get the first task, if the running time hasn't been reached then wait a bit and retry. Else reschedule the task and then run it. *
     *
     * If the task queue is empty, sleep until a task comes in or if slept for too long, suspend the thread.
     */
    @Override
    public void run()
    {
        long elapsedTime;
        try
        {
            while (true && !Thread.currentThread().isInterrupted())
            {
                synchronized (this)
                {
                    if (thread == null)
                    {
                        return;
                    }
                }

                Task task = null;
                boolean lockReAcquired = true;
                synchronized (queue)
                {
                    if (queue.getIsEmpty())
                    {
                        try{
                        //Monitor.Wait(queue, (int) suspend_interval);
                        long startTime = System.nanoTime();
                        Monitor.wait(queue); //queue.wait();
                        long endTime = System.nanoTime();
                        lockReAcquired = (endTime - startTime) < suspend_interval;
                        }catch (InterruptedException exception)
                        {
                            break;
                        }
                    }

                    if (lockReAcquired)
                    {
                        QueuedEvent e = queue.Peek();
                        if (e != null)
                        {
                            synchronized (e)
                            {
                                task = e.Task;
                                if (task.IsCancelled())
                                {
                                    queue.Pop();
                                    continue;
                                }

                                elapsedTime = e.getElapsedTime();
                                if (elapsedTime >= e.getInterval())
                                {
                                    queue.Pop();
                                    if (e.ReQueue())
                                    {
                                        queue.Push(e);
                                    }
                                }
                            }
                            if (elapsedTime < e.getInterval())
                            {
                                try{
                                    Monitor.wait(queue,e.getInterval() - elapsedTime);
                                }
                                catch (InterruptedException exception)
                                {
                                    break;
                                }
                                continue;
                            }
                        }
                    }
                }
                synchronized (this)
                {
                    if (queue.getIsEmpty() && !lockReAcquired)
                    {
                        _suspend();
                        return;
                    }
                }
                try
                {
                    if (task != null)
                    {
                        task.Run();
                    }
                }
                catch (Exception ex)
                {
                }
            }
        }
        catch (Exception ex)
        {
        }

    }

    /**
     * Create a scheduler that executes tasks in dynamically adjustable intervals
     *
     * @param suspend_interval The time that the scheduler will wait for at least one task to be placed in the task queue before suspending the scheduling thread
     *
     */
    public TimeScheduler(long suspend_interval)
    {
        queue = new EventQueue();
        this.suspend_interval = suspend_interval;
    }

    /**
     * Create a scheduler that executes tasks in dynamically adjustable intervals
     */
    public TimeScheduler()
    {
        this(2000);
    }

    /**
     * <b>Relative Scheduling</b> <tt>true</tt>:<br></br> Task is rescheduled relative to the last time it <i>actually</i> started execution <p> <tt>false</tt>:<br></br> Task is
     * scheduled relative to its <i>last</i> execution schedule. This has the effect that the time between two consecutive executions of the task remains the same. </p>
     *
     *
     * Add a task for execution at adjustable intervals
     *
     * @param t The task to execute
     * @param relative Use relative scheduling
     */
    public final void AddTask(Task t, boolean relative)
    {
        long interval;
        synchronized (this)
        {
            if (thread_state == State.DISPOSED)
            {
                return;
            }
            if ((interval = t.GetNextInterval()) < 0)
            {
                return;
            }

            queue.Push(new QueuedEvent(t));

            switch (thread_state)
            {
                case RUN: 
                    break;
                case SUSPEND:
                    _unsuspend();
                    break;
                case STOPPING:
                    break;
                case STOP:
                    break;
            }
        }
    }

    /**
     * Add a task for execution at adjustable intervals
     *
     * @param t The task to execute
     */
    public final void AddTask(Task t)
    {
        AddTask(t, true);
    }

    /**
     * Start the scheduler, if it's suspended or stopped
     */
    public final void Start()
    {
        synchronized (this)
        {
            switch (thread_state)
            {
                case DISPOSED:
                    break;
                case RUN:
                    break;
                case SUSPEND:
                    _unsuspend();
                    break;
                case STOPPING:
                    break;
                case STOP:
                    _start();
                    break;
            }
        }
    }

    /**
     * Stop the scheduler if it's running. Switch to stopped, if it's suspended. Clear the task queue.
     */
    public final void Stop() throws InterruptedException
    {
        // i. Switch to STOPPING, interrupt thread
        // ii. Wait until thread ends
        // iii. Clear the task queue, switch to STOPPED,
        synchronized (this)
        {
            switch (thread_state)
            {
                case RUN:
                    _stopping();
                    break;
                case SUSPEND:
                    _stop();
                    return;
                case STOPPING:
                    return;
                case STOP:
                    return;
                case DISPOSED:
                    return;
            }
            thread.interrupt();
        }
        thread.join();
        synchronized (this)
        {
            queue.Clear();
            _stop();
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    public void dispose() throws InterruptedException
    {
        Thread tmp = null;
        synchronized (this)
        {
            if (thread_state == State.DISPOSED)
            {
                return;
            }

            tmp = thread;
            thread_state = State.DISPOSED;
            thread = null;
            if (tmp != null)
            {
                tmp.interrupt();
            }
        }
        if (tmp != null)
        {
            tmp.join();
            queue.Clear();
        }
    }
}
