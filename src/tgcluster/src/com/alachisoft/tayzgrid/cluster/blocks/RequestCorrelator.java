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

import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.cluster.util.Rsp;
import com.alachisoft.tayzgrid.cluster.stack.Protocol;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Transport;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.IThreadRunnable;
import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.Header;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.stats.TimeStats;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Framework to send requests and receive matching responses (matching on request ID). Multiple requests can be sent at a time. Whenever a response is received, the correct
 * <code>RspCollector</code> is looked up (key = id) and its method
 * <code>receiveResponse()</code> invoked. A caller may use
 * <code>done()</code> to signal that no more responses are expected, and that the corresponding entry may be removed. <p>
 * <code>RequestCorrelator</code> can be installed at both client and server sides, it can also switch roles dynamically, i.e. send a request and at the same time process an
 * incoming request (when local delivery is enabled, this is actually the default). <p>
 *
 *
 * <author> Bela Ban </author>
 */
public class RequestCorrelator
{

    /**
     * Switch the deadlock detection mechanism on/off
     *
     * @param flag the deadlock detection flag
     *
     */
    public void setDeadlockDetection(boolean value)
    {
        if (deadlock_detection != value)
        {
            // only set it if different
            deadlock_detection = value;
            if (started)
            {
                if (deadlock_detection)
                {
                    startScheduler();
                }
                else
                {
                    stopScheduler();
                }
            }
        }
    }

    public void setRequestHandler(RequestHandler value)
    {
        request_handler = value;
        start();
    }

    public void setConcurrentProcessing(boolean value)
    {
        this.concurrent_processing = value;
    }

    public Address getLocalAddress()
    {
        return local_addr;
    }

    public void setLocalAddress(Address value)
    {
        this.local_addr = value;
    }
    /**
     * The protocol layer to use to pass up/down messages. Can be either a Protocol or a Transport
     */
    protected Object transport = null;
    /**
     * The table of pending requests (keys=Long (request IDs), values=<tt>RequestEntry</tt>)
     */
    protected java.util.HashMap requests = new java.util.HashMap();
    /**
     * The handler for the incoming requests. It is called from inside the dispatcher thread
     */
    protected RequestHandler request_handler = null;
    /**
     * makes the instance unique (together with IDs)
     */
    protected String name = null;
    /**
     * The dispatching thread pool
     */
//		protected internal Scheduler scheduler = null;
    /**
     * The address of this group member
     */
    protected Address local_addr = null;
    /**
     * This field is used only if deadlock detection is enabled. In case of nested synchronous requests, it holds a list of the addreses of the senders with the address at the
     * bottom being the address of the first caller
     */
    protected java.util.ArrayList call_stack = null;
    /**
     * Whether or not to perform deadlock detection for synchronous (potentially recursive) group method invocations. If on, we use a scheduler (handling a priority queue),
     * otherwise we don't and call handleRequest() directly.
     */
    protected boolean deadlock_detection = false;
    /**
     * This field is used only if deadlock detection is enabled. It sets the calling stack for to that for the currently running request
     */
//		protected internal CallStackSetter call_stack_setter = null;
    /**
     * Process items on the queue concurrently (Scheduler). The default is to wait until the processing of an item has completed before fetching the next item from the queue. Note
     * that setting this to true may destroy the properties of a protocol stack, e.g total or causal order may not be guaranteed. Set this to true only if you know what you're
     * doing !
     */
    protected boolean concurrent_processing = false;
    private boolean stopReplying;
    protected boolean started = false;

    private ILogger _ncacheLog = null;

    public final ILogger getCacheLog()
    {
        return _ncacheLog;
    }
    private java.util.List members = new java.util.ArrayList(); //current members list;
    private long last_req_id = -1;
    private Object req_mutex = new Object();
    private java.util.HashMap _reqStatusTable = new java.util.HashMap();
    private Thread _statusCleanerThread = null;
    protected ReentrantReadWriteLock req_lock = new ReentrantReadWriteLock();
    protected Lock readLock = req_lock.readLock();
    protected Lock writeLock = req_lock.writeLock();


    /**
     * Constructor. Uses transport to send messages. If
     * <code>handler</code> is not null, all incoming requests will be dispatched to it (via
     * <code>handle(Message)</code>).
     *
     *
     * @param name Used to differentiate between different RequestCorrelators (e.g. in different protocol layers). Has to be unique if multiple request correlators are used.
     *
     *
     * @param transport Used to send/pass up requests. Can be either a Transport (only send() will be used then), or a Protocol (passUp()/passDown() will be used)
     *
     *
     * @param handler Request handler. Method
     * <code>handle(Message)</code> will be called when a request is received.
     *
     */
    public RequestCorrelator(String name, Object transport, RequestHandler handler, ILogger NCacheLog)
    {
        this.name = name;
        this.transport = transport;
        request_handler = handler;
        this._ncacheLog = NCacheLog;
        //this.nTrace = nTrace;
        start();
    }

    public RequestCorrelator(String name, Object transport, RequestHandler handler, Address local_addr, ILogger NCacheLog)
    {
        this.name = name;
        this.transport = transport;
        this.local_addr = local_addr;
        request_handler = handler;
        this._ncacheLog = NCacheLog;
        
        start();
    }

    public final void StopReplying()
    {
        stopReplying = true;
    }

    public final void StartReplying()
    {
        stopReplying = false;
    }

    /**
     * Constructor. Uses transport to send messages. If
     * <code>handler</code> is not null, all incoming requests will be dispatched to it (via
     * <code>handle(Message)</code>).
     *
     *
     * @param name Used to differentiate between different RequestCorrelators (e.g. in different protocol layers). Has to be unique if multiple request correlators are used.
     *
     *
     * @param transport Used to send/pass up requests. Can be either a Transport (only send() will be used then), or a Protocol (passUp()/passDown() will be used)
     *
     *
     * @param handler Request handler. Method
     * <code>handle(Message)</code> will be called when a request is received.
     *
     *
     * @param deadlock_detection When enabled (true) recursive synchronous message calls will be detected and processed with higher priority in order to solve deadlocks. Slows down
     * processing a little bit when enabled due to runtime checks involved.
     *
     */
    public RequestCorrelator(String name, Object transport, RequestHandler handler, boolean deadlock_detection, ILogger NCacheLog)
    {
        this.deadlock_detection = deadlock_detection;
        this.name = name;
        this.transport = transport;
        request_handler = handler;
        this._ncacheLog = NCacheLog;
       
        start();
    }

    public RequestCorrelator(String name, Object transport, RequestHandler handler, boolean deadlock_detection, boolean concurrent_processing, ILogger NCacheLog)
    {
        this.deadlock_detection = deadlock_detection;
        this.name = name;
        this.transport = transport;
        request_handler = handler;
        this.concurrent_processing = concurrent_processing;

        this._ncacheLog = NCacheLog;

        start();
    }

    public RequestCorrelator(String name, Object transport, RequestHandler handler, boolean deadlock_detection, Address local_addr, ILogger NCacheLog)
    {
        this.deadlock_detection = deadlock_detection;
        this.name = name;
        this.transport = transport;
        this.local_addr = local_addr;
        request_handler = handler;
        this._ncacheLog = NCacheLog;
        start();
    }

    public RequestCorrelator(String name, Object transport, RequestHandler handler, boolean deadlock_detection, Address local_addr, boolean concurrent_processing, ILogger NCacheLog)
    {
        this.deadlock_detection = deadlock_detection;
        this.name = name;
        this.transport = transport;
        this.local_addr = local_addr;
        request_handler = handler;
        this.concurrent_processing = concurrent_processing;

        this._ncacheLog = NCacheLog;

        start();
    }

    /**
     * Helper method for {@link #sendRequest(long,List,Message,RspCollector)}.
     */
    public void sendRequest(long id, Message msg, RspCollector coll)
    {
        sendRequest(id, null, msg, coll);
    }

    public void sendRequest(long id, java.util.List dest_mbrs, Message msg, RspCollector coll)
    {
        sendRequest(id, dest_mbrs, msg, coll, RequestCorrelatorHDR.REQ);
    }

    /**
     * Send a request to a group. If no response collector is given, no responses are expected (making the call asynchronous).
     *
     *
     * @param id The request ID. Must be unique for this JVM (e.g. current time in millisecs)
     *
     * @param dest_mbrs The list of members who should receive the call. Usually a group RPC is sent via multicast, but a receiver drops the request if its own address is not in
     * this list. Will not be used if it is null.
     *
     * @param msg The request to be sent. The body of the message carries the request data
     *
     *
     * @param coll A response collector (usually the object that invokes this method). Its methods
     * <code>ReceiveResponse</code> and
     * <code>Suspect</code> will be invoked when a message has been received or a member is suspected, respectively.
     *
     */
    public void sendRequest(long id, java.util.List dest_mbrs, Message msg, RspCollector coll, byte hdrType)
    {
        RequestCorrelatorHDR hdr = null;

        if (transport == null)
        {
            getCacheLog().Warn("RequestCorrelator.sendRequest()", "transport is not available !");
            return;
        }

        // i. Create the request correlator header and add it to the
        // msg
        // ii. If a reply is expected (sync call / 'coll != null'), add a
        // coresponding entry in the pending requests table
        // iii. If deadlock detection is enabled, set/update the call stack
        // iv. Pass the msg down to the protocol layer below

        Header tempVar = msg.getHeader(HeaderType.REQUEST_COORELATOR);
        hdr = (RequestCorrelatorHDR) ((tempVar instanceof RequestCorrelatorHDR) ? tempVar : null);
        if (hdr == null)
        {
            hdr = new RequestCorrelatorHDR();
            hdr.type = hdrType;
            hdr.id = id;
            hdr.rsp_expected = coll != null ? true : false;
            hdr.dest_mbrs = dest_mbrs;
        }

        if (coll != null)
        {
            if (deadlock_detection)
            {
                if (local_addr == null)
                {
                    getCacheLog().Error("RequestCorrelator.sendRequest()", "local address is null !");
                    return;
                }
                java.util.ArrayList new_call_stack = (call_stack != null ? (java.util.ArrayList) call_stack.clone() : new java.util.ArrayList());
                new_call_stack.add(local_addr);
                hdr.call_stack = new_call_stack;
            }
            addEntry(hdr.id, new RequestEntry(coll), dest_mbrs);
        }
        msg.putHeader(HeaderType.REQUEST_COORELATOR, hdr);

        try
        {
            if (transport instanceof Protocol)
            {
               
                Event evt = new Event();
                evt.setType(Event.MSG);
                evt.setArg(msg);
                ((Protocol) transport).passDown(evt);
            }
            else if (transport instanceof Transport)
            {
                ((Transport) transport).send(msg);
            }
            else
            {
                getCacheLog().Error("RequestCorrelator.sendRequest()", "transport object has to be either a " + "Transport or a Protocol, however it is a " + transport.getClass());
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("RequestCorrelator.sendRequest()", e.toString());
        }
    }

    public void sendNHopRequest(long id, java.util.List dest_mbrs, Message msg, RspCollector coll)
    {
        sendRequest(id, dest_mbrs, msg, coll, RequestCorrelatorHDR.NHOP_REQ);
    }

    /**
     * Used to signal that a certain request may be garbage collected as all responses have been received.
     */
    public void done(long id)
    {
        removeEntry(id);
    }

    /**
     * Checks wheter the given adress is memeber or not.
     *
     * @param member address of the member
     * @return True if given address is member or not.
     */
    public final boolean CheckForMembership(Address member)
    {

        try
        {
            readLock.lock();
            if (members != null && members.contains(member))
            {
                return true;
            }
        }
        finally
        {
          readLock.unlock();
        }
        return false;
    }

    /**
     * <b>Callback</b>. <p> Called by the protocol below when a message has been received. The algorithm should test whether the message is destined for us and, if not, pass it up
     * to the next layer. Otherwise, it should remove the header and check whether the message is a request or response. In the first case, the message will be delivered to the
     * request handler registered (calling its
     * <code>handle()</code> method), in the second case, the corresponding response collector is looked up and the message delivered.
     */
    public boolean receive(Event evt)
    {
        switch (evt.getType())
        {
            case Event.SUSPECT: // don't wait for responses from faulty members
                receiveSuspect((Address) evt.getArg());
                break;

            case Event.VIEW_CHANGE: // adjust number of responses to wait for
                receiveView((View) evt.getArg());
                break;

            case Event.SET_LOCAL_ADDRESS:
                setLocalAddress((Address) evt.getArg());
                break;

            case Event.MSG:
                if (!receiveMessage((Message) evt.getArg()))
                {
                    return true;
                }
                break;

            case Event.RESET_SEQUENCE:

                receiveSequenceReset();
                return true;
        }
        return false;
    }

    public void start()
    {
        if (deadlock_detection)
        {
            startScheduler();
        }
        started = true;

        if (_statusCleanerThread == null)
        {
           
            _statusCleanerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    RequstStatusClean();
                }
            });
            _statusCleanerThread.start();

        }
    }

    protected void startScheduler()
    {

    }

    public void stop()
    {
        stopScheduler();
        started = false;

        if (_statusCleanerThread != null && _statusCleanerThread.isAlive())
        {
            try
            {
                getCacheLog().Flush();
                _statusCleanerThread.stop();
            }
            catch (Exception e)
            {
            }
        }
        _reqStatusTable.clear();
        _reqStatusTable = null;
    }

    protected void stopScheduler()
    {

    }

    // .......................................................................
    public final void receiveSequenceReset()
    {
        RequestEntry entry;
        java.util.ArrayList copy;

      
        readLock.lock();
        try
        {
            copy = new java.util.ArrayList(requests.values());
        }
        finally
        {
            readLock.unlock();
        }
        for (java.util.Iterator it = copy.iterator(); it.hasNext();)
        {
            entry = (RequestEntry) it.next();
            if (entry.coll != null && entry.coll instanceof GroupRequest)
            {
                ((GroupRequest) entry.coll).SequenceReset();
            }
        }
    }

    /**
     * <tt>Event.SUSPECT</tt> event received from a layer below <p> All response collectors currently registered will be notified that
     * <code>mbr</code> may have crashed, so they won't wait for its response.
     */
    public void receiveSuspect(Address mbr)
    {
        RequestEntry entry;
        java.util.ArrayList copy;

        if (mbr == null)
        {
            return;
        }

        getCacheLog().Debug("suspect=" + mbr);

     
        readLock.lock();
        try
        {
            copy = new java.util.ArrayList(requests.values());
        }
        finally
        {
            readLock.unlock();
        }
        for (java.util.Iterator it = copy.iterator(); it.hasNext();)
        {
            entry = (RequestEntry) it.next();
            if (entry.coll != null)
            {
                entry.coll.suspect(mbr);
            }
        }
    }

    private void MarkRequestArrived(long requestId, Address node)
    {
        java.util.HashMap nodeStatusTable = null;
        RequestStatus status = new RequestStatus(requestId);
        status.MarkReceived();

        synchronized (_reqStatusTable)
        {
            nodeStatusTable = (java.util.HashMap) ((_reqStatusTable.get(node) instanceof java.util.HashMap) ? _reqStatusTable.get(node) : null);
            if (nodeStatusTable == null)
            {
                nodeStatusTable = new java.util.HashMap();
                _reqStatusTable.put(node, nodeStatusTable);
            }
            nodeStatusTable.put(requestId, status);
        }
    }

    private void MarkRequestProcessed(long requestId, Address node)
    {
        java.util.HashMap nodeStatusTable = null;
        RequestStatus status = null;

        synchronized (_reqStatusTable)
        {
            nodeStatusTable = (java.util.HashMap) ((_reqStatusTable.get(node) instanceof java.util.HashMap) ? _reqStatusTable.get(node) : null);
            if (nodeStatusTable != null && nodeStatusTable.get(requestId) != null)
            {
                status = (RequestStatus) ((nodeStatusTable.get(requestId) instanceof RequestStatus) ? nodeStatusTable.get(requestId) : null);
                status.MarkProcessed();
            }
        }
    }

    private RequestStatus GetRequestStatus(long requestId, Address node)
    {
        java.util.HashMap nodeStatusTable = null;

        synchronized (_reqStatusTable)
        {
            nodeStatusTable = (java.util.HashMap) ((_reqStatusTable.get(node) instanceof java.util.HashMap) ? _reqStatusTable.get(node) : null);
            if (nodeStatusTable != null && nodeStatusTable.get(requestId) != null)
            {
                return (RequestStatus) ((nodeStatusTable.get(requestId) instanceof RequestStatus) ? nodeStatusTable.get(requestId) : null);
            }
        }
        return new RequestStatus(requestId);
    }

    private void RequstStatusClean()
    {
        java.util.HashMap nodeReqTable = null;
        java.util.ArrayList expiredReqStatus = new java.util.ArrayList();
        RequestStatus reqStatus = null;


        while (_statusCleanerThread != null)
        {
            try
            {

                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("RequestCorrelator.RequestCleaner", "request cleaning is happening " + Thread.currentThread().getId());
                }

                expiredReqStatus.clear();
                java.util.ArrayList nodes = new java.util.ArrayList();

                if (_reqStatusTable.size() > 0)
                {
                    synchronized (_reqStatusTable)
                    {
                        nodes.addAll(_reqStatusTable.keySet());
                    }
                }
                for (Iterator it = nodes.iterator(); it.hasNext();)
                {
                    Address node = (Address)it.next();
                    synchronized (_reqStatusTable)
                    {

                        nodeReqTable = (java.util.HashMap) ((_reqStatusTable.get(node) instanceof java.util.HashMap) ? _reqStatusTable.get(node) : null);
                        if (nodeReqTable != null)
                        {
                            Iterator ide = nodeReqTable.entrySet().iterator();
                            while (ide.hasNext())
                            {
                                Map.Entry ent = (Map.Entry)ide.next();
                                reqStatus = (RequestStatus) ((ent.getValue() instanceof RequestStatus) ? ent.getValue() : null);
                                if (reqStatus != null && reqStatus.HasExpired())
                                {
                                    expiredReqStatus.add(reqStatus.getReqId());
                                }
                            }
                        }
                    }
                    if (nodeReqTable != null)
                    {
                        for (Iterator it1 = expiredReqStatus.iterator(); it1.hasNext();)
                        {
                            long reqId = (Long)it1.next();
                            synchronized (_reqStatusTable)
                            {
                                nodeReqTable.remove(reqId);
                            }
                        }
                        if (getCacheLog().getIsInfoEnabled())
                        {
                            getCacheLog().Info("RequestCorrelator.RequestCleaner", "total requst_status :" + nodeReqTable.size() + " ;expired :" + expiredReqStatus.size());
                        }
                    }
                }
                Thread.sleep(15000);
            }

            catch (Exception e)
            {
                if (getCacheLog().getIsErrorEnabled())
                {
                    getCacheLog().Error("RequestCorrelator.RequestCleaner", "An error occured while cleaning request_status. " + e.toString());
                }
            }
        }
    }

    /**
     * Fetches the request status from the nodes.
     *
     * @param nodes
     * @return
     */
    public final java.util.HashMap FetchRequestStatus(java.util.ArrayList nodes, java.util.List clusterMembership, long reqId)
    {
        java.util.HashMap result = new java.util.HashMap();
        if (nodes != null && nodes.size() > 0)
        {
            RequestCorrelatorHDR hdr = new RequestCorrelatorHDR(RequestCorrelatorHDR.GET_REQ_STATUS, getNextRequestId(), true, null);
            hdr.status_reqId = reqId;
            Message msg = new Message();
            msg.putHeader(HeaderType.REQUEST_COORELATOR, hdr);
            msg.setDests(nodes);
            msg.setIsSeqRequired(false);
            msg.setIsUserMsg(true);
            msg.setRequestId(reqId);
            msg.setBuffer(new byte[0]);

            GroupRequest req = new GroupRequest(msg, this, nodes, clusterMembership, GroupRequest.GET_ALL, 2000, 0, this._ncacheLog);
            req.execute();

            RspList rspList = req.getResults();
            RequestStatus reqStatus = null;
            if (rspList != null)
            {
                for (int i = 0; i < rspList.size(); i++)
                {
                    Object tempVar = rspList.elementAt(i);
                    Rsp rsp = (Rsp) ((tempVar instanceof Rsp) ? tempVar : null);
                    if (rsp != null)
                    {
                        if (rsp.received)
                        {
                            if (getCacheLog().getIsInfoEnabled())
                            {
                                getCacheLog().Info("ReqCorrelator.FetchReqStatus", reqId + " status response received from " + rsp.sender);
                            }
                            Object rspValue = rsp.getValue();
                            if (rspValue instanceof byte[])
                            {

                                Object tempVar2 = null;
                               
                                reqStatus = (RequestStatus) ((tempVar2 instanceof RequestStatus) ? tempVar2 : null);
                            }
                            else
                            {
                                Object tempVar3 = rsp.getValue();
                                reqStatus = (RequestStatus) ((tempVar3 instanceof RequestStatus) ? tempVar3 : null);
                            }

                            if (getCacheLog().getIsInfoEnabled())
                            {
                                getCacheLog().Info("ReqCorrelator.FetchReqStatus", reqId + " status response: " + reqStatus);
                            }

                            result.put(rsp.getSender(), reqStatus);
                        }
                        else
                        {
                            if (getCacheLog().getIsInfoEnabled())
                            {
                                getCacheLog().Info("ReqCorrelator.FetchReqStatus", reqId + " status response NOT received from " + rsp.sender);
                            }
                            result.put(rsp.getSender(), new RequestStatus(reqId, RequestStatus.NONE));
                        }
                    }
                }
            }

        }
        return result;
    }

    /**
     * <tt>Event.VIEW_CHANGE</tt> event received from a layer below <p> Mark all responses from members that are not in new_view as NOT_RECEIVED.
     *
     */
    public void receiveView(View new_view)
    {
        RequestEntry entry;
        java.util.ArrayList copy;
        java.util.ArrayList oldMembers = new java.util.ArrayList();
       
       readLock.lock();
        try
        {
            if (new_view != null)
            {
                if (members != null)
                {
                    for (Iterator it = members.iterator(); it.hasNext();)
                    {
                        Address member = (Address) it.next();
                        if (!new_view.getMembers().contains(member))
                        {
                            oldMembers.add(member);
                        }
                    }
                }
                Object tempVar = GenericCopier.DeepCopy(new_view.getMembers());//.clone();
                members = (java.util.List) ((tempVar instanceof java.util.List) ? tempVar : null);
            }
            copy = new java.util.ArrayList(requests.values());
        }
        finally
        {
            readLock.unlock();
        }
        for (java.util.Iterator it = copy.iterator(); it.hasNext();)
        {
            entry = (RequestEntry) it.next();
            if (entry.coll != null)
            {
                entry.coll.viewChange(new_view);
            }
        }
        //Remove request status information for all requests from the old members.
        synchronized (_reqStatusTable)
        {
            if (members != null)
            {
                for (Iterator it = oldMembers.iterator(); it.hasNext();)
                {
                    Address oldmember = (Address) it.next();
                    _reqStatusTable.remove(oldmember);
                }
            }
        }
    }

    /**
     * Handles a message coming from a layer below
     *
     * @return true if the event should be forwarded further up, otherwise false (message was consumed)
     *
     */
    public boolean receiveMessage(Message msg)
    {
        Object tmpHdr;
        RequestCorrelatorHDR hdr;
        RspCollector coll;
        java.util.List dests;

        // i. If header is not an instance of request correlator header, ignore
        //
        // ii. Check whether the message was sent by a request correlator with
        // the same name (there may be multiple request correlators in the same
        // protocol stack...)
        tmpHdr = msg.getHeader(HeaderType.REQUEST_COORELATOR);
        if (!(tmpHdr instanceof RequestCorrelatorHDR))
        {
            return (true);
        }

        hdr = (RequestCorrelatorHDR) tmpHdr;

        // If the header contains a destination list, and we are not part of it, then we discard the
        // request (was addressed to other members)
        dests = hdr.dest_mbrs;

        if (dests != null && local_addr != null && !dests.contains(local_addr))
        {
            getCacheLog().Debug("RequestCorrelator.receiveMessage()", "discarded request from " + msg.getSrc() + " as we are not part of destination list (local_addr="
                    + local_addr + ", hdr=" + hdr + ')');
            return false;
        }
        if (!hdr.doProcess)
        {
            getCacheLog().Debug("RequestCorrelator.receiveMessage()", hdr.id + " I should not process");
            return false;
        }
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RequestCorrelator.receiveMessage()", "header is " + hdr);
        }

        // [RequestCorrelatorHDR.REQ]:
        // i. If there is no request handler, discard
        // ii. Check whether priority: if synchronous and call stack contains
        // address that equals local address -> add priority request. Else
        // add normal request.
        //
        // [RequestCorrelatorHDR.RSP]:
        // Remove the msg request correlator header and notify the associated
        // <tt>RspCollector</tt> that a reply has been received
        switch (hdr.type)
        {
            case RequestCorrelatorHDR.GET_REQ_STATUS:
            case RequestCorrelatorHDR.REQ:
                if (request_handler == null)
                {
                    return (false);
                }

                //In case of NHop requests, the response is not sent to the sender of the request. Instead,
                //response is sent back to a node whose address is informed by the sender.
              
                handleRequest(msg, hdr.whomToReply);
                break;

            case RequestCorrelatorHDR.NHOP_REQ:
                handleNHopRequest(msg);
                break;

            case RequestCorrelatorHDR.RSP:
                msg.removeHeader(HeaderType.REQUEST_COORELATOR);
                coll = findEntry(hdr.id);
                if (coll != null)
                {
                    coll.receiveResponse(msg);

                }
                break;

            case RequestCorrelatorHDR.NHOP_RSP:               
                msg.removeHeader(HeaderType.REQUEST_COORELATOR);
                coll = findEntry(hdr.id);

                if (coll != null)
                {
                    if (hdr.expectResponseFrom != null)
                    {
                        if (coll instanceof GroupRequest)
                        {
                            GroupRequest groupRequest = (GroupRequest) ((coll instanceof GroupRequest) ? coll : null);
                            groupRequest.AddNHop(hdr.expectResponseFrom);
                            groupRequest.AddNHopDefaultStatus(hdr.expectResponseFrom);
                        }
                    }
                   
                    coll.receiveResponse(msg);
                }
                break;

            default:
                msg.removeHeader(HeaderType.REQUEST_COORELATOR);
                getCacheLog().Error("RequestCorrelator.receiveMessage()", "header's type is neither REQ nor RSP !");
                break;

        }

        return (false);
    }

    /**
     * Generates a new unique request ID
     */
    public final long getNextRequestId()
    {
        synchronized (req_mutex)
        {
            //TAIM: Request id ranges from 0 to long.Max. If it reaches the max we
            //re-initialize it to -1;
            if (last_req_id == Long.MAX_VALUE)
            {
                last_req_id = -1;
            }
            long result = ++last_req_id;
            return result;
        }
    }
    // .......................................................................

    /**
     * Add an association of:<br> ID -> <tt>RspCollector</tt>
     */
    private void addEntry(long id, RequestEntry entry, java.util.List dests)
    {
        long id_obj = (long) id;
      
        writeLock.lock();
        try
        {
            //we check whether all the destination are still alive or not
            //if view has changed and one or more destination members has left
            //then we should declare them suspect.
            if (dests != null)
            {
                for (Iterator it = dests.iterator(); it.hasNext();)
                {
                    Address dest = (Address)it.next();
                    if (!members.contains(dest) && entry.coll != null)
                    {
                        entry.coll.suspect(dest);
                    }
                }
            }
            if (!requests.containsKey(id_obj))
            {
                requests.put(id_obj, entry);
            }

        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Remove the request entry associated with the given ID
     *
     *
     * @param id the id of the <tt>RequestEntry</tt> to remove
     *
     */
    private void removeEntry(long id)
    {
        long id_obj = (long) id;

        // changed by bela Feb 28 2003 (bug fix for 690606)
        // changed back to use synchronization by bela June 27 2003 (bug fix for #761804),
        // we can do this because we now copy for iteration (viewChange() and suspect())
        //lock (requests.SyncRoot)
        writeLock.lock();
        try
        {
            requests.remove(id_obj);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * @param id the ID of the corresponding <tt>RspCollector</tt>
     *
     *
     * @return the <tt>RspCollector</tt> associated with the given ID
     *
     */
    private RspCollector findEntry(long id)
    {
        long id_obj = (long) id;
        RequestEntry entry;

        readLock.lock();
        try
        {
            entry = (RequestEntry) requests.get(id_obj);
        }
        finally
        {
            readLock.unlock();
        }
        return ((entry != null) ? entry.coll : null);
    }

    /**
     * Handle a request msg for this correlator
     *
     *
     * @param req the request msg
     *
     */
    private void handleNHopRequest(Message req)
    {
        Object retval = null;

        byte[] rsp_buf = null;
        RequestCorrelatorHDR hdr, rsp_hdr, replicaMsg_hdr;
        Message rsp;

        Address destination = null;
        Message replicationMsg = null;

        // i. Remove the request correlator header from the msg and pass it to
        // the registered handler
        //
        // ii. If a reply is expected, pack the return value from the request
        // handler to a reply msg and send it back. The reply msg has the same
        // ID as the request and the name of the sender request correlator
        hdr = (RequestCorrelatorHDR) req.removeHeader(HeaderType.REQUEST_COORELATOR);

        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RequestCorrelator.handleNHopRequest()", "calling (" + (request_handler != null ? request_handler.getClass().getName() : "null") + ") with request "
                    + hdr.id);
        }

        try
        {
            if (hdr.rsp_expected)
            {
                req.setRequestId(hdr.id);
            }
            else
            {
                req.setRequestId(-1);
            }

            if (req.getHandledAysnc())
            {
                request_handler.handle(req);
                return;
            }
            if (hdr.type == RequestCorrelatorHDR.NHOP_REQ)
            {
                MarkRequestArrived(hdr.id, req.getSrc());
                tangible.RefObject<Address> tempRef_destination = new tangible.RefObject<Address>(destination);
                tangible.RefObject<com.alachisoft.tayzgrid.cluster.Message> tempRef_replicationMsg = new tangible.RefObject<com.alachisoft.tayzgrid.cluster.Message>(replicationMsg);
                retval = request_handler.handleNHopRequest(req, tempRef_destination, tempRef_replicationMsg);
                destination = tempRef_destination.argvalue;
                replicationMsg = tempRef_replicationMsg.argvalue;
            }
        }
        catch (Exception t)
        {
            getCacheLog().Error("RequestCorrelator.handleNHopRequest()", "error invoking method, exception=" + t.toString());
            retval = t;
        }

        if (!hdr.rsp_expected || stopReplying)
        {
            return;
        }

        if (transport == null)
        {
            getCacheLog().Error("RequestCorrelator.handleNHopRequest()", "failure sending " + "response; no transport available");
            return;
        }

        //1. send request to other replica.
        //   this node will send the response to original node.
        if (replicationMsg != null)
        {
            replicaMsg_hdr = new RequestCorrelatorHDR();
            replicaMsg_hdr.type = RequestCorrelatorHDR.REQ;
            replicaMsg_hdr.id = hdr.id;
            replicaMsg_hdr.rsp_expected = true;
            replicaMsg_hdr.whomToReply = req.getSrc();

            replicationMsg.setDest(destination);
            replicationMsg.putHeader(HeaderType.REQUEST_COORELATOR, replicaMsg_hdr);

            try
            {
                if (transport instanceof Protocol)
                {
                    Event evt = new Event();
                    evt.setType(Event.MSG);
                    evt.setArg(replicationMsg);
                    ((Protocol) transport).passDown(evt);
                }
                else if (transport instanceof Transport)
                {
                    ((Transport) transport).send(replicationMsg);
                }
                else
                {
                    getCacheLog().Error("RequestCorrelator.handleRequest()", "transport object has to be either a " + "Transport or a Protocol, however it is a "
                            + transport.getClass());
                }
            }
            catch (Exception e)
            {
                getCacheLog().Error("RequestCorrelator.handleRequest()", e.toString());
            }
        }
       

        //2. send reply back to original node
        //   and inform the original node that it must expect another response
        //   from the replica node. (the response of the request sent in part 1)
        rsp = req.makeReply();

        try
        {
            if (retval instanceof OperationResponse)
            {
                rsp_buf = (byte[]) ((OperationResponse) retval).SerializablePayload;
                rsp.setPayload(((OperationResponse) retval).UserPayload);
                rsp.responseExpected = true;
            }
            else if (retval instanceof byte[])
            {
                rsp_buf = (byte[]) retval;
            }
            else
            {
                rsp_buf = CompactBinaryFormatter.toByteBuffer(retval, ""); // retval could be an exception, or a real value
            }
        }
        catch (Exception t)
        {
            getCacheLog().Error("RequestCorrelator.handleRequest()", t.toString());
            try
            {
                rsp_buf = CompactBinaryFormatter.toByteBuffer(t, ""); // this call shoudl succeed (all exceptions are serializable)
            }
            catch (Exception e)
            {
                getCacheLog().Error("RequestCorrelator.handleRequest()", "failed sending response: " + "return value (" + retval + ") is not serializable");
                return;
            }
        }

        if (rsp_buf != null)
        {
            rsp.setBuffer(rsp_buf);
        }

        rsp_hdr = new RequestCorrelatorHDR();
        rsp_hdr.type = RequestCorrelatorHDR.NHOP_RSP;
        rsp_hdr.id = hdr.id;
        rsp_hdr.rsp_expected = false;

        if (replicationMsg != null)
        {
            rsp_hdr.expectResponseFrom = destination;
        }
     
        rsp.putHeader(HeaderType.REQUEST_COORELATOR, rsp_hdr);

        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RequestCorrelator.handleRequest()", "sending rsp for " + rsp_hdr.id + " to " + rsp.getDest());
        }

        try
        {
            if (transport instanceof Protocol)
            {
                Event evt = new Event();
                evt.setType(Event.MSG);
                evt.setArg(rsp);
                ((Protocol) transport).passDown(evt);
            }
            else if (transport instanceof Transport)
            {
                ((Transport) transport).send(rsp);
            }
            else
            {
                getCacheLog().Error("RequestCorrelator.handleRequest()", "transport object has to be either a " + "Transport or a Protocol, however it is a "
                        + transport.getClass());
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("RequestCorrelator.handleRequest()", e.toString());
        }

        MarkRequestProcessed(hdr.id, req.getSrc());
    }

    /**
     * Handle a request msg for this correlator
     *
     *
     * @param req the request msg
     *
     */
    private void handleRequest(Message req, Address replyTo)
    {
        Object retval;

        byte[] rsp_buf = null;
        RequestCorrelatorHDR hdr, rsp_hdr;
        Message rsp;

        // i. Remove the request correlator header from the msg and pass it to
        // the registered handler
        //
        // ii. If a reply is expected, pack the return value from the request
        // handler to a reply msg and send it back. The reply msg has the same
        // ID as the request and the name of the sender request correlator
        hdr = (RequestCorrelatorHDR) req.removeHeader(HeaderType.REQUEST_COORELATOR);

        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RequestCorrelator.handleRequest()", "calling (" + (request_handler != null ? request_handler.getClass().getName() : "null") + ") with request "
                    + hdr.id);
        }

        TimeStats appTimeStats = null;
        boolean isProfilable = false;
        try
        {

            if (hdr.rsp_expected)
            {
                req.setRequestId(hdr.id);
            }
            else
            {
                req.setRequestId(-1);
            }

            if (req.getHandledAysnc())
            {
                request_handler.handle(req);
                return;
            }
            if (hdr.type == RequestCorrelatorHDR.GET_REQ_STATUS)
            {
                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("ReqCorrelator.handleRequet", hdr.status_reqId + " receive RequestStatus request from " + req.getSrc());
                }
                retval = GetRequestStatus(hdr.status_reqId, req.getSrc());
                if (getCacheLog().getIsInfoEnabled())
                {
                    getCacheLog().Info("ReqCorrelator.handleRequet", hdr.status_reqId + " RequestStatus :" + retval);
                }
            }
            else
            {
                MarkRequestArrived(hdr.id, req.getSrc());
                retval = request_handler.handle(req);
            }

            //request is being handled asynchronously, so response will be send by
            //the the user itself.

        }
        catch (Exception t)
        {
            getCacheLog().Error("RequestCorrelator.handleRequest()", "error invoking method, exception=" + t.toString());
            retval = t;
        }

        if (!hdr.rsp_expected || stopReplying)
        // asynchronous call, we don't need to send a response; terminate call here
        {
            return;
        }

        if (transport == null)
        {
            getCacheLog().Error("RequestCorrelator.handleRequest()", "failure sending " + "response; no transport available");
            return;
        }

        rsp = req.makeReply();
        if (replyTo != null)
        {
            rsp.setDest(replyTo);
        }
        // changed (bela Feb 20 2004): catch exception and return exception
        try
        {
            if (retval instanceof OperationResponse)
            {
                rsp_buf = (byte[]) ((OperationResponse) retval).SerializablePayload;
                rsp.setPayload(((OperationResponse) retval).UserPayload);
                rsp.responseExpected = true;
            }
            else if (retval instanceof byte[])
            {
                rsp_buf = (byte[]) retval;
            }
            else
            {
                rsp_buf = CompactBinaryFormatter.toByteBuffer(retval, ""); // retval could be an exception, or a real value
            }
        }
        catch (Exception t)
        {
            getCacheLog().Error("RequestCorrelator.handleRequest()", t.toString());
            try
            {
                rsp_buf = CompactBinaryFormatter.toByteBuffer(t, ""); // this call shoudl succeed (all exceptions are serializable)
            }
            catch (Exception e)
            {
                getCacheLog().Error("RequestCorrelator.handleRequest()", "failed sending response: " + "return value (" + retval + ") is not serializable");
                return;
            }
        }

        if (rsp_buf != null)
        {
            rsp.setBuffer(rsp_buf);
        }

        if (rsp.getDest().equals(local_addr))
        {
           
            //we need not to put our response on the stack.
            rsp.setSrc(local_addr);
            ReceiveLocalResponse(rsp, hdr.id);
            return;
        }
        
        rsp_hdr = new RequestCorrelatorHDR();
        rsp_hdr.type = RequestCorrelatorHDR.RSP;
        rsp_hdr.id = hdr.id;
        rsp_hdr.rsp_expected = false;

        rsp.putHeader(HeaderType.REQUEST_COORELATOR, rsp_hdr);

        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RequestCorrelator.handleRequest()", "sending rsp for " + rsp_hdr.id + " to " + rsp.getDest());
        }

        try
        {

            if (transport instanceof Protocol)
            {
              
                Event evt = new Event();
                evt.setType(Event.MSG);
                evt.setArg(rsp);
                ((Protocol) transport).passDown(evt);
            }
            else if (transport instanceof Transport)
            {
                ((Transport) transport).send(rsp);
            }
            else
            {
                getCacheLog().Error("RequestCorrelator.handleRequest()", "transport object has to be either a " + "Transport or a Protocol, however it is a "
                        + transport.getClass());
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("RequestCorrelator.handleRequest()", e.toString());
        }
        MarkRequestProcessed(hdr.id, req.getSrc());
    }

    private void handleStatusRequest(Message req)
    {
        Object retval;

        byte[] rsp_buf = null;
        RequestCorrelatorHDR hdr, rsp_hdr;
        Message rsp;

        // i. Remove the request correlator header from the msg and pass it to
        // the registered handler
        //
        // ii. If a reply is expected, pack the return value from the request
        // handler to a reply msg and send it back. The reply msg has the same
        // ID as the request and the name of the sender request correlator
        hdr = (RequestCorrelatorHDR) req.removeHeader(HeaderType.REQUEST_COORELATOR);

        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RequestCorrelator.handleStatusRequest()", "calling (" + (request_handler != null ? request_handler.getClass().getName() : "null")
                    + ") with request " + hdr.id);
        }


        if (transport == null)
        {
            getCacheLog().Error("RequestCorrelator.handleStatusRequest()", "failure sending " + "response; no transport available");
            return;
        }
        RequestStatus status = GetRequestStatus(hdr.id, req.getSrc());

        rsp_hdr = new RequestCorrelatorHDR();
        rsp_hdr.type = RequestCorrelatorHDR.GET_REQ_STATUS_RSP;
        rsp_hdr.id = hdr.id;
        rsp_hdr.rsp_expected = false;
        rsp_hdr.reqStatus = status;

        rsp = req.makeReply();
        rsp.putHeader(HeaderType.REQUEST_COORELATOR, rsp_hdr);

        if (rsp.getDest().equals(local_addr))
        {
           
            rsp.setSrc(local_addr);
            ReceiveLocalResponse(rsp, hdr.id);
            return;
        }

        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RequestCorrelator.handleStatusRequest()", "sending rsp for " + rsp_hdr.id + " to " + rsp.getDest());
        }

        try
        {

            if (transport instanceof Protocol)
            {
                
                Event evt = new Event();
                evt.setType(Event.MSG);
                evt.setArg(rsp);
                ((Protocol) transport).passDown(evt);
            }
            else if (transport instanceof Transport)
            {
                ((Transport) transport).send(rsp);
            }
            else
            {
                getCacheLog().Error("RequestCorrelator.handleStatusRequest()", "transport object has to be either a " + "Transport or a Protocol, however it is a "
                        + transport.getClass());
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("RequestCorrelator.handleStatusRequest()", e.toString());
        }
    }

    public final void AsyncProcessRequest(Object req)
    {
        request_handler.handle((Message) req);
    }

    public final void SendResponse(long resp_id, Message response)
    {
        if (response.getDest().equals(local_addr))
        {
           
            response.setSrc(local_addr);
            ReceiveLocalResponse(response, resp_id);
            return;
        }

        RequestCorrelatorHDR rsp_hdr = new RequestCorrelatorHDR();
        rsp_hdr.type = RequestCorrelatorHDR.RSP;
        rsp_hdr.id = resp_id;
        rsp_hdr.rsp_expected = false;

        response.putHeader(HeaderType.REQUEST_COORELATOR, rsp_hdr);

        try
        {

            if (transport instanceof Protocol)
            {
        
                Event evt = new Event();
                evt.setType(Event.MSG);
                evt.setArg(response);
                ((Protocol) transport).passDown(evt);
            }
            else if (transport instanceof Transport)
            {
                ((Transport) transport).send(response);
            }
            else
            {
                getCacheLog().Error("RequestCorrelator.handleRequest()", "transport object has to be either a " + "Transport or a Protocol, however it is a "
                        + transport.getClass());
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("RequestCorrelator.handleRequest()", e.toString());
        }
    }
    // .......................................................................

    /**
     * Associates an ID with an <tt>RspCollector</tt>
     */
    private static class RequestEntry
    {

        public RspCollector coll = null;

        public RequestEntry(RspCollector coll)
        {
            this.coll = coll;
        }
    }

    private void ReceiveLocalResponse(Message rsp, long req_id)
    {
        RspCollector coll = findEntry(req_id);
        if (coll != null)
        {
            coll.receiveResponse(rsp);
        }
    }

 
    /**
     * The runnable for an incoming request which is submitted to the dispatcher
     */
    private static class Request implements IThreadRunnable
    {

        private RequestCorrelator enclosingInstance;

        public final RequestCorrelator getEnclosing_Instance()
        {
            return enclosingInstance;
        }
        public Message req;

        public Request(RequestCorrelator enclosingInstance, Message req)
        {
            this.enclosingInstance = enclosingInstance;
            this.req = req;
        }

        public void Run()
        {
            getEnclosing_Instance().handleRequest(req, null);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            if (req != null)
            {
                sb.append("req=" + req + ", headers=" + Global.CollectionToString(req.getHeaders()));
            }
            return sb.toString();
        }
    }

   

}
