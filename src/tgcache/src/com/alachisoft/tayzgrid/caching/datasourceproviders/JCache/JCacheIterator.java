
package com.alachisoft.tayzgrid.caching.datasourceproviders.JCache;
import java.util.*;

    
/**
 * converts an array of keys into an iterator.
 * Was needed because the JCache ReadThrough Provider implementation needed an iterator.
 *
 */


public class JCacheIterator<T> implements Iterator<T>{
    /**
     * Array being converted to an iterator.
     */
    private T _keys[];
    /**
     * Current Array index position.
     */
    private int _pos = 0;
    /**
     * Tells whether the last array element has been removed or not.
     */
    private boolean _lastRemoved = false;
    
    /**
     * Creates an iterator from an array.
     * @param keys = array of keys that requires enumeration
     */
    public JCacheIterator(T[] keys)
    {
        _keys = keys;
    }
    
    /**
     * Tests if this Iterator contains more than one element.
     * 
     * @return true ONLY IF this iterator object contains at least
     * one more element to provide; otherwise, return false.
     */
    @Override
    public boolean hasNext() {
      return (_pos < _keys.length);
    }

    /**
     * Returns the next element of this Iterator if this Iterator object
     * has at least one more element to provide.
     * 
     * @return the next element of this Iterator
     * 
     * @throws NoSuchElementException if no more element exists. 
     */
    @Override
    public T next() throws NoSuchElementException {
        if(_pos >= _keys.length)
            throw new NoSuchElementException("No such index exists.");
        T value = _keys[_pos];
        _pos++;
        _lastRemoved = false;
        return value;
    }

    /**
     * Removes the last object from the array by setting the slot
     * in the array to null.
     * Maybe called once per call to next.
     * 
     * @throws IllegalStateException if the next method has not yet
     * been called, or the remove method has already been called after the 
     * last call to the next method.
     */
    
    @Override
    public void remove() {
        if(_pos == 0)
            throw new IllegalStateException();
        if(_lastRemoved) throw new IllegalStateException();
        _keys[_pos-1] = null;
        _lastRemoved = true;
    }
    

    

    
        
}
