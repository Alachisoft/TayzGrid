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
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

/**
 * A class that helps doing things that can be done asynchronously
 */
public class AsyncProcessor implements Runnable
{

    /**
     * Interface to be implemented by all async events.
     */
    public interface IAsyncTask
    {

        /**
         * Process itself.
         * @throws Alachisoft.NCache.Cluster.ChannelClosedException
         */
        void Process() throws OperationFailedException, java.io.IOException,CacheException,LockingException;
    }
    /**
     * The worker thread.
     */
    /**
     * The queue of events.
     */
    private java.util.LinkedList _eventsHi, _eventsLow;
    private ILogger NCacheLog;
    
    private boolean _started;
    private int _numProcessingThreads = 1;
    private Thread[] _workerThreads;
    
    Boolean _isShutdown = false;
    Object _shutdownMutex = new Object();

    /**
     * Constructor
     * @param NCacheLog Logger
     */
    public AsyncProcessor(ILogger NCacheLog)
    {
        this();
        this.NCacheLog = NCacheLog;
    }

    public AsyncProcessor()
    {
        this(1);
    }
    
    public AsyncProcessor(int numProcessingThread)
    {
        if(numProcessingThread < 1)
            numProcessingThread = 1;
        
        _workerThreads = null;
        _numProcessingThreads = numProcessingThread;
        _eventsHi = new java.util.LinkedList();
        _eventsLow = new java.util.LinkedList();
    }
    
    public final void WindUpTask()
    {
            NCacheLog.CriticalInfo("AsyncProcessor", "WindUp Task Started.");

            if (_eventsHi != null)
            {
                    NCacheLog.CriticalInfo("AsyncProcessor", "Async operation(s) Queue Count: " + _eventsHi.size());
            }

            _isShutdown = true;
            synchronized (this)
            {
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);
            }
            NCacheLog.CriticalInfo("AsyncProcessor", "WindUp Task Ended.");
    }

    public final void WaitForShutDown(long interval)
    {
            NCacheLog.CriticalInfo("AsyncProcessor", "Waiting for  async process queue shutdown task completion.");
            if (interval > 0)
            {
                    boolean waitlock = false;
                    synchronized (_shutdownMutex)
                    {
                            if (_eventsHi.size() > 0)
                            {
                                try
                                {
                                    waitlock = com.alachisoft.tayzgrid.common.threading.Monitor.wait(_shutdownMutex, (int)((interval-1) * 1000));
                                } 
                                catch(Exception ex)
                                {
                                     NCacheLog.Error("AsyncProcessor", "Asyncronous operations has intruppted. " + ex.getMessage());
                                }
                            }
                    }

                    if (!waitlock)
                    {
                            if (_eventsHi.size() > 0)
                            {
                                    NCacheLog.CriticalInfo("AsyncProcessor", "Remaining Async operations in queue: " + _eventsHi.size());
                            }
                    }
            }

       NCacheLog.CriticalInfo("AsyncProcessor", "Shutdown task completed.");
    }
    

    /**
     * Add a low priority event to the event queue
     *
     * @param evnt event
     */
    public final void Enqueue(IAsyncTask evnt)
    {
        synchronized (this)
        {
            _eventsHi.offer(evnt);
            //: Monitor.Pulse(this) changed to this.notify()
            //Monitor.Pulse(this);
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);// this.notifyAll();
        }
    }

    /**
     * Add a low priority event to the event queue
     *
     * @param evnt event
     */
    public final void EnqueueLowPriority(IAsyncTask evnt)
    {
        synchronized (this)
        {
            _eventsLow.offer(evnt);
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);//this.notify();
        }
    }

    /**
     * Start processing
     */
    public final void Start() {
        try {
            synchronized (this) {
                if (!_started) {
                    _workerThreads = new Thread[_numProcessingThreads];
                    _started = true;

                    for (int i=0; i< _workerThreads.length; i++) {
                        Thread tThread = new Thread(this);
                        tThread.setDaemon(true);
                        tThread.setName("AsyncProcessor");
                        tThread.start();
                        _workerThreads[i] = tThread;
                    }

                }
            }
        } catch (Exception ex) {
            if (NCacheLog != null) {
                NCacheLog.Error("AsyncProcessor.Start)_", ex.getMessage());
            }
        }
    }

    /**
     * Stop processing.
     */
    public final void Stop()
    {
        synchronized (this)
        {
            for(Thread tThread : _workerThreads)
            {
                if(tThread != null && tThread.isAlive())
                {
                    if (NCacheLog != null)
                    {
                        NCacheLog.Flush();
                    }
                    tThread.interrupt();
                    tThread = null;
                }
            }
        }
    }

    /**
     * Thread function, keeps running.
     */
    @Override
    public void run()
    {
        while (_workerThreads != null && !Thread.currentThread().isInterrupted())
        {
            IAsyncTask evnt = null;
            try
            {
                synchronized (this)
                {
                    if ((_eventsHi.size() < 1) && (_eventsLow.size() < 1) && !_isShutdown)
                    {
                       
                            com.alachisoft.tayzgrid.common.threading.Monitor.wait(this);//this.wait();
                        
                    }
                    
                    if ((_eventsHi.size() < 1) && _isShutdown)
                    {
                            synchronized (_shutdownMutex)
                            {
                               com.alachisoft.tayzgrid.common.threading.Monitor.pulse(_shutdownMutex);
                               break;
                            }
                    }

                    if (_eventsHi.size() > 0)
                    {
                        evnt = (IAsyncTask) _eventsHi.poll();
                    }
                    else if (_eventsLow.size() > 0)
                    {
                        evnt = (IAsyncTask) _eventsLow.poll();
                    }
                }
                
                if (evnt == null && _eventsHi.size() < 1 && _isShutdown)
                {
                        synchronized (_shutdownMutex)
                        {
                                 com.alachisoft.tayzgrid.common.threading.Monitor.pulse(_shutdownMutex);
                                break;
                        }
                }
                
                if (evnt == null)
                {
                    continue;
                }
                evnt.Process();
            }
            catch (NullPointerException e)
            {
            }
            catch(InterruptedException e){
                break;
            }
            catch (Exception e)
            {
                String exceptionString = e.toString();
                if (!exceptionString.equals("ChannelNotConnectedException") && !exceptionString.equals("ChannelClosedException"))
                {
                    if (NCacheLog != null)
                    {
                        NCacheLog.Error("AsyncProcessor.Run()", exceptionString);

                    }
                }
            }
        }
    }
}
