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

package com.alachisoft.tayzgrid.common.monitoring;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.io.Serializable;

/**
 * Node represent a physical machine participating either as server or client.
 */
public class Node implements InternalCompactSerializable
{

    private String _name;
    private Address _address;

    public Node()
    {
    }

    public Node(String name, Address address)
    {
        setName(name);
        setAddress(address);
    }

    /**
     * Gets/Sets the name of the node.
     */
    public final String getName()
    {
        return _name;
    }

    public final void setName(String value)
    {
        if (value != null)
        {
            _name = value.toLowerCase();
        }
    }

    /**
     * Gets/Sets the IPAddress of the node.
     */
    public final Address getAddress()
    {
        return _address;
    }

    public final void setAddress(Address value)
    {
        _address = value;
    }

    @Override
    public boolean equals(Object obj)
    {
        Node other = (Node) ((obj instanceof Node) ? obj : null);
        boolean equal = false;
        if (other != null)
        {
            if (getName() == null && other.getName() == null)
            {
                equal = true;
            }

            if (equal)
            {
                equal = false;
                if (getAddress() == null && other.getAddress() == null)
                {
                    equal = true;
                }
                if (getAddress() != null && other.getAddress() != null && getAddress().equals(other.getAddress()))
                {
                    equal = true;
                }
            }

        }
        return equal;
    }

        public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
        {
            _name = (String) Common.readAs(reader.ReadObject(), String.class);
            _address = (Address) Common.readAs(reader.ReadObject(), Address.class);
        }

        public void Serialize(CompactWriter writer) throws IOException
        {
            writer.WriteObject(_name);
            writer.WriteObject(_address);
        }
}
