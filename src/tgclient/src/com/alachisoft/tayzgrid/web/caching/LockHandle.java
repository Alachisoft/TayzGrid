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

import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import java.util.Date;

/**
 * An instance of this class is used to lock and unlock the cache items in pessimistic concurrency model.
 */
public final class LockHandle
{

    private String _lockId;
    private Date _lockDate = new Date(0);

    public LockHandle()
    {
        try
        {
            _lockDate = new NCDateTime(1970, 1, 1, 0, 0, 0, 0).getDate();
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Create a new LockHandle
     *
     * @param lockId Lock id
     * @param lockDate Lock date
     */
    public LockHandle(String lockId, Date lockDate)
    {
        this._lockId = lockId;
        this._lockDate = lockDate;
    }

    /**
     * Get lock id
     *
     * @return Lock id
     */
    public String getLockId()
    {
        return this._lockId;
    }

    /**
     * Set lock id
     *
     * @param lockId New lock id
     */
    public void setLockId(String lockId)
    {
       
        this._lockId = lockId;
    }

    /**
     * Get lock date
     *
     * @return Lock date
     */
    public Date getLockDate()
    {
        return this._lockDate;
    }

    /**
     * Set lock date
     *
     * @param lockDate New lock date
     */
    public void setLockDate(Date lockDate)
    {
        this._lockDate = lockDate;
    }
}
