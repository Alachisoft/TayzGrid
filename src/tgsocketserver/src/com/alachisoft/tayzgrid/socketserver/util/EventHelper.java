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

import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.AsyncOpCode;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.EventCacheEntry;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.protobuf.AsyncOperationCompletedCallbackResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CacheClearedEventResponseProtocol.CacheClearedEventResponse;
import com.alachisoft.tayzgrid.common.protobuf.CacheStoppedEventResponseProtocol.CacheStoppedEventResponse;
import com.alachisoft.tayzgrid.common.protobuf.CustomEventResponseProtocol.CustomEventResponse;
import com.alachisoft.tayzgrid.common.protobuf.DSUpdatedCallbackResponseProtocol.DSUpdatedCallbackResponse;
import com.alachisoft.tayzgrid.common.protobuf.EventCacheItemProtocol.EventCacheItem;
import com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ItemAddedEventResponseProtocol.ItemAddedEventResponse;
import com.alachisoft.tayzgrid.common.protobuf.ItemRemoveCallbackResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ItemRemoveCallbackResponseProtocol.ItemRemoveCallbackResponse;
import com.alachisoft.tayzgrid.common.protobuf.ItemRemovedEventResponseProtocol.ItemRemovedEventResponse;
import com.alachisoft.tayzgrid.common.protobuf.ItemUpdatedCallbackResponseProtocol.ItemUpdatedCallbackResponse;
import com.alachisoft.tayzgrid.common.protobuf.ItemUpdatedEventResponseProtocol.ItemUpdatedEventResponse;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.protobuf.TaskCallbackResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskCallbackResponseProtocol.TaskCallbackResponse;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class EventHelper
{
	public static AsyncOperationCompletedCallbackResponseProtocol.AsyncOperationCompletedCallbackResponse.Builder GetAsyncOpCompletedResponse(ClientManager clientManager, AsyncCallbackInfo cbInfo, Object opResult, Object opCode, Object key, String serializationContext) throws IOException
	{
            AsyncOperationCompletedCallbackResponseProtocol.AsyncOperationCompletedCallbackResponse.Builder asyncOperationCompleted = AsyncOperationCompletedCallbackResponseProtocol.AsyncOperationCompletedCallbackResponse.newBuilder();
            switch ((AsyncOpCode)opCode)
            {
                case Add :
                case Remove:
                case Update:
                    asyncOperationCompleted.setKey(CacheKeyUtil.toByteString(key, serializationContext));
                    break;
            }

		asyncOperationCompleted.setRequestId(cbInfo.getRequestID());

		if (opResult instanceof Exception)
		{
			com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Builder exc = ExceptionProtocol.Exception.newBuilder();
			exc.setMessage(((Exception)opResult).getMessage());
			exc.setException(((Exception)opResult).toString());
			exc.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.GENERALFAILURE);

			asyncOperationCompleted.setExc(exc);
			asyncOperationCompleted.setSuccess(false);
		}
		else
		{
			asyncOperationCompleted.setSuccess(true);
		}

		return asyncOperationCompleted;
	}

	public static DSUpdatedCallbackResponse.Builder GetDSUPdateCallbackResponse(short id, OpCode opCode, Object result, String serializationContext) throws IOException
	{
            DSUpdatedCallbackResponse.Builder dsUpdatedCallback = com.alachisoft.tayzgrid.common.protobuf.DSUpdatedCallbackResponseProtocol.DSUpdatedCallbackResponse.newBuilder();            
            dsUpdatedCallback.setCallbackId(id);
            dsUpdatedCallback.setOpCode((int)opCode.getValue());
            KeyPackageBuilder.PackageMisc((java.util.HashMap)((result instanceof java.util.HashMap) ? result : null), dsUpdatedCallback, serializationContext);
            
            return dsUpdatedCallback;
	}

	public static  ItemRemoveCallbackResponse.Builder GetItemRemovedCallbackResponse(EventContext eventContext, short id, Object key, UserBinaryObject value, BitSet flag, ItemRemoveReason reason, EventDataFilter dataFilter, String serializationContext) throws IOException
	{
            
            ItemRemoveCallbackResponse.Builder itemRemovedCallback=ItemRemoveCallbackResponseProtocol.ItemRemoveCallbackResponse.newBuilder();

		itemRemovedCallback.setKey(CacheKeyUtil.toByteString(key, serializationContext));
		itemRemovedCallback.setCallbackId(id);
		itemRemovedCallback.setItemRemoveReason((int)reason.getValue());
		itemRemovedCallback.setFlag(flag != null? (int)flag.getData() : 0);
		itemRemovedCallback.setDataFilter((short)dataFilter.getValue());
		com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventID=com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.newBuilder();
                
		
                UserBinaryObject binaryObject = eventContext.getItem() != null ?(UserBinaryObject) eventContext.getItem().getValue() : null;
                if (binaryObject != null)
		{
                    itemRemovedCallback.addAllValue(getByteStringList(binaryObject.getDataList()));
		}

		if (eventContext != null)
		{
			eventID.setEventUniqueId(eventContext.getEventID().getEventUniqueID());
			eventID.setOperationCounter(eventContext.getEventID().getOperationCounter());
			eventID.setEventCounter(eventContext.getEventID().getEventCounter());
			if (eventContext.getItem() != null)
			{
				itemRemovedCallback.setFlag(eventContext.getItem().getFlags().getData());
			}
                        
                        EventCacheItem.Builder eventItem=ConvertToEventItem(eventContext.getItem(), EventDataFilter.forValue(dataFilter.getValue()));
			
                        if(eventItem!=null)
                        {
                            if(!eventItem.getValueList().isEmpty()) eventItem.clearValue();
                            eventID.setItem(eventItem);
                        }
                        
		}
                itemRemovedCallback.setEventId(eventID);

		return itemRemovedCallback;
	}

	public static ItemUpdatedCallbackResponse.Builder GetItemUpdatedCallbackResponse(EventContext eventContext, Object key, short callbackid, EventDataFilter dataFilter, String serializationContext) throws IOException
	{
		ItemUpdatedCallbackResponse.Builder itemUpdatedCallback=com.alachisoft.tayzgrid.common.protobuf.ItemUpdatedCallbackResponseProtocol.ItemUpdatedCallbackResponse.newBuilder();

		itemUpdatedCallback.setKey(CacheKeyUtil.toByteString(key, serializationContext));
		itemUpdatedCallback.setCallbackId(callbackid);
		itemUpdatedCallback.setDataFilter((short)dataFilter.getValue());
		
                com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventID=com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.newBuilder();
                
                if (eventContext != null)
		{
			eventID.setEventUniqueId(eventContext.getEventID().getEventUniqueID());
			eventID.setOperationCounter(eventContext.getEventID().getOperationCounter());
			eventID.setEventCounter(eventContext.getEventID().getEventCounter());

                         EventCacheItem.Builder eventItem=ConvertToEventItem(eventContext.getItem(), dataFilter);
                         
                         if(eventItem!=null)
                             eventID.setItem(eventItem);
                         
                         EventCacheItem.Builder oldEventItem=ConvertToEventItem(eventContext.getOldItem(), dataFilter);
                         
                         if(oldEventItem!=null)
                             eventID.setOldItem(oldEventItem);
		}
                itemUpdatedCallback.setEventId(eventID);
		return itemUpdatedCallback;
	}

	public static CacheStoppedEventResponse.Builder GetCacheStoppedEventResponse(String cacheId)
	{
		CacheStoppedEventResponse.Builder cacheStopped=com.alachisoft.tayzgrid.common.protobuf.CacheStoppedEventResponseProtocol.CacheStoppedEventResponse.newBuilder();
		cacheStopped.setCacheId(cacheId);

		return cacheStopped;
	}

	public static CustomEventResponse.Builder GetCustomEventResponse(byte[] key, byte[] value)
	{
		CustomEventResponse.Builder customeEventRespone = com.alachisoft.tayzgrid.common.protobuf.CustomEventResponseProtocol.CustomEventResponse.newBuilder();
                
		customeEventRespone.setKey(ByteString.copyFrom(key));
		customeEventRespone.setValue(ByteString.copyFrom(value));

		return customeEventRespone;
	}

	public static ItemAddedEventResponse.Builder GetItemAddedEventResponse(EventContext eventContext, Object key, EventDataFilter datafilter, String serializationContext) throws IOException
	{
		ItemAddedEventResponse.Builder itemAdded = com.alachisoft.tayzgrid.common.protobuf.ItemAddedEventResponseProtocol.ItemAddedEventResponse.newBuilder();
		itemAdded.setKey(CacheKeyUtil.toByteString(key, serializationContext));

		com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventID=com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.newBuilder();
		if (eventContext != null)
		{
			eventID.setEventUniqueId(eventContext.getEventID().getEventUniqueID());
			eventID.setOperationCounter(eventContext.getEventID().getOperationCounter());
			eventID.setEventCounter(eventContext.getEventID().getEventCounter());

			if (eventContext != null && eventContext.getItem() != null && eventContext.getItem().getFlags() != null)
			{
				itemAdded.setFlag(eventContext.getItem().getFlags().getData());
			}
                        
                        EventCacheItem.Builder eventItem=ConvertToEventItem(eventContext.getItem(), datafilter);
                       
                        if(eventItem!=null)
                            eventID.setItem(eventItem);
		}
                itemAdded.setEventId(eventID);
		return itemAdded;
	}

	public static ItemRemovedEventResponse.Builder GetItemRemovedEventResponse(EventContext eventContext, Object key, EventDataFilter datafilter, BitSet flag, ItemRemoveReason reason, UserBinaryObject value, String serializationContext) throws IOException
	{
		ItemRemovedEventResponse.Builder itemRemoved =com.alachisoft.tayzgrid.common.protobuf.ItemRemovedEventResponseProtocol.ItemRemovedEventResponse.newBuilder();

		itemRemoved.setKey(CacheKeyUtil.toByteString(key, serializationContext));

		itemRemoved.setItemRemoveReason((int)reason.getValue());

		//value sent seperately to support old clients
		if (eventContext.getItem() != null && eventContext.getItem().getValue() != null)
		{                                        
                     itemRemoved.setFlag(eventContext.getItem().getFlags().getData());
                     itemRemoved.addAllValue(getByteStringList(((UserBinaryObject)eventContext.getItem().getValue()).getDataList()));                     
                }                
                com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventId=com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.newBuilder();
                
		if (eventContext != null)
		{
			eventId.setEventUniqueId(eventContext.getEventID().getEventUniqueID());
			eventId.setOperationCounter(eventContext.getEventID().getOperationCounter());
			eventId.setEventCounter(eventContext.getEventID().getEventCounter());
                        
                        EventCacheItem.Builder eventItem=ConvertToEventItem(eventContext.getItem(), datafilter);
                        
                        if(eventItem!=null)
                        {
                            if(!eventItem.getValueList().isEmpty()) eventItem.clearValue();
                            eventId.setItem(eventItem);
                        }

		}
                itemRemoved.setEventId(eventId);

		return itemRemoved;
	}

	public static ItemUpdatedEventResponse.Builder GetItemUpdatedEventResponse(EventContext eventContext, Object key, EventDataFilter datafilter, String serializationContext) throws IOException
	{
		ItemUpdatedEventResponse.Builder itemUpdated =com.alachisoft.tayzgrid.common.protobuf.ItemUpdatedEventResponseProtocol.ItemUpdatedEventResponse.newBuilder();

		itemUpdated.setKey(CacheKeyUtil.toByteString(key, serializationContext));
                
                com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventId=com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.newBuilder();
		
                if (eventContext != null)
		{
			eventId.setEventUniqueId(eventContext.getEventID().getEventUniqueID());
			eventId.setOperationCounter(eventContext.getEventID().getOperationCounter());
			eventId.setEventCounter(eventContext.getEventID().getEventCounter());

			if (eventContext != null && eventContext.getItem() != null && eventContext.getItem().getFlags() != null)
			{
				itemUpdated.setFlag(eventContext.getItem().getFlags().getData());
			}
                        
                        EventCacheItem.Builder oldEventItem=ConvertToEventItem(eventContext.getOldItem(), datafilter);
			
                        if(oldEventItem!=null)
                            eventId.setOldItem(oldEventItem);
                        
                        EventCacheItem.Builder eventItem=ConvertToEventItem(eventContext.getItem(), datafilter);
                        
                        if(eventItem!=null)
                            eventId.setItem(eventItem);

		}
                itemUpdated.setEventId(eventId);
                
		return itemUpdated;
        }

	public static CacheClearedEventResponse.Builder GetCacheClearedResponse(EventContext eventContext)
	{
		CacheClearedEventResponse.Builder cacheCleared =com.alachisoft.tayzgrid.common.protobuf.CacheClearedEventResponseProtocol.CacheClearedEventResponse.newBuilder();
		
                com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventId=com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.newBuilder();
               
                if (eventContext != null)
		{
			eventId.setEventUniqueId(eventContext.getEventID().getEventUniqueID());
			eventId.setOperationCounter(eventContext.getEventID().getOperationCounter());
			eventId.setEventCounter(eventContext.getEventID().getEventCounter());
		}
                cacheCleared.setEventId(eventId);

		return cacheCleared;
	}

        public static TaskCallbackResponse.Builder GetMapReduceTaskCallbackResponse(EventContext eventContext, String taskId, int taskStatus, short callbackId)
        {
            TaskCallbackResponseProtocol.TaskCallbackResponse.Builder mrTaskCallbackResponse = TaskCallbackResponseProtocol.TaskCallbackResponse.newBuilder();
            
            mrTaskCallbackResponse.setTaskId(taskId);
            mrTaskCallbackResponse.setTaskStatus(taskStatus);
            mrTaskCallbackResponse.setTaskResult(ByteString.EMPTY);
            mrTaskCallbackResponse.setCallbackId(callbackId);
            
            com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.Builder eventId=com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId.newBuilder();
    
            if (eventContext != null)
            {
                eventId.setEventUniqueId(eventContext.getEventID().getEventUniqueID());
                eventId.setOperationCounter(eventContext.getEventID().getOperationCounter());
                eventId.setEventCounter(eventContext.getEventID().getEventCounter());

            }
            mrTaskCallbackResponse.setEventId(eventId);
            
            return mrTaskCallbackResponse;
        }


	public static EventCacheItem.Builder ConvertToEventItem(EventCacheEntry entry, EventDataFilter datafilter)
	{
		if (datafilter.equals(EventDataFilter.None) || entry == null)
		{
			return null;
		}

		EventCacheItem.Builder cacheItem =com.alachisoft.tayzgrid.common.protobuf.EventCacheItemProtocol.EventCacheItem.newBuilder();
                
                if(entry.getGroup() != null)
                    cacheItem.setGroup(entry.getGroup());
                
                if(entry.getSubGroup() != null)
                    cacheItem.setSubGroup(entry.getSubGroup());

                cacheItem.setItemVersion(entry.getVersion());
		cacheItem.setPriority((int)entry.getPriority());
                
		cacheItem.setResyncExpiredItems(entry.getReSyncExpiredItems());
                
                if(entry.getReSyncProviderCacheItem() != null)
                cacheItem.setResyncProviderName(entry.getReSyncProviderCacheItem());

		UserBinaryObject userBinary = (UserBinaryObject)((entry.getValue() instanceof UserBinaryObject) ? entry.getValue() : null);
		if (userBinary == null)
		{
			if (entry.getValue() instanceof CallbackEntry)
			{
				userBinary = (UserBinaryObject)((((CallbackEntry)entry.getValue()).getValue() instanceof UserBinaryObject) ? ((CallbackEntry)entry.getValue()).getValue() : null);
			}
		}

		if (userBinary != null)
		{
                        cacheItem.addAllValue(getByteStringList(userBinary.getDataList()));
                }

		          //Can be optimized
            if (datafilter != null) {
                if (datafilter.equals(EventDataFilter.None)) {
                    return null;
                } else if (datafilter.equals(EventDataFilter.Metadata)) {
                    cacheItem.clearValue();
                }
            }
            return cacheItem;
	}
        
        public static Collection<ByteString> getByteStringList(List<byte[]> list)
        {            
             ArrayList<ByteString> byteStringList=new ArrayList<ByteString>();
             
             for (int i = 0; i < list.size(); i++)
             {
                 byteStringList.add(ByteString.copyFrom(list.get(i)));
             }
             return byteStringList;
        }
}
