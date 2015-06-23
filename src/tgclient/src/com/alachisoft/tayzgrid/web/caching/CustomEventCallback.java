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

package com.alachisoft.tayzgrid.web.caching;


public class CustomEventCallback {

    /** User didnot specified any callback. */
    public static final int NONE = 0;

    /** User specified only update callback. */
    public static final int UPDATE = 1;

    /** User specified only remove callback. */
    public static final int REMOVE = 2;

    /** User specified both remove and update callbacks.. */
    public static final int BOTH = 3;
}


//~ Formatted by Jindent --- http://www.jindent.com
