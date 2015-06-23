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
/** This class represents an action in a LALR State. 
 There is one and only one action for any given symbol.
*/
public class LRAction {
	private Symbol m_symbol;
	private Action m_action = getAction().values()[0];
	private int m_value;

	/* properties */

	public final Symbol getSymbol() {
		return m_symbol;
	}
	public final void setSymbol(Symbol value) {
		m_symbol = value;
	}

	public final Action getAction() {
		return m_action;
	}
	public final void setAction(Action value) {
		m_action = value;
	}

	public final int getValue() {
		return m_value;
	}
	public final void setValue(int value) {
		m_value = value;
	}

	/* public methods */

	@Override
	public String toString() {
		return "LALR action [symbol=" + m_symbol + ",action=" + m_action + ",value=" + m_value + "]";
	}
}