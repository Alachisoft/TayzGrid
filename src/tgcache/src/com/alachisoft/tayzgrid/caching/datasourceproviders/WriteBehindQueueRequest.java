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
import java.io.IOException;

public class WriteBehindQueueRequest implements ICompactSerializable
{

    private String _nextChunkId;
    private String _prevChunkId;
    @Deprecated
    public WriteBehindQueueRequest()
    {
    }

    public WriteBehindQueueRequest(String nextChunkId, String prevChunkId)
    {
        _nextChunkId = nextChunkId;
        _prevChunkId = prevChunkId;
    }

    public final String getNextChunkId()
    {
        return _nextChunkId;
    }

    public final void setNextChunkId(String value)
    {
        _nextChunkId = value;
    }

    public final String getPrevChunkId()
    {
        return _prevChunkId;
    }

    public final void setPrevChunkId(String value)
    {
        _prevChunkId = value;
    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        Object tempVar = reader.readUTF();
        _nextChunkId = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = reader.readUTF();
        _prevChunkId = (String) ((tempVar2 instanceof String) ? tempVar2 : null);
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeUTF(_nextChunkId);
        writer.writeUTF(_prevChunkId);
    }
}
