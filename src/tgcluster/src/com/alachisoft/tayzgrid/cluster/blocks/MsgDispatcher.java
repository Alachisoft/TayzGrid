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

import com.alachisoft.tayzgrid.cluster.MessageListener;
import com.alachisoft.tayzgrid.cluster.UpHandler;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.MessageResponder;
import com.alachisoft.tayzgrid.cluster.GroupChannel;
import com.alachisoft.tayzgrid.cluster.ChannelException;
import com.alachisoft.tayzgrid.cluster.Channel;
import com.alachisoft.tayzgrid.cluster.MembershipListener;
import com.alachisoft.tayzgrid.cluster.ChannelClosedException;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.cluster.util.Rsp;
import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.stats.HPTimeStats;
import com.alachisoft.tayzgrid.common.stats.TimeStats;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// $Id: MessageDispatcher.java,v 1.30 2004/09/02 14:00:40 belaban Exp $
public class MsgDispatcher implements RequestHandler, UpHandler
{

    private Channel channel;
    private RequestCorrelator corr;
    private MessageListener msg_listener;
    private MembershipListener membership_listener;
    private RequestHandler _req_handler;
    private java.util.ArrayList _members;
    private MessageResponder _msgResponder;
    private Object _statSync = new Object();
    protected boolean concurrent_processing;
    protected boolean deadlock_detection;
    private java.util.HashMap syncTable = new java.util.HashMap();
    private TimeStats _stats = new TimeStats();
    private long profileId = 0;

    private ILogger _ncacheLog = null;

    public final ILogger getCacheLog()
    {
        return _ncacheLog;
    }
    private HPTimeStats _avgReqExecutionTime = new HPTimeStats();
    private HPTimeStats _operationOnCacheTimeStats = new HPTimeStats();
    private boolean useAvgStats = false;

    public MsgDispatcher(Channel channel, MessageListener l, MembershipListener l2, RequestHandler req_handler, MessageResponder responder)
    {
        this(channel, l, l2, req_handler, responder, false);
    }

    public MsgDispatcher(Channel channel, MessageListener l, MembershipListener l2, RequestHandler req_handler, MessageResponder responder, boolean deadlock_detection)
    {
        this(channel, l, l2, req_handler, responder, deadlock_detection, false);
    }

    public MsgDispatcher(Channel channel, MessageListener l, MembershipListener l2, RequestHandler req_handler, MessageResponder responder, boolean deadlock_detection, boolean concurrent_processing)
    {
        this.channel = channel;
        this._ncacheLog = ((GroupChannel) channel).getCacheLog();
        this.deadlock_detection = deadlock_detection;
        this.concurrent_processing = concurrent_processing;
        msg_listener = l;
        membership_listener = l2;
        _req_handler = req_handler;
        _msgResponder = responder;

        /*
         * prot_adapter = new ProtocolAdapter(this); if (channel != null) { channel.UpHandler = prot_adapter; }
         */
        channel.setUpHandler(this);
        start();
    }

    public void start()
    {
        if (corr == null)
        {
            corr = new RequestCorrelator("MsgDisp", channel, this, deadlock_detection, channel.getLocalAddress(), concurrent_processing, this._ncacheLog);
            corr.start();
            if (ServicePropValues.useAvgStats != null)
            {
                useAvgStats = Boolean.parseBoolean(ServicePropValues.useAvgStats);
            }
        }
    }

    public void stop()
    {
        if (corr != null)
        {
            corr.stop();
        }
    }

    public final void StopReplying()
    {
        if (corr != null)
        {
            corr.StopReplying();
        }
    }

    /**
     * Called by channel (we registered before) when event is received. This is the UpHandler interface.
     */
    public final void up(Event evt)
    {
        try
        {
            if (corr != null)
            {
                
                if (corr.receive(evt))
                {
                    return;
                }
            }

            passUp(evt);
        }
        catch (NullPointerException e)
        {
            int x = 0;
        }
        catch (Exception e)
        {
            getCacheLog().Error("MsgDispatcher.up()", "exception=" + e);
        }
    }

    /**
     * Called by request correlator when message was not generated by it. We handle it and call the message listener's corresponding methods
     */
    protected final void passUp(Event evt) throws Exception
    {
        switch (evt.getType())
        {
            case Event.MSG:
                if (msg_listener != null)
                {
                    HPTimeStats reqHandleStats = null;
                    
                    msg_listener.receive((Message) evt.getArg());

                    if (reqHandleStats != null)
                    {
                        
                        if (!useAvgStats)
                        {
                           
                        }
                        else
                        {
                            
                        }
                    }
                  
                }
                break;

            case Event.HASHMAP_REQ:
                if (_msgResponder != null)
                {
                    getCacheLog().Debug("MessageDispatcher.PassUp()", "here comes the request for hashmap");
                 
                    Object map = null;
                    try
                    {
                        map = _msgResponder.GetDistributionAndMirrorMaps(evt.getArg());
                    }
                    catch (Exception e)
                    {
                        getCacheLog().CriticalInfo("MsgDispatcher.passUP", "An error occured while getting new hashmap. Error: " + e.toString());
                    }
                    Event evnt = new Event();
                    evnt.setType(Event.HASHMAP_RESP);
                    evnt.setArg(map);
                    channel.down(evnt);
                    getCacheLog().Debug("MessageDispatcher.PassUp()", "sending the response for hashmap back...");
                }
                break;

           

            case Event.VIEW_CHANGE:
                View v = (View) evt.getArg();
                java.util.List new_mbrs = v.getMembers();
                if (membership_listener != null)
                {
                    getCacheLog().Debug("MessageDispatcher.passUp", "Event.VIEW_CHANGE-> Entering: " + v.toString());
                    try
                    {
                        membership_listener.viewAccepted(v);
                    }
                    catch (InterruptedException interruptedException)
                    {
                        getCacheLog().Error("MessageDispatcher.passUp", "Event.VIEW_CHANGE->interruptedException " + interruptedException.getMessage());
                    }
                    getCacheLog().Debug("MessageDispatcher.passUp", "Event.VIEW_CHANGE->Done" + v.toString());
                   
                }
                break;
            case Event.ASK_JOIN:
                if (membership_listener != null)
                {
                    Event et = new Event();
                    et.setType(Event.ASK_JOIN_RESPONSE);
                    et.setArg(membership_listener.AllowJoin());
                    channel.down(et);
                    
                }

                break;
            case Event.SET_LOCAL_ADDRESS:
                break;

            case Event.SUSPECT:
                if (membership_listener != null)
                {
                    membership_listener.suspect((Address) evt.getArg());
                   
                }
                break;

            case Event.BLOCK:
                if (membership_listener != null)
                {
                    membership_listener.block();
                    
                }
                break;
        }
    }

    public void send(Message msg) throws ChannelClosedException, ChannelException
    {
        if (channel != null)
        {
            channel.send(msg);
        }
        else
        {
            getCacheLog().Error("channel == null");
        }
    }

    /**
     * Cast a message to all members, and wait for
     * <code>mode</code> responses. The responses are returned in a response list, where each response is associated with its sender.<p> Uses
     * <code>GroupRequest</code>.
     *
     *
     * @param dests The members to which the message is to be sent. If it is null, then the message is sent to all members
     *
     * @param msg The message to be sent to n members
     *
     * @param mode Defined in <code>GroupRequest</code>. The number of responses to wait for: <ol> <li>GET_FIRST: return the first response received. <li>GET_ALL: wait for all
     * responses (minus the ones from suspected members) <li>GET_MAJORITY: wait for a majority of all responses (relative to the grp size) <li>GET_ABS_MAJORITY: wait for majority
     * (absolute, computed once) <li>GET_N: wait for n responses (may block if n > group size) <li>GET_NONE: wait for no responses, return immediately (non-blocking) </ol>
     *
     * @param timeout If 0: wait forever. Otherwise, wait for <code>mode</code> responses <em>or</em> timeout time.
     *
     * @return RspList A list of responses. Each response is an <code>Object</code> and associated to its sender.
     *
     */

    public RspList castMessage(java.util.List dests, Message msg, byte mode, long timeout)
    {
        GroupRequest _req = null;
        java.util.List real_dests;

        java.util.List clusterMembership = channel.getView().getMembers() != null ? (java.util.List) GenericCopier.DeepCopy(channel.getView().getMembers())/*.clone()*/ : null;
        // we need to clone because we don't want to modify the original
        // (we remove ourselves if LOCAL is false, see below) !
        real_dests = dests != null ? (List) GenericCopier.DeepCopy(dests) : clusterMembership;
        //real_dests = dests != null ? (ArrayList) dests.Clone():null;

        // if local delivery is off, then we should not wait for the message from the local member.
        // therefore remove it from the membership
        if (channel != null && channel.getOpt(Channel.LOCAL).equals(false))
        {
            real_dests.remove(channel.getLocalAddress());
        }

        // don't even send the message if the destination list is empty
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("MsgDispatcher.castMessage()", "real_dests=" + Global.CollectionToString(real_dests));
        }

        if (real_dests == null || real_dests.isEmpty())
        {
            if (getCacheLog().getIsInfoEnabled())
            {
                getCacheLog().Info("MsgDispatcher.castMessage()", "destination list is empty, won't send message");
            }
            //((GroupChannel)channel).MsgProvider.SubmittObject(msg);
            return new RspList(); // return empty response list
        }

        _req = new GroupRequest(msg, corr, real_dests, clusterMembership, mode, timeout, 0, this._ncacheLog);


        _req.execute();

        if (mode != GroupRequest.GET_NONE)
        {
            ((GroupChannel) channel).getStack().perfStatsColl.incrementClusteredOperationsPerSecStats();
        }

        RspList rspList = _req.getResults();

        if (rspList != null)
        {
            for (int i = 0; i < rspList.size(); i++)
            {
                Object tempVar = rspList.elementAt(i);
                Rsp rsp = (Rsp) ((tempVar instanceof Rsp) ? tempVar : null);
                if (rsp != null)
                {
                    if (!rsp.wasReceived() && !rsp.wasSuspected())
                    {
                        if (corr.CheckForMembership((Address) rsp.sender))
                        {
                            rsp.suspected = true;
                        }

                    }
                }
            }
        }
        return rspList;
    }

    public final void SendResponse(long resp_id, Message response)
    {
        corr.SendResponse(resp_id, response);
    }

    /**
     * Multicast a message request to all members in
     * <code>dests</code> and receive responses via the RspCollector interface. When done receiving the required number of responses, the caller has to call done(req_id) on the
     * underlyinh RequestCorrelator, so that the resources allocated to that request can be freed.
     *
     *
     * @param dests The list of members from which to receive responses. Null means all members
     *
     * @param req_id The ID of the request. Used by the underlying RequestCorrelator to correlate responses with requests
     *
     * @param msg The request to be sent
     *
     * @param coll The sender needs to provide this interface to collect responses. Call will return immediately if this is null
     *
     */
    public void castMessage(java.util.ArrayList dests, long req_id, Message msg, RspCollector coll)
    {
        java.util.List real_dests;
        if (msg == null)
        {
            getCacheLog().Error("MsgDispatcher.castMessage()", "request is null");
            return;
        }

        if (coll == null)
        {
            getCacheLog().Error("MessageDispatcher.castMessage()", "response collector is null (must be non-null)");
            return;
        }

        real_dests = dests != null ? (java.util.ArrayList) dests.clone() : (java.util.List) GenericCopier.DeepCopy(channel.getView().getMembers());//.clone();

     
        if (channel != null && channel.getOpt(Channel.LOCAL).equals(false))
        {
            real_dests.remove(channel.getLocalAddress());
        }

        // don't even send the message if the destination list is empty
        if (real_dests.isEmpty())
        {
            getCacheLog().Debug("MsgDispatcher.castMessage()", "destination list is empty, won't send message");
            return;
        }

        corr.sendRequest(req_id, real_dests, msg, coll);
    }


    public void done(long req_id)
    {
        corr.done(req_id);
    }

    /**
     * Sends a message to a single member (destination = msg.dest) and returns the response. The message's destination must be non-zero !
     */

    public Object sendMessage(Message msg, byte mode, long timeout) throws TimeoutException, OperationFailedException, SuspectedException
    {
        RspList rsp_list = null;
        Object dest = msg.getDest();
        Rsp rsp;
        GroupRequest _req = null;

        if (dest == null)
        {
            return null;
        }

        List mbrs = Collections.synchronizedList(new ArrayList(1));
        mbrs.add(dest); // dummy membership (of destination address)

        java.util.List clusterMembership = channel.getView().getMembers() != null ? (java.util.List) GenericCopier.DeepCopy(channel.getView().getMembers())/*.clone()*/ : null;

        _req = new GroupRequest(msg, corr, mbrs, clusterMembership, mode, timeout, 0, this._ncacheLog);

        _req.execute();

        if (mode == GroupRequest.GET_NONE)
        {
            return null;
        }
        ((GroupChannel) channel).getStack().perfStatsColl.incrementClusteredOperationsPerSecStats();
        rsp_list = _req.getResults();

        if (rsp_list.size() == 0)
        {
            getCacheLog().Warn("MsgDispatcher.sendMessage()", " response list is empty");
            return null;
        }
        if (rsp_list.size() > 1)
        {
            getCacheLog().Warn("MsgDispatcher.sendMessage()", "response list contains more that 1 response; returning first response !");
        }
        rsp = (Rsp) rsp_list.elementAt(0);
        if (rsp.wasSuspected())
        {
            throw new SuspectedException("operation failed because the group member was suspected:" + dest.toString());
        }
        if (!rsp.wasReceived())
        {
            //we verify for the destination whether it is still part of the cluster or not.
            if (corr.CheckForMembership((Address) rsp.getSender()))

            {
                throw new TimeoutException("operation timeout");
            }

            else
            {
                rsp.suspected = true;
                throw new OperationFailedException(dest.toString());
            }
        }
        return rsp.getValue();
    }


    /*
     * ------------------------ RequestHandler Interface ----------------------
     */
    public Object handle(Message msg)
    {
        if (_req_handler != null)
        {


            Object result = _req_handler.handle(msg);
            return result;
        }
        return null;
    }

    public Object handleNHopRequest(Message msg, tangible.RefObject<Address> destination, tangible.RefObject<Message> replicationMsg)
    {
        destination.argvalue = null;
        replicationMsg.argvalue = null;

        if (_req_handler != null)
        {
            Object result = _req_handler.handleNHopRequest(msg, destination, replicationMsg);
            return result;
        }

        return null;
    }
}
