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

package com.alachisoft.tayzgrid.runtime.cacheloader;

import java.util.HashMap;
import java.util.LinkedHashMap;

public interface CacheLoader {

       /**
	* Perform tasks like allocating resources or acquiring connections etc.
        * @param parameters - Startup parameters defined in the configuration
	*/

        void init(HashMap parameters) throws Exception;

        /**
         * Responsible for loading of items.
         * @param data - key used to reference object
         * @param index - starting index for the collection
         * @return object
        */
        boolean loadNext(LinkedHashMap data, LoaderState state) throws Exception;

        /**
         * Perform tasks associated with freeing, releasing, or resetting resources.
        **/
        void dispose() throws Exception;

}
