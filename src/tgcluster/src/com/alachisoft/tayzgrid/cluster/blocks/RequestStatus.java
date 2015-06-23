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

package com.alachisoft.tayzgrid.cluster.blocks;

import java.io.Serializable;

// $Id: RequestCorrelator.java,v 1.12 2004/09/05 04:54:21 ovidiuf Exp $

public class RequestStatus implements Serializable
{


    public static final byte REQ_NOT_RECEIVED = 1;

    public static final byte REQ_RECEIVED_NOT_PROCESSED = 2;

    public static final byte REQ_PROCESSED = 4;

    public static final byte NONE = 8;
 
    public static final long CLEAN_TIME = 15000L;

    private long _reqId;

    private byte _status = REQ_NOT_RECEIVED;


    private long _creationTime = 0; // start time

    public RequestStatus(long reqId)
    {
        _reqId = reqId;
   
        _creationTime = System.currentTimeMillis();
    }


    public RequestStatus(long reqId, byte status)
    {
        this(reqId);
        _status = status;
    }

    /**
     * Gets the request id.
     */
    public final long getReqId()
    {
        return _reqId;
    }

    /**
     * Gets the status of the request. Following can be the status RequestStatus.REQ_NOT_RECEIVED -> request not received at this node RequestStatus.REQ_RECEIVED_NOT_PROCESSED ->
     * request received but not yet processed. RequestStatus.REQ_PROCESSED -> request received,processed and response sent.
     */

    public final byte getStatus()
    {
        return _status;
    }

    /**
     * Marks the request as arrived.
     */
    public final void MarkReceived()
    {
        _status = REQ_RECEIVED_NOT_PROCESSED;
    }

    /**
     * Marks the request as processd.
     */
    public final void MarkProcessed()
    {
        _status = REQ_PROCESSED;
    }

    public final boolean HasExpired()
    {
        if (_creationTime != 0)
        {
            //: TimeSpan changed to System.currentTimeMillis
            long timePassed = System.currentTimeMillis() - _creationTime;
            if (timePassed >= CLEAN_TIME)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("RequestStatus[req_id=" + _reqId + ";status=");

        switch (_status)
        {
            case REQ_NOT_RECEIVED:
                sb.append("REQ_NOT_RECEIVED");
                break;
            case REQ_PROCESSED:
                sb.append("REQ_PROCESSED");
                break;
            case REQ_RECEIVED_NOT_PROCESSED:
                sb.append("REQ_RECEIVED_NOT_PROCESSED");
                break;
            case NONE:
                sb.append("NONE");
                break;
        }
        return sb.toString();
    }
}
