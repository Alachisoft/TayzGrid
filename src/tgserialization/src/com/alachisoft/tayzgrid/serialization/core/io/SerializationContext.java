/*
 * @(#)SerializationContext.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core.io;

import com.alachisoft.tayzgrid.serialization.util.CookieList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that maintains serialization/deserialization context. This is used to
 * resolve circular and shared references.
 *
 * @version 1.0, September 18, 2008
 */
public class SerializationContext {
    /*
     * Represents an invalid cookie value.
     */

    public static final int InvalidCookie = -1;

    private TypeSurrogateSelector mSurrogateSelector;
//    private Map<Object, Integer> mCookieList;
    private CookieList mCookieList;
    private Map<Object, Object> mUserItems;
    private List<Object> mGraphList;

    /**
     * Creates a new instance of SerializationContext
     */
    public SerializationContext() {
        this.mSurrogateSelector = null;
        this.mCookieList = new CookieList();
        this.mUserItems = new HashMap<Object, Object>();
        this.mGraphList = new ArrayList<Object>();
    }

    /**
     * Creates a new instance of SerializationContext
     *
     */
    public SerializationContext(TypeSurrogateSelector surrogateSelector) {
        this.mSurrogateSelector = surrogateSelector;
        this.mCookieList = new CookieList();
        this.mUserItems = new HashMap<Object, Object>();
        this.mGraphList = new ArrayList<Object>();
    }

    /**
     * Returns the associated <see cref="INxTypeSurrogateSelector"/> instance.
     */
    public TypeSurrogateSelector getSurrogateSelector() {
        return this.mSurrogateSelector;
    }

    /**
     * Gets user specified item indicated by the given key.
     *
     * @param key Identifier of the item.
     * @return Object associated with the given key.
     */
    public Object getUserItem(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return this.mUserItems.get(key);
    }

    /**
     * Saves user specified key, value pair.
     *
     * @param key Key of the pair.
     * @param value Value of the pair.
     */
    public void putUserItem(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        this.mUserItems.put(key, value);
    }

    /**
     * Resets this instance of SerializationContext
     */
    public void clear() {
        mGraphList.clear();
        mCookieList.clear();
        mUserItems.clear();
    }

    /**
     * Returns cookie for a given graph.
     *
     * @param graph Object whose cookie is to be returned.
     * @return cookie for the graph valid in this serialization context.
     */
    public Integer getCookie(Object graph) {
        if (this.mCookieList.contains(graph)) {
            return (Integer) this.mCookieList.indexOf(graph);
        }
        return InvalidCookie;
    }

    /**
     * Returns a graph by its cookie. If there is no such cookie null is
     * returned.
     *
     * @param key Cookie whose associated object is to be returned.
     * @return Object represented by the cookie.
     */
    public Object getObject(Integer key) {
        if (key > SerializationContext.InvalidCookie && key < this.mGraphList.size()) {
            return this.mGraphList.get(key);
        }
        return null;
    }

    /**
     * Adds a graph to the reader context, assigns a cookie to it. Currently the
     * index of the graph in the list is its cookie.
     *
     * @param graph Object whose cookie is to be returned.
     * @return cookie for the graph valid in this serialization context.
     */
    public Integer rememberForRead(Object graph) {
        Integer cookie = this.mGraphList.size();
        this.mGraphList.add(graph);
        return cookie;
    }

    /**
     * Adds a graph to the writer context, assigns a cookie to it. Currently the
     * index of the graph in the list is its cookie.
     *
     * @param graph Object whose cookie is to be returned.
     * @return cookie for the graph valid in this serialization context.
     */
    public Integer rememberForWrite(Object graph) {
        Integer cookie = this.mCookieList.size();
        this.mCookieList.add(graph);
        return this.mCookieList.indexOf(graph);
    }
}
