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


 public enum CommandType {

    ADD(1),
    ADD_BULK(2),
    GET_BULK(3),
    INSERT_BULK(4),
    REMOVE_BULK(5),
    CLEAR(6),
    CONTAINS(7),
    COUNT(8),
    DISPOSE(9),
    GET_CACHE_ITEM(10),
    GET(11),
    GET_ENUMERATOR(12),
    GET_GROUP(13),
    GET_HASHMAP(14),
    GET_OPTIMAL_SERVER(15),
    GET_THRESHOLD_SIZE(16),
    GET_TYPEINFO_MAP(17),
    INIT(18),
    INSERT(19),
    RAISE_CUSTOM_EVENT(20),
    REGISTER_KEY_NOTIF(21),
    REGISTER_NOTIF(22),
    REMOVE(23),
    REMOVE_GROUP(24),
    SEARCH(25),
    GET_TAG(26),
    LOCK(27),
    UNLOCK(28),
    ISLOCKED(29),
    LOCK_VERIFY(30),
    UNREGISTER_KEY_NOTIF(31),
    UNREGISTER_BULK_KEY_NOTIF(32),
    REGISTER_BULK_KEY_NOTIF(33),
    GET_LOGGING_INFO(34),
    REMOVE_BY_TAG(35),
    GET_KEYS_TAG(36),
    DELETE_BULK(37),
    DELETE(38),
    GET_NEXT_CHUNK(39),
    GETGROUP_NEXT_CHUNK(40),
    ADDATTRIBUTES(41),
    GET_RUNNING_SERVERS(42),
    SYNC_EVENTS(43),
    DELETEQUERY(44),
    GETCACHEBINDING(45),
    INVOKE_ENTRYPROCESSOR(46),
    MAPREDUCE_TASK(47),
    MAPREDUCE_TASK_CALLBACK(48),
    MAPREDUCE_TASK_CANCEL(49),
    GET_RUNNING_TASKS(50),
    GET_TASK_PROGRESS(51),
    GET_NEXT_RECORD(52),
    GET_TASK_ENUMERATOR(53),
    GET_CACHE_CONFIG(54);

	private final int value;


    CommandType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
 }
