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
import com.alachisoft.tayzgrid.cluster.Membership;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.ViewId;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.threading.Promise;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.GenericCopier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

// $Id: ParticipantGmsImpl.java,v 1.7 2004/07/28 22:46:59 belaban Exp $
public class ParticipantGmsImpl extends GmsImpl {
    
    public java.util.List suspected_mbrs = Collections.synchronizedList(new java.util.ArrayList(11));
    public Promise leave_promise = new Promise();
    private boolean isNewMember = true;
    public java.util.List initial_mbrs = Collections.synchronizedList(new java.util.ArrayList(11));
    public boolean initial_mbrs_received = false;
    public Promise join_promise = new Promise();
    private boolean _deathVerificationInProgress;
    private Object _deathVerSyncLock = new Object();
    
    public ParticipantGmsImpl(GMS g) {
        gms = g;
        suspected_mbrs.clear();
    }
    
    @Override
    public void join(Address mbr, boolean isStartedAsMirror) {
        wrongMethod("join");
    }

    /**
     * Loop: determine coord. If coord is me --> handleLeave(). Else send
     * handleLeave() to coord until success
     */
    @Override
    public void leave(Address mbr) {
        Address coord;
        int max_tries = 3;
        Object result;
        
        leave_promise.Reset();
        
        if (mbr.equals(gms.local_addr)) {
            leaving = true;
        }
        
        while ((coord = gms.determineCoordinator()) != null && max_tries-- > 0) {
            if (gms.local_addr.equals(coord)) {
                // I'm the coordinator
                gms.becomeCoordinator();
                gms.getImpl().handleLeave(mbr, false); // regular leave
                return;
            }
            
            gms.getStack().getCacheLog().Debug("sending LEAVE request to " + coord);
            
            sendLeaveMessage(coord, mbr);
            synchronized (leave_promise) {
                result = leave_promise.WaitResult(gms.leave_timeout);
                if (result != null) {
                    break;
                }
            }
        }
        gms.becomeClient();
    }
    
    @Override
    public void handleJoinResponse(JoinRsp join_rsp) {
        join_promise.SetResult(join_rsp); // will wake up join() method
        gms.getStack().getCacheLog().CriticalInfo("CoordGMSImpl.handleJoin called at startup");
        wrongMethod("handleJoinResponse");
    }
    
    @Override
    public void handleNotifyLeaving() {
        leaving = true;
    }

    @Override
    public void handleLeaveResponse() {
        if (leave_promise == null) {
            gms.getStack().getCacheLog().Error("ParticipantGmsImpl.handleLeaveResponse", "leave_promise is null.");
            return;
        }
        synchronized (leave_promise) {
            leave_promise.SetResult((Object) true); // unblocks thread waiting in leave()
        }
    }
    
    @Override
    public void suspect(Address mbr) throws InterruptedException {
        handleSuspect(mbr);
    }

    /**
     * Removes previously suspected member from list of currently suspected
     * members
     */
    @Override
    public void unsuspect(Address mbr) {
        if (mbr != null) {
            suspected_mbrs.remove(mbr);
        }
    }
    
    @Override
    public JoinRsp handleJoin(Address mbr, String subGroup_name, boolean isStartedAsMirror, String gmsId, tangible.RefObject<Boolean> acquireHashmap) {
        wrongMethod("handleJoin");
        return null;
    }
    
    @Override
    public void handleLeave(Address mbr, boolean suspected) {
        wrongMethod("handleLeave");
    }

    /**
     * If we are leaving, we have to wait for the view change (last msg in the
     * current view) that excludes us before we can leave.
     *
     * @param new_view The view to be installed
     *
     * @param digest If view is a MergeView, digest contains the seqno digest of
     * all members and has to be set by GMS
     *
     */
    @Override
    public void handleViewChange(View new_view, Digest digest) {
        if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
            gms.getStack().getCacheLog().Info("ParticipentGMSImpl.handleViewChange", "received view");
        }
        java.util.List mbrs = new_view.getMembers();
        gms.getStack().getCacheLog().Debug("view="); // + new_view);
        suspected_mbrs.clear();
        if (leaving && !mbrs.contains(gms.local_addr)) {
            // received a view in which I'm not member: ignore
            return;
        }
        
        ViewId vid = gms.view_id != null ? gms.view_id.Copy() : null;
        if (vid != null) {
            int rc = vid.compareTo(new_view.getVid());
            if (rc < 0) {
                isNewMember = false;
                if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                    gms.getStack().getCacheLog().Info("ParticipantGmsImp", "isNewMember : " + isNewMember);
                }
            }
        }
        gms.installView(new_view, digest);
    }
    
    @Override
    public void handleInformAboutNodeDeath(Address sender, Address deadNode) {
        synchronized (_deathVerSyncLock) {
            if (_deathVerificationInProgress) {
                if (gms.getStack().getCacheLog().getIsErrorEnabled()) {
                    gms.getStack().getCacheLog().CriticalInfo("ParticipantGmsImp.handleInformAboutNodeDeath", "verification already in progress");
                }
                return;
            } else {
                _deathVerificationInProgress = true;
            }
        }
        
        java.util.HashMap nodeGMSIds = new java.util.HashMap();
        
        try {
            java.util.ArrayList suspectedMembers = new java.util.ArrayList();
            
            if (gms.getStack().getCacheLog().getIsErrorEnabled()) {
                gms.getStack().getCacheLog().CriticalInfo("ParticipantGmsImp.handleInformAboutNodeDeath", sender + " reported node " + deadNode + " as down");
            }
            
            if (gms.members.contains(deadNode)) {
                java.util.List members = gms.members.getMembers();
                
                for (int i = 0; i < members.size(); i++) {
                    Address mbr = (Address) ((members.get(i) instanceof Address) ? members.get(i) : null);
                    String gmsId = gms.GetNodeGMSId(mbr);
                    if (gmsId != null) {
                        nodeGMSIds.put(mbr, gmsId);
                    }
                }

                //Verify connectivity of over all cluster;
                for (int i = 0; i < members.size(); i++) {
                    
                    Address mbr = (Address) ((members.get(i) instanceof Address) ? members.get(i) : null);
                    
                    if (mbr.equals(gms.local_addr)) {
                        break;
                    }
                    
                    Address sameNode = null;
                    
                    if (gms.isPartReplica) {
                        Membership mbrShip = gms.members.copy();
                        
                        if (mbrShip.contains(mbr)) {
                            String mbrPartitionId = gms.getPartition(mbr);
                            for (int j = 0; j < mbrShip.size(); j++) {
                                Address other = mbrShip.elementAt(j);
                                if (other != null && !other.equals(mbr)) {
                                    String otherMbrPartitionId = gms.getPartition(other);
                                    if (otherMbrPartitionId != null && mbrPartitionId != null && otherMbrPartitionId.equals(mbrPartitionId)) {
                                        sameNode = other;
                                        break;
                                    }
                                }
                            }
                        }

                        //In case, if any other of either main/replica is veriifed dead, we consider the other one to be dead as well.
                        if (sameNode != null && suspectedMembers.contains(sameNode) && !suspectedMembers.contains(mbr)) {
                            suspectedMembers.add(mbr);
                            continue;
                        }
                    }
                    
                    if (gms.VerifySuspect(mbr)) {
                        if (gms.getStack().getCacheLog().getIsErrorEnabled()) {
                            gms.getStack().getCacheLog().CriticalInfo("ParticipantGmsImp.handleInformAboutNodeDeath", "verification of member down for " + mbr);
                        }
                        suspectedMembers.add(mbr);
                    } else {
                        if (gms.getStack().getCacheLog().getIsErrorEnabled()) {
                            gms.getStack().getCacheLog().CriticalInfo("ParticipantGmsImp.handleInformAboutNodeDeath", mbr + " is up and running");
                        }
                        break;
                    }
                }
            } else {
                if (gms.getStack().getCacheLog().getIsErrorEnabled()) {
                    gms.getStack().getCacheLog().CriticalInfo("ParticipantGmsImp.handleInformAboutNodeDeath", "node is not part of members ");
                }
            }
            for (Iterator it = suspectedMembers.iterator(); it.hasNext();) {
                Address suspectedMbr = (Address) it.next();
                String currentGmsId = gms.GetNodeGMSId(suspectedMbr);
                String reportedGmsId = (String) ((nodeGMSIds.get(suspectedMbr) instanceof String) ? nodeGMSIds.get(suspectedMbr) : null);
                if (currentGmsId != null && reportedGmsId != null && currentGmsId.equals(reportedGmsId)) {
                    handleSuspect(suspectedMbr);
                }
            }
        } finally {
            synchronized (_deathVerSyncLock) {
                _deathVerificationInProgress = false;
            }
        }
        
    }
    
    @Override
    public void handleSuspect(Address mbr) {
        
        java.util.List suspects = null;
        
        synchronized (this) {
            if (mbr == null) {
                return;
            }
            if (!suspected_mbrs.contains(mbr)) {
                Address sameNode = null;
                
                if (gms.isPartReplica) {
                    Membership mbrShip = gms.members.copy();
                    
                    if (mbrShip.contains(mbr)) {
                        String mbrPartitionId = gms.getPartition(mbr);
                        
                        for (int i = 0; i < mbrShip.size(); i++) {
                            Address other = mbrShip.elementAt(i);
                            if (other != null && !other.equals(mbr)) {
                                String otherPartitionId = gms.getPartition(other);
                                    if (otherPartitionId != null && mbrPartitionId!=null && otherPartitionId.equals(mbrPartitionId)){
                                    sameNode = other;
                                    break;
                                }
                            }
                        }
                    }
                }
                
                if (sameNode != null && !sameNode.getIpAddress().equals(gms.local_addr.getIpAddress())) {
                    if (sameNode.getPort() > mbr.getPort()) {
                        suspected_mbrs.add(sameNode);
                        suspected_mbrs.add(mbr);
                    } else {
                        suspected_mbrs.add(mbr);
                        suspected_mbrs.add(sameNode);
                    }
                } else {
                    suspected_mbrs.add(mbr);
                }
            }
            
            if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                gms.getStack().getCacheLog().Info("suspected mbr=" + mbr + ", suspected_mbrs=" + Global.CollectionToString(suspected_mbrs));
            }
            
            if (!leaving) {
                if (wouldIBeCoordinator() && !gms.getIsCoordinator()) {
                    suspects = (java.util.List) GenericCopier.DeepCopy(suspected_mbrs);//.clone();
                    suspected_mbrs.clear();
                    gms.becomeCoordinator();
                    for (Iterator it = suspects.iterator(); it.hasNext();) {
                        Address leavingMbr = (Address) it.next();
                        if (!gms.members.getMembers().contains(leavingMbr)) {
                            gms.getStack().getCacheLog().Debug("pbcast.PariticipantGmsImpl.handleSuspect()", "mbr " + leavingMbr + " is not a member !");
                            continue;
                        }
                        if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                            gms.getStack().getCacheLog().Info("suspected mbr=" + leavingMbr + "), members are " + gms.members + ", coord=" + gms.local_addr
                                    + ": I'm the new coord !");
                        }
                        String subGroup = (String) gms._mbrSubGroupMap.get(leavingMbr);
                        if (subGroup != null) {
                            synchronized (gms._mbrSubGroupMap) {
                                gms._mbrSubGroupMap.remove(leavingMbr);
                            }
                            synchronized (gms._subGroupMbrsMap) {
                                java.util.List subGroupMbrs = (java.util.List) gms._subGroupMbrsMap.get(subGroup);
                                if (subGroupMbrs != null) {
                                    subGroupMbrs.remove(leavingMbr);
                                    if (subGroupMbrs.isEmpty()) {
                                        gms._subGroupMbrsMap.remove(subGroup);
                                    }
                                }
                            }
                        }
                        java.util.ArrayList list = new java.util.ArrayList(1);
                        list.add(leavingMbr);
                        String partitionId= gms.getPartition(leavingMbr);
                        gms.acquireHashmap(list, false, subGroup, false, partitionId);
                    }

                    //Throws interrupted exception, to be handled or to be thrown
                    gms.castViewChange(null, null, suspects, gms._hashmap);
                } else {
                    if (gms.getIsCoordinator()) {
                        sendMemberLeftNotificationToCoordinator(mbr, gms.local_addr);
                    } else {
                        sendMemberLeftNotificationToCoordinator(mbr, gms.determineCoordinator());
                    }
                }
            }
        }
    }
    
    private void sendMemberLeftNotificationToCoordinator(Address suspected, Address coordinator) {
        if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
            gms.getStack().getCacheLog().Info("ParticipantGmsImp.sendMemberLeftNotification", "informing coordinator about abnormal connection breakage with " + suspected);
        }
        
        GMS.HDR hdr = new GMS.HDR(GMS.HDR.CONNECTION_BROKEN, suspected);
        Message nodeLeftMsg = new Message(coordinator, null, new byte[0]);
        nodeLeftMsg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG, nodeLeftMsg, Priority.Critical));
    }
    
    @Override
    public void handleConnectedNodesRequest(Address src, int reqId) {
        if (gms.determineCoordinator().equals(src)) {
            java.util.List mbrs = gms.members.getMembers();
            Object tempVar = GenericCopier.DeepCopy(suspected_mbrs);//.clone();
            java.util.ArrayList suspected = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
            for (Iterator it = suspected_mbrs.iterator(); it.hasNext();) {
                Address suspect = (Address) it.next();
                mbrs.remove(suspect);
            }
            
            if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                gms.getStack().getCacheLog().Info("ParticipantGmsImp.handleConnectedNodesRequest    " + gms.local_addr + " --> " + Global.ArrayListToString(mbrs));
            }
            
            Message rspMsg = new Message(src, null, new byte[0]);
            GMS.HDR hdr = new GMS.HDR(GMS.HDR.CONNECTED_NODES_RESPONSE, (Object) (Integer) reqId);
            hdr.nodeList = mbrs;
            rspMsg.putHeader(HeaderType.GMS, hdr);
            gms.passDown(new Event(Event.MSG, rspMsg, Priority.Critical));
        }
    }

    /**
     * Informs the coodinator about the nodes to which this node can not
     * establish connection on receiving the first view.Only the node who has
     * most recently joined the cluster should inform the coodinator other nodes
     * will neglect this event.
     *
     * @param nodes
     */
    @Override
    public void handleConnectionFailure(java.util.ArrayList nodes) {
        if (nodes != null && nodes.size() > 0) {
            if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                gms.getStack().getCacheLog().Info("ParticipantGmsImp.handleConnectionFailure", "informing coordinator about connection failure with ["
                        + Global.CollectionToString(nodes) + "]");
            }
            GMS.HDR header = new GMS.HDR(GMS.HDR.CAN_NOT_CONNECT_TO);
            header.nodeList = nodes;
            Message msg = new Message(gms.determineCoordinator(), null, new byte[0]);
            msg.putHeader(HeaderType.GMS, header);
            gms.passDown(new Event(Event.MSG, msg, Priority.Critical));
        }
        
    }
    
    @Override
    public void handleLeaveClusterRequest(Address sender) {
        if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
            gms.getStack().getCacheLog().Info("ParticipantGmsImp.handleLeaveClusterRequest", sender + " has asked me to leave the cluster");
        }
        
        if (gms.determineCoordinator().equals(sender)) {
            if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                gms.getStack().getCacheLog().Info("ParticipantGmsImp.handleLeaveClusterRequest", "leaving the cluster on coordinator request");
            }
            
            java.util.List suspected = gms.members.getMembers();
            suspected.remove(gms.local_addr);
            for (Iterator it = suspected.iterator(); it.hasNext();) {
                Address leavingMbr = (Address) it.next();
                String subGroup = (String) ((gms._mbrSubGroupMap.get(leavingMbr) instanceof String) ? gms._mbrSubGroupMap.get(leavingMbr) : null);
                if (subGroup != null) {
                    synchronized (gms._mbrSubGroupMap) {
                        gms._mbrSubGroupMap.remove(leavingMbr);
                    }
                    
                    synchronized (gms._subGroupMbrsMap) {
                        java.util.ArrayList subGroupMbrs = (java.util.ArrayList) ((gms._subGroupMbrsMap.get(subGroup) instanceof java.util.ArrayList) ? gms._subGroupMbrsMap.get(subGroup) : null);
                        if (subGroupMbrs != null) {
                            subGroupMbrs.remove(leavingMbr);
                            if (subGroupMbrs.isEmpty()) {
                                gms._subGroupMbrsMap.remove(subGroup);
                            }
                        }
                    }
                }
                java.util.ArrayList list = new java.util.ArrayList(1);
                list.add(leavingMbr);
                String partitionId= gms.getPartition(leavingMbr);
                gms.acquireHashmap(list, false, subGroup, false, partitionId);
            }
            
            suspected_mbrs.clear();
            gms.becomeCoordinator();
            
            gms.castViewChange(null, null, suspected, gms._hashmap);
        }
        
    }
    
    @Override
    public void handleNodeRejoining(Address node) {
        if (node != null) {
            if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                gms.getStack().getCacheLog().Info("ParticipantGmsImpl.handleNodeRejoining", "I should inform coordinator about node rejoining with " + node);
            }
            
            if (gms.members.contains(node)) {
                //inform coordinator about the node rejoining in the cluster.
                GMS.HDR header = new GMS.HDR(GMS.HDR.INFORM_NODE_REJOINING, node);
                Message rejoiningMsg = new Message(gms.determineCoordinator(), null, new byte[0]);
                rejoiningMsg.putHeader(HeaderType.GMS, header);
                gms.passDown(new Event(Event.MSG, rejoiningMsg, Priority.Critical));
            }
        }
    }
    
    @Override
    public void handleResetOnNodeRejoining(Address sender, Address node, View view) {
        gms.handleResetOnNodeRejoining(sender, node, view);
    }
    /*
     * ---------------------------------- Private Methods ---------------------------------------
     */

    /**
     * Determines whether this member is the new coordinator given a list of
     * suspected members. This is computed as follows: the list of currently
     * suspected members (suspected_mbrs) is removed from the current
     * membership. If the first member of the resulting list is equals to the
     * local_addr, then it is true, otherwise false. Example: own address is B,
     * current membership is {A, B, C, D}, suspected members are {A, D}. The
     * resulting list is {B, C}. The first member of {B, C} is B, which is equal
     * to the local_addr. Therefore, true is returned.
     */
    public boolean wouldIBeCoordinator() {
        Address new_coord = null;
        java.util.List mbrs = gms.members.getMembers(); // getMembers() returns a *copy* of the membership vector

        for (int i = 0; i < suspected_mbrs.size(); i++) {
            mbrs.remove(suspected_mbrs.get(i));
        }
        
        if (mbrs.size() < 1) {
            return false;
        }
        new_coord = (Address) mbrs.get(0);
        return gms.local_addr.equals(new_coord);
    }
    
    public void sendLeaveMessage(Address coord, Address mbr) {
        Message msg = new Message(coord, null, null);
        GMS.HDR hdr = new GMS.HDR(GMS.HDR.LEAVE_REQ, mbr);
        
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG, msg));
    }

    /*
     * ------------------------------ End of Private Methods ------------------------------------
     */
    @Override
    public void ReCheckClusterHealth(Object mbr) {
        JoinRsp rsp;
        Digest tmp_digest;
        Address coordinator = (Address) mbr;
        
        gms.getStack().getCacheLog().Debug("ReCheck", "Force join cluster: " + mbr.toString());
        int retry_count = 6;
        
        try {
            while (!leaving) {
                
                if (coordinator != null && !coordinator.equals(gms.local_addr)) {
                    sendJoinMessage(coordinator, gms.local_addr, gms.subGroup_addr, true);
                    rsp = (JoinRsp) join_promise.WaitResult(gms.join_timeout * 5);
                } else {
                    retry_count--;
                    if (retry_count <= 0) {
                        return;
                    }
                    continue;
                }
                
                if (rsp == null) {
                    
                    Util.sleep(gms.join_retry_timeout * 5);
                    initial_mbrs_received = false;
                    retry_count--;
                    if (retry_count <= 0) {
                        return;
                    }
                    continue;
                } else if (rsp.getJoinResult() == JoinResult.Rejected) {
                    return;
                } else if (rsp.getJoinResult() == JoinResult.MembershipChangeAlreadyInProgress) {
                    Util.sleep(gms.join_timeout);
                    continue;
                } else {
                    tmp_digest = rsp.getDigest();
                    if (tmp_digest != null) {
                        tmp_digest.incrementHighSeqno(coordinator); // see DESIGN for an explanantion
                        gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "digest is " + tmp_digest);
                        gms.setDigest(tmp_digest);
                    } else {
                        gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", "digest of JOIN response is null");
                    }

                    // 2. Install view
                    gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.join()", "[" + gms.local_addr + "]: JoinRsp=" + rsp.getView() + " [size=" + rsp.getView().size() + "]\n\n");
                    
                    if (rsp.getView() != null) {
                        rsp.getView().setForceInstall(true); //Forces this view for installation
                        if (!installView(rsp.getView())) {
                            gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", "view installation failed, retrying to join group");
                            return;
                        }
                        gms.getStack().setIsOperational(true);
                        return;
                    }
                    return;
                }
            }
        } catch (Exception ex) {
            
            gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.join()", ex.toString());
        }
        
    }
    
    private boolean installView(View new_view) {
        java.util.List mems = new_view.getMembers();
        gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.installView()", "new_view=" + new_view);
        if (gms.local_addr == null || mems == null || !mems.contains(gms.local_addr)) {
            gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.installView()", "I (" + gms.local_addr + ") am not member of " + Global.CollectionToString(mems)
                    + ", will not install view");
            return false;
        }
        
        Address replica = (Address) gms.members.getMembers().get(1);

        //Cast view to the replica node as well
        gms.installView(new_view);
        
        gms.becomeParticipant();
        gms.getStack().setIsOperational(true);
        return true;
    }
    
    private Address determineCoord(java.util.ArrayList mbrs) {
        if (mbrs == null || mbrs.size() < 1) {
            return null;
        }
        
        Address winner = null;
        int max_votecast = 0;

        //HashMap is synced by default
        java.util.HashMap votes = new java.util.HashMap(11);
        for (int i = 0; i < mbrs.size(); i++) {
            PingRsp mbr = (PingRsp) mbrs.get(i);
            if (mbr.getCoordAddress() != null) {
                if (!votes.containsKey(mbr.getCoordAddress())) {
                    votes.put(mbr.getCoordAddress(), mbr.getHasJoined() ? 1000 : 1);
                } else {
                    int count = ((Integer) votes.get(mbr.getCoordAddress()));
                    votes.put(mbr.getCoordAddress(), (int) (count + 1));
                }

                /**
                 * Find the maximum vote cast value. This will be used to
                 * resolve a tie later on. ()
                 */
                if (((Integer) votes.get(mbr.getCoordAddress())) > max_votecast) {
                    max_votecast = ((Integer) votes.get(mbr.getCoordAddress()));
                }
                
                if ((mbr.getOwnAddress().getIpAddress().equals(gms.local_addr.getIpAddress())) && (mbr.getOwnAddress().getPort() < gms.local_addr.getPort())) {
                    gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.determineCoord()", "WINNER SET TO ACTIVE NODE's Coord = " + String.valueOf(mbr.getCoordAddress()));
                    winner = mbr.getCoordAddress();
                    break;
                }
            }
        }
        
        if (winner == null) {
            return null;
        } else {
            return winner;
        }
    }

    /**
     * Pings initial members. Removes self before returning vector of initial
     * members. Uses IP multicast or gossiping, depending on parameters.
     */
    public final java.util.List FindAliveMembers() {
        PingRsp ping_rsp;
        initial_mbrs.clear();
        initial_mbrs_received = false;
        gms.passDown(new Event(Event.FIND_INITIAL_MBRS));

        // the initial_mbrs_received flag is needed when passDown() is executed on the same thread, so when
        // it returns, a response might actually have been received (even though the initial_mbrs might still be empty)
        if (initial_mbrs_received == false) {
            try {
                //Changing pulse to notify
                Monitor.wait(initial_mbrs); //initial_mbrs.wait();
                
            } catch (Exception ex) {
                gms.getStack().getCacheLog().Error("COORDGmsImpl.findInitialMembers", ex.getMessage());
            }
        }
        
        for (int i = 0; i < initial_mbrs.size(); i++) {
            ping_rsp = (PingRsp) initial_mbrs.get(i);
            if (ping_rsp.getOwnAddress() != null && gms.local_addr != null && ping_rsp.getOwnAddress().equals(gms.local_addr)) {
                //initial_mbrs.RemoveAt(i);
                break;
            }
            if (!ping_rsp.getIsStarted()) {
                initial_mbrs.remove(i);
            }
        }
        
        return initial_mbrs;
    }
    
    @Override
    public boolean handleUpEvent(Event evt) {
        java.util.ArrayList tmp;
        
        switch (evt.getType()) {
            case Event.FIND_INITIAL_MBRS_OK:
                tmp = (java.util.ArrayList) evt.getArg();
                synchronized (initial_mbrs) {
                    if (tmp != null && tmp.size() > 0) {
                        for (int i = 0; i < tmp.size(); i++) {
                            initial_mbrs.add(tmp.get(i));
                        }
                    }
                    initial_mbrs_received = true;

                    //Changing pulse to notify
                    Monitor.pulse(initial_mbrs);                    
                }
                return false; // don't pass up the stack
        }
        return true;
    }
    
    public void sendJoinMessage(Address coord, Address mbr, String subGroup_name, boolean isStartedAsMirror) {
        
        Message msg;
        GMS.HDR hdr;
        
        msg = new Message(coord, null, null);
        hdr = new GMS.HDR(GMS.HDR.JOIN_REQ, mbr, subGroup_name, isStartedAsMirror);
        hdr.setGMSId(gms.unique_id);
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG_URGENT, msg, Priority.Critical));
    }
}
