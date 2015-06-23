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

import com.alachisoft.tayzgrid.common.net.Address;

/** 
 Used to listen for connection changes in the Channel
 <p><b>Author:</b> Chris Koiak, Bela Ban</p>
 <p><b>Date:</b>  12/03/2003</p>
*/
public interface ChannelListener {
	/** 
	 Channel Connected Event
	 
	 @param channel Channel that was connected
	*/
	void channelConnected(Channel channel);

	/** 
	 Channel Disconnected Event
	 
	 @param channel Channel that was disconnected
	*/
	void channelDisconnected(Channel channel);

	/** 
	 Channel Closed Event
	 
	 @param channel Channel that was closed
	*/
	void channelClosed(Channel channel);

	/** 
	 Channel Shunned Event
	*/
	void channelShunned();

	/** 
	 Channel Reconnected Event 
	 
	 @param addr Channel that was reconnected
	*/
	void channelReconnected(Address addr);
}
