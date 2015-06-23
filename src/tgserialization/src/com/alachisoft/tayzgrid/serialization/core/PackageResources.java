/*
 * @(#)PackageResources.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * PackageResources class.
 *
 * @version 1.0, September 18, 2008
 */
public class PackageResources {

    /*
     * Package level string constants.
     */
    public static final String Surrogates_HandleOutOfRange = 
            PackageResources.getResourceString("Surrogates_HandleOutOfRange");
    public static final String Surrogates_AlreadyRegistered = 
            PackageResources.getResourceString("Surrogates_AlreadyRegistered");
    public static final String Type_AlreadyRegistered = 
            PackageResources.getResourceString("Type_AlreadyRegistered");
    public static final String Surrogates_NotFound = 
            PackageResources.getResourceString("Surrogates_NotFound");
    
    /** Creates a new instance of PackageResources */
    public PackageResources() {
    }

    public static String getResourceString(String key)
    {
        try
        {
            return ResourceBundle.getBundle("com/nextreme/opennxserialization/NxResources").getString(key);
        }
        catch(MissingResourceException e)
        {
        }
        return "";
    }
}
