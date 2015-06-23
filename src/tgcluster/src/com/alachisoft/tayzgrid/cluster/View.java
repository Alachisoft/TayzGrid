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
package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.mirroring.CacheNode;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class View implements Cloneable, ICompactSerializable {

    /**
     * The view id contains the creator address and a lamport time. the lamport
     * time is the highest timestamp seen or sent from a view. if a view change
     * comes in with a lower lamport time, the event is discarded.
     *
     *
     * A view is uniquely identified by its ViewID
     */
    private ViewId vid;
    /**
     * This list is always ordered, with the coordinator being the first member.
     * the second member will be the new coordinator if the current one
     * disappears or leaves the group.
     *
     *
     * A list containing all the _members of the view
     */
    private java.util.List _members;
    /**
     * contains the mbrs list against subgroups
     */
    private java.util.HashMap _sequencerTbl;
    /**
     * contains the subgroup against the mbr addresses.
     */
    private java.util.HashMap _mbrsSubgroupMap;
    private boolean _forceInstall;
    /**
     * the HashMap that is used for load distribution in partitioned caches.
     */
    //private ArrayList _hashMap;
    /**
     * Hashmap buckets assigned to each node.
     */
    //private HashMap _bucketsOwnershipMap;
    private DistributionMaps _distributionMaps;
    private String _coordinatorGmsId;

    /**
     * Map table or some serialized link list for dynamic mirroring.
     */
    private CacheNode[] _mirrorMapping;

    /**
     * This is the unique id used by each bridge source cache to communicate
     * with bridge. Because each node from one source cache communicate with
     * bridge with this same unique id, bridge understands that multiple nodes
     * from one cache are connected to it.
     */
    private String _bridgeSourceCacheId;
    private HashMap nodeGmsIds = new HashMap();
    private HashMap partitionIds = new HashMap();

    /**
     * creates an empty view, should not be used
     */
    public View() {
    }

    /**
     * Constructor
     *
     * @param vid The view id of this view (can not be null)
     * @param _members Contains a list of all the _members in the view, can be
     * empty but not null.
     */
    public View(ViewId vid, java.util.List _members) {
        this.vid = vid;
        this._members = _members;
    }

    /**
     * Constructor
     *
     * @param vid The view id of this view (can not be null)
     * @param _members Contains a list of all the _members in the view, can be
     * empty but not null.
     * @param sequencerTbl
     */
    public View(ViewId vid, java.util.List _members, java.util.HashMap sequencerTbl) {
        this.vid = vid;
        this._members = _members;
        this._sequencerTbl = sequencerTbl;
    }

    /**
     * Constructor
     *
     * @param creator The creator of this view
     * @param id The lamport timestamp of this view
     * @param _members Contains a list of all the _members in the view, can be
     * empty but not null.
     */
    public View(Address creator, long id, java.util.List _members) {
        this(new ViewId(creator, id), _members);
    }

    /**
     * returns the view ID of this view if this view was created with the empty
     * constructor, null will be returned
     *
     * @return the view ID of this view
     *
     */
    public final ViewId getVid() {
        return vid;
    }

    public final boolean getForceInstall() {
        return _forceInstall;
    }

    public final void setForceInstall(boolean value) {
        _forceInstall = value;
    }

    public final String getCoordinatorGmsId() {
        return _coordinatorGmsId;
    }

    public final void setCoordinatorGmsId(String value) {
        _coordinatorGmsId = value;
    }

    public final DistributionMaps getDistributionMaps() {
        return _distributionMaps;
    }

    public final void setDistributionMaps(DistributionMaps value) {
        _distributionMaps = value;
    }

    public final Address getCoordinator() {
        return _members != null && _members.size() > 0 ? (Address) ((_members.get(0) instanceof Address) ? _members.get(0) : null) : null;
    }

    public final java.util.HashMap getSequencerTbl() {
        return this._sequencerTbl;
    }

    public final void setSequencerTbl(java.util.HashMap value) {
        this._sequencerTbl = value;
    }

    public final java.util.HashMap getMbrsSubgroupMap() {
        return this._mbrsSubgroupMap;
    }

    public final void setMbrsSubgroupMap(java.util.HashMap value) {
        this._mbrsSubgroupMap = value;
    }

    public final String getBridgeSourceCacheId() {
        return _bridgeSourceCacheId;
    }

    public final void setBridgeSourceCacheId(String value) {
        _bridgeSourceCacheId = value;
    }

    /**
     * returns the creator of this view if this view was created with the empty
     * constructor, null will be returned
     *
     * @return the creator of this view in form of an Address object
     *
     */
    public final Address getCreator() {
        return vid != null ? vid.getCoordAddress() : null;
    }

    public final void AddGmsId(Address node, String id) {
        if (node != null) {
            nodeGmsIds.put(node, id);
        }
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
            partitionIds.put(node, id);
        }
    }

    public void removePartition(Address node) {
        if (node != null) {
            partitionIds.remove(node);
        }
    }

    public void removePartition(ArrayList nodes) {
        for (Object obj : nodes) {
            Address node = (Address)Common.as(obj, Address.class);
            if (node != null) {
                partitionIds.remove(node);
            }
        }
    }

    public HashMap getPartitions() {
        return partitionIds;
    }

    public String getPartition(Address node) {
        return (String) ((partitionIds.get(node) instanceof String) ? partitionIds.get(node) : null);
    }

    /**
     * Returns a reference to the List of _members (ordered) Do NOT change this
     * list, hence your will invalidate the view Make a copy if you have to
     * modify it.
     *
     * @return a reference to the ordered list of _members in this view
     *
     */
    public final java.util.List getMembers() {
        return _members;
    }

    /**
     * HashMap or some serialized object used for dynamic mirroring in case of
     * Partitioned Replica topology. This along with Distribution Map is sent
     * back to the joining node or to all the nodes in the cluster in case of
     * leaving.
     * @return 
     */
    public final CacheNode[] getMirrorMapping() {
        return _mirrorMapping;
    }

    public final void setMirrorMapping(CacheNode[] value) {
        _mirrorMapping = value;
    }

    /**
     * Returns true, if this view contains a certain member
     *
     * @param mbr The address of the member
     * @return True, if this view contains a certain member
     */
    public final boolean containsMember(Address mbr) {
        if (mbr == null || _members == null) {
            return false;
        }
        return _members.contains(mbr);
    }

    /**
     * Returns the number of _members in this view
     *
     * @return The number of _members in this view
     */
    public final int size() {
        if (_members == null) {
            return 0;
        } else {
            return _members.size();
        }
    }

    /**
     * creates a copy of this view
     *
     * @return a copy of this view
     * @throws java.lang.CloneNotSupportedException
     *
     */
    @Override
    public Object clone()  {
        ViewId vid2 = vid != null ? (ViewId) vid.clone() : null;
        java.util.List members2 = _members != null ? (java.util.List) GenericCopier.DeepCopy(_members)/*.clone() */ : null;
        View v = new View(vid2, members2);
        if (getSequencerTbl() != null) {
            Object tempVar = getSequencerTbl().clone();
            {
                v.setSequencerTbl((java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null));
            }
        }
        if (getMbrsSubgroupMap() != null) {
            Object tempVar2 = getMbrsSubgroupMap().clone();
            {
                v.setMbrsSubgroupMap((java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null));
            }
        }
        v._coordinatorGmsId = _coordinatorGmsId;
        if (getDistributionMaps() != null) {
            Object tempVar3 = getDistributionMaps().clone();
            {
                v.setDistributionMaps((DistributionMaps) ((tempVar3 instanceof DistributionMaps) ? tempVar3 : null));
            }
        }
        if (getMirrorMapping() != null) {
            v.setMirrorMapping(getMirrorMapping());
        }
        v._bridgeSourceCacheId = _bridgeSourceCacheId;
        if (nodeGmsIds != null) {
            Object tempVar4 = nodeGmsIds.clone();
            {
                v.nodeGmsIds = (java.util.HashMap) ((tempVar4 instanceof java.util.HashMap) ? tempVar4 : null);
            }
        }
        if (partitionIds != null) {
            Object tempVar5 = partitionIds.clone();
            {
                v.partitionIds = (java.util.HashMap) ((tempVar5 instanceof java.util.HashMap) ? tempVar5 : null);
            }
        }

        return (v);
    }

    /**
     * copy the View
     *
     * @return A copy of the View
     */
    public final View Copy() {
        return (View) Copy();
    }

    /**
     * Returns a string representation of the View
     *
     * @return A string representation of the View
     */
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        StringBuilder append = ret.append(vid).append(" [gms_id:").append(_coordinatorGmsId).append("] ").append(Global.CollectionToString(_members));
        return append.toString();
    }

    //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        vid = (ViewId) reader.readObject();
        _members = (java.util.List) reader.readObject();
        _sequencerTbl = (java.util.HashMap) reader.readObject();
        _mbrsSubgroupMap = (java.util.HashMap) reader.readObject();
        _distributionMaps = (DistributionMaps) reader.readObject();
        Object tempVar = reader.readObject();
        Object[] objs = (Object[]) ((tempVar instanceof Object[]) ? tempVar : null);
        if (objs != null) {
            for (int i = 0; i < objs.length; i++) {
                if (objs[i] instanceof CacheNode) {
                    if (_mirrorMapping == null) {
                        _mirrorMapping = new CacheNode[objs.length];
                    }
                    _mirrorMapping[i] = (CacheNode) objs[i];
                }
            }
        }
        Object tempVar2 = reader.readObject();
        _bridgeSourceCacheId = (String) ((tempVar2 instanceof String) ? tempVar2 : null);
        Object tempVar3 = reader.readObject();
        nodeGmsIds = (java.util.HashMap) ((tempVar3 instanceof java.util.HashMap) ? tempVar3 : null);
        Object tempVar4 = reader.readObject();
        _coordinatorGmsId = (String) ((tempVar4 instanceof String) ? tempVar4 : null);
        Object tempVar5 = reader.readObject();
        partitionIds = (java.util.HashMap) ((tempVar5 instanceof java.util.HashMap) ? tempVar5 : null);
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(vid);
        writer.writeObject(_members);
        writer.writeObject(_sequencerTbl);
        writer.writeObject(_mbrsSubgroupMap);
        writer.writeObject(_distributionMaps);
        writer.writeObject(_mirrorMapping);
        writer.writeObject(_bridgeSourceCacheId);
        writer.writeObject(nodeGmsIds);
        writer.writeObject(_coordinatorGmsId);
        writer.writeObject(partitionIds);
    }
    //</editor-fold>

    public static View ReadView(CacheObjectInput reader) throws IOException, ClassNotFoundException {
        byte isNull = reader.readByte();
        if (isNull == 1) {
            return null;
        }
        View newView = new View();
        newView.deserialize(reader);
        return newView;
    }

    public static void WriteView(CacheObjectOutput writer, View v) throws IOException {
        byte isNull = 1;
        if (v == null) {
            writer.writeByte(isNull);
        } else {
            isNull = 0;
            writer.writeByte(isNull);
            v.serialize(writer);
        }
    }
}
