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

package com.alachisoft.tayzgrid.communication;

import com.alachisoft.tayzgrid.command.CommandResponse;
import java.util.HashMap;

    public class ResponseList
    {
        final int FIRST_CHUNK = 1;

        private long _requestId = -1;
        private HashMap<Integer, CommandResponse> _responses = new HashMap<Integer, CommandResponse>();
        private final Object _mutex = new Object();

        public void setRequestID(long id) {
            _requestId = id;
        }

        public long getRequestID() {
            return _requestId;
        }

        public HashMap<Integer, CommandResponse> getResponses() {
            return _responses;
        }

        public boolean IsComplete() {
            synchronized (_mutex)
            {
                Integer chunk = Integer.valueOf(FIRST_CHUNK);
                boolean result = _responses.containsKey(chunk);
                if (result)
                {
                    CommandResponse firstChunk = _responses.get(chunk);
                    result = _responses.size() == firstChunk.getNumberOfChunks();
                }

                return result;
            }
        }

       public void AddResponse(CommandResponse response) {
           synchronized (_mutex)
            {
                Integer sequence = Integer.valueOf(response.getSequenceId());
                if (!_responses.containsKey(sequence))
                    _responses.put(sequence, response);
            }
       }

        public void Clear() {
            synchronized (_mutex)
            {
                _responses.clear();
            }
        }
    }
