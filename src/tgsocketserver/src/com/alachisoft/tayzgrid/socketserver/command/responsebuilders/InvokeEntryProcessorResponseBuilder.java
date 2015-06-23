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
package com.alachisoft.tayzgrid.socketserver.command.responsebuilders;

import com.alachisoft.tayzgrid.common.protobuf.BulkGetResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InvokeEPKeyValuePackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InvokeEntryProcessorResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValuePackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.processor.TayzGridEntryProcessorResult;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
//Dated: April 30, 2015

/**
 * This class is responsible for providing the responses based on the command
 * Version specified. Main role of this class is to provide the backward
 * compatibility. As different version of command can be processed by the same
 * server. In that case the response should be in the form understandable by the
 * client who sent the command.
 *
 * This class only processes the different versions of BulkGet command
 */
public class InvokeEntryProcessorResponseBuilder extends ResponseBuilderBase {

    public static java.util.List<byte[]> BuildResponse(java.util.HashMap getResult, int commandVersion, String RequestId, java.util.List<byte[]> _serializedResponse, String intendedRecepient, String serializationContext) throws IOException {
        long requestId = Long.parseLong(RequestId);

        java.util.ArrayList<InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse> keyValuesPackageChuncks = InvokeEPPackageKeysValues(getResult, serializationContext);
        int sequenceId = 1;
        InvokeEntryProcessorResponseProtocol.InvokeEntryProcessorResponse.Builder invokeEPResponse = InvokeEntryProcessorResponseProtocol.InvokeEntryProcessorResponse.newBuilder();
        ResponseProtocol.Response.Builder response = ResponseProtocol.Response.newBuilder();
        response.setRequestId(requestId);
        response.setNumberOfChuncks(keyValuesPackageChuncks.size());
        response.setResponseType(ResponseProtocol.Response.Type.INVOKE_ENTRYPROCESSOR);
        for (InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse package_Renamed : keyValuesPackageChuncks) {
            response.setSequenceId(sequenceId++);
            invokeEPResponse.setKeyValuePackage(package_Renamed);
            response.setInvokeEntryProcessorResponse(invokeEPResponse);
            _serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
        }
        return _serializedResponse;
    }
    
      /**
     * Makes a key and data package form the keys and values of hashtable
     *
     * @param dic HashMap containing the keys and values to be packaged
     * @param serializationContext
     * @return 
     * @throws java.io.IOException
     */
    public static java.util.ArrayList<InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse> InvokeEPPackageKeysValues(java.util.HashMap dic, String serializationContext) throws IOException
    {
        int estimatedSize = 0;
        java.util.ArrayList<InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse> ListOfKeyPackageResponse = new java.util.ArrayList<InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse>();
        if (dic != null && dic.size() > 0)
        {
            InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse.Builder keyPackageResponse = InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse.newBuilder();

            Iterator enu = dic.entrySet().iterator();
            Map.Entry pair;
            while (enu.hasNext())
            {
                pair = (Map.Entry) enu.next();
                TayzGridEntryProcessorResult tgEntryProcessorResult = (TayzGridEntryProcessorResult) ((TayzGridEntryProcessorResult) pair.getValue()!=null?pair.getValue(): null);

                keyPackageResponse.addKeys(CacheKeyUtil.toByteString(pair.getKey(), serializationContext));
                byte[] serializedResult=CacheKeyUtil.Serialize(tgEntryProcessorResult, serializationContext);
                keyPackageResponse.addValues(ByteString.copyFrom(serializedResult));

                estimatedSize = estimatedSize + serializedResult.length;

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
            ListOfKeyPackageResponse.add(InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse.newBuilder().build());
        }

        return ListOfKeyPackageResponse;
    }
}
