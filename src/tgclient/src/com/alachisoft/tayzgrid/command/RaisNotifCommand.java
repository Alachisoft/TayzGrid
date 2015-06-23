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

package com.alachisoft.tayzgrid.command;

import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;


public final class RaisNotifCommand extends Command {

    private Object _notifData = null;

    /**
     * Creates a new instance of AddCommand
     *
     * @param key
     * @param value
     * @param absoluteExpiration
     * @param slidingExpiration
     * @param priority
     * @param isResyncExpiredItems
     * @param group
     * @param subGroup
     * @param async
     * @throws java.io.UnsupportedEncodingException
     * @throws java.io.IOException
     */
    public RaisNotifCommand(String notifId, Object value, boolean isAsync)
            throws UnsupportedEncodingException, IOException {
        name = "RAISENOTIF";

        this._notifData = value;
        this.isAsync = isAsync;
    }

    public void createCommand() throws CommandException {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (this._notifData == null) {
            throw new NullPointerException("values");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key");
        }
        String cmdString = "RAISENOTIF \"" + getRequestId() + "\"" + this.key
                + "\"";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(value);
            this.value = bos.toByteArray();

            oos.close();
        } catch (IOException ex) {
            
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                
            }
        }

        try {

            commandBytes = constructCommand(cmdString, bos.toByteArray());
        } catch (UnsupportedEncodingException ex) {
           
        }
    }

    /**
     *
     * @return
     */
    protected boolean parseCommand() {
        return true;
    }

    /**
     * Returns the command type and the parameters as a <code>String</code>.
     *
     * @return a string representation of this command.
     */
    public String toString() {
        return this.name;
    }

    public CommandType getCommandType() {
        return null;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
