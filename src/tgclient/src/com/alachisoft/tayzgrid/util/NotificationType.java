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

package com.alachisoft.tayzgrid.util;

public class NotificationType {

    private static final int REG_NOTIF_FIRST = 1;
    public static final int REGISTER_ADD = 1;
    public static final int REGISTER_INSERT = 2;
    public static final int REGISTER_REMOVE = 3;
    public static final int REGISTER_CLEAR = 4;
    public static final int REGISTER_CUSTOM = 5;
    public static final int REGISTER_MEMBER_JOINED = 6;
    public static final int REGISTER_MEMBER_LEFT = 7;
    public static final int REGISTER_CACHE_STOPPED = 8;
    public static final int REGISTER_HASHMAP_RECIEVED = 9;


    private static final int UNREG_NOTIF_FIRST = 100;
    public static final int UNREGISTER_ADD = 100;
    public static final int UNREGISTER_INSERT = 101;
    public static final int UNREGISTER_REMOVE = 102;
    public static final int UNREGISTER_CLEAR = 103;
    public static final int UNREGISTER_CUSTOM = 104;
    public static final int UNREGISTER_MEMBER_JOINED = 105;
    public static final int UNREGISTER_MEMBER_LEFT = 106;
    public static final int UNREGISTER_CACHE_STOPPED = 107;

}
