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


import java.lang.Thread;


/**
 * Support class used to handle threads
 */
public class ThreadClass implements Runnable
{

    /**
     * The instance of Thread
     * Don't start the default thread
     */
    private Thread threadField;

    /**
     * Initializes a new instance of the ThreadClass class
     * Don't start the default thread
     */
    public ThreadClass()
    {
        threadField = new Thread(this);
    }

    /**
     * Initializes a new instance of the Thread class.
     *
     * @param Name The name of the thread
     */
    public ThreadClass(String Name)
    {
        threadField = new Thread(this);
        this.setName(Name);
    }

    /**
     * Initializes a new instance of the Thread class.
     *
     * @param Start A ThreadStart delegate that references the methods to be invoked when this thread begins executing
     */
    public ThreadClass(Runnable Start)
    {
        threadField = new Thread(Start);
    }

    /**
     * Initializes a new instance of the Thread class.
     *
     * @param Start A ThreadStart delegate that references the methods to be invoked when this thread begins executing
     * @param Name The name of the thread
     */
    public ThreadClass(Runnable Start, String Name)
    {
        threadField = new Thread(Start);
        this.setName(Name);
    }

    /**
     * This method has no functionality unless the method is overridden
     */
    @Override
    public void run()
    {
    }

    /**
     * Causes the operating system to change the state of the current thread instance to ThreadState.Running
     */
    public void Start()
    {
        threadField.start();
    }

    /**
     * Interrupts a thread that is in the WaitSleepJoin thread state
     */
    public void Interrupt()
    {
        threadField.interrupt();
    }

    /**
     * Gets or sets the name of the thread
     */
    public final String getName()
    {
        return threadField.getName();
    }

    public final void setName(String value)
    {
        if (threadField.getName() == null)
        {
            threadField.setName(value);
        }
    }

    /**
     * Gets or sets a value indicating the scheduling priority of a thread
     */
    public final int getPriority()
    {
        return threadField.getPriority();
    }

    public final void setPriority(int value)
    {
        threadField.setPriority(value);
    }

    /**
     * Gets a value indicating the execution status of the current thread
     */
    public final boolean getIsAlive()
    {
        return threadField.isAlive();
    }

    /**
     * Gets a value indicating the execution status of the current thread
     */
    public final boolean getIsInterrupted()
    {
        //return (threadField.ThreadState & ThreadState.WaitSleepJoin) == ThreadState.WaitSleepJoin;
        return threadField.isInterrupted();
    }

    /**
     * Gets or sets a value indicating whether or not a thread is a background thread.
     */
    public final boolean getIsBackground()
    {
        return threadField.isDaemon();
    }

    public final void setIsBackground(boolean value)
    {
        threadField.setDaemon(value);
    }

    /**
     * Blocks the calling thread until a thread terminates
     * @throws InterruptedException
     */
    public final void Join() throws InterruptedException
    {
        threadField.join();
    }

    /**
     * Blocks the calling thread until a thread terminates or the specified time elapses
     *
     * @param MiliSeconds Time of wait in milliseconds
     * @throws InterruptedException
     */
    public final void Join(long MiliSeconds) throws InterruptedException
    {
        synchronized (this)
        {
            threadField.join(MiliSeconds);
        }
    }

    /**
     * Blocks the calling thread until a thread terminates or the specified time elapses
     *
     * @param MiliSeconds Time of wait in milliseconds
     * @param NanoSeconds Time of wait in nanoseconds
     * @throws InterruptedException
     */
    public final void Join(long MiliSeconds, int NanoSeconds) throws InterruptedException
    {
        synchronized (this)
        {
            threadField.join(MiliSeconds, NanoSeconds);
        }
    }

    /**
     * Resumes a thread that has been suspended
     */
    public final void Resume()
    {
        threadField.resume();
    }

    /**
     * Raises a ThreadAbortException in the thread on which it is invoked, to begin the process of terminating the thread. Calling this method usually terminates the thread
     */
    public final void Abort()
    {
        threadField.stop();
    }

    /**
     * Raises a ThreadAbortException in the thread on which it is invoked, to begin the process of terminating the thread while also providing exception information about the
     * thread termination. Calling this method usually terminates the thread.
     *
     * @param stateInfo An object that contains application-specific information, such as state, which can be used by the thread being aborted
     * @deprecated Abort necessarily interrupts the thread making it do what ever is required by setting a volatile boolean as true that triggers the information to abort thread
     */
    @Deprecated
    public final void Abort(Object stateInfo)
    {
        synchronized (this)
        {
            
            threadField.interrupt();
        }
    }

    /**
     * Suspends the thread, if the thread is already suspended it has no effect
     */
    public final void Suspend()
    {
        threadField.suspend();
    }

    /**
     * Obtain a String that represents the current Object
     *
     * @return A String that represents the current Object
     */
    @Override
    public String toString()
    {
        return "Thread[" + this.getName() + "," + Integer.toString(this.getPriority()) + "," + "" + "]";
    }

    /**
     * Gets the currently running thread
     *
     * @return The currently running thread
     */
    public static ThreadClass Current()
    {
        ThreadClass CurrentThread = new ThreadClass();
        CurrentThread.threadField = Thread.currentThread();
        return CurrentThread;
    }
}
