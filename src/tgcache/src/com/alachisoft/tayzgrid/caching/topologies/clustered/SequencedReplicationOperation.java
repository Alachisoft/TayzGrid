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

import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.common.net.Address;
import java.util.Iterator;
import java.util.Map;

public class SequencedReplicationOperation {

    private Address _source;
    private Address _destination;
    private long _sequenceId;
    private long _viewId;
    private java.util.ArrayList<ReplicationOperation> _operations;
    private Object[] _opCodes;
    private Object[] _infos;
    private Object[] _userPayloads;
    private java.util.ArrayList _compilationInfos;
    private OperationContext _operationContext;

    public SequencedReplicationOperation(long sequenceId) {
        _sequenceId = sequenceId;
        _operations = new java.util.ArrayList<ReplicationOperation>();
    }

    public final long getSequenceId() {
        return _sequenceId;
    }

    public final Object[] getOpCodes() {
        return _opCodes;
    }

    public final void setOpCodes(Object[] value) {
        _opCodes = value;
    }

    public final Object[] getInfos() {
        return _infos;
    }

    public final void setInfos(Object[] value) {
        _infos = value;
    }

    public final Object[] getUserPayload() {
        return _userPayloads;
    }

    public final void setUserPayload(Object[] value) {
        _userPayloads = value;
    }

    public final java.util.ArrayList getCompilationInfo() {
        return _compilationInfos;
    }

    public final void setCompilationInfo(java.util.ArrayList value) {
        _compilationInfos = value;
    }

    public final Address getSource() {
        return _source;
    }

    public final void setSource(Address value) {
        _source = value;
    }

    public final Address getDestination() {
        return _destination;
    }

    public final void setDestination(Address value) {
        _destination = value;
    }

    public final long getViewId() {
        return _viewId;
    }

    public final void setViewId(long value) {
        _viewId = value;
    }

    public final OperationContext getOperationContext() {
        return _operationContext;
    }

    public final void setOperationContext(OperationContext value) {
        _operationContext = value;
    }

    public final void Add(ReplicationOperation operation) {
        _operations.add(operation);
    }

    public final void Compile(tangible.RefObject<Object[]> opCodes, tangible.RefObject<Object[]> info, tangible.RefObject<Object[]> userPayload, tangible.RefObject<java.util.ArrayList> compilationInfo) {
        java.util.ArrayList opCodesToBeReplicated = new java.util.ArrayList();
        java.util.ArrayList infoToBeReplicated = new java.util.ArrayList();
        java.util.ArrayList payLoad = new java.util.ArrayList();
        compilationInfo.argvalue = new java.util.ArrayList();
        for (Iterator<ReplicationOperation> it = _operations.iterator(); it.hasNext();) {
            ReplicationOperation operation = it.next();
            Map.Entry entry = (Map.Entry) operation.getData();
            opCodesToBeReplicated.add(entry.getKey());
            infoToBeReplicated.add(entry.getValue());
            if (operation.getUserPayLoad() != null) {
                for (int j = 0; j < operation.getUserPayLoad().length; j++) {
                    payLoad.add(operation.getUserPayLoad()[j]);
                }
            }
            compilationInfo.argvalue.add(operation.getPayLoadSize());
        }

        opCodes.argvalue = opCodesToBeReplicated.toArray(new Object[0]);
        info.argvalue = infoToBeReplicated.toArray(new Object[0]);
        userPayload.argvalue = payLoad.toArray(new Object[0]);
    }
}
