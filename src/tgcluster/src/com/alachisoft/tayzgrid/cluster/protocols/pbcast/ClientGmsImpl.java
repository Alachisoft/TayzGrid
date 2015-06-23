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

import com.alachisoft.tayzgrid.cluster.util.Util;
import com.alachisoft.tayzgrid.cluster.protocols.PingRsp;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.ViewId;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.threading.Promise;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.enums.Priority;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// $Id: ClientGmsImpl.java,v 1.12 2004/09/08 09:17:17 belaban Exp $
/**
 * Client part of GMS. Whenever a new member wants to join a group, it starts in the CLIENT role. No multicasts to the group will be received and processed until the member has
 * been joined and turned into a SERVER (either coordinator or participant, mostly just participant). This class only implements
 * <code>Join</code> (called by clients who want to join a certain group, and
 * <code>ViewChange</code> which is called by the coordinator that was contacted by this client, to tell the client what its initial membership is.
 *
 * <author> Bela Ban </author> <version> $Revision: 1.12 $ </version>
 */
public class ClientGmsImpl extends GmsImpl
{

    public java.util.List initial_mbrs = Collections.synchronizedList(new java.util.ArrayList(11));
    public boolean initial_mbrs_received = false;
    public Object view_installation_mutex = new Object();
    public Promise join_promise = new Promise();

    public ClientGmsImpl(GMS g)
    {
        gms = g;
    }

    /**
     * Joins this process to a group. Determines the coordinator and sends a unicast handleJoin() message to it. The coordinator returns a JoinRsp and then broadcasts the new view,
     * which contains a message digest and the current membership (including the joiner). The joiner is then supposed to install the new view and the digest and starts accepting
     * mcast messages. Previous mcast messages were discarded (this is done in PBCAST).<p> If successful, impl is changed to an instance of ParticipantGmsImpl. Otherwise, we
     * continue trying to send join() messages to	the coordinator, until we succeed (or there is no member in the group. In this case, we create our own singleton group). <p>When
     * GMS.disable_initial_coord is set to true, then we won't become coordinator on receiving an initial membership of 0, but instead will retry (forever) until we get an initial
     * membership of > 0.
     *
     * @param mbr Our own address (assigned through SET_LOCAL_ADDRESS)
     *
     */
    @Override
    public void join(Address mbr, boolean isStartedAsMirror)
    {
        Address coord = null;
        Address last_tried_coord = null;
        JoinRsp rsp = null;
        Digest tmp_digest = null;
        leaving = false;
        int join_retries = 1;

       

        join_promise.Reset();
        while (!leaving)
        {
            findInitialMembers();
             

            gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "initial_mbrs are " + Global.CollectionToString(initial_mbrs));
            if (initial_mbrs.isEmpty())
            {
                if (gms.disable_initial_coord)
                {
                    gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "received an initial membership of 0, but cannot become coordinator (disable_initial_coord="
                            + gms.disable_initial_coord + "), will retry fetching the initial membership");
                    continue;
                }
                gms.getStack().getCacheLog().CriticalInfo("pb.ClientGmsImpl.join()", "no initial members discovered: creating group as first member");
                
                becomeSingletonMember(mbr);
                 return;
            }

            coord = determineCoord(initial_mbrs);
            if (coord == null)
            {
                gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", "could not determine coordinator from responses " + Global.CollectionToString(initial_mbrs));
                continue;
            }
            if (coord.compareTo(gms.local_addr) == 0)
            {
                gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", "coordinator anomaly. More members exist yet i am the coordinator "
                        + Global.CollectionToString(initial_mbrs));
               
                java.util.ArrayList members = new java.util.ArrayList();
                for (int i = 0; i < initial_mbrs.size(); i++)
                {
                    PingRsp ping_rsp = (PingRsp) initial_mbrs.get(i);
                    if (ping_rsp.getOwnAddress() != null && gms.local_addr != null && !ping_rsp.getOwnAddress().equals(gms.local_addr))
                    {
                        members.add(ping_rsp.getOwnAddress());
                    }
                }
               
                gms.InformOthersAboutCoordinatorDeath(members, coord);
                
                if (last_tried_coord == null)
                {
                    last_tried_coord = coord;
                }
                else
                {
                    if (last_tried_coord.equals(coord))
                    {
                        join_retries++;
                    }
                    else
                    {
                        last_tried_coord = coord;
                        join_retries = 1;
                    }
                }
                try
                {
                    Util.sleep(gms.join_timeout);
                }
                catch (InterruptedException ex)
                {
                   if(gms.getStack().getCacheLog().getIsErrorEnabled())
                   {
                       gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", ex.toString());
                   
                   }
                }
                continue;
               
            }

            try
            {
                gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "sending handleJoin(" + mbr + ") to " + coord);

                if (last_tried_coord == null)
                {
                    last_tried_coord = coord;
                }
                else
                {
                    if (last_tried_coord.equals(coord))
                    {
                        join_retries++;
                    }
                    else
                    {
                        last_tried_coord = coord;
                        join_retries = 1;
                    }
                }
  
                sendJoinMessage(coord, mbr, gms.subGroup_addr, isStartedAsMirror);
          
                rsp = (JoinRsp) join_promise.WaitResult(gms.join_timeout);
                
                gms._doReDiscovery = false; //block the re-discovery of members as we have found initial members

                if (rsp == null)
                {
                    if (join_retries >= gms.join_retry_count)
                    {
                        gms.getStack().getCacheLog().Error("ClientGmsImpl.Join", "received no joining response after " + join_retries + " tries, so becoming a singlton member");
                        becomeSingletonMember(mbr);
                    
                        return;
                    }
                    else
                    {
                        //I did not receive join response, so there is a chance that coordinator is down
                        //Lets verifiy it.
                        if (gms.VerifySuspect(coord, false))
                        {
                            if (gms.getStack().getCacheLog().getIsErrorEnabled())
                            {
                                gms.getStack().getCacheLog().CriticalInfo("ClientGmsImpl.Join()", "selected coordinator " + coord + " seems down; Lets inform others");
                            }
                            //Coordinator is not alive;Lets inform the others
                            java.util.ArrayList members = new java.util.ArrayList();
                            for (int i = 0; i < initial_mbrs.size(); i++)
                            {
                                PingRsp ping_rsp = (PingRsp) initial_mbrs.get(i);

                                if (ping_rsp.getOwnAddress() != null && gms.local_addr != null && !ping_rsp.getOwnAddress().equals(gms.local_addr))
                                {
                                    members.add(ping_rsp.getOwnAddress());
                                }
                            }
                            gms.InformOthersAboutCoordinatorDeath(members, coord);
                        }
                    }
                    gms.getStack().getCacheLog().Error("ClientGmsImpl.Join()", "handleJoin(" + mbr + ") failed, retrying; coordinator:" + coord + " ;No of retries : "
                            + (join_retries
                            + 1));

                }
                else
                {
                    if (rsp.getJoinResult() == JoinResult.Rejected)
                    {
                        gms.getStack().getCacheLog().Error("ClientGmsImpl.Join", "joining request rejected by coordinator");
                        becomeSingletonMember(mbr);
                      
                        return;
                    }

                    if (rsp.getJoinResult() == JoinResult.MembershipChangeAlreadyInProgress)
                    {
                        gms.getStack().getCacheLog().CriticalInfo("Coord.CheckOwnClusterHealth", "Reply: JoinResult.MembershipChangeAlreadyInProgress");
                        Util.sleep(gms.join_timeout);
                        continue;
                    }

                    gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "Join successfull");

                    // 1. Install digest
                    tmp_digest = rsp.getDigest();
                    if (tmp_digest != null)
                    {
                        tmp_digest.incrementHighSeqno(coord); // see DESIGN for an explanantion
                        gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "digest is " + tmp_digest);
                        gms.setDigest(tmp_digest);
                    }
                    else
                    {
                        gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", "digest of JOIN response is null");
                    }

                    // 2. Install view
                    gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "[" + gms.local_addr + "]: JoinRsp=" + rsp.getView() + " [size=" + rsp.getView().size() + "]\n\n");

                    if (rsp.getView() != null)
                    {

                        if (!installView(rsp.getView()))
                        {
                            gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", "view installation failed, retrying to join group");
                            continue;
                        }
                        gms.getStack().setIsOperational(true);
                        return;
                    }
                    else
                    {
                        gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", "view of JOIN response is null");
                    }
                }
            }
            catch (Exception e)
            {
                gms.getStack().getCacheLog().Error("ClientGmsImpl.join()", e.getMessage() + ", retrying");
            }
            try
            {
                Util.sleep(gms.join_retry_timeout);
            }
            catch (InterruptedException ex)
            {
                gms.getStack().getCacheLog().Error("ClientGmsImpl.join()", ex.toString());
            }
        }

        
    }

    @Override
    public void leave(Address mbr)
    {
        leaving = true;
        wrongMethod("leave");
    }

    @Override
    public void handleJoinResponse(JoinRsp join_rsp)
    {
        join_promise.SetResult(join_rsp); // will wake up join() method
    }

    @Override
    public void handleLeaveResponse()
    {
        ; // safely ignore this
    }

    @Override
    public void suspect(Address mbr)
    {
        wrongMethod("suspect");
    }

    @Override
    public void unsuspect(Address mbr)
    {
        wrongMethod("unsuspect");
    }

    @Override
    public JoinRsp handleJoin(Address mbr, String subGroup_name, boolean isStartedAsMirror, String gmsId, tangible.RefObject<Boolean> acquireHashmap)
    {
        wrongMethod("handleJoin");
        return null;
    }

    /**
     * Returns false. Clients don't handle leave() requests
     */
    @Override
    public void handleLeave(Address mbr, boolean suspected)
    {
        wrongMethod("handleLeave");
    }

    /**
     * Does nothing. Discards all views while still client.
     */
    @Override
    public void handleViewChange(View new_view, Digest digest)
    {
        synchronized (this)
        {
            gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.handleViewChange()", "view " + Global.CollectionToString(new_view.getMembers())
                    + " is discarded as we are not a participant");
        }
        gms.passDown(new Event(Event.VIEW_CHANGE_OK, new Object(), Priority.Critical));
    }

    /**
     * Called by join(). Installs the view returned by calling Coord.handleJoin() and becomes coordinator.
     */
    private boolean installView(View new_view)
    {
        java.util.List mems = new_view.getMembers();
        gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.installView()", "new_view=" + new_view);
        if (gms.local_addr == null || mems == null || !mems.contains(gms.local_addr))
        {
            gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.installView()", "I (" + gms.local_addr + ") am not member of " + Global.CollectionToString(mems)
                    + ", will not install view");
            return false;
        }
        gms.installView(new_view);
        gms.becomeParticipant();
        gms.getStack().setIsOperational(true);
        return true;
    }

    /**
     * Returns immediately. Clients don't handle suspect() requests
     */
    @Override
    public void handleSuspect(Address mbr)
    {
        wrongMethod("handleSuspect");
        return;
    }

    /**
     * Informs the coodinator about the nodes to which this node can not establish connection on receiving the first view. Only the node who has most recently joined the cluster
     * should inform the coodinator other nodes will neglect this event.
     *
     * @param nodes
     */
    @Override
    public void handleConnectionFailure(java.util.ArrayList nodes)
    {
        if (nodes != null && nodes.size() > 0)
        {
            if (gms.getStack().getCacheLog().getIsInfoEnabled())
            {
                gms.getStack().getCacheLog().Info("ClientGmsImp.handleConnectionFailure", "informing coordinator about connection failure with ["
                        + Global.CollectionToString(nodes)
                        + "]");
            }
            GMS.HDR header = new GMS.HDR(GMS.HDR.CAN_NOT_CONNECT_TO);
            header.nodeList = nodes;
            Message msg = new Message(gms.determineCoordinator(), null, new byte[0]);
            msg.putHeader(HeaderType.GMS, header);
            gms.passDown(new Event(Event.MSG, msg, Priority.Critical));
        }

    }

    @Override
    public boolean handleUpEvent(Event evt)
    {
        java.util.List tmp;

        switch (evt.getType())
        {
            case Event.FIND_INITIAL_MBRS_OK:
                tmp = (java.util.List) evt.getArg();
                synchronized (initial_mbrs)
                {
                    if (tmp != null && tmp.size() > 0)
                    {
                        for (int i = 0; i < tmp.size(); i++)
                        {
                            initial_mbrs.add(tmp.get(i));
                        }
                    }
                    initial_mbrs_received = true;
                 
                    com.alachisoft.tayzgrid.common.threading.Monitor.pulse(initial_mbrs);// initial_mbrs.notify();
                }
                return false; // don't pass up the stack
        }
        return true;
    }

    /*
     * --------------------------- Private Methods ------------------------------------
     */
    public void sendJoinMessage(Address coord, Address mbr, String subGroup_name, boolean isStartedAsMirror)
    {
        Message msg;
        GMS.HDR hdr;

        msg = new Message(coord, null, null);
        hdr = new GMS.HDR(GMS.HDR.JOIN_REQ, mbr, subGroup_name, isStartedAsMirror, gms.getPartitionId());
        hdr.setGMSId(gms.unique_id);
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG_URGENT, msg, Priority.Critical));
    }

    public void sendSpeicalJoinMessage(Address mbr, java.util.ArrayList dests)
    {
        Message msg;
        GMS.HDR hdr;

        msg = new Message(null, null, new byte[0]);
        msg.setDests(dests);
        hdr = new GMS.HDR(GMS.HDR.SPECIAL_JOIN_REQUEST, mbr);
        hdr.setGMSId(gms.unique_id);
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG_URGENT, msg, Priority.Critical));
    }

    /**
     * Pings initial members. Removes self before returning vector of initial members. Uses IP multicast or gossiping, depending on parameters.
     */
    public void findInitialMembers()
    {
        PingRsp ping_rsp;

        synchronized (initial_mbrs)
        {
            initial_mbrs.clear();
            initial_mbrs_received = false;
            gms.passDown(new Event(Event.FIND_INITIAL_MBRS));

            // the initial_mbrs_received flag is needed when passDown() is executed on the same thread, so when
            // it returns, a response might actually have been received (even though the initial_mbrs might still be empty)
            if (initial_mbrs_received == false)
            {
                try
                {
                     
                    
                    com.alachisoft.tayzgrid.common.threading.Monitor.wait(initial_mbrs);//initial_mbrs.wait();
                }
                catch (Exception ex)
                {
                    gms.getStack().getCacheLog().Error("ClientGmsImpl.findInitialMembers", ex.getMessage());
                }
            }

            for (int i = 0; i < initial_mbrs.size(); i++)
            {

                ping_rsp = (PingRsp) initial_mbrs.get(i);
                if (ping_rsp.getOwnAddress() != null && gms.local_addr != null && ping_rsp.getOwnAddress().equals(gms.local_addr))
                {
                    
                    break;
                }
                if (!ping_rsp.getIsStarted())
                {
                    initial_mbrs.remove(i);
                }
            }
        }
    }

    /**
     * The coordinator is determined by a majority vote. If there are an equal number of votes for more than 1 candidate, we determine the winner randomly.
     *
     * This is bad!. I've changed the election process altogether. I guess i'm the new pervez musharaf here Let everyone cast a vote and unlike the non-deterministic coordinator
     * selection process of jgroups. Ours is a deterministic one. First we find members with most vote counts. If there is a tie member with lowest IP addres wins, if there is tie
     * again member with low port value wins.
     *
     * This algortihm is determistic and ensures same results on every node icluding the coordinator. ()
     */
    public Address determineCoord(java.util.List mbrs)
    {
        if (mbrs == null || mbrs.size() < 1)
        {
            return null;
        }

        Address winner = null;
        int max_votecast = 0;
        // Hastable is synchronized by default
        java.util.HashMap votes = new java.util.HashMap(11);
        for (int i = 0; i < mbrs.size(); i++)
        {
            PingRsp mbr = (PingRsp) mbrs.get(i);
            if (mbr.getCoordAddress() != null)
            {
                if (!votes.containsKey(mbr.getCoordAddress()))
                {
                    votes.put(mbr.getCoordAddress(), mbr.getHasJoined() ? 1000 : 1);
                }
                else
                {
                    int count = ((Integer) votes.get(mbr.getCoordAddress()));
                    votes.put(mbr.getCoordAddress(), (int) (count + 1));
                }

                /**
                 * Find the maximum vote cast value. This will be used to resolve a tie later on. ()
                 */
                if (((Integer) votes.get(mbr.getCoordAddress())) > max_votecast)
                {
                    max_votecast = ((Integer) votes.get(mbr.getCoordAddress()));
                }

                gms.getStack().getCacheLog().CriticalInfo("pb.ClientGmsImpl.determineCoord()", "Owner " + mbr.getOwnAddress() + " -- CoordAddress " + mbr.getCoordAddress()
                        + " -- Vote " + (Integer) votes.get(mbr.getCoordAddress()));

                if ((mbr.getOwnAddress().getIpAddress().equals(gms.local_addr.getIpAddress())) && (mbr.getOwnAddress().getPort() < gms.local_addr.getPort()))
                {
                    gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.determineCoord()", "WINNER SET TO ACTIVE NODE's Coord = " + String.valueOf(mbr.getCoordAddress()));
                    winner = mbr.getCoordAddress();
                }
            }
        }



        /**
         * Collect all the candidates with the highest but similar vote count. Ideally there should only be one. ()
         */
        java.util.ArrayList candidates = new java.util.ArrayList(votes.size());
        for (Iterator it = votes.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry e = (Map.Entry) it.next();
            if (((Integer) e.getValue()) == max_votecast)
            {
                candidates.add(e.getKey());
            }
        }

        Collections.sort(candidates);
        if (winner == null)
        {
            winner = (Address) candidates.get(0);
        }

        if (candidates.size() > 1)
        {
            gms.getStack().getCacheLog().Warn("pb.ClientGmsImpl.determineCoord()", "there was more than 1 candidate for coordinator: " + Global.CollectionToString(candidates));
        }
        gms.getStack().getCacheLog().CriticalInfo("pb.ClientGmsImpl.determineCoord()", "election winner: " + winner + " with votes " + max_votecast);


        /*
         * // determine who got the most votes int most_votes; Address winner = null, tmp; most_votes = 0; for (IEnumerator e = votes.Keys.GetEnumerator(); e.MoveNext(); ) { tmp =
         * (Address) e.Current; int count = ((System.Int32) votes[tmp]); if (count > most_votes) { winner = tmp; most_votes = count; } } votes.Clear();
         */

        return winner;
    }

    public void becomeSingletonMember(Address mbr)
    {
        Digest initial_digest;
        ViewId view_id = null;
        java.util.List mbrs = Collections.synchronizedList(new java.util.ArrayList(1));

        // set the initial digest (since I'm the first member)
        initial_digest = new Digest(1); // 1 member (it's only me)
        initial_digest.add(gms.local_addr, 0, 0); // initial seqno mcast by me will be 1 (highest seen +1)
        gms.setDigest(initial_digest);

        view_id = new ViewId(mbr); // create singleton view with mbr as only member
        mbrs.add(mbr);

        View v = new View(view_id, mbrs);
        v.addPartition(gms.local_addr, gms.getPartitionId());
        v.setCoordinatorGmsId(gms.unique_id);
        java.util.ArrayList subgroupMbrs = new java.util.ArrayList();
        subgroupMbrs.add(mbr);
        gms._subGroupMbrsMap.put(gms.subGroup_addr, subgroupMbrs);
        gms._mbrSubGroupMap.put(mbr, gms.subGroup_addr);
        Object tempVar = gms._subGroupMbrsMap.clone();
        v.setSequencerTbl((java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null));
        Object tempVar2 = gms._mbrSubGroupMap.clone();
        v.setMbrsSubgroupMap((java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null));
        v.AddGmsId(mbr, gms.unique_id);
       
        gms.installView(v);
        gms.becomeCoordinator(); // not really necessary - installView() should do it

        gms.getStack().setIsOperational(true);
        gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.becomeSingletonMember()", "created group (first member). My view is " + gms.view_id + ", impl is "
                + gms.getImpl().getClass().getName());

    }

    @Override
    public boolean getisInStateTransfer()
    {
        return false;
    }
}
