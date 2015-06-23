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

import com.alachisoft.tayzgrid.cluster.protocols.PingRsp;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Membership;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.MergeView;
import com.alachisoft.tayzgrid.cluster.ViewId;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.threading.Promise;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.cluster.util.Util;
import java.util.*;

// $Id: CoordGmsImpl.java,v 1.13 2004/09/08 09:17:17 belaban Exp $
/**
 * Coordinator role of the Group MemberShip (GMS) protocol. Accepts JOIN and
 * LEAVE requests and emits view changes accordingly.
 *
 * <author> Bela Ban </author>
 */
public class CoordGmsImpl extends GmsImpl {

    private void InitBlock() {
        merge_task = new MergeTask(this);
    }
    public boolean merging = false;
    public MergeTask merge_task;
    public java.util.List merge_rsps = Collections.synchronizedList(new java.util.ArrayList(11));
    // for MERGE_REQ/MERGE_RSP correlation, contains MergeData elements
    public Object merge_id = null;
    private Object connection_break_mutex = new Object(); //synchronizes the thread informing the connection breakage.
    public java.util.List initial_mbrs = Collections.synchronizedList(new java.util.ArrayList(11));
    public boolean initial_mbrs_received = false;
    public Promise join_promise = new Promise();
    public Promise connectedNodesPromise = new Promise();
    // Hastable is synchronized by default
    public java.util.HashMap connectedNodesMap = new java.util.HashMap();
    public int missingResults;
    public int conNodesReqId = 0;
    private java.util.ArrayList viewRejectingMembers = new java.util.ArrayList();

    private static final int MAX_CLUSTER_MBRS = 2;

    public CoordGmsImpl(GMS g) {
        InitBlock();

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(g.getUniqueID())) {

            _uniqueId = UUID.randomUUID().toString();
            g.setUniqueID(_uniqueId);
        } else {
            _uniqueId = g.getUniqueID();
        }

        gms = g;
    }

    @Override
    public void join(Address mbr, boolean isStartedAsMirror) {

    }

    /**
     * The coordinator itself wants to leave the group
     */
    @Override
    public void leave(Address mbr) {
        if (mbr == null) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.leave", "member's address is null !");
            return;
        }
        if (mbr.equals(gms.local_addr)) {
            leaving = true;
        }
        handleLeave(mbr, false); // regular leave
    }

    @Override
    public void handleJoinResponse(JoinRsp join_rsp) {
        join_promise.SetResult(join_rsp); // will wake up join() method
        gms.getStack().getCacheLog().CriticalInfo("CoordGMSImpl.handleJoin called at startup");
        wrongMethod("handleJoinResponse");
    }

    @Override
    public void handleLeaveResponse() {
        ; // safely ignore this
    }

    @Override
    public void suspect(Address mbr) {
        handleSuspect(mbr);
    }

    @Override
    public void unsuspect(Address mbr) {
    }

    /**
     * Invoked upon receiving a MERGE event from the MERGE layer. Starts the
     * merge protocol. See description of protocol in DESIGN.
     *
     * @param other_coords A list of coordinators (including myself) found by
     * MERGE protocol
     *
     */
    @Override
    public void merge(java.util.ArrayList other_coords) {
        Membership tmp;
        Address leader = null;

        if (merging) {
            gms.getStack().getCacheLog().Warn("CoordGmsImpl.merge", "merge already in progress, discarded MERGE event");
            return;
        }

        if (other_coords == null) {
            gms.getStack().getCacheLog().Warn("CoordGmsImpl.merge", "list of other coordinators is null. Will not start merge.");
            return;
        }

        if (other_coords.size() <= 1) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.merge", "number of coordinators found is " + other_coords.size() + "; will not perform merge");
            return;
        }

        /*
         * Establish deterministic order, so that coords can elect leader
         */
        tmp = new Membership(other_coords);
        tmp.sort();
        leader = (Address) tmp.elementAt(0);
        gms.getStack().getCacheLog().Debug("coordinators in merge protocol are: " + tmp);
        if (leader.equals(gms.local_addr)) {
            gms.getStack().getCacheLog().Debug("I (" + leader + ") will be the leader. Starting the merge task");
            startMergeTask(other_coords);
        }
    }

    /**
     * Get the view and digest and send back both (MergeData) in the form of a
     * MERGE_RSP to the sender. If a merge is already in progress, send back a
     * MergeData with the merge_rejected field set to true.
     */
    @Override
    public void handleMergeRequest(Address sender, Object merge_id) {
        Digest digest;
        View view;

        if (sender == null) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.handleMergeRequest", "sender == null; cannot send back a response");
            return;
        }
        if (merging) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.handleMergeRequest", "merge already in progress");
            sendMergeRejectedResponse(sender);
            return;
        }
        merging = true;
        this.merge_id = merge_id;

        gms.getStack().getCacheLog().Debug("CoordGmsImpl.handleMergeRequest", "sender=" + sender + ", merge_id=" + merge_id);

        digest = gms.getDigest();
        view = new View(gms.view_id.Copy(), gms.members.getMembers());
        view.setCoordinatorGmsId(gms.unique_id);
        sendMergeResponse(sender, view, digest);
    }

    public MergeData getMergeResponse(Address sender, Object merge_id) {
        Digest digest;
        View view;
        MergeData retval;

        if (sender == null) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.getMergeResponse", "sender == null; cannot send back a response");
            return null;
        }
        if (merging) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.getMergeResponse", "merge already in progress");
            retval = new MergeData(sender, null, null);
            retval.merge_rejected = true;
            return retval;
        }
        merging = true;
        this.merge_id = merge_id;

        gms.getStack().getCacheLog().Debug("sender=" + sender + ", merge_id=" + merge_id);

        digest = gms.getDigest();
        view = new View(gms.view_id.Copy(), gms.members.getMembers());
        retval = new MergeData(sender, view, digest);
        retval.view = view;
        retval.digest = digest;
        return retval;
    }

    @Override
    public void handleMergeResponse(MergeData data, Object merge_id) {
        if (data == null) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.handleMergeResponse", "merge data is null");
            return;
        }
        if (merge_id == null || this.merge_id == null) {

            gms.getStack().getCacheLog().Error("CoordGmsImpl.handleMergeResponse", "merge_id (" + merge_id + ") or this.merge_id (" + this.merge_id + ") == null (sender="
                    + data.getSender() + ").");
            return;
        }

        if (!this.merge_id.equals(merge_id)) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.handleMergeResponse", "this.merge_id (" + this.merge_id + ") is different from merge_id (" + merge_id + ')');
            return;
        }

        synchronized (merge_rsps) {
            if (!merge_rsps.contains(data)) {
                merge_rsps.add(data);

                Monitor.pulse(merge_rsps);// merge_rsps.notifyAll();
            }
        }
    }

    /**
     * If merge_id != this.merge_id --> discard Else cast the view/digest to all
     * members of this group.
     */
    @Override
    public void handleMergeView(MergeData data, Object merge_id) {
        if (merge_id == null || this.merge_id == null || !this.merge_id.equals(merge_id)) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.handleMergeView", "merge_ids don't match (or are null); merge view discarded");
            return;
        }
        gms.castViewChange(data.view, data.digest);
        merging = false;
        merge_id = null;
    }

    @Override
    public void handleMergeCancelled(Object merge_id) {
        if (merge_id != null && this.merge_id != null && this.merge_id.equals(merge_id)) {

            gms.getStack().getCacheLog().Debug("merge was cancelled (merge_id=" + merge_id + ')');
            this.merge_id = null;
            merging = false;
        }
    }

    /**
     * Computes the new view (including the newly joined member) and get the
     * digest from PBCAST. Returns both in the form of a JoinRsp
     */
    @Override
    public JoinRsp handleJoin(Address mbr, String subGroup_name, boolean isStartedAsMirror, String gmsId, tangible.RefObject<Boolean> acquireHashmap) {
        synchronized (this) {

            java.util.List new_mbrs = Collections.synchronizedList(new java.util.ArrayList(1));
            View v = null;
            Digest d, tmp;

            gms.getStack().getCacheLog().CriticalInfo("CoordGmsImpl.handleJoin", "mbr=" + mbr);

            if (gms.local_addr.equals(mbr)) {

                gms.getStack().getCacheLog().Error("CoordGmsImpl.handleJoin", "cannot join myself !");
                return null;
            }

            if (gms.members.contains(mbr)) {
                gms.getStack().getCacheLog().Error("CoordGmsImpl.handleJoin()", "member " + mbr + " already present; returning existing view "
                        + Global.CollectionToString(gms.members.getMembers()));
                acquireHashmap.argvalue = false;
                View view = new View(gms.view_id, gms.members.getMembers());
                view.setCoordinatorGmsId(gms.unique_id);
                JoinRsp rsp = new JoinRsp(view, gms.getDigest());
                rsp.getView().setSequencerTbl(gms._subGroupMbrsMap);
                rsp.getView().setMbrsSubgroupMap(gms._mbrSubGroupMap);
                return rsp;
                // already joined: return current digest and membership
            }
            new_mbrs.add(mbr);
            //=====================================
            // update the subGroupMbrsMap and mbrSubGroupMap
            if (gms._subGroupMbrsMap.containsKey(subGroup_name)) {
                synchronized (gms._subGroupMbrsMap) {
                    java.util.List groupMbrs = (java.util.List) gms._subGroupMbrsMap.get(subGroup_name);
                    if (!groupMbrs.contains(mbr)) {
                        groupMbrs.add(mbr);
                    }
                }
            } else {
                synchronized (gms._subGroupMbrsMap) {
                    java.util.ArrayList groupMbrs = new java.util.ArrayList();
                    groupMbrs.add(mbr);
                    gms._subGroupMbrsMap.put(subGroup_name, groupMbrs);
                }
            }

            if (!gms._mbrSubGroupMap.containsKey(mbr)) {
                synchronized (gms._mbrSubGroupMap) {
                    gms._mbrSubGroupMap.put(mbr, subGroup_name);
                }
            }
            //=====================================
            tmp = gms.getDigest(); // get existing digest
            if (tmp == null) {
                gms.getStack().getCacheLog().Error("CoordGmsImpl.handleJoin", "received null digest from GET_DIGEST: will cause JOIN to fail");
                return null;
            }

            gms.getStack().getCacheLog().Debug("got digest=" + tmp);

            d = new Digest(tmp.size() + 1);
            // create a new digest, which contains 1 more member
            d.add(tmp); // add the existing digest to the new one
            d.add(mbr, 0, 0);
            // ... and add the new member. it's first seqno will be 1
            v = gms.getNextView(new_mbrs, null, null);
            v.setSequencerTbl(gms._subGroupMbrsMap);
            v.setMbrsSubgroupMap(gms._mbrSubGroupMap);
            v.AddGmsId(mbr, gmsId);

            //add coordinator own's gms id[bug fix]; so that new member could know cordinator id
            v.AddGmsId(gms.local_addr, gms.unique_id);

            if (gms.getGmsIds() != null) {
                Object tempVar = gms.getGmsIds().clone();
                java.util.HashMap gmsIds = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                Iterator ide = gmsIds.entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry ent = (Map.Entry) ide.next();
                    v.AddGmsId((Address) ent.getKey(), (String) ent.getValue());
                }
            }

            if (gms.getPartitions() != null) {
                Object tempVar = gms.getPartitions().clone();
                java.util.HashMap partIds = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                if (partIds != null) {
                    Iterator ide = partIds.entrySet().iterator();
                    while (ide.hasNext()) {
                        Map.Entry ent = (Map.Entry) ide.next();
                        v.addPartition((Address) ent.getKey(), (String) ent.getValue());
                    }
                }
            }

            gms.getStack().getCacheLog().Debug("joined member " + mbr + ", view is " + v);

            return new JoinRsp(v, d);
        }
    }

    /**
     * Exclude <code>mbr</code> from the membership. If <code>suspected</code>
     * is true, then this member crashed and therefore is forced to leave,
     * otherwise it is leaving voluntarily.
     */
    @Override
    public void handleLeave(Address mbr, boolean suspected) {
        synchronized (this) {

            if (gms.disconnected_nodes.contains(mbr)) {
                gms.disconnected_nodes.remove(mbr);
            }

            Address sameNode = null;
            if (gms.isPartReplica) {
                Membership mbrShip = gms.members.copy();

                if (mbrShip.contains(mbr)) {
                    String leavingMbrPatitionId = gms.getPartition(mbr);
                    for (int i = 0; i < mbrShip.size(); i++) {
                        Address other = mbrShip.elementAt(i);
                        if (other != null && !other.equals(mbr)) {
                            String mbrPatitionId = gms.getPartition(other);
                            if (mbrPatitionId != null && leavingMbrPatitionId != null && mbrPatitionId.equals(leavingMbrPatitionId)) {
                                sameNode = other;
                                break;
                            }
                        }
                    }
                }
            }
            java.util.ArrayList leavingNodes = new java.util.ArrayList();

            if (sameNode != null && !sameNode.getIpAddress().equals(gms.local_addr.getIpAddress())) {
                if (sameNode.getPort() > mbr.getPort()) {
                    leavingNodes.add(sameNode);
                    leavingNodes.add(mbr);
                } else {
                    leavingNodes.add(mbr);
                    leavingNodes.add(sameNode);
                }
            } else {
                leavingNodes.add(mbr);
            }
            java.util.List v = Collections.synchronizedList(new java.util.ArrayList(1));
            // contains either leaving mbrs or suspected mbrs
            gms.getStack().getCacheLog().Debug("pbcast.CoordGmsImpl.handleLeave()", "mbr=" + mbr);
            for (Iterator it = leavingNodes.iterator(); it.hasNext();) {
                Address leavingNode = (Address) it.next();
                if (!gms.members.contains(leavingNode)) {
                    gms.getStack().getCacheLog().Debug("pbcast.CoordGmsImpl.handleLeave()", "mbr " + leavingNode + " is not a member !");
                    if (!suspected) { // send an ack to the leaving member
                        sendLeaveResponse(leavingNode);
                    }
                    return;
                }
                if (gms.view_id == null) {
                    // we're probably not the coord anymore (we just left ourselves), let someone else do it
                    // (client will retry when it doesn't get a response
                    gms.getStack().getCacheLog().Debug("pbcast.CoordGmsImpl.handleLeave()", "gms.view_id is null, I'm not the coordinator anymore (leaving=" + leaving
                            + "); the new coordinator will handle the leave request");
                    return;
                }
                if (!suspected) { // send an ack to the leaving member
                    sendLeaveResponse(leavingNode);
                }
                String subGroup = (String) gms._mbrSubGroupMap.get(leavingNode);
                if (subGroup != null) {
                    synchronized (gms._mbrSubGroupMap) {
                        gms._mbrSubGroupMap.remove(leavingNode);
                    }
                    synchronized (gms._subGroupMbrsMap) {
                        java.util.List subGroupMbrs = (java.util.List) gms._subGroupMbrsMap.get(subGroup);
                        if (subGroupMbrs != null) {
                            subGroupMbrs.remove(leavingNode);
                            if (subGroupMbrs.isEmpty()) {
                                gms._subGroupMbrsMap.remove(subGroup);
                            }
                        }
                    }
                }
                v.add(leavingNode);
                java.util.ArrayList mbrs = new java.util.ArrayList(1);
                mbrs.add(leavingNode);
                String partitionId = gms.getPartition(leavingNode);
                gms.acquireHashmap(mbrs, false, subGroup, false, partitionId);
            }
            if (suspected) {
                gms.castViewChange(null, null, v, gms._hashmap);
            } else {
                gms.castViewChange(null, v, null, gms._hashmap);
            }
        }
    }

    public void sendLeaveResponse(Address mbr) {
        Message msg = new Message(mbr, null, null);
        GMS.HDR hdr = new GMS.HDR(GMS.HDR.LEAVE_RSP);
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG, msg));
    }

    /**
     * Called by the GMS when a VIEW is received.
     *
     * @param new_view The view to be installed
     *
     * @param digest If view is a MergeView, digest contains the seqno digest of
     * all members and has to be set by GMS
     *
     */
    @Override
    public void handleViewChange(View new_view, Digest digest) {
        java.util.List mbrs = new_view.getMembers();
        if (digest != null) {
            gms.getStack().getCacheLog().Debug("view=" + new_view + ", digest=" + digest);
        } else {
            gms.getStack().getCacheLog().Debug("view=" + new_view);
        }

        if (leaving && !mbrs.contains(gms.local_addr)) {
            return;
        }
        gms.installView(new_view, digest);

        synchronized (viewRejectingMembers) {
            //we handle the request of those nodes who have rejected our this view
            Address rejectingMbr;
            for (int i = 0; i < viewRejectingMembers.size(); i++) {
                rejectingMbr = (Address) ((viewRejectingMembers.get(i) instanceof Address) ? viewRejectingMembers.get(i) : null);
                handleViewRejected(rejectingMbr);
            }
            viewRejectingMembers.clear();
        }
    }

    @Override
    public void handleSuspect(Address mbr) {
        if (mbr.equals(gms.local_addr)) {
            gms.getStack().getCacheLog().Warn("I am the coord and I'm being am suspected -- will probably leave shortly");
            return;
        }
        handleLeave(mbr, true); // irregular leave - forced
    }

    @Override
    public void handleNodeRejoining(Address node) {
        handleInformNodeRejoining(gms.local_addr, node);
    }

    @Override
    public void handleCanNotConnectTo(Address src, java.util.List failedNodes) {
        if (src != null && failedNodes != null) {
            java.util.HashMap nodeGmsIds = new java.util.HashMap();
            String sourceGmsId = gms.GetNodeGMSId(src);
            for (Iterator it = failedNodes.iterator(); it.hasNext();) {
                Address node = (Address) it.next();
                String gmsId = gms.GetNodeGMSId(node);
                if (gmsId != null) {
                    nodeGmsIds.put(node, gmsId);
                }
            }
            if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                gms.getStack().getCacheLog().Info("CoodGmsImpl.handleCanNotConnectTo", src + " can not connect to " + Global.CollectionToString(failedNodes));
            }
            for (Iterator it = failedNodes.iterator(); it.hasNext();) {
                Address node = (Address) it.next();
                if (!gms.VerifySuspect(node)) {
                    Membership mbrs = gms.members.copy();
                    Address seniorNode = mbrs.DetermineSeniority(src, node);

                    if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                        gms.getStack().getCacheLog().Info("CoodGmsImpl.handleCanNotConnectTo", "senior node " + seniorNode);
                    }
                    String nodeGmsId = (String) ((nodeGmsIds.get(node) instanceof String) ? nodeGmsIds.get(node) : null);

                    if (seniorNode.equals(src)) {
                        //suspect has to leave the cluster
                        AskToLeaveCluster(node, nodeGmsId);
                    } else {
                        //informer has to leave the cluster
                        AskToLeaveCluster(src, sourceGmsId);
                    }

                } else {
                    if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                        gms.getStack().getCacheLog().Info("CoodGmsImpl.handleCanNotConnectTo", node + " is already declared dead");
                    }
                }
            }
        }
    }

    @Override
    public void handleInformNodeRejoining(Address sender, Address node) {
        if (node != null) {
            if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                gms.getStack().getCacheLog().Info("CoordinatorGmsImpl.handleInformNodeRejoining", sender.toString() + " informed about rejoining with " + node);
            }
            if (gms.members.contains(node)) {
                ViewId viewId = gms.GetNextViewId();
                GMS.HDR header = new GMS.HDR(GMS.HDR.RESET_ON_NODE_REJOINING, node);
                Object tempVar = gms.members.clone();
                header.view = new View(viewId, (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null));
                header.view.setCoordinatorGmsId(gms.unique_id);
                Message rejoiningMsg = new Message(null, null, new byte[0]);
                rejoiningMsg.putHeader(HeaderType.GMS, header);
                gms.passDown(new Event(Event.MSG, rejoiningMsg, Priority.Critical));
            }
        }
    }

    @Override
    public void handleResetOnNodeRejoining(Address sender, Address node, View view) {
        gms.handleResetOnNodeRejoining(sender, node, view);
    }

    @Override
    public void handleConnectionBroken(Address informer, Address suspected) {
        if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
            gms.getStack().getCacheLog().Info("CoodGmsImpl.handleConnectionBroken", informer + " informed about connection breakage with " + suspected);
        }

        /**
         * synchronizes the multiple parallel requests so that one request is
         * executed at a time.
         */
        String nodeGmsId = gms.GetNodeGMSId(suspected);
        String sourceGmsId = gms.GetNodeGMSId(informer);

        synchronized (connection_break_mutex) {
            if (!gms.VerifySuspect(suspected)) {
                if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                    gms.getStack().getCacheLog().Info("CoodGmsImpl.handleConnectionBroken", suspected + " is not dead");
                }

                Membership mbrs = gms.members.copy();
                Address seniorNode = mbrs.DetermineSeniority(informer, suspected);

                if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                    gms.getStack().getCacheLog().Info("CoodGmsImpl.handleConnectionBroken", "senior node " + seniorNode);
                }

                if (seniorNode.equals(informer)) {
                    //suspect has to leave the cluster
                    AskToLeaveCluster(suspected, nodeGmsId);
                } else {
                    //informer has to leave the cluster
                    AskToLeaveCluster(informer, sourceGmsId);
                }

            }
        }
    }

    /**
     * When we broadcast a view and any of the member reject the view then we
     * should also remove it from the our membership list.
     *
     * @param mbrRejected
     */
    @Override
    public void handleViewRejected(Address mbrRejected) {
        synchronized (viewRejectingMembers) {
            if (gms.determineCoordinator().equals(gms.local_addr)) {
                if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                    gms.getStack().getCacheLog().Info("CoodGmsImpl.handleViewRejected", mbrRejected + " rejcted the view");
                }

                handleLeave(mbrRejected, false);
            } else {
                if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
                    gms.getStack().getCacheLog().Info("CoodGmsImpl.handleViewRejected", mbrRejected + " rejcted the view, but we have not installed the view ourself");
                }

                //It is the case when we have broadcasted the view to all the members
                //including ourself. A member rejection is received before we have
                //not installed this view yet.
                viewRejectingMembers.add(mbrRejected);

            }
        }
    }

    @Override
    public void stop() {
        leaving = true;
        merge_task.stop();
    }

    public final void AskToLeaveCluster(Address leavingMember, String urGmsId) {
        if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
            gms.getStack().getCacheLog().Info("CoodGmsImpl.AskToLeaveCluster", leavingMember + " is requested to leave the cluster");
        }

        Message msg = new Message(leavingMember, null, new byte[0]);
        GMS.HDR hdr = new GMS.HDR(GMS.HDR.LEAVE_CLUSTER, gms.local_addr);
        hdr.arg = urGmsId;
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG, msg, Priority.Critical));
        ;
    }

    /*
     * ------------------------------------------ Private methods -----------------------------------------
     */
    public void startMergeTask(java.util.ArrayList coords) {
        merge_task.start(coords);
    }

    public void stopMergeTask() {
        merge_task.stop();
    }

    /**
     * Sends a MERGE_REQ to all coords and populates a list of MergeData (in
     * merge_rsps). Returns after coords.size() response have been received, or
     * timeout msecs have elapsed (whichever is first)
     * .<p>
     * If a subgroup coordinator rejects the MERGE_REQ (e.g. because of
     * participation in a different merge), <em>that member will be removed from
     * coords !</em>
     *
     * @param coords A list of Addresses of subgroup coordinators (inluding
     * myself)
     *
     * @param timeout Max number of msecs to wait for the merge responses from
     * the subgroup coords
     *
     */
    public void getMergeDataFromSubgroupCoordinators(java.util.ArrayList coords, long timeout) {
        Message msg;
        GMS.HDR hdr;
        Address coord;
        long curr_time, time_to_wait = 0, end_time;
        int num_rsps_expected = 0;

        if (coords == null || coords.size() <= 1) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.getMergeDataFromSubgroupCoordinator", "coords == null or size <= 1");
            return;
        }

        synchronized (merge_rsps) {
            merge_rsps.clear();

            gms.getStack().getCacheLog().Debug("sending MERGE_REQ to " + Global.CollectionToString(coords));
            for (int i = 0; i < coords.size(); i++) {
                coord = (Address) coords.get(i);

                if (gms.local_addr != null && gms.local_addr.equals(coord)) {
                    merge_rsps.add(getMergeResponse(gms.local_addr, merge_id));
                    continue;
                }

                msg = new Message(coord, null, null);
                hdr = new GMS.HDR(GMS.HDR.MERGE_REQ);
                hdr.mbr = gms.local_addr;
                hdr.merge_id = merge_id;
                msg.putHeader(HeaderType.GMS, hdr);
                gms.passDown(new Event(Event.MSG, msg));
            }

            // wait until num_rsps_expected >= num_rsps or timeout elapsed
            num_rsps_expected = coords.size();
            //  changing to System.currentTimeMillis

            curr_time = System.currentTimeMillis();
            end_time = curr_time + timeout;
            while (end_time > curr_time) {
                time_to_wait = end_time - curr_time;

                gms.getStack().getCacheLog().Debug("waiting for " + time_to_wait + " msecs for merge responses");
                if (merge_rsps.size() < num_rsps_expected) {
                    try {
                        //  chagnig Wait to wait

                        Monitor.wait(merge_rsps, time_to_wait); //merge_rsps.wait(time_to_wait);
                    } catch (Exception ex) {
                        gms.getStack().getCacheLog().Error("CoordGmsImpl.getMergeDataFromSubgroupCoordinators()", ex.toString());
                    }
                }

                // SAL:
                if (time_to_wait < 0) {
                    gms.getStack().getCacheLog().Fatal("[Timeout]CoordGmsImpl.getMergeDataFromSubgroupCoordinators:" + time_to_wait);
                }

                gms.getStack().getCacheLog().Debug("num_rsps_expected=" + num_rsps_expected + ", actual responses=" + merge_rsps.size());

                if (merge_rsps.size() >= num_rsps_expected) {
                    break;
                }

                //  changing to System.currentTimeMillis
                curr_time = System.currentTimeMillis();
            }
        }
    }

    /**
     * Generates a unique merge id by taking the local address and the current
     * time
     */
    public final Object generateMergeId() {
        return new ViewId(gms.local_addr, System.currentTimeMillis());
        // we're (ab)using ViewId as a merge id
    }

    /**
     * Merge all MergeData. All MergeData elements should be disjunct (both
     * views and digests). However, this method is prepared to resolve duplicate
     * entries (for the same member). Resolution strategy for views is to merge
     * only 1 of the duplicate members. Resolution strategy for digests is to
     * take the higher seqnos for duplicate digests.<p>
     * After merging all members into a Membership and subsequent sorting, the
     * first member of the sorted membership will be the new coordinator.
     *
     * @param v A list of MergeData items. Elements with merge_rejected=true
     * were removed before. Is guaranteed not to be null and to contain at least
     * 1 member.
     *
     */
    public MergeData consolidateMergeData(java.util.List v) {
        MergeData ret = null;
        MergeData tmp_data;
        long logical_time = 0; // for new_vid
        ViewId new_vid, tmp_vid;
        MergeView new_view;
        View tmp_view;
        Membership new_mbrs = new Membership();
        int num_mbrs = 0;
        Digest new_digest = null;
        Address new_coord;
        java.util.List subgroups = Collections.synchronizedList(new java.util.ArrayList(11));
        // contains a list of Views, each View is a subgroup

        for (int i = 0; i < v.size(); i++) {
            tmp_data = (MergeData) v.get(i);

            gms.getStack().getCacheLog().Debug("merge data is " + tmp_data);

            tmp_view = tmp_data.getView();
            if (tmp_view != null) {
                tmp_vid = tmp_view.getVid();
                if (tmp_vid != null) {
                    // compute the new view id (max of all vids +1)
                    logical_time = Math.max(logical_time, tmp_vid.getId());
                }
            }
            // merge all membership lists into one (prevent duplicates)
            new_mbrs.add(tmp_view.getMembers());
            subgroups.add(tmp_view.clone());
        }

        // the new coordinator is the first member of the consolidated & sorted membership list
        new_mbrs.sort();
        num_mbrs = new_mbrs.size();
        new_coord = num_mbrs > 0 ? (Address) new_mbrs.elementAt(0) : null;
        if (new_coord == null) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.consolodateMergeData", "new_coord is null.");
            return null;
        }
        // should be the highest view ID seen up to now plus 1
        new_vid = new ViewId(new_coord, logical_time + 1);

        // determine the new view
        new_view = new MergeView(new_vid, new_mbrs.getMembers(), subgroups);

        gms.getStack().getCacheLog().Debug("new merged view will be " + new_view);

        // determine the new digest
        new_digest = consolidateDigests(v, num_mbrs);
        if (new_digest == null) {
            gms.getStack().getCacheLog().Error("CoordGmsImpl.consolidateMergeData", "digest could not be consolidated.");
            return null;
        }

        gms.getStack().getCacheLog().Debug("consolidated digest=" + new_digest);

        ret = new MergeData(gms.local_addr, new_view, new_digest);
        return ret;
    }

    /**
     * Merge all digests into one. For each sender, the new value is
     * min(low_seqno), max(high_seqno), max(high_seqno_seen)
     */
    public Digest consolidateDigests(java.util.List v, int num_mbrs) {
        MergeData data;
        Digest tmp_digest, retval = new Digest(num_mbrs);

        for (int i = 0; i < v.size(); i++) {
            data = (MergeData) v.get(i);
            tmp_digest = data.getDigest();
            if (tmp_digest == null) {
                gms.getStack().getCacheLog().Error("tmp_digest == null; skipping");
                continue;
            }
            retval.merge(tmp_digest);
        }
        return retval;
    }

    /**
     * Sends the new view and digest to all subgroup coordinors in coords. Each
     * coord will in turn <ol> <li>cast the new view and digest to all the
     * members of its subgroup (MergeView) <li>on reception of the view, if it
     * is a MergeView, each member will set the digest and install the new view
     * </ol>
     */
    public void sendMergeView(java.util.ArrayList coords, MergeData combined_merge_data) {
        Message msg;
        GMS.HDR hdr;
        Address coord;
        View v;
        Digest d;

        if (coords == null || combined_merge_data == null) {
            return;
        }
        v = combined_merge_data.view;
        d = combined_merge_data.digest;
        if (v == null || d == null) {
            gms.getStack().getCacheLog().Error("view or digest is null, cannot send consolidated merge view/digest");
            return;
        }

        for (int i = 0; i < coords.size(); i++) {
            coord = (Address) coords.get(i);
            msg = new Message(coord, null, null);
            hdr = new GMS.HDR(GMS.HDR.INSTALL_MERGE_VIEW);
            hdr.view = v;
            hdr.digest = d;
            hdr.merge_id = merge_id;
            msg.putHeader(HeaderType.GMS, hdr);
            gms.passDown(new Event(Event.MSG, msg));
        }
    }

    /**
     * Send back a response containing view and digest to sender
     */
    public void sendMergeResponse(Address sender, View view, Digest digest) {
        Message msg = new Message(sender, null, null);
        GMS.HDR hdr = new GMS.HDR(GMS.HDR.MERGE_RSP);
        hdr.merge_id = merge_id;
        hdr.view = view;
        hdr.digest = digest;
        msg.putHeader(HeaderType.GMS, hdr);

        gms.getStack().getCacheLog().Debug("response=" + hdr);

        gms.passDown(new Event(Event.MSG, msg));
    }

    public void sendMergeRejectedResponse(Address sender) {
        Message msg = new Message(sender, null, null);
        GMS.HDR hdr = new GMS.HDR(GMS.HDR.MERGE_RSP);
        hdr.merge_rejected = true;
        hdr.merge_id = merge_id;
        msg.putHeader(HeaderType.GMS, hdr);

        gms.getStack().getCacheLog().Debug("response=" + hdr);

        gms.passDown(new Event(Event.MSG, msg));
    }

    public void sendMergeCancelledMessage(java.util.ArrayList coords, Object merge_id) {
        Message msg;
        GMS.HDR hdr;
        Address coord;

        if (coords == null || merge_id == null) {
            gms.getStack().getCacheLog().Error("coords or merge_id == null");
            return;
        }
        for (int i = 0; i < coords.size(); i++) {
            coord = (Address) coords.get(i);
            msg = new Message(coord, null, null);
            hdr = new GMS.HDR(GMS.HDR.CANCEL_MERGE);
            hdr.merge_id = merge_id;
            msg.putHeader(HeaderType.GMS, hdr);
            gms.passDown(new Event(Event.MSG, msg));
        }
    }

    /**
     * Removed rejected merge requests from merge_rsps and coords
     */
    public void removeRejectedMergeRequests(java.util.ArrayList coords) {
        for (int i = merge_rsps.size() - 1; i >= 0; i--) {
            MergeData data = (MergeData) merge_rsps.get(i);
            if (data.merge_rejected) {
                if (data.getSender() != null && coords != null) {
                    coords.remove(data.getSender());
                }

                merge_rsps.remove(i);
                gms.getStack().getCacheLog().Debug("removed element " + data);
            }
        }
    }

    public final void ReSaturateCluster() {
    }

    /*
     * --------------------------------------- End of Private methods -------------------------------------
     */
    /**
     * Starts the merge protocol (only run by the merge leader). Essentially
     * sends a MERGE_REQ to all coordinators of all subgroups found. Each coord
     * receives its digest and view and returns it. The leader then computes the
     * digest and view for the new group from the return values. Finally, it
     * sends this merged view/digest to all subgroup coordinators; each
     * coordinator will install it in their subgroup.
     */
    public static class MergeTask implements Runnable {

        public MergeTask(CoordGmsImpl enclosingInstance) {
            InitBlock(enclosingInstance);
        }

        private void InitBlock(CoordGmsImpl enclosingInstance) {
            this.enclosingInstance = enclosingInstance;
        }
        private CoordGmsImpl enclosingInstance;

        public boolean getRunning() {
            return t != null && t.isAlive();
        }

        public final CoordGmsImpl getEnclosing_Instance() {
            return enclosingInstance;
        }
        public Thread t = null;
        public java.util.ArrayList coords = null; // list of subgroup coordinators to be contacted

        public void start(java.util.ArrayList coords) {
            if (t == null) {
                this.coords = coords;
                t = new Thread(this);
                t.setName("MergeTask thread");
                t.setDaemon(true);
                t.start();
            }
        }

        public void stop() {
            Thread tmp = t;
            if (getRunning()) {
                t = null;
                tmp.interrupt();
            }
            t = null;
            coords = null;
        }

        /**
         * Runs the merge protocol as a leader
         */
        public void run() {
            MergeData combined_merge_data = null;

            if (getEnclosing_Instance().merging == true) {
                getEnclosing_Instance().gms.getStack().getCacheLog().Warn("CoordGmsImpl.Run()", "merge is already in progress, terminating");
                return;
            }

            getEnclosing_Instance().gms.getStack().getCacheLog().Debug("CoordGmsImpl.Run()", "merge task started");
            try {

                /*
                 * 1. Generate a merge_id that uniquely identifies the merge in progress
                 */
                getEnclosing_Instance().merge_id = getEnclosing_Instance().generateMergeId();

                /*
                 * 2. Fetch the current Views/Digests from all subgroup coordinators
                 */
                getEnclosing_Instance().getMergeDataFromSubgroupCoordinators(coords, getEnclosing_Instance().gms.merge_timeout);

                /*
                 * 3. Remove rejected MergeData elements from merge_rsp and coords (so we'll send the new view only to members who accepted the merge request)
                 */
                getEnclosing_Instance().removeRejectedMergeRequests(coords);

                if (getEnclosing_Instance().merge_rsps.size() <= 1) {
                    getEnclosing_Instance().gms.getStack().getCacheLog().Warn("CoordGmsImpl.Run()", "merge responses from subgroup coordinators <= 1 ("
                            + Global.CollectionToString(getEnclosing_Instance().merge_rsps) + "). Cancelling merge");
                    getEnclosing_Instance().sendMergeCancelledMessage(coords, getEnclosing_Instance().merge_id);
                    return;
                }

                /*
                 * 4. Combine all views and digests into 1 View/1 Digest
                 */
                combined_merge_data = getEnclosing_Instance().consolidateMergeData(getEnclosing_Instance().merge_rsps);
                if (combined_merge_data == null) {
                    getEnclosing_Instance().gms.getStack().getCacheLog().Error("CoordGmsImpl.Run()", "combined_merge_data == null");
                    getEnclosing_Instance().sendMergeCancelledMessage(coords, getEnclosing_Instance().merge_id);
                    return;
                }

                /*
                 * 5. Send the new View/Digest to all coordinators (including myself). On reception, they will install the digest and view in all of their subgroup members
                 */
                getEnclosing_Instance().sendMergeView(coords, combined_merge_data);
            } catch (Exception ex) {
                getEnclosing_Instance().gms.getStack().getCacheLog().Error("MergeTask.Run()", ex.toString());
            } finally {
                getEnclosing_Instance().merging = false;

                getEnclosing_Instance().gms.getStack().getCacheLog().Debug("CoordGmsImpl.Run()", "merge task terminated");
                t = null;
            }
        }
    }

    /**
     * Should be called only incase of POR, this will perform same steps as that
     * of ClientGMSImpl.join will call in findInitialMembers will perform
     * determine coordinator will call join request
     *
     * This is for the case when all nodes of a cluster are simultaneously
     * started and every node creates a separate cluster after cluster startup
     * this function will only make sure that they should join in.
     *
     * @param isPOR <rereturns>true was cluster health is ok</rereturns>
     */
    public final boolean CheckOwnClusterHealth(boolean isPOR, int retryNumber) throws InterruptedException {
        if (gms.getStack().getCacheLog().getIsInfoEnabled()) {
            gms.getStack().getCacheLog().Info("CoordGmsImpl.Join()", "CheckOwnClusterHealth - Retry Number: " + (new Integer(retryNumber)).toString());
        }
        JoinRsp rsp;
        Digest tmp_digest;

        if (retryNumber != 1) {
            if (isPOR) {
                if (!((initial_mbrs.size() > 2) && (gms.members.getMembers().size() <= 2))) {
                    //FindAliveMembers();
                    return true;
                }
            } else {

                FindAliveMembers();
                if (!((initial_mbrs.size() > 1) && (gms.members.getMembers().size() == 1))) {

                    return true;
                }
            }
        }

        if (retryNumber > 1) {
            Util.sleep(gms._castViewChangeTimeOut / 2);
        }

        int findAliveMembersRetry = 1;
        int retryCount = 3;

        try {
            while (!leaving) {

                gms.getStack().getCacheLog().CriticalInfo("CoordGmsImpl.Join()", "CheckOwnClusterHealth - Retry Count: " + (new Integer(retryCount)).toString());
                java.util.List initMembers = FindAliveMembers();

                join_promise.Reset();
                if (initMembers.isEmpty()) {
                    findAliveMembersRetry--;
                    if (findAliveMembersRetry <= 0) {
                        return true;
                    }
                    Util.sleep(gms.join_retry_timeout);
                    initial_mbrs_received = false;
                    continue;
                }

                //This will determine that coord that is already a coord of a cluster or the one with the lowest IP
                Address coord = determineCoord(initMembers);

                if (coord == null) {
                    Util.sleep(gms.join_retry_timeout);
                    continue;
                } else if (coord.equals(gms.local_addr)) {
                    return true;
                } else {
                    sendJoinMessage(coord, gms.local_addr, gms.subGroup_addr, false);
                    rsp = (JoinRsp) join_promise.WaitResult(gms._castViewChangeTimeOut);
                }

                if (rsp == null) {

                    gms.getStack().getCacheLog().CriticalInfo("CoordGmsImpl.Join()", "Reply was NULL");
                    retryCount--;
                    if (retryCount <= 0) {
                        return true;
                    }

                    Util.sleep(gms.join_timeout);
                    initial_mbrs_received = false;
                    continue;
                } else if (rsp.getJoinResult() == JoinResult.Rejected) {
                    gms.getStack().getCacheLog().CriticalInfo("CoordGmsImpl.Join()", "Reply: JoinResult.Rejected");
                    return true;
                } else if (rsp.getJoinResult() == JoinResult.MembershipChangeAlreadyInProgress) {
                    gms.getStack().getCacheLog().CriticalInfo("CoordGmsImpl.Join()", "Reply: JoinResult.MembershipChangeAlreadyInProgress");
                    Util.sleep(gms.join_timeout);
                    continue;
                } else {
                    tmp_digest = rsp.getDigest();
                    if (tmp_digest != null) {
                        tmp_digest.incrementHighSeqno(coord); // see DESIGN for an explanantion
                        gms.getStack().getCacheLog().Debug("CoordGmsImpl.Join()", "digest is " + tmp_digest);
                        gms.setDigest(tmp_digest);
                    } else {
                        gms.getStack().getCacheLog().Error("CoordGmsImpl.Join()", "digest of JOIN response is null");
                    }

                    // 2. Install view
                    gms.getStack().getCacheLog().Debug("CoordGmsImpl.Join()", "[" + gms.local_addr + "]: JoinRsp=" + rsp.getView() + " [size=" + rsp.getView().size() + "]\n\n");

                    if (rsp.getView() != null) {

                        if (!installView(rsp.getView(), isPOR)) {
                            gms.getStack().getCacheLog().Error("CoordGmsImpl.Join()", "view installation failed, retrying to join group");
                            return true;
                        }
                        gms.getStack().setIsOperational(true);

                        return false;
                    }
                    return true;
                }
            }
        } catch (Exception ex) {
            gms.getStack().getCacheLog().CriticalInfo("CoordGmsImpl.Join()", ex.toString());
        }
        return true;

    }

    /**
     * Called by join(). Installs the view returned by calling
     * Coord.handleJoin() and becomes coordinator.
     */
    private boolean installView(View new_view, boolean isPOR) throws InterruptedException {
        if (isPOR) {
            Address replica = (Address) gms.members.getMembers().get(1);
            SendCheckClusterHealth(replica, new_view.getCoordinator());
        }

        java.util.List mems = new_view.getMembers();
        gms.getStack().getCacheLog().Debug("pb.ClientGmsImpl.installView()", "new_view=" + new_view);
        if (gms.local_addr == null || mems == null || !mems.contains(gms.local_addr)) {
            gms.getStack().getCacheLog().Error("pb.ClientGmsImpl.installView()", "I (" + gms.local_addr + ") am not member of " + Global.CollectionToString(mems)
                    + ", will not install view");
            return false;
        }

        //Cast view to the replica node as well
        gms.installView(new_view);

        gms.becomeParticipant();
        gms.getStack().setIsOperational(true);

        Util.sleep(gms.join_retry_timeout);
        return true;
    }

    /**
     * Pings initial members. Removes self before returning vector of initial
     * members. Uses IP multicast or gossiping, depending on parameters.
     */
    public final java.util.List FindAliveMembers() {
        PingRsp ping_rsp;
        initial_mbrs.clear();
        initial_mbrs_received = false;
        synchronized (initial_mbrs) {
            gms.passDown(new Event(Event.FIND_INITIAL_MBRS));

            // the initial_mbrs_received flag is needed when passDown() is executed on the same thread, so when
            // it returns, a response might actually have been received (even though the initial_mbrs might still be empty)
            if (initial_mbrs_received == false) {
                try {
                    //chaning Wait to wait
                    //System.Threading.Monitor.Wait(initial_mbrs.SyncRoot);
                    Monitor.wait(initial_mbrs);//initial_mbrs.wait();
                } catch (Exception ex) {
                    gms.getStack().getCacheLog().Error("COORDGmsImpl.findInitialMembers", ex.getMessage());
                }
            }

            for (int i = 0; i < initial_mbrs.size(); i++) {
                ping_rsp = (PingRsp) initial_mbrs.get(i);
                if (ping_rsp.getOwnAddress() != null && gms.local_addr != null && ping_rsp.getOwnAddress().equals(gms.local_addr)) {

                    break;
                }
                if (!ping_rsp.getIsStarted()) {
                    initial_mbrs.remove(i);
                }
            }
        }

        return initial_mbrs;
    }

    @Override
    public boolean handleUpEvent(Event evt) {
        java.util.List tmp;

        switch (evt.getType()) {
            case Event.FIND_INITIAL_MBRS_OK:
                tmp = (java.util.List) evt.getArg();

                // changing pulse to notify
                Monitor.pulse(initial_mbrs);// initial_mbrs.notify();
                synchronized (initial_mbrs) {
                    if (tmp != null && tmp.size() > 0) {
                        for (int i = 0; i < tmp.size(); i++) {
                            initial_mbrs.add(tmp.get(i));
                        }
                    }
                    initial_mbrs_received = true;
                }
                return false; // don't pass up the stack
        }
        return true;
    }

    public Address determineCoord(java.util.List mbrs) {
        if (mbrs == null || mbrs.size() < 1) {
            return null;
        }

        Address winner = null;
        int max_votecast = 0;

        //Hashtables are synced by default
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

                gms.getStack().getCacheLog().CriticalInfo("pb.CoordGmsImpl.determineCoord()", "Owner " + mbr.getOwnAddress() + " -- CoordAddress " + mbr.getCoordAddress()
                        + " -- Vote " + (Integer) votes.get(mbr.getCoordAddress()));

            }
        }

        /**
         * Collect all the candidates with the highest but similar vote count.
         * Ideally there should only be one. ()
         */
        java.util.ArrayList candidates = new java.util.ArrayList(votes.size());
        for (Iterator e = votes.entrySet().iterator(); e.hasNext();) {
            Map.Entry ent = (Map.Entry) e.next();
            if (((Integer) ent.getValue()) == max_votecast) {
                candidates.add(ent.getKey());
            }
        }

        Collections.sort(candidates);
        if (winner == null) {
            winner = (Address) candidates.get(0);
        }

        if (candidates.size() > 1) {
            gms.getStack().getCacheLog().Warn("pb.CoordGmsImpl.determineCoord()", "there was more than 1 candidate for coordinator: " + Global.CollectionToString(candidates));
        }
        gms.getStack().getCacheLog().CriticalInfo("pb.CoordGmsImpl.determineCoord()", "election winner: " + winner + " with votes " + max_votecast);

        return winner;
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

    public void SendCheckClusterHealth(Address destination, Address coord) {

        Message msg;
        GMS.HDR hdr;

        msg = new Message(destination, null, null);
        hdr = new GMS.HDR(GMS.HDR.RE_CHECK_CLUSTER_HEALTH, coord);
        hdr.setGMSId(gms.unique_id);
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG_URGENT, msg, Priority.Critical));
    }
}
