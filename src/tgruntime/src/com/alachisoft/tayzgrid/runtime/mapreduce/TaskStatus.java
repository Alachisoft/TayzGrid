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

package com.alachisoft.tayzgrid.runtime.mapreduce;

import java.io.Serializable;

public class TaskStatus implements Serializable{

    public enum Status {
        InProgress(1),
        Completed(2),
        Cancelled(3),
        Failed(4),
        Waiting(5);

        private int value;

        Status(int val) {
            this.value = val;
        }
        public void setValue(int value) {
            this.value = value;
        }    
    }
    
    private Status status;
    private long processed;
    
    public TaskStatus(Status val, long processed)
    {
        this.status = val;
        this.processed = processed;
    }
    public long getProcessed() {
        return processed;
    }
    public Status getStatus()
    {
        return status;
    }
}
