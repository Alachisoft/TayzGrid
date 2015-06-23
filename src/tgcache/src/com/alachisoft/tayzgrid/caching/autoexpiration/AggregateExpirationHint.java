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
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.EventArgs;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.lang.Iterable;
import java.util.Iterator;

/**
 * Combines multiple expiration hints and provides a single hint.
 *
 *
 */
public class AggregateExpirationHint extends ExpirationHint implements IExpirationEventSink, ICompactSerializable,java.io.Serializable, Iterable
{

    /**
     * expiration hints
     */
    private java.util.ArrayList _hints = new java.util.ArrayList();

    /**
     * Constructor.
     */
    public AggregateExpirationHint()
    {
        super._hintType = ExpirationHintType.AggregateExpirationHint;
    }

    /**
     * Constructor.
     *
     * @param first expiration hints
     */
    public AggregateExpirationHint(ExpirationHint... hints)
    {
        super._hintType = ExpirationHintType.AggregateExpirationHint;
        Initialize(hints);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    @Override
    protected void DisposeInternal()
    {
        for (int i = 0; i < _hints.size(); i++)
        {
                ((IDisposable) _hints.get(i)).dispose();
        }
    }

    /**
     * expiration hints
     */
    public final ExpirationHint[] getHints()
    {
        return (ExpirationHint[]) _hints.toArray(new ExpirationHint[0]);
    }

    /**
     * Return the enumerator on internal hint collection
     *
     * @return
     */
    public final java.util.Iterator iterator()
    {
        return _hints.iterator();
    }

    /**
     * Set the cache key
     */
    @Override
    public void setCacheKey(Object value)
    {
        for (int i = 0; i < _hints.size(); i++)
        {
            ((ExpirationHint) _hints.get(i)).setCacheKey(value);
        }
    }

    @Override
    public boolean SetBit(int bit)
    {
        boolean result = false;

        //needs_resync is set for every individual hint.
        //this is because we treat the resync for db dependency differently.
        if (bit == NEEDS_RESYNC)
        {
            for (int i = 0; i < _hints.size(); i++)
            {
                result = ((ExpirationHint) _hints.get(i)).SetBit(bit);
                if (!result)
                {
                    return result;
                }
            }
        }

        return super.SetBit(bit);
    }

    /**
     * Add an expiration hint to the hints
     *
     * @param eh
     */
    public final void Add(ExpirationHint eh)
    {
        synchronized (this)
        {
            if (!eh.getIsRoutable())
            {
                this.SetBit(NON_ROUTABLE);
            }
            if (eh.getIsVariant())
            {
                this.SetBit(IS_VARIANT);
            }
            eh.SetExpirationEventSink(this);

            AggregateExpirationHint aggregate = (AggregateExpirationHint) ((eh instanceof AggregateExpirationHint) ? eh : null);
            if (aggregate != null)
            {
                for (Iterator it = aggregate._hints.iterator(); it.hasNext();)
                {
                    ExpirationHint expirationHint = (ExpirationHint) it.next();
                    _hints.add(expirationHint);
                }
            }
            else
            {
                _hints.add(eh);
            }

            boolean isFixed = false;
            for (int i = _hints.size() - 1; i >= 0; i--)
            {
                if (isFixed && _hints.get(i) instanceof FixedExpiration)
                {
                    _hints.remove(i);
                    break;
                }
                if (!isFixed && _hints.get(i) instanceof FixedExpiration)
                {
                    isFixed = true;
                }
            }

        }
    }

    /**
     * key to compare expiration hints.
     */
    @Override
    public int getSortKey()
    {
        ExpirationHint minHint = (ExpirationHint) _hints.get(0);
        for (int i = 0; i < _hints.size(); i++)
        {
            if (((java.lang.Comparable) _hints.get(i)).compareTo(minHint) < 0)
            {
                minHint = (ExpirationHint) _hints.get(i);
            }
        }
        return minHint.getSortKey();
    }

    /**
     * Initializes the aggregate hint with multiple dependent hints.
     *
     * @param first expiration hints
     */
    protected final void Initialize(ExpirationHint[] hints)
    {
        if (hints == null)
        {
            throw new IllegalArgumentException("hints");
        }
        for (int i = 0; i < hints.length; i++)
        {
            _hints.add(hints[i]);
        }

        for (int i = 0; i < _hints.size(); i++)
        {
            if (!((ExpirationHint) _hints.get(i)).getIsRoutable())
            {
                this.SetBit(NON_ROUTABLE);
            }
            if (((ExpirationHint) _hints.get(i)).getIsVariant())
            {
                this.SetBit(IS_VARIANT);
            }
            ((ExpirationHint) _hints.get(i)).SetExpirationEventSink(this);
        }
    }

    /**
     * Determines if any of the aggregated expiration hints has expired.
     *
     * @return true if expired
     */
    @Override
    public boolean DetermineExpiration(CacheRuntimeContext context)
    {
        if (getHasExpired())
        {
            return true;
        }

        for (int i = 0; i < _hints.size(); i++)
        {
            if (((ExpirationHint) _hints.get(i)).DetermineExpiration(context))
            {
                this.NotifyExpiration(_hints.get(i), null);
                break;
            }
        }
        return getHasExpired();
    }

    /**
     * Determines if any of the aggregated expiration hints has expired.
     *
     * @return true if expired
     */
    @Override
    public boolean CheckExpired(CacheRuntimeContext context)
    {
        if (getHasExpired())
        {
            return true;
        }

        for (int i = 0; i < _hints.size(); i++)
        {
            if (((ExpirationHint) _hints.get(i)).CheckExpired(context))
            {
                this.NotifyExpiration(_hints.get(i), null);
                break;
            }
        }
        return getHasExpired();
    }

    /**
     * Resets both the contained ExpirationHints.
     */
    @Override
    public boolean Reset(CacheRuntimeContext context) throws OperationFailedException
    {
        boolean flag = super.Reset(context);
        for (int i = 0; i < _hints.size(); i++)
        {
            if (((ExpirationHint) _hints.get(i)).Reset(context))
            {
                flag = true;
            }
        }
        return flag;
    }

    /**
     * Resets only the variant ExpirationHints.
     */
    @Override
    public void ResetVariant(CacheRuntimeContext context) throws OperationFailedException
    {
        for (int i = 0; i < _hints.size(); i++)
        {
            ExpirationHint hint = (ExpirationHint) _hints.get(i);
            if (hint.getIsVariant())
            {
                hint.Reset(context);
            }
        }
    }

    @Override
    public String toString()
    {
        String toString = (!(_hints.get(0) instanceof IdleExpiration || _hints.get(0) instanceof FixedExpiration)) ? toString = "INNER\r\n" : "";
       
        return toString;
    }

    @Override
    public void DependentExpired(Object sender, EventArgs e)
    {
        this.NotifyExpiration(sender, e);
    }

    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        super.deserialize(reader);

        int length = reader.readInt();

        if (_hints == null)
        {
            _hints = new java.util.ArrayList(length);
        }

        for (int i = 0; i < length; i++)
        {
            _hints.add(i, (ExpirationHint) reader.readObject());
        }
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        super.serialize(writer);
        writer.writeInt(_hints.size());
        for (int i = 0; i < _hints.size(); i++)
        {
            writer.writeObject(_hints.get(i));
        }
    }

    public final AggregateExpirationHint GetRoutableClone(Address sourceNode)
    {
        if (_hints == null || _hints.isEmpty())
        {
            return null;
        }

        AggregateExpirationHint hint = new AggregateExpirationHint();

        NodeExpiration ne = null;
        for (int i = 0; i < _hints.size(); i++)
        {
            ExpirationHint eh = (ExpirationHint) _hints.get(i);
            if (!eh.getIsRoutable() && ne == null)
            {
                ne = new NodeExpiration(sourceNode);
                hint.Add(ne);
            }
            else
            {
                hint.Add(eh);
            }
        }
        return hint;
    }
    
}
