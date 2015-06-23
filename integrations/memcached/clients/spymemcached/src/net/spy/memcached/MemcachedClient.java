/**
 * Copyright (C) 2006-2009 Dustin Sallings Copyright (C) 2009-2011 Couchbase,
 * Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING IN THE
 * SOFTWARE.
 */
package net.spy.memcached;

import java.io.IOException;
import java.net.SocketAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.*;
import net.spy.memcached.auth.AuthThreadMonitor;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.BulkGetFuture;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.internal.SingleElementInfiniteIterator;
import net.spy.memcached.ops.CASOperationStatus;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.GetAndTouchOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.transcoders.TranscodeService;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.memcached.util.StringUtils;
import com.alachisoft.tayzgrid.integrations.memcached.provider.*;
import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.*;
import java.io.FileInputStream;
import static net.spy.memcached.ops.ConcatenationType.append;
import static net.spy.memcached.ops.ConcatenationType.prepend;
import static net.spy.memcached.ops.StoreType.add;
import static net.spy.memcached.ops.StoreType.replace;
import static net.spy.memcached.ops.StoreType.set;
import java.lang.Object;
import java.net.InetSocketAddress;
import java.io.InputStream;
import static net.spy.memcached.ops.Mutator.incr;
import net.spy.memcached.ops.OperationException;

/**
 * Client to a memcached server.
 *
 * <h2>Basic usage</h2>
 *
 * <pre>
 * MemcachedClient c = new MemcachedClient(
 *    new InetSocketAddress(&quot;hostname&quot;, portNum));
 *
 * // Store a value (async) for one hour
 * c.set(&quot;someKey&quot;, 3600, someObject);
 * // Retrieve a value.
 * Object myObject = c.get(&quot;someKey&quot;);
 * </pre>
 *
 * <h2>Advanced Usage</h2>
 *
 * <p>
 * MemcachedClient may be processing a great deal of asynchronous messages or
 * possibly dealing with an unreachable memcached, which may delay processing.
 * If a memcached is disabled, for example, MemcachedConnection will continue to
 * attempt to reconnect and replay pending operations until it comes back up. To
 * prevent this from causing your application to hang, you can use one of the
 * asynchronous mechanisms to time out a request and cancel the operation to the
 * server.
 * </p>
 *
 * <pre>
 *      // Get a memcached client connected to several servers
 *      // over the binary protocol
 *      MemcachedClient c = new MemcachedClient(new BinaryConnectionFactory(),
 *              AddrUtil.getAddresses("server1:11211 server2:11211"));
 *
 *      // Try to get a value, for up to 5 seconds, and cancel if it
 *      // doesn't return
 *      Object myObj = null;
 *      Future&lt;Object&gt; f = c.asyncGet("someKey");
 *      try {
 *          myObj = f.get(5, TimeUnit.SECONDS);
 *      // throws expecting InterruptedException, ExecutionException
 *      // or TimeoutException
 *      } catch (Exception e) {  /*  /
 *          // Since we don't need this, go ahead and cancel the operation.
 *          // This is not strictly necessary, but it'll save some work on
 *          // the server.  It is okay to cancel it if running.
 *          f.cancel(true);
 *          // Do other timeout related stuff
 *      }
 * </pre>
 *
 * <p>Optionally, it is possible to activate a check that makes sure that the
 * node is alive and responding before running actual operations (even before
 * authentication. Only enable this if you are sure that you do not run into
 * issues during connection (some memcached services have problems with it). You
 * can enable it by setting the net.spy.verifyAliveOnConnect System Property to
 * "true".</p>
 */
public class MemcachedClient extends SpyObject implements MemcachedClientIF,
        ConnectionObserver {

    private IMemcachedProvider memcachedProvider;
    protected volatile boolean shuttingDown = false;
    protected final long operationTimeout;
//  protected final MemcachedConnection mconn;
    protected final OperationFactory opFact;
    protected final Transcoder<Object> transcoder;
    protected final TranscodeService tcService;
//  protected final AuthDescriptor authDescriptor;
    protected final ConnectionFactory connFactory;
    protected final AuthThreadMonitor authMonitor = new AuthThreadMonitor();
    private boolean isAlive = false;

    /**
     * Get a memcache client operating on the specified memcached locations.
     *
     * @param ia the memcached locations
     * @throws IOException if connections cannot be established
     */
    public MemcachedClient(InetSocketAddress... ia) throws IOException {
        this(new DefaultConnectionFactory(), Arrays.asList(ia));
    }

    /**
     * Get a memcache client over the specified memcached locations.
     *
     * @param addrs the socket addrs
     * @throws IOException if connections cannot be established
     */
    public MemcachedClient(List<InetSocketAddress> addrs) throws IOException {
        this(new DefaultConnectionFactory(), addrs);
    }

    /**
     * Get a memcache client over the specified memcached locations.
     *
     * @param cf the connection factory to configure connections for this client
     * @param addrs the socket addresses
     * @throws IOException if connections cannot be established
     */
    public MemcachedClient(ConnectionFactory cf, List<InetSocketAddress> addrs) {
        if (cf == null) {
            throw new NullPointerException("Connection factory required");
        }
        if (addrs == null) {
            throw new NullPointerException("Server list required");
        }
        if (addrs.isEmpty()) {
            throw new IllegalArgumentException("You must have at least one server to"
                    + " connect to");
        }
        if (cf.getOperationTimeout() <= 0) {
            throw new IllegalArgumentException("Operation timeout must be positive.");
        }


        connFactory = cf;
        tcService = new TranscodeService(cf.isDaemon());
        transcoder = cf.getDefaultTranscoder();
        opFact = cf.getOperationFactory();
        assert opFact != null : "Connection factory failed to make op factory";
        operationTimeout = cf.getOperationTimeout();
        Properties prop = new Properties();
        String cacheName = "";
        try {
            //load a properties file
            InputStream input = new FileInputStream("config.properties");
            prop.load(input);
            cacheName = prop.getProperty("TayzGrid.CacheName");
            if (cacheName.equals("")) {
                Exception ex = new Exception("Unable to read Cache Name from configuration");
                throw ex;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            memcachedProvider = CacheFactory.createCacheProvider(cacheName);
        } catch (Exception e) {
        }
        isAlive = true;
    }

    /**
     * Get the addresses of available servers.
     *
     * <p>
     * This is based on a snapshot in time so shouldn't be considered completely
     * accurate, but is a useful for getting a feel for what's working and
     * what's not working.
     * </p>
     *
     * @return point-in-time view of currently available servers
     */
    public Collection<SocketAddress> getAvailableServers() {
        ArrayList<SocketAddress> rv = new ArrayList<SocketAddress>();
        return rv;
    }

    /**
     * Get the addresses of unavailable servers.
     *
     * <p>
     * This is based on a snapshot in time so shouldn't be considered completely
     * accurate, but is a useful for getting a feel for what's working and
     * what's not working.
     * </p>
     *
     * @return point-in-time view of currently available servers
     */
    public Collection<SocketAddress> getUnavailableServers() {
        ArrayList<SocketAddress> rv = new ArrayList<SocketAddress>();
        return rv;
    }

    /**
     * Get a read-only wrapper around the node locator wrapping this instance.
     *
     * @return this instance's NodeLocator
     */
    public NodeLocator getNodeLocator() {
        return null;
    }

    /**
     * Get the default transcoder that's in use.
     *
     * @return this instance's Transcoder
     */
    public Transcoder<Object> getTranscoder() {
        return transcoder;
    }

    public CountDownLatch broadcastOp(final BroadcastOpFactory of) {
        return new CountDownLatch(1);
    }

    public CountDownLatch broadcastOp(final BroadcastOpFactory of,
            Collection<MemcachedNode> nodes) {
        return new CountDownLatch(nodes.size());
    }

    private <T> OperationFuture<Boolean> asyncStore(StoreType storeType,
            String key, int exp, T value, Transcoder<T> tc) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        CachedData co = tc.encode(value);
        StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<Boolean> rv =
                new OperationFuture<Boolean>(key, latch, operationTimeout);

        Operation op = opFact.store(storeType, key, co.getFlags(), exp,
                co.getData(), new StoreOperation.Callback() {
            public void receivedStatus(OperationStatus val) {
            }

            public void gotData(String key, long cas) {
            }

            public void complete() {
            }
        });

        OperationResult opResult = new OperationResult();
        boolean success = false;
        String message = "";
        CASResponse response = null;
        if (memcachedProvider != null) {
            try {
                switch (storeType) {
                    case set:
                        opResult = memcachedProvider.set(key, co.getFlags(), exp, co.getData());
                        break;
                    case add:
                        opResult = memcachedProvider.add(key, co.getFlags(), exp, co.getData());
                        break;
                    case replace:
                        opResult = memcachedProvider.replace(key, co.getFlags(), exp, co.getData());
                        break;
                }
                switch (opResult.getReturnResult()) {
                    case SUCCESS:
                        success = true;
                        response = CASResponse.OK;
                        message = getMessage(ResponseType.stored);
                        break;
                    case ITEM_EXISTS:
                        response = CASResponse.EXISTS;
                        message = getMessage(ResponseType.exists);
                        break;
                    case ITEM_NOT_FOUND:
                        response = CASResponse.NOT_FOUND;
                        message = getMessage(ResponseType.notfound);
                        break;
                    case ITEM_MODIFIED:
                        response = CASResponse.EXISTS;
                        message = getMessage(ResponseType.exists);
                        break;
                }
            } catch (InvalidArgumentsException e) {
                message = "CLIENT_ERROR bad data chunk";
            } catch (Exception e) {
                message = "SERVER_ERROR";
            }
        }
        if (opFact instanceof BinaryOperationFactory) {
            if (success) {
                rv.setCas(Long.parseLong(opResult.getReturnValue().toString()));
            }
            CASOperationStatus val = new CASOperationStatus(success, message, response);
            rv.set(val.isSuccess(), val);
        } else {
            rv.set(success, new OperationStatus(success, message));
        }
        rv.setOperation(op);
        return rv;
    }

    private OperationFuture<Boolean> asyncStore(StoreType storeType, String key,
            int exp, Object value) {
        return asyncStore(storeType, key, exp, value, transcoder);
    }

    private <T> OperationFuture<Boolean> asyncCat(ConcatenationType catType,
            long cas, String key, T value, Transcoder<T> tc) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        CachedData co = tc.encode(value);
        StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);

        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<Boolean> rv = new OperationFuture<Boolean>(key,
                latch, operationTimeout);
        Operation op = opFact.cat(catType, cas, key, co.getData(),
                new OperationCallback() {
            public void receivedStatus(OperationStatus val) {
            }

            public void complete() {
            }
        });

        if (cas < 0) {
            if (opFact instanceof BinaryOperationFactory) {
                rv.set(false, new CASOperationStatus(false, "Data exists for key.", CASResponse.EXISTS));
            }
        } else {
            OperationResult opResult = new OperationResult();
            String message = "";
            CASResponse response = null;
            boolean success = false;
            if (memcachedProvider != null) {
                try {
                    switch (catType) {
                        case append:
                            if (opFact instanceof BinaryOperationFactory) {
                                opResult = memcachedProvider.append(key, co.getData(), cas);
                            } else {
                                opResult = memcachedProvider.append(key, co.getData(), 0);
                            }
                            break;
                        case prepend:
                            if (opFact instanceof BinaryOperationFactory) {
                                opResult = memcachedProvider.prepend(key, co.getData(), cas);
                            } else {
                                opResult = memcachedProvider.prepend(key, co.getData(), 0);
                            }
                            break;
                    }
                    switch (opResult.getReturnResult()) {
                        case SUCCESS:
                            success = true;
                            response = CASResponse.OK;
                            message = getMessage(ResponseType.stored);
                            break;
                        case ITEM_NOT_FOUND:
                            response = CASResponse.NOT_FOUND;
                            message = message = getMessage(ResponseType.notfound);
                            break;
                        case ITEM_MODIFIED:
                            response = CASResponse.EXISTS;
                            message = message = getMessage(ResponseType.exists);
                            break;
                    }
                } catch (InvalidArgumentsException e) {
                    message = "CLIENT_ERROR bad data chunk";
                } catch (Exception e) {
                    message = "SERVER_ERROR";
                }
            }
            if (opFact instanceof BinaryOperationFactory) {
                CASOperationStatus val = new CASOperationStatus(success, message, response);
                rv.set(val.isSuccess(), val);
            } else {
                rv.set(success, new OperationStatus(success, message));
            }
        }
        rv.setOperation(op);
        return rv;
    }

    /**
     * Touch the given key to reset its expiration time with the default
     * transcoder.
     *
     * @param key the key to fetch
     * @param exp the new expiration to set for the given key
     * @return a future that will hold the return value of whether or not the
     * fetch succeeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    @Override
    public <T> OperationFuture<Boolean> touch(final String key, final int exp) {
        return touch(key, exp, transcoder);
    }

    /**
     * Touch the given key to reset its expiration time.
     *
     * @param key the key to fetch
     * @param exp the new expiration to set for the given key
     * @param tc the transcoder to serialize and unserialize value
     * @return a future that will hold the return value of whether or not the
     * fetch succeeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    @Override
    public <T> OperationFuture<Boolean> touch(final String key, final int exp,
            final Transcoder<T> tc) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<Boolean> rv =
                new OperationFuture<Boolean>(key, latch, operationTimeout);

        Operation op = opFact.touch(key, exp, new OperationCallback() {
            public void receivedStatus(OperationStatus status) {
            }

            public void complete() {
            }
        });
        OperationResult opResult = new OperationResult();
        String message = "";
        boolean success = false;
        if (memcachedProvider != null) {
            try {
                opResult = memcachedProvider.touch(key, exp);
                switch (opResult.getReturnResult()) {
                    case SUCCESS:
                        success = true;
                        message = "TOUCHED";
                        break;
                    case ITEM_NOT_FOUND:
                        message = getMessage(ResponseType.notfound);
                        break;
                }
            } catch (InvalidArgumentsException e) {
                message = "CLIENT_ERROR bad data chunk";
            } catch (Exception e) {
                message = "SERVER_ERROR";
            }
        }
        rv.set(success, new OperationStatus(success, message));

        rv.setOperation(op);
        return rv;
    }

    /**
     * Append to an existing value in the cache.
     *
     * If 0 is passed in as the CAS identifier, it will override the value on
     * the server without performing the CAS check.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param cas cas identifier (ignored in the ascii protocol)
     * @param key the key to whose value will be appended
     * @param val the value to append
     * @return a future indicating success, false if there was no change to the
     * value
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> append(long cas, String key, Object val) {
        return append(cas, key, val, transcoder);
    }

    /**
     * Append to an existing value in the cache.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param key the key to whose value will be appended
     * @param val the value to append
     * @return a future indicating success, false if there was no change to the
     * value
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> append(String key, Object val) {
        return append(0, key, val, transcoder);
    }

    /**
     * Append to an existing value in the cache.
     *
     * If 0 is passed in as the CAS identifier, it will override the value on
     * the server without performing the CAS check.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param <T>
     * @param cas cas identifier (ignored in the ascii protocol)
     * @param key the key to whose value will be appended
     * @param val the value to append
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future indicating success
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<Boolean> append(long cas, String key, T val,
            Transcoder<T> tc) {
        return asyncCat(ConcatenationType.append, cas, key, val, tc);
    }

    /**
     * Append to an existing value in the cache.
     *
     * If 0 is passed in as the CAS identifier, it will override the value on
     * the server without performing the CAS check.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param <T>
     * @param key the key to whose value will be appended
     * @param val the value to append
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future indicating success
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<Boolean> append(String key, T val,
            Transcoder<T> tc) {
        return asyncCat(ConcatenationType.append, 0, key, val, tc);
    }

    /**
     * Prepend to an existing value in the cache.
     *
     * If 0 is passed in as the CAS identifier, it will override the value on
     * the server without performing the CAS check.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param cas cas identifier (ignored in the ascii protocol)
     * @param key the key to whose value will be prepended
     * @param val the value to append
     * @return a future indicating success
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> prepend(long cas, String key, Object val) {
        return prepend(cas, key, val, transcoder);
    }

    /**
     * Prepend to an existing value in the cache.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param key the key to whose value will be prepended
     * @param val the value to append
     * @return a future indicating success
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> prepend(String key, Object val) {
        return prepend(0, key, val, transcoder);
    }

    /**
     * Prepend to an existing value in the cache.
     *
     * If 0 is passed in as the CAS identifier, it will override the value on
     * the server without performing the CAS check.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param <T>
     * @param cas cas identifier (ignored in the ascii protocol)
     * @param key the key to whose value will be prepended
     * @param val the value to append
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future indicating success
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<Boolean> prepend(long cas, String key, T val,
            Transcoder<T> tc) {
        return asyncCat(ConcatenationType.prepend, cas, key, val, tc);
    }

    /**
     * Prepend to an existing value in the cache.
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * @param <T>
     * @param key the key to whose value will be prepended
     * @param val the value to append
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future indicating success
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<Boolean> prepend(String key, T val,
            Transcoder<T> tc) {
        return asyncCat(ConcatenationType.prepend, 0, key, val, tc);
    }

    /**
     * Asynchronous CAS operation.
     *
     * @param <T>
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param value the new value
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future that will indicate the status of the CAS
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, T value, Transcoder<T> tc) {
        return asyncCAS(key, casId, 0, value, tc);
    }

    /**
     * Asynchronous CAS operation.
     *
     * @param <T>
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param exp the expiration of this object
     * @param value the new value
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future that will indicate the status of the CAS
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, int exp, T value, Transcoder<T> tc) {

        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }

        CachedData co = tc.encode(value);
        StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<CASResponse> rv =
                new OperationFuture<CASResponse>(key, latch, operationTimeout);
        Operation op = opFact.cas(StoreType.set, key, casId, co.getFlags(), exp,
                co.getData(), new StoreOperation.Callback() {
            public void receivedStatus(OperationStatus val) {
            }

            public void gotData(String key, long cas) {
            }

            public void complete() {
            }
        });
        if (casId < 0) {
            if (opFact instanceof BinaryOperationFactory) {
                rv.set(CASResponse.EXISTS, new CASOperationStatus(false, "Data exists for key.", CASResponse.EXISTS));
            }
        } else {
            OperationResult opResult = new OperationResult();
            CASResponse response = null;
            boolean success = false;
            String message = "";
            if (memcachedProvider != null) {
                try {
                    opResult = memcachedProvider.checkAndSet(key, co.getFlags(), exp, casId, co.getData());
                    switch (opResult.getReturnResult()) {
                        case SUCCESS:
                            success = true;
                            message = getMessage(ResponseType.stored);
                            response = CASResponse.OK;
                            break;
                        case ITEM_NOT_FOUND:
                            message = getMessage(ResponseType.notfound);
                            response = CASResponse.NOT_FOUND;
                            break;
                        case ITEM_MODIFIED:
                            message = getMessage(ResponseType.exists);
                            response = CASResponse.EXISTS;
                            break;
                    }
                } catch (InvalidArgumentsException e) {
                    throw new RuntimeException("Unhandled state: " + new OperationStatus(false, "CLIENT_ERROR bad data chunk"));
                } catch (Exception e) {
                    throw new RuntimeException("Unhandled state: " + new OperationStatus(false, "SERVER_ERROR"));
                }
            }
            CASOperationStatus val = new CASOperationStatus(success, message, response);
            rv.set(((CASOperationStatus) val).getCASResponse(), val);
            if (opFact instanceof BinaryOperationFactory && success) {
                rv.setCas(Long.parseLong(opResult.getReturnValue().toString()));
            }
        }
        rv.setOperation(op);
        return rv;
    }

    /**
     * Asynchronous CAS operation using the default transcoder.
     *
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param value the new value
     * @return a future that will indicate the status of the CAS
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<CASResponse> asyncCAS(String key, long casId, Object value) {
        return asyncCAS(key, casId, value, transcoder);
    }

    /**
     * Perform a synchronous CAS operation.
     *
     * @param <T>
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param value the new value
     * @param tc the transcoder to serialize and unserialize the value
     * @return a CASResponse
     * @throws OperationTimeoutException if global operation timeout is exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> CASResponse cas(String key, long casId, T value,
            Transcoder<T> tc) {
        return cas(key, casId, 0, value, tc);
    }

    /**
     * Perform a synchronous CAS operation.
     *
     * @param <T>
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param exp the expiration of this object
     * @param value the new value
     * @param tc the transcoder to serialize and unserialize the value
     * @return a CASResponse
     * @throws OperationTimeoutException if global operation timeout is exceeded
     * @throws CancellationException if operation was canceled
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> CASResponse cas(String key, long casId, int exp, T value,
            Transcoder<T> tc) {

        CASResponse casr = null;
        try {
            OperationFuture<CASResponse> casOp = asyncCAS(key,
                    casId, exp, value, tc);
            casr = casOp.get(operationTimeout,
                    TimeUnit.MILLISECONDS);
            return casr;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for value", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CancellationException) {
                throw (CancellationException) e.getCause();
            } else {
                throw new RuntimeException("Exception waiting for value", e);
            }
        } catch (TimeoutException e) {
            throw new OperationTimeoutException("Timeout waiting for value: "
                    + buildTimeoutMessage(operationTimeout, TimeUnit.MILLISECONDS), e);
        }

    }

    /**
     * Perform a synchronous CAS operation with the default transcoder.
     *
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param value the new value
     * @return a CASResponse
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public CASResponse cas(String key, long casId, Object value) {
        return cas(key, casId, value, transcoder);
    }

    /**
     * Add an object to the cache iff it does not exist already.
     *
     * <p>
     * The
     * <code>exp</code> value is passed along to memcached exactly as given, and
     * will be processed per the memcached protocol specification:
     * </p>
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * <blockquote>
     * <p>
     * The actual value sent may either be Unix time (number of seconds since
     * January 1, 1970, as a 32-bit value), or a number of seconds starting from
     * current time. In the latter case, this number of seconds may not exceed
     * 60*60*24*30 (number of seconds in 30 days); if the number sent by a
     * client is larger than that, the server will consider it to be real Unix
     * time value rather than an offset from current time.
     * </p>
     * </blockquote>
     *
     * @param <T>
     * @param key the key under which this object should be added.
     * @param exp the expiration of this object
     * @param o the object to store
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future representing the processing of this operation
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<Boolean> add(String key, int exp, T o,
            Transcoder<T> tc) {
        return asyncStore(StoreType.add, key, exp, o, tc);
    }

    /**
     * Add an object to the cache (using the default transcoder) iff it does not
     * exist already.
     *
     * <p>
     * The
     * <code>exp</code> value is passed along to memcached exactly as given, and
     * will be processed per the memcached protocol specification:
     * </p>
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * <blockquote>
     * <p>
     * The actual value sent may either be Unix time (number of seconds since
     * January 1, 1970, as a 32-bit value), or a number of seconds starting from
     * current time. In the latter case, this number of seconds may not exceed
     * 60*60*24*30 (number of seconds in 30 days); if the number sent by a
     * client is larger than that, the server will consider it to be real Unix
     * time value rather than an offset from current time.
     * </p>
     * </blockquote>
     *
     * @param key the key under which this object should be added.
     * @param exp the expiration of this object
     * @param o the object to store
     * @return a future representing the processing of this operation
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> add(String key, int exp, Object o) {
        return asyncStore(StoreType.add, key, exp, o, transcoder);
    }

    /**
     * Set an object in the cache regardless of any existing value.
     *
     * <p>
     * The
     * <code>exp</code> value is passed along to memcached exactly as given, and
     * will be processed per the memcached protocol specification:
     * </p>
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * <blockquote>
     * <p>
     * The actual value sent may either be Unix time (number of seconds since
     * January 1, 1970, as a 32-bit value), or a number of seconds starting from
     * current time. In the latter case, this number of seconds may not exceed
     * 60*60*24*30 (number of seconds in 30 days); if the number sent by a
     * client is larger than that, the server will consider it to be real Unix
     * time value rather than an offset from current time.
     * </p>
     * </blockquote>
     *
     * @param <T>
     * @param key the key under which this object should be added.
     * @param exp the expiration of this object
     * @param o the object to store
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future representing the processing of this operation
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<Boolean> set(String key, int exp, T o,
            Transcoder<T> tc) {
        return asyncStore(StoreType.set, key, exp, o, tc);
    }

    /**
     * Set an object in the cache (using the default transcoder) regardless of
     * any existing value.
     *
     * <p>
     * The
     * <code>exp</code> value is passed along to memcached exactly as given, and
     * will be processed per the memcached protocol specification:
     * </p>
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * <blockquote>
     * <p>
     * The actual value sent may either be Unix time (number of seconds since
     * January 1, 1970, as a 32-bit value), or a number of seconds starting from
     * current time. In the latter case, this number of seconds may not exceed
     * 60*60*24*30 (number of seconds in 30 days); if the number sent by a
     * client is larger than that, the server will consider it to be real Unix
     * time value rather than an offset from current time.
     * </p>
     * </blockquote>
     *
     * @param key the key under which this object should be added.
     * @param exp the expiration of this object
     * @param o the object to store
     * @return a future representing the processing of this operation
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> set(String key, int exp, Object o) {
        return asyncStore(StoreType.set, key, exp, o, transcoder);
    }

    /**
     * Replace an object with the given value iff there is already a value for
     * the given key.
     *
     * <p>
     * The
     * <code>exp</code> value is passed along to memcached exactly as given, and
     * will be processed per the memcached protocol specification:
     * </p>
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * <blockquote>
     * <p>
     * The actual value sent may either be Unix time (number of seconds since
     * January 1, 1970, as a 32-bit value), or a number of seconds starting from
     * current time. In the latter case, this number of seconds may not exceed
     * 60*60*24*30 (number of seconds in 30 days); if the number sent by a
     * client is larger than that, the server will consider it to be real Unix
     * time value rather than an offset from current time.
     * </p>
     * </blockquote>
     *
     * @param <T>
     * @param key the key under which this object should be added.
     * @param exp the expiration of this object
     * @param o the object to store
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future representing the processing of this operation
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<Boolean> replace(String key, int exp, T o,
            Transcoder<T> tc) {
        return asyncStore(StoreType.replace, key, exp, o, tc);
    }

    /**
     * Replace an object with the given value (transcoded with the default
     * transcoder) iff there is already a value for the given key.
     *
     * <p>
     * The
     * <code>exp</code> value is passed along to memcached exactly as given, and
     * will be processed per the memcached protocol specification:
     * </p>
     *
     * <p>
     * Note that the return will be false any time a mutation has not occurred.
     * </p>
     *
     * <blockquote>
     * <p>
     * The actual value sent may either be Unix time (number of seconds since
     * January 1, 1970, as a 32-bit value), or a number of seconds starting from
     * current time. In the latter case, this number of seconds may not exceed
     * 60*60*24*30 (number of seconds in 30 days); if the number sent by a
     * client is larger than that, the server will consider it to be real Unix
     * time value rather than an offset from current time.
     * </p>
     * </blockquote>
     *
     * @param key the key under which this object should be added.
     * @param exp the expiration of this object
     * @param o the object to store
     * @return a future representing the processing of this operation
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> replace(String key, int exp, Object o) {
        return asyncStore(StoreType.replace, key, exp, o, transcoder);
    }

    /**
     * Get the given key asynchronously.
     *
     * @param <T>
     * @param key the key to fetch
     * @param tc the transcoder to serialize and unserialize value
     * @return a future that will hold the return value of the fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> GetFuture<T> asyncGet(final String key, final Transcoder<T> tc) {

        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final GetFuture<T> rv = new GetFuture<T>(latch, operationTimeout, key);
        Operation op = opFact.get(key, new GetOperation.Callback() {
            public void receivedStatus(OperationStatus status) {
            }

            public void gotData(String k, int flags, byte[] data) {
            }

            public void complete() {
            }
        });



        Future<T> val = null;
        boolean success = true;
        CASResponse response = null;
        String message = getMessage(ResponseType.ended);

        if (memcachedProvider != null) {

            if (key == null) {
                java.lang.NullPointerException ex = new java.lang.NullPointerException();
                throw ex;
            }
            try {
                List<GetOpResult> opResult = memcachedProvider.get(new String[]{key});
                if (opResult.size() > 0) {
                    response = CASResponse.OK;
                    int flags = opResult.get(0).getFlag();
                    byte[] data = (byte[]) opResult.get(0).getReturnValue();
                    val = tcService.decode(tc, new CachedData(flags, data, tc.getMaxSize()));
                }
            } catch (InvalidArgumentsException e) {
                success = false;
                message = "CLIENT_ERROR bad data chunk";
            } catch (Exception e) {
                success = false;
                message = "SERVER_ERROR";
            }
        }
        if (opFact instanceof BinaryOperationFactory) {
            CASOperationStatus casval = new CASOperationStatus(success, message, response);
            rv.set(val, casval);
        } else {
            rv.set(val, new OperationStatus(success, message));
        }
        rv.setOperation(op);
//    mconn.enqueueOperation(key, op);
        return rv;
    }

    /**
     * Get the given key asynchronously and decode with the default transcoder.
     *
     * @param key the key to fetch
     * @return a future that will hold the return value of the fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public GetFuture<Object> asyncGet(final String key) {
        return asyncGet(key, transcoder);
    }

    /**
     * Gets (with CAS support) the given key asynchronously.
     *
     * @param <T>
     * @param key the key to fetch
     * @param tc the transcoder to serialize and unserialize value
     * @return a future that will hold the return value of the fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<CASValue<T>> asyncGets(final String key,
            final Transcoder<T> tc) {

        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<CASValue<T>> rv =
                new OperationFuture<CASValue<T>>(key, latch, operationTimeout);

        Operation op = opFact.gets(key, new GetsOperation.Callback() {
            public void receivedStatus(OperationStatus status) {
            }

            public void gotData(String k, int flags, long cas, byte[] data) {
            }

            public void complete() {
            }
        });

        CASValue<T> val = null;
        boolean success = true;
        String message = "END";
        if (memcachedProvider != null) {
            try {
                List<GetOpResult> opResult = memcachedProvider.get(new String[]{key});
                if (opResult.size() > 0) {
                    int flags = opResult.get(0).getFlag();
                    byte[] data = (byte[]) opResult.get(0).getReturnValue();
                    long cas = opResult.get(0).getVersion();
                    val = new CASValue<T>(cas, tc.decode(new CachedData(flags, data,
                            tc.getMaxSize())));
                }
            } catch (InvalidArgumentsException e) {
                success = false;
                message = "CLIENT_ERROR bad data chunk";
            } catch (Exception e) {
                success = false;
                message = "SERVER_ERROR";
            }
        }
        rv.set(val, new OperationStatus(success, message));
        rv.setOperation(op);
//    mconn.enqueueOperation(key, op);
        return rv;
    }

    /**
     * Gets (with CAS support) the given key asynchronously and decode using the
     * default transcoder.
     *
     * @param key the key to fetch
     * @return a future that will hold the return value of the fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<CASValue<Object>> asyncGets(final String key) {
        return asyncGets(key, transcoder);
    }

    /**
     * Gets (with CAS support) with a single key.
     *
     * @param <T>
     * @param key the key to get
     * @param tc the transcoder to serialize and unserialize value
     * @return the result from the cache and CAS id (null if there is none)
     * @throws OperationTimeoutException if global operation timeout is exceeded
     * @throws CancellationException if operation was canceled
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> CASValue<T> gets(String key, Transcoder<T> tc) {
        try {
            return asyncGets(key, tc).get(operationTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for value", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CancellationException) {
                throw (CancellationException) e.getCause();
            } else {
                throw new RuntimeException("Exception waiting for value", e);
            }
        } catch (TimeoutException e) {
            throw new OperationTimeoutException("Timeout waiting for value", e);
        }
    }

    /**
     * Get with a single key and reset its expiration.
     *
     * @param <T>
     * @param key the key to get
     * @param exp the new expiration for the key
     * @param tc the transcoder to serialize and unserialize value
     * @return the result from the cache (null if there is none)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws CancellationException if operation was canceled
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc) {
        try {
            return asyncGetAndTouch(key, exp, tc).get(operationTimeout,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for value", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CancellationException) {
                throw (CancellationException) e.getCause();
            } else {
                throw new RuntimeException("Exception waiting for value", e);
            }
        } catch (TimeoutException e) {
            throw new OperationTimeoutException("Timeout waiting for value", e);
        }
    }

    /**
     * Get a single key and reset its expiration using the default transcoder.
     *
     * @param key the key to get
     * @param exp the new expiration for the key
     * @return the result from the cache and CAS id (null if there is none)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public CASValue<Object> getAndTouch(String key, int exp) {
        return getAndTouch(key, exp, transcoder);
    }

    /**
     * Gets (with CAS support) with a single key using the default transcoder.
     *
     * @param key the key to get
     * @return the result from the cache and CAS id (null if there is none)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public CASValue<Object> gets(String key) {
        return gets(key, transcoder);
    }

    /**
     * Get with a single key.
     *
     * @param <T>
     * @param key the key to get
     * @param tc the transcoder to serialize and unserialize value
     * @return the result from the cache (null if there is none)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws CancellationException if operation was canceled
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> T get(String key, Transcoder<T> tc) {
        try {
            GetFuture rv = asyncGet(key, tc);
            T obj = (T) rv.get(operationTimeout, TimeUnit.MILLISECONDS);
            return obj;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for value", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CancellationException) {
                throw (CancellationException) e.getCause();
            } else {
                throw new RuntimeException("Exception waiting for value", e);
            }
        } catch (TimeoutException e) {
            throw new OperationTimeoutException("Timeout waiting for value: "
                    + buildTimeoutMessage(operationTimeout, TimeUnit.MILLISECONDS), e);
        }
    }

    /**
     * Get with a single key and decode using the default transcoder.
     *
     * @param key the key to get
     * @return the result from the cache (null if there is none)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public Object get(String key) {
        return get(key, transcoder);
    }

    /**
     * Asynchronously get a bunch of objects from the cache.
     *
     * @param <T>
     * @param keyIter Iterator that produces keys.
     * @param tcIter an iterator of transcoders to serialize and unserialize
     * values; the transcoders are matched with the keys in the same order. The
     * minimum of the key collection length and number of transcoders is used
     * and no exception is thrown if they do not match
     * @return a Future result of that fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter,
            Iterator<Transcoder<T>> tcIter) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        final Map<String, Future<T>> m = new ConcurrentHashMap<String, Future<T>>();
        final Map<String, Transcoder<T>> tcMap =
                new HashMap<String, Transcoder<T>>();
        List<String> keysList = new ArrayList<String>();
        while (keyIter.hasNext() && tcIter.hasNext()) {
            String key = keyIter.next();
            tcMap.put(key, tcIter.next());
            StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
            keysList.add(key);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final Collection<Operation> ops = new ArrayList<Operation>(1);


        GetOperation.Callback cb = new GetOperation.Callback() {
            @SuppressWarnings("synthetic-access")
            public void receivedStatus(OperationStatus status) {
            }

            public void gotData(String k, int flags, byte[] data) {
            }

            public void complete() {
            }
        };
        List<GetOpResult> opResult = new ArrayList<GetOpResult>();
        boolean success = true;
        String message = getMessage(ResponseType.ended);
        CASResponse response = null;
        if (memcachedProvider != null) {
            try {
                String[] keys = new String[keysList.size()];
                keysList.toArray(keys);
                opResult = memcachedProvider.get(keys);
                response = CASResponse.OK;
            } catch (InvalidArgumentsException e) {
                success = false;
                message = "CLIENT_ERROR bad data chunk";
            } catch (Exception e) {
                success = false;
                message = "SERVER_ERROR";
            }
        }
        Operation op = opFact.get(keysList, cb);
        ops.add(op);
        for (GetOpResult entry : opResult) {
            Transcoder<T> tc = tcMap.get(entry.getKey());
            int flags = entry.getFlag();
            byte[] data = (byte[]) entry.getReturnValue();
            m.put(entry.getKey(),
                    tcService.decode(tc, new CachedData(flags, data, tc.getMaxSize())));
        }
        final BulkGetFuture<T> rv = new BulkGetFuture<T>(m, ops, latch);
        if (opFact instanceof BinaryOperationFactory) {
            rv.setStatus(new CASOperationStatus(success, message, response));
        } else {
            rv.setStatus(new OperationStatus(success, message));
        }
        return rv;
    }

    /**
     * Asynchronously get a bunch of objects from the cache.
     *
     * @param <T>
     * @param keys the keys to request
     * @param tcIter an iterator of transcoders to serialize and unserialize
     * values; the transcoders are matched with the keys in the same order. The
     * minimum of the key collection length and number of transcoders is used
     * and no exception is thrown if they do not match
     * @return a Future result of that fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
            Iterator<Transcoder<T>> tcIter) {
        return asyncGetBulk(keys.iterator(), tcIter);
    }

    /**
     * Asynchronously get a bunch of objects from the cache.
     *
     * @param <T>
     * @param keyIter Iterator for the keys to request
     * @param tc the transcoder to serialize and unserialize values
     * @return a Future result of that fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter,
            Transcoder<T> tc) {
        return asyncGetBulk(keyIter,
                new SingleElementInfiniteIterator<Transcoder<T>>(tc));
    }

    /**
     * Asynchronously get a bunch of objects from the cache.
     *
     * @param <T>
     * @param keys the keys to request
     * @param tc the transcoder to serialize and unserialize values
     * @return a Future result of that fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
            Transcoder<T> tc) {
        return asyncGetBulk(keys, new SingleElementInfiniteIterator<Transcoder<T>>(
                tc));
    }

    /**
     * Asynchronously get a bunch of objects from the cache and decode them with
     * the given transcoder.
     *
     * @param keyIter Iterator that produces the keys to request
     * @return a Future result of that fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public BulkFuture<Map<String, Object>> asyncGetBulk(
            Iterator<String> keyIter) {
        return asyncGetBulk(keyIter, transcoder);
    }

    /**
     * Asynchronously get a bunch of objects from the cache and decode them with
     * the given transcoder.
     *
     * @param keys the keys to request
     * @return a Future result of that fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
        return asyncGetBulk(keys, transcoder);
    }

    /**
     * Varargs wrapper for asynchronous bulk gets.
     *
     * @param <T>
     * @param tc the transcoder to serialize and unserialize value
     * @param keys one more more keys to get
     * @return the future values of those keys
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc,
            String... keys) {
        return asyncGetBulk(Arrays.asList(keys), tc);
    }

    /**
     * Varargs wrapper for asynchronous bulk gets with the default transcoder.
     *
     * @param keys one more more keys to get
     * @return the future values of those keys
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys) {
        return asyncGetBulk(Arrays.asList(keys), transcoder);
    }

    /**
     * Get the given key to reset its expiration time.
     *
     * @param key the key to fetch
     * @param exp the new expiration to set for the given key
     * @return a future that will hold the return value of the fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<CASValue<Object>> asyncGetAndTouch(final String key,
            final int exp) {
        return asyncGetAndTouch(key, exp, transcoder);
    }

    /**
     * Get the given key to reset its expiration time.
     *
     * @param key the key to fetch
     * @param exp the new expiration to set for the given key
     * @param tc the transcoder to serialize and unserialize value
     * @return a future that will hold the return value of the fetch
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> OperationFuture<CASValue<T>> asyncGetAndTouch(final String key,
            final int exp, final Transcoder<T> tc) {

        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<CASValue<T>> rv = new OperationFuture<CASValue<T>>(
                key, latch, operationTimeout);

        Operation op = opFact.getAndTouch(key, exp,
                new GetAndTouchOperation.Callback() {
            private CASValue<T> val = null;

            public void receivedStatus(OperationStatus status) {
                rv.set(val, status);
            }

            public void complete() {
                latch.countDown();
            }

            public void gotData(String k, int flags, long cas, byte[] data) {
                assert k.equals(key) : "Wrong key returned";
                assert cas > 0 : "CAS was less than zero:  " + cas;
                val =
                        new CASValue<T>(cas, tc.decode(new CachedData(flags, data,
                        tc.getMaxSize())));
            }
        });

        OperationResult opResult = new OperationResult();
        String message = "";
        boolean success = false;
        CASValue<T> val = null;
        if (memcachedProvider != null) {
            try {
                opResult = memcachedProvider.touch(key, exp);
                switch (opResult.getReturnResult()) {
                    case SUCCESS:
                        success = true;
                        message = "TOUCHED";
                        GetOpResult getOpResult = (GetOpResult) opResult.getReturnValue();
                        long cas = getOpResult.getVersion();
                        byte[] data = (byte[]) getOpResult.getReturnValue();
                        int flags = getOpResult.getFlag();
                        val = new CASValue<T>(cas, tc.decode(new CachedData(flags, data,
                                tc.getMaxSize())));
                        break;
                    case ITEM_NOT_FOUND:
                        message = "NOT_FOUND";
                        break;
                }
            } catch (InvalidArgumentsException e) {
                message = "CLIENT_ERROR bad data chunk";
            } catch (Exception e) {
                message = "SERVER_ERROR";
            }
        }
        rv.set(val, new OperationStatus(success, message));
        rv.setOperation(op);
        return rv;

    }

    /**
     * Get the values for multiple keys from the cache.
     *
     * @param <T>
     * @param keyIter Iterator that produces the keys
     * @param tc the transcoder to serialize and unserialize value
     * @return a map of the values (for each value that exists)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws CancellationException if operation was canceled
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> Map<String, T> getBulk(Iterator<String> keyIter,
            Transcoder<T> tc) {
        try {
            return asyncGetBulk(keyIter, tc).get(operationTimeout,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted getting bulk values", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CancellationException) {
                throw (CancellationException) e.getCause();
            } else {
                throw new RuntimeException("Exception waiting for bulk values", e);
            }
        } catch (TimeoutException e) {
            throw new OperationTimeoutException("Timeout waiting for bulk values: "
                    + buildTimeoutMessage(operationTimeout, TimeUnit.MILLISECONDS), e);
        }
    }

    /**
     * Get the values for multiple keys from the cache.
     *
     * @param keyIter Iterator that produces the keys
     * @return a map of the values (for each value that exists)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public Map<String, Object> getBulk(Iterator<String> keyIter) {
        return getBulk(keyIter, transcoder);
    }

    /**
     * Get the values for multiple keys from the cache.
     *
     * @param <T>
     * @param keys the keys
     * @param tc the transcoder to serialize and unserialize value
     * @return a map of the values (for each value that exists)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> Map<String, T> getBulk(Collection<String> keys,
            Transcoder<T> tc) {
        return getBulk(keys.iterator(), tc);
    }

    /**
     * Get the values for multiple keys from the cache.
     *
     * @param keys the keys
     * @return a map of the values (for each value that exists)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public Map<String, Object> getBulk(Collection<String> keys) {
        return getBulk(keys, transcoder);
    }

    /**
     * Get the values for multiple keys from the cache.
     *
     * @param <T>
     * @param tc the transcoder to serialize and unserialize value
     * @param keys the keys
     * @return a map of the values (for each value that exists)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) {
        return getBulk(Arrays.asList(keys), tc);
    }

    /**
     * Get the values for multiple keys from the cache.
     *
     * @param keys the keys
     * @return a map of the values (for each value that exists)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public Map<String, Object> getBulk(String... keys) {
        return getBulk(Arrays.asList(keys), transcoder);
    }

    /**
     * Get the versions of all of the connected memcacheds.
     *
     * @return a Map of SocketAddress to String for connected servers
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public Map<SocketAddress, String> getVersions() {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        OperationResult opResult = new OperationResult();
        String version = "";
        if (memcachedProvider != null) {
            try {
                opResult = memcachedProvider.getVersion();
                version = opResult.getReturnValue().toString();
            } catch (Exception e) {
            }
        }
        final Map<SocketAddress, String> rv =
                new ConcurrentHashMap<SocketAddress, String>();
        SocketAddress sa = new InetSocketAddress("", 11211);
        rv.put(sa, version);

        return rv;
    }

    /**
     * Get all of the stats from all of the connections.
     *
     * @return a Map of a Map of stats replies by SocketAddress
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public Map<SocketAddress, Map<String, String>> getStats() {
        return getStats("");
    }

    /**
     * Get a set of stats from all connections.
     *
     * @param arg which stats to get
     * @return a Map of the server SocketAddress to a map of String stat keys to
     * String stat values.
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public Map<SocketAddress, Map<String, String>> getStats(final String arg) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        final Map<SocketAddress, Map<String, String>> rv =
                new HashMap<SocketAddress, Map<String, String>>();
        OperationResult opResult = new OperationResult();
        Map<String, String> hashMap = new HashMap<String, String>();
        if (memcachedProvider != null) {
            try {
                opResult = memcachedProvider.getStatistics(arg);
                Hashtable ht = (Hashtable) opResult.getReturnValue();
                hashMap = ht;
            } catch (Exception e) {
                hashMap = null;
            }
        }
        SocketAddress sa = new InetSocketAddress("", 11211);
        rv.put(sa, hashMap);
        return rv;
    }

    private long mutate(Mutator m, String key, long by, long def, int exp) {

        OperationFuture opFuture = asyncMutate(m, key, by, def, exp);
        long rv = -1;
        try {
            rv = (Long) opFuture.get();
        } catch (OperationTimeoutException e) {
            throw new OperationTimeoutException("Mutate operation timed out,"
                    + "unable to modify counter [" + key + "]");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        } catch (ExecutionException ex) {
        }
        return rv;
    }

    /**
     * Increment the given key by the given amount.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the amount to increment
     * @return the new value (-1 if the key doesn't exist)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long incr(String key, long by) {
        return mutate(Mutator.incr, key, by, 0, 0);
    }

    /**
     * Increment the given key by the given amount.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the amount to increment
     * @return the new value (-1 if the key doesn't exist)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long incr(String key, int by) {
        return mutate(Mutator.incr, key, (long) by, 0, 0);
    }

    /**
     * Decrement the given key by the given value.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the value
     * @return the new value (-1 if the key doesn't exist)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long decr(String key, long by) {
        return mutate(Mutator.decr, key, by, 0, 0);
    }

    /**
     * Decrement the given key by the given value.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the value
     * @return the new value (-1 if the key doesn't exist)
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long decr(String key, int by) {
        return mutate(Mutator.decr, key, (long) by, 0, 0);
    }

    /**
     * Increment the given counter, returning the new value.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the amount to increment
     * @param def the default value (if the counter does not exist)
     * @param exp the expiration of this object
     * @return the new value, or -1 if we were unable to increment or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long incr(String key, long by, long def, int exp) {
        return mutateWithDefault(Mutator.incr, key, by, def, exp);
    }

    /**
     * Increment the given counter, returning the new value.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the amount to increment
     * @param def the default value (if the counter does not exist)
     * @param exp the expiration of this object
     * @return the new value, or -1 if we were unable to increment or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long incr(String key, int by, long def, int exp) {
        return mutateWithDefault(Mutator.incr, key, (long) by, def, exp);
    }

    /**
     * Decrement the given counter, returning the new value.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the amount to decrement
     * @param def the default value (if the counter does not exist)
     * @param exp the expiration of this object
     * @return the new value, or -1 if we were unable to decrement or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long decr(String key, long by, long def, int exp) {
        return mutateWithDefault(Mutator.decr, key, by, def, exp);
    }

    /**
     * Decrement the given counter, returning the new value.
     *
     * Due to the way the memcached server operates on items, incremented and
     * decremented items will be returned as Strings with any operations that
     * return a value.
     *
     * @param key the key
     * @param by the amount to decrement
     * @param def the default value (if the counter does not exist)
     * @param exp the expiration of this object
     * @return the new value, or -1 if we were unable to decrement or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long decr(String key, int by, long def, int exp) {
        return mutateWithDefault(Mutator.decr, key, (long) by, def, exp);
    }

    private long mutateWithDefault(Mutator t, String key, long by, long def,
            int exp) {
        long rv = mutate(t, key, by, def, exp);
        // The ascii protocol doesn't support defaults, so I added them
        // manually here.
        if (rv == -1) {
            Future<Boolean> f = asyncStore(StoreType.add, key, exp,
                    String.valueOf(def));
            try {
                if (f.get(operationTimeout, TimeUnit.MILLISECONDS)) {
                    rv = def;
                } else {
                    rv = mutate(t, key, by, 0, exp);
                    assert rv != -1 : "Failed to mutate or init value";
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted waiting for store", e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof CancellationException) {
                    throw (CancellationException) e.getCause();
                } else {
                    throw new RuntimeException("Failed waiting for store", e);
                }
            } catch (TimeoutException e) {
                throw new OperationTimeoutException("Timeout waiting to mutate or init"
                        + " value" + buildTimeoutMessage(operationTimeout,
                        TimeUnit.MILLISECONDS), e);
            }
        }
        return rv;
    }

    private OperationFuture<Long> asyncMutate(Mutator m, String key, long by,
            long def, int exp) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<Long> rv =
                new OperationFuture<Long>(key, latch, operationTimeout);
        Operation op = opFact.mutate(m, key, by, def, exp,
                new OperationCallback() {
            public void receivedStatus(OperationStatus s) {
            }

            public void complete() {
            }
        });

        MutateOpResult opResult = new MutateOpResult();
        boolean success = false;
        String message = "-1";
        long ret = -1;
        if (memcachedProvider != null) {
            if (by < 0) {
                getLogger().error("OperationException: CLIENT: CLIENT_ERROR invalid numeric delta argument");
                message = "cancelled";
            } else {
                try {
                    switch (m) {
                        case incr:
                            if (opFact instanceof BinaryOperationFactory) {
                                opResult = memcachedProvider.increment(key, by, def, exp, 0);
                            } else {
                                opResult = memcachedProvider.increment(key, by, null, 0, 0);
                            }
                            break;
                        case decr:
                            if (opFact instanceof BinaryOperationFactory) {
                                opResult = memcachedProvider.decrement(key, by, def, exp, 0);
                            } else {
                                opResult = memcachedProvider.decrement(key, by, null, 0, 0);
                            }
                            break;
                    }
                    switch (opResult.getReturnResult()) {
                        case SUCCESS:
                            success = true;
                            message = opResult.getMutateResult() + "";
                            break;
                        case ITEM_NOT_FOUND:
                            message = "NOT_FOUND";
                            break;
                        case ITEM_TYPE_MISMATCHED:
                            getLogger().error("OperationException: CLIENT: CLIENT_ERROR cannot increment or decrement non-numeric value");

                    }
                } catch (InvalidArgumentsException e) {
                    message = "CLIENT_ERROR bad data chunk";
                } catch (Exception e) {
                    message = "SERVER_ERROR";
                }
            }
        }
        OperationStatus opStatus = new OperationStatus(success, message);
        rv.set(new Long(opStatus.isSuccess() ? opStatus.getMessage() : "-1"), opStatus);
        rv.setOperation(op);
        return rv;
    }

    /**
     * Asychronous increment.
     *
     * @param key key to increment
     * @param by the amount to increment the value by
     * @return a future with the incremented value, or -1 if the increment
     * failed.
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Long> asyncIncr(String key, long by) {
        return asyncMutate(Mutator.incr, key, by, 0, 0);
    }

    /**
     * Asychronous increment.
     *
     * @param key key to increment
     * @param by the amount to increment the value by
     * @return a future with the incremented value, or -1 if the increment
     * failed.
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Long> asyncIncr(String key, int by) {
        return asyncMutate(Mutator.incr, key, (long) by, 0, 0);
    }

    /**
     * Asynchronous decrement.
     *
     * @param key key to increment
     * @param by the amount to increment the value by
     * @return a future with the decremented value, or -1 if the increment
     * failed.
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Long> asyncDecr(String key, long by) {
        return asyncMutate(Mutator.decr, key, by, 0, 0);
    }

    /**
     * Asynchronous decrement.
     *
     * @param key key to increment
     * @param by the amount to increment the value by
     * @return a future with the decremented value, or -1 if the increment
     * failed.
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Long> asyncDecr(String key, int by) {
        return asyncMutate(Mutator.decr, key, (long) by, 0, 0);
    }

    /**
     * Increment the given counter, returning the new value.
     *
     * @param key the key
     * @param by the amount to increment
     * @param def the default value (if the counter does not exist)
     * @return the new value, or -1 if we were unable to increment or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long incr(String key, long by, long def) {
        return mutateWithDefault(Mutator.incr, key, by, def, 0);
    }

    /**
     * Increment the given counter, returning the new value.
     *
     * @param key the key
     * @param by the amount to increment
     * @param def the default value (if the counter does not exist)
     * @return the new value, or -1 if we were unable to increment or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long incr(String key, int by, long def) {
        return mutateWithDefault(Mutator.incr, key, (long) by, def, 0);
    }

    /**
     * Decrement the given counter, returning the new value.
     *
     * @param key the key
     * @param by the amount to decrement
     * @param def the default value (if the counter does not exist)
     * @return the new value, or -1 if we were unable to decrement or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long decr(String key, long by, long def) {
        return mutateWithDefault(Mutator.decr, key, by, def, 0);
    }

    /**
     * Decrement the given counter, returning the new value.
     *
     * @param key the key
     * @param by the amount to decrement
     * @param def the default value (if the counter does not exist)
     * @return the new value, or -1 if we were unable to decrement or add
     * @throws OperationTimeoutException if the global operation timeout is
     * exceeded
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public long decr(String key, int by, long def) {
        return mutateWithDefault(Mutator.decr, key, (long) by, def, 0);
    }

    /**
     * Delete the given key from the cache.
     *
     * <p>
     * The hold argument specifies the amount of time in seconds (or Unix time
     * until which) the client wishes the server to refuse "add" and "replace"
     * commands with this key. For this amount of item, the item is put into a
     * delete queue, which means that it won't possible to retrieve it by the
     * "get" command, but "add" and "replace" command with this key will also
     * fail (the "set" command will succeed, however). After the time passes,
     * the item is finally deleted from server memory.
     * </p>
     *
     * @param key the key to delete
     * @param hold how long the key should be unavailable to add commands
     *
     * @return whether or not the operation was performed
     * @deprecated Hold values are no longer honored.
     */
    @Deprecated
    public OperationFuture<Boolean> delete(String key, int hold) {
        return delete(key);
    }

    /**
     * Delete the given key from the cache.
     *
     * @param key the key to delete
     * @return whether or not the operation was performed
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> delete(String key) {
        return delete(key, (long) 0);
    }

    /**
     * Delete the given key from the cache of the given CAS value applies.
     *
     * @param key the key to delete
     * @param cas the CAS value to apply.
     * @return whether or not the operation was performed
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> delete(String key, long cas) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
        boolean withCAS = false;
        if (opFact instanceof BinaryOperationFactory && cas > 0) {
            withCAS = true;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<Boolean> rv = new OperationFuture<Boolean>(key,
                latch, operationTimeout);

        DeleteOperation.Callback callback = new DeleteOperation.Callback() {
            public void receivedStatus(OperationStatus s) {
            }

            public void gotData(long cas) {
            }

            public void complete() {
            }
        };

        DeleteOperation op = null;
        if (cas == 0) {
            op = opFact.delete(key, callback);
        } else {
            op = opFact.delete(key, cas, callback);
        }
        if (cas < 0) {
            if (opFact instanceof BinaryOperationFactory) {
                rv.set(false, new CASOperationStatus(false, "Data exists for key.", CASResponse.EXISTS));
            }
        } else {
            OperationResult opResult = new OperationResult();
            boolean success = false;
            String message = "";
            CASResponse response = null;
            if (memcachedProvider != null) {
                try {
                    opResult = memcachedProvider.delete(key, cas);
                    switch (opResult.getReturnResult()) {
                        case SUCCESS:
                            success = true;
                            response = CASResponse.OK;
                            message = "DELETED";
                            break;
                        case ITEM_NOT_FOUND:
                            response = CASResponse.NOT_FOUND;
                            message = "NOT_FOUND";
                            break;
                        case ITEM_MODIFIED:
                            response = CASResponse.EXISTS;
                            message = "EXISTS";
                            break;
                    }
                } catch (InvalidArgumentsException e) {
                    message = "CLIENT_ERROR bad data chunk";
                } catch (Exception e) {
                    message = "SERVER_ERROR";
                }
            }
            if (withCAS) {
                CASOperationStatus val = new CASOperationStatus(success, response.toString(), response);
                rv.set(val.isSuccess(), val);
            } else {
                rv.set(success, new OperationStatus(success, message));
            }
            if (opFact instanceof BinaryOperationFactory && success) {
                rv.setCas(0);
            }
        }
        rv.setOperation(op);
        return rv;
    }

    /**
     * Flush all caches from all servers with a delay of application.
     *
     * @param delay the period of time to delay, in seconds
     * @return whether or not the operation was accepted
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> flush(final int delay) {
        if (!isAlive) {
            java.lang.IllegalStateException ex = new java.lang.IllegalStateException("Shutting down");
            throw ex;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        OperationFuture<Boolean> opFuture = new OperationFuture<Boolean>(null, latch, new AtomicReference<Boolean>(null),
                operationTimeout) {
            @Override
            public boolean cancel(boolean ign) {
                return true;
            }

            @Override
            public Boolean get(long duration, TimeUnit units) {
                return true;
            }

            @Override
            public boolean isCancelled() {
                return true;
            }

            @Override
            public boolean isDone() {
                return true;
            }
        };
        boolean success = false;
        String message = "OK";
        try {
            OperationResult opResult = memcachedProvider.flush_All(delay);
            success = true;
        } catch (InvalidArgumentsException e) {
            message = "CLIENT_ERROR bad data chunk";
        } catch (Exception e) {
            message = "SERVER_ERROR";
        }
        opFuture.set(success, new OperationStatus(success, message));
        return opFuture;
    }

    /**
     * Flush all caches from all servers immediately.
     *
     * @return whether or not the operation was performed
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public OperationFuture<Boolean> flush() {
        return flush(0);
    }

    public Set<String> listSaslMechanisms() {
        final ConcurrentMap<String, String> rv =
                new ConcurrentHashMap<String, String>();
        return rv.keySet();
    }

    /**
     * Shut down immediately.
     */
    public void shutdown() {
        shutdown(-1, TimeUnit.MILLISECONDS);
    }

    /**
     * Shut down this client gracefully.
     *
     * @param timeout the amount of time time for shutdown
     * @param unit the TimeUnit for the timeout
     * @return result of the shutdown request
     */
    public boolean shutdown(long timeout, TimeUnit unit) {
        // Guard against double shutdowns (bug 8).
        if (shuttingDown) {
            getLogger().info("Suppressing duplicate attempt to shut down");
            return false;
        }
        shuttingDown = true;
        boolean rv = true;
        // Conditionally wait
        if (timeout > 0) {
            long dueTimeInMilliseconds = timeout * 1000;
            final Timer _timer = new Timer();
            _timer.schedule(new TimerTask() {
                public void run() {
                    try {
                        CacheFactory.disposeCacheProvider();
                        tcService.shutdown();
                        _timer.cancel();
                    } catch (Exception e) {
                        getLogger().warn("exception while shutting down", e);
                    }
                }
            }, dueTimeInMilliseconds);
        } else // But always begin the shutdown sequence
        {
            try {
                CacheFactory.disposeCacheProvider();
                tcService.shutdown();
            } catch (Exception e) {
                getLogger().warn("exception while shutting down", e);
            }
        }
        isAlive = false;
        return rv;
    }

    /**
     * Wait for the queues to die down.
     *
     * @param timeout the amount of time time for shutdown
     * @param unit the TimeUnit for the timeout
     * @return result of the request for the wait
     * @throws IllegalStateException in the rare circumstance where queue is too
     * full to accept any more requests
     */
    public boolean waitForQueues(long timeout, TimeUnit unit) {
        return true;
    }

    /**
     * Add a connection observer.
     *
     * If connections are already established, your observer will be called with
     * the address and -1.
     *
     * @param obs the ConnectionObserver you wish to add
     * @return true if the observer was added.
     */
    public boolean addObserver(ConnectionObserver obs) {
        return true;
    }

    /**
     * Remove a connection observer.
     *
     * @param obs the ConnectionObserver you wish to add
     * @return true if the observer existed, but no longer does
     */
    public boolean removeObserver(ConnectionObserver obs) {
        return true;
    }

    public void connectionEstablished(SocketAddress sa, int reconnectCount) {
    }

    private MemcachedNode findNode(SocketAddress sa) {
        MemcachedNode node = null;
        return node;
    }

    private String buildTimeoutMessage(long timeWaited, TimeUnit unit) {
        StringBuilder message = new StringBuilder();

        message.append(MessageFormat.format("waited {0} ms.",
                unit.convert(timeWaited, TimeUnit.MILLISECONDS)));
        return message.toString();
    }

    public void connectionLost(SocketAddress sa) {
        // Don't care.
    }

    private enum ResponseType {

        stored, exists, notfound, deleted, ended, nonnumeric
    }

    private String getMessage(ResponseType responseType) {
        String message = "";
        boolean isBinary = false;
        if (opFact instanceof BinaryOperationFactory) {
            isBinary = true;
        }
        switch (responseType) {
            case stored:
                if (isBinary) {
                    message = "OK";
                } else {
                    message = "STORED";
                }
                break;
            case exists:
                if (isBinary) {
                    message = "Data exists for key.";
                } else {
                    message = "EXISTS";
                }
                break;
            case notfound:
                if (isBinary) {
                    message = "Not found";
                } else {
                    message = "NOT_FOUND";
                }
                break;
            case deleted:
                if (isBinary) {
                    message = "OK";
                } else {
                    message = "DELETED";
                }
                break;
            case ended:
                if (isBinary) {
                    message = "OK";
                } else {
                    message = "END";
                }
                break;
            case nonnumeric:
                if (isBinary) {
                    message = "Non-numeric server-side value for incr or decr";
                } else {
                    message = "Cancelled";
                }
                break;
        }

        return message;
    }

    @Override
    public String toString() {
        return connFactory.toString();
    }
}
