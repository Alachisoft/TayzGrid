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

/**
 * The RedBlackEnumerator class returns the keys or data objects of the treap in sorted order.
 * Iterator will also return keys and values separately
 */
public class RedBlackEnumerator implements java.util.Iterator
{
    // the treap uses the stack to order the nodes

    private java.util.Stack stack;
    // return the keys
    // return in ascending order (true) or descending (false)
    private boolean ascending;
    // key
    private java.lang.Comparable ordKey;
    // the data or value associated with the key
    private Object objValue;
    private RedBlackNode _sentinelNode;


    //<editor-fold defaultstate="collapsed" desc="/  --- IDictionary Enumerator & some more --- /">
    public Object getKey()
    {
        return ordKey;
    }

    public Object getValue()
    {
        return objValue;
    }

    // Not being used

    /**
     * Functions as it should, returns if next value exists
     * @return true if it has next value
     */

    @Override
    public boolean hasNext()
    {
        if (HasMoreElements())
        {
            try{
                NextElement();
            }catch(Exception ex){
            }
            return true;
        }
        return false;
    }

    /**
     * MoveNext For .NET compatibility
     * this will move the element forward BUT will populate the key and value information
     * @return boolean that Only represents hasNext function
     */
    @Override
    public Object next()
    {
            return objValue;
    }

    /**
     * MoveNext For .NET compatibility
     * this will move the element forward BUT will populate the key and value information
     * @return true if next value exists and populates the getKey and getValue functions
     */
    public boolean MoveNext()
    {
        if (HasMoreElements())
        {
            try{
                NextElement();
            }catch(Exception ex){
            }
            return true;
        }
        return false;
    }

    /**
     * Not implemented since it this did not exist in .Net
     */
    @Override
    public void remove()
    {

    }

    //</editor-fold>

    private Object getCurrent()
    {
        return null;
    }

    private void Reset()
    {
    }

    public RedBlackEnumerator()
    {
    }

    /**
     * Determine order, walk the tree and push the nodes onto the stack
     */
    public RedBlackEnumerator(RedBlackNode tnode, boolean ascending, RedBlackNode sentinelNode)
    {

        stack = new java.util.Stack();
        this.ascending = ascending;
        _sentinelNode = sentinelNode;

        // use depth-first traversal to push nodes into stack
        // the lowest node will be at the top of the stack
        if (ascending)
        { // find the lowest node
            while (tnode != _sentinelNode)
            {
                stack.push(tnode);
                tnode = tnode.getLeft();
            }
        }
        else
        {
            // the highest node will be at top of stack
            while (tnode != _sentinelNode)
            {
                stack.push(tnode);
                tnode = tnode.getRight();
            }
        }

    }

    /**
     * HasMoreElements
     */
    public final boolean HasMoreElements()
    {
        boolean result = stack != null && stack.size() > 0;
        return result;
    }

    /**
     * NextElement
     */
    public final Object NextElement()throws RedBlackException
    {
        if (stack.empty())
        {
            throw (new RedBlackException("Element not found"));
        }

        // the top of stack will always have the next item
        // get top of stack but don't remove it as the next nodes in sequence
        // may be pushed onto the top
        // the stack will be popped after all the nodes have been returned
        RedBlackNode node = (RedBlackNode) stack.peek(); //next node in sequence

        if (ascending)
        {
            if (node.getRight() == _sentinelNode)
            {
                // yes, top node is lowest node in subtree - pop node off stack
                RedBlackNode tn = (RedBlackNode) stack.pop();
                // peek at right node's parent
                // get rid of it if it has already been used
                while (HasMoreElements() && ((RedBlackNode) stack.peek()).getRight() == tn)
                {
                    tn = (RedBlackNode) stack.pop();
                }
            }
            else
            {
                // find the next items in the sequence
                // traverse to left; find lowest and push onto stack
                RedBlackNode tn = node.getRight();
                while (tn != _sentinelNode)
                {
                    stack.push(tn);
                    tn = tn.getLeft();
                }
            }
        }
        else
        { // descending, same comments as above apply
            if (node.getLeft() == _sentinelNode)
            {
                // walk the tree
                RedBlackNode tn = (RedBlackNode) stack.pop();
                while (HasMoreElements() && ((RedBlackNode) stack.peek()).getLeft() == tn)
                {
                    tn = (RedBlackNode) stack.pop();
                }
            }
            else
            {
                // determine next node in sequence
                // traverse to left subtree and find greatest node - push onto stack
                RedBlackNode tn = node.getLeft();
                while (tn != _sentinelNode)
                {
                    stack.push(tn);
                    tn = tn.getRight();
                }
            }
        }

        // the following is for .NET compatibility (see MoveNext())
        ordKey = node.getKey();
        objValue = node.getData();
        return node.getKey();
    }


}
