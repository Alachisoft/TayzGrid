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


/** 
 It represents a cache node.
 <p>
 The properties NodeGroup and MirrorGroup give the group id of the active
 and mirror cache on the same node.
 
 Note: Mirror on the same node belongs to some other active node hence group id
 for both are different for a single node.
 </p>
*/
public class GroupInfo {
	private String nodeGroup;
	private String mirrorGroup;

	/** 
	 Group id for the active cache on the node
	*/
	public final String getNodeGroup() {
		return nodeGroup;
	}
	public final void setNodeGroup(String value) {
		nodeGroup = value;
	}

	/** 
	 Group id for the mirror cache on the node
	*/
	public final String getMirrorGroup() {
		return mirrorGroup;
	}
	public final void setMirrorGroup(String value) {
		mirrorGroup = value;
	}

	public GroupInfo() {
	}

	public GroupInfo(String nodeGroup, String mirrorGroup) {
		this.nodeGroup = nodeGroup;
		this.mirrorGroup = mirrorGroup;
	}

	@Override
	public String toString() {
		return String.format("Group = %1$s, Mirror = %2$s", nodeGroup, mirrorGroup);
	}
}
