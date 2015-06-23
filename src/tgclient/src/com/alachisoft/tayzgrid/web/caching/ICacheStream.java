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



package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.runtime.exceptions.NotSupportedException;


interface ICacheStream {

        long length() throws Exception;

            /// <summary>
        /// Gets a value indicating whether the current
        /// stream supports reading.
        /// </summary>
        /// <remarks>Returns 'True' if stream is opened with either StreamMode.Read or StreamMode.ReadWithoutLock. </remarks>
        boolean canRead();
        /// <summary>
        /// Gets a value indicating whether the current stream supports seeking.
        /// </summary>
        /// <remarks>Alwasy returns 'False' because CacheStream does not support seek operation.</remarks>
        boolean canSeek();

        /// <summary>
        /// Gets a value indicating whether the current
        /// stream supports writing.
        /// </summary>
        /// <remarks>Returns 'True' if stream is opened with StreamMode.Write. </remarks>
        boolean canWrite();

        /// <summary>
        /// Gets a value indicating whether stream is closed.
        /// </summary>
        boolean closed();

        /// <summary>
        /// When overridden in a derived class, clears all buffers for this stream and causes any
        /// buffered data to be written to the underlying device.
        /// </summary>
        /// <remarks>CacheStream does not buffer the data. Each read/write operations is performed on
        /// the cache.</remarks>
        void flush();

        /// <summary>
        /// Gets/Sets the position within current stream.
        /// </summary>
        ///<exception cref="NotSupportedException">Stream does not support seeking.</exception>
         public long position() throws Exception;

        /// <summary>
        /// Sets the position within the current stream.</summary>
        /// <param name="offset">A byte offset relative to the origin parameter.</param>
        /// <param name="origin">A value of type SeekOrigin indicating the reference point used to obtain the new position. </param>
        /// <remarks>CacheStream does not support seeking. </remarks>
        /// <exception cref="NotSupportedException">Stream does not support seeking.</exception>
        public long seek(long offset) throws NotSupportedException;

        /// <summary>
        /// Sets the length of the stream.
        /// </summary>
        /// <param name="value">The desired length of the current stream in bytes.</param>
        /// <exception cref="NotSupportedException">Stream does not support both writing and seeking</exception>
        public void setLength(long value) throws NotSupportedException;

}
