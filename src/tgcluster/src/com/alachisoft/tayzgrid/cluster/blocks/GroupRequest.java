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

import com.alachisoft.tayzgrid.cluster.Transport;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.cluster.util.Command;
import com.alachisoft.tayzgrid.cluster.util.Rsp;
import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.threading.ThreadUtil;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

// $Id: GroupRequest.java,v 1.8 2004/09/05 04:54:22 ovidiuf Exp $

/**
 * Sends a message to all members of the group and waits for all responses (or timeout). Returns a boolean value (success or failure). Results (if any) can be retrieved when
 * _done.<p> The supported transport to send requests is currently either a RequestCorrelator or a generic Transport. One of them has to be given in the constructor. It will then
 * be used to send a request. When a message is received by either one, the receiveResponse() of this class has to be called (this class does not actively receive
 * requests/responses itself). Also, when a view change or suspicion is received, the methods viewChange() or suspect() of this class have to be called.<p> When started, an array
 * of responses, correlating to the membership, is created. Each response is added to the corresponding field in the array. When all fields have been set, the algorithm terminates.
 * This algorithm can optionally use a suspicion service (failure detector) to detect (and exclude from the membership) fauly members. If no suspicion service is available,
 * timeouts can be used instead (see
 * <code>execute()</code>). When _done, a list of suspected members can be retrieved.<p> Because a channel might deliver requests, and responses to <em>different</em> requests, the
 * <code>GroupRequest</code> class cannot itself receive and process requests/responses from the channel. A mechanism outside this class has to do this; it has to determine what
 * the responses are for the message sent by the
 * <code>execute()</code> method and call
 * <code>receiveResponse()</code> to do so.<p> <b>Requirements</b>: lossless delivery, e.g. acknowledgment-based message confirmation.
 *
 * <author> Bela Ban </author> <version> $Revision: 1.8 $ </version>
 */
public class GroupRequest implements RspCollector, Command
{

    /**
     * Returns the results as a RspList
     */

    public RspList getResults()
    {
        RspList retval = new RspList();
        Address sender;
        synchronized (rsp_mutex)
        {
            for (int i = 0; i < membership.length; i++)
            {
                sender = membership[i];
                switch (received[i])
                {

                    case SUSPECTED:
                        retval.addSuspect(sender);
                        break;

                    case RECEIVED:
                        retval.addRsp(sender, responses[i]);
                        break;

                    case NOT_RECEIVED:
                        retval.addNotReceived(sender);
                        break;
                }
            }

            return retval;
        }
    }


    public int getNumSuspects()
    {
        return suspects.size();
    }

    public java.util.List getSuspects()
    {
        return suspects;
    }

    public boolean getDone()
    {
        return _done;
    }

    /**
     * Generates a new unique request ID
     */
    private static long getRequestId()
    {
        synchronized (req_mutex)
        {
            
            if (last_req_id == Long.MAX_VALUE)
            {
                last_req_id = -1;
            }
            long result = ++last_req_id;
            return result;
        }
    }

    public final void AddNHop(Address sender)
    {

        synchronized (_nhopMutex)
        {
            expectedNHopResponses++;

            if (!nHops.contains(sender))
            {
                nHops.add(sender);
            }
        }

    }

    public final void AddNHopDefaultStatus(Address sender)
    {
        
        if (!receivedFromNHops.containsKey(sender))
        {
            receivedFromNHops.put(sender, NOT_RECEIVED);
        }

    }

    protected boolean getResponses()
    {
        int num_not_received = getNum(NOT_RECEIVED);
        int num_received = getNum(RECEIVED);
        int num_suspected = getNum(SUSPECTED);
        int num_total = membership.length;

        int num_receivedFromNHops = getNumFromNHops(RECEIVED);
        int num_suspectedNHops = getNumFromNHops(SUSPECTED);
        int num_okResponsesFromNHops = num_receivedFromNHops + num_suspectedNHops;

        switch (rsp_mode)
        {
            case GET_FIRST:
                if (num_received > 0)
                {
                    return true;
                }
                if (num_suspected >= num_total)
                // e.g. 2 members, and both suspected
                {
                    return true;
                }
                break;

            case GET_FIRST_NHOP:
                if (num_received > 0 && num_okResponsesFromNHops == expectedNHopResponses)
                {
                    return true;
                }
                if (num_suspected >= num_total)
                {
                    return true;
                }
                break;

            case GET_ALL:
                if (num_not_received > 0)
                {
                    return false;
                }
                return true;

            case GET_ALL_NHOP:
                if (num_not_received > 0)
                {
                    return false;
                }
                if (num_okResponsesFromNHops < expectedNHopResponses)
                {
                    return false;
                }

                return true;

            case GET_N:
                if (expected_mbrs >= num_total)
                {
                    rsp_mode = GET_ALL;
                    return getResponses();
                }
                if (num_received >= expected_mbrs)
                {
                    return true;
                }
                if (num_received + num_not_received < expected_mbrs)
                {
                    if (num_received + num_suspected >= expected_mbrs)
                    {
                        return true;
                    }
                    return false;
                }
                return false;

            case GET_NONE:
                return true;

            default:
                getCacheLog().Error("rsp_mode " + rsp_mode + " unknown !");
                break;

        }
        return false;
    }
    
    public static final byte GET_FIRST = 1;
 
    public static final byte GET_ALL = 2;
    
    public static final byte GET_N = 3;
   
    public static final byte GET_NONE = 4;
 
    public static final byte GET_FIRST_NHOP = 5;

    public static final byte GET_ALL_NHOP = 6;

    private static final byte NOT_RECEIVED = 0;

    private static final byte RECEIVED = 1;

    private static final byte SUSPECTED = 2;
    private Address[] membership = null; // current membership
    private Object[] responses = null; // responses corresponding to membership

    private byte[] received = null; // status of response for each mbr (see above)
    private long[] timeStats = null; // responses corresponding to membership
    /**
     * replica nodes in the cluster from where we are expecting responses. Following is the detail of how it works. 1. In case of synchronous POR, when an operation is transferred
     * to main node through clustering layer, main node does the following: - a) it executes the operation on itself. b) it transfers the operation to its replica (the next hop).
     * c) it sends the response of this operation back and as part of this response, it informs the node that another response is expected from replica node (the next hop). 2. this
     * dictionary is filled with the replica addresses (next hop addresses) received as part of the response from main node along with the status (RECEIVED/NOT_RECEIVED...).
     */
    private java.util.HashMap<Address, Byte> receivedFromNHops = new java.util.HashMap<Address, Byte>();
    /**
     * list of next hop members.
     */
    //: List changed to ArrayList
    private ArrayList<Address> nHops = new ArrayList<Address>();
    
    /**
     * number of responses expected from next hops. When one node send requests to other node (NHop Request), the node may or may not send the same request to next hop depending on
     * the success/failure of the request on this node. this counter tells how many requests were sent to next hops and their responses are now expected.
     */
    private int expectedNHopResponses = 0;
    private Object _nhopMutex = new Object();
    /**
     * bounded queue of suspected members
     */
    private java.util.List suspects = Collections.synchronizedList(new java.util.ArrayList(10));
    /**
     * list of members, changed by viewChange()
     */
    private java.util.List members = Collections.synchronizedList(new java.util.ArrayList(10));
    /**
     * the list of all the current members in the cluster. this list is different from the members list of the Group Request which only contains the addresses of members to which
     * this group request must send the message. list of total membership is used to determine which member has been suspected after the new list of members is received through
     * view change event.
     */
    private java.util.List clusterMembership = Collections.synchronizedList(new java.util.ArrayList(10));
    /**
     * keep suspects vector bounded
     */
    private int max_suspects = 40;
    protected Message request_msg = null;
    protected RequestCorrelator corr = null; // either use RequestCorrelator or ...
    protected Transport transport = null; // Transport (one of them has to be non-null)

    protected byte rsp_mode = GET_ALL;
    private boolean _done = false;
    protected final Object rsp_mutex = new Object();
    protected long timeout = 0;
    protected int expected_mbrs = 0;
    /**
     * to generate unique request IDs (see getRequestId())
     */
    private static long last_req_id = -1;
    protected long req_id = -1; // request ID for this request
    private static Object req_mutex = new Object();
  
    private ILogger _ncacheLog;

    private ILogger getCacheLog()
    {
        return _ncacheLog;
    }
    private static boolean s_allowRequestEnquiry;
    private static int s_requestEnquiryInterval = 20;
    private static int s_requestEnquiryRetries = 1;
    private boolean _seqReset;
    private int _retriesAfteSeqReset;

    static
    {
        String str = ServicePropValues.CacheServer_AllowRequestEnquiry;
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(str))
        {
            s_allowRequestEnquiry = Boolean.parseBoolean(str);
        }
        if (s_allowRequestEnquiry)
        {
            str = ServicePropValues.CacheServer_RequestEnquiryInterval;
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(str))
            {
                s_requestEnquiryInterval = Integer.parseInt(str);
            }
            str = ServicePropValues.CacheServer_RequestEnquiryRetries;
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(str))
            {
                s_requestEnquiryRetries = Integer.parseInt(str);
                if (s_requestEnquiryRetries <= 0)
                {
                    s_requestEnquiryRetries = 1;
                }
            }
        }
    }

    /**
     * @param m The message to be sent
     *
     * @param corr The request correlator to be used. A request correlator sends requests tagged with a unique ID and notifies the sender when matching responses are received. The
     * reason
     * <code>GroupRequest</code> uses it instead of a
     * <code>Transport</code> is that multiple requests/responses might be sent/received concurrently.
     *
     * @param members The initial membership. This value reflects the membership to which the request is sent (and from which potential responses are expected). Is reset by
     * reset().
     *
     * @param rsp_mode How many responses are expected. Can be <ol> <li><code>GET_ALL</code>: wait for all responses from non-suspected members. A suspicion service might warn us
     * when a member from which a response is outstanding has crashed, so it can be excluded from the responses. If no suspision service is available, a timeout can be used (a
     * value of 0 means wait forever). <em>If a timeout of 0 is used, no suspicion service is available and a member from which we expect a response has crashed, this methods
     * blocks forever !</em>. <li><code>GET_FIRST</code>: wait for the first available response. <li><code>GET_MAJORITY</code>: wait for the majority of all responses. The majority
     * is re-computed when a member is suspected. <li><code>GET_ABS_MAJORITY</code>: wait for the majority of <em>all</em> members. This includes failed members, so it may block if
     * no timeout is specified. <li><code>GET_N</CODE>: wait for N members. Return if n is >= membership+suspects. <li><code>GET_NONE</code>: don't wait for any response.
     * Essentially send an asynchronous message to the group members. </ol>
     *
     */

    public GroupRequest(Message m, RequestCorrelator corr, java.util.List members, java.util.List clusterCompleteMembership, byte rsp_mode, ILogger NCacheLog)
    {
        request_msg = m;
        this.corr = corr;
        this.rsp_mode = rsp_mode;
        this._ncacheLog = NCacheLog;
        this.clusterMembership = clusterCompleteMembership;
        reset(members);
        
    }

    /**
     * @param timeout Time to wait for responses (ms). A value of <= 0 means wait indefinitely (e.g. if a suspicion service is available; timeouts are not needed).
     *
     */

    public GroupRequest(Message m, RequestCorrelator corr, java.util.List members, java.util.List clusterCompleteMembership, byte rsp_mode, long timeout, int expected_mbrs, ILogger NCacheLog)
    {
        this(m, corr, members, clusterCompleteMembership, rsp_mode, NCacheLog);
        if (timeout > 0)
        {
            this.timeout = timeout;
        }
        this.expected_mbrs = expected_mbrs;
    }


    public GroupRequest(Message m, Transport transport, java.util.List members, java.util.List clusterCompleteMembership, byte rsp_mode, ILogger NCacheLog)
    {
        request_msg = m;
        this.transport = transport;
        this.rsp_mode = rsp_mode;

        this._ncacheLog = NCacheLog;

        this.clusterMembership = clusterCompleteMembership;
        reset(members);
 
    }

    /**
     * @param timeout Time to wait for responses (ms). A value of <= 0 means wait indefinitely (e.g. if a suspicion service is available; timeouts are not needed).
     *
     */

    public GroupRequest(Message m, Transport transport, java.util.List members, java.util.List clusterCompleteMembership, byte rsp_mode, long timeout, int expected_mbrs, ILogger NCacheLog)
    {
        this(m, transport, members, clusterCompleteMembership, rsp_mode, NCacheLog);
        if (timeout > 0)
        {
            this.timeout = timeout;
        }
        this.expected_mbrs = expected_mbrs;
    }

    /**
     * Sends the message. Returns when n responses have been received, or a timeout has occurred. <em>n</em> can be the first response, all responses, or a majority of the
     * responses.
     */
    public boolean execute()
    {
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("GrpReq.Exec", "mode :" + rsp_mode);
        }

        boolean retval;
        if (corr == null && transport == null)
        {
            getCacheLog().Error("GroupRequest.execute()", "both corr and transport are null, cannot send group request");
            return false;
        }
        synchronized (rsp_mutex)
        {
            _done = false;
           
            retval = doExecute(timeout);
     
            if (retval == false)
            {
                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("GroupRequest.execute()", "call did not execute correctly, request is " + toString());
                }
            }
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("GrpReq.Exec", "exited; result:" + retval);
            }

            _done = true;
            return retval;
        }
    }

    /**
     * Resets the group request, so it can be reused for another execution.
     */

    public void reset(Message m, byte mode, long timeout)
    {
        synchronized (rsp_mutex)
        {
            _done = false;
            request_msg = m;
            rsp_mode = mode;
            this.timeout = timeout;
            Monitor.pulse(rsp_mutex);//rsp_mutex.notifyAll();
        }
    }


    public void reset(Message m, java.util.ArrayList members, byte rsp_mode, long timeout, int expected_rsps)
    {
        synchronized (rsp_mutex)
        {
            reset(m, rsp_mode, timeout);
            reset(members);
           
            this.expected_mbrs = expected_rsps;
            Monitor.pulse(rsp_mutex);//Monitor.pulse(rsp_mutex);//rsp_mutex.notifyAll();
        }
    }

    /**
     * This method sets the
     * <code>membership</code> variable to the value of
     * <code>members</code>. It requires that the caller already hold the
     * <code>rsp_mutex</code> lock.
     *
     * @param mbrs The new list of members
     *
     */
    public void reset(java.util.List mbrs)
    {
        if (mbrs != null)
        {
            int size = mbrs.size();
            membership = new Address[size];
            responses = new Object[size];
            received = new byte[size];
            timeStats = new long[size];
            for (int i = 0; i < size; i++)
            {
                membership[i] = (Address) mbrs.get(i);
                responses[i] = null;
                received[i] = NOT_RECEIVED;
                timeStats[i] = 0;
            }
          
            this.members.clear();
            this.members.addAll(mbrs);
        }
        else
        {
            if (membership != null)
            {
                for (int i = 0; i < membership.length; i++)
                {
                    responses[i] = null;
                    received[i] = NOT_RECEIVED;
                }
            }
        }
    }

    public final void SequenceReset()
    {
        synchronized (rsp_mutex)
        {
            _seqReset = true;
            _retriesAfteSeqReset = 0;
            Monitor.pulse(rsp_mutex);//rsp_mutex.notifyAll();
        }
    }
    /*
     * ---------------------- Interface RspCollector --------------------------
     */

    /**
     * <b>Callback</b> (called by RequestCorrelator or Transport). Adds a response to the response table. When all responses have been received,
     * <code>execute()</code> returns.
     */


    public void receiveResponse(Message m)
    {
        Address sender = m.getSrc(), mbr;
        Object val = null;
        if (_done)
        {
            getCacheLog().Warn("GroupRequest.receiveResponse()", "command is done; cannot add response !");
            return;
        }
        if (suspects != null && suspects.size() > 0 && suspects.contains(sender))
        {
            getCacheLog().Warn("GroupRequest.receiveResponse()", "received response from suspected member " + sender + "; discarding");
            return;
        }
        if (m.getLength() > 0)
        {
            try
            {
                if (m.responseExpected)
                {
                    OperationResponse opRes = new OperationResponse();
                    opRes.SerializablePayload = m.getFlatObject();
                    opRes.UserPayload = m.getPayload();
                    val = opRes;
                }
                else
                {
                    val = m.getFlatObject();
                }
            }
            catch (Exception e)
            {
                getCacheLog().Error("GroupRequest.receiveResponse()", "exception=" + e.getMessage());
            }
        }
       
        synchronized (rsp_mutex)
        {
            boolean isMainMember = false;
            for (int i = 0; i < membership.length; i++)
            {
                mbr = membership[i];
                if (mbr.equals(sender))
                {
                    isMainMember = true;

                    if (received[i] == NOT_RECEIVED)
                    {
                        responses[i] = val;
                        received[i] = RECEIVED;
                        if (getCacheLog().getIsInfoEnabled())
                        {
                            getCacheLog().CriticalInfo("GroupRequest.receiveResponse()", "received response for request " + req_id + ", sender=" + sender + ", val=" + val);
                        }
                        Monitor.pulse(rsp_mutex);// rsp_mutex.notifyAll(); // wakes up execute()
                        break;
                    }
                }
            }

            if (!isMainMember)
            {

                receivedFromNHops.put(sender, RECEIVED);
               
                Monitor.pulse(rsp_mutex);//rsp_mutex.notifyAll();

            }
        }
    }

    /**
     * <b>Callback</b> (called by RequestCorrelator or Transport). Report to
     * <code>GroupRequest</code> that a member is reported as faulty (suspected). This method would probably be called when getting a suspect message from a failure detector (where
     * available). It is used to exclude faulty members from the response list.
     */
    public void suspect(Address suspected_member)
    {
        Address mbr;
        boolean isMainMember = false;

        synchronized (rsp_mutex)
        {
            // modify 'suspects' and 'responses' array
            for (int i = 0; i < membership.length; i++)
            {
                mbr = membership[i];
                if (mbr.equals(suspected_member))
                {
                    isMainMember = true;
                    addSuspect(suspected_member);
                    responses[i] = null;
                    received[i] = SUSPECTED;
                    Monitor.pulse(rsp_mutex);//rsp_mutex.notifyAll();
                    break;
                }
            }

            if (!isMainMember)
            {

                if (clusterMembership != null && clusterMembership.contains(suspected_member))
                {
                    receivedFromNHops.put(suspected_member, SUSPECTED);
                }
                Monitor.pulse(rsp_mutex);//rsp_mutex.notifyAll();

            }
        }
        
    }

    /**
     * Any member of 'membership' that is not in the new view is flagged as SUSPECTED. Any member in the new view that is <em>not</em> in the membership (ie, the set of responses
     * expected for the current RPC) will <em>not</em> be added to it. If we did this we might run into the following problem: <ul> <li>Membership is {A,B} <li>A sends a
     * synchronous group RPC (which sleeps for 60 secs in the invocation handler) <li>C joins while A waits for responses from A and B <li>If this would generate a new view {A,B,C}
     * and if this expanded the response set to {A,B,C}, A would wait forever on C's response because C never received the request in the first place, therefore won't send a
     * response. </ul>
     *
     * @param new_view
     */
    @Override
    public void viewChange(View new_view)
    {
        Address mbr;
        java.util.List mbrs = new_view != null ? new_view.getMembers() : null;
        if (membership == null || membership.length == 0 || mbrs == null)
        {
            return;
        }

        synchronized (rsp_mutex)
        {
            Object tempVar = GenericCopier.DeepCopy(clusterMembership);//clusterMembership.clone();
            java.util.ArrayList oldMembership = clusterMembership != null ? (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null) : null;
            clusterMembership.clear();
            clusterMembership.addAll(mbrs);

            this.members.clear();
            this.members.addAll(mbrs);
            for (int i = 0; i < membership.length; i++)
            {
                mbr = membership[i];
                if (!mbrs.contains(mbr))
                {
                    addSuspect(mbr);
                    responses[i] = null;
                    received[i] = SUSPECTED;
                }

                if (oldMembership != null)
                {
                    oldMembership.remove(mbr);
                }
            }

            //by this time, membershipClone cotains all those members that are not part of
            //group request normal membership and are no longer part of the cluster membership
            //according to the new view.
            //this way we are suspecting replica members.
            if (oldMembership != null)
            {
                for (Iterator it = oldMembership.iterator(); it.hasNext();)
                {
                    Address member = (Address) it.next();
                    if (!mbrs.contains(member))
                    {
                        receivedFromNHops.put(member, SUSPECTED);
                    }
                }
            }

            Monitor.pulse(rsp_mutex);//rsp_mutex.notifyAll();
        }
    }


    /*
     * -------------------- End of Interface RspCollector -----------------------------------
     */
    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder();
        ret.append("[GroupRequest:\n");
        ret.append("req_id=").append(req_id).append('\n');
        ret.append("members: ");
        for (int i = 0; i < membership.length; i++)
        {
            ret.append(membership[i] + " ");
        }
        ret.append("\nresponses: ");
        for (int i = 0; i < responses.length; i++)
        {
            ret.append(responses[i] + " ");
        }
        if (suspects.size() > 0)
        {
            ret.append("\nsuspects: " + Global.CollectionToString(suspects));
        }
        ret.append("\nrequest_msg: " + request_msg);
        ret.append("\nrsp_mode: " + rsp_mode);
        ret.append("\ndone: " + _done);
        ret.append("\ntimeout: " + timeout);
        ret.append("\nexpected_mbrs: " + expected_mbrs);
        ret.append("\n]");
        return ret.toString();
    }

    /*
     * --------------------------------- Private Methods -------------------------------------
     */
    /**
     * This method runs with rsp_mutex locked (called by
     * <code>execute()</code>).
     */
    protected boolean doExecute_old(long timeout)
    {
        long start_time = 0;
        Address mbr, suspect;
        if (rsp_mode != GET_NONE)
        {
            req_id = corr.getNextRequestId();
        }
        reset(null); // clear 'responses' array
        if (suspects != null)
        {
            // mark all suspects in 'received' array
            for (int i = 0; i < suspects.size(); i++)
            {
                suspect = (Address) suspects.get(i);
                for (int j = 0; j < membership.length; j++)
                {
                    mbr = membership[j];
                    if (mbr.equals(suspect))
                    {
                        received[j] = SUSPECTED;
                        break; // we can break here because we ensure there are no duplicate members
                    }
                }
            }
        }

        try
        {
            if (getCacheLog().getIsInfoEnabled())
            {
                getCacheLog().Info("GroupRequest.doExecute()", "sending request (id=" + req_id + ')');
            }
            if (corr != null)
            {
                java.util.List tmp = members != null ? members : null;
                corr.sendRequest(req_id, tmp, request_msg, rsp_mode == GET_NONE ? null : this);
            }
            else
            {
                transport.send(request_msg);
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("GroupRequest.doExecute()", "exception=" + e.getMessage());
            if (corr != null)
            {
                corr.done(req_id);
            }
            return false;
        }

        long orig_timeout = timeout;
        if (timeout <= 0)
        {
            while (true)
            {
                /*
                 * Wait for responses:
                 */
                adjustMembership(); // may not be necessary, just to make sure...
                if (getResponses())
                {
                    if (corr != null)
                    {
                        corr.done(req_id);
                    }
                    if (getCacheLog().getIsInfoEnabled())
                    {
                        getCacheLog().Info("GroupRequest.doExecute()", "received all responses: " + toString());
                    }
                    return true;
                }
                try
                {
                    Monitor.wait(rsp_mutex);//rsp_mutex.wait();
                }
                catch (Exception e)
                {
                    getCacheLog().Error("GroupRequest.doExecute():2", "exception=" + e.getMessage());
                }
            }
        }
        else
        {
           
            start_time = System.currentTimeMillis();
            while (timeout > 0)
            {
                /*
                 * Wait for responses:
                 */
                if (getResponses())
                {
                    if (corr != null)
                    {
                        corr.done(req_id);
                    }
                    if (getCacheLog().getIsInfoEnabled())
                    {
                        getCacheLog().Info("GroupRequest.doExecute()", "received all responses: " + toString());
                    }
                    return true;
                }

                //: changed to Syste.currentTimeinMillis, accuracy issue expected
                timeout = orig_timeout - (System.currentTimeMillis() - start_time);
                //timeout = orig_timeout - ((System.currentTimeMillis() - 621355968000000000L) / 10000 - start_time);
                if (timeout > 0)
                {
                    try
                    {
                        Monitor.wait(rsp_mutex,timeout);//rsp_mutex.wait(timeout);
                    }
                    catch (Exception e)
                    {
                        getCacheLog().Error("GroupRequest.doExecute():3", "exception=" + e);
                        //e.printStacknTrace();
                    }
                }
            }

            //SAL:
            if (timeout <= 0)
            {
                RspList rspList = getResults();
                String failedNodes = "";
                if (rspList != null)
                {
                    for (int i = 0; i < rspList.size(); i++)
                    {
                        Object tempVar = rspList.elementAt(i);
                        Rsp rsp = (Rsp) ((tempVar instanceof Rsp) ? tempVar : null);
                        if (rsp != null && !rsp.wasReceived())
                        {
                            failedNodes += rsp.getSender();
                        }
                    }
                }

                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("GroupRequest.doExecute:", "[ " + req_id + " ] did not receive rsp from " + failedNodes + " [Timeout] " + timeout + " [timeout-val ="
                            + orig_timeout + "]");
                }
            }

            if (corr != null)
            {
                corr.done(req_id);
            }
            return false;
        }
    }

    protected boolean doExecute(long timeout)
    {
        long start_time = 0;
        Address mbr, suspect;
        if (rsp_mode != GET_NONE)
        {
            req_id = corr.getNextRequestId();
        }
        reset(null); // clear 'responses' array
        if (suspects != null)
        {
            // mark all suspects in 'received' array
            for (int i = 0; i < suspects.size(); i++)
            {
                suspect = (Address) suspects.get(i);
                for (int j = 0; j < membership.length; j++)
                {
                    mbr = membership[j];
                    if (mbr.equals(suspect))
                    {
                        received[j] = SUSPECTED;
                        break; // we can break here because we ensure there are no duplicate members
                    }
                }
            }
        }
       
        try
        {
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("GrpReq.doExec", "sending req_id :" + req_id + "; timeout: " + timeout);
            }
            if (getCacheLog().getIsInfoEnabled())
            {
                getCacheLog().Info("GroupRequest.doExecute()", "sending request (id=" + req_id + ')');
            }
            if (corr != null)
            {
                java.util.List tmp = members != null ? members : null;

                if (rsp_mode == GET_FIRST_NHOP || rsp_mode == GET_ALL_NHOP)
                {
                    corr.sendNHopRequest(req_id, tmp, request_msg, this);
                }
                else
                {
                    corr.sendRequest(req_id, tmp, request_msg, rsp_mode == GET_NONE ? null : this);
                }
            }
            else
            {
                transport.send(request_msg);
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("GroupRequest.doExecute()", "exception=" + e.getMessage());
            if (corr != null)
            {
                corr.done(req_id);
            }
            return false;
        }

        long orig_timeout = timeout;
        if (timeout <= 0)
        {
            while (true)
            {
                /*
                 * Wait for responses:
                 */
                adjustMembership(); // may not be necessary, just to make sure...
                if (getResponses())
                {
                    if (corr != null)
                    {
                        corr.done(req_id);
                    }
                    if (getCacheLog().getIsInfoEnabled())
                    {
                        getCacheLog().Info("GroupRequest.doExecute()", "received all responses: " + toString());
                    }
                    return true;
                }
                try
                {
                    Monitor.wait(rsp_mutex);//rsp_mutex.wait();
                }
                catch (Exception e)
                {
                    getCacheLog().Error("GroupRequest.doExecute():2", "exception=" + e.getMessage());
                }
            }
        }
        else
        {
            
            start_time = System.currentTimeMillis();
       
            long wakeuptime = timeout;
            int retries = s_requestEnquiryRetries;
            int enquiryFailure = 0;

            if (s_allowRequestEnquiry)
            {
                wakeuptime = s_requestEnquiryInterval * 1000;
            }

            while (timeout > 0)
            {
                /*
                 * Wait for responses:
                 */
                if (getResponses())
                {
                    
                    if (ServerMonitor.getMonitorActivity())
                    {
                        ServerMonitor.LogClientActivity("GrpReq.doExec", "req_id :" + req_id + " completed");
                    }

                    if (corr != null)
                    {
                        corr.done(req_id);
                    }
                    if (getCacheLog().getIsInfoEnabled())
                    {
                        getCacheLog().Info("GroupRequest.doExecute()", "received all responses: " + toString());
                    }
                    return true;
                }
                
                timeout = orig_timeout - (System.currentTimeMillis() - start_time);
               

                if (s_allowRequestEnquiry)
                {
                    if (wakeuptime > timeout)
                    {
                        wakeuptime = timeout;
                    }
                }
                else
                {
                    wakeuptime = timeout;
                }

                if (timeout > 0)
                {
                    try
                    {
                        
                        timeout = orig_timeout - (System.currentTimeMillis() - start_time);
                        boolean reacquired = Monitor.wait(rsp_mutex,wakeuptime);//ThreadUtil.wait(rsp_mutex, wakeuptime);

                        if ((!reacquired || _seqReset) && s_allowRequestEnquiry)
                        {
                         

                            if (getResponses())
                            {
                                if (ServerMonitor.getMonitorActivity())
                                {
                                    ServerMonitor.LogClientActivity("GrpReq.doExec", "req_id :" + req_id + " completed");
                                }

                                if (corr != null)
                                {
                                    corr.done(req_id);
                                }
                                if (getCacheLog().getIsInfoEnabled())
                                {
                                    getCacheLog().Info("GroupRequest.doExecute()", "received all responses: " + toString());
                                }
                                return true;
                            }
                            else
                            {
                                if (ServerMonitor.getMonitorActivity())
                                {
                                    ServerMonitor.LogClientActivity("GrpReq.doExec", "req_id :" + req_id + " completed");
                                }

                                if ((timeout > 0 && wakeuptime < timeout) && retries > 0)
                                {
                                    if (_seqReset)
                                    {
                                        _retriesAfteSeqReset++;
                                    }

                                    retries--;

                                    boolean enquireAgain = GetRequestStatus();

                                    if (!enquireAgain)
                                    {
                                        enquiryFailure++;
                                    }

                                    //for debugging and tesing purpose only. will be removed after testing.
                                    getCacheLog().CriticalInfo("GetRequestStatus, retries : " + retries + ", enquire again : " + ((Boolean) (enquireAgain)).toString()
                                            + ", enquiry failure : " + enquiryFailure);

                                    if (enquiryFailure >= 3 || _retriesAfteSeqReset > 3)
                                    {
                                        if (corr != null)
                                        {
                                            corr.done(req_id);
                                        }
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        getCacheLog().Error("GroupRequest.doExecute():3", "exception=" + e);
                        
                    }
                }
            }

           
            if (timeout <= 0)
            {
                RspList rspList = getResults();
                String failedNodes = "";
                if (rspList != null)
                {
                    for (int i = 0; i < rspList.size(); i++)
                    {
                        Object tempVar = rspList.elementAt(i);
                        Rsp rsp = (Rsp) ((tempVar instanceof Rsp) ? tempVar : null);
                        if (rsp != null && !rsp.wasReceived())
                        {
                            failedNodes += rsp.getSender();
                        }
                    }
                }

                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("GroupRequest.doExecute:", "[ " + req_id + " ] did not receive rsp from " + failedNodes + " [Timeout] " + timeout + " [timeout-val ="
                            + orig_timeout + "]");
                }
            }

            if (corr != null)
            {
                corr.done(req_id);
            }
            return false;
        }
    }

    private boolean GetRequestStatus()
    {
        java.util.HashMap statusResult = null;
        java.util.ArrayList failedNodes = new java.util.ArrayList();
        RspList rspList = getResults();
        boolean enquireStatusAgain = true;
        int suspectCount = 0;
        String notRecvNodes = "";

        if (rspList != null)
        {
            for (int i = 0; i < rspList.size(); i++)
            {
                Object tempVar = rspList.elementAt(i);
                Rsp rsp = (Rsp) ((tempVar instanceof Rsp) ? tempVar : null);

                if (rsp != null && !rsp.wasReceived())
                {
                    notRecvNodes += rsp.sender + ",";
                    failedNodes.add(rsp.getSender());
                }
                if (rsp != null && rsp.wasSuspected())
                {
                    suspectCount++;
                }
            }
        }
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("GroupRequest.GetReqStatus", req_id + " rsp not received from " + failedNodes.size() + " nodes");
        }

        boolean resendReq = true;
        java.util.ArrayList resendList = new java.util.ArrayList();
        int notRespondingCount = 0;

        if (failedNodes.size() > 0)
        {
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("GrpReq.GetReqStatus", " did not recv rsps from " + notRecvNodes + " nodes");
            }

            statusResult = corr.FetchRequestStatus(failedNodes, this.clusterMembership, req_id);
            StringBuilder sb = null;
            if (ServerMonitor.getMonitorActivity())
            {
                sb = new StringBuilder();
            }
            if (statusResult != null)
            {
                for (Iterator it = failedNodes.iterator(); it.hasNext();)
                {
                    Address node = (Address) it.next();
                    RequestStatus status = (RequestStatus) ((statusResult.get(node) instanceof RequestStatus) ? statusResult.get(node) : null);
                    if (status.getStatus() == RequestStatus.REQ_NOT_RECEIVED)
                    {
                        if (sb != null)
                        {
                            sb.append("(" + node + ":" + "REQ_NOT_RECEIVED)");
                        }
                        resendList.add(node);
                    }
                    if (status.getStatus() == RequestStatus.NONE)
                    {
                        if (sb != null)
                        {
                            sb.append("(" + node + ":" + "NONE)");
                        }
                        notRespondingCount++;
                    }
                    if (status.getStatus() == RequestStatus.REQ_PROCESSED)
                    {
                        if (sb != null)
                        {
                            sb.append("(" + node + ":" + "REQ_PROCESSED)");
                        }
                        if (!request_msg.getIsSeqRequired())
                        {
                            resendList.add(node);
                        }
                    }
                }
                if (sb != null && ServerMonitor.getMonitorActivity())
                {
                    ServerMonitor.LogClientActivity("GrpReq.GetReqStatus", "status of failed nodes " + sb.toString());
                }

                if (request_msg.getIsSeqRequired())
                {
                    if (resendList.size() != rspList.size())
                    {
                        if (getCacheLog().getIsInfoEnabled())
                        {
                            getCacheLog().Info("GroupRequest.GetReqStatus", req_id + "sequence message; no need to resend; resend_count " + resendList.size());
                        }
                        if (notRespondingCount > 0)
                        {
                            resendReq = false;
                        }
                        else
                        {
                            enquireStatusAgain = false;
                            resendReq = false;
                        }
                    }
                }
                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("GroupRequest.GetReqStatus", req_id + "received REQ_NOT_RECEIVED status from " + resendList.size() + " nodes");
                }

            }
            else
            {
                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("GroupRequest.GetReqStatus", req_id + " status result is NULL");
                }
            }



            if (resendReq && resendList.size() > 0)
            {
                if (corr != null)
                {
                    if (resendList.size() == 1)
                    {
                        request_msg.setDest((Address) ((resendList.get(0) instanceof Address) ? resendList.get(0) : null));
                    }
                    else
                    {
                        request_msg.setDests(resendList);
                    }

                    if (getCacheLog().getIsInfoEnabled())
                    {
                        getCacheLog().Info("GroupRequest.GetReqStatus", req_id + " resending messages to " + resendList.size());
                    }
                    corr.sendRequest(req_id, resendList, request_msg, rsp_mode == GET_NONE ? null : this);
                }
            }

        }
        return enquireStatusAgain;
    }

    /**
     * Return number of elements of a certain type in array 'received'. Type can be RECEIVED, NOT_RECEIVED or SUSPECTED
     */
    public int getNum(int type)
    {
        int retval = 0;
        for (int i = 0; i < received.length; i++)
        {
            if (received[i] == type)
            {
                retval++;
            }
        }
        return retval;
    }

    public int getNumFromNHops(int type)
    {
        int retval = 0;

        synchronized (_nhopMutex)
        {

            for (Address replica : nHops)
            {

                byte status = 0;

                if (receivedFromNHops.containsKey(replica))
                {
                    status = receivedFromNHops.get(replica);
                    if (status == type)
                    {
                        retval++;
                    }
                }
            }

        }

        return retval;
    }

    public void printReceived()
    {
        for (int i = 0; i < received.length; i++)
        {
            if (getCacheLog().getIsInfoEnabled())
            {
                getCacheLog().Info(membership[i] + ": " + (received[i] == NOT_RECEIVED ? "NOT_RECEIVED" : (received[i] == RECEIVED ? "RECEIVED" : "SUSPECTED")));
            }
        }
    }

    /**
     * Adjusts the 'received' array in the following way: <ul> <li>if a member P in 'membership' is not in 'members', P's entry in the 'received' array will be marked as SUSPECTED
     * <li>if P is 'suspected_mbr', then P's entry in the 'received' array will be marked as SUSPECTED </ul> This call requires exclusive access to rsp_mutex (called by
     * getResponses() which has a the rsp_mutex locked, so this should not be a problem).
     */
    public void adjustMembership()
    {
        Address mbr;
        if (membership == null || membership.length == 0)
        {
            
            return;
        }
        for (int i = 0; i < membership.length; i++)
        {
            mbr = membership[i];
            if ((this.members != null && !this.members.contains(mbr)) || suspects.contains(mbr))
            {
                addSuspect(mbr);
                responses[i] = null;
                received[i] = SUSPECTED;
            }
        }
    }

    /**
     * Adds a member to the 'suspects' list. Removes oldest elements from 'suspects' list to keep the list bounded ('max_suspects' number of elements)
     */
    public void addSuspect(Address suspected_mbr)
    {
        if (!suspects.contains(suspected_mbr))
        {
            suspects.add(suspected_mbr);
            while (suspects.size() >= max_suspects && suspects.size() > 0)
            {
                suspects.remove(0); // keeps queue bounded
            }
        }
    }


}
