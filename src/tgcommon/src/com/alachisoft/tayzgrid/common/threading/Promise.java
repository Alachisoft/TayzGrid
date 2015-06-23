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

/**
 * The caller may choose to check for the result at a later time, or immediately and it may block or not. Both the caller and responder have to know the promise.
 *
 *
 * Allows a thread to submit an asynchronous request and to wait for the result. <p><b>Author:</b> Chris Koiak, Bela Ban</p> <p><b>Date:</b> 12/03/2003</p>
 */
public class Promise
{

    /**
     * The result of the request
     */
    private Object _result = null;
    /**
     * Used to wait on the result
     */
    private Object _mutex = new Object();

    /**
     * If result was already submitted, returns it immediately, else blocks until results becomes available.      *
     * @param timeout Maximum time to wait for result.
     * @return Promise result
     */
    public final Object WaitResult(long timeout)
    {
        Object ret = null;

        synchronized (_mutex)
        {
            if (_result != null)
            {
                ret = _result;
                _result = null;
                return ret;
            }
            if (timeout <= 0)
            {
                try
                {
                    Monitor.wait(_mutex); 
                }
                catch (java.lang.InterruptedException ex)
                {
                }
            }
            else
            {
                try
                {
                    Monitor.wait(_mutex,timeout);// _mutex.wait(timeout);
                }
                catch (java.lang.InterruptedException ex)
                {
                }
            }

            if (_result != null)
            {
                ret = _result;
                _result = null;
                return ret;
            }
            return null;
        }
    }

    /**
     * Checks whether result is available. Does not block.
     *
     * @return Result if available
     */
    public final Object getIsResultAvailable()
    {
        synchronized (_mutex)
        {
            return _result;
        }
    }

    /**
     * Sets the result and notifies any threads waiting for it
     *
     * @param obj Result of request
     */
    public final void SetResult(Object obj)
    {
        synchronized (_mutex)
        {
            _result = obj;
            Monitor.pulse(_mutex); //_mutex.notifyAll();
        }
    }

    /**
     * Clears the result and causes all waiting threads to return
     */
    public final void Reset()
    {
        synchronized (_mutex)
        {
            _result = null;
            Monitor.pulse(_mutex);
        }
    }

    /**
     * String representation of the result
     *
     * @return String representation of the result
     */
    @Override
    public String toString()
    {
        return "result=" + _result;
    }
}
