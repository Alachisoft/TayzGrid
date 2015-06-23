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
import java.util.ArrayList;

/**
 * This exception is thrown when multiple exceptions occur from multiple nodes.
 * It combines all the exceptions as inner exceptions and throw it to the client
 * application.
 *
 */
public class AggregateException extends CacheException {

    private Exception[] _exceptions;

    public AggregateException(ArrayList exceptions) {
        if (exceptions != null) {
            _exceptions = new Exception[exceptions.size()];
            for (int i = 0; i < exceptions.size(); i++) {
                _exceptions[i] = (Exception) exceptions.get(i);
            }
        }
    }

    public AggregateException(Exception[] _exceptions) {
        this._exceptions = _exceptions;
    }

    public AggregateException(String reason, ArrayList exceptions) {
        super(reason);
        if (exceptions != null) {
            _exceptions = (Exception[]) exceptions.toArray();
        }
    }

    public Exception[] getExceptions() {
        return _exceptions;

    }

    @Override
    public String getMessage() {
        if (_exceptions != null && _exceptions.length > 0) {
            String aggregateMsg = "Aggregate Excetpion was found";
            for (int i = 0; i < _exceptions.length; i++) {
                aggregateMsg += "\r\n";
                aggregateMsg += "Exception:" + (i + 1) + " ";
                aggregateMsg += _exceptions[i].toString();
            }
            aggregateMsg += "\r\n";
            return aggregateMsg;
        }
        return super.getMessage();
    }
}
