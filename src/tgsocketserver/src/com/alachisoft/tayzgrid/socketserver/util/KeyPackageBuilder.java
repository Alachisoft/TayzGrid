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

package com.alachisoft.tayzgrid.socketserver.util;

import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.common.protobuf.DSUpdatedCallbackResultProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyExceptionPackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.DSUpdatedCallbackResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValuePackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ValueProtocol;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.OperationResult;
import java.util.Iterator;
import java.util.Map;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;

public final class KeyPackageBuilder
{

    /**
     * Make a package containing quote separated keys from list
     *
     * @param keysList list of keys to be packaged
     * @return key package being constructed
     */
    public static String PackageKeys(java.util.ArrayList keyList)
    {
        StringBuilder keyPackage = new StringBuilder(keyList.size() * 256);

        for (int i = 0; i < keyList.size(); i++)
        {
            keyPackage.append((String) keyList.get(i) + "\"");
        }

        return keyPackage.toString();
    }

    /**
     * Make a package containing quote separated keys from list
     *
     * @param keysList list of keys to be packaged
     * @return key package being constructed
     */
    public static String PackageKeys(java.util.Collection keyList)
    {
        String packagedKeys = "";
        if (keyList != null && keyList.size() > 0)
        {
            StringBuilder keyPackage = new StringBuilder(keyList.size() * 256);

            java.util.Iterator ie = keyList.iterator();
            while (ie.hasNext())
            {
                keyPackage.append((String) ie.next() + "\"");
            }
            packagedKeys = keyPackage.toString();
        }
        return packagedKeys;
    }

    public static void PackageKeys(Iterator dicEnu, tangible.RefObject<String> keyPackage, tangible.RefObject<Integer> keyCount)
    {
        StringBuilder keys = new StringBuilder(1024);
        keyCount.argvalue = 0;

        Map.Entry pair;
        while (dicEnu.hasNext())
        {
            pair = (Map.Entry) dicEnu.next();
            keys.append(pair.getKey() + "\"");
            keyCount.argvalue++;
        }

        keyPackage.argvalue = keys.toString();
    }

    public static void PackageKeys(java.util.Iterator enumerator, java.util.List<ByteString> keys, String serializationContext) throws IOException
    {
        if (enumerator instanceof Iterator)
        {
            Iterator ide = (Iterator) ((enumerator instanceof Iterator) ? enumerator : null);
            Object nextValue;
            while (ide.hasNext())
            {
                nextValue = ide.next();
                keys.add(CacheKeyUtil.toByteString(nextValue, serializationContext));
            }
        }
        else
        {
            while (enumerator.hasNext())
            {
                keys.add(CacheKeyUtil.toByteString(enumerator.next(), serializationContext));
            }
        }
    }

    /**
     * Makes a key and data package form the keys and values of hashtable
     *
     * @param dic HashMap containing the keys and values to be packaged
     * @param keys Contains packaged keys after execution
     * @param data Contains packaged data after execution
     * @param currentContext Current cache
     */
    public static java.util.ArrayList<KeyValuePackageResponseProtocol.KeyValuePackageResponse> PackageKeysValues(java.util.HashMap dic, String serializationContext) throws IOException
    {
        int estimatedSize = 0;
        java.util.ArrayList<KeyValuePackageResponseProtocol.KeyValuePackageResponse> ListOfKeyPackageResponse = new java.util.ArrayList<KeyValuePackageResponseProtocol.KeyValuePackageResponse>();
        if (dic != null && dic.size() > 0)
        {
            KeyValuePackageResponseProtocol.KeyValuePackageResponse.Builder keyPackageResponse = KeyValuePackageResponseProtocol.KeyValuePackageResponse.newBuilder();

            Iterator enu = dic.entrySet().iterator();
            Map.Entry pair;
            while (enu.hasNext())
            {
                pair = (Map.Entry) enu.next();
                UserBinaryObject ubObject = (UserBinaryObject) ((((CompressedValueEntry) pair.getValue()).Value instanceof UserBinaryObject) ? ((CompressedValueEntry) pair.getValue()).Value : null);

                ValueProtocol.Value.Builder valueBuilder = ValueProtocol.Value.newBuilder();
                for (int i = 0; i < ubObject.getDataList().size(); i++)
                {
                    Object[] test = ubObject.getData();
                    valueBuilder.addData(ByteString.copyFrom((byte[]) test[i]));
                }

                keyPackageResponse.addKeys(CacheKeyUtil.toByteString(pair.getKey(), serializationContext));
                keyPackageResponse.addFlag((int) ((CompressedValueEntry) pair.getValue()).Flag.getData());
                keyPackageResponse.addValues(valueBuilder.build());

                estimatedSize = estimatedSize + ubObject.getSize();

                if (estimatedSize >= SocketServer.CHUNK_SIZE_FOR_OBJECT)
                { 
                    ListOfKeyPackageResponse.add(keyPackageResponse.build());
                    estimatedSize = 0;
                }
            }

            if (estimatedSize != 0)
            {
                ListOfKeyPackageResponse.add(keyPackageResponse.build());
            }
        }
        else
        {
            ListOfKeyPackageResponse.add(KeyValuePackageResponseProtocol.KeyValuePackageResponse.newBuilder().build());
        }

        return ListOfKeyPackageResponse;
    }

    /**
     * Makes a key and data package form the keys and values of hashtable
     *
     * @param dic HashMap containing the keys and values to be packaged
     * @param keys Contains packaged keys after execution
     * @param data Contains packaged data after execution
     * @param currentContext Current cache
     */
    public static KeyValuePackageResponseProtocol.KeyValuePackageResponse PackageKeysValues(java.util.HashMap dic, KeyValuePackageResponseProtocol.KeyValuePackageResponse keyPackageResponse, String serializationContext) throws IOException
    {
        if (dic != null && dic.size() > 0)
        {
            if (keyPackageResponse == null)
            {

                keyPackageResponse = KeyValuePackageResponseProtocol.KeyValuePackageResponse.newBuilder().build();
            }
            ;

            Iterator enu = dic.entrySet().iterator();
            Map.Entry pair;
            while (enu.hasNext())
            {
                pair = (Map.Entry) enu.next();
                keyPackageResponse.getKeysList().add(CacheKeyUtil.toByteString(pair.getKey(), serializationContext));
                keyPackageResponse.getFlagList().add((int) ((CompressedValueEntry) pair.getValue()).Flag.getData());
                UserBinaryObject ubObject = (UserBinaryObject) ((((CompressedValueEntry) pair.getValue()).Value instanceof UserBinaryObject) ? ((CompressedValueEntry) pair.getValue()).Value : null);
                ValueProtocol.Value.Builder valueBuilder = ValueProtocol.Value.newBuilder();
                ValueProtocol.Value value = valueBuilder.build();
                value.getDataList().addAll((List) ubObject.getDataList());
                keyPackageResponse.getValuesList().add(value);
            }
        }

        return keyPackageResponse;
    }

    /**
     * Makes a key and data package form the keys and values of hashtable, for bulk operations
     *
     * @param dic HashMap containing the keys and values to be packaged
     * @param keys Contains packaged keys after execution
     * @param data Contains packaged data after execution
     */
    public static void PackageKeysExceptions(java.util.HashMap dic, KeyExceptionPackageResponseProtocol.KeyExceptionPackageResponse.Builder keyExceptionPackage, String serializationContext) throws IOException
    {
        if (dic != null && dic.size() > 0)
        {
            Iterator enu = dic.entrySet().iterator();
            Map.Entry pair;
            while (enu.hasNext())
            {
                pair = (Map.Entry) enu.next();
                Exception ex = (Exception) ((pair.getValue() instanceof Exception) ? pair.getValue() : null);
                if (ex != null)
                {
                    keyExceptionPackage.addKeys(CacheKeyUtil.toByteString(pair.getKey(), serializationContext));

                    ExceptionProtocol.Exception.Builder excBuilder = ExceptionProtocol.Exception.newBuilder();
                    excBuilder.setMessage(ex.getMessage());
                    excBuilder.setException(ex.toString());
                    excBuilder.setType(ExceptionProtocol.Exception.Type.GENERALFAILURE);

                    ExceptionProtocol.Exception exc = excBuilder.build();

                    keyExceptionPackage.addExceptions(exc);
                }
                //for DS write failed operations
                if (pair.getValue() instanceof OperationResult.Status) {
                    OperationResult.Status status = (OperationResult.Status) pair.getValue();
                    if (status == OperationResult.Status.Failure || status == OperationResult.Status.FailureDontRemove) {
                        keyExceptionPackage.addKeys(CacheKeyUtil.toByteString(pair.getKey(), serializationContext));

                        ExceptionProtocol.Exception.Builder excBuilder = ExceptionProtocol.Exception.newBuilder();
                        excBuilder.setMessage(ex.getMessage());
                        excBuilder.setException(ex.toString());
                        excBuilder.setType(ExceptionProtocol.Exception.Type.GENERALFAILURE);
                        ExceptionProtocol.Exception exc = excBuilder.build();
                        keyExceptionPackage.addExceptions(exc);
                    }

                }
            }
        }
    }

    /**
     * Package keys and values where values can be Exception or not. If they are no exception, currently, 0 bytes is returned
     *
     * @param dic
     * @param keyPackage
     * @param dataPackage
     */
    public static void PackageMisc(java.util.Map dic, DSUpdatedCallbackResponseProtocol.DSUpdatedCallbackResponse.Builder results, String serializationContext) throws IOException
    {
        if (dic != null && dic.size() > 0)
        {
            Iterator enu = dic.entrySet().iterator();
            Map.Entry keyVal;
            while (enu.hasNext())
            {
                keyVal = (Map.Entry) enu.next();
                DSUpdatedCallbackResultProtocol.DSUpdatedCallbackResult.Builder resultBuilder = DSUpdatedCallbackResultProtocol.DSUpdatedCallbackResult.newBuilder();

                resultBuilder.setKey(CacheKeyUtil.toByteString(keyVal.getKey(), serializationContext));

                if (keyVal.getValue() instanceof Exception)
                {
                    resultBuilder.setSuccess(false);

                    ExceptionProtocol.Exception.Builder exBuilder = ExceptionProtocol.Exception.newBuilder();

                    exBuilder.setMessage(((Exception) keyVal.getValue()).getMessage());
                    exBuilder.setException(((Exception) keyVal.getValue()).toString());
                    exBuilder.setType(ExceptionProtocol.Exception.Type.GENERALFAILURE);

                     ExceptionProtocol.Exception ex = exBuilder.build();
                    resultBuilder.setException(ex);
                }
                else if (keyVal.getValue() instanceof OperationResult.Status)
                {
                    switch ((OperationResult.Status)keyVal.getValue())
                    {
                        case Success:
                            resultBuilder.setSuccess(true);
                            break;
                        case Failure:
                        case FailureDontRemove:
                            resultBuilder.setSuccess(false);
                            break;
                    }
                }
                DSUpdatedCallbackResultProtocol.DSUpdatedCallbackResult result = resultBuilder.build();
                results.addResult(result);
            }
        }
    }
}
