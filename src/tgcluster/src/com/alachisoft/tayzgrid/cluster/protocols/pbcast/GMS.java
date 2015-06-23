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

import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Membership;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.ViewId;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.cluster.Header;
import com.alachisoft.tayzgrid.cluster.protocols.NodeStatus;
import com.alachisoft.tayzgrid.cluster.stack.Protocol;
import com.alachisoft.tayzgrid.cluster.stack.ProtocolStackType;
import com.alachisoft.tayzgrid.cluster.util.BoundedList;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.common.mirroring.CacheNode;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.*;

// $Id: GMS.java,v 1.17 2004/09/03 12:28:04 belaban Exp $
/**
 * Group membership protocol. Handles joins/leaves/crashes (suspicions) and
 * emits new views accordingly. Use VIEW_ENFORCER on top of this layer to make
 * sure new members don't receive any messages until they are members.
 */
public class GMS extends Protocol {

    @Override
    public String getName() {
        return "GMS";
    }

    public GmsImpl getImpl() {
        return impl;
    }

    public void setImpl(GmsImpl value) {
        synchronized (impl_mutex) {
            impl = value;

        }
    }

    /**
     * Sends down a GET_DIGEST event and waits for the GET_DIGEST_OK response,
     * or timeout, whichever occurs first
     *
     * Send down a SET_DIGEST event
     */
    public Digest getDigest() {
        Digest ret = null;

        synchronized (digest_mutex) {
            digest = null;
            passDown(new Event(Event.GET_DIGEST));
            if (digest == null) {
                try {
                    Monitor.wait(digest_mutex, digest_timeout); //digest_mutex.wait(digest_timeout);
                } catch (Exception ex) {
                    getStack().getCacheLog().Error("GMS.Digest", ex.getMessage());
                }
            }
            if (digest != null) {
                ret = digest;
                digest = null;
                return ret;
            } else {
                getStack().getCacheLog().Error("digest could not be fetched from PBCAST layer");
                return null;
            }
        }
    }

    public void setDigest(Digest value) {
        passDown(new Event(Event.SET_DIGEST, value));
    }

    private GmsImpl impl = null;
    public Address local_addr = null;
    public String group_addr = null;
    public String subGroup_addr = null;
    public Membership members = new Membership(); // real membership
    public Membership tmp_members = new Membership(); // base for computing next view
    /**
     * Members joined but for which no view has been received yet
     */
    public java.util.List joining = Collections.synchronizedList(new java.util.ArrayList(7));
    /**
     * Members excluded from group, but for which no view has been received yet
     */
    public java.util.List leaving = Collections.synchronizedList(new java.util.ArrayList(7));
    public ViewId view_id = null;
    public long ltime = 0;
    public long join_timeout = 3000;
    public long join_retry_timeout = 1000;
    public long leave_timeout = 5000;
    public long digest_timeout = 5000; // time to wait for a digest (from PBCAST). should be fast
    public long merge_timeout = 10000; // time to wait for all MERGE_RSPS
    public Object impl_mutex = new Object(); // synchronizes event entry into impl
    private Object digest_mutex = new Object(); // synchronizes the GET_DIGEST/GET_DIGEST_OK events
    private Digest digest = null; // holds result of GET_DIGEST event
    // HashMap is sync by default
    private java.util.HashMap impls = new java.util.HashMap(3);
    private boolean shun = false;
    private boolean print_local_addr = true;
    public boolean disable_initial_coord = false; // can the member become a coord on startup or not ?
    public static final String CLIENT = "Client";
    public static final String COORD = "Coordinator";
    public static final String PART = "Participant";
    public int join_retry_count = 20; //Join retry count with same coordinator;
    private Object join_mutex = new Object(); //Stops simoultaneous node joining
    public TimeScheduler timer = null;
    private Object acquireMap_mutex = new Object(); //synchronizes the HASHMAP_REQ/HASHMAP_RESP events
    public Object _hashmap;
    //=======================================
    public java.util.HashMap _subGroupMbrsMap = new java.util.HashMap();
    public java.util.HashMap _mbrSubGroupMap = new java.util.HashMap();
    //=======================================
    /**
     * Max number of old members to keep in history
     */
    protected int num_prev_mbrs = 50;
    /**
     * Keeps track of old members (up to num_prev_mbrs)
     */
    public BoundedList prev_members = null;
    private Object suspect_verify_mutex = new Object();
    private NodeStatus nodeStatus;
    private Address nodeTobeSuspect;
    public boolean isPartReplica;
    public java.util.ArrayList disconnected_nodes = new java.util.ArrayList();
    public String unique_id;
    public java.util.HashMap nodeGmsIds = new java.util.HashMap();
    private String _uniqueID = "";
    private boolean _nodeJoiningInProgress = false;
    private boolean _isStarting = true;
    private ViewPromise _promise;
    // how much should this be
    public int _castViewChangeTimeOut = 15000;
    private Address _memberBeingHandled;
    private boolean _isLeavingInProgress;
    private Object acquirePermission_mutex = new Object(); //synchronizes the ASK_JOIN/ASK_JOIN_RESP events
    private Object membership_mutex = new Object(); //synchronizes the ASK_JOIN/ASK_JOIN_RESP events
    private boolean _membershipChangeInProgress = false;
    private Object resume_mutex = new Object(); //synchronizes the ASK_JOIN/ASK_JOIN_RESP events
    public boolean _allowJoin = false;
    public boolean _doReDiscovery = true;
    public boolean _stateTransferInProcess;
    public Object _syncLock = new Object();
    private long _stateTransferMarkTime = 0;
    public SateTransferPromise _stateTransferPromise;
    public int _stateTransferQueryTimesout = 3000;
    private boolean _startedAsMirror;
    private String partitionId;
    private HashMap partitionsIds = new HashMap();

    public GMS() {
        try {
            partitionId = java.net.InetAddress.getLocalHost().getHostName() + "_" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        } catch (UnknownHostException ex) {

        }
        initState();
    }

    public String getPartitionId() {
        return partitionId;
    }

    /**
     * This flag is set to true if we need to initialize a unique identifier
     * that is shared by all the nodes participating in cluster.
     *
     * @return
     */
    public final String getUniqueID() {
        return _uniqueID;
    }

    public final void setUniqueID(String value) {
        _uniqueID = value;
    }

    @Override
    public java.util.List requiredDownServices() {
        java.util.List retval = Collections.synchronizedList(new java.util.ArrayList(3));
        retval.add((int) Event.GET_DIGEST);
        retval.add((int) Event.SET_DIGEST);
        retval.add((int) Event.FIND_INITIAL_MBRS);
        return retval;
    }

    @Override
    public void init() throws Exception {
        //unique_id = Guid.NewGuid().toString();
        unique_id = UUID.randomUUID().toString();
        prev_members = new BoundedList(num_prev_mbrs);
        timer = stack != null ? stack.timer : null;
        if (timer == null) {
            throw new Exception("GMS.init(): timer is null");
        }
        if (impl != null) {
            impl.init();
        }
    }

    @Override
    public void start() {
        if (getStack() != null && getStack().getCacheLog() != null) {

        }
        if (impl != null) {
            impl.start();
        }

    }

    @Override
    public void stop() {
        if (impl != null) {
            impl.stop();
        }
        if (prev_members != null) {
            prev_members.removeAll();
        }
    }

    public void becomeCoordinator() {
        CoordGmsImpl tmp = (CoordGmsImpl) impls.get(COORD);

        if (tmp == null) {
            tmp = new CoordGmsImpl(this);
            impls.put(COORD, tmp);
        }
        tmp.leaving = false;
        setImpl(tmp);
    }

    public final boolean getIsCoordinator() {
        return getImpl() instanceof CoordGmsImpl;
    }

    public void becomeParticipant() {
        ParticipantGmsImpl tmp = (ParticipantGmsImpl) impls.get(PART);

        if (tmp == null) {
            tmp = new ParticipantGmsImpl(this);
            impls.put(PART, tmp);
        }
        tmp.leaving = false;
        setImpl(tmp);
    }

    public void becomeClient() {
        ClientGmsImpl tmp = (ClientGmsImpl) impls.get(CLIENT);

        if (tmp == null) {
            tmp = new ClientGmsImpl(this);
            impls.put(CLIENT, tmp);
        }
        tmp.initial_mbrs.clear();
        setImpl(tmp);
    }

    public boolean haveCoordinatorRole() {
        return impl != null && impl instanceof CoordGmsImpl;
    }

    public final void MarkStateTransferInProcess() {
        if (isPartReplica) {
            synchronized (_syncLock) {
                _stateTransferMarkTime = System.currentTimeMillis();
                _stateTransferInProcess = true;
            }
        }
    }

    public final void MarkStateTransferCompleted() {
        if (isPartReplica) {
            _stateTransferInProcess = false;
        }
    }

    public final boolean GetStateTransferStatus() {
        if (isPartReplica) {
            if (_startedAsMirror) {
                return false;
            }
            synchronized (_syncLock) {
                if (!_stateTransferInProcess) {
                    return false;
                }

                //chaning TimeSpan to Millisecond calculation, accuracy dependes on the OS (Win XP resolution ~ 15ms)
                long timeSpent = System.currentTimeMillis() - _stateTransferMarkTime;

                if ((timeSpent / 1000) >= 20) {
                    _stateTransferInProcess = false;
                    return false;
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Computes the next view. Returns a copy that has <code>old_mbrs</code> and
     * <code>suspected_mbrs</code> removed and <code>new_mbrs</code> added.
     */
    public View getNextView(java.util.List new_mbrs, java.util.List old_mbrs, java.util.List suspected_mbrs) {
        java.util.List mbrs;
        long vid = 0;
        View v;
        Membership tmp_mbrs = null;
        Address tmp_mbr;

        synchronized (members) {

            if (view_id == null) {
                getStack().getCacheLog().Error("pb.GMS.getNextView()", "view_id is null");
                return null; // this should *never* happen !
            }
            vid = Math.max(view_id.getId(), ltime) + 1;
            ltime = vid;

            getStack().getCacheLog().Debug("pb.GMS.getNextView()", "VID=" + vid + ", current members=" + Global.CollectionToString(members.getMembers()) + ", new_mbrs="
                    + Global.CollectionToString(new_mbrs) + ", old_mbrs=" + Global.CollectionToString(old_mbrs) + ", suspected_mbrs=" + Global.CollectionToString(suspected_mbrs));

            tmp_mbrs = tmp_members.copy(); // always operate on the temporary membership
            tmp_mbrs.remove(suspected_mbrs);
            tmp_mbrs.remove(old_mbrs);
            tmp_mbrs.add(new_mbrs);
            mbrs = tmp_mbrs.getMembers();
            v = new View(local_addr, vid, mbrs);
            v.setCoordinatorGmsId(unique_id);
            // Update membership (see DESIGN for explanation):
            tmp_members.set(mbrs);

            // Update joining list (see DESIGN for explanation)
            if (new_mbrs != null) {
                for (int i = 0; i < new_mbrs.size(); i++) {
                    tmp_mbr = (Address) new_mbrs.get(i);
                    if (!joining.contains(tmp_mbr)) {
                        joining.add(tmp_mbr);
                    }
                }
            }

            // Update leaving list (see DESIGN for explanations)
            if (old_mbrs != null) {
                for (java.util.Iterator it = old_mbrs.iterator(); it.hasNext();) {
                    Address addr = (Address) it.next();
                    if (!leaving.contains(addr)) {
                        leaving.add(addr);
                    }
                }
            }
            if (suspected_mbrs != null) {
                for (java.util.Iterator it = suspected_mbrs.iterator(); it.hasNext();) {
                    Address addr = (Address) it.next();
                    if (!leaving.contains(addr)) {
                        leaving.add(addr);
                    }
                }
            }

            getStack().getCacheLog().Debug("pb.GMS.getNextView()", "new view is " + v);
            v.setBridgeSourceCacheId(impl.getUniqueId());
            return v;
        }
    }

    /**
     * Compute a new view, given the current view, the new members and the
     * suspected/left members. Then simply mcast the view to all members. This
     * is different to the VS GMS protocol, in which we run a FLUSH protocol
     * which tries to achive consensus on the set of messages mcast in the
     * current view before proceeding to install the next view. The members for
     * the new view are computed as follows:
     * <pre>
     * existing          leaving        suspected          joining
     * 1. new_view      y                 n               n                 y
     * 2. tmp_view      y                 y               n                 y
     * (view_dest)
     * </pre> <ol> <li> The new view to be installed includes the existing
     * members plus the joining ones and excludes the leaving and suspected
     * members. <li> A temporary view is sent down the stack as an
     * <em>event</em>. This allows the bottom layer (e.g. UDP or TCP) to
     * determine the members to which to send a multicast message. Compared to
     * the new view, leaving members are <em>included</em> since they have are
     * waiting for a view in which they are not members any longer before they
     * leave. So, if we did not set a temporary view, joining members would not
     * receive the view (signalling that they have been joined successfully).
     * The temporary view is essentially the current view plus the joining
     * members (old members are still part of the current view). </ol>
     *
     * @return View The new view
     *
     */
    public View castViewChange(java.util.List new_mbrs, java.util.List old_mbrs, java.util.List suspected_mbrs, Object mapsPackage) {

        View new_view;

        new_view = getNextView(new_mbrs, old_mbrs, suspected_mbrs);

        if (new_view == null) {
            return null;
        }

        if (mapsPackage != null) {
            Object[] distributionAndMirrorMaps = (Object[]) mapsPackage;
            if (distributionAndMirrorMaps[0] != null) {
                DistributionMaps maps = (DistributionMaps) distributionAndMirrorMaps[0];

                new_view.setDistributionMaps(maps);
            }

            if (distributionAndMirrorMaps[1] != null) {
                new_view.setMirrorMapping((CacheNode[]) distributionAndMirrorMaps[1]);
            }

        }

        //===============================================
        if (getStack().getCacheLog().getIsInfoEnabled()) {
            getStack().getCacheLog().Info("GMS.castViewChange()", "created a new view id = " + new_view.getVid());
        }
        new_view.setSequencerTbl(this._subGroupMbrsMap);
        if (getStack().getCacheLog().getIsInfoEnabled()) {
            getStack().getCacheLog().Info("GMS.castViewChange()", "new_view.SequencerTbl.count = " + new_view.getSequencerTbl().size());
        }
        new_view.setMbrsSubgroupMap(this._mbrSubGroupMap);
        if (getStack().getCacheLog().getIsInfoEnabled()) {
            getStack().getCacheLog().Info("GMS.castViewChange()", "new_view.MbrsSubgroupMap.count = " + new_view.getMbrsSubgroupMap().size());
        }

        castViewChange(new_view);
        return new_view;
    }

    public void castViewChange(View new_view) {
        castViewChange(new_view, null);
    }

    public void castViewChange(View new_view, Digest digest) {
        Message view_change_msg;
        HDR hdr;

        getStack().getCacheLog().Debug("pb.GMS.castViewChange()", "mcasting view {" + new_view + "} (" + new_view.size() + " mbrs)\n");

        if (new_view != null) {
            new_view.setBridgeSourceCacheId(impl.getUniqueId());
        }

        view_change_msg = new Message(); // bcast to all members

        hdr = new HDR(HDR.VIEW, new_view);
        hdr.digest = digest;
        view_change_msg.putHeader(HeaderType.GMS, hdr);
        Object tempVar = GenericCopier.DeepCopy(new_view.getMembers());//.clone();
        view_change_msg.setDests((java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null));

        if (stack.getCacheLog().getIsInfoEnabled()) {
            getStack().getCacheLog().Info("CastView.Watch", "Count of members: " + new_view.getMembers().size());
        }

        _promise = new ViewPromise(new_view.getMembers().size());

        boolean waitForViewAcknowledgement = true;
        if (!new_view.containsMember(local_addr)) {
            waitForViewAcknowledgement = false;
            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("GMS.castViewChange()", "I am coordinator and i am leaving");
            }
            passDown(new Event(Event.MSG, view_change_msg, Priority.Critical));
        } else {
            passDown(new Event(Event.MSG, view_change_msg, Priority.Critical));
        }

        if (waitForViewAcknowledgement) {
            try {
                _promise.WaitResult(_castViewChangeTimeOut);
            } catch (InterruptedException interruptedException) {
                getStack().getCacheLog().Error("GMS.castViewChange()", "1st promise.WaitResult inturrupted");
            }

            if (!_promise.AllResultsReceived()) {
                Object tempVar2 = GenericCopier.DeepCopy(new_view.getMembers());//.clone();
                view_change_msg.setDests((java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null));
                passDown(new Event(Event.MSG, view_change_msg, Priority.Critical));
                try {
                    _promise.WaitResult(_castViewChangeTimeOut);
                } catch (InterruptedException interruptedException) {
                    getStack().getCacheLog().Error("GMS.castViewChange()", "1st promise.WaitResult inturrupted");
                }
            }

            if (_promise.AllResultsReceived()) {
                getStack().getCacheLog().CriticalInfo("GMS.castViewChange()", "View applied");
            }
        }
    }

    /**
     * Sets the new view and sends a VIEW_CHANGE event up and down the stack. If
     * the view is a MergeView (subclass of View), then digest will be non-null
     * and has to be set before installing the view.
     *
     * @param new_view
     * @param digest
     */
    public void installView(View new_view, Digest digest) {
        if (digest != null) {
            mergeDigest(digest);
        }
        installView(new_view);
    }

    private void SendViewAcknowledgment(Address coordinator) {
        Message m = new Message(coordinator, null, null);
        HDR hdr = new HDR(HDR.VIEW_RESPONSE, true);
        m.putHeader(HeaderType.GMS, hdr);
        passDown(new Event(Event.MSG, m, Priority.Critical));
    }

    /**
     * Sets the new view and sends a VIEW_CHANGE event up and down the stack.
     *
     * @param new_view
     */
    public void installView(View new_view) {
        getStack().getCacheLog().CriticalInfo("GMS.InstallView", "Installing new View " + local_addr.toString() + " --> " + new_view);

        Address coord = null;
        try {
            //Lest inform coordinator about view receiption
            SendViewAcknowledgment(new_view.getCoordinator());

            int rc;
            ViewId vid = new_view.getVid();
            java.util.List mbrs = new_view.getMembers();

            impl.setUniqueId(new_view.getBridgeSourceCacheId());
            _uniqueID = new_view.getBridgeSourceCacheId();

            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("GMS.installView", "[local_addr=" + local_addr + "] view is " + new_view);
            }

            // Discards view with id lower than our own. Will be installed without check if first view
            if (view_id != null) {
                rc = vid.compareTo(view_id);
                if (rc <= 0) {
                    getStack().getCacheLog().Error("[" + local_addr + "] received view <= current view;" + " discarding it (current vid: " + view_id + ", new vid: " + vid + ')');
                    Event viewEvt = new Event(Event.VIEW_CHANGE_OK, null, Priority.Critical);
                    passDown(viewEvt);
                    return;
                }

                Address currentCoodinator = determineCoordinator();
                Address newCoordinator = new_view.getCoordinator();
                Address sender = vid.getCoordAddress(); // creater of the view

                if (!currentCoodinator.equals(newCoordinator) && !newCoordinator.equals(local_addr) && !sender.equals(currentCoodinator)) {

                    getStack().getCacheLog().CriticalInfo("GMS.InstallView", "Force Join Cluster");
                    if (!new_view.getForceInstall()) {
                        if (!VerifySuspect(currentCoodinator)) {
                            getStack().getCacheLog().Error("GMS.installView", "rejecting the view from " + newCoordinator + " as my own coordinator[" + currentCoodinator
                                    + "] is not down");
                            Event viewEvt = new Event(Event.VIEW_CHANGE_OK, null, Priority.Critical);

                            passDown(viewEvt);

                            //we should inform the coordinator of this view that i can't be the member
                            //of your view as my own coordinator is alive.
                            Message msg = new Message(new_view.getCoordinator(), null, new byte[0]);
                            msg.putHeader(HeaderType.GMS, new GMS.HDR(GMS.HDR.VIEW_REJECTED, local_addr));
                            passDown(new Event(Event.MSG, msg, Priority.Critical));

                            return;
                        }
                    }
                }
            }

            ltime = Math.max(vid.getId(), ltime); // compute Lamport logical time

            /*
             * Check for self-inclusion: if I'm not part of the new membership, I just discard it. This ensures that messages sent in view V1 are only received by members of V1
             */
            if (checkSelfInclusion(mbrs) == false) {
                getStack().getCacheLog().Error("GMS.InstallView", "CheckSelfInclusion() failed, " + local_addr + " is not a member of view " + new_view + "; discarding view");

                // only shun if this member was previously part of the group. avoids problem where multiple
                // members (e.g. X,Y,Z) join {A,B} concurrently, X is joined first, and Y and Z get view
                // {A,B,X}, which would cause Y and Z to be shunned as they are not part of the membership
                // bela Nov 20 2003
                if (shun && local_addr != null && prev_members.contains(local_addr)) {
                    getStack().getCacheLog().CriticalInfo("I (" + local_addr + ") am being shunned, will leave and " + "rejoin group (prev_members are " + prev_members + ')');
                    passUp(new Event(Event.EXIT));
                }
                return;
            }

            synchronized (members) {
                //@UH Members are same as in the previous view. No need to apply view
                if (view_id != null) {
                    Membership newMembers = new Membership(mbrs);
                    if (members.equals(newMembers) && vid.getCoordAddress().equals(view_id.getCoordAddress())) {

                        getStack().getCacheLog().Error("GMS.InstallView", "[" + local_addr + "] received view has the same members as current view;"
                                + " discarding it (current vid: " + view_id + ", new vid: " + vid + ')');
                        Event viewEvt = new Event(Event.VIEW_CHANGE_OK, null, Priority.Critical);
                        try {
                            //joining, leaving and tmp_members are needed to be synchronized even if view is same
                            Global.ICollectionSupport.RemoveAll(new ArrayList(joining), new ArrayList(mbrs));
                            // remove all elements from 'leaving' that are not in 'mbrs'
                            Global.ICollectionSupport.RetainAll(new ArrayList(leaving), new ArrayList(mbrs));
                        } catch (Exception ex) {

                        }

                        passDown(viewEvt);
                        return;
                    }
                }

                //=========================================
                //
                getStack().getCacheLog().CriticalInfo("GMS.InstallView", "Installing view in gms");

                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("GMS.InstallView ", new_view.toString() + "\\n" + "seq tble : " + new_view.getSequencerTbl().size());
                }
                Object tempVar = new_view.getSequencerTbl().clone();
                this._subGroupMbrsMap = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                Object tempVar2 = new_view.getMbrsSubgroupMap().clone();
                this._mbrSubGroupMap = (java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null);
                //=========================================

                // serialize access to views
                // assign new_view to view_id
                view_id = vid.Copy();
                getStack().getCacheLog().CriticalInfo("GMS.InstallView", "=== View ID = " + view_id.toString());

                // Set the membership. Take into account joining members
                if (mbrs != null && mbrs.size() > 0) {
                    for (int i = 0; i < members.size(); i++) {
                        Address mbr = members.elementAt(i);
                        if (!mbrs.contains(mbr)) {
                            RemoveGmsId(mbr);
                        }
                    }

                    for (int i = 0; i < members.size(); i++) {
                        Address mbr = members.elementAt(i);
                        if (!mbrs.contains(mbr)) {
                            removePartition(mbr);
                        }
                    }

                    HashMap gmsIds = new_view.getGmsIds();

                    if (gmsIds != null) {
                        Iterator ide = gmsIds.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry ent = (Map.Entry) ide.next();
                            if (getStack().getCacheLog().getIsInfoEnabled()) {
                                getStack().getCacheLog().Info("GMS.InstallView", "mbr  = " + ent.getKey() + " ; gms_id = " + ent.getValue());
                            }
                            AddGmsId((Address) ent.getKey(), (String) ent.getValue());
                        }
                    }

                    HashMap partitions = new_view.getPartitions();

                    if (partitions != null) {
                        Iterator ide = partitions.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry ent = (Map.Entry) ide.next();
                            if (getStack().getCacheLog().getIsInfoEnabled()) {
                                getStack().getCacheLog().Info("GMS.InstallView", "mbr  = " + ent.getKey() + " ; partition_id = " + ent.getValue());
                            }
                            addPartition((Address) ent.getKey(), (String) ent.getValue());
                        }
                    }

                    for (Object mbr : mbrs) {
                        getStack().getCacheLog().CriticalInfo("GMS.InstallView", "Members.set = " + mbr != null ? mbr.toString() : null);
                    }

                    members.set(mbrs);
                    tmp_members.set(members);
                    joining.removeAll(mbrs);
                    leaving.removeAll(mbrs);
                    // Global.ICollectionSupport.RemoveAll(joining, mbrs); // remove all members in mbrs from joining
                    // remove all elements from 'leaving' that are not in 'mbrs'
                    // Global.ICollectionSupport.RetainAll(leaving, mbrs);

                    tmp_members.add(joining); // add members that haven't yet shown up in the membership
                    tmp_members.remove(leaving); // remove members that haven't yet been removed from the membership

                    // add to prev_members
                    for (java.util.Iterator it = mbrs.iterator(); it.hasNext();) {
                        Address addr = (Address) it.next();
                        if (!prev_members.contains(addr)) {
                            prev_members.add(addr);
                        }
                    }
                }

                // Send VIEW_CHANGE event up and down the stack:
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("GMS.installView", "broadcasting view change within stack");
                }

                coord = determineCoordinator();

                // changed on suggestion by yaronr and Nicolas Piedeloupe
                if (coord != null && coord.equals(local_addr) && !haveCoordinatorRole()) {
                    becomeCoordinator();
                } else {
                    if (haveCoordinatorRole() && !local_addr.equals(coord)) {
                        becomeParticipant();
                    }
                }
                if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(new_view.getBridgeSourceCacheId())) {
                    new_view.setBridgeSourceCacheId(impl.getUniqueId());
                }

                MarkStateTransferInProcess();
                Event view_event = new Event(Event.VIEW_CHANGE, new_view.clone(), Priority.Critical);
                passDown(view_event); // needed e.g. by failure detector or UDP
            }
        } finally {
        }
    }

    protected Address determineCoordinator() {
        synchronized (members) {
            return members != null && members.size() > 0 ? (Address) members.elementAt(0) : null;
        }
    }

    public final void AddGmsId(Address node, String id) {
        if (node != null) {
            nodeGmsIds.put(node, id);
        }
    }

    public final String GetNodeGMSId(Address node) {
        if (node != null) {
            return (String) ((nodeGmsIds.get(node) instanceof String) ? nodeGmsIds.get(node) : null);
        }

        return null;
    }

    public final void RemoveGmsId(Address node) {
        if (node != null) {
            nodeGmsIds.remove(node);
        }
    }

    public final void RemoveGmsId(java.util.ArrayList nodes) {
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            Address node = (Address) it.next();
            if (node != null) {
                nodeGmsIds.remove(node);
            }
        }
    }

    public final java.util.HashMap getGmsIds() {
        return nodeGmsIds;
    }

    public final String GetGmsId(Address node) {
        return (String) ((nodeGmsIds.get(node) instanceof String) ? nodeGmsIds.get(node) : null);
    }

    public void addPartition(Address node, String id) {
        if (node != null) {
            partitionsIds.put(node, id);
        }
    }

    public void removePartition(Address node) {
        if (node != null) {
            partitionsIds.remove(node);
        }
    }

    public void removePartition(ArrayList nodes) {
        for (Object obj : nodes) {
            Address node = Common.as(obj, Address.class);
            if (node != null) {
                partitionsIds.remove(node);
            }
        }
    }

    public HashMap getPartitions() {
        return partitionsIds;
    }

    public String getPartition(Address node) {
        return (String) ((partitionsIds.get(node) instanceof String) ? partitionsIds.get(node) : null);
    }

    /**
     * Checks whether the potential_new_coord would be the new coordinator (2nd
     * in line)
     */
    protected boolean wouldBeNewCoordinator(Address potential_new_coord) {
        Address new_coord = null;

        if (potential_new_coord == null) {
            return false;
        }

        synchronized (members) {
            if (members.size() < 2) {
                return false;
            }
            new_coord = (Address) members.elementAt(1); // member at 2nd place
            if (new_coord != null && new_coord.equals(potential_new_coord)) {
                return true;
            }
            return false;
        }
    }

    /**
     * Returns true if local_addr is member of mbrs, else false
     */
    protected boolean checkSelfInclusion(java.util.List mbrs) {
        Object mbr;
        if (mbrs == null) {
            return false;
        }
        for (int i = 0; i < mbrs.size(); i++) {
            mbr = mbrs.get(i);
            if (mbr != null && local_addr.equals(mbr)) {
                return true;
            }
        }
        return false;
    }

    public View makeView(java.util.ArrayList mbrs) {
        Address coord = null;
        long id = 0;

        if (view_id != null) {
            coord = view_id.getCoordAddress();
            id = view_id.getId();
        }
        View view = new View(coord, id, mbrs);
        view.setCoordinatorGmsId(unique_id);
        return view;
    }

    public View makeView(java.util.ArrayList mbrs, ViewId vid) {
        Address coord = null;
        long id = 0;

        if (vid != null) {
            coord = vid.getCoordAddress();
            id = vid.getId();
        }
        View view = new View(coord, id, mbrs);
        view.setCoordinatorGmsId(unique_id);
        return view;
    }

    /**
     * Send down a MERGE_DIGEST event
     */
    public void mergeDigest(Digest d) {
        passDown(new Event(Event.MERGE_DIGEST, d));
    }

    @Override
    public void up(final Event evt) {
        Object obj;
        Message msg;
        final HDR hdr;
        MergeData merge_data;

        switch (evt.getType()) {

            case Event.MSG:
                msg = (Message) evt.getArg();

                obj = msg.getHeader(HeaderType.GMS);
                if (obj == null || !(obj instanceof HDR)) {
                    break;
                }
                hdr = (HDR) msg.removeHeader(HeaderType.GMS);
                switch (hdr.type) {

                    case HDR.JOIN_REQ:
                        final Object[] args = new Object[5];
                        args[0] = hdr.mbr;
                        args[1] = hdr.subGroup_name;
                        args[2] = hdr.isStartedAsMirror;
                        args[3] = hdr.getGMSId();
                        args[4] = hdr.getPartitionId();

                        Thread workerThread = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    handleJoinrequestAsync(args);
                                } catch (InterruptedException ex) {
                                    if (getStack().getCacheLog().getIsErrorEnabled()) {
                                        getStack().getCacheLog().Error("GMS.Up() Received JOIN_REQ ", ex.toString());
                                    }
                                }
                            }
                        });
                        workerThread.start();

                        break;

                    case HDR.SPECIAL_JOIN_REQUEST:
                        HandleSpecialJoinRequest(hdr.mbr, hdr.getGMSId());
                        break;

                    case HDR.JOIN_RSP:
                        MarkStateTransferInProcess();
                        impl.handleJoinResponse(hdr.join_rsp);
                        break;

                    case HDR.LEAVE_REQ:
                        getStack().getCacheLog().Debug("received LEAVE_REQ " + hdr + " from " + msg.getSrc());

                        if (hdr.mbr == null) {
                            getStack().getCacheLog().Error("LEAVE_REQ's mbr field is null");
                            return;
                        }

                        if (isPartReplica && getIsCoordinator()) {
                            //if replica node on the coordinator is leaving then send a special event to TCP
                            //to mark himself leaving. This way other node asking for death status through keep
                            //alive will get dead status.
                            if (hdr.mbr != null && hdr.mbr.getIpAddress().equals(local_addr.getIpAddress())) {
                                down(new Event(Event.I_AM_LEAVING));
                            }
                        }
                        Thread workerThread2 = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                handleLeaveAsync(new Object[]{hdr.mbr, false});
                            }
                        });
                        workerThread2.start();

                        break;

                    case HDR.LEAVE_RSP:
                        impl.handleLeaveResponse();
                        break;

                    case HDR.VIEW_RESPONSE:
                        if (_promise != null) {
                            _promise.SetResult(hdr.arg);
                        }
                        break;

                    case HDR.VIEW:
                        if (hdr.view == null) {
                            getStack().getCacheLog().Error("[VIEW]: view == null");
                            return;
                        } else {
                            getStack().getCacheLog().CriticalInfo("gms.Up", "received view from :" + msg.getSrc() + " ; view = " + hdr.view);
                        }
                        impl.handleViewChange(hdr.view, hdr.digest);
                        break;

                    case HDR.MERGE_REQ:
                        impl.handleMergeRequest(msg.getSrc(), hdr.merge_id);
                        break;

                    case HDR.MERGE_RSP:
                        merge_data = new MergeData(msg.getSrc(), hdr.view, hdr.digest);
                        merge_data.merge_rejected = hdr.merge_rejected;
                        impl.handleMergeResponse(merge_data, hdr.merge_id);
                        break;

                    case HDR.INSTALL_MERGE_VIEW:
                        impl.handleMergeView(new MergeData(msg.getSrc(), hdr.view, hdr.digest), hdr.merge_id);
                        break;

                    case HDR.CANCEL_MERGE:
                        impl.handleMergeCancelled(hdr.merge_id);
                        break;

                    case HDR.CAN_NOT_CONNECT_TO:
                        impl.handleCanNotConnectTo(msg.getSrc(), hdr.nodeList);
                        break;

                    case HDR.LEAVE_CLUSTER:

                        String gmsId = (String) ((hdr.arg instanceof String) ? hdr.arg : null); //reported gms id
                        String myGmsId = GetNodeGMSId(local_addr);

                        if (gmsId != null && myGmsId != null && gmsId.equals(myGmsId)) {
                            Thread workerThread1 = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    handleLeaveClusterRequestAsync(hdr.mbr);
                                }
                            });
                            workerThread1.start();

                        }
                        break;

                    case HDR.CONNECTION_BROKEN:
                        impl.handleConnectionBroken(msg.getSrc(), hdr.mbr);
                        break;

                    case HDR.VIEW_REJECTED:
                        impl.handleViewRejected(hdr.mbr);
                        break;

                    case HDR.INFORM_NODE_REJOINING:
                        impl.handleInformNodeRejoining(msg.getSrc(), hdr.mbr);
                        break;

                    case HDR.RESET_ON_NODE_REJOINING:
                        impl.handleResetOnNodeRejoining(msg.getSrc(), hdr.mbr, hdr.view);
                        break;

                    case HDR.RE_CHECK_CLUSTER_HEALTH:

                        Thread t = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                impl.ReCheckClusterHealth(hdr.mbr);
                            }
                        });

                        t.start();

                        break;

                    case HDR.INFORM_ABOUT_NODE_DEATH:
                        //Replica is not supposed to handle this event
                        if (isPartReplica && _startedAsMirror) {
                            break;
                        }

                        impl.handleInformAboutNodeDeath(msg.getSrc(), (Address) hdr.arg);
                        break;

                    case HDR.IS_NODE_IN_STATE_TRANSFER:
                        impl.handleIsClusterInStateTransfer(msg.getSrc());
                        break;

                    case HDR.IS_NODE_IN_STATE_TRANSFER_RSP:
                        if (_stateTransferPromise != null) {
                            if (getStack().getCacheLog().getIsInfoEnabled()) {
                                getStack().getCacheLog().Info("gms.UP", "(state transfer rsp) sender: " + msg.getSrc() + " ->" + hdr.arg);
                            }
                            _stateTransferPromise.SetResult(hdr.arg);
                        }
                        break;

                    default:
                        getStack().getCacheLog().Error("HDR with type=" + hdr.type + " not known");
                        break;

                }

                return; // don't pass up

            case Event.CONNECT_OK:
            // sent by someone else, but WE are responsible for sending this !
            case Event.DISCONNECT_OK: // dito (e.g. sent by UDP layer). Don't send up the stack
                return;

            case Event.GET_NODE_STATUS_OK:
                synchronized (suspect_verify_mutex) {
                    Object tempVar = evt.getArg();
                    NodeStatus status = (NodeStatus) ((tempVar instanceof NodeStatus) ? tempVar : null);
                    if (status.getNode() != null && status.getNode().equals(nodeTobeSuspect)) {
                        nodeStatus = status;
                        Monitor.pulse(suspect_verify_mutex);// suspect_verify_mutex.notifyAll();
                    }
                }
                break;

            case Event.SET_LOCAL_ADDRESS:
                local_addr = (Address) evt.getArg();

                break; // pass up

            case Event.SUSPECT:

                Thread workerThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            handleSuspectAsync(evt.getArg());
                        } catch (InterruptedException ex) {
                            if (getStack().getCacheLog().getIsErrorEnabled()) {
                                getStack().getCacheLog().Error("gms.Up() Event.SUSPECT ", ex.toString());
                            }
                        }
                    }
                });
                workerThread.start();

                break; // pass up

            case Event.UNSUSPECT:
                impl.unsuspect((Address) evt.getArg());
                return; // discard

            case Event.MERGE:
                impl.merge((java.util.ArrayList) evt.getArg());
                return; // don't pass up

            case Event.CONNECTION_FAILURE:

                Object tempVar2 = evt.getArg();
                impl.handleConnectionFailure((java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null));
                return; //dont passup

            case Event.NODE_REJOINING:

                Object tempVar3 = evt.getArg();
                impl.handleNodeRejoining((Address) ((tempVar3 instanceof Address) ? tempVar3 : null));
                return;

            case Event.CONNECTION_BREAKAGE:
                Object tempVar4 = evt.getArg();
                Address node = (Address) ((tempVar4 instanceof Address) ? tempVar4 : null);
                if (!disconnected_nodes.contains(node)) {
                    disconnected_nodes.add(node);
                }
                break;

            case Event.CONNECTION_RE_ESTABLISHED:
                Object tempVar5 = evt.getArg();
                node = (Address) ((tempVar5 instanceof Address) ? tempVar5 : null);
                if (disconnected_nodes.contains(node)) {
                    disconnected_nodes.remove(node);
                }
                break;
        }

        if (impl.handleUpEvent(evt)) {
            passUp(evt);
        }
    }

    /**
     * This method is overridden to avoid hanging on getDigest(): when a JOIN is
     * received, the coordinator needs to retrieve the digest from the PBCAST
     * layer. It therefore sends down a GET_DIGEST event, to which the PBCAST
     * layer responds with a GET_DIGEST_OK event.<p>
     * However, the GET_DIGEST_OK event will not be processed because the thread
     * handling the JOIN request won't process the GET_DIGEST_OK event until the
     * JOIN event returns. The receiveUpEvent() method is executed by the
     * up-handler thread of the lower protocol and therefore can handle the
     * event. All we do here is unblock the mutex on which JOIN is waiting,
     * allowing JOIN to return with a valid digest. The GET_DIGEST_OK event is
     * then discarded, because it won't be processed twice.
     */
    @Override
    public void receiveUpEvent(Event evt) {
        if (evt.getType() == Event.GET_DIGEST_OK) {
            synchronized (digest_mutex) {
                digest = (Digest) evt.getArg();
                Monitor.pulse(digest_mutex);//digest_mutex.notifyAll();
            }
            return;
        }
        super.receiveUpEvent(evt);
    }

    public final boolean VerifySuspect(Address suspect) {
        return VerifySuspect(suspect, true);
    }

    /**
     * Verifes whether the given node is dead or not.
     *
     * @param suspect suspected node
     * @return true, if node is dead otherwise false
     */
    public final boolean VerifySuspect(Address suspect, boolean matchGmsId) {
        boolean isDead = true;
        String gmsId = null;
        if (suspect != null) {
            getStack().getCacheLog().CriticalInfo("GMS.VerifySuspect", " verifying the death of node " + suspect);
            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("GMS.VerifySuspect", " verifying the death of node " + suspect);
            }

            Object tempVar = getGmsIds().get(suspect);
            gmsId = (String) ((tempVar instanceof String) ? tempVar : null);
            synchronized (suspect_verify_mutex) {
                nodeStatus = null;
                nodeTobeSuspect = suspect;
                passDown(new Event(Event.GET_NODE_STATUS, suspect, Priority.Critical));
                //we wait for the verification

                try {
                    com.alachisoft.tayzgrid.common.threading.Monitor.wait(suspect_verify_mutex);// suspect_verify_mutex.wait();
                } catch (InterruptedException interruptedException) {
                    getStack().getCacheLog().Error("GMS.VerifySuspect", "suspect_verify_mutex interrupted");
                }

                if (nodeStatus != null) {
                    if (getStack().getCacheLog().getIsInfoEnabled()) {
                        getStack().getCacheLog().Info("GMS.VerifySuspect", " node status is " + nodeStatus.toString());
                    }
                    switch (nodeStatus.getStatus()) {
                        case NodeStatus.IS_ALIVE:
                            isDead = false;
                            break;
                        case NodeStatus.IS_DEAD:
                            isDead = true;
                            break;
                        case NodeStatus.IS_LEAVING:
                            isDead = true;
                            break;

                    }
                }
            }

        }

        if (isDead && matchGmsId) {
            //we verify whether current gms id is same as when node was reported suspect.
            Object tempVar2 = getGmsIds().get(suspect);
            String currentGmsId = (String) ((tempVar2 instanceof String) ? tempVar2 : null);

            if (currentGmsId != null && gmsId != null && currentGmsId.equals(gmsId)) {
                return true;
            } else {
                if (getStack().getCacheLog().getIsErrorEnabled()) {
                    getStack().getCacheLog().CriticalInfo("GMS.VerifySuspect", "node gms ids differ; old : " + gmsId + " new: " + currentGmsId + nodeStatus.toString());
                }
                return false;
            }
        }

        return isDead;
    }

    /**
     * We inform other nodes about the possible death of coordinator
     *
     *
     * @param otherNodes
     */
    public final void InformOthersAboutCoordinatorDeath(java.util.ArrayList otherNodes, Address deadNode) {
        if (otherNodes != null && otherNodes.size() > 0) {
            Message msg = new Message(null, null, new byte[0]);
            msg.setDests(otherNodes);
            GMS.HDR hdr = new HDR(GMS.HDR.INFORM_ABOUT_NODE_DEATH);
            hdr.arg = deadNode;
            msg.putHeader(HeaderType.GMS, hdr);
            down(new Event(Event.MSG, msg, Priority.Critical));
        }
    }

    /**
     * Checks if state transfer is in progress any where in the cluster. First
     * status is checked on local node. If current node is not in state transfer
     * then it is verified from other nodes in the cluster.
     *
     * @return
     */
    public final boolean IsClusterInStateTransfer() throws InterruptedException {

        if (GetStateTransferStatus()) {
            return true;
        }

        //check with other members
        if (this.members != null) {
            java.util.List allmembers = this.members.getMembers();

            if (allmembers != null && allmembers.size() > 0) {
                _stateTransferPromise = new SateTransferPromise(allmembers.size());

                Message msg = new Message(null, null, new byte[0]);
                msg.setDests(allmembers);
                GMS.HDR hdr = new HDR(GMS.HDR.IS_NODE_IN_STATE_TRANSFER);
                msg.putHeader(HeaderType.GMS, hdr);
                down(new Event(Event.MSG, msg, Priority.Critical));

                Object objectState = _stateTransferPromise.WaitResult(_stateTransferQueryTimesout);

                boolean isInstateTransfer = objectState != null ? (Boolean) objectState : false; //(Boolean) _stateTransferPromise.WaitResult(_stateTransferQueryTimesout);

                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("gms.IsClusterInStateTransfer", "result : " + (isInstateTransfer));
                }
                return isInstateTransfer;
            }
        }

        return false;
    }

    @Override
    public void down(Event evt) {
        switch (evt.getType()) {

            case Event.NOTIFY_LEAVING:
                impl.handleNotifyLeaving();
                return;
            case Event.CONNECT_PHASE_2:
                boolean isStartedAsMirror = (Boolean) evt.getArg();
                _startedAsMirror = isStartedAsMirror;
                impl.join(local_addr, isStartedAsMirror);
                passUp(new Event(Event.CONNECT_OK_PHASE_2));
                break;

            case Event.MARK_CLUSTER_IN_STATETRANSFER:
                MarkStateTransferInProcess();
                break;

            case Event.MARK_CLUSTER_STATETRANSFER_COMPLETED:
                MarkStateTransferCompleted();
                break;

            case Event.CONNECT:
                passDown(evt);
                isStartedAsMirror = false;
                boolean twoPhaseConnect = false;
                try {
                    Object[] addrs = (Object[]) evt.getArg();
                    group_addr = (String) addrs[0];
                    subGroup_addr = (String) addrs[1];

                    isStartedAsMirror = (Boolean) addrs[2];
                    twoPhaseConnect = (Boolean) addrs[3];
                } catch (ClassCastException e)// InvalidCastException changed to ClassCastException
                {
                    getStack().getCacheLog().Error("[CONNECT]: group address must be a string (channel name)", e.getMessage());
                }
                if (local_addr == null) {
                    if (getStack().getCacheLog().getIsInfoEnabled()) {
                        getStack().getCacheLog().Info("[CONNECT] local_addr is null");
                    }
                }
                if (!twoPhaseConnect) {
                    impl.join(local_addr, isStartedAsMirror);
                }

                passUp(new Event(Event.CONNECT_OK));
                return; // don't pass down: was already passed down

            case Event.DISCONNECT:
                impl.leave((Address) evt.getArg());
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException interruptedException) {
                    getStack().getCacheLog().Error("GMS.down()", "Event.DISCONNECT Sleep interrupted ");
                }
                getStack().getCacheLog().Info("GMS.down()", "passing up DISCONNECT_OK ");

                passUp(new Event(Event.DISCONNECT_OK));
                initState(); // in case connect() is called again
                break; // pass down

            case Event.HASHMAP_RESP:
                synchronized (acquireMap_mutex) {
                    _hashmap = evt.getArg();
                    getStack().getCacheLog().Debug("pbcast.GMS.down()", " DistributionMap and MirrorMap response received.");
                    getStack().getCacheLog().Debug("pbcast.GMS.down()", _hashmap == null ? "null map..." : "maps package received.");
                    //pulse the thread waiting to send join response.
                    Monitor.pulse(acquireMap_mutex);
                }
                break;

            case Event.CONFIRM_CLUSTER_STARTUP:
                Object[] arg = (Object[]) evt.getArg();
                boolean isPOR = (Boolean) arg[0];
                int retryNumber = (Integer) arg[1];

                if (!_doReDiscovery) {
                    return;
                }
                //If the cluster is singleton perform network cluster checkup
                if (isPOR) {
                    if (members.getMembers().size() > 2) {

                        return;
                    }
                }
                //This will always be true but to be sure

                if (haveCoordinatorRole()) {
                    CoordGmsImpl coordImpl = (CoordGmsImpl) ((impl instanceof CoordGmsImpl) ? impl : null);
                    if (coordImpl != null) {
                        try {
                            if (!coordImpl.CheckOwnClusterHealth(isPOR, retryNumber)) {

                            }
                        } catch (InterruptedException ex) {
                            if (getStack().getCacheLog().getIsErrorEnabled()) {
                                getStack().getCacheLog().Error("gms.Up() Event.CONFIRM_CLUSTER_STARTUP ", ex.toString());
                            }
                        }
                    }
                }

                return;
            case Event.HAS_STARTED:
                synchronized (join_mutex) {
                    _isStarting = false;
                }
                break;
            case Event.ASK_JOIN_RESPONSE:
                synchronized (acquirePermission_mutex) {
                    _allowJoin = (Boolean) evt.getArg();

                    //pulse the thread waiting to send join response.
                    Monitor.pulse(acquirePermission_mutex);
                }
                break;

        }

        if (impl.handleDownEvent(evt)) {
            passDown(evt);
        }
    }

    /**
     * Generates the next view Id.
     *
     * @return
     */
    public final ViewId GetNextViewId() {
        long vid = -1;
        synchronized (members) {
            if (view_id == null) {
                getStack().getCacheLog().Error("pb.GMS.getNextView()", "view_id is null");
                return null; // this should *never* happen !
            }
            vid = Math.max(view_id.getId(), ltime) + 1;
            ltime = vid;
        }
        return new ViewId(local_addr, vid);
    }

    /**
     * Setup the Protocol instance according to the configuration string
     */
    @Override
    public boolean setProperties(java.util.HashMap props) throws Exception {
        super.setProperties(props);

        if (stack.getStackType() == ProtocolStackType.TCP) {
            this.up_thread = false;
            this.down_thread = false;
            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info(getName() + ".setProperties", "part of TCP stack");
            }
        }
        if (props.containsKey("shun")) {
            shun = Boolean.parseBoolean((String) props.get("shun"));
            props.remove("shun");
        }

        if (props.containsKey("is_part_replica")) {
            isPartReplica = Boolean.parseBoolean((String) props.get("is_part_replica"));
            props.remove("is_part_replica");
        }

        if (props.containsKey("print_local_addr")) {
            print_local_addr = Boolean.parseBoolean((String) props.get("print_local_addr"));
            props.remove("print_local_addr");
        }

        if (props.containsKey("join_timeout")) {
            join_timeout = Long.decode((String) props.get("join_timeout"));
            props.remove("join_timeout");
        }

        if (props.containsKey("join_retry_count")) {
            join_retry_count = Integer.decode((String) props.get("join_retry_count"));
            props.remove("join_retry_count");
        }

        if (props.containsKey("join_retry_timeout")) {
            join_retry_timeout = Long.decode((String) props.get("join_retry_timeout")) * 1000;
            props.remove("join_retry_timeout");
        }

        if (props.containsKey("leave_timeout")) {
            leave_timeout = Long.decode((String) props.get("leave_timeout"));
            props.remove("leave_timeout");
        }

        if (props.containsKey("merge_timeout")) {
            merge_timeout = Long.decode((String) props.get("merge_timeout"));
            props.remove("merge_timeout");
        }

        if (props.containsKey("digest_timeout")) {
            digest_timeout = Long.decode((String) props.get("digest_timeout"));
            props.remove("digest_timeout");
        }

        if (props.containsKey("disable_initial_coord")) {
            disable_initial_coord = Boolean.parseBoolean((String) props.get("disable_initial_coord"));
            props.remove("disable_initial_coord");
        }

        if (props.containsKey("num_prev_mbrs")) {
            num_prev_mbrs = Integer.decode((String) props.get("num_prev_mbrs"));
            props.remove("num_prev_mbrs");
        }

        if (props.size() > 0) {
            getStack().getCacheLog().Error("GMS.setProperties(): the following properties are not recognized: \n" + Global.CollectionToString(props.keySet()));
            return true;
        }
        return true;
    }

    public final boolean CheckAllNodesConnected(java.util.ArrayList memebers) {
        return false;
    }

    /*
     * ------------------------------- Private Methods ---------------------------------
     */
    private void sendUp(Object data) {
        Event evt = (Event) ((data instanceof Event) ? data : null);
        up(evt);
    }

    public void initState() {
        becomeClient();
        view_id = null;
    }

    /**
     *
     * @param mbrs
     * @param isJoining
     * @param subGroup
     * @param isStartedAsMirror
     * @param partitionId
     */
    public final void acquireHashmap(ArrayList mbrs, boolean isJoining, String subGroup, boolean isStartedAsMirror,  String partitionId) {
        int maxTries = 3;
        //muds:
        //new code for getting hash map from caching layer.
        synchronized (acquireMap_mutex) {
            //+ 20110910 -> In NCache there was a problem when sometime on starting the cache the
            //Everything get's hang we get null reference exception.
            //The problem fixed here was that we were not reseting the _hashmap before requesting the
            //new hashmap and in that case even when we wont get hashmap in three seconds we sent the old hashmap
            //to the joining node. For more details you can compare this code with the previous version in VSS.
            final Event evt = new Event();
            evt.setType(Event.HASHMAP_REQ);
            evt.setArg(new Object[]{
                mbrs, isJoining, subGroup, isStartedAsMirror, partitionId
            });
            _hashmap = null; //Reseting because it will be set by the down() method of GMS when upper layer will give the new hashmap
            Thread workerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    sendUp(evt);
                }
            });
            workerThread.start();

            getStack().getCacheLog().CriticalInfo("pbcast.GMS.acquireHashmap()", (isStartedAsMirror ? "Mirror" : "") + " request the caching layer for hashmap");
            if (getStack().getCacheLog().getIsInfoEnabled()) {
                getStack().getCacheLog().Info("pbcast.GMS.acquireHashmap()", (isStartedAsMirror ? "Mirror" : "") + " request the caching layer for hashmap");
            }
            //we wait for maximum 3 seconds to acquire a hashmap ( 3 * 3[retries]] = 9 seconds MAX)
            do {

                getStack().getCacheLog().CriticalInfo("pbcast.GMS.acquireHashmap()", "Going to wait on acquireMap_mutex try->" + (new Integer(maxTries)).toString());

                try {
                    Monitor.wait(acquireMap_mutex, 3000);
                } catch (InterruptedException interruptedException) {
                    getStack().getCacheLog().Error("pbcast.GMS.acquireHashmap()", "wait interrupted");
                }

                getStack().getCacheLog().CriticalInfo("pbcast.GMS.acquireHashmap()", "Return from wait on acquireMap_mutex try->" + (new Integer(maxTries)).toString());

                if (_hashmap != null) {
                    getStack().getCacheLog().CriticalInfo("pbcast.GMS.acquireHashmap()", "hashmap:" + _hashmap.toString());
                    break;
                }

                maxTries--;
                if (getStack().getCacheLog().getIsInfoEnabled()) {
                    getStack().getCacheLog().Info("pbcast.GMS.acquireHashmap()", (isStartedAsMirror ? "Mirror" : "") + "null map received... requesting the hashmap again");
                }

            } while (maxTries > 0);
        }

        if (maxTries < 0 && _hashmap == null) {
            getStack().getCacheLog().Error("GMS.AcquireHashmap", "Hashmap acquisition failure for :" + Global.CollectionToString(mbrs) + " joining? " + isJoining);
        }
        if (getStack().getCacheLog().getIsInfoEnabled()) {
            getStack().getCacheLog().Info("pbcast.GMS.acquireHashmap()", (isStartedAsMirror ? "Mirror" : "") + "request for hashmap end");
        }
    }

    public final boolean allowJoin(Address mbr, boolean isStartedAsMirror,String partitionId) throws InterruptedException {

        if (!isPartReplica) {
            return true;
        }
        //muds:
        //new code for disabling the join while in state transfer.
        synchronized (acquirePermission_mutex) {
            _allowJoin = false;

            java.util.List existingMembers = members.getMembers();
            Address lastJoiney = null;
            if (existingMembers.size() > 0) {
                lastJoiney = (Address) ((existingMembers.get(existingMembers.size() - 1) instanceof Address) ? existingMembers.get(existingMembers.size() - 1) : null);
                String lastJoineePartitionId = getPartition(lastJoiney);
                if (isStartedAsMirror) {
                    if(lastJoineePartitionId != null ){
                        if(!lastJoineePartitionId.equals(partitionId)){
                         return false;
                        }
                        else{
                            //replica on last active node is allowed to join
                            _allowJoin = true;
                            return _allowJoin;
                        }
                    }
                } else {
                    if (existingMembers.size() > 1) {
                        Address secondLastJoinee = (Address) ((existingMembers.get(existingMembers.size() - 2) instanceof Address) ? existingMembers.get(existingMembers.size() - 2) : null);
                        String secondLastJoineePartitionId = getPartition(secondLastJoinee);
                        if(lastJoineePartitionId != null && !lastJoineePartitionId.equals(secondLastJoineePartitionId)){
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }

            if (isStartedAsMirror) {
                //if members contain my active then i need to join
                if (members.ContainsIP(mbr)) {
                    _allowJoin = true;
                }

            } else {
                getStack().getCacheLog().CriticalInfo("pbcast.GMS.allowJoin()", "Join permission for " + mbr.toString());
                boolean inStateTransfer = IsClusterInStateTransfer();
                _allowJoin = !inStateTransfer;
            }
            return _allowJoin;
        }

    }

    public void HandleSpecialJoinRequest(Address mbr, String gmsId) {
        getStack().getCacheLog().CriticalInfo("pbcast.GMS.HandleSpecialJoinRequest()", "mbr=" + mbr);

        if (haveCoordinatorRole()) {
            return;
        }

        Address new_coord = null;
        java.util.List mbrs = members.getMembers(); //returns a *copy* of the membership vector

        Address sameNode = null;
        if (isPartReplica) {
            Membership mbrShip = members.copy();

            if (mbrShip.contains(mbr)) {
                for (int i = 0; i < mbrShip.size(); i++) {
                    Address other = mbrShip.elementAt(i);
                    if (other != null && !other.equals(mbr)) {
                        if (other.getIpAddress().equals(mbr.getIpAddress())) {
                            sameNode = other;
                            break;
                        }
                    }
                }

            }
        }

        mbrs.remove(mbr);
        mbrs.remove(sameNode);

        for (int i = 0; i < mbrs.size() - 1; i++) {
            new_coord = (Address) mbrs.get(i);
            if (local_addr.equals(new_coord)) {

                becomeCoordinator();
                impl.handleSuspect(mbr);
                return;
            } else if (!VerifySuspect(new_coord)) {
                return;
            }

        }

        getStack().getCacheLog().CriticalInfo("HandleSpecialJoinRequest", "Anomoly at choosing next coordinator");
        return;

    }

    public final void handleLeaveAsync(Object args) {
        Object[] arr = (Object[]) args;
        impl.handleLeave((Address) ((arr[0] instanceof Address) ? arr[0] : null), (Boolean) arr[1]);
    }

    public final void handleJoinrequestAsync(Object args) throws InterruptedException {
        Object[] arr = (Object[]) args;
        handleJoinRequest(
                (Address) ((arr[0] instanceof Address) ? arr[0] : null),
                (String) ((arr[1] instanceof String) ? arr[1] : null), 
                (Boolean) arr[2], 
                (String) ((arr[3] instanceof String) ? arr[3] : null), 
                (String) ((arr[4] instanceof String) ? arr[4] : null)
        );
    }

    public final void handleSuspectAsync(Object mbr) throws InterruptedException {
        impl.handleSuspect((Address) ((mbr instanceof Address) ? mbr : null));
    }

    public final void handleLeaveClusterRequestAsync(Object mbr) {
        impl.handleLeaveClusterRequest((Address) ((mbr instanceof Address) ? mbr : null));
    }

    public void handleJoinRequest(Address mbr, String subGroup_name, boolean isStartedAsMirror, String gmsId, String partitionId) throws InterruptedException {

        JoinRsp join_rsp = null;
        Message m;
        HDR hdr;

        if (mbr == null) {
            getStack().getCacheLog().Error("mbr is null");
            return;
        }

        getStack().getCacheLog().Debug("pbcast.GMS.handleJoinRequest()", "mbr=" + mbr);

        if (!isStartedAsMirror) {
            synchronized (join_mutex) {

                if (_nodeJoiningInProgress || _isLeavingInProgress || (_isStarting && !local_addr.getIpAddress().equals(mbr.getIpAddress()))) {
                    getStack().getCacheLog().CriticalInfo("GMS.HandleJoinRequest()", "node join already in progess" + mbr);

                    join_rsp = new JoinRsp(null, null);
                    join_rsp.setJoinResult(JoinResult.MembershipChangeAlreadyInProgress);

                    m = new Message(mbr, null, null);
                    hdr = new HDR(HDR.JOIN_RSP, join_rsp);
                    m.putHeader(HeaderType.GMS, hdr);
                    passDown(new Event(Event.MSG, m, Priority.Critical));
                    return;
                } else {

                    _nodeJoiningInProgress = true;
                }
            }
        }

        // 1. Get the new view and digest
        if (members.contains(mbr)) {

            String oldGmsId = GetGmsId(mbr);

            if (oldGmsId != null && !gmsId.equals(oldGmsId)) {
                getStack().getCacheLog().Error("pbcast.GMS.handleJoinRequest()", mbr + " has sent a joining request while it is already in member list and has wrong gmsID");

                join_rsp = null;
                m = new Message(mbr, null, null);
                hdr = new HDR(HDR.JOIN_RSP, join_rsp);
                m.putHeader(HeaderType.GMS, hdr);
                passDown(new Event(Event.MSG, m, Priority.Critical));

                impl.handleSuspect(mbr);

                synchronized (join_mutex) {
                    _nodeJoiningInProgress = false;
                }
                return;
            } else {

                getStack().getCacheLog().Error("pbcast.GMS.handleJoinRequest()", mbr
                        + " has sent a joining request while it is already in member list - Resending current view and digest");

                View view = new View(this.view_id, members.getMembers());
                view.setCoordinatorGmsId(unique_id);
                join_rsp = new JoinRsp(view, this.digest, JoinResult.Success);
                m = new Message(mbr, null, null);
                hdr = new HDR(HDR.JOIN_RSP, join_rsp);
                m.putHeader(HeaderType.GMS, hdr);
                passDown(new Event(Event.MSG, m, Priority.Critical));
                synchronized (join_mutex) {
                    _nodeJoiningInProgress = false;
                }
                return;
            }
        }

        if (allowJoin(mbr, isStartedAsMirror,partitionId)) {
            getStack().getCacheLog().Debug("pbcast.GMS.handleJoinRequest()", " joining allowed");
            boolean acauireHashmap = true;

            tangible.RefObject<Boolean> tempRef_acauireHashmap = new tangible.RefObject<Boolean>(acauireHashmap);
            join_rsp = impl.handleJoin(mbr, subGroup_name, isStartedAsMirror, gmsId, tempRef_acauireHashmap);
            acauireHashmap = tempRef_acauireHashmap.argvalue;

            if (join_rsp == null) {
                getStack().getCacheLog().Error("pbcast.GMS.handleJoinRequest()", impl.getClass().toString() + ".handleJoin(" + mbr
                        + ") returned null: will not be able to multicast new view");
            }

            //muds:
            //sends a request to the caching layer for the new hashmap after this member joins.
            java.util.ArrayList mbrs = new java.util.ArrayList(1);
            mbrs.add(mbr);

            //muds:
            //some time coordinator gms impl returns the same existing view in join response.
            //we dont need to acquire the hashmap again in this case coz that hashmap has already been acquired.
            if (acauireHashmap) {
                acquireHashmap(mbrs, true, subGroup_name, isStartedAsMirror, partitionId);
            }

            // 2. Send down a local TMP_VIEW event. This is needed by certain layers (e.g. NAKACK) to compute correct digest
            //    in case client's next request (e.g. getState()) reaches us *before* our own view change multicast.
            // Check NAKACK's TMP_VIEW handling for details
            if (join_rsp != null && join_rsp.getView() != null) {
                join_rsp.getView().addPartition(mbr, partitionId);
                //muds:
                //add the hash map as part of view.
                if (_hashmap != null) {
                    Object[] mapsArray = (Object[]) _hashmap;
                    DistributionMaps maps = (DistributionMaps) mapsArray[0];
                    if (maps != null) {

                        join_rsp.getView().setDistributionMaps(maps);
                    }

                    join_rsp.getView().setMirrorMapping((CacheNode[]) ((mapsArray[1] instanceof CacheNode[]) ? mapsArray[1] : null));

                }
                passDown(new Event(Event.TMP_VIEW, join_rsp.getView()));
            }
        } else {
            getStack().getCacheLog().Debug("pbcast.GMS.handleJoinRequest()", " joining not allowed");
        }

        // 3. Return result to client
        m = new Message(mbr, null, null);

        hdr = new HDR(HDR.JOIN_RSP, join_rsp);
        m.putHeader(HeaderType.GMS, hdr);
        passDown(new Event(Event.MSG, m, Priority.Critical));

        // 4. Bcast the new view
        if (join_rsp != null) {
            castViewChange(join_rsp.getView());
        }

        synchronized (join_mutex) {
            _nodeJoiningInProgress = false;
        }

    }

    public final void handleResetOnNodeRejoining(Address sender, Address node, View view) {
        ViewId vid = view.getVid();
        int rc;
        if (getStack().getCacheLog().getIsInfoEnabled()) {
            getStack().getCacheLog().Info("GMS.handleResetOnNodeRejoining", "Sequence reset request");
        }
        if (view_id != null) {
            rc = vid.compareTo(view_id);
            if (rc <= 0) {
                return;
            }
            ltime = Math.max(vid.getId(), ltime); // compute Lamport logical time
            synchronized (members) {
                view_id = vid.Copy();
            }
        }
        Event evt = new Event(Event.RESET_SEQUENCE, vid);
        passUp(evt);
    }

    public static class HDR extends Header implements ICompactSerializable {

        public static final byte JOIN_REQ = 1;

        public static final byte JOIN_RSP = 2;

        public static final byte LEAVE_REQ = 3;

        public static final byte LEAVE_RSP = 4;

        public static final byte VIEW = 5;

        public static final byte MERGE_REQ = 6;

        public static final byte MERGE_RSP = 7;

        public static final byte INSTALL_MERGE_VIEW = 8;

        public static final byte CANCEL_MERGE = 9;

        public static final byte CAN_NOT_CONNECT_TO = 10;

        public static final byte LEAVE_CLUSTER = 11;

        public static final byte CONNECTION_BROKEN = 12;

        public static final byte CONNECTED_NODES_REQUEST = 13;

        public static final byte CONNECTED_NODES_RESPONSE = 14;

        public static final byte VIEW_REJECTED = 15;

        public static final byte INFORM_NODE_REJOINING = 16;

        public static final byte RESET_ON_NODE_REJOINING = 17;

        public static final byte RE_CHECK_CLUSTER_HEALTH = 18;

        public static final byte VIEW_RESPONSE = 19;

        public static final byte SPECIAL_JOIN_REQUEST = 20;

        public static final byte INFORM_ABOUT_NODE_DEATH = 21;

        public static final byte IS_NODE_IN_STATE_TRANSFER = 22;

        public static final byte IS_NODE_IN_STATE_TRANSFER_RSP = 23;

        public byte type = 0;
        public View view = null; // used when type=VIEW or MERGE_RSP or INSTALL_MERGE_VIEW
        public Address mbr = null; // used when type=JOIN_REQ or LEAVE_REQ
        public JoinRsp join_rsp = null; // used when type=JOIN_RSP
        public Digest digest = null; // used when type=MERGE_RSP or INSTALL_MERGE_VIEW
        public Object merge_id = null; // used when type=MERGE_REQ or MERGE_RSP or INSTALL_MERGE_VIEW or CANCEL_MERGE
        public boolean merge_rejected = false; // used when type=MERGE_RSP
        public String subGroup_name = null; // to identify the subgroup of the current member.
        public boolean isStartedAsMirror = false; // to identify the current memebr as active or mirror.
        public java.util.List nodeList; //nodes to which this can not establish the connection.
        public Object arg;
        public String gms_id;
        public String partitionId;

        public HDR() {
        } // used for Externalization

        public HDR(byte type) {
            this.type = type;
        }

        public HDR(byte type, Object argument) {
            this.type = type;
            this.arg = argument;
        }

        /**
         * Used for VIEW header
         */
        public HDR(byte type, View view) {
            this.type = type;
            this.view = view;
        }

        /**
         * Used for JOIN_REQ or LEAVE_REQ header
         */
        public HDR(byte type, Address mbr) {
            this.type = type;
            this.mbr = mbr;
        }

        /**
         * Used for JOIN_REQ or LEAVE_REQ header
         */
        public HDR(byte type, Address mbr, String subGroup_name) {
            this.type = type;
            this.mbr = mbr;
            this.subGroup_name = subGroup_name;
        }

        /**
         * Used for JOIN_REQ header
         * @param type
         * @param mbr
         * @param subGroup_name
         * @param isStartedAsMirror
         */
        public HDR(byte type, Address mbr, String subGroup_name, boolean isStartedAsMirror) {
            this.type = type;
            this.mbr = mbr;
            this.subGroup_name = subGroup_name;
            this.isStartedAsMirror = isStartedAsMirror;
            
        }

          /**
         * Used for JOIN_REQ header
         * @param type
         * @param mbr
         * @param subGroup_name
         * @param isStartedAsMirror
         * @param partitionId
         */
        public HDR(byte type, Address mbr, String subGroup_name, boolean isStartedAsMirror, String partitionId) {
            this.type = type;
            this.mbr = mbr;
            this.subGroup_name = subGroup_name;
            this.isStartedAsMirror = isStartedAsMirror;
            this.partitionId = partitionId;
        }
        
        
        public String getPartitionId() {
            return partitionId;
        }

        public void setPartitionId(String partitionId) {
            this.partitionId = partitionId;
        }

        /**
         * Used for JOIN_RSP header
         */
        public HDR(byte type, JoinRsp join_rsp) {
            this.type = type;
            this.join_rsp = join_rsp;
        }

        public final String getGMSId() {
            return gms_id;
        }
        
     

        public final void setGMSId(String value) {
            gms_id = value;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("HDR");
            sb.append('[' + type2String(type) + ']');
            switch (type) {

                case JOIN_REQ:
                    sb.append(": mbr=" + mbr);
                    break;

                case SPECIAL_JOIN_REQUEST:
                    sb.append(": mbr=" + mbr);
                    break;

                case RE_CHECK_CLUSTER_HEALTH:
                    sb.append(": mbr=" + mbr);
                    break;

                case JOIN_RSP:
                    sb.append(": join_rsp=" + join_rsp);
                    break;

                case LEAVE_REQ:
                    sb.append(": mbr=" + mbr);
                    break;

                case LEAVE_RSP:
                    break;

                case VIEW:
                    sb.append(": view=" + view);
                    break;

                case MERGE_REQ:
                    sb.append(": merge_id=" + merge_id);
                    break;

                case MERGE_RSP:
                    sb.append(": view=" + view + ", digest=" + digest + ", merge_rejected=" + merge_rejected + ", merge_id=" + merge_id);
                    break;

                case INSTALL_MERGE_VIEW:
                    sb.append(": view=" + view + ", digest=" + digest);
                    break;

                case CANCEL_MERGE:
                    sb.append(", <merge cancelled>, merge_id=" + merge_id);
                    break;

                case CONNECTION_BROKEN:
                    sb.append("<suspected member : " + mbr + " >");
                    break;

                case VIEW_REJECTED:
                    sb.append("<rejected by : " + mbr + " >");

                    break;

                case INFORM_NODE_REJOINING:
                    sb.append("INFORM_NODE_REJOINING");
                    break;

                case RESET_ON_NODE_REJOINING:
                    sb.append("RESET_ON_NODE_REJOINING");
                    break;

                case VIEW_RESPONSE:
                    sb.append("VIEW_RESPONSE");
                    break;

                case IS_NODE_IN_STATE_TRANSFER:
                    sb.append("IS_NODE_IN_STATE_TRANSFER");
                    break;

                case IS_NODE_IN_STATE_TRANSFER_RSP:
                    sb.append("IS_NODE_IN_STATE_TRANSFER_RSP->" + arg);
                    break;

                case INFORM_ABOUT_NODE_DEATH:
                    sb.append("INFORM_ABOUT_NODE_DEATH (" + arg + ")");
                    break;
            }

            return sb.toString();
        }

        public static String type2String(int type) {
            switch (type) {

                case JOIN_REQ:
                    return "JOIN_REQ";

                case SPECIAL_JOIN_REQUEST:
                    return "SPECIAL_JOIN_REQUEST";

                case JOIN_RSP:
                    return "JOIN_RSP";

                case LEAVE_REQ:
                    return "LEAVE_REQ";

                case LEAVE_RSP:
                    return "LEAVE_RSP";

                case VIEW:
                    return "VIEW";

                case MERGE_REQ:
                    return "MERGE_REQ";

                case MERGE_RSP:
                    return "MERGE_RSP";

                case INSTALL_MERGE_VIEW:
                    return "INSTALL_MERGE_VIEW";

                case CANCEL_MERGE:
                    return "CANCEL_MERGE";

                case CAN_NOT_CONNECT_TO:
                    return "CAN_NOT_CONNECT_TO";

                case LEAVE_CLUSTER:
                    return "LEAVE_CLUSTER";

                case CONNECTION_BROKEN:
                    return "CONNECTION_BROKEN";

                case CONNECTED_NODES_REQUEST:
                    return "CONNECTED_NODES_REQUEST";

                case CONNECTED_NODES_RESPONSE:
                    return "CONNECTED_NODES_RESPONSE";

                case VIEW_REJECTED:
                    return "VIEW_REJECTED";

                case RE_CHECK_CLUSTER_HEALTH:
                    return "RE_CHECK_CLUSTER_HEALTH";

                case INFORM_ABOUT_NODE_DEATH:
                    return "RE_CHECK_CLUSTER_HEALTH";

                case IS_NODE_IN_STATE_TRANSFER:
                    return "IS_NODE_IN_STATE_TRANSFER";

                case IS_NODE_IN_STATE_TRANSFER_RSP:
                    return "IS_NODE_IN_STATE_TRANSFER_RSP";

                default:
                    return "<unknown>";

            }
        }

        public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
            type = reader.readByte();
            view = View.ReadView(reader);

            if (reader.readBoolean()) {
                mbr = (Address) reader.readObject();
            } else {
                mbr = null;
            }
            join_rsp = (JoinRsp) reader.readObject();
            digest = (Digest) reader.readObject();
            merge_id = reader.readObject();
            merge_rejected = reader.readBoolean();
            subGroup_name = reader.readUTF();
            Object tempVar = reader.readObject();
            nodeList = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
            arg = reader.readObject();
            isStartedAsMirror = reader.readBoolean();
            Object tempVar2 = reader.readObject();
            gms_id = (String) ((tempVar2 instanceof String) ? tempVar2 : null);
            partitionId= (String) Common.as(reader.readObject(), String.class);
        }

        public void serialize(CacheObjectOutput writer) throws IOException {
            writer.writeByte(type);
            View.WriteView(writer, view);

            if (mbr != null) {
                writer.writeBoolean(true);
                writer.writeObject(mbr);
            } else {
                writer.writeBoolean(false);
            }
            writer.writeObject(join_rsp);
            writer.writeObject(digest);
            writer.writeObject(merge_id);
            writer.writeBoolean(merge_rejected);
            writer.writeUTF(subGroup_name);
            writer.writeObject(nodeList);
            writer.writeObject(arg);
            writer.writeBoolean(isStartedAsMirror);
            writer.writeObject(gms_id);
            writer.writeObject(partitionId);
        }

    }
}
