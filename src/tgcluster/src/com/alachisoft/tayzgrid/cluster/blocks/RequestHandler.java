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

package com.alachisoft.tayzgrid.cluster.blocks;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.cluster.Message;

// $Id: RequestHandler.java,v 1.1.1.1 2003/09/09 01:24:08 belaban Exp $



public interface RequestHandler {
	Object handle(com.alachisoft.tayzgrid.cluster.Message msg);

	Object handleNHopRequest(com.alachisoft.tayzgrid.cluster.Message msg, tangible.RefObject<Address> destination, tangible.RefObject<com.alachisoft.tayzgrid.cluster.Message> replicationMsg);
}
