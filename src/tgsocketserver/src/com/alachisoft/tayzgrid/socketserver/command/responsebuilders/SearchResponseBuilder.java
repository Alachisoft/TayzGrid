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

import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.common.protobuf.AggregateFunctionTypeProtocol;
import com.alachisoft.tayzgrid.common.protobuf.DictionaryItemProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryResultSetProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryTypeProtocol;
import com.alachisoft.tayzgrid.common.protobuf.SearchResponseProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
public class SearchResponseBuilder extends ResponseBuilderBase {
	public static SearchResponseProtocol.SearchResponse BuildResponse(QueryResultSet resultSet, int commandVersion, String serializationContext) {
		SearchResponseProtocol.SearchResponse.Builder searchResponse = SearchResponseProtocol.SearchResponse.newBuilder();
		try {
                    QueryResultSetProtocol.QueryResultSet.Builder query = QueryResultSetProtocol.QueryResultSet.newBuilder();
			switch (commandVersion) {
				case 0: { 
						com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeys(resultSet.getSearchKeysResult().iterator(), searchResponse.getKeysList(), serializationContext);
				}
					break;
				case 1: 
				case 2: { 

						switch (resultSet.getType()) {
							case AggregateFunction:
                                                                query.setQueryType(QueryTypeProtocol.QueryType.AGGREGATE_FUNCTIONS);
                                                                query.setAggregateFunctionType(AggregateFunctionTypeProtocol.AggregateFunctionType.valueOf(resultSet.getAggregateFunctionType().getValue()));
                                                                query.setAggregateFunctionResult(DictionaryItemProtocol.DictionaryItem.newBuilder());
                                                                DictionaryItemProtocol.DictionaryItem.Builder dicItem=DictionaryItemProtocol.DictionaryItem.newBuilder();
                                                                dicItem.setKey(resultSet.getAggregateFunctionResult().getKey().toString());
                                                                if(resultSet.getAggregateFunctionResult().getValue() != null)
                                                                    dicItem.setValue(ByteString.copyFrom(CompactBinaryFormatter.toByteBuffer(resultSet.getAggregateFunctionResult().getValue(), "")));
                                                                query.setAggregateFunctionResult(dicItem.build());
                                                                searchResponse.setQueryResultSet(query.build());


								break;

							case SearchKeys:
                                                            query.setQueryType(QueryTypeProtocol.QueryType.SEARCH_KEYS);
                                                            for (int i = 0; i < resultSet.getSearchKeysResult().size(); i++)
                                                            {
                                                                query.addSearchKeyResults(CacheKeyUtil.toByteString(resultSet.getSearchKeysResult().get(i), serializationContext));
                                                            }
                                                            searchResponse.setQueryResultSet(query.build());
                                                            break;
						}
				}
					break;
				default: {
						throw new Exception("Unsupported Command Version");
				}
			}
		} catch (Exception ex) {
			if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
				SocketServer.getLogger().getCacheLog().Error(ex.toString());
				if (resultSet == null) {
					SocketServer.getLogger().getCacheLog().Error("QueryResultSet is null");
				} else if (resultSet.getAggregateFunctionResult().getKey() == null) {
					SocketServer.getLogger().getCacheLog().Error("QueryResultSet.AggregateFunctionResult.Key is null");
				} else if (resultSet.getAggregateFunctionResult().getValue() == null) {
					SocketServer.getLogger().getCacheLog().Error("QueryResultSet.AggregateFunctionResult.Value is null");
				}
			}
		}

		return searchResponse.build();
	}
}
