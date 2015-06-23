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

package com.alachisoft.tayzgrid.common.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReaderWriterLock
{
    //setting ReentrantReadWRiteLock to fair usage(true) so it may behave like .NET ReaderWriterLock
    //This will allow to operate the locks in the followin manner
    //A thread that tries to acquire a fair read lock (non-reentrantly) will block if either the write lock is held, or there is a waiting writer
    //thread. The thread will not acquire the read lock until after the oldest currently waiting writer thread has acquired and released the write lock. Of course, if a waiting writer abandons its wait, leaving one or more reader threads as the longest waiters in the queue with the write lock free, then those readers will be assigned the read lock.
    //A thread that tries to acquire a fair write lock (non-reentrantly) will block unless both the read lock and write lock are free
    //Internally it initiates a ReadLock and WriteLock

    ReentrantReadWriteLock _syncObj = new ReentrantReadWriteLock(true);

    /**
     * If the current thread holds the writer lock
     * @return true if the current thread holds the writer lock; otherwise, false.
     */
    public boolean IsWriterLockHeld()
    {
        return _syncObj.isWriteLockedByCurrentThread();
    }

    /**
     * AcquireReaderLock supports recursive reader-lock requests. That is, a thread can call AcquireReaderLock multiple times, which increments the lock count each time.
     * You must call ReleaseReaderLock once for each time you call AcquireReaderLock. Alternatively, you can call ReleaseLock to reduce the lock count to zero immediately.
     * Recursive lock requests are always granted immediately, without placing the requesting thread in the reader queue. Use recursive locks with caution, to avoid blocking
     * writer-lock requests for long periods.
     *
     * Takes non-interruptible Lock
     *
     * @param timeOut timeOut to acquiring lock
     */
    public void AcquireReaderLock()
    {
        _syncObj.readLock().lock();
    }

    /**
     * Decrements the lock count.
     */
    public void ReleaseReaderLock()
    {
        _syncObj.readLock().unlock();
    }


    /**
     *  AcquireWriterLock supports recursive writer-lock requests. That is, a thread can call AcquireWriterLock multiple times, which increments the lock count each time.
     * You must call ReleaseWriterLock once for each time you call AcquireWriterLock. Alternatively, you can call ReleaseLock to reduce the lock count to zero immediately.
     */
    public void AcquireWriterLock()
    {
        _syncObj.writeLock().lock();
    }


    /**
     * Decrements the lock count.
     */
    public void ReleaseWriterLock()
    {
        _syncObj.writeLock().unlock();
    }

}
