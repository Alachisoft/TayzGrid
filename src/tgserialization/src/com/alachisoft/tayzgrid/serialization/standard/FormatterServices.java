/*
 * @(#)FormatterServices.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.standard;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateSelector;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.TypeSurrogateSelectorImpl;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EnumSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ExternalizableSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.GenericArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.InternalCompactSerializableSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.XExternalizableSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ThrowableSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.VersionCompatibleInternalCompactSerializableSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.VersionCompatibleXExternalizableSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * FormatterServices class.
 *
 * @version 1.0, September 18, 2008
 */
public class FormatterServices
{

    private static FormatterServices msDefault;
    private TypeSurrogateSelector mSurrogateSelector;
    private String _cacheContext;

    public void SetCacheContext(String context)
    {
        this._cacheContext = context;
    }
    /**
     * Creates a new instance of FormatterServices
     */
    public FormatterServices()
    {
        mSurrogateSelector = TypeSurrogateSelectorImpl.getDefault();
    }

    /**
     * Creates a new instance of FormatterServices
     *
     * @param selector
     */
    public FormatterServices(TypeSurrogateSelector selector)
    {
        if (selector == null)
        {
            throw new NullPointerException();
        }
        this.mSurrogateSelector = selector;
    }

    /*
     * Returns the default type surrogate selector Object
     */
    public static FormatterServices getDefault()
    {
        if (FormatterServices.msDefault == null)
        {
            FormatterServices.msDefault = new FormatterServices();
        }
        return FormatterServices.msDefault;
    }

    /*
     * Returns the default type surrogate selector Object
     */
    public TypeSurrogateSelector getSurrogateSelector()
    {
        return this.mSurrogateSelector;
    }

    /*
     * Returns the default type surrogate selector Object
     */
    public void setSurrogateSelector(TypeSurrogateSelector selector)
    {
        if (selector == null)
        {
            throw new NullPointerException();
        }
        this.mSurrogateSelector = selector;
    }

    /*
     * Registers a type that implements <see cref="INxSerializable"/> with the system. If the type is an array of <see cref="INxSerializable"/>s appropriate surrogates for arrays
     * and the element type are also registered.
     *
     * Else the class is sent to be dynimcally serialized
     *
     * <param name="type">type that implements <see cref="INxSerializable"/></param> <param name="typeHandle">specified HARD handle for type</param> <exception
     * cref="ArgumentNullException">If <paramref name="type"/> is null</exception> <exception cref="ArgumentException"> If the <paramref name="type"/> is already registered or when
     * no appropriate surrogate is found for the specified <paramref name="type"/>. </exception>
     */
    public void registerKnownType(Class cls, short typeHandle, short subHandle, String cacheContext, boolean portable,HashMap nonCompactFieldsMap)
            throws CacheArgumentException
    {
        if (cls == null)
        {
            throw new NullPointerException("cls");
        }
        SerializationSurrogate surrogate = mSurrogateSelector.getSurrogateForType(cls, true, cacheContext);
        if (surrogate != null)
        {
            if(surrogate.getClassHandle() == typeHandle && (surrogate.getSubHandle() == subHandle || surrogate.getSubHandle()!=0))
                return; //Type is already registered with same handle.
            throw new IllegalArgumentException(PackageResources.Surrogates_AlreadyRegistered);
        }

        //SerializationSurrogate surrogate = null;
        try
        {
            surrogate = selectBestSurrogate(cls, portable, subHandle,nonCompactFieldsMap, false);
        }
        catch (Exception exception)
        {
            throw new CacheArgumentException(exception.getMessage());
        }
        if (surrogate == null)
        {
            throw new IllegalArgumentException(PackageResources.Surrogates_NotFound);
        }

        this.mSurrogateSelector.register(surrogate, typeHandle, surrogate.getSubHandle(), cacheContext, portable);
    }

    public void register(Map type, HashMap attributeOrder, String cacheContext, HashMap portable) throws CacheArgumentException
    {
       
        this._cacheContext = cacheContext;
        setAttributeOrder(attributeOrder);

        Iterator it = type.entrySet().iterator();
        while (it.hasNext())
        {
            
            try{
            Map.Entry pairs = (Map.Entry) it.next();
            
            HashMap handleNonCompactFieldMap=(HashMap)pairs.getValue();
            Short typeHandle=(Short)handleNonCompactFieldMap.get("handle");
            
            HashMap nonCompactFieldsMap=(HashMap)handleNonCompactFieldMap.get("non-compact-fields");            

            boolean isPortable = false;
            if (portable != null && portable.get((Short)typeHandle) != null)
            {
                isPortable = (Boolean) portable.get((Short) typeHandle);
            }
            short subHandle = 0;
            if (isPortable)
            {
                subHandle = SerializationUtil.GetSubTypeHandle(cacheContext, Short.toString((Short) typeHandle), (Class) pairs.getKey());
            }

            registerKnownType((Class) pairs.getKey(), (Short) typeHandle, subHandle, cacheContext, isPortable,nonCompactFieldsMap);
            }catch(Exception e)
            {
                
            }
        }
       
    }
    static HashMap _attributeOrder = new HashMap();

    public static void setAttributeOrder(HashMap attrib)
    {
        _attributeOrder = attrib;
    }

    public static HashMap getAttributeOrder()
    {
        return _attributeOrder;
    }

    /*
     * Registers a type that implements <see cref="INxSerializable"/> with the system. If the type is an array of <see cref="INxSerializable"/>s appropriate surrogates for arrays
     * and the element type are also registered. </summary> <param name="type">type that implements <see cref="INxSerializable"/></param> <exception cref="ArgumentNullException">If
     * <paramref name="type"/> is null. </exception> <exception cref="ArgumentException"> If the <paramref name="type"/> is already registered or when no appropriate surrogate is
     * found for the specified <paramref name="type"/>. </exception>
     */
    public void registerKnownType(Class cls)
            throws CacheArgumentException
    {
        if (cls == null)
        {
            throw new NullPointerException("cls");
        }
        if (this.mSurrogateSelector.getSurrogateForType(cls, true, _cacheContext) != null)
        {
            throw new IllegalArgumentException(PackageResources.Surrogates_AlreadyRegistered);
        }

        SerializationSurrogate surrogate = null;
        try
        {
            //portable is kept true to keep things working as if Portable types are registerd
            surrogate = selectBestSurrogate(cls, false, (short) 0,null, false);
        }
        catch (Exception exception)
        {
            throw new CacheArgumentException(exception.getMessage());
        }
        if (surrogate == null)
        {
            throw new IllegalArgumentException(PackageResources.Surrogates_NotFound);
        }

        this.mSurrogateSelector.register(surrogate);
    }

        /**
     * Registers a type that implements <see cref="INxSerializable"/> with the system. If the type is an array of <see cref="INxSerializable"/>s appropriate surrogates for arrays
     * and the element type are also registered. </summary> <param name="type">type that implements <see cref="INxSerializable"/></param> <exception cref="ArgumentNullException">If
     * <paramref name="type"/> is null. </exception> <exception cref="ArgumentException"> If the <paramref name="type"/> is already registered or when no appropriate surrogate is
     * found for the specified <paramref name="type"/>. </exception>
     *
     * @param cls Class to register
     * @param handle the handle to which the class is to be registered, if the handle already exists IllegalArgumentException will be thrown "Surrogates_AlreadyRegistered"
     * @throws CacheArgumentException when class is null or this handle is already taken or any exception arrives from selecting the best surrogate
     */
    public void registerKnownTypes(Class cls, short handle) throws CacheArgumentException
    {
        registerKnownTypes(cls, handle, true);        
    }
    
    /**
     * Registers a type that implements <see cref="INxSerializable"/> with the system. If the type is an array of <see cref="INxSerializable"/>s appropriate surrogates for arrays
     * and the element type are also registered. </summary> <param name="type">type that implements <see cref="INxSerializable"/></param> <exception cref="ArgumentNullException">If
     * <paramref name="type"/> is null. </exception> <exception cref="ArgumentException"> If the <paramref name="type"/> is already registered or when no appropriate surrogate is
     * found for the specified <paramref name="type"/>. </exception>
     *
     * @param cls Class to register
     * @param handle the handle to which the class is to be registered, if the handle already exists IllegalArgumentException will be thrown "Surrogates_AlreadyRegistered"
     * @throws CacheArgumentException when class is null or this handle is already taken or any exception arrives from selecting the best surrogate
     */
    public void registerNonVersionCompatibleCompactType(Class cls, short handle) throws CacheArgumentException
    {
        registerKnownTypes(cls, handle, false);
    }
    
    private void registerKnownTypes(Class cls, short handle, boolean versionCompatible) throws CacheArgumentException
    {
        if (cls == null)
        {
            throw new NullPointerException("cls");
        }
        if (this.mSurrogateSelector.getSurrogateForType(cls, true, _cacheContext) != null)
        {
            SerializationSurrogate tempSurrogate = this.mSurrogateSelector.getSurrogateForTypeHandle(handle, _cacheContext);
            if (tempSurrogate != null && tempSurrogate.getRealClass().equals(cls))
            {
                return;
            }
            throw new IllegalArgumentException(PackageResources.Surrogates_AlreadyRegistered);
        }

        SerializationSurrogate surrogate = null;
        try
        {
            //portable is kept true to keep things working as if Portable types are registerd
            surrogate = selectBestSurrogate(cls, false, (short) 0,null, versionCompatible);
        }
        catch (Exception exception)
        {
            throw new CacheArgumentException(exception.getMessage());
        }
        if (surrogate == null)
        {
            throw new IllegalArgumentException(PackageResources.Surrogates_NotFound);
        }

        this.mSurrogateSelector.register(surrogate, handle);
    }

    /*
     *
     * <see cref="INxSerializable"/> from the system. </summary> <param name="type">the specified type</param>
     */
    /**
     * Unregisters the surrogate for the specified type that implements
     *
     * @param cls Class type
     * @param cacheContext name of the cache
     * @param removeCache set to true if all the registered types with respect to this cache is to be removed, param class can be any/null for this
     */
    public void unregisterKnownType(Class cls, String cacheContext, boolean removeCache)
    {
        if (cls == null && !removeCache)
        {
            throw new NullPointerException("cls");
        }

        SerializationSurrogate surrogate = null;

        if (!removeCache)
        {
            surrogate = mSurrogateSelector.getSurrogateForType(cls, true, _cacheContext);
        }

        this.mSurrogateSelector.unregister(surrogate, cacheContext, removeCache);
    }

    /*
     * Finds and returns the best surrogate responsible for serializing the specified <paramref name="type"/> </summary> <param name="type">the given <see cref="Type"/>, whose
     * surrogate is returned</param> <returns>instance of <see cref="INxSerializationSurrogate"/></returns>
     */
    private SerializationSurrogate selectBestSurrogate(Class cls, boolean portable, short subHandle,HashMap nonCompactFieldsMap, boolean versionCompatible) throws Exception
    {
        SerializationSurrogate surrogate = null;

        if (Throwable.class.isAssignableFrom(cls))
        {
            surrogate = new ThrowableSerializationSurrogate(cls);
        }
        else if(cls.isArray())
        {
            surrogate = new GenericArraySerializationSurrogate(cls);
        }
        else if (ICompactSerializable.class.isAssignableFrom(cls))
        {
            if(versionCompatible)
                surrogate=new VersionCompatibleXExternalizableSerializationSurrogate(cls);
            else
                surrogate = new XExternalizableSerializationSurrogate(cls);
        }
        else if (InternalCompactSerializable.class.isAssignableFrom(cls))
        {
            if(versionCompatible)
                surrogate=new VersionCompatibleInternalCompactSerializableSurrogate(cls);
            else
                surrogate = new InternalCompactSerializableSurrogate(cls);
        }
        else if (ICompactSerializable.class.isAssignableFrom(cls))
        {
                surrogate = new ExternalizableSerializationSurrogate(cls);
        }
        else if (Enum.class.isAssignableFrom(cls))
        {
            surrogate = new EnumSerializationSurrogate(cls);
        }
        else 
        {
            surrogate = DynamicSurrogateBuilder.createTypeSurrogate(mSurrogateSelector, cls, getAttributeOrder(), this._cacheContext, portable, subHandle,nonCompactFieldsMap);
        }
        return surrogate;
    }
}
