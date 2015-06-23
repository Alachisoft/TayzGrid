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
package com.alachisoft.tayzgrid.common.datastructures;

import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;

/**
 * The RedBlackNode class encapsulates a node in the tree
 */
public class RedBlackNode implements ISizableIndex {

    /**
     * Tree node color.
     */
    public static final int RED = 0;
    /**
     * Tree node color.
     */
    public static final int BLACK = 1;

    /**
     * key provided by the calling class.
     */
    private java.lang.Comparable _key;

    /**
     * the data or value associated with the key.
     */
    private java.util.HashMap _value;

    /**
     * color - used to balance the tree.
     */
    private int _color;

    /**
     * Max Count of values in Hashtable
     */
    private long _maxItemCount;

    /**
     * Left node.
     */
    private RedBlackNode _leftNode;

    /**
     * Right node.
     */
    private RedBlackNode _rightNode;

    /**
     * Parent node.
     */
    private RedBlackNode _parentNode;
    private RedBlackNodeReference _rbReference;

    /**
     * Default constructor.
     */
    public RedBlackNode() {
        setColor(RED);
        setData(new java.util.HashMap());
        _rbReference = new RedBlackNodeReference(this);
    }

    /**
     * Key
     */
    public final java.lang.Comparable getKey() {
        return _key;
    }

    public final void setKey(java.lang.Comparable value) {
        _key = value;
    }

    /**
     * Data
     */
    public final java.util.HashMap getData() {
        return _value;
    }

    public final void setData(java.util.HashMap value) {
        _value = value;
    }

    /**
     * Color
     */
    public final int getColor() {
        return _color;
    }

    public final void setColor(int value) {
        _color = value;
    }

    /**
     * Left
     */
    public final RedBlackNode getLeft() {
        return _leftNode;
    }

    public final void setLeft(RedBlackNode value) {
        _leftNode = value;
    }

    /**
     * Right
     */
    public final RedBlackNode getRight() {
        return _rightNode;
    }

    public final void setRight(RedBlackNode value) {
        _rightNode = value;
    }

    /**
     * Parent node
     */
    public final RedBlackNode getParent() {
        return _parentNode;
    }

    public final void setParent(RedBlackNode value) {
        _parentNode = value;
    }

    public final RedBlackNodeReference getRBNodeReference() {
        return _rbReference;
    }

    public final void setRBNodeReference(RedBlackNodeReference value) {
        _rbReference = value;
    }

    public void insert(Object key, Object value)
    {
        getData().put(key, null);
        if(getData().size() > _maxItemCount)
            _maxItemCount = getData().size();
    }
    
    @Override
    public long getIndexInMemorySize() {
        return _maxItemCount * MemoryUtil.NetHashtableOverHead;
    }

}
