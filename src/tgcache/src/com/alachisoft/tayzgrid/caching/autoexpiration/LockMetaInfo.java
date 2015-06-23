/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alachisoft.tayzgrid.caching.autoexpiration;

import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.common.locking.LockManager;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author 
 */
public class LockMetaInfo implements ICompactSerializable{

    
    private Object _lockId;
    private Date _lockDate;
    private TimeSpan _lockAge;
    private LockAccessType _accessType;
    private LockExpiration _lockExpiration;
    private com.alachisoft.tayzgrid.common.locking.LockManager _lockManager;
 

    /**
     * @return the _lockId
     */
    public Object getLockId() {
        return _lockId;
    }

    /**
     * @param _lockId the _lockId to set
     */
    public void setLockId(Object _lockId) {
        this._lockId = _lockId;
    }

    /**
     * @return the _lockDate
     */
    public Date getLockDate() {
        return _lockDate;
    }

    /**
     * @param _lockDate the _lockDate to set
     */
    public void setLockDate(Date _lockDate) {
        this._lockDate = _lockDate;
    }

    /**
     * @return the _lockAge
     */
    public TimeSpan getLockAge() {
        return _lockAge;
    }

    /**
     * @param _lockAge the _lockAge to set
     */
    public void setLockAge(TimeSpan _lockAge) {
        this._lockAge = _lockAge;
    }

    /**
     * @return the _accessType
     */
    public LockAccessType getAccessType() {
        return _accessType;
    }

    /**
     * @param _accessType the _accessType to set
     */
    public void setAccessType(LockAccessType _accessType) {
        this._accessType = _accessType;
    }

    /**
     * @return the _lockExpiration
     */
    public LockExpiration getLockExpiration() {
        return _lockExpiration;
    }

    /**
     * @param _lockExpiration the _lockExpiration to set
     */
    public void setLockExpiration(LockExpiration _lockExpiration) {
        this._lockExpiration = _lockExpiration;
    }

    /**
     * @return the _lockManager
     */
    public com.alachisoft.tayzgrid.common.locking.LockManager getLockManager() {
        return _lockManager;
    }

    /**
     * @param _lockManager the _lockManager to set
     */
    public void setLockManager(com.alachisoft.tayzgrid.common.locking.LockManager _lockManager) {
        this._lockManager = _lockManager;
    }  
    
    
    @Override
    public void serialize(CacheObjectOutput out) throws IOException {
       out.writeObject(_lockId);
       out.writeObject(_lockDate);
       out.writeObject(_lockExpiration);
       out.writeObject(_lockManager);
       
    }

    @Override
    public void deserialize(CacheObjectInput in) throws IOException, ClassNotFoundException {
      _lockId = in.readObject();
      _lockDate = (Date) in.readObject();
      _lockExpiration = (LockExpiration) in.readObject();
      _lockManager = (LockManager) in.readObject();
      
    }
}
