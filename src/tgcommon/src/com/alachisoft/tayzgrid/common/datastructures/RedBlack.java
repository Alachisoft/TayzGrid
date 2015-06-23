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
import com.alachisoft.tayzgrid.common.util.WildcardEnabledRegex;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A red-black tree must satisfy these properties:
 *
 * 1. The root is black. 2. All leaves are black. 3. Red nodes can only have black children. 4. All paths from a node to its leaves contain the same number of black nodes.
 */
public class RedBlack implements ISizableIndex
{

    //REGEX is the comparison based on the regular expression.
    //It is used for LIKE type comparisons.
    //IREGEX is the inverse comparison based on the regular expression.
    //It is used for NOT LIKE type of comparisons.

    public enum COMPARE
    {

        EQ,
        NE,
        LT,
        GT,
        LTEQ,
        GTEQ,
        REGEX,
        IREGEX;

        public int getValue()
        {
            return this.ordinal();
        }

        public static COMPARE forValue(int value)
        {
            return values()[value];
        }
    }
    
    
    public enum AttributeTypeSize
    {
        Variable,
        Byte1,
        Byte2,
        Byte4,
        Byte8,
        Byte16,
    }
    // the number of nodes contained in the tree
    private int intCount;
    // a simple randomized hash code. The hash code could be used as a key
    // if it is "unique" enough. Note: The IComparable interface would need to
    // be replaced with int.
    private RedBlackNode rbTree;
    //  sentinelNode is convenient way of indicating a leaf node.
    private RedBlackNode _sentinelNode = new RedBlackNode();
    // the node that was last found; used to optimize searches
    private RedBlackNode lastNodeFound;
   //used for logging
    private String _cacheName;
    
    // Tree Type
    private AttributeTypeSize _typeSize;
    //Tree Node Keys Size
    private long _rbNodeKeySize;
    // Tree Data Size
    private long _rbNodeDataSize;

    public RedBlack()
    {
        _sentinelNode.setRight(_sentinelNode);
        _sentinelNode.setLeft(_sentinelNode.getRight());
        _sentinelNode.setParent(null);
        _sentinelNode.setColor(RedBlackNode.BLACK);
        rbTree = _sentinelNode;
        lastNodeFound = _sentinelNode;
    }

    public RedBlack(String cacheName)
    {
        this();
        _cacheName = cacheName;
    }
    
    public RedBlack(String cacheName, AttributeTypeSize size)
    {
        this();
        _cacheName = cacheName;
        _typeSize  = size;
    }

    public final RedBlackNode getSentinelNode()
    {
        return _sentinelNode;
    }

    /**
     * Add args: ByVal key As IComparable, ByVal data As Object key is object that implements IComparable interface performance tip: change to use use int type (such as the
     * hashcode)
     */
    public final Object Add(java.lang.Comparable key, Object data) throws RedBlackException
    {
        boolean collision = false;
        RedBlackNodeReference keyNodeRfrnce = null;
        try
        {
            if (key == null || data == null)
            {
                throw new RedBlackException("RedBlackNode key and data must not be null");
            }

            // traverse tree - find where node belongs
            int result = 0;
            // create new node
            RedBlackNode node = new RedBlackNode();
            RedBlackNode temp = rbTree; // grab the rbTree node of the tree

            while (temp != _sentinelNode)
            { // find Parent
                node.setParent(temp);
                
                if (key instanceof String)
                {
                    result = key.toString().toLowerCase().compareTo(temp.getKey().toString().toLowerCase());
                }
                else
                {
                    result = key.compareTo(temp.getKey());
                }
                    
                if (result == 0)
                {
                    collision = true; //data with the same key.
                    break;
                }
                if (result > 0)
                {
                    temp = temp.getRight();
                    collision = false;
                }
                else
                {
                    temp = temp.getLeft();
                    collision = false;
                }
            }

            if (collision)
            {
                long prevSize = temp.getIndexInMemorySize();
                temp.insert(data, null);// .Data[data] = null;
                keyNodeRfrnce = temp.getRBNodeReference();
                
                _rbNodeDataSize += temp.getIndexInMemorySize() - prevSize;
            }
            else
            {
                // setup node
                node.setKey(key);
                node.insert(data, null);//.Data.Add(data,null);
                node.setLeft(_sentinelNode);
                node.setRight(_sentinelNode);

                if (_typeSize != AttributeTypeSize.Variable)
                    _rbNodeKeySize += MemoryUtil.getTypeSize(_typeSize);
                else
                    _rbNodeKeySize += MemoryUtil.getStringSize(key);

                _rbNodeDataSize += node.getIndexInMemorySize();
                
                // insert node into tree starting at parent's location
                if (node.getParent() != null)
                {
                    if (key instanceof String)
                    {
                        result = node.getKey().toString().toLowerCase().compareTo(node.getParent().getKey().toString().toLowerCase());
                    }
                    else
                    {
                        result = node.getKey().compareTo(node.getParent().getKey());
                    }
                    
                    //result = node.getKey().compareTo(node.getParent().getKey());
                    if (result > 0)
                    {
                        node.getParent().setRight(node);
                    }
                    else
                    {
                        node.getParent().setLeft(node);
                    }
                }
                else
                {
                    rbTree = node; // first node added
                }

                RestoreAfterInsert(node); // restore red-black properities

                lastNodeFound = node;

                intCount = intCount + 1;
                keyNodeRfrnce = node.getRBNodeReference();
            }
        }
        catch (Exception ex)
        {
        }
        
        return keyNodeRfrnce;
    }

    /**
     * RestoreAfterInsert Additions to red-black trees usually destroy the red-black properties. Examine the tree and restore. Rotations are normally required to restore it
     */
    private void RestoreAfterInsert(RedBlackNode x)
    {
        // x and y are used as variable names for brevity, in a more formal
        // implementation, you should probably change the names

        RedBlackNode y;

        // maintain red-black tree properties after adding x
        while (x != rbTree && x.getParent().getColor() == RedBlackNode.RED)
        {
            // Parent node is .Colored red;
            if (x.getParent() == x.getParent().getParent().getLeft()) // determine traversal path
            { // is it on the Left or Right subtree?
                y = x.getParent().getParent().getRight(); // get uncle
                if (y != null && y.getColor() == RedBlackNode.RED)
                { // uncle is red; change x's Parent and uncle to black
                    x.getParent().setColor(RedBlackNode.BLACK);
                    y.setColor(RedBlackNode.BLACK);
                    // grandparent must be red. Why? Every red node that is not
                    // a leaf has only black children
                    x.getParent().getParent().setColor(RedBlackNode.RED);
                    x = x.getParent().getParent(); // continue loop with grandparent
                }
                else
                {
                    // uncle is black; determine if x is greater than Parent
                    if (x == x.getParent().getRight())
                    { // yes, x is greater than Parent; rotate Left
                        // make x a Left child
                        x = x.getParent();
                        RotateLeft(x);
                    }
                    // no, x is less than Parent
                    x.getParent().setColor(RedBlackNode.BLACK); // make Parent black
                    x.getParent().getParent().setColor(RedBlackNode.RED); // make grandparent black
                    RotateRight(x.getParent().getParent()); // rotate right
                }
            }
            else
            { // x's Parent is on the Right subtree
                // this code is the same as above with "Left" and "Right" swapped
                y = x.getParent().getParent().getLeft();
                if (y != null && y.getColor() == RedBlackNode.RED)
                {
                    x.getParent().setColor(RedBlackNode.BLACK);
                    y.setColor(RedBlackNode.BLACK);
                    x.getParent().getParent().setColor(RedBlackNode.RED);
                    x = x.getParent().getParent();
                }
                else
                {
                    if (x == x.getParent().getLeft())
                    {
                        x = x.getParent();
                        RotateRight(x);
                    }
                    x.getParent().setColor(RedBlackNode.BLACK);
                    x.getParent().getParent().setColor(RedBlackNode.RED);
                    RotateLeft(x.getParent().getParent());
                }
            }
        }
        rbTree.setColor(RedBlackNode.BLACK); // rbTree should always be black
    }

    /**
     * RotateLeft Rebalance the tree by rotating the nodes to the left
     */
    public final void RotateLeft(RedBlackNode x)
    {
        // pushing node x down and to the Left to balance the tree. x's Right child (y)
        // replaces x (since y > x), and y's Left child becomes x's Right child
        // (since it's < y but > x).

        RedBlackNode y = x.getRight(); // get x's Right node, this becomes y

        // set x's Right link
        x.setRight(y.getLeft()); // y's Left child's becomes x's Right child

        // modify parents
        if (y.getLeft() != _sentinelNode)
        {
            y.getLeft().setParent(x); // sets y's Left Parent to x
        }

        if (y != _sentinelNode)
        {
            y.setParent(x.getParent()); // set y's Parent to x's Parent
        }

        if (x.getParent() != null)
        { // determine which side of it's Parent x was on
            if (x == x.getParent().getLeft())
            {
                x.getParent().setLeft(y); // set Left Parent to y
            }
            else
            {
                x.getParent().setRight(y); // set Right Parent to y
            }
        }
        else
        {
            rbTree = y; // at rbTree, set it to y
        }

        // link x and y
        y.setLeft(x); // put x on y's Left
        if (x != _sentinelNode)
        { // set y as x's Parent
            x.setParent(y);
        }
    }

    /**
     * RotateRight Rebalance the tree by rotating the nodes to the right
     */
    public final void RotateRight(RedBlackNode x)
    {
        // pushing node x down and to the Right to balance the tree. x's Left child (y)
        // replaces x (since x < y), and y's Right child becomes x's Left child
        // (since it's < x but > y).

        RedBlackNode y = x.getLeft(); // get x's Left node, this becomes y

        // set x's Right link
        x.setLeft(y.getRight()); // y's Right child becomes x's Left child

        // modify parents
        if (y.getRight() != _sentinelNode)
        {
            y.getRight().setParent(x); // sets y's Right Parent to x
        }

        if (y != _sentinelNode)
        {
            y.setParent(x.getParent()); // set y's Parent to x's Parent
        }

        if (x.getParent() != null) // null=rbTree, could also have used rbTree
        { // determine which side of it's Parent x was on
            if (x == x.getParent().getRight())
            {
                x.getParent().setRight(y); // set Right Parent to y
            }
            else
            {
                x.getParent().setLeft(y); // set Left Parent to y
            }
        }
        else
        {
            rbTree = y; // at rbTree, set it to y
        }

        // link x and y
        y.setRight(x); // put x on y's Right
        if (x != _sentinelNode)
        { // set y as x's Parent
            x.setParent(y);
        }
    }

    /**
     * GetData Gets the data object associated with the specified key
     *
     */
    public final Object GetData(java.lang.Comparable key, COMPARE compareType)
    {
        int result;
        java.util.ArrayList keyList = new java.util.ArrayList();
        RedBlackNode treeNode = rbTree; // begin at root
        RedBlackEnumerator en = this.GetEnumerator();
        String pattern;
        Matcher matcher;
        Pattern regex;
        java.util.HashMap finalTable = null;
        java.util.HashMap skippedKeys = null;

        boolean isStringValue = false;

        if (key instanceof String)
            isStringValue = true;
        
        switch (compareType)
        {
            case EQ:
                // traverse tree until node is found
                while (treeNode != _sentinelNode)
                { 
                    if (isStringValue && treeNode.getKey() instanceof String)
                    {
                        result = treeNode.getKey().toString().toLowerCase().compareTo(key.toString().toLowerCase());
                    }
                    else
                        result = treeNode.getKey().compareTo(key);
                    
                    if (result == 0)
                    {
                        lastNodeFound = treeNode;
                        keyList.addAll(treeNode.getData().keySet());
                        return keyList;
                    }
                    if (result > 0)
                    { //treenode is Greater then the one we are looking. Move to Left branch
                        treeNode = treeNode.getLeft();
                    }
                    else
                    {
                        treeNode = treeNode.getRight(); //treenode is Less then the one we are looking. Move to Right branch.
                    }
                }
                break;

            case NE:
                // traverse tree until node is found
                finalTable = new java.util.HashMap();

                while (en.MoveNext())
                {
                    if (isStringValue && en.getKey() instanceof String)							
                    {
                        result = ((java.lang.Comparable)en.getKey()).toString().toLowerCase().compareTo(key.toString().toLowerCase());
                    }
                    else
                    {
                        result = ((java.lang.Comparable)en.getKey()).compareTo(key);
                    }

                    if (result != 0)
                    {
                        java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                        Iterator ide = tmp.entrySet().iterator();

                        while (ide.hasNext())
                        {
                            Map.Entry pair = (Map.Entry) ide.next();
                            finalTable.put(pair.getKey(), pair.getValue());
                        }
                    }
                }

                return new java.util.ArrayList(finalTable.keySet()); //keyList;

            case GT:
                finalTable = new java.util.HashMap();
                while (en.MoveNext())
                {
                    if (isStringValue && en.getKey() instanceof String)
                    {
                        result = ((java.lang.Comparable)en.getKey()).toString().toLowerCase().compareTo(key.toString().toLowerCase());
                    }
                    else
                    {
                        result = ((java.lang.Comparable)en.getKey()).compareTo(key);
                    }

                    if (result > 0)
                    {
                        java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                        Iterator ide = tmp.entrySet().iterator();

                        while (ide.hasNext())
                        {
                            Map.Entry pair = (Map.Entry) ide.next();
                            finalTable.put(pair.getKey(), pair.getValue());
                        }
                    }
                }

                return new java.util.ArrayList(finalTable.keySet()); //keyList;

            case LT:
                finalTable = new java.util.HashMap();
                while (en.MoveNext())
                {
                    if (isStringValue && en.getKey() instanceof String)
                    {
                        result = ((java.lang.Comparable)en.getKey()).toString().toLowerCase().compareTo(key.toString().toLowerCase());
                    }
                    else
                    {
                        result = ((java.lang.Comparable)en.getKey()).compareTo(key);
                    }

                    if (result < 0)
                    {
                        java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                        Iterator ide = tmp.entrySet().iterator();
                        while (ide.hasNext())
                        {
                            Map.Entry pair = (Map.Entry) ide.next();
                            finalTable.put(pair.getKey(), pair.getValue());
                        }

                    }
                }

                return new java.util.ArrayList(finalTable.keySet());

            case GTEQ:
                finalTable = new java.util.HashMap();

                while (en.MoveNext())
                {
                    if (isStringValue && en.getKey() instanceof String)
                    {
                        result = ((java.lang.Comparable)en.getKey()).toString().toLowerCase().compareTo(key.toString().toLowerCase());
                    }
                    else
                    {
                        result = ((java.lang.Comparable)en.getKey()).compareTo(key);
                    }

                    if (result >= 0)
                    {
                        java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                        Iterator ide = tmp.entrySet().iterator();
                        while (ide.hasNext())
                        {
                            Map.Entry pair = (Map.Entry) ide.next();
                            finalTable.put(pair.getKey(), pair.getValue());
                        }

                    }
                }

                return new java.util.ArrayList(finalTable.keySet()); //keyList;

            case LTEQ:
                finalTable = new java.util.HashMap();
                while (en.MoveNext())
                {
                    if (isStringValue && en.getKey() instanceof String)
                    {
                        result = ((java.lang.Comparable)en.getKey()).toString().toLowerCase().compareTo(key.toString().toLowerCase());
                    }
                    else
                    {
                        result = ((java.lang.Comparable)en.getKey()).compareTo(key);
                    }

                    if (result <= 0)
                    {
                        java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                        Iterator ide = tmp.entrySet().iterator();
                        while (ide.hasNext())
                        {
                            Map.Entry pair = (Map.Entry) ide.next();
                            finalTable.put(pair.getKey(), pair.getValue());
                        }

                    }
                    else
                    {
                        break;
                    }
                }

                return new java.util.ArrayList(finalTable.keySet()); 

            case REGEX:
                finalTable = new java.util.HashMap();
                pattern = (String) ((key instanceof String) ? key : null);
                //converting pattern
                String ptrn = WildcardEnabledRegex.ConvertWildCard(pattern);
                regex = Pattern.compile(ptrn);

                while (en.MoveNext())
                {
                    if (en.getKey() instanceof String)
                    {
                        matcher = regex.matcher((String) en.getKey().toString().toLowerCase());
                        if (matcher.matches())
                        {
                            java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                            Iterator ide = tmp.entrySet().iterator();

                            while (ide.hasNext())
                            {
                                Map.Entry pair = (Map.Entry) ide.next();
                                finalTable.put(pair.getKey(), pair.getValue());
                            }
                        }
                    }
                }

                return new java.util.ArrayList(finalTable.keySet());

            case IREGEX:
                finalTable = new java.util.HashMap();
                pattern = (String) ((key instanceof String) ? key : null);
                String irPatrn = WildcardEnabledRegex.ConvertWildCard(pattern);
                regex = Pattern.compile(irPatrn);
                skippedKeys = new java.util.HashMap();
                while (en.MoveNext())
                {
                    if (en.getKey() instanceof String)
                    {
                        matcher = regex.matcher((String) en.getKey().toString().toLowerCase());
                        if (matcher.matches())
                        {
                            java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                            Iterator ide = tmp.entrySet().iterator();
                            while (ide.hasNext())
                            {
                                Map.Entry pair = (Map.Entry) ide.next();
                                skippedKeys.put(pair.getKey(), pair.getValue());
                            }
                        }
                        else
                        {
                            java.util.HashMap tmp = (java.util.HashMap) ((en.getValue() instanceof java.util.HashMap) ? en.getValue() : null);
                            Iterator ide = tmp.entrySet().iterator();
                            while (ide.hasNext())
                            {
                                Map.Entry pair = (Map.Entry) ide.next();
                                finalTable.put(pair.getKey(), pair.getValue());
                            }
                        }
                    }
                }

                java.util.ArrayList list = new java.util.ArrayList(finalTable.keySet()); // keyList;

                for (int idx = list.size() - 1; idx >= 0; idx--)
                {
                    if (skippedKeys.containsKey(list.get(idx)))
                    {
                        list.remove(idx);
                    }
                }

                return list;
        }

        return keyList;
    }

    /**
     * return true if a specifeid key exists
     *
     * @param key
     * @return
     */
    public final boolean Contains(java.lang.Comparable key)
    {
        int result;
        RedBlackNode treeNode = rbTree; // begin at root

        // traverse tree until node is found
        while (treeNode != _sentinelNode)
        {
            result = treeNode.getKey().compareTo(key);
            if (result == 0)
            {
                lastNodeFound = treeNode;
                //return treeNode.Data;
                return true;
            }

            if (result > 0)
            { //treenode is Greater then the one we are looking. Move to Left branch
                treeNode = treeNode.getLeft();
            }
            else
            {
                treeNode = treeNode.getRight(); //treenode is Less then the one we are looking. Move to Right branch.
            }
        }
        return false;
    }

    /**
     * GetMinKey Returns the minimum key value
     *
     * @return
     */
    public final java.lang.Comparable getMinKey()
    {
        RedBlackNode treeNode = rbTree;
        if (treeNode == null || treeNode == _sentinelNode)
        {
            return null;
        }

        // traverse to the extreme left to find the smallest key
        while (treeNode.getLeft() != _sentinelNode)
        {
            treeNode = treeNode.getLeft();
        }

        lastNodeFound = treeNode;
        return treeNode.getKey();
    }

    /**
     * GetMaxKey Returns the maximum key value
     *
     * @return
     * @throws RedBlackException
     */
    public final java.lang.Comparable getMaxKey() throws RedBlackException
    {
        RedBlackNode treeNode = rbTree;
        if (treeNode == null || treeNode == _sentinelNode)
        {
            throw (new RedBlackException("RedBlack tree is empty"));
        }

        // traverse to the extreme right to find the largest key
        while (treeNode.getRight() != _sentinelNode)
        {
            treeNode = treeNode.getRight();
        }

        lastNodeFound = treeNode;
        return treeNode.getKey();
    }

    /*
     * ///<summary> /// GetMinValue /// Returns the object having the minimum key value ///<summary> public object MinValue { get { return GetData(MinKey); } }
     *
     * ///<summary> /// GetMaxValue /// Returns the object having the maximum key ///<summary> public object MaxValue { get { return GetData(MaxKey); } }
     */
    /**
     * GetEnumerator return an enumerator that returns the tree nodes in order
     *
     * @return
     */
    public final RedBlackEnumerator GetEnumerator()
    {
        // elements is simply a generic name to refer to the
        // data objects the nodes contain
        return Elements(true);
    }

    /**
     * Keys if(ascending is true, the keys will be returned in ascending order, else the keys will be returned in descending order.
     *
     */
    public final RedBlackEnumerator Keys()
    {
        return Keys(true);
    }

    public final RedBlackEnumerator Keys(boolean ascending)
    {
        return new RedBlackEnumerator(rbTree, ascending, _sentinelNode);
    }

    /*
     * ///<summary> /// Values /// Provided for .NET compatibility. ///<summary> public RedBlackEnumerator Values() { return Elements(true); }
     */
    /**
     * Elements Returns an enumeration of the data objects. if(ascending is true, the objects will be returned in ascending order, else the objects will be returned in descending
     * order.
     *
     * @return
     */
    public final RedBlackEnumerator Elements()
    {
        return Elements(true);
    }

    public final RedBlackEnumerator Elements(boolean ascending)
    {
        return new RedBlackEnumerator(rbTree, ascending, _sentinelNode);
    }

    /**
     * IsEmpty Is the tree empty?
     *
     */
    public final boolean getIsEmpty()
    {
        return (rbTree == null);
    }

    public final void Remove(Object indexKey) throws RedBlackException
    {
        Remove(indexKey, null);
    }

    /**
     Remove
     removes the key and data object (delete)

    */
    public final boolean Remove(Object cacheKey, Object node)
    {
        boolean isNodeRemoved = false;
        RedBlackNodeReference keyNodeReference = (RedBlackNodeReference)node;
        RedBlackNode keyNode = keyNodeReference.getRBReference();
        try
        {
            if (cacheKey != null && keyNode.getData().size() > 1)
            {
                if (keyNode.getData().containsKey(cacheKey))
                {
                        keyNode.getData().remove(cacheKey);
                        isNodeRemoved = false;
                }
            }
            else
            {
                if (_typeSize != AttributeTypeSize.Variable)
                        _rbNodeKeySize -= MemoryUtil.getTypeSize(_typeSize);
                    else
                        _rbNodeKeySize -= MemoryUtil.getStringSize(keyNode.getKey());

                    _rbNodeDataSize -= keyNode.getIndexInMemorySize();
                Delete(keyNode);
                isNodeRemoved = true;
            }

        }
        catch (RuntimeException e)
        {
                throw e;
        }

        if (isNodeRemoved)
        {
                intCount = intCount - 1;
        }

        return isNodeRemoved;
    }
    
    /**
     * Remove removes the key and data object (delete)
     *
     * @param indexKey
     * @param cacheKey
     * @throws RedBlackException
     */
    public final void Remove(java.lang.Comparable indexKey, Object cacheKey) throws RedBlackException
    {
        boolean isNodeRemoved = false;
        if (indexKey == null)
        {
            throw (new RedBlackException("RedBlackNode key is null"));
        }

        try
        {
            // find node
            int result;
            RedBlackNode node;

            // see if node to be deleted was the last one found
            //replacing .net equivalent (object.CompareTo(null)==1) in java.
            
            if (indexKey instanceof String)
            {
                result = indexKey.toString().toLowerCase().compareTo(lastNodeFound.getKey().toString().toLowerCase());
            }
            else
            {
                result = indexKey.compareTo(lastNodeFound.getKey());
            }
            

            if (result == 0)
            {
                node = lastNodeFound;
            }
            else
            { // not found, must search
                node = rbTree;
                while (node != _sentinelNode)
                {
                    if (indexKey instanceof String)
                    {
                        result = indexKey.toString().toLowerCase().compareTo(node.getKey().toString().toLowerCase());
                    }
                    else
                        result = indexKey.compareTo(node.getKey());
                    
                    if (result == 0)
                    {
                        break;
                    }
                    if (result < 0)
                    {
                        node = node.getLeft();
                    }
                    else
                    {
                        node = node.getRight();
                    }
                }

                if (node == _sentinelNode)
                {
                    return; // key not found
                }
            }

            try
            {
                if (cacheKey != null && node.getData().size() > 1)
                {
                    if (node.getData().containsKey(cacheKey))
                    {
                        node.getData().remove(cacheKey);
                        isNodeRemoved = false;
                    }
                }
                else
                {
                    if (_typeSize != AttributeTypeSize.Variable)
                        _rbNodeKeySize -= MemoryUtil.getTypeSize(_typeSize);
                    else
                        _rbNodeKeySize -= MemoryUtil.getStringSize(node.getKey());

                    _rbNodeDataSize -= node.getIndexInMemorySize();
                    Delete(node);
                    isNodeRemoved = true;
                }
            }
            catch (Exception e)
            {
                return;
            }
        }
        catch (RuntimeException e2)
        {
            throw e2;
        }

        if (isNodeRemoved)
        {
            intCount = intCount - 1;
        }
    }

    /**
     * Delete Delete a node from the tree and restore red black properties
     *
     */
    private void Delete(RedBlackNode z)
    {
        // A node to be deleted will be:
        //		1. a leaf with no children
        //		2. have one child
        //		3. have two children
        // If the deleted node is red, the red black properties still hold.
        // If the deleted node is black, the tree needs rebalancing

        RedBlackNode x = new RedBlackNode(); // work node to contain the replacement node
        RedBlackNode y; // work node

        // find the replacement node (the successor to x) - the node one with
        // at *most* one child.
        if (z.getLeft() == _sentinelNode || z.getRight() == _sentinelNode)
        {
            y = z; // node has sentinel as a child
        }
        else
        {
            // z has two children, find replacement node which will
            // be the leftmost node greater than z
            y = z.getRight(); // traverse right subtree
            while (y.getLeft() != _sentinelNode)
            { // to find next node in sequence
                y = y.getLeft();
            }
        }

        // at this point, y contains the replacement node. it's content will be copied
        // to the valules in the node to be deleted

        // x (y's only child) is the node that will be linked to y's old parent.
        if (y.getLeft() != _sentinelNode)
        {
            x = y.getLeft();
        }
        else
        {
            x = y.getRight();
        }

        // replace x's parent with y's parent and
        // link x to proper subtree in parent
        // this removes y from the chain
        x.setParent(y.getParent());
        if (y.getParent() != null)
        {
            if (y == y.getParent().getLeft())
            {
                y.getParent().setLeft(x);
            }
            else
            {
                y.getParent().setRight(x);
            }
        }
        else
        {
            rbTree = x; // make x the root node
        }

        // copy the values from y (the replacement node) to the node being deleted.
        // note: this effectively deletes the node.
        if (y != z)
        {
            z.setKey(y.getKey());
            z.setData(y.getData()); 
            
            z.setRBNodeReference(y.getRBNodeReference());
            z.getRBNodeReference().setRBReference(z);
        }

        if (y.getColor() == RedBlackNode.BLACK)
        {
            RestoreAfterDelete(x);
        }

        lastNodeFound = _sentinelNode;
    }

    /**
     * RestoreAfterDelete Deletions from red-black trees may destroy the red-black properties. Examine the tree and restore. Rotations are normally required to restore it
     */
    private void RestoreAfterDelete(RedBlackNode x)
    {
        // maintain Red-Black tree balance after deleting node

        RedBlackNode y;

        while (x != rbTree && x.getColor() == RedBlackNode.BLACK)
        {
            if (x == x.getParent().getLeft())
            { // determine sub tree from parent
                y = x.getParent().getRight(); // y is x's sibling
                if (y.getColor() == RedBlackNode.RED)
                { // x is black, y is red - make both black and rotate
                    y.setColor(RedBlackNode.BLACK);
                    x.getParent().setColor(RedBlackNode.RED);
                    RotateLeft(x.getParent());
                    y = x.getParent().getRight();
                }
                if (y.getLeft().getColor() == RedBlackNode.BLACK && y.getRight().getColor() == RedBlackNode.BLACK)
                { // children are both black
                    y.setColor(RedBlackNode.RED); // change parent to red
                    x = x.getParent(); // move up the tree
                }
                else
                {
                    if (y.getRight().getColor() == RedBlackNode.BLACK)
                    {
                        y.getLeft().setColor(RedBlackNode.BLACK);
                        y.setColor(RedBlackNode.RED);
                        RotateRight(y);
                        y = x.getParent().getRight();
                    }
                    y.setColor(x.getParent().getColor());
                    x.getParent().setColor(RedBlackNode.BLACK);
                    y.getRight().setColor(RedBlackNode.BLACK);
                    RotateLeft(x.getParent());
                    x = rbTree;
                }
            }
            else
            { // right subtree - same as code above with right and left swapped
                y = x.getParent().getLeft();
                if (y.getColor() == RedBlackNode.RED)
                {
                    y.setColor(RedBlackNode.BLACK);
                    x.getParent().setColor(RedBlackNode.RED);
                    RotateRight(x.getParent());
                    y = x.getParent().getLeft();
                }
                if (y.getRight().getColor() == RedBlackNode.BLACK && y.getLeft().getColor() == RedBlackNode.BLACK)
                {
                    y.setColor(RedBlackNode.RED);
                    x = x.getParent();
                }
                else
                {
                    if (y.getLeft().getColor() == RedBlackNode.BLACK)
                    {
                        y.getRight().setColor(RedBlackNode.BLACK);
                        y.setColor(RedBlackNode.RED);
                        RotateLeft(y);
                        y = x.getParent().getLeft();
                    }
                    y.setColor(x.getParent().getColor());
                    x.getParent().setColor(RedBlackNode.BLACK);
                    y.getLeft().setColor(RedBlackNode.BLACK);
                    RotateRight(x.getParent());
                    x = rbTree;
                }
            }
        }
        x.setColor(RedBlackNode.BLACK);
    }

    /**
     * RemoveMin removes the node with the minimum key
     *
     */
    public final void RemoveMin() throws RedBlackException
    {
        if (rbTree == null)
        {
            throw (new RedBlackException("RedBlackNode is null"));
        }

        Remove(getMinKey());
    }

    /**
     * RemoveMax removes the node with the maximum key
     *
     */
    public final void RemoveMax() throws RedBlackException
    {
        if (rbTree == null)
        {
            throw (new RedBlackException("RedBlackNode is null"));
        }

        Remove(getMaxKey());
    }

    /**
     * Clear Empties or clears the tree
     *
     */
    public final void Clear()
    {
        rbTree = _sentinelNode;
        intCount = 0;
        
        _rbNodeDataSize = 0;
        _rbNodeKeySize = 0;
    }

    /**
     * Size returns the size (number of nodes) in the tree
     *
     */
    // number of keys
    public final int getCount()
    {
        return intCount;
    }

    /**
     * Equals
     *
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }

        if (!(obj instanceof RedBlackNode))
        {
            return false;
        }

        if (this == obj)
        {
            return true;
        }

        return (toString().equals(((RedBlackNode) (obj)).toString()));

    }

    /**
     * HashCode
     *
     */
    @Override
    public int hashCode()
    {
        return 0;
    }

    /**
     * ToString
     *
     */
    @Override
    public String toString()
    {
        return "";
    }
    
    @Override
    public long getIndexInMemorySize() {
        return _rbNodeKeySize + _rbNodeDataSize;
    }
}
