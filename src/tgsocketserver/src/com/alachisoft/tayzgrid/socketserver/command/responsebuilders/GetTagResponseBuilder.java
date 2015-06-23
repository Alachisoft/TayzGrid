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
import com.alachisoft.tayzgrid.common.protobuf.GetTagResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;

import java.io.IOException;

//Dated: July 20, 2011
/**
 This class is responsible for providing the responses based on the command Version specified.
 Main role of this class is to provide the backward compatibility. As different version of command can
 be processed by the same server. In that case the response should be in the form understandable by the
 client who sent the command.

 This class only processes the different versions of GetByTag command
*/
public class GetTagResponseBuilder extends ResponseBuilderBase {
	public static java.util.List<byte[]> BuildResponse(java.util.HashMap getTagResult, int commandVersion, String RequestId, java.util.List<byte[]> _serializedResponse, String serializationContext) throws IOException {
		long requestId = Long.parseLong(RequestId);
		switch (commandVersion) {
			case 0: { 
                                        ResponseProtocol.Response.Builder response=ResponseProtocol.Response.newBuilder();
                                        GetTagResponseProtocol.GetTagResponse.Builder getTagResponse=GetTagResponseProtocol.GetTagResponse.newBuilder();
					response.setRequestId(requestId);
					getTagResponse.setKeyValuePackage(com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeysValues(getTagResult, getTagResponse.getKeyValuePackage(), serializationContext));
					response.setResponseType(ResponseProtocol.Response.Type.GET_TAG);
					response.setGetTag(getTagResponse);
					_serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
			}
				break;
			case 1: { 
					java.util.ArrayList<KeyValuePackageResponseProtocol.KeyValuePackageResponse> keyValuesPackageChuncks = com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeysValues(getTagResult, serializationContext);
					int sequenceId = 1;
                                        ResponseProtocol.Response.Builder response=ResponseProtocol.Response.newBuilder();
                                        GetTagResponseProtocol.GetTagResponse.Builder getTagResponse=GetTagResponseProtocol.GetTagResponse.newBuilder();
					response.setRequestId(requestId);
					response.setNumberOfChuncks(keyValuesPackageChuncks.size());
					response.setResponseType(ResponseProtocol.Response.Type.GET_TAG);

					for (KeyValuePackageResponseProtocol.KeyValuePackageResponse package_Renamed : keyValuesPackageChuncks) {
						response.setSequenceId(sequenceId++);
						getTagResponse.setKeyValuePackage(package_Renamed);
						response.setGetTag(getTagResponse);
						_serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
					}
			}
				break;

		}
		return _serializedResponse;
	}

}
