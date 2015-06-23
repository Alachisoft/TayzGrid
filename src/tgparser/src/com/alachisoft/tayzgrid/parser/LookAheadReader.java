/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/


package com.alachisoft.tayzgrid.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StreamCorruptedException;

/** This is a wrapper around StreamReader which supports lookahead.
*/
public class LookAheadReader {
	private static final int BUFSIZE = 256;

	private BufferedReader m_reader;
	private char[] m_buffer;
	private int m_curpos;
	private int m_buflen;

	/* constructor */

	/** Creates a new LookAheadReader around the specified StreamReader.
	*/
	public LookAheadReader(BufferedReader p_reader) {
		m_reader = p_reader;
		m_curpos = -1;
		m_buffer = new char[BUFSIZE];
	}

	/* private methods */

	/** Makes sure there are enough characters in the buffer.
	*/
	private void FillBuffer(int p_length) throws IOException {
		int av = m_buflen - m_curpos; // het aantal chars na curpos

		if (m_curpos == -1) {
			// fill the buffer
			m_buflen = m_reader.read(m_buffer, 0, BUFSIZE);
			m_curpos = 0;
		} else if (av < p_length) {
			if (m_buflen < BUFSIZE)
				// not available
			{
                               throw new StreamCorruptedException();
			} else {
				// re-fill the buffer
				System.arraycopy(m_buffer, m_curpos, m_buffer, 0, av);
				int read = m_reader.read(m_buffer, av, m_curpos);
				m_buflen = read + av;
				m_curpos = 0;

                                    //Fix for client issue regarding the length of the query
                                m_reader.mark(m_buflen+1);
				if (m_reader.read() == -1 && m_buflen == BUFSIZE && read == 0) {
					throw new StreamCorruptedException();
				}
                                m_reader.reset();;
			}
		}

		// append a newline on EOF
		if (m_buflen < BUFSIZE) {
			m_buffer[m_buflen++] = '\n';
		}
	}

	/* public methods */

	/** Returns the next char in the buffer but doesn't advance the current position.
	*/
	public final char LookAhead() throws IOException, StreamCorruptedException {
		FillBuffer(1);
		return m_buffer[m_curpos];
	}

	/** Returns the char at current position + the specified number of characters.
	 Does not change the current position.
	 @param p_pos The position after the current one where the character to return is
	*/
	public final char LookAhead(int p_pos) throws IOException, StreamCorruptedException {
		FillBuffer(p_pos + 1);
		return m_buffer[m_curpos + p_pos];
	}

	/** Returns the next char in the buffer and advances the current position by one.
	*/
	public final char Read() throws IOException, StreamCorruptedException {
		FillBuffer(1);
		return m_buffer[m_curpos++];
	}

	/** Returns the next n characters in the buffer and advances the current position by n.
	*/
	public final String Read(int p_length) throws IOException, StreamCorruptedException {
		FillBuffer(p_length);
		String result = new String(m_buffer, m_curpos, p_length);
		m_curpos += p_length;
		return result;
	}

	/** Advances the current position in the buffer until a newline is encountered.
	*/
	public final void DiscardLine() throws IOException, StreamCorruptedException {
		while (LookAhead() != '\n') {
			m_curpos++;
		}
	}

	/** Closes the underlying StreamReader.
	*/
	public final void Close() throws IOException {
		m_reader.close();
	}
}