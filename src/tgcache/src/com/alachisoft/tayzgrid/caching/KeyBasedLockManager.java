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

package com.alachisoft.tayzgrid.caching;

public class KeyBasedLockManager
{
    private static class LockInfo
    {
        private java.util.LinkedList _queue = new java.util.LinkedList();
        private boolean _lastPulsed;

        public final boolean AddWaitingThread() throws InterruptedException
        {
            Object syncObject = new Object();
            synchronized (syncObject)
            {
                synchronized (_queue)
                {
                    if (_lastPulsed)
                    {
                        return false;
                    }
                    _queue.offer(syncObject);
                }
                syncObject.wait();
            }
            return true;
        }

        /**
         * Pulse any thread waiting for queue.
         *
         * @return true if thread was waiting otherwise false
         */
        public final boolean PulseWaitingThread()
        {
            Object syncObject = null;
            synchronized (_queue)
            {
                if (_queue.isEmpty())
                {
                    _lastPulsed = true;
                    return false;
                }
                syncObject = _queue.poll();
            }

            synchronized (syncObject)
            {
                syncObject.notify();
            }
            return true;
        }

        public final int getCount()
        {
            return _queue.size();
        }
    }

    private static class LockingContext
    {

        private Thread _currentThread;
        private int _refCount;

        public LockingContext()
        {
            _currentThread = Thread.currentThread();
        }

        public final boolean getIsCurrentContext()
        {
            boolean isCurrentThread = _currentThread.equals(Thread.currentThread());
            return isCurrentThread;
        }

        public final void IncrementRefCount()
        {
            _refCount++;
        }

        public final boolean DecrementRefCount()
        {
            _refCount--;
            return _refCount == 0 ? true : false;
        }
    }
    
    private java.util.HashMap _lockTable = new java.util.HashMap();
    private Object _sync_mutex = new Object();
    private boolean _globalLock;
    private boolean _waiting4globalLock;
    private LockInfo _globalLockInfo;
    private LockingContext _globalLockingContext;

    public final void AcquireLock(Object key) throws InterruptedException
    {
        LockInfo info = null;
        synchronized (_sync_mutex)
        {
            while (_globalLock || _waiting4globalLock)
            {
                if (_globalLock && _globalLockingContext != null && _globalLockingContext.getIsCurrentContext())
                {
                    break;
                }
                _sync_mutex.wait();
            }

            if (!_lockTable.containsKey(key))
            {
                _lockTable.put(key, new LockInfo());
            }
            else
            {
                info = (LockInfo) ((_lockTable.get(key) instanceof LockInfo) ? _lockTable.get(key) : null);

            }
        }
        if (info != null)
        {
            boolean lockAcquired = info.AddWaitingThread();
            //retry
            if (!lockAcquired)
            {
                AcquireLock(key);
            }
        }
    }

    public final void ReleaseLock(Object key)
    {
        synchronized (_sync_mutex)
        {
            if (_lockTable.containsKey(key))
            {
                LockInfo info = (LockInfo) ((_lockTable.get(key) instanceof LockInfo) ? _lockTable.get(key) : null);
                if (!info.PulseWaitingThread())
                {
                    _lockTable.remove(key);
                }
            }
            if (_waiting4globalLock && _lockTable.isEmpty())
            {
                _globalLock = true;
                _waiting4globalLock = false;
                _globalLockInfo.PulseWaitingThread();
            }
        }
    }

    public final void AcquireGlobalLock() throws InterruptedException
    {
        synchronized (_sync_mutex)
        {
            //wait untill global lock is released.
            if (_globalLockingContext != null && _globalLockingContext.getIsCurrentContext())
            {
                _globalLockingContext.IncrementRefCount();
                return;
            }
            while (_globalLock || _waiting4globalLock)
            {
                _sync_mutex.wait();
            }

            if (_lockTable.isEmpty())
            {
                //global lock is acquired.
                _globalLock = true;
            }
            else
            {
                _waiting4globalLock = true;
                _globalLockInfo = new LockInfo();
            }
        }

        if (_globalLockInfo != null && !_globalLock)
        {
            _globalLockInfo.AddWaitingThread();
        }

        _globalLockingContext = new LockingContext();
        _globalLockingContext.IncrementRefCount();
    }

    public final void ReleaseGlobalLock()
    {
        synchronized (_sync_mutex)
        {
            if (_globalLockingContext.getIsCurrentContext() && _globalLockingContext.DecrementRefCount())
            {
                _globalLock = false;
                _globalLockInfo = null;
                _globalLockingContext = null;
                _sync_mutex.notifyAll();
            }
        }
    }
}
