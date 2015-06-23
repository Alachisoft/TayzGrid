/*
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

package com.alachisoft.tayzgrid.cluster.util;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

// $Id: List.java,v 1.6 2004/07/05 14:17:35 belaban Exp $

/**
 * Doubly-linked list. Elements can be added at head or tail and removed from head/tail. This class is tuned for element access at either head or tail, random access to elements is
 * not very fast; in this case use Vector. Concurrent access is supported: a thread is blocked while another thread adds/removes an object. When no objects are available, removal
 * returns null.
 *
 * <author> Bela Ban </author>
 */
public class List implements Cloneable, ICompactSerializable, Serializable
{

    public final java.util.List getContents()
    {
        java.util.List retval = Collections.synchronizedList(new java.util.ArrayList(_size));
        Element el;

        synchronized (mutex)
        {
            el = head;
            while (el != null)
            {
                retval.add(el.obj);
                el = el.next;
            }
        }
        return retval;
    }
    protected Element head = null, tail = null;
    protected int _size = 0;
    protected Object mutex = new Object();

    protected static class Element implements Serializable
    {

        private void InitBlock(List enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private List enclosingInstance;

        public final List getEnclosing_Instance()
        {
            return enclosingInstance;
        }
        public Object obj = null;
        public Element next = null;
        public Element prev = null;

        public Element(List enclosingInstance, Object o)
        {
            InitBlock(enclosingInstance);
            obj = o;
        }
    }

    public List()
    {
    }

    /**
     * Adds an object at the tail of the list.
     */
    public void add(Object obj)
    {
        Element el = new Element(this, obj);

        synchronized (mutex)
        {
            if (head == null)
            {
                head = el;
                tail = head;
                _size = 1;
            }
            else
            {
                el.prev = tail;
                tail.next = el;
                tail = el;
                _size++;
            }
        }
    }

    /**
     * Adds an object at the head of the list.
     */
    public void addAtHead(Object obj)
    {
        Element el = new Element(this, obj);

        synchronized (mutex)
        {
            if (head == null)
            {
                head = el;
                tail = head;
                _size = 1;
            }
            else
            {
                el.next = head;
                head.prev = el;
                head = el;
                _size++;
            }
        }
    }

    /**
     * Removes an object from the tail of the list. Returns null if no elements available
     */
    public final Object remove()
    {
        Element retval = null;

        synchronized (mutex)
        {
            if (tail == null)
            {
                return null;
            }
            retval = tail;
            if (head == tail)
            {
                // last element
                head = null;
                tail = null;
            }
            else
            {
                tail.prev.next = null;
                tail = tail.prev;
                retval.prev = null;
            }

            _size--;
        }
        return retval.obj;
    }

    /**
     * Removes an object from the head of the list. Returns null if no elements available
     */
    public final Object removeFromHead()
    {
        Element retval = null;

        synchronized (mutex)
        {
            if (head == null)
            {
                return null;
            }
            retval = head;
            if (head == tail)
            {
                // last element
                head = null;
                tail = null;
            }
            else
            {
                head = head.next;
                head.prev = null;
                retval.next = null;
            }
            _size--;
        }
        return retval.obj;
    }

    /**
     * Returns element at the tail (if present), but does not remove it from list.
     */
    public final Object peek()
    {
        synchronized (mutex)
        {
            return tail != null ? tail.obj : null;
        }
    }

    /**
     * Returns element at the head (if present), but does not remove it from list.
     */
    public final Object peekAtHead()
    {
        synchronized (mutex)
        {
            return head != null ? head.obj : null;
        }
    }

    /**
     * Removes element
     * <code>obj</code> from the list, checking for equality using the
     * <code>equals</code> operator. Only the first duplicate object is removed. Returns the removed object.
     */
    public final Object removeElement(Object obj)
    {
        Element el = null;
        Object retval = null;

        synchronized (mutex)
        {
            el = head;
            while (el != null)
            {
                if (el.obj.equals(obj))
                {
                    retval = el.obj;
                    if (head == tail)
                    {
                        // only 1 element left in the list
                        head = null;
                        tail = null;
                    }
                    else if (el.prev == null)
                    {
                        // we're at the head
                        head = el.next;
                        head.prev = null;
                        el.next = null;
                    }
                    else if (el.next == null)
                    {
                        // we're at the tail
                        tail = el.prev;
                        tail.next = null;
                        el.prev = null;
                    }
                    else
                    {
                        // we're somewhere in the middle of the list
                        el.prev.next = el.next;
                        el.next.prev = el.prev;
                        el.next = null;
                        el.prev = null;
                    }
                    _size--;
                    break;
                }

                el = el.next;
            }
        }
        return retval;
    }

    public final void removeAll()
    {
        synchronized (mutex)
        {
            _size = 0;
            head = null;
            tail = null;
        }
    }

    public final int size()
    {
        return _size;
    }

    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder("[");
        Element el = head;

        while (el != null)
        {
            if (el.obj != null)
            {
                ret.append(el.obj + " ");
            }
            el = el.next;
        }
        ret.append(']');
        return ret.toString();
    }

    public final String dump()
    {
        StringBuilder ret = new StringBuilder("[");
        for (Element el = head; el != null; el = el.next)
        {
            ret.append(el.obj + " ");
        }

        return ret.toString() + ']';
    }

    public final java.util.Iterator elements()
    {
        return new ListEnumerator(this, head);
    }

    public final boolean contains(Object obj)
    {
        Element el = head;

        while (el != null)
        {
            if (el.obj != null && el.obj.equals(obj))
            {
                return true;
            }
            el = el.next;
        }
        return false;
    }

    public final List copy()
    {
        List retval = new List();

        synchronized (mutex)
        {
            for (Element el = head; el != null; el = el.next)
            {
                retval.add(el.obj);
            }
        }
        return retval;
    }

    public final Object clone()
    {
        return copy();
    }

 
    public static class ListEnumerator implements java.util.Iterator
    {

        private void InitBlock(List enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private Object tempAuxObj;

        public final boolean hasNext()
        {
            boolean result = hasMoreElements();
            if (result)
            {
                tempAuxObj = next();
            }
            return result;
        }

        public final void Reset()
        {
            tempAuxObj = null;
        }

        public final Object getCurrent()
        {
            return tempAuxObj;
        }
        private List enclosingInstance;

        public final List getEnclosing_Instance()
        {
            return enclosingInstance;
        }
        public Element curr = null;

        public ListEnumerator(List enclosingInstance, Element start)
        {
            InitBlock(enclosingInstance);
            curr = start;
        }

        public final boolean hasMoreElements()
        {
            return curr != null;
        }

        public final Object next()
        {
            Object retval;

            if (curr == null)
            {
                throw new IllegalArgumentException();
            }
            retval = curr.obj;
            curr = curr.next;
            return retval;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

 
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        Object obj;
        int new_size = reader.readInt();

        if (new_size == 0)
        {
            return;
        }

        for (int i = 0; i < new_size; i++)
        {
            obj = reader.readObject();
            add(obj);
        }
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        Element el;
        synchronized (mutex)
        {
            el = head;
            writer.writeInt(_size);
            for (int i = 0; i < _size; i++)
            {
                writer.writeObject(el.obj);
                el = el.next;
            }
        }
    }
 
}
