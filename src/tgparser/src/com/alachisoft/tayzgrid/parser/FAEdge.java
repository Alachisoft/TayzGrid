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
/** Each state in the Determinstic Finite Automata contains multiple edges which
 link to other states in the automata. This class is used to represent an edge.
*/
public class FAEdge {
	private String m_characters;
	private int m_targetIndex;

	/* constructor */

	public FAEdge(String p_characters, int p_targetIndex) {
		m_characters = p_characters;
		m_targetIndex = p_targetIndex;
	}

	/* properties */

	public final String getCharacters() {
		return m_characters;
	}
	public final void setCharacters(String value) {
		m_characters = value;
	}

	public final int getTargetIndex() {
		return m_targetIndex;
	}
	public final void setTargetIndex(int value) {
		m_targetIndex = value;
	}

	/* public methods */

	public final void AddCharacters(String p_characters) {
		m_characters = m_characters + p_characters;
	}

	@Override
	public String toString() {
		return "DFA edge [chars=[" + m_characters + "],action=" + m_targetIndex + "]";
	}
}