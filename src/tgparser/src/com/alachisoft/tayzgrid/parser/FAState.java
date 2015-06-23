/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/


package com.alachisoft.tayzgrid.parser;

/** Represents a state in the Deterministic Finite Automata which is used by
 the tokenizer.
*/
public class FAState {
	private java.util.ArrayList m_edges;
	private int m_acceptSymbol;

	/* constructor */

	public FAState() {
		m_edges = new java.util.ArrayList();
		m_acceptSymbol = -1;
	}

	/* properties */

	public final java.util.ArrayList getEdges() {
		return m_edges;
	}

	public final int getAcceptSymbol() {
		return m_acceptSymbol;
	}
	public final void setAcceptSymbol(int value) {
		m_acceptSymbol = value;
	}

	public final int getEdgeCount() {
		return m_edges.size();
	}

	/* public methods */

	public final FAEdge GetEdge(int p_index) {
		if (p_index >= 0 && p_index < m_edges.size()) {
			return (FAEdge)m_edges.get(p_index);
		} else {
			return null;
		}
	}

	public final void AddEdge(String p_characters, int p_targetIndex) {
		if (p_characters.equals("")) {
			FAEdge edge = new FAEdge(p_characters, p_targetIndex);
			m_edges.add(edge);
		} else {
			int index = -1;
			int edgeCount = m_edges.size();

			// find the edge with the specified index
			for (int n = 0; (n < edgeCount) && (index == -1); n++) {
				FAEdge edge = (FAEdge)m_edges.get(n);
				if (edge.getTargetIndex() == p_targetIndex) {
					index = n;
				}
			}

			// if not found, create a new edge
			if (index == -1) {
				FAEdge edge = new FAEdge(p_characters, p_targetIndex);
				m_edges.add(edge);
			}
			// else add the characters to the existing edge
			else {
				FAEdge edge = (FAEdge)m_edges.get(index);
				edge.AddCharacters(p_characters);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("DFA state:\n");
                FAEdge edge;
		for (Object edgeObj : m_edges) {
                        edge=(FAEdge)edgeObj;
			result.append("- ").append(edge).append("\n");
		}

		if (m_acceptSymbol != -1) {
			result.append("- accept symbol: ").append(m_acceptSymbol).append("\n");
		}

		return result.toString();
	}
}