/*
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

package com.alachisoft.tayzgrid.cluster;

import java.io.Serializable;

public class ChannelClosedException extends ChannelException implements Serializable
{

    public ChannelClosedException()
    {
        super();
    }

    public ChannelClosedException(String msg)
    {
        super(msg);
    }

    @Override
    public String toString()
    {
        return "ChannelClosedException";
    }
}
