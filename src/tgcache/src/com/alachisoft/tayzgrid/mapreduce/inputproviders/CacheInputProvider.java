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

package com.alachisoft.tayzgrid.mapreduce.inputproviders;

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.local.LocalCacheBase;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.KeyValuePair;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceInput;
import com.alachisoft.tayzgrid.runtime.mapreduce.QueryFilter;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSet;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSetConstant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tayzgrid_dev
 */
public class CacheInputProvider implements MapReduceInput
{
    private QueryFilter queryFilter;
    private CacheBase dataSource;
    private ArrayList keyList;
    private Iterator itKeys;
    private boolean isSerilizedData=true;
    private String serializationContext;
    private final CacheRuntimeContext _context;
    
    public CacheInputProvider(CacheBase source,QueryFilter filter,CacheRuntimeContext context)    
    {
        queryFilter=filter;
        dataSource=source;
        _context=context;
        
        if(_context!=null)
            this.serializationContext=_context._serializationContext;
    }
    
    @Override
    public void initialize(HashMap parameters) 
    {
        
    }

    @Override
    public boolean hasMoreElements() 
    {        
        if(itKeys!=null && itKeys.hasNext())
            return true;
        else
            return false;
    }

    @Override
    public KeyValuePair nextElement() 
    {
        KeyValuePair pair=null;
        try {
            Object key=itKeys.next();
            OperationContext oc = new OperationContext();
            oc.Add(OperationContextFieldName.DataFormat, DataFormat.Object);
            CacheEntry entry = dataSource.Get(key,false,oc);
            
            Object value=null;
            if(entry.getFlag().IsBitSet((byte)SerializationBitSetConstant.Flattened))
            {
                value=entry.getFullUserData();
                value=com.alachisoft.tayzgrid.serialization.util.SerializationUtil.safeDeserialize(value,serializationContext,new SerializationBitSet());
            }
            else
                value = entry.getValue();
            
            pair=new KeyValuePair();
            pair.setKey(key);
            pair.setValue(value);
            
            return pair;
        } catch (Exception ex) 
        {
            if(_context!=null && _context.getCacheLog()!=null)
                _context.getCacheLog().Error("CacheInputProvider.nextElement", ex.getMessage());
        }
        return pair;    
    }

    @Override
    public void load() throws OperationFailedException 
    {
        try 
        {
            if(this.queryFilter!=null)
            {
                QueryResultSet result=dataSource.Search(queryFilter.getQuery(),queryFilter.getParameters(),new OperationContext());
                if(result!=null) {

                    keyList=result.getSearchKeysResult();

                    if(dataSource.getContext().getCacheLog().getIsInfoEnabled())
                        dataSource.getContext().getCacheLog().Info("InputProvider.LoadQuery", "Task Input Loaded, items found: " + keyList.size());

                } else {
                    throw new Exception("QueryResultSet is null.");
                }
            }
            else
            {

                keyList=new ArrayList(Arrays.asList(dataSource.getKeys()));

                if(dataSource.getContext().getCacheLog().getIsInfoEnabled())
                    dataSource.getContext().getCacheLog().Info("InputProvider.LoadKeys", "Task Input Loaded, items found: " + keyList.size());

            }
            if(keyList!=null)
                itKeys=keyList.iterator();
            
        } catch (Exception ex) {
            dataSource.getContext().getCacheLog().Error("InputProvider.LoadInput", "Failed, Exception: " + ex.getMessage());
            throw new OperationFailedException(ex.getMessage());
        } 
    }
    
}
