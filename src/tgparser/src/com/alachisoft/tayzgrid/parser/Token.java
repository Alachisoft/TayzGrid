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
/** While the Symbol represents a class of terminals and nonterminals,
 the Token represents an individual piece of information.
*/
public class Token extends Symbol {
	private int m_state;
	private Object m_data;

	/* constructors */

	/**
	*/
	public Token() {
		m_state = -1;
		m_data = "";
	}

	/**
	*/
	public Token(Symbol p_symbol) {
		this();
		SetParent(p_symbol);
	}

	/* properties */

	/** Gets the state 
	*/
	public final int getState() {
		return m_state;
	}
	public final void setState(int value) {
		m_state = value;
	}

	/** Gets or sets the information stored in the token.
	*/
	public final Object getData() {
		return m_data;
	}
	public final void setData(Object value) {
		m_data = value;
	}

	/* public methods */

	/** 
	*/
	public final void SetParent(Symbol p_symbol) {
		CopyData(p_symbol);
	}

	/** Returns the text representation of the token's parent symbol.
	 In the case of nonterminals, the name is delimited by angle brackets, 
	 special terminals are delimited by parenthesis and terminals are delimited 
	 by single quotes (if special characters are present).
	*/
	@Override
	public String toString() {
		return super.toString();
	}
}