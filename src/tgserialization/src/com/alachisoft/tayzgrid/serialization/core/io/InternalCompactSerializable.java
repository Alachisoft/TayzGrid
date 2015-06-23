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


package com.alachisoft.tayzgrid.serialization.core.io;

import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public interface InternalCompactSerializable {
    
            /// <summary>
            /// Load the state from the passed stream reader object.
            /// </summary>
            /// <param name="reader">A <see cref="CompactBinaryReader"/> object</param>
            /// <remarks>
            /// As per current implementation when a <see cref="ICompactSerializable"/> is deserialized 
            /// the default constructor is not invoked, therefore the object must "construct" itself in 
            /// <see cref="ICompactSerializable.Deserialize"/>.
            /// </remarks>

            void Deserialize(CompactReader reader) throws IOException,ClassNotFoundException;

            /// <summary>
            /// Save the the state to the passed stream reader object.
            /// </summary>
            /// <param name="writer">A <see cref="BinaryWriter"/> object</param>

            void Serialize(CompactWriter writer)throws IOException;
    
}
