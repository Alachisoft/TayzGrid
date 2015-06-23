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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class StateTxfrInfo implements ICompactSerializable {

    public java.util.HashMap data;
    public boolean transferCompleted;
    private java.util.ArrayList _payLoad;
    private java.util.ArrayList _payLoadCompilationInformation;
    private long sendDataSize;

    /**
     *
     * @deprecated Used only for ICompact
     */
    @Deprecated
    public StateTxfrInfo() {
    }

    public StateTxfrInfo(boolean transferCompleted) {
        this.transferCompleted = transferCompleted;
        data = null;
    }

    public StateTxfrInfo(java.util.HashMap data, java.util.ArrayList payLoad, java.util.ArrayList payLoadCompInfo, boolean transferCompleted) {
        this.data = data;
        this.transferCompleted = transferCompleted;
        _payLoad = payLoad;
        _payLoadCompilationInformation = payLoadCompInfo;
    }
    public StateTxfrInfo(java.util.HashMap data, java.util.ArrayList payLoad, java.util.ArrayList payLoadCompInfo, boolean transferCompleted,long size) {
        this.data = data;
        this.transferCompleted = transferCompleted;
        _payLoad = payLoad;
        _payLoadCompilationInformation = payLoadCompInfo;
        this.sendDataSize = size;
    }

    public long getDataSize(){
        return sendDataSize;
    }
    public final java.util.ArrayList getPayLoad() {
        return _payLoad;
    }

    public final java.util.ArrayList getPayLoadCompilationInfo() {
        return _payLoadCompilationInformation;
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        data = (java.util.HashMap) reader.readObject();
        transferCompleted = reader.readBoolean();
        Object tempVar = reader.readObject();
        _payLoadCompilationInformation = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
        this.sendDataSize = (long)reader.readLong();
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(data);
        writer.writeBoolean(transferCompleted);
        writer.writeObject(_payLoadCompilationInformation);
        writer.writeLong(sendDataSize);
        
    }
}