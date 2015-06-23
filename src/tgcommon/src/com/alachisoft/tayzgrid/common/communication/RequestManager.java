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

package com.alachisoft.tayzgrid.common.communication;

import com.alachisoft.tayzgrid.common.communication.exceptions.ChannelException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

public class RequestManager implements IChannelEventListener, IDisposable
{

    private IChannel _channel;
    private java.util.HashMap _requests = new java.util.HashMap();
    private Object _lock = new Object();
    private long _lastRequestId;
    private boolean _resendRequestOnChannelDisconnect = true;
    private int _requestTimeout = 90 *1000; //default is ninety second

    public RequestManager(IChannel chnnel)
    {
        if (chnnel == null)
        {
            throw new IllegalArgumentException("channel");
        }

        _channel = chnnel;
        _channel.RegisterEventListener(this);
    }
    
   public final int getRequestTimedout()
   {
	  return _requestTimeout;
   }
   public final void setRequestTimedout(int value)
   {
	  _requestTimeout = value;
   }

    public final Object SendRequest(IRequest request) throws TimeoutException, UnsupportedEncodingException, InterruptedException
    {
        Object response = null;

        request.setRequestId(GenerateRequestId());
        RequestResponsePair reqRespPair = new RequestResponsePair();

        synchronized (_lock)
        {
            reqRespPair.setRequest(request);

            if (!_requests.containsKey(request.getRequestId()))
            {
                _requests.put(request.getRequestId(), reqRespPair);
            }
        }

        synchronized (reqRespPair)
        {
            try
            {
                _channel.SendMessage(request);
                reqRespPair.setRequestSentOverChannel(true);
                Monitor.wait(reqRespPair, _requestTimeout); 
            }
            catch (ChannelException e)
            {
                throw e;
            }
            finally
            {
                synchronized (_lock)
                {
                    _requests.remove(request.getRequestId());
                }
            }
        }

        if (!reqRespPair.getLockAccquired())
        {
            throw new TimeoutException();
        }
        reqRespPair.setLockAccquired(false);

        if (reqRespPair.getChannelException() != null)
        {
            throw reqRespPair.getChannelException();
        }

        response = reqRespPair.getResponse();

        return response;
    }

    private long GenerateRequestId()
    {
        synchronized (this)
        {
            long requestId = ++_lastRequestId;
            if (requestId < 0)
            {
                _lastRequestId = 0;
                requestId = 0;
            }
            return requestId;
        }
    }

    public final void ReceiveResponse(IResponse response)
    {
        IResponse protoResponse = response;
        RequestResponsePair reqResponsePair = (RequestResponsePair) ((_requests.get(protoResponse.getRequestId()) instanceof RequestResponsePair) ? _requests.get(protoResponse.getRequestId()) : null);

        synchronized (reqResponsePair)
        {

            if (reqResponsePair != null)
            {
                reqResponsePair.setResponse(protoResponse);
                reqResponsePair.setLockAccquired(true);
                Monitor.pulse(reqResponsePair); 
            }

        }
    }

    public final void ChannelDisconnected(String reason) throws UnsupportedEncodingException
    {
        synchronized (_lock)
        {
            Object tempVar = _requests.clone();
            java.util.HashMap requestClone = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
            Iterator ide = requestClone.entrySet().iterator();

            while (ide.hasNext())
            {
                Map.Entry current = (Map.Entry) ide.next();
                RequestResponsePair reqRspPair = (RequestResponsePair) ((current.getValue() instanceof RequestResponsePair) ? current.getValue() : null);

                if (!reqRspPair.getRequestSentOverChannel())
                {
                    continue;
                }

                synchronized (reqRspPair)
                {
                    if (_resendRequestOnChannelDisconnect)
                    {
                        //resend the request when channel is disconnected
                        try
                        {
                            if (_channel != null)
                            {
                                _channel.SendMessage(reqRspPair.getRequest());
                            }
                        }
                        catch (ChannelException ce)
                        {
                            reqRspPair.setChannelException(ce);
                            Monitor.pulse(reqRspPair);
                        }
                    }
                    else
                    {
                        reqRspPair.setChannelException(new ChannelException(reason));
                        //System.Threading.Monitor.PulseAll(reqRspPair);
                        Monitor.pulse(reqRspPair);//reqRspPair.notifyAll();
                    }
                }
            }
        }
    }

    public final void dispose()
    {
        try
        {
            synchronized (_lock)
            {
                _requests.clear();
            }

            if (_channel != null)
            {
                _channel.Disconnect();
                _channel = null;
            }

        }
        catch (Exception e)
        {
        }
    }

    protected void finalize() throws Throwable
    {
        dispose();
    }
}
