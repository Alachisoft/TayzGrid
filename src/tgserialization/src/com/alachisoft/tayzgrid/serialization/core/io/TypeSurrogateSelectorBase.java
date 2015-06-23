/*
 * @(#)TypeSurrogateSelectorBase.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io;

import com.alachisoft.tayzgrid.serialization.core.PackageResources;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.BuiltinSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * TypeSurrogateSelectorBase class.
 *
 * @version 1.0, September 18, 2008
 */
public class TypeSurrogateSelectorBase implements TypeSurrogateSelector
{
    /**
     * Minimum id that can be assigned to a user defined type.
     */
    protected SerializationSurrogate mNullSurrogate;

    /**
     * Minimum id that can be assigned to a user defined type.
     */
    protected SerializationSurrogate mDefaultSurrogate;

    protected SerializationSurrogate mDefaultArraySurrogate;

    /**
     * Minimum id that can be assigned to a user defined type.
     */
    protected Map<Class, SerializationSurrogate> mTypeSurrogateMap;

    protected Map<String, HashMap<Class, SerializationSurrogate>> mCacheContextTypeSurrogateMap;
    protected Map<String, HashMap<Short, Object>> mCacheContextHandleSurrogateMap;

    /**
     * Minimum id that can be assigned to a user defined type.
     */
    protected Map<Short, SerializationSurrogate> mHandleSurrogateMap;

    /**
     * Minimum id that can be assigned to a user defined type.
     */
    protected Short mNewTypeHandle;

    /**
     * Creates a new instance of TypeSurrogateSelectorBase
     */
    public TypeSurrogateSelectorBase() {
        construct(true);
    }

    /**
     * Creates a new instance of TypeSurrogateSelectorBase
     */
    public TypeSurrogateSelectorBase(boolean useBuiltinSurrogates)
    {
        construct(useBuiltinSurrogates);
    }

    private void construct(boolean useBuiltinSurrogates)
    {
        mTypeSurrogateMap = new HashMap<Class, SerializationSurrogate>();
        mHandleSurrogateMap = new HashMap<Short, SerializationSurrogate>();
        mCacheContextTypeSurrogateMap = new HashMap<String, HashMap<Class, SerializationSurrogate>>();
        mCacheContextHandleSurrogateMap = new HashMap<String, HashMap<Short, Object>>();
        mNewTypeHandle = TypeSurrogateConstants.MinTypeHandle;
    }

    /*
     * Returns the default type surrogate selector Object
    */
    public SerializationSurrogate getNullSurrogate()
    {
        return this.mNullSurrogate;
    }

    /*
     * Returns the default type surrogate selector Object
    */
    public void setNullSurrogate(SerializationSurrogate value)
    {
        this.mNullSurrogate = value;
    }
    /*
     * Returns the default type surrogate selector Object
    */
    public SerializationSurrogate getDefaultSurrogate()
    {
        return this.mDefaultSurrogate;
    }

    /*
     * Returns the default type surrogate selector Object
    */
    public void setDefaultSurrogate(SerializationSurrogate value)
    {
        this.mDefaultSurrogate = value;
    }

    /**
     * Use this to determine whether the specified surrogate is a built-in surrogate
     * @return true if the specified surrogate is a built-in surrogate
     */
    public boolean isBuiltinSurrogate(SerializationSurrogate surrogate)
    {
        if(surrogate == null)
            throw new IllegalArgumentException();
        return BuiltinSerializationSurrogate.class.isAssignableFrom(surrogate.getClass());
    }

    /**
     * Finds and returns an appropriate <tt>SerializationSurrogate</tt> for the given
     * Object. If no surrogate is found for the specified object, the <tt>Default"<tt/>
     * surrogate is returned.
     *
     * @param graph the Object whose surrogate is to be found.
     * @return an <tt>SerializationSurrogate</tt> Object
     * @see SerializationSurrogate
     */
    public SerializationSurrogate getSurrogateForObject(Object graph, String cacheContext)
    {
        if (graph == null)
            return mNullSurrogate;
        return getSurrogateForType(graph.getClass(), cacheContext.toLowerCase());
    }

    /**
     * Finds and returns an appropriate <tt>SerializationSurrogate</tt> for the given
     * type. If no surrogate is found for the specified type, the <tt>Default"<tt/>
     * surrogate is returned.
     *
     * @param type the Class whose surrogate is to be found.
     * @return an <tt>SerializationSurrogate</tt> Object
     * @see SerializationSurrogate
     */
    public SerializationSurrogate getSurrogateForType(Class type, String cacheContext)
    {
        SerializationSurrogate surrogate = (SerializationSurrogate) mTypeSurrogateMap.get(type);

        if(surrogate == null && cacheContext != null)
        {
            if (mCacheContextTypeSurrogateMap.containsKey(cacheContext.toLowerCase()))
            {
                surrogate = (SerializationSurrogate) mCacheContextTypeSurrogateMap.get(cacheContext.toLowerCase()).get(type);
            }
        }

        if (surrogate == null && !type.isArray())
            surrogate = mDefaultSurrogate;
        else if(surrogate == null && type.isArray())
            surrogate = mDefaultArraySurrogate;
        return surrogate;
    }

    /**
     * Finds and returns an appropriate <tt>SerializationSurrogate</tt> for the given
     * Object. Allows you to inhibit the usage of default surrogate. If no surrogate is
     * found for the specified object and strict is false, the <tt>Default"<tt/> surrogate
     * is returned.
     *
     * @param type the Class whose surrogate is to be found.
     * @param strict specified whether the default surrogate will be returned in case of
     * no match.
     * @return an <tt>SerializationSurrogate</tt> Object if a match is found, otherwise
     * retursn null or the default surrogate depending upon the value of "strict".
     * @see SerializationSurrogate
     */
    public SerializationSurrogate getSurrogateForType(Class type, boolean strict, String cacheContext)
    {
        SerializationSurrogate surrogate = (SerializationSurrogate) mTypeSurrogateMap.get(type);

        if(surrogate == null && cacheContext != null)
        {
            if (mCacheContextTypeSurrogateMap.containsKey(cacheContext.toLowerCase()))
            {
                surrogate = (SerializationSurrogate) mCacheContextTypeSurrogateMap.get(cacheContext.toLowerCase()).get(type);
            }
        }

        if (!strict && surrogate == null && !type.isArray())
            surrogate = mDefaultSurrogate;
        else if(!strict && surrogate == null && type.isArray())
            surrogate = mDefaultArraySurrogate;
        return surrogate;
    }

    /**
     * Finds and returns an appropriate <tt>SerializationSurrogate</tt> for the given
     * type handle. If no surrogate is found for the specified object, the <tt>Default"<tt/> surrogate
     * is returned.
     *
     * @param handle the handle of the surrogate that is to be returned.
     * @return an <tt>SerializationSurrogate</tt> Object if a match is found, otherwise
     * retursn the default surrogate.
     * @see SerializationSurrogate
     */
    public SerializationSurrogate getSurrogateForTypeHandle(short handle, String cacheContext)
    {
        SerializationSurrogate surrogate = (SerializationSurrogate)mHandleSurrogateMap.get(handle);

        if(surrogate == null && cacheContext != null)
        {
            if (mCacheContextTypeSurrogateMap.containsKey(cacheContext.toLowerCase()))
            {
                HashMap userMap = mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase());
                if(userMap != null && userMap.containsKey(handle))
                {
                    if(userMap.get(handle) instanceof SerializationSurrogate)
                        surrogate = (SerializationSurrogate)userMap.get(handle);
                    else
                        return null;

                }
                surrogate = (SerializationSurrogate) mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase()).get(handle);
            }
        }

        if(surrogate == null)
            surrogate = mDefaultSurrogate;

        return surrogate;
    }

    public SerializationSurrogate GetSurrogateForSubTypeHandle(short handle, short subHandle, String cacheContext)
        {
            SerializationSurrogate surrogate = null;
            HashMap userTypeMap = (HashMap)mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase());
            if (userTypeMap != null && userTypeMap.containsKey(handle))
            {
                surrogate = (SerializationSurrogate)((HashMap)userTypeMap.get(handle)).get(subHandle);
                if (surrogate == null && ((HashMap)userTypeMap.get(handle)).size() > 0)
                {
                    Iterator surr = ((HashMap)userTypeMap.get(handle)).entrySet().iterator();
                    Map.Entry pair = (Map.Entry) surr.next();
                    surrogate = (SerializationSurrogate)pair.getValue();
                }
            }

            if (surrogate == null)
                surrogate = mDefaultSurrogate;
            return surrogate;
        }

    /**
     * Registers the specified <tt>SerializationSurrogate<tt/> with the system.
     *
     * @param surrogate specified surrogate
     * @return false if the surrogated type already has a surrogate
     */
    public boolean register(SerializationSurrogate surrogate)
         throws CacheArgumentException
    {
        if (surrogate == null)
            throw new CacheArgumentException();

        for (;;)
        {
            try {
                return register(surrogate, ++mNewTypeHandle);
            } catch (IllegalArgumentException e) {
                continue;
            } catch (CacheArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new CacheArgumentException(e);
            }
        }
    }

    /**
     * Registers the specified <tt>SerializationSurrogate<tt/> with the given type handle.
     * Gives more control over the way type handles are generated by the system and allows the
     * user to supply *HARD* handles for better interoperability among applications.
     *
     * @param surrogate specified surrogate
     * @param typeHandle specified HARD handle for type. <code>typeHandle<code/> must be in the
     * range <code>TypeSurrogateSelector.MinTypeHandle<code/> and <code> TypeSurrogateSelector.MaxTypeHandle<code/> (inclusive)
     * @return false if the surrogated type already has a surrogate
     */
    public boolean register(SerializationSurrogate surrogate, short typeHandle)
     throws CacheArgumentException
    {
        if (!isBuiltinSurrogate(surrogate))
        {
            if (typeHandle < TypeSurrogateConstants.MinTypeHandle)
            {
                throw new CacheArgumentException(PackageResources.Surrogates_HandleOutOfRange);
            }
        }

        synchronized(mTypeSurrogateMap)
        {
            if (mHandleSurrogateMap.containsKey(typeHandle))
                throw new IllegalArgumentException(PackageResources.Surrogates_AlreadyRegistered);

            if (!mTypeSurrogateMap.containsKey(surrogate.getClassHandle()))
            {
                surrogate.setClassHandle(typeHandle);
                mTypeSurrogateMap.put(surrogate.getRealClass(), surrogate);
                mHandleSurrogateMap.put(surrogate.getClassHandle(), surrogate);
                return true;
            }
        }
        return false;
    }

    /**
     * Registers the specified <tt>SerializationSurrogate<tt/> with the given type handle.
     * Gives more control over the way type handles are generated by the system and allows the
     * user to supply *HARD* handles for better inter-operability among applications.
     *
     * @param surrogate specified surrogate
     * @param typeHandle specified HARD handle for type. <code>typeHandle<code/> must be in the
     * range <code>TypeSurrogateSelector.MinTypeHandle<code/> and <code> TypeSurrogateSelector.MaxTypeHandle<code/> (inclusive)
     * @param cacheContext CacheName on which the surrogate is to be registered
     * @return false if the surrogated type already has a surrogate
     */
    public boolean register(SerializationSurrogate surrogate, short typeHandle, short subHandle, String cacheContext, boolean portable)
     throws CacheArgumentException
    {
        if (!isBuiltinSurrogate(surrogate))
        {
            if (typeHandle < TypeSurrogateConstants.MinTypeHandle)
            {
                throw new CacheArgumentException(PackageResources.Surrogates_HandleOutOfRange);
            }
        }

        if (mCacheContextTypeSurrogateMap.containsKey(cacheContext.toLowerCase()))
        {
            synchronized (mCacheContextTypeSurrogateMap)
            {
                if (mHandleSurrogateMap.containsKey(typeHandle) && mTypeSurrogateMap.containsKey(surrogate.getClassHandle()))
                    throw new IllegalArgumentException(PackageResources.Surrogates_AlreadyRegistered);

                if (portable)
                {
                    surrogate.setClassHandle(typeHandle);
                    if (!mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase()).containsKey(surrogate.getClassHandle()))
                    {
                        mCacheContextTypeSurrogateMap.get(cacheContext.toLowerCase()).put(surrogate.getRealClass(), surrogate);
                        HashMap temp = (new HashMap<Short, SerializationSurrogate>());
                        temp.put(surrogate.getSubHandle(), surrogate);
                        mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase()).put(surrogate.getClassHandle(),temp);
                        return true;
                    }
                    else
                    {
                        mCacheContextTypeSurrogateMap.get(cacheContext.toLowerCase()).put(surrogate.getRealClass(), surrogate);
                        HashMap typeMap = (HashMap)mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase()).get(surrogate.getClassHandle());
                        typeMap.put(surrogate.getSubHandle(), surrogate);
                    }
                }
                else
                {
                    if (!mCacheContextTypeSurrogateMap.get(cacheContext.toLowerCase()).containsKey(surrogate.getClassHandle()))
                    {
                        surrogate.setClassHandle(typeHandle);
                        mCacheContextTypeSurrogateMap.get(cacheContext.toLowerCase()).put(surrogate.getRealClass(), surrogate);
                        mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase()).put(surrogate.getClassHandle(), surrogate);
                        return true;
                    }
                }
            }
        }
        else
        {
            surrogate.setClassHandle(typeHandle);


            if (portable)
            {
                HashMap temp1 = new HashMap<Class, SerializationSurrogate>();
                temp1.put(surrogate.getRealClass(), surrogate);

                HashMap temp2 = new HashMap<Short, Object>();
                HashMap temp3 = new HashMap<Short, SerializationSurrogate>();
                temp3.put(surrogate.getSubHandle(), surrogate);
                temp2.put(surrogate.getClassHandle(), temp3);

                mCacheContextTypeSurrogateMap.put(cacheContext.toLowerCase(), temp1);
                mCacheContextHandleSurrogateMap.put(cacheContext.toLowerCase(), temp2);
            }
            else
            {
                HashMap temp1 = new HashMap<Class, SerializationSurrogate>();
                temp1.put(surrogate.getRealClass(), surrogate);
                HashMap temp2 = new HashMap<Short, SerializationSurrogate>();
                temp2.put(surrogate.getClassHandle(), surrogate);
                mCacheContextTypeSurrogateMap.put(cacheContext.toLowerCase(), temp1);
                mCacheContextHandleSurrogateMap.put(cacheContext.toLowerCase(), temp2);
            }
        }
        return false;
    }

    /**
     * Unregisters the specified <tt>SerializationSurrogate<tt/> from the system.
     *
     * @param surrogate specified surrogate
     * @param cacheContext null if Builtin surrogate is to be removed, else name of the cache
     *  @param removeCache false if Builtin surrogate is to be removed or only a single surrogate type is to be removed, else whole cache context is removed
     */
    public void unregister(SerializationSurrogate surrogate, String cacheContext, boolean removeCache)
    {
        if (surrogate == null && !removeCache)
            throw new IllegalArgumentException();

        if (cacheContext == null && removeCache == false) {
            synchronized (mTypeSurrogateMap) {
                mTypeSurrogateMap.remove(surrogate.getRealClass());
                mHandleSurrogateMap.remove(surrogate.getClassHandle());
            }
        }
        else
        {
            if(removeCache)
            {
                mCacheContextTypeSurrogateMap.remove(cacheContext.toLowerCase());
                mCacheContextHandleSurrogateMap.remove(cacheContext.toLowerCase());
            }
            else
            {
                HashMap temp1 = new HashMap<Class, SerializationSurrogate>();
                temp1 = mCacheContextTypeSurrogateMap.get(cacheContext.toLowerCase());
                temp1.remove(surrogate.getRealClass());
                HashMap temp2 = new HashMap<Short, SerializationSurrogate>();
                temp2 = mCacheContextHandleSurrogateMap.get(cacheContext.toLowerCase());
                temp2.remove(surrogate.getClassHandle());
            }
        }
    }

    /**
     * Unregisters all surrogates, except null and default ones.
     */
    public void clear()
    {
        synchronized(mTypeSurrogateMap)
        {
            mTypeSurrogateMap.clear();
            mHandleSurrogateMap.clear();

            mNewTypeHandle = TypeSurrogateConstants.MinTypeHandle;
        }
    }
}
