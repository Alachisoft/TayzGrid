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

package com.alachisoft.tayzgrid.cluster.blocks;

import java.net.SocketException;


public class ExtSocketException extends SocketException
{
   

    private String message;

    public ExtSocketException(String message)
    {
        this.message = message;
    }

    /**
     * Gets the error message for the exception.
     */
    @Override
    public String getMessage()
    {
        return message;
    }
}
