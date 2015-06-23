/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alachisoft.tayzgrid.serialization.standard.io;

import java.io.IOException;
import java.math.BigInteger;

/**
 *
 * @author 
 */
public class CompactWriter {
     
        private ObjectOutputStream writer;

        /// <summary>
        /// Constructs a compact writer over a <see cref="Stream"/> object.
        /// </summary>
        /// <param name="output"><see cref="Stream"/> object</param>
        public CompactWriter(ObjectOutputStream output)
            
        {
            writer = output;
        }
        
        /// <summary>
        /// Writes <paramref name="graph"/> to the current stream and advances the stream position. 
        /// </summary>
        /// <param name="graph">Object to write</param>
        public void WriteObject(Object graph) throws IOException
        {
            writer.writeObject(graph);
        }

        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public  void Write(boolean value) throws IOException { writer.writeBoolean(value); }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public void Write(byte value) throws IOException { writer.writeByte(value); }
        /// <summary>
        /// Writes <paramref name="ch"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="ch">Object to write</param>
        public  void Write(char ch) throws IOException { writer.writeChar(ch); }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public  void Write(short value) throws IOException { writer.writeShort(value); }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public  void Write(int value) throws IOException, IOException, IOException, IOException { writer.writeInt(value); }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public  void Write(long value) throws IOException { writer.writeLong(value); }
                
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public  void Write(float value) throws IOException { writer.writeFloat(value); }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public  void Write(double value) throws IOException { writer.writeDouble(value); }
        
        /// <summary>
        /// Writes <paramref name="buffer"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="buffer">Object to write</param>
        public  void Write(byte[] buffer) throws IOException
        {
            if (buffer != null)
                writer.write(buffer,0,buffer.length);
            else
                WriteObject(null);
        }
        /// <summary>
        /// Writes <paramref name="chars"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="chars">Object to write</param>
        public  void Write(char[] chars) throws IOException
        {
            if (chars != null) {
                for(int i = 0 ; i< chars.length; i++)
                {
                writer.writeChar(chars[i]);
                }
            }
            else {
                WriteObject(null);
            }
        }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        public  void Write(String value) throws IOException
        {
            if (value != null) {
                writer.writeUTF(value);
            }
            else {
                WriteObject(null);
            }
        }
        /// <summary>
        /// Writes <paramref name="buffer"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="buffer">buffer to write</param>
        /// <param name="index">starting position in the buffer</param>
        /// <param name="count">number of bytes to write</param>
        public  void Write(byte[] buffer, int index, int count) throws IOException
        {
            if (buffer != null) {
                writer.write(buffer, index, count);
            }
            else {
                WriteObject(null);
            }
        }
        /// <summary>
        /// Writes <paramref name="chars"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="chars">buffer to write</param>
        /// <param name="index">starting position in the buffer</param>
        /// <param name="count">number of bytes to write</param>
        public  void Write(char[] chars, int index, int count) throws IOException
        {
            if (chars != null)
            {
                for(int i = index; i<count; i++)
                {
                    writer.writeChar(chars[i]);
                }
            }
            else
                WriteObject(null);
        }
        
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        
        public  void WriteUint16(int value) throws IOException { writer.writeUInt16(value); }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        
        public  void WriteUint32(long value) throws IOException { writer.writeUInt32(value); }
        /// <summary>
        /// Writes <paramref name="value"/> to the current stream and advances the stream position. 
        /// This method writes directly to the underlying stream.
        /// </summary>
        /// <param name="value">Object to write</param>
        
        public  void Write(BigInteger value) throws IOException { writer.writeUInt64(value); }

       
}
