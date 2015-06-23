
package com.alachisoft.tayzgrid.caching.datasourceproviders.JCache;

import javax.cache.Cache;

/**
 *Temporary implementation. Will be deleted after the actual implementation is complete.
 */



public class Entry<K, V> implements Cache.Entry<K, V> {

  private final K key;
  private final V value;
 // private final V oldValue;

  /**
   * Constructor
   */
  public Entry(K key, V value) {
    this.key = key;
    this.value = value;
    //this.oldValue = null;
  }
    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
//  private final K key;
//  private final V value;
//  private final V oldValue;
//
//  /**
//   * Constructor
//   */
//  public Entry(K key, V value) {
//    this.key = key;
//    this.value = value;
//    this.oldValue = null;
//  }
//
//  /**
//   * Constructor
//   */
//  public Entry(K key, V value, V oldValue) {
//    this.key = key;
//    this.value = value;
//    this.oldValue = oldValue;
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public K getKey() {
//    return key;
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public V getValue() {
//    return value;
//  }
//
//  /**
//   *
//   * @return the old value, if any
//   */
//  public V getOldValue() {
//    return oldValue;
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public <T> T unwrap(Class<T> clazz) {
//    if (clazz != null && clazz.isInstance(this)) {
//      return (T) this;
//    } else {
//      throw new IllegalArgumentException("Class " + clazz + " is unknown to this implementation");
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public boolean equals(Object o) {
//    if (this == o) return true;
//    if (o == null || ((Object) this).getClass() != o.getClass()) return false;
//
//    Entry<?, ?> e2 = (Entry<?, ?>) o;
//
//    return this.getKey().equals(e2.getKey()) &&
//        this.getValue().equals(e2.getValue()) &&
//        (this.oldValue == null && e2.oldValue == null ||
//            this.getOldValue().equals(e2.getOldValue()));
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public int hashCode() {
//    return getKey().hashCode();
//  }
}

