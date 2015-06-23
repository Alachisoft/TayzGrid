package com.alachisoft.tayzgrid.caching.datasourceproviders.JCache;

import java.util.*;
/**
 *
 *
 */


public class JCacheIterable<T> implements Iterable<T>{

    private JCacheIterator<T> _iterator;
    
    public JCacheIterable(T[] keys)
    {
        _iterator = new JCacheIterator<T>(keys);
    }
    
    @Override
    public Iterator<T> iterator() {
        return _iterator;
    }
    
}
