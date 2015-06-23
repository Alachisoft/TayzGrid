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

package com.alachisoft.tayzgrid.caching;


import com.alachisoft.tayzgrid.common.ServicePropValues;

public class GracefulTimeout {

    public static String GetGracefulShutDownTimeout(tangible.RefObject<Integer> shutdownTimeout, tangible.RefObject<Integer> blockTimeout) {
        shutdownTimeout.argvalue = 180;
        blockTimeout.argvalue = 3;
        String exceptionMsgST = null;

        try {
            if (ServicePropValues.Cache_GracefullShutdownTimeout != null && !ServicePropValues.Cache_GracefullShutdownTimeout.isEmpty()) {
                shutdownTimeout.argvalue = Integer.parseInt(ServicePropValues.Cache_GracefullShutdownTimeout);
            }

        } catch (Exception ex) {
            exceptionMsgST = "Invalid value is assigned to CacheServer.GracefullShutdownTimeout. Reassigning it to default value.(180 seconds)";
            shutdownTimeout.argvalue = 180;
        }
        try {
            if (ServicePropValues.Cache_BlockingActivityTimeout != null && !ServicePropValues.Cache_BlockingActivityTimeout.isEmpty()) {
                blockTimeout.argvalue = Integer.parseInt(ServicePropValues.Cache_BlockingActivityTimeout);
            }
        } catch (Exception ex) {
            exceptionMsgST = "Invalid value is assigned to CacheServer.BlockingActivityTimeout. Reassigning it to default value.(3 seconds)";
            blockTimeout.argvalue = 3;
        }

        if (shutdownTimeout.argvalue <= 0) {
            exceptionMsgST = "Invalid value is assigned to CacheServer.GracefullShutdownTimeout. Reassigning it to default value.(180 seconds)";
            shutdownTimeout.argvalue = 180;
        }

        if (blockTimeout.argvalue <= 0) {
            exceptionMsgST = "0 or negtive value is assigned to CacheServer.BlockingActivityTimeout. Reassigning it to default value.(3 seconds)";
            blockTimeout.argvalue = 3;
        }


        if (blockTimeout.argvalue >= shutdownTimeout.argvalue) {
            exceptionMsgST = "CacheServer.BlockingActivityTimeout is greater than or equal to CacheServer.GracefullShutdownTimeout. Reassigning both to default value.";
            blockTimeout.argvalue = 3;
            shutdownTimeout.argvalue = 180;
        }

        return exceptionMsgST;
    }


    public final class RefObject<T> {

        public T argvalue;

        public RefObject(T refarg) {
            argvalue = refarg;
        }
    }
}
