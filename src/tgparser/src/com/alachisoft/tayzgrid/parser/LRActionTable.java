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
/** This class contains the actions (reduce/shift) and goto information
 for a STATE in a LR parser. Essentially, this is just a row of actions in
 the LR state transition table. The only data structure is a list of
 LR Actions.
*/
public class LRActionTable {
	private java.util.ArrayList m_members;

	/* constructor */

	public LRActionTable() {
		m_members = new java.util.ArrayList();
	}

	/* properties */

	public final int getCount() {
		return m_members.size();
	}

	public final java.util.ArrayList getMembers() {
		return m_members;
	}

	/* public methods */

	public final LRAction GetActionForSymbol(int p_symbolIndex) {
                 LRAction action;
		for (Object actionObj : m_members) {
                    action=(LRAction)actionObj;
			if (action.getSymbol().getTableIndex() == p_symbolIndex) {
				return action;
			}
		}

		return null;
	}

	public final LRAction GetItem(int p_index) {
		if (p_index >= 0 && p_index < m_members.size()) {
			return (LRAction)m_members.get(p_index);
		} else {
			return null;
		}
	}

	/** Adds an new LRAction to this table.
	 @param p_symbol The Symbol.
	 @param p_action The Action.
	 @param p_value The value.
	*/
	public final void AddItem(Symbol p_symbol, Action p_action, int p_value) {
		LRAction item = new LRAction();
		item.setSymbol(p_symbol);
		item.setAction(p_action);
		item.setValue(p_value);
		m_members.add(item);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("LALR table:\n");
                LRAction action;
		for (Object actionObj : m_members) {
                     action=(LRAction)actionObj;
			result.append("- ").append(action.toString() + "\n");
		}
		return result.toString();
	}
}