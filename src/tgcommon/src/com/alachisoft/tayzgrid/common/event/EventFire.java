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
package com.alachisoft.tayzgrid.common.event;

import com.alachisoft.tayzgrid.common.logger.ILogger;
import java.net.SocketException;

class EventFire implements Runnable
{

    private NEventStart caller;
    private NEventEnd callback;
    private Object[] args;
    private ILogger _ncacheLog;
    private String _name;
    private boolean removeSubscriberOnException;
    private NEvent source;

    EventFire(NEventStart caller, NEventEnd callback, ILogger ncacheLog, NEvent source, Object[] args)
    {
        if (source != null)
        {
            _name = source.getName();
        }

        _ncacheLog = ncacheLog;
        this.caller = caller;
        this.callback = callback;
        this.args = args;
    }

    @Override
    public void run()
    {
        Exception exception = null;
        //<editor-fold defaultstate="collapsed" desc="caller">
        try
        {
            caller.hanleEvent(args);
        }
        catch (SocketException e)
        {
            exception = e;
            if (_ncacheLog != null)
            {
                _ncacheLog.Error("FireEvent.run.caller: " + _name, "Event# " + Thread.currentThread().getName() + "with Exception: \t" + e.getMessage());
            }

            if(removeSubscriberOnException)
                source.removeNEventListners(caller);
        }
        catch (Exception e)
        {
            exception = e;
            if (_ncacheLog != null)
            {
                _ncacheLog.Error("FireEvent.run.caller: " + _name, "Event# " + Thread.currentThread().getName() + "with Exception: \t" + e.getMessage());
            }
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="callback">
        if (callback != null)
        {
            try
            {
                callback.endEvent(exception, args);
            }
            catch (SocketException e)
            {
                exception = e;
                if (_ncacheLog != null)
                {
                    _ncacheLog.Error("FireEvent.run.caller: " + _name, "Event# " + Thread.currentThread().getName() + "with Exception: \t" + e.getMessage());
                }

                if (removeSubscriberOnException)
                {
                    source.removeNEventListners(caller);
                }
            }
            catch (Exception e)
            {
                if (_ncacheLog != null)
                {
                    _ncacheLog.Error("FireEvent.run.callback " + _name, "Event# " + Thread.currentThread().getName() + "with Exception: \t" + e.getMessage());
                }
            }
        }
        //</editor-fold>
    }



    void setRemoveSubscriberOnException(boolean removeSubscriberOnException)
    {
        this.removeSubscriberOnException = removeSubscriberOnException;
    }
}
