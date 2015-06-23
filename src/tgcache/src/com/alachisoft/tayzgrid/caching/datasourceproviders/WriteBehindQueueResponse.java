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

package com.alachisoft.tayzgrid.caching.datasourceproviders;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;

public class WriteBehindQueueResponse implements ICompactSerializable
{
	private WriteBehindAsyncProcessor.WriteBehindQueue _queue;
	private String _nextChunkId;
	private String _prevChunkId;
        @Deprecated
        public WriteBehindQueueResponse()
        {
        }
	public WriteBehindQueueResponse(WriteBehindAsyncProcessor.WriteBehindQueue queue, String nextChunkId, String prvChunkId) {
		_queue = queue;
		_nextChunkId = nextChunkId;
		_prevChunkId = prvChunkId;
	}

	public final String getNextChunkId() {
		return _nextChunkId;
	}
	public final void setNextChunkId(String value) {
		_nextChunkId = value;
	}

	public final String getPrevChunkId() {
		return _prevChunkId;
	}
	public final void setPrevChunkId(String value) {
		_prevChunkId = value;
	}

	public final WriteBehindAsyncProcessor.WriteBehindQueue getQueue() {
		return _queue;
	}
	public final void setQueue(WriteBehindAsyncProcessor.WriteBehindQueue value) {
		_queue = value;
	}


	public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, java.io.IOException {
		Object tempVar = reader.readObject();
                _queue = (WriteBehindAsyncProcessor.WriteBehindQueue)((tempVar instanceof WriteBehindAsyncProcessor.WriteBehindQueue) ? tempVar : null);
		Object tempVar2 = reader.readObject();
		_nextChunkId = (String)((tempVar2 instanceof String) ? tempVar2 : null);
		Object tempVar3 = reader.readObject();
		_prevChunkId = (String)((tempVar3 instanceof String) ? tempVar3 : null);
	}

	public final void serialize(CacheObjectOutput writer) throws java.io.IOException{
		writer.writeObject(_queue);
		writer.writeObject(_nextChunkId);
		writer.writeObject(_prevChunkId);
	}
}
