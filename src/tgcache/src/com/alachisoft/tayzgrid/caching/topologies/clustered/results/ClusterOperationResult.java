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

package com.alachisoft.tayzgrid.caching.topologies.clustered.results;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class ClusterOperationResult implements ICompactSerializable
{
    public enum Result
    {

        Completed,
        ParitalTimeout,
        FullTimeout;

        public int getValue()
        {
            return this.ordinal();
        }

        public static Result forValue(int value)
        {
            return values()[value];
        }
    }
    private Result _result = Result.values()[0];
    private String _lockId;

    public ClusterOperationResult()
    {
    }

    public ClusterOperationResult(Result executed)
    {
        _result = executed;
    }

    public final Result getExecutionResult()
    {
        return _result;
    }

    public final void setExecutionResult(Result value)
    {
        _result = value;
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _result = Result.forValue(reader.readByte());
    }

    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeByte(_result.getValue());
    }
}
