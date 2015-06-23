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
package com.alachisoft.tayzgrid.common.datastructures;

import com.alachisoft.tayzgrid.common.net.Address;

public class PartNodeInfo {

    private Address _address;
    private String _subGroupId;
    private boolean _isCoordinator;
    private int _priorityIndex;
    private String partitionId;

    public PartNodeInfo() {
        _address = new Address();
        _subGroupId = "";
        _isCoordinator = false;
        _priorityIndex = -1;
    }

    public PartNodeInfo(Address address, String subGroup, boolean isCoordinator) {
        _address = address;
        _subGroupId = subGroup;
        _isCoordinator = isCoordinator;
    }

    public final Address getNodeAddress() {
        return _address;
    }

    public final void setNodeAddress(Address value) {
        _address = value;
    }

    public final String getSubGroup() {
        return _subGroupId;
    }

    public final void setSubGroup(String value) {
        _subGroupId = value;
    }

    public final boolean getIsCoordinator() {
        return _isCoordinator;
    }

    public final void setIsCoordinator(boolean value) {
        _isCoordinator = value;
    }

    public final int getPriorityIndex() {
        return _priorityIndex;
    }

    public final void setPriorityIndex(int value) {
        _priorityIndex = value;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PartNodeInfo) {
            PartNodeInfo other = (PartNodeInfo) obj;
            if ((this.getNodeAddress().equals(other.getNodeAddress())) && this._subGroupId.equals(other._subGroupId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "PartNodeInfo(" + getNodeAddress().toString() + ", " + getSubGroup() + "," + (new Boolean(getIsCoordinator())).toString() + ")";
    }
}
