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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * provides Enumerator over replicated client cache
 */
public class LazyKeysetEnumerator implements ResetableIterator
{

    /**
     * Parent of the enumerator.
     */
    protected CacheBase _cache = null;
    /**
     * The list of keys to enumerate over.
     */
    protected Object[] _keyList = null;
    /**
     * The current position of the enumeration.
     */
    protected int _current = 0;
    /**
     * Flag to indicate invalid state of enumerator.
     */
    protected boolean _bvalid = false;
    /**
     * Flag to allow the enumerator to return null object values.
     */
    protected boolean _bAllowNulls = false;
    /**
     * Holder for current dictionary entry.
     */
    protected Map.Entry _de;

    /**
     * Constructor      *
     * @param cache
     * @param keyList
     * @param bAllowNulls
     */
    public LazyKeysetEnumerator(CacheBase cache, Object[] keyList, boolean bAllowNulls)
    {
        _cache = cache;
        _keyList = keyList;
        _bAllowNulls = bAllowNulls;
        this.reset();
    }

    /**
     * Set the enumerator to its initial position. which is before the first element in the collection
     */
    public void reset()
    {
        _bvalid = false;
        _current = 0;
    }

    /**
     * Advance the enumerator to the next element of the collection      *
     * @return
     */
    private boolean MoveNext()
    {
        _bvalid = _keyList != null && _current < _keyList.length;

        if (_bvalid)
        {
            _de = (Map.Entry)new AbstractMap.SimpleEntry(_keyList[_current++], null) {};
        }

        return _bvalid;
    }

    /**
     * Gets the current element in the collection
     */
    private Object getCurrent() throws TimeoutException, CacheException
    {
        if (!_bvalid)
        {
            throw new UnsupportedOperationException();
        }
        if (_de.getValue() == null)
        {
            try
            {
                _de.setValue(FetchObject(_de.getKey(), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation)));
            }
            catch (Exception e)
            {
                _cache.getCacheLog().Error("LazyKeysetEnumerator.Current", e.getMessage());
            }
            if (!_bAllowNulls && (_de.getValue() == null))
            {
                throw new UnsupportedOperationException();
            }
        }
        return _de;
    }

    /**
     * Gets the key and value of the current dictionary entry.
     */
    private Map.Entry getEntry()
    {
        if (!_bvalid)
        {
            throw new UnsupportedOperationException();
        }
        return _de;
    }

    /**
     * Gets the key of the current dictionary entry
     */
    private Object getKey()
    {
        if (!_bvalid)
        {
            throw new UnsupportedOperationException();
        }
        return _de.getKey();
    }

    /**
     * Gets the value of the current dictionary entry
     */
    private Object getValue()
    {
        if (!_bvalid)
        {
            throw new UnsupportedOperationException();
        }
        if (_de.getValue() == null)
        {
            try
            {
                _de.setValue(FetchObject(_de.getKey(), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation)));
            }
            catch (Exception e)
            {
                _cache.getCacheLog().Error("LazyKeysetEnumerator.Value", e.toString());
            }
            if (!_bAllowNulls && (_de.getValue() == null))
            {
                throw new UnsupportedOperationException();
            }
        }
        return _de.getValue();
    }

    /**
     * Does the lazy loading of object. This method is virtual so containers can customize object fetching logic.
     *
     * @param key
     * @return
     */
    protected Object FetchObject(Object key, OperationContext operationContext)throws TimeoutException, CacheException, OperationFailedException, LockingException, SuspectedException
    {
        operationContext.Add(OperationContextFieldName.GenerateQueryInfo, true);
        return _cache.Get(key, operationContext);
    }

    protected Object LoadValue(Object key)throws TimeoutException, CacheException, OperationFailedException, LockingException, SuspectedException
    {
        return FetchObject(key, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
    }


    @Override
    public boolean hasNext()
    {
        _bvalid = _keyList != null && (_current) < _keyList.length;
        return _bvalid;
    }

    @Override
    public Object next()
    {
        try
        {
            if (_bvalid)
            {
                _de = new LazyLoadedEntry(this,_keyList[_current]);
                _current++;
                return _de;
            }
            throw new NoSuchElementException("Item does not exist");
        }
        catch (Exception cacheException)
        {
            throw new NoSuchElementException(cacheException.getMessage());
        }
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    //This entry is designed in such a way that value is fetched only when
    class LazyLoadedEntry implements Map.Entry{

        private Object _key;
        private Object _value;
        private boolean _loaded;
        private LazyKeysetEnumerator _loader;
        
        public LazyLoadedEntry(LazyKeysetEnumerator loader){
            _loader = loader;
        }
        
        public LazyLoadedEntry(LazyKeysetEnumerator loader,Object key){
            this(loader);
            _key = key;
        }
        
        @Override
        public Object getKey() {
            return _key;
        }

        @Override
        public Object getValue() {
            if(!_loaded){
                try{
                    _value = _loader.LoadValue(_key);
                }
                catch(Exception e){
                    
                }
                _loaded = true;
            }
            
            return _value;
                
        }

        @Override
        public Object setValue(Object value) {
            Object oldValue = _value;
            _value = value;
            return oldValue;
        }
        
    }
}
