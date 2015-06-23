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

import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.common.protobuf.RecordSetProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RecordColumnProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ColumnValueProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ColumnTypeProtocol;
import com.alachisoft.tayzgrid.common.protobuf.SearchEntriesResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ColumnDataTypeProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryTypeProtocol;
import com.alachisoft.tayzgrid.common.protobuf.DictionaryItemProtocol;
import com.alachisoft.tayzgrid.common.protobuf.AverageResultProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValuePackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryResultSetProtocol;
import com.alachisoft.tayzgrid.common.protobuf.AggregateFunctionTypeProtocol;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.datastructures.ColumnDataType;
import com.alachisoft.tayzgrid.common.datastructures.RecordColumn;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.google.protobuf.ByteString;

public class SearchEnteriesResponseBuilder extends ResponseBuilderBase {
	public static java.util.List<byte[]> BuildResponse(QueryResultSet resultSet, int commandVersion, String RequestId, java.util.List<byte[]> _serializedResponse, String serializationContext) {

		long requestId = Long.parseLong(RequestId);
                QueryResultSetProtocol.QueryResultSet.Builder query = QueryResultSetProtocol.QueryResultSet.newBuilder();
		try {
			switch (commandVersion) {
				case 0: { 
					      ResponseProtocol.Response.Builder response=ResponseProtocol.Response.newBuilder();
                                                SearchEntriesResponseProtocol.SearchEntriesResponse.Builder searchEntriesResponse=SearchEntriesResponseProtocol.SearchEntriesResponse.newBuilder();
						response.setRequestId(requestId);
						searchEntriesResponse.setKeyValuePackage(com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeysValues(resultSet.getSearchEntriesResult(), searchEntriesResponse.getKeyValuePackage(), serializationContext));
						response.setResponseType(ResponseProtocol.Response.Type.SEARCH_ENTRIES);
						response.setSearchEntries(searchEntriesResponse);
						_serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
				}
					break;
				case 1: 
				case 2: { 
						switch (resultSet.getType()) {
							case AggregateFunction: {
                                                                        ResponseProtocol.Response.Builder response=ResponseProtocol.Response.newBuilder();
                                                                        SearchEntriesResponseProtocol.SearchEntriesResponse.Builder searchEntriesResponse=SearchEntriesResponseProtocol.SearchEntriesResponse.newBuilder();
									searchEntriesResponse.setQueryResultSet(QueryResultSetProtocol.QueryResultSet.newBuilder());
									response.setRequestId(requestId);



                                                                        query.setQueryType(QueryTypeProtocol.QueryType.AGGREGATE_FUNCTIONS);
                                                                        query.setAggregateFunctionType(AggregateFunctionTypeProtocol.AggregateFunctionType.valueOf(resultSet.getAggregateFunctionType().getValue()));
                                                                        query.setAggregateFunctionResult(DictionaryItemProtocol.DictionaryItem.newBuilder());

                                                                        DictionaryItemProtocol.DictionaryItem.Builder dicItem=DictionaryItemProtocol.DictionaryItem.newBuilder();
                                                                        dicItem.setKey(resultSet.getAggregateFunctionResult().getKey().toString());
                                                                        dicItem.setValue(resultSet.getAggregateFunctionResult().getValue() != null ? ByteString.copyFrom(CompactBinaryFormatter.toByteBuffer(resultSet.getAggregateFunctionResult().getValue(), "")) : null);

                                                                        query.setAggregateFunctionResult(dicItem.build());

                                                                        searchEntriesResponse.setQueryResultSet(query.build());
									response.setResponseType(ResponseProtocol.Response.Type.SEARCH_ENTRIES);
									response.setSearchEntries(searchEntriesResponse);
									_serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
							}
							break;

							case SearchEntries: {
									int sequenceId = 1;
									java.util.ArrayList<KeyValuePackageResponseProtocol.KeyValuePackageResponse> keyValuesPackageChuncks = com.alachisoft.tayzgrid.socketserver.util.KeyPackageBuilder.PackageKeysValues(resultSet.getSearchEntriesResult(), serializationContext);
                                                                        ResponseProtocol.Response.Builder response=ResponseProtocol.Response.newBuilder();
                                                                        SearchEntriesResponseProtocol.SearchEntriesResponse.Builder searchEntriesResponse=SearchEntriesResponseProtocol.SearchEntriesResponse.newBuilder();
									searchEntriesResponse.setQueryResultSet(QueryResultSetProtocol.QueryResultSet.newBuilder());
									response.setRequestId(requestId);
                                                                        query.setQueryType(QueryTypeProtocol.QueryType.SEARCH_ENTRIES);
									response.setNumberOfChuncks(keyValuesPackageChuncks.size());
									response.setResponseType(ResponseProtocol.Response.Type.SEARCH_ENTRIES);

									for (KeyValuePackageResponseProtocol.KeyValuePackageResponse package_Renamed : keyValuesPackageChuncks) {
										response.setSequenceId(sequenceId++);
                                                                                query.setSearchKeyEnteriesResult(package_Renamed);
                                                                                searchEntriesResponse.setQueryResultSet(query.build());
										response.setSearchEntries(searchEntriesResponse);
										_serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
									}
							}
							break;
                                                        case GroupByAggregateFunction:
                                                            ResponseProtocol.Response.Builder response=ResponseProtocol.Response.newBuilder();
                                                            SearchEntriesResponseProtocol.SearchEntriesResponse.Builder searchEntriesResponse=SearchEntriesResponseProtocol.SearchEntriesResponse.newBuilder();
                                                            searchEntriesResponse.setQueryResultSet(QueryResultSetProtocol.QueryResultSet.newBuilder());
                                                            query.setQueryType(QueryTypeProtocol.QueryType.GROUPBY_AGGREGATE_FUNCTIONS);
                                                            RecordSetProtocol.RecordSet.Builder groupByresult=RecordSetProtocol.RecordSet.newBuilder();
                                                            groupByresult.setRowCount(resultSet.getGroupByResult().getRowCount());
                                                            
                                                            for(com.alachisoft.tayzgrid.common.datastructures.RecordColumn col : resultSet.getGroupByResult().getData().values())
                                                            {
                                                                groupByresult.addKeys(col.getName());
                                                                RecordColumnProtocol.RecordColumn.Builder protoColumn=RecordColumnProtocol.RecordColumn.newBuilder();
                                                                protoColumn.setName(col.getName());
                                                                protoColumn.setColumnType(ColumnTypeProtocol.ColumnType.valueOf(col.getType().getValue()));
                                                                protoColumn.setIsHidden(col.getIsHidden());
                                                                protoColumn.setDataType(ColumnDataTypeProtocol.ColumnDataType.valueOf(col.getDataType().getValue()));
                                                                protoColumn.setAggregateFunctionType(AggregateFunctionTypeProtocol.AggregateFunctionType.valueOf(col.getAggregateFunctionType().getValue()));
                                                                protoColumn.addAllKeys(col.getData().keySet());
                                                                
                                                                for(Object obj : col.getData().values())
                                                                {
                                                                    ColumnValueProtocol.ColumnValue.Builder columnValue=ColumnValueProtocol.ColumnValue.newBuilder();

                                                                    if(col.getDataType()==com.alachisoft.tayzgrid.common.datastructures.ColumnDataType.AverageResult)
                                                                    {
                                                                        com.alachisoft.tayzgrid.common.queries.AverageResult avgResult = (com.alachisoft.tayzgrid.common.queries.AverageResult)obj;
                                                                        AverageResultProtocol.AverageResult.Builder protoAvgResult=AverageResultProtocol.AverageResult.newBuilder();
                                                                        protoAvgResult.setSum(avgResult.getSum().toString());
                                                                        protoAvgResult.setCount(avgResult.getCount().toString());
                                                                        
                                                                        columnValue.setAvgResult(protoAvgResult.build());
                                                                    }
                                                                    else
                                                                    {
                                                                        columnValue.setStringValue(com.alachisoft.tayzgrid.common.datastructures.RecordSet.GetString(obj, col.getDataType()));
                                                                    }
                                                                    protoColumn.addValues(columnValue);
                                                                }
                                                                groupByresult.addColumns(protoColumn);
                                                            }
                                                            //@20mar2015 proto missing- also group by to be shifted in reader
                                                            //query.setGroupByAggregateFunctionResult(groupByresult);
                                                            searchEntriesResponse.setQueryResultSet(query); 
                                                            response.setRequestId(requestId);
                                                            response.setResponseType(ResponseProtocol.Response.Type.SEARCH_ENTRIES);
                                                            response.setSearchEntries(searchEntriesResponse);
                                                            _serializedResponse.add(ResponseHelper.SerializeResponse(response.build()));
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

		return _serializedResponse;
	}
}
