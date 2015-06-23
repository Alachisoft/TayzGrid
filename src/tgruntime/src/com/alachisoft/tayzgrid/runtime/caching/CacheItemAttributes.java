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

package com.alachisoft.tayzgrid.runtime.caching;

import java.util.Date;

/**
 * Set Cache Item Attributes to be update in cache
 */
public class CacheItemAttributes
{

    

    private Date _absoluteExpiration = null;

    /**
     * Gets Absolute Expiration for the object
     * @return Absolute Expiration
     */
    public Date getAbsoluteExpiration()
    {
        return _absoluteExpiration;
    }



    /**
     * Sets the Absolute expiration for the object
     * @param _absoluteExpiration Absolute Expiration
     */
    public void setAbsoluteExpiration(Date _absoluteExpiration)
    {
        this._absoluteExpiration = _absoluteExpiration;
    }





}
