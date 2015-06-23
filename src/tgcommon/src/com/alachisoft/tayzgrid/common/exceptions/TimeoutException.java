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

package com.alachisoft.tayzgrid.common.exceptions;

import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import java.io.Serializable;

public class TimeoutException extends CacheException implements Serializable
{

    public java.util.List failed_mbrs = null; // members that failed responding

    public TimeoutException()
    {
        super("TimeoutExeption");
    }

    public TimeoutException(String msg)
    {
        super(msg);
    }

    public TimeoutException(java.util.List failed_mbrs)
    {
        super("TimeoutExeption");
        this.failed_mbrs = failed_mbrs;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());

        if (failed_mbrs != null && failed_mbrs.size() > 0)
        {
            sb.append(" (failed members: ").append(failed_mbrs);
        }
        return sb.toString();
    }
}
