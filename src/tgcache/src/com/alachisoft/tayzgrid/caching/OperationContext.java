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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.UUID;


/**
 * make it serializable coz cache operations performed through remoting will fail otherwise.
 */
public class OperationContext implements ICompactSerializable, Cloneable,java.io.Serializable
{
    private java.util.HashMap _fieldValueTable;
    private static String s_operationUniqueID;
    private static long s_operationCounter;
    private static Object s_lock = new Object();
    
    static 
    { 
       s_operationUniqueID = UUID.randomUUID().toString().substring(0, 4);
    }
    
    public OperationContext()
    {
        CreateOperationId(); 
    }

    public OperationContext(OperationContextFieldName fieldName, Object fieldValue)
    {
        CreateOperationId(); 
        Add(fieldName, fieldValue);
    }

    private void CreateOperationId()
    {
        long opCounter = 0;
        synchronized (s_lock)
        {
                opCounter = s_operationCounter++;
        }
        OperationID operationId = new OperationID(s_operationUniqueID,opCounter);
        Add(OperationContextFieldName.OperationId, operationId);
    }
    
    public final OperationID getOperatoinID()
    {
        return (OperationID)GetValueByField(OperationContextFieldName.OperationId);
    }
    
    public final boolean getIsRemoveQueryOperation()
    {        
         return ((Boolean) GetValueByField(OperationContextFieldName.RemoveQueryOperation)).booleanValue();
    }
    
    public final void Add(OperationContextFieldName fieldName, Object fieldValue)
    {
        synchronized (this)
        {
            if (_fieldValueTable == null)
            {
                _fieldValueTable = new java.util.HashMap();
            }

            _fieldValueTable.put(fieldName, fieldValue);
        }
    }

    public final Object GetValueByField(OperationContextFieldName fieldName)
    {
        Object result = null;

        if (_fieldValueTable != null)
        {
            result = _fieldValueTable.get(fieldName);
        }

        return result;
    }

    public final boolean Contains(OperationContextFieldName fieldName)
    {
        boolean contains = false;

        if (_fieldValueTable != null)
        {
            contains = _fieldValueTable.containsKey(fieldName);
        }

        return contains;
    }

    public final void RemoveValueByField(OperationContextFieldName fieldName)
    {
        if (_fieldValueTable != null)
        {
            _fieldValueTable.remove(fieldName);
        }
    }

    public final boolean IsOperation(OperationContextOperationType operationType)
    {
        if ((OperationContextOperationType) this.GetValueByField(OperationContextFieldName.OperationType) == operationType)
        {
            return true;
        }
        return false;
    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _fieldValueTable = (java.util.HashMap) reader.readObject();
        
        // + Usman
        // Addeded spicifically for the case when a clustered inproc cache makes a remote call. This allows serialization for sync wrapper. 
        if(!_fieldValueTable.containsKey(OperationContextFieldName.IsClusteredCall))
            _fieldValueTable.put(OperationContextFieldName.IsClusteredCall, true);
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(_fieldValueTable);
    }

    public final Object clone()
    {
        OperationContext oc = new OperationContext();
        synchronized (this)
        {
            oc._fieldValueTable = (java.util.HashMap)_fieldValueTable.clone();
        }
        return oc;
    }
}
