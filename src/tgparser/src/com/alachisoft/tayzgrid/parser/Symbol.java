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
/** This class is used to store the nonterminals used by the DFA and LALR parser
 Symbols can be either terminals (which represent a class of tokens, such as
 identifiers) or non-terminals (which represent the rules and structures of
 the grammar). Symbols fall into several categories for use by the
 GoldParser Engine which are enumerated in type <c>SymbolType</c> enum.
*/
public class Symbol {
	private static final String c_quotedChars = "|-+*?()[]{}<>!\\u0022";

	private int m_tableIndex;
	private String m_name;
	private SymbolType m_kind = SymbolType.values()[0];

	/* constructor */

	/** Creates a new Symbol object.
	*/
	public Symbol(int p_index, String p_name, SymbolType p_kind) {
		m_tableIndex = p_index;
		m_name = p_name;
		m_kind = p_kind;
	}

	/**
	*/
	protected Symbol() {
		this(-1, "", SymbolType.Error);
	}

	/* properties */

	/** Gets the index of this symbol in the GoldParser's symbol table.
	*/
	public final int getTableIndex() {
		return m_tableIndex;
	}

	/** Gets the name of the symbol.
	*/
	public final String getName() {
		return m_name;
	}

	/** Gets the <c>SymbolType</c> of the symbol.
	*/
	public final SymbolType getKind() {
		return m_kind;
	}

	/* public methods */

	/** Returns true if the specified symbol is equal to this one.
	*/
	@Override
	public boolean equals(Object p_object) {
		Symbol symbol = (Symbol)p_object;
		return m_name.equals(symbol.getName()) && m_kind == symbol.getKind();
	}

	/** Returns the hashcode for the symbol.
	*/
	@Override
	public int hashCode() {
		return (m_name + "||" + m_kind).hashCode();
	}

	/** Returns the text representation of the symbol.
	 In the case of nonterminals, the name is delimited by angle brackets,
	 special terminals are delimited by parenthesis and terminals are delimited
	 by single quotes (if special characters are present).
	*/
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		if (m_kind == SymbolType.NonTerminal) {
			result.append("<").append(m_name).append(">");
		} else if (m_kind == SymbolType.Terminal)
			 { result.append(m_name);
			} else {
			result.append("(").append(m_name).append(")");
		}

		return result.toString();
	}

	/* private methods */

	/**
	*/
	private void PatternFormat(String p_source, StringBuilder p_target) {
		for (int i = 0; i < p_source.length(); i++) {
			char ch = p_source.charAt(i);
			if (ch == '\'') {
				p_target.append("''");
			} else if (c_quotedChars.indexOf(ch) != -1) {
				p_target.append("'").append(ch).append("'");
			} else {
				p_target.append(ch);
			}
		}
	}

	/**
	*/
	protected final void CopyData(Symbol p_symbol) {
		m_name = p_symbol.getName();
		m_kind = p_symbol.getKind();
		m_tableIndex = p_symbol.getTableIndex();
	}
}