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

package com.alachisoft.tayzgrid.common.util;

import java.io.Serializable;

public class QueueObject implements Serializable
{

    private String _key;
    private String _command;
    private long _index;

    public QueueObject(String key, String command)
    {
        _key = key;
        _command = command;
    }

    public final long getIndex()
    {
        return _index;
    }

    public final void setIndex(long value)
    {
        _index = value;
    }

    public final String getKey()
    {
        return _key;
    }

    public final void setKey(String value)
    {
        _key = value;
    }

    public final String getCommand()
    {
        return _command;
    }

    public final void setCommand(String value)
    {
        _command = value;
    }
}
