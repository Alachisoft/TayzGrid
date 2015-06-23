/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/

package com.alachisoft.tayzgrid.parser;

// C# Translation of GoldParser, by Marcus Klimstra <klimstra@home.nl>.
import java.lang.reflect.Array;
import java.util.List;

// Based on GOLDParser by Devin Cook <http://www.devincook.com/goldparser>.
/**
 */
public class TokenStack
{

    private java.util.ArrayList m_items;

    /*
     * constructor
     */
    /**
     */
    public TokenStack()
    {
        m_items = new java.util.ArrayList();
    }

    /*
     * indexer
     */
    /**
     * Returns the token at the specified position from the top.
     */
    public final Token getItem(int p_index)
    {
        return (Token) m_items.get(p_index);
    }

    /*
     * properties
     */
    /**
     * Gets the number of items in the stack.
     */
    public final int getCount()
    {
        return m_items.size();
    }

    /*
     * public methods
     */
    /**
     * Removes all tokens from the stack.
     */
    public final void Clear()
    {
        m_items.clear();
    }

    /**
     * Pushes the specified token on the stack.
     */
    public final void PushToken(Token p_token)
    {
        m_items.add(p_token);
    }

    /**
     * Returns the token on top of the stack.
     */
    public final Token PeekToken()
    {
        int last = m_items.size() - 1;
        return (last < 0 ? null : (Token) m_items.get(last));
    }

    /**
     * Pops a token from the stack. The token on top of the stack will be removed and returned by the method.
     */
    public final Token PopToken()
    {
        int last = m_items.size() - 1;
        if (last < 0)
        {
            return null;
        }
        Token result = (Token) m_items.get(last);
        m_items.remove(last);
        return result;
    }

    /**
     * Pops the specified number of tokens from the stack and adds them to the specified <c>Reduction</c>.
     */
    public final void PopTokensInto(Reduction p_reduction, int p_count)
    {
        int start = m_items.size() - p_count;
        int end = m_items.size();

        for (int i = start; i < end; i++)
        {
            p_reduction.AddToken((Token) m_items.get(i));
        }


        for (int i = 0; i < p_count ; i++)
        {
            m_items.remove(start);
        }

    }

    /**
     * Returns the token at the specified position from the top. <example>GetToken(0) returns the token on top off the stack, GetToken(1) the next one, etc.</example>
     */
    public final Token GetToken(int p_index)
    {
        return (Token) m_items.get(p_index);
    }

    /**
     * Returns an <c>IEnumerator</c> for the tokens on the stack.
     */
    public final java.util.Iterator GetEnumerator()
    {
        return m_items.iterator();
    }
}