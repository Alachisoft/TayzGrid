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

package net.spy.memcached.auth;

import java.util.HashMap;
import java.util.Map;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.compat.SpyObject;

/**
 * This will ensure no more than one AuthThread will exist for a given
 * MemcachedNode.
 */
public class AuthThreadMonitor extends SpyObject {

  private Map<Object, AuthThread> nodeMap;

  public AuthThreadMonitor() {
    nodeMap = new HashMap<Object, AuthThread>();
  }

  /**
   * Authenticate a new connection. This is typically used by a MemcachedNode in
   * order to authenticate a connection right after it has been established.
   *
   * If an old, but not yet completed authentication exists this will stop it in
   * order to create a new authentication attempt.
   *
   * @param conn
   * @param opFact
   * @param authDescriptor
   * @param node
   */
  public synchronized void authConnection(MemcachedConnection conn,
      OperationFactory opFact, AuthDescriptor authDescriptor,
      MemcachedNode node) {
    interruptOldAuth(node);
    AuthThread newSASLAuthenticator =
        new AuthThread(conn, opFact, authDescriptor, node);
    nodeMap.put(node, newSASLAuthenticator);
  }

  private void interruptOldAuth(MemcachedNode nodeToStop) {
    AuthThread toStop = nodeMap.get(nodeToStop);
    if (toStop != null) {
      if (toStop.isAlive()) {
        getLogger().warn(
            "Incomplete authentication interrupted for node " + nodeToStop);
        toStop.interrupt();
      }
      nodeMap.remove(nodeToStop);
    }
  }
}
