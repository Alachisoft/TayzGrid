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

import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * An info object that wraps a function code and object. Function codes are to
 * be defined by the clients/derivations of clustered cache.
 */
public class Function implements ICompactSerializable, IRentableObject, java.io.Serializable {

    /**
     * The function code.
     */
    private byte _opcode;
    /**
     * The paramter for the function.
     */
    private Object _operand;
    /**
     * Inhibit processing own messages.
     */
    private boolean _excludeSelf = true;
    private Object _syncKey;
    private int _rentId;
    private Object[] _userDataPayload;
    private boolean _responseEpected = false;

    /**
     * Constructor.
     *
     * @param opcode The operation code
     * @param operand Parameter for the operation
     */
    public Function(byte opcode, Object operand) {
        _opcode = opcode;
        _operand = operand;
    }

    public Function() {
    }

    /**
     * Overloaded Constructor.
     *
     * @param opcode The operation code
     * @param operand Parameter for the operation
     * @param excludeSelf Flag to inhibit processing self messages
     */
    public Function(byte opcode, Object operand, boolean excludeSelf) {
        _opcode = opcode;
        _operand = operand;
        _excludeSelf = excludeSelf;

    }

    /**
     * Overloaded Constructor.
     *
     * @param opcode The operation code
     * @param operand Parameter for the operation
     * @param excludeSelf Flag to inhibit processing self messages
     */
    public Function(byte opcode, Object operand, boolean excludeSelf, Object syncKey) {
        this(opcode, operand, excludeSelf);
        _syncKey = syncKey;
    }

    /**
     * The function code.
     */
    public final byte getOpcode() {
        return _opcode;
    }

    public final void setOpcode(byte value) {
        _opcode = value;
    }

    /**
     * The function code.
     */
    public final Object getOperand() {
        return _operand;
    }

    public final void setOperand(Object value) {
        _operand = value;
    }

    /**
     * The function code.
     */
    public final boolean getExcludeSelf() {
        return _excludeSelf;
    }

    public final void setExcludeSelf(boolean value) {
        _excludeSelf = value;
    }

    /**
     * Gets or sets the SyncKey for the current operation.
     */
    public final Object getSyncKey() {
        return _syncKey;
    }

    public final void setSyncKey(Object value) {
        _syncKey = value;
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _opcode = reader.readByte();
        _excludeSelf = reader.readBoolean();
        _operand = reader.readObject();
        _syncKey = reader.readObject();
        _responseEpected = reader.readBoolean();
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeByte(_opcode);
        writer.writeBoolean(_excludeSelf);
        writer.writeObject(_operand);
        writer.writeObject(_syncKey);
        writer.writeBoolean(_responseEpected);
    }

    public final int getRentId() {
        return _rentId;
    }

    public final void setRentId(int value) {
        _rentId = value;
    }

    public final Object[] getUserPayload() {
        return _userDataPayload;
    }

    public final void setUserPayload(Object[] value) {
        _userDataPayload = value;
    }

    public final boolean getResponseExpected() {
        return _responseEpected;
    }

    public final void setResponseExpected(boolean value) {
        _responseEpected = value;
    }

    public String toString() {
        return (com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase.OpCodes.valueOf((new Byte(this.getOpcode())).toString())).toString();
    }
}