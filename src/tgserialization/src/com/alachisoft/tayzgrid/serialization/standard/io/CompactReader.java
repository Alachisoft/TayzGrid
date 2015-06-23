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


package com.alachisoft.tayzgrid.serialization.standard.io;

import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class CompactReader {
        private ObjectInputStream reader;

        /// <summary>
        /// Constructs a compact reader over a <see cref="Stream"/> object.
        /// </summary>
        /// <param name="input"><see cref="Stream"/> object</param>
        public CompactReader(ObjectInputStream input)
        {
            reader = input;
        }
        
       

        /// <summary>
        /// Reads an object of type <see cref="object"/> from the current stream 
        /// and advances the stream position. 
        /// </summary>
        /// <returns>object read from the stream</returns>
        public Object ReadObject()throws IOException, ClassNotFoundException 
        {
           return reader.readObject();
        }

       

        /// <summary>
        /// Skips an object of type <see cref="object"/> from the current stream 
        /// and advances the stream position. 
        /// </summary>
        public  void SkipObject() throws IOException
        {
            reader.skipObject();
        }

        /// <summary>
        /// Reads an object of type <see cref="bool"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public  boolean ReadBoolean() throws IOException 
        {
            return reader.readBoolean(); 
        }
        /// <summary>
        /// Reads an object of type <see cref="byte"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public  byte ReadByte() throws IOException { return reader.readByte(); }
        /// <summary>
        /// Reads an object of type <see cref="byte[]"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <param name="count">number of bytes read</param>
        /// <returns>object read from the stream</returns>
        public byte[] ReadBytes(int count) throws IOException
        {
            byte[] buffer = new byte[count];
            reader.read(buffer, 0, count); 
            return buffer;
        }
        /// <summary>
        /// Reads an object of type <see cref="char"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public  char ReadChar() throws IOException { return reader.readChar(); }
        /// <summary>
        /// Reads an object of type <see cref="char[]"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public char[] ReadChars(int count) throws IOException 
        { 
            char[] characters = new char[count];
            for(int i =0; i< count; i++)
            {
               characters[i]= reader.readChar();
            }
            return characters;
        }
        /// <summary>
        /// Reads an object of type <see cref="float"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public float ReadSingle() throws IOException { return reader.readFloat(); }
        
        /// <summary>
        /// Reads an object of type <see cref="double"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public double ReadDouble() throws IOException { return reader.readDouble(); }
        /// <summary>
        /// Reads an object of type <see cref="short"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public short ReadInt16() throws IOException { return reader.readShort(); }
        /// <summary>
        /// Reads an object of type <see cref="int"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public int ReadInt32() throws IOException { return reader.readInt(); }
        /// <summary>
        /// Reads an object of type <see cref="long"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public long ReadInt64() throws IOException { return reader.readLong(); }
        /// <summary>
        /// Reads an object of type <see cref="string"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        public String ReadString() throws IOException { return reader.readUTF(); }
        /// <summary>
        /// Reads the specifies number of bytes into <paramref name="buffer"/>.
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <param name="buffer">buffer to read into</param>
        /// <param name="index">starting position in the buffer</param>
        /// <param name="count">number of bytes to write</param>
        /// <returns>number of buffer read</returns>
        public  int Read(byte[] buffer, int index, int count) throws IOException { return reader.read(buffer, index, count); }
        /// <summary>
        /// Reads the specifies number of bytes into <paramref name="buffer"/>.
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <param name="buffer">buffer to read into</param>
        /// <param name="index">starting position in the buffer</param>
        /// <param name="count">number of bytes to write</param>
        /// <returns>number of chars read</returns>
        public  int Read(char[] buffer, int index, int count) throws IOException
        {
            for(int i = index; i<count; i++)
            {
                buffer[index]= reader.readChar();
            }
            return count;
        }
        
        
        /// <summary>
        /// Reads an object of type <see cref="ushort"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        
        public  int ReadUInt16() throws IOException { return reader.readUInt16(); }
        /// <summary>
        /// Reads an object of type <see cref="uint"/> from the current stream 
        /// and advances the stream position. 
        /// This method reads directly from the underlying stream.
        /// </summary>
        /// <returns>object read from the stream</returns>
        
        public  long ReadUInt32() throws IOException { return reader.readUInt32(); }
        /// <summary>
        /// Skips an object of type <see cref="bool"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public  void SkipBoolean() throws IOException
        {
            reader.skipBoolean();
        }

        /// <summary>
        /// Skips an object of type <see cref="byte"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipByte() throws IOException
        {
            reader.skipByte();
        }

        /// <summary>
        /// Skips an object of type <see cref="byte[]"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        /// <param name="count">number of bytes read</param>
        public void SkipBytes(int count) throws IOException
        {
            reader.skipBytes(count);
        }

        /// <summary>
        /// Skips an object of type <see cref="char"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipChar() throws IOException
        {
            reader.skipChar();
        }

        /// <summary>
        /// Skips an object of type <see cref="char[]"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipChars(int count) throws IOException
        {
            for(int i = 0 ; i<count; i++)
                reader.skipChar();
        }

        /// <summary>
        /// Skips an object of type <see cref="float"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public  void SkipSingle() throws IOException
        {
            reader.skipFloat();
        }

        /// <summary>
        /// Skips an object of type <see cref="double"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipDouble() throws IOException
        {
            reader.skipDouble();
        }

        /// <summary>
        /// Skips an object of type <see cref="short"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipInt16() throws IOException
        {
            reader.skipShort();
        }

        /// <summary>
        /// Skips an object of type <see cref="int"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipInt32() throws IOException
        {
            reader.skipInt();
        }

        /// <summary>
        /// Skips an object of type <see cref="long"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipInt64() throws IOException
        {
            reader.skipLong();
        }

        
        /// <summary>
        /// Skips an object of type <see cref="sbyte"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public void SkipSByte() throws IOException
        {
            reader.skipByte();
        }

        /// <summary>
        /// Skips an object of type <see cref="ushort"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public  void SkipUInt16() throws IOException
        {
            reader.skipUInt16();
        }

        /// <summary>
        /// Skips an object of type <see cref="uint"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public  void SkipUInt32() throws IOException
        {
            reader.skipUInt32();
        }

        /// <summary>
        /// Skips an object of type <see cref="ulong"/> from the current stream 
        /// and advances the stream position. 
        /// This method Skips directly from the underlying stream.
        /// </summary>
        public  void SkipUInt64() throws IOException
        {
            reader.skipLong();
        }
        
}
