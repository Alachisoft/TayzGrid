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

package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.AsyncOpCode;
import com.alachisoft.tayzgrid.caching.AsyncOperationCompletedCallback;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.threading.ThreadPool;
import com.alachisoft.tayzgrid.web.caching.runnable.AsyncCacheClearedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.AsyncItemAddedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.AsyncItemRemovedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.AsyncItemUpdatedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.DataSourceClearedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.DataSourceItemsAddedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.DataSourceItemsRemovedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.DataSourceItemsUpdatedCallbackRunnable;
import com.alachisoft.tayzgrid.web.caching.runnable.JCacheListener;
import com.alachisoft.tayzgrid.web.caching.runnable.JCacheLoaderCallbackRunnable;
import java.util.Iterator;
import java.util.Set;
import javax.cache.integration.CompletionListener;

public class CacheAsyncEventsListener implements IDisposable {

    Cache _parent;
    private AsyncOperationCompletedCallback _asyncOperationCompleted;

    /**
     * Constructor.
     *
     * @param parent
     */
    public CacheAsyncEventsListener(Cache parent) {
        _parent = parent;
    }

    public void dispose() {
    }

    public void OnDataSourceUpdated(short callbackId, java.util.Hashtable result, OpCode operationType, boolean notifyAsync) {
//        int processid = Kernel32.INSTANCE.GetCurrentProcessId();

        switch (operationType) {
            case Add:
                try {
                    if (callbackId != -1) {
                        DataSourceItemsAddedCallback cb = (DataSourceItemsAddedCallback) _parent.getAsyncCallbackIDsMap().GetResource(callbackId);
                        if (cb != null) {
                            _parent.getAsyncCallbackIDsMap().RemoveResource(callbackId, result.size());
//                            _parent.getAsyncCallbacksMap().RemoveResource("dsiacb-" + processid + "-" + cb.toString());

                            if (notifyAsync) {
                                DataSourceItemsAddedCallbackRunnable runnable = new DataSourceItemsAddedCallbackRunnable(cb, result);
                                ThreadPool.executeTask(runnable);
                            } else {
                                cb.dataSourceItemAdded(result);
                            }

                            if (_parent._perfStatsCollector != null) {
                                _parent._perfStatsCollector.incrementEventProcessedPerSec();
                            }
                        }
                    }
                } catch (java.lang.Exception e) {
                }
                break;
            case Update:
                try {
                    if (callbackId != -1) {
                        DataSourceItemsUpdatedCallback cb = (DataSourceItemsUpdatedCallback) _parent.getAsyncCallbackIDsMap().GetResource(callbackId);
                        if (cb != null) {
                            _parent.getAsyncCallbackIDsMap().RemoveResource(callbackId, result.size());
//                            _parent.getAsyncCallbacksMap().RemoveResource("dsiucb-" + processid + "-" + cb.toString());

                            if (notifyAsync) {
                                DataSourceItemsUpdatedCallbackRunnable runnable = new DataSourceItemsUpdatedCallbackRunnable(cb, result);
                                ThreadPool.executeTask(runnable);
                            } else {
                                cb.dataSourceItemUpdated(result);
                            }
                        }
                    }
                } catch (java.lang.Exception e2) {
                }
                break;
            case Remove:
                try {
                    if (callbackId != -1) {
                        DataSourceItemsRemovedCallback cb = (DataSourceItemsRemovedCallback) _parent.getAsyncCallbackIDsMap().GetResource(callbackId);
                        if (cb != null) {
                            _parent.getAsyncCallbackIDsMap().RemoveResource(callbackId, result.size());
//                            _parent.getAsyncCallbacksMap().RemoveResource("dsiucb-" + processid + "-" + cb.toString());

                            if (notifyAsync) {
                                DataSourceItemsRemovedCallbackRunnable runnable = new DataSourceItemsRemovedCallbackRunnable(cb, result);
                                ThreadPool.executeTask(runnable);
                            } else {
                                cb.dataSourceItemRemoved(result);
                            }
                        }
                    }
                } catch (java.lang.Exception e3) {
                }
                break;
            case Clear:
                try {
                    if (callbackId != -1) {
                        DataSourceClearedCallback cb = (DataSourceClearedCallback) _parent.getAsyncCallbackIDsMap().GetResource(callbackId);
                        if (cb != null) {

                            _parent.getAsyncCallbackIDsMap().RemoveResource(callbackId);
//                            _parent.getAsyncCallbacksMap().RemoveResource("dsccb-" + processid + "-" + cb.toString());

                            Object param = null;

                            Set itorSet = result.entrySet();
                            Iterator itor = itorSet.iterator();
                            while (itor.hasNext()) {
                                param = itor.next();
                                if (notifyAsync) {
                                    DataSourceClearedCallbackRunnable runnable = new DataSourceClearedCallbackRunnable(cb, result);
                                    ThreadPool.executeTask(runnable);

                                } else {
                                    cb.dataSourceCleared(param);
                                }
                            }
                        }
                    }
                } catch (java.lang.Exception e4) {
                }
                break;
        }
    }

    public void OnAsyncOperationCompleted(Object opCode, Object result, boolean notifyAsync) {
        try {
            BitSet flag = new BitSet();
            Object[] package_Renamed = null;

            package_Renamed = (Object[]) _parent.safeDeserialize(result, _parent.getSerializationContext(), flag);

            Object key = package_Renamed[0];
            AsyncCallbackInfo cbInfo = (AsyncCallbackInfo) package_Renamed[1];
            Object res = package_Renamed[2];

            AsyncOpCode code = (AsyncOpCode) opCode;
           // int processid = Kernel32.INSTANCE.GetCurrentProcessId();

            switch (code) {
                case Add:
                    try {
                        if (cbInfo != null) {
                            AsyncItemAddedCallback cb = (AsyncItemAddedCallback) _parent.getAsyncCallbackIDsMap().GetResource(cbInfo.getCallback());
                            if (cb != null) {
                                _parent.getAsyncCallbackIDsMap().RemoveResource(cbInfo.getCallback());
//                                _parent.getAsyncCallbacksMap().RemoveResource("aiacb-" + processid + "-" + cb.toString());
                             //   _parent.getAsyncCallbacksMap().RemoveResource(cb);

                                if (notifyAsync) {
                                    AsyncItemAddedCallbackRunnable runnable = new AsyncItemAddedCallbackRunnable(cb, key, res);
                                    ThreadPool.executeTask(runnable);
                                } else {
                                    cb.asyncItemAdded(key, res);
                                }

                                if (_parent._perfStatsCollector != null) {
                                    _parent._perfStatsCollector.incrementEventProcessedPerSec();
                                }
                            }
                        }
                    } catch (java.lang.Exception e) {
                    }
                    break;
                case Update:
                    try {
                        if (cbInfo != null) {
                            AsyncItemUpdatedCallback cb = (AsyncItemUpdatedCallback) _parent.getAsyncCallbackIDsMap().GetResource(cbInfo.getCallback());
                            if (cb != null) {
                                if (notifyAsync) {
                                    AsyncItemUpdatedCallbackRunnable runnable = new AsyncItemUpdatedCallbackRunnable(cb, key, res);
                                    ThreadPool.executeTask(runnable);
                                } else {
                                    cb.asyncItemUpdated(key, res);
                                }

                                _parent.getAsyncCallbackIDsMap().RemoveResource(cbInfo.getCallback());
                          //      _parent.getAsyncCallbacksMap().RemoveResource("aiucb-" + processid + "-" + cb.toString());
                            }
                        }
                    } catch (java.lang.Exception e2) {
                    }
                    break;
                case Remove:
                    try {
                        if (cbInfo != null) {
                            AsyncItemRemovedCallback cb = (AsyncItemRemovedCallback) _parent.getAsyncCallbackIDsMap().GetResource(cbInfo.getCallback());
                            if (cb != null) {
                                if (notifyAsync) {
                                    AsyncItemRemovedCallbackRunnable runnable = new AsyncItemRemovedCallbackRunnable(cb, key, res);
                                    ThreadPool.executeTask(runnable);                                    
                                } else {
                                    cb.asyncItemRemoved(key, res);
                                }
                                _parent.getAsyncCallbackIDsMap().RemoveResource(cbInfo.getCallback());
                         //       _parent.getAsyncCallbacksMap().RemoveResource("aircb-" + processid + "-" + cb.toString());
                            }
                        }
                    } catch (java.lang.Exception e3) {
                    }
                    break;
                case Clear:
                    try {
                        if (cbInfo != null) {
                            AsyncCacheClearedCallback cb = (AsyncCacheClearedCallback) _parent.getAsyncCallbackIDsMap().GetResource(cbInfo.getCallback());                            
                            if (cb != null) {
                                if (notifyAsync) {
                                    AsyncCacheClearedCallbackRunnable runnable = new AsyncCacheClearedCallbackRunnable(cb, key, res);
                                    ThreadPool.executeTask(runnable);                                    
                                } else {
                                    cb.asyncCacheCleared(res);
                                }
                                _parent.getAsyncCallbackIDsMap().RemoveResource(cbInfo.getCallback());
                           //     _parent.getAsyncCallbacksMap().RemoveResource("acccb-" + processid + "-" + cb.toString());
                            }
                        }
                    } catch (java.lang.Exception e4) {
                    }
                    break;
                case loadAll:
                    try {
                        if(cbInfo != null) {
                            CompletionListener cb = (CompletionListener) _parent.getAsyncCallbackIDsMap().GetResource(cbInfo.getCallback());
                           if (notifyAsync) {
                               JCacheLoaderCallbackRunnable runnable = new JCacheLoaderCallbackRunnable(cb, res);
                               ThreadPool.executeTask(runnable); 
                           } 
                           else {
                               JCacheListener listener = new JCacheListener(cb, res);
                               listener.ExecuteListener();
                        }
                           
                           _parent.getAsyncCallbackIDsMap().RemoveResource(cbInfo.getCallback());
                           //ask
                        }
                    }
                    catch (java.lang.Exception e5) {
                        
                    }
                    break;
            }
        } catch (java.lang.Exception e6) {
        }
    }
}
