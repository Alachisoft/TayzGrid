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

public class DistributionInfoData {
	private DistributionMode _distMode = DistributionMode.values()[0];
	private ClusterActivity _clustActivity = ClusterActivity.values()[0];
	private ManualDistType _manualDistType = getManualDistType().values()[0];
	private int _percentToMove;
	private Address _source;
	//ArrayList _affectedNodes;
	private PartNodeInfo _affectedNode;
	private Address[] _destinations;

	public DistributionInfoData(DistributionMode distMode, ClusterActivity clustActivity, ManualDistType manDistType, int percentMove, Address source, Address[] dests) {
		_distMode = distMode;
		_clustActivity = clustActivity;
		_manualDistType = manDistType;
		_percentToMove = percentMove;
		_source = source;
		_destinations = dests;
	}

	public DistributionInfoData(DistributionMode distMode, ClusterActivity clustActivity, PartNodeInfo affectedNode) {
		_distMode = distMode;
		_clustActivity = clustActivity;
		_affectedNode = affectedNode;
	}

	public final DistributionMode getDistribMode() {
		return _distMode;
	}
	public final void setDistribMode(DistributionMode value) {
		_distMode = value;
	}

	public final ClusterActivity getClustActivity() {
		return _clustActivity;
	}
	public final void setClustActivity(ClusterActivity value) {
		_clustActivity = value;
	}

	public final ManualDistType getManualDistType() {
		return _manualDistType;
	}
	public final void setManualDistType(ManualDistType value) {
		_manualDistType = value;
	}

	public final String getGroup() {
		return _affectedNode.getSubGroup();
	}
	public final void setGroup(String value) {
		_affectedNode.setSubGroup(value);
	}

	public final int getPercentToMove() {
		return _percentToMove;
	}
	public final void setPercentToMove(int value) {
		_percentToMove = value;
	}

	public final Address getSource() {
		return _source;
	}
	public final void setSource(Address value) {
		_source = value;
	}

	public final Address[] getDestinations() {
		return _destinations;
	}
	public final void setDestinations(Address[] value) {
		_destinations = value;
	}

	public final PartNodeInfo getAffectedNode() {
		return _affectedNode;
	}
	public final void setAffectedNode(PartNodeInfo value) {
		_affectedNode = value;
	}
	@Override
	public String toString() {
		return "DistributionInfoData( " + getAffectedNode().toString() + ")";
	}
}
