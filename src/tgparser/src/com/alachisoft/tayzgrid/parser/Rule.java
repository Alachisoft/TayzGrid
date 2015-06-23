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
/** The Rule class is used to represent the logical structures of the grammar.
 Rules consist of a head containing a nonterminal followed by a series of
 both nonterminals and terminals.
*/
public class Rule {
	private Symbol m_ruleNT; // non-terminal rule
	private java.util.ArrayList m_ruleSymbols;
	private int m_tableIndex;

	/* constructor */

	/** Creates a new Rule.
	*/
	public Rule(int p_tableIndex, Symbol p_head) {
		m_ruleSymbols = new java.util.ArrayList();
		m_tableIndex = p_tableIndex;
		m_ruleNT = p_head;
	}

	/* public properties */

	/** Gets the index of this <c>Rule</c> in the GoldParser's rule-table.
	*/
	public final int getTableIndex() {
		return m_tableIndex;
	}

	/** Gets the head symbol of this rule.
	*/
	public final Symbol getRuleNonTerminal() {
		return m_ruleNT;
	}

	/** Gets the number of symbols in the body (right-hand-side) of the rule.
	*/
	public final int getSymbolCount() {
		return m_ruleSymbols.size();
	}

	/* internal properties */

	/** The name of this rule.
	*/
	public final String getName() {
		return "<" + m_ruleNT.getName() + ">";
	}

	/** The definition of this rule.
	*/
	public final String getDefinition() {
		StringBuilder result = new StringBuilder();
		java.util.Iterator enumerator = m_ruleSymbols.iterator();

		while (enumerator.hasNext()) {
			Symbol symbol = (Symbol)enumerator.next();
			result.append(symbol.toString()).append(" ");
		}

		return result.toString();
	}

	public final boolean getContainsOneNonTerminal() {
		return m_ruleSymbols.size() == 1 && ((Symbol)m_ruleSymbols.get(0)).getKind().getValue() == 0;
	}

	/* public methods */

	/** Returns the symbol in the body of the rule with the specified index.
	*/
	public final Symbol GetSymbol(int p_index) {
		if (p_index >= 0 && p_index < m_ruleSymbols.size()) {
			return (Symbol)m_ruleSymbols.get(p_index);
		} else {
			return null;
		}
	}

	/** Returns the Backus-Noir representation of this <c>Rule</c>.
	*/
	@Override
	public String toString() {
		return getName() + " ::= " + getDefinition();
	}


	/**
	*/
	public final void AddItem(Symbol p_symbol) {
		m_ruleSymbols.add(p_symbol);
	}
}