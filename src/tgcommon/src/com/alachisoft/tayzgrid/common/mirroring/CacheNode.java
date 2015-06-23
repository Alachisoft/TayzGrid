/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
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

package com.alachisoft.tayzgrid.common.mirroring;

import java.io.Serializable;


/**
 * It serves as a node in the logical linked list of cache nodes in which each node's mirror is maintained on the next node in the list.
 *
 * Note: Mirror of a node is knows as backup (next node in the list).
 */

/*
 *In java we dont have Iserializable interface that provides Serialization Info and StreamingContext. We only have Externalizable Inteface to control reading and writing
 * of Objects
 *
 */
public class CacheNode implements Cloneable, Serializable //: Implemented Serializable instead of commenting the whole class, let Java handle serialization
{
    private static int sequenceSeed = 1;
    private String nodeId;
    private String backupNodeId;
    private String prevNodeId;
    private int mySequence;

    private CacheNode()
    {
    }

    public CacheNode(String nodeId, int sequence)
    {
        this.nodeId = nodeId;
        this.mySequence = sequence;
    }

    public final int getSequence()
    {
        return mySequence;
    }

    public final String getNodeId()
    {
        return nodeId;
    }

    public final String getBackupNodeId()
    {
        return backupNodeId;
    }

    public final void setBackupNodeId(String value)
    {
        backupNodeId = value;
    }

    public final String getPreviousNodeId()
    {
        return prevNodeId;
    }

    public final void setPreviousNodeId(String value)
    {
        prevNodeId = value;
    }

    @Override
    public String toString()
    {
        return String.format("Node: %1$s, Backup: %2$s", nodeId, backupNodeId);
    }

    public final Object clone()
    {
        CacheNode clone = new CacheNode();
        clone.nodeId = getNodeId();
        clone.setPreviousNodeId(getPreviousNodeId());
        clone.setBackupNodeId(getBackupNodeId());
        clone.mySequence = getSequence();
        return clone;
    }

}
