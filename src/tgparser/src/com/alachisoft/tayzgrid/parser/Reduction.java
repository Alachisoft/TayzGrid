/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/

package com.alachisoft.tayzgrid.parser;

// C# Translation of GoldParser, by Marcus Klimstra <klimstra@home.nl>.
// Based on GOLDParser by Devin Cook <http://www.devincook.com/goldparser>.
/** This class is used by the engine to hold a reduced rule. Rather than contain
 a list of Symbols, a reduction contains a list of Tokens corresponding to the
 the rule it represents. This class is important since it is used to store the
 actual source program parsed by the Engine.
*/
public class Reduction {
	private java.util.ArrayList m_tokens;
	private Rule m_parentRule;
	private Object m_tag;

	/* constructor */

	/** Creates a new Reduction.
	*/
	public Reduction() {
		m_tokens = new java.util.ArrayList();
	}

	/* properties */

	/** Returns an <c>ArrayList</c> containing the <c>Token</c>s in this reduction.
	*/
	public final java.util.ArrayList getTokens() {
		return m_tokens;
	}

	/** Returns the <c>Rule</c> that this <c>Reduction</c> represents.
	*/
	public final Rule getParentRule() {
		return m_parentRule;
	}
	public final void setParentRule(Rule value) {
		m_parentRule = value;
	}

	/** This is a general purpose field that can be used at the developer's leisure.
	*/
	public final Object getTag() {
		return m_tag;
	}
	public final void setTag(Object value) {
		m_tag = value;
	}

	/* public methods */

	/** Returns the token with the specified index.
	*/
	public final Token GetToken(int p_index) {
		return (Token)m_tokens.get(p_index);
	}

	/** Returns a string-representation of this Reduction.
	*/
	@Override
	public String toString() {
		return m_parentRule.toString();
	}

	/** Makes the <c>IGoldVisitor</c> visit this <c>Reduction</c>.
	 <example>See the GoldTest sample project.</example>
	*/
	public final void Accept(IGoldVisitor p_visitor) {
		p_visitor.Visit(this);
	}

	/** Makes the <c>IGoldVisitor</c> visit the children of this
			  <c>Reduction</c>.
	 <example>See the GoldTest sample project.</example>
	*/
	public final void ChildrenAccept(IGoldVisitor p_visitor) {
            Token token;
                 Object tempVar=new Object();
		for (Object tokenObj : m_tokens) {
                    token=(Token)tokenObj;
			if (token.getKind() == SymbolType.NonTerminal)
				tempVar = token.getData(); {
				((Reduction)((tempVar instanceof Reduction) ? tempVar : null)).Accept(p_visitor);
				}
		}
	}

	/* internal methods */

	/**
	*/
	public final void AddToken(Token p_token) {
		m_tokens.add(p_token);
	}
}