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

package com.alachisoft.tayzgrid.common.util;

public final class EnumerationPointerConversionUtil
{

    public static com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer GetFromProtobufEnumerationPointer(com.alachisoft.tayzgrid.common.protobuf.EnumerationPointerProtocol.EnumerationPointer pointer)
    {
        com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer enumerationPointer = new com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer(pointer.getId(), pointer.getChunkId());
        enumerationPointer.setDisposable(pointer.getIsDisposed());
        return enumerationPointer;
    }

    public static com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer GetFromProtobufGroupEnumerationPointer(com.alachisoft.tayzgrid.common.protobuf.GroupEnumerationPointerProtocol.GroupEnumerationPointer pointer)
    {
        com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer enumerationPointer = new com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer(pointer.getId(), pointer.getChunkId(), pointer.getGroup(), pointer.getSubGroup());
        return enumerationPointer;
    }

    public static com.alachisoft.tayzgrid.common.protobuf.EnumerationPointerProtocol.EnumerationPointer ConvertToProtobufEnumerationPointer(com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer pointer)
    {
        return com.alachisoft.tayzgrid.common.protobuf.EnumerationPointerProtocol.EnumerationPointer.newBuilder()
                .setChunkId(pointer.getChunkId())
                .setId(pointer.getId())
                .setIsDisposed(pointer.isDisposable())
                .build();
    }

    public static com.alachisoft.tayzgrid.common.protobuf.GroupEnumerationPointerProtocol.GroupEnumerationPointer ConvertToProtobufGroupEnumerationPointer(com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer pointer)
    {
        return com.alachisoft.tayzgrid.common.protobuf.GroupEnumerationPointerProtocol.GroupEnumerationPointer.newBuilder()
                .setId(pointer.getId())
                .setChunkId(pointer.getChunkId())
                .setGroup(pointer.getGroup())
                .setSubGroup(pointer.getSubGroup())
                .build();
    }
}
