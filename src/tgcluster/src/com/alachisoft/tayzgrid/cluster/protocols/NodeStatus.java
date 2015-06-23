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

package com.alachisoft.tayzgrid.cluster.protocols;

import com.alachisoft.tayzgrid.common.net.Address;

	public class NodeStatus {
		private Address _node;
 
		private byte _status;
 
		public static final byte IS_ALIVE = 1;
 
		public static final byte IS_DEAD = 2;
 
		public static final byte IS_LEAVING = 3;

 
		public NodeStatus(Address node, byte status) {
			_node = node;
			_status = status;
		}

		public final Address getNode() {
			return _node;
		}
 
		public final byte getStatus() {
			return _status;
		}

		@Override
		public String toString() {
			String toString = _node != null ? _node.toString() + ":" : ":";
			switch (_status) {
				case IS_ALIVE:
					toString += "IS_ALIVE";
					break;
				case IS_DEAD:
					toString += "IS_DEAD";
					break;
				case IS_LEAVING:
					toString += "IS_LEAVING";
					break;
				default:
					toString += "NA";
					break;
			}
			return toString;
		}
	}
	 

