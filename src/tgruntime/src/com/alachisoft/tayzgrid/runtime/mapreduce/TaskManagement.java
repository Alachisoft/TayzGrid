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

import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.Enumeration;

public interface TaskManagement {

    Enumeration getTaskEnumerator(String taskId, short callbackId) throws OperationFailedException;

    void cancelTask(String taskId) throws OperationFailedException;

    public TaskStatus getTaskProgress(String taskId) throws GeneralFailureException, OperationFailedException, ConnectionException, AggregateException;
}
