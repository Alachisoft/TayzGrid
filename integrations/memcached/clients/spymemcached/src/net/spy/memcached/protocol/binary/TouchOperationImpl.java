/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2011 Couchbase, Inc.
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.OperationCallback;
import static net.spy.memcached.protocol.binary.OperationImpl.STATUS_OK;

/**
 * Operation to reset a timeout in Membase server.
 */
public class TouchOperationImpl extends SingleKeyOperationImpl {

  static final byte CMD = 0x1c;

  private final int exp;

  protected TouchOperationImpl(String k, int e, OperationCallback cb) {
    super(CMD, generateOpaque(), k, cb);
    exp = e;
  }

  @Override
  public void initialize() {
    prepareBuffer(key, 0, EMPTY_BYTES, exp);
  }

  @Override
  protected void decodePayload(byte[] pl) {
    getCallback().receivedStatus(STATUS_OK);
  }

  @Override
  public String toString() {
    return super.toString() + " Exp: " + exp;
  }
}
