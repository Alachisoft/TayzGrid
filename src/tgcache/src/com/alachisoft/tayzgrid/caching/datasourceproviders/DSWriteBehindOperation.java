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

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.OperationResult;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Calendar;

public class DSWriteBehindOperation extends DSWriteOperation implements ICompactSerializable {

    private java.util.Date enqueueTime = new java.util.Date(0); //enqueue time
    private WriteBehindAsyncProcessor.OperationState operationState = WriteBehindAsyncProcessor.OperationState.values()[0];
    /**
     * task id
     */
    private String _taskId;
    /**
     */
    private String _source;
    private long _delayInterval;
    /**
     */
    private WriteBehindAsyncProcessor.TaskState _state = WriteBehindAsyncProcessor.TaskState.values()[0];
    private OperationResult.Status _dsOpState;
    private Exception _exception;

    public DSWriteBehindOperation() {
    }

    public DSWriteBehindOperation(String cacheId, Object key, Object value, CacheEntry entry, OpCode opcode, String providerName, long operationDelay, String taskId, String source, WriteBehindAsyncProcessor.TaskState taskState) {
        super(cacheId, key, value, entry, opcode, providerName);
        this._taskId = taskId;
        this._state = taskState;
        this._source = source;
        this._delayInterval = operationDelay;

    }

    public final WriteBehindAsyncProcessor.OperationState getOperationState() {
        return this.operationState;
    }

    public final void setOperationState(WriteBehindAsyncProcessor.OperationState value) {
        this.operationState = value;
    }

    public final java.util.Date getEnqueueTime() {
        return this.enqueueTime;
    }

    public final void setEnqueueTime(java.util.Date value) {
        this.enqueueTime = value;
    }

    public final void setOperationDelay(long value) {
        this._delayInterval = value;
    }

    public final String getSource() {
        return _source;
    }

    public final void setSource(String value) {
        _source = value;
    }

    public final String getTaskId() {
        return _taskId;
    }

    public final void setTaskId(String value) {
        _taskId = value;
    }

    public final WriteBehindAsyncProcessor.TaskState getState() {
        return _state;
    }

    public final void setState(WriteBehindAsyncProcessor.TaskState value) {
        _state = value;
    }

    public final long getSize() {
        if (this._entry != null) {
            return this._entry.getSize();
        }
        return 0;
    }

    public final OperationResult.Status getDSOpState() {
        return this._dsOpState;
    }

    public final void setDSOpState(OperationResult.Status value) {
        this._dsOpState = value;
    }

    public final Exception getException() {
        return this._exception;
    }

    public final void setException(Exception value) {
        this._exception = value;
    }

    public final boolean getOperationDelayExpired() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTime(this.enqueueTime);
        cal.add(Calendar.MILLISECOND, (int) this._delayInterval);//?
        java.util.Date expireTime = cal.getTime();
        if (expireTime.compareTo(new java.util.Date()) <= 0) {
            return true;
        }
        return false;
    }

    public final int compareTo(Object obj) {
        DSWriteBehindOperation dsOp = (DSWriteBehindOperation) obj;
        return (new Integer(this._retryCount)).compareTo(dsOp._retryCount);
    }

    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
        super.deserialize(reader);
        enqueueTime = (new NCDateTime(reader.readLong())).getDate();
        operationState = (WriteBehindAsyncProcessor.OperationState) reader.readObject();
        Object tempVar = reader.readObject();
        _taskId = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = reader.readObject();
        _source = (String) ((tempVar2 instanceof String) ? tempVar2 : null);
        _delayInterval = reader.readLong();
        _state = (WriteBehindAsyncProcessor.TaskState) reader.readObject();
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        super.serialize(writer);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.MILLISECOND, 0);
        c.setTime(enqueueTime);
        NCDateTime ncdt = null;
        try {
            ncdt = new NCDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
        } catch (ArgumentException argumentException) {
        }
        writer.writeLong(ncdt.getTicks());
        writer.writeObject(operationState);
        writer.writeObject(_taskId);
        writer.writeObject(_source);
        writer.writeLong(_delayInterval);
        writer.writeObject(_state);
    }
}
