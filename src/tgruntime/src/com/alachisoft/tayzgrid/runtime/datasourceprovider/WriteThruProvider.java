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


package com.alachisoft.tayzgrid.runtime.datasourceprovider;

import java.util.HashMap;

public interface WriteThruProvider {

    /// <summary>
    /// Perform tasks like allocating resources or acquiring connections etc.
    /// </summary>
    /// <param name="parameters">Startup paramters defined in the configuration</param>
    /// <param name="cacheId">cache name</param>
    void init(HashMap parameters, String cacheId) throws Exception;
    /// <summary>
    /// Responsible for atomic write operations on data source.
    /// </summary>
    /// <param name="operation">write operation to be applied on data source</param>
    /// <returns>failed operations, null otherwise</returns>

    OperationResult writeToDataSource(WriteOperation operation)throws Exception;;
    /// <summary>
    /// Responsible for bulk write operations on data source.
    /// </summary>
    /// <param name="operation">array of write operations to be applied on data source</param>
    /// <returns>array of failed operations</returns>

    OperationResult[] writeToDataSource(WriteOperation[] operations)throws Exception;;
    /// <summary>
    /// Perform tasks associated with freeing, releasing, or resetting resources.
    /// </summary>

    void dispose()throws Exception;;

}
