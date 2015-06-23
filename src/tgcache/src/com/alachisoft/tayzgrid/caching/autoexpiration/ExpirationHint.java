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

package com.alachisoft.tayzgrid.caching.autoexpiration;

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.EventArgs;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.ISizable;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class that defines an interface used by the Cache. Different
 * sort of expiration policies must derive from this class including the complex
 * CacheDependency classes in Web.Caching package.
 */
public abstract class ExpirationHint implements java.lang.Comparable, IDisposable, ICompactSerializable, ISizable {

    public static final int EXPIRED = 1;
    public static final int NEEDS_RESYNC = 2;
    public static final int IS_VARIANT = 4;
    public static final int NON_ROUTABLE = 8;
    public static final int DISPOSED = 16;
    private Object _cacheKey;
    private int _bits;
    private IExpirationEventSink _objNotify;
    public ExpirationHintType _hintType = ExpirationHintType.values()[0];

    // _cacheKey + _bits + _hintType
    public final int ExpirationHintSize = 24;
    
    protected ExpirationHint() {
        _hintType = ExpirationHintType.Parent;
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        SetBit(DISPOSED);
        DisposeInternal();
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    protected void DisposeInternal() {
    }

    /**
     * key to compare expiration hints.
     */
    public abstract int getSortKey();

    public ILogger _ncacheLog = null;

    public final ILogger getCacheLog() {
        return _ncacheLog;
    }

    /**
     * Property that returns true when the expiration has taken place, returns
     * false otherwise.
     */
    public final boolean getHasExpired() {
        return IsBitSet(EXPIRED);
    }

    /**
     * virtual method that returns true if user has selected to Re-Sync the
     * object when expired else false.
     */
    public final boolean getNeedsReSync() {
        return IsBitSet(NEEDS_RESYNC);
    }

    /**
     * Return if hint is to be updated on Reset
     */
    public final boolean getIsVariant() {
        return IsBitSet(IS_VARIANT);
    }

    /**
     * Returns true if the hint can be routed to other nodes, otherwise returns
     * false.
     */
    public final boolean getIsRoutable() {
        return !IsBitSet(NON_ROUTABLE);
    }

    /**
     * Returns true if the hint can be routed to other nodes, otherwise returns
     * false.
     */
    public final boolean getIsDisposed() {
        return IsBitSet(DISPOSED);
    }

    public void setCacheKey(Object value) {
        _cacheKey = value;
    }

    public Object getCacheKey() {
        return _cacheKey;
    }
    
    @Override
    public int getSize()
    {
        return ExpirationHintSize;
    }
    
    public int getInMemorySize()
    {
        return ExpirationHintSize;
    }

    /**
     * virtual method that returns true when the expiration has taken place,
     * returns false otherwise.
     */
    public boolean DetermineExpiration(CacheRuntimeContext context) {
        return getHasExpired();
    }

    /**
     * virtual method that returns true when the expiration has taken place,
     * returns false otherwise. Used only for those hints that are validated at
     * the time of Get operation on the cache.
     *
     * @param context
     * @return
     */
    public boolean CheckExpired(CacheRuntimeContext context) {
        return false;
    }

    /**
     * Resets the value of the hint. Called by the Cache manager upon a
     * successful HIT.
     */
    public boolean Reset(CacheRuntimeContext context) throws OperationFailedException {
        _bits &= ~EXPIRED;
        if (_ncacheLog == null) {
            _ncacheLog = context.getCacheLog();
        }
        //return false;
        return true;
    }

    public void ResetVariant(CacheRuntimeContext context) throws OperationFailedException {
        if (this.getIsVariant()) {
            Reset(context);
        }
    }

    protected final void SetExpirationEventSink(IExpirationEventSink objNotify) {
        this._objNotify = objNotify;
    }

    protected final void NotifyExpiration(Object sender, EventArgs e) {
        if (this.SetBit(EXPIRED)) {
            IExpirationEventSink changed1 = this._objNotify;
            if (changed1 != null) {
                changed1.DependentExpired(sender, e);
            }
        }
    }

    /**
     * Returns true if the hint can be routed to other nodes, otherwise returns
     * false.
     */
    public final boolean IsBitSet(int bit) {
        return ((_bits & bit) != 0);
    }

    /**
     * Sets various flags of this expiration hint.
     */
    public boolean SetBit(int bit) {
        while (true) {
            int oldBits = this._bits;
            if ((oldBits & bit) != 0) {
                return false;
            }
            tangible.RefObject<Integer> tempRef__bits = new tangible.RefObject<Integer>(this._bits);

            java.util.concurrent.atomic.AtomicInteger atom = new AtomicInteger(this._bits);

            boolean val = atom.compareAndSet(oldBits, (int) (oldBits | bit));
            this._bits = atom.get();
            return val;
        }
    }

    /**
     * Compares the current instance with another object of the same type.
     *
     * @param obj An object to compare with this instance.
     * @return A 32-bit signed integer that indicates the relative order of the
     * comparands.
     */
    @Override
    public int compareTo(Object obj) {
        if (obj instanceof ExpirationHint) {
            return (new Integer(getSortKey())).compareTo(((ExpirationHint) obj).getSortKey());
        } else {
            return 1; // Consider throwing an exception
        }
    }

    public static ExpirationHint ReadExpHint(CacheObjectInput reader) throws IOException, ClassNotFoundException {
        ExpirationHintType expHint = ExpirationHintType.Parent;
        expHint = ExpirationHintType.forValue(reader.readShort());
        ExpirationHint tmpObj = null;
        switch (expHint) {
            case NULL:
                return null;

            case Parent:
                tmpObj = (ExpirationHint) reader.readObject();
                return (ExpirationHint) tmpObj;

            case FixedExpiration:
                FixedExpiration fe = new FixedExpiration();
                ((ICompactSerializable) fe).deserialize(reader);
                return (ExpirationHint) fe;

            case TTLExpiration:
                TTLExpiration ttle = new TTLExpiration();
                ((ICompactSerializable) ttle).deserialize(reader);
                return (ExpirationHint) ttle;

            case TTLIdleExpiration:
                TTLIdleExpiration ttlie = new TTLIdleExpiration();
                ((ICompactSerializable) ttlie).deserialize(reader);
                return (ExpirationHint) ttlie;

            case FixedIdleExpiration:
                FixedIdleExpiration fie = new FixedIdleExpiration();
                ((ICompactSerializable) fie).deserialize(reader);
                return (ExpirationHint) fie;

            case NodeExpiration:
                NodeExpiration ne = new NodeExpiration();
                ((ICompactSerializable) ne).deserialize(reader);
                return (ExpirationHint) ne;

            case IdleExpiration:
                IdleExpiration ie = new IdleExpiration();
                ((ICompactSerializable) ie).deserialize(reader);
                return (ExpirationHint) ie;

            case AggregateExpirationHint:
                AggregateExpirationHint aeh = new AggregateExpirationHint();
                ((ICompactSerializable) aeh).deserialize(reader);
                return (ExpirationHint) aeh;
           
           

            default:
                break;
        }
        return null;
    }

    public static void WriteExpHint(CacheObjectOutput writer, ExpirationHint expHint) throws IOException {
        if (expHint == null) {
            writer.writeShort((short) ExpirationHintType.NULL.getValue());
            return;
        }

        writer.writeShort((short) expHint._hintType.getValue());
        if (expHint._hintType == ExpirationHintType.ExtensibleDependency) {
            writer.writeObject(expHint);
        } else {
            ((ICompactSerializable) expHint).serialize(writer);
        }

        return;

    }

    /**
     * Override this method for hints that should be reinitialized when they are
     * moved to another partition. e.g SqlYukondependency Hint must be
     * reinitialized after state transfer so that its listeners are created on
     * new subcoordinator.
     *
     * @param context CacheRuntimeContex for required contextual information.
     * @return True if reinitialization was successfull.
     */
    public boolean ReInitializeHint(CacheRuntimeContext context) {
        return false;
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        this._hintType = (ExpirationHintType) reader.readObject();
        this._bits = reader.readInt();
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(this._hintType);
        writer.writeInt(_bits);
    }
}
