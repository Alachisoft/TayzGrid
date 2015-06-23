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

package com.alachisoft.tayzgrid.web.mapreduce;


public class TaskStatistics {

    public enum TaskStage {
        WAITING(1),
        MAPPING(2),
        COMBINING(3),
        REDUCING(4),
        FINISHED(5),
        FAILED(6);
        
        int _value;
        TaskStage(int value)
        {
            this._value = value;
        }
        public int getValue()
        {
            return this._value;
        }
        public void setValue(int value)
        {
            this._value = value;
        }
    }
    
    TaskStage taskStage;
    
    
    public TaskStatistics(int stage)
    {
        taskStage.setValue(stage);
    }
    
    
}
