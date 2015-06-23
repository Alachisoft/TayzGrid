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

package com.alachisoft.tayzgrid.runtime.exceptions;

import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;

/**
 * Throws when a specific Item is not supported in TayzGrid Java Client
 */
public class NotSupportedException extends CacheException
{


    /**
     * Constructs an <code>OperationFailedException</code> with <code>null</code>
     * as its error detail message.
     */
    public NotSupportedException()
    {
    }

    /**
     * Constructs an <code>OperationFailedException</code> with the specified detail
     * message. The error message string <code>s</code> can later be
     * retrieved by the <code>{@link java.lang.Throwable#getMessage}</code>
     * method of class <code>java.lang.Throwable</code>.
     *
     * @param   s   the detail message.
     */
    public NotSupportedException(java.lang.String s)
    {
        super(s);
    }
}
