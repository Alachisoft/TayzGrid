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
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.enums.Priority;

// $Id: GmsImpl.java,v 1.4 2004/09/03 12:28:04 belaban Exp $
public abstract class GmsImpl
{

    protected String _uniqueId;
    public GMS gms = null;
    public boolean leaving = false;

    public abstract void join(Address mbr, boolean isStartedAsMirror);

    public abstract void leave(Address mbr);

    public abstract void handleJoinResponse(JoinRsp join_rsp);

    public abstract void handleLeaveResponse();

    public abstract void suspect(Address mbr)throws InterruptedException;

    public abstract void unsuspect(Address mbr);

    public void merge(java.util.ArrayList other_coords)
    {
    } // only processed by coord

    public void handleMergeRequest(Address sender, Object merge_id)
    {
    } // only processed by coords

    public void handleMergeResponse(MergeData data, Object merge_id)
    {
    } // only processed by coords

    public void handleMergeView(MergeData data, Object merge_id)
    {
    } // only processed by coords

    public void handleMergeCancelled(Object merge_id)
    {
    } // only processed by coords

    public void handleNotifyLeaving()
    {
        
    }
    public abstract JoinRsp handleJoin(Address mbr, String subGroup_name, boolean isStartedAsMirror, String gmsId, tangible.RefObject<Boolean> acquireHashmap);

    public abstract void handleLeave(Address mbr, boolean suspected);

    public abstract void handleViewChange(View new_view, Digest digest);

    public abstract void handleSuspect(Address mbr);

    public void handleInformAboutNodeDeath(Address sender, Address deadNode)
    {
    }

    public boolean getisInStateTransfer()
    {
        return gms.GetStateTransferStatus();
    }

    public void handleIsClusterInStateTransfer(Address sender)
    {
        Message msg = new Message(sender, null, new byte[0]);
        GMS.HDR hdr = new GMS.HDR(GMS.HDR.IS_NODE_IN_STATE_TRANSFER_RSP);
        gms.getStack().getCacheLog().Debug("gmsImpl.handleIsClusterInStateTransfer", "(state transfer request) sender: " + sender + " ->" + getisInStateTransfer());
        hdr.arg = getisInStateTransfer();
        msg.putHeader(HeaderType.GMS, hdr);
        gms.passDown(new Event(Event.MSG, msg, Priority.Critical));
    }

    /**
     * A unique identifier that is shared by all the nodes participating in cluster. Upper cache layer uses this unique identifier when connected to the bridge as a source cache.
     */
    public String getUniqueId()
    {
        return _uniqueId;
    }

    public void setUniqueId(String value)
    {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(_uniqueId))
        {
            _uniqueId = value;
        }
    }

    public boolean handleUpEvent(Event evt)
    {
        return true;
    }

    public boolean handleDownEvent(Event evt)
    {
        return true;
    }

    public void init()
    {
        leaving = false;
    }

    public void start()
    {
        leaving = false;
    }

    public void stop()
    {
        leaving = true;
    }

    public void handleConnectionFailure(java.util.ArrayList nodes)
    {
    }

    public void handleNodeRejoining(Address node)
    {
    }

    public void handleInformNodeRejoining(Address sender, Address node)
    {
    }

    public void handleResetOnNodeRejoining(Address sender, Address node, View view)
    {
    }

    public void handleCanNotConnectTo(Address src, java.util.List failedNode)
    {
    }

    public void handleLeaveClusterRequest(Address sender)
    {
    }

    public void handleConnectedNodesRequest(Address sender, int reqid)
    {
    }

    public void handleConnectedNodesResponse(Address sender, int reqid)
    {
    }

    public void handleConnectionBroken(Address informer, Address suspected)
    {
    }

    public void handleViewRejected(Address mbrRejected)
    {
    }

    protected void wrongMethod(String method_name)
    {
        if (gms.getStack().getCacheLog().getIsInfoEnabled())
        {
            gms.getStack().getCacheLog().Info(method_name + "() should not be invoked on an instance of " + getClass().getName());
        }
    }

    /**
     * Returns potential coordinator based on lexicographic ordering of member addresses. Another approach would be to keep track of the primary partition and return the first
     * member if we are the primary partition.
     */
    protected boolean iWouldBeCoordinator(java.util.ArrayList new_mbrs)
    {
        Membership tmp_mbrs = gms.members.copy();
        tmp_mbrs.merge(new_mbrs, null);
        tmp_mbrs.sort();
        if (tmp_mbrs.size() <= 0 || gms.local_addr == null)
        {
            return false;
        }
        return gms.local_addr.equals(tmp_mbrs.elementAt(0));
    }

    public void ReCheckClusterHealth(Object mbr)
    {
        wrongMethod("ReCheckClusterHealth()");
    }
}
