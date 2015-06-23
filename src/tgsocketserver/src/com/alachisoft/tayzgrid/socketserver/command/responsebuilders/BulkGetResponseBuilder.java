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

import com.alachisoft.tayzgrid.common.protobuf.KeyValuePackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkGetResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;

import java.io.IOException;

//Dated: July 20, 2011
/**
 This class is responsible for providing the responses based on the command Version specified.
 Main role of this class is to provide the backward compatibility. As different version of command can
 be processed by the same server. In that case the response should be in the form understandable by the
 client who sent the command.

 This class only processes the different versions of BulkGet command
*/
public class BulkGetResponseBuilder extends ResponseBuilderBase {
	public static java.util.List<byte[]> BuildResponse(java.util.HashMap getResult, int commandVersion, String RequestId, java.util.List<byte[]> _serializedResponse, String intendedRecepient, String serializationContext) throws IOException {
		long requestId = Long.parseLong(RequestId);
		switch (commandVersion) {
			case 0: { 
                                BulkGetResponseProtocol.BulkGetResponse.Builder bulkGetResponse=BulkGetResponseProtocol.BulkGetResponse.newBuilder();
                                ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setBulkGet(bulkGetResponse.build())
				                                                                          .setRequestId(requestId)
                                                                                                          .setIntendedRecipient(intendedRecepient)
                                                                                                          .setResponseType(ResponseProtocol.Response.Type.GET_BULK).build();
                                bulkGetResponse.setKeyValuePackage(com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeysValues(getResult, bulkGetResponse.getKeyValuePackage(), serializationContext));
				_serializedResponse.add(ResponseHelper.SerializeResponse(response));
			}
			break;
			case 1: { 
				java.util.ArrayList<KeyValuePackageResponseProtocol.KeyValuePackageResponse> keyValuesPackageChuncks = com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeysValues(getResult, serializationContext);
				int sequenceId = 1;
                                BulkGetResponseProtocol.BulkGetResponse.Builder bulkGetResponse=BulkGetResponseProtocol.BulkGetResponse.newBuilder();
                                ResponseProtocol.Response.Builder response= ResponseProtocol.Response.newBuilder();
				response.setRequestId(requestId);
				response.setNumberOfChuncks(keyValuesPackageChuncks.size());
				response.setResponseType(ResponseProtocol.Response.Type.GET_BULK);
				for (KeyValuePackageResponseProtocol.KeyValuePackageResponse package_Renamed : keyValuesPackageChuncks) {
					response.setSequenceId(sequenceId++);
					bulkGetResponse.setKeyValuePackage(package_Renamed);
					response.setBulkGet(bulkGetResponse);
					_serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
				}
			}
			break;

		}
		return _serializedResponse;
	}
}
