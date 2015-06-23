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

package com.alachisoft.tayzgrid.cluster.protocols.pbcast;

import com.alachisoft.tayzgrid.common.threading.Monitor;

public class ViewPromise
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
     * How many responses expected
     */
    private int _countExpected;
    /**
     * How many responses received
     */
    private int _countReceived;

    public ViewPromise(int count)
    {
        this._countExpected = count;
    }

    /**
     * If result was already submitted, returns it immediately, else blocks until results becomes available.
     *
     * @param timeout Maximum time to wait for result.
     * @return Promise result
     */
    public final Object WaitResult(long timeout) throws InterruptedException
    {
        Object ret = null;

        synchronized (_mutex)
        {
            if (_result != null && (_countExpected == _countReceived))
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
                catch (Exception ex)
                {
                     
                }
            }
            else
            {
                try
                {
                    Monitor.wait(_mutex,timeout); 
                }
                catch (Exception ex)
                {
                     
                }
            }

            
            if (_result != null && (_countExpected == _countReceived))
            {
                ret = _result;
                _result = null;
                return ret;
            }
            return null;
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
            _countReceived++;
            if (_countExpected == _countReceived)
            {
                Monitor.pulse(_mutex); 
            }
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
        return "result=" + _result + " countReceived=" + _countReceived + " countExpected=" + _countExpected;
    }

    /**
     * Checks whether all the nodes responded
     */
    public final boolean AllResultsReceived()
    {
        return _countExpected == _countReceived;
    }
}
