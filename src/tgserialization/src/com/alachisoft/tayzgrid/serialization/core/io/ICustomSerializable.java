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


/**
 *Implementations of ICompactSerializable can add their state directly to the output stream,
 * enabling them to bypass costly serialization.
 * Objects that implement CompactSerializable must implement default constructor
 * As per current implementation when a ICompactSerializable is deserialized the default constructor is not invoked, therefore the object must "construct" itself in
 * ICompactSerializable.Deserialize
 */
public interface ICustomSerializable
{

    /**
     * Load the state from the passed stream reader object.
     * As per current implementation when a <see cref="ICompactSerializable"/> is deserialized
     * the default constructor is not invoked, therefore the object must "construct" itself in
     * @param reader
     */
    void DeserializeLocal(BlockDataInputStream reader);


    /**
     * Save the the state to the passed stream reader object.
     * @param writer
     */
    void SerializeLocal(BlockDataOutputStream writer);
}
