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

// $Id: ChannelException.java,v 1.4 2004/08/04 14:26:34 belaban Exp $
import java.io.Serializable;

/**
 * This class represents the super class for all exception types thrown by JGroups.
 */
public class ChannelException extends Exception implements  Serializable
{

    /**
     * Retrieves the cause of this exception as passed to the constructor. <p> This method is provided so that in the case that a 1.3 VM is used, 1.4-like exception chaining
     * functionality is possible. If a 1.4 VM is used, this method will override
     * <code>Throwable.getCause()</code> with a version that does exactly the same thing.
     *
     *
     * @return the cause of this exception.
     *
     */
    public Exception getCause()
    {
        return _cause;
    }
    // Instance-level implementation.
    private Exception _cause;

    public ChannelException()
    {
        super();
    }

    public ChannelException(String reason)
    {
        super(reason);
    }

    public ChannelException(String reason, Exception cause)
    {
        super(reason);
        _cause = cause;
    }

    @Override
    public String toString()
    {
        return "ChannelException: " + super.getLocalizedMessage();
    }


    /**
     * Prints this exception's stack trace to standard error. <p> This method is provided so that in the case that a 1.3 VM is used, calls to
     * <code>printStackTrace</code> can be intercepted so that 1.4-like exception chaining functionality is possible.
     */
    public final void printStackTrace()
    {
    }


  
}
