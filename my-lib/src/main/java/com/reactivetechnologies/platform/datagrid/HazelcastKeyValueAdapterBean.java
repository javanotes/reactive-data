/* ============================================================================
*
* FILE: HazelcastKeyValueAdaptor.java
*
The MIT License (MIT)

Copyright (c) 2016 Sutanu Dalui

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*
* ============================================================================
*/
package com.reactivetechnologies.platform.datagrid;

import java.io.Serializable;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;

import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.LocalMapEntryPutListener;
import com.reactivetechnologies.platform.datagrid.handlers.MembershipEventObserver;
import com.reactivetechnologies.platform.datagrid.handlers.MigratedEntryProcessor;
/**
 * A {@linkplain KeyValueAdapter} implementation for Hazelcast.
 */
public class HazelcastKeyValueAdapterBean extends AbstractKeyValueAdapter {
  
  private final HazelcastClusterServiceBean hz;
 /**
  * 
  * @param joinImmediate - if start listening immediately
  * @param xmlCfg
  */
  public HazelcastKeyValueAdapterBean(boolean joinImmediate, HazelcastClusterServiceBean hz) {
    this.hz = hz;
    if(joinImmediate)
      acceptJoin();
  }
  
  /**
   * Initialized but not joined
   * @param classpathXmlCfg
   */
  public HazelcastKeyValueAdapterBean(HazelcastClusterServiceBean hz) {
    this(false, hz);
  }
  /**
   * Add a local entry listener on the given map for add/update entry. Local entry listeners
   * can be registered any time.
   * @param <V>
   * @param mapL
   * @param keyspace
   */
  public <V> void addLocalKeyspaceListener(LocalMapEntryPutListener<V> callback)
  {
    hz.addLocalEntryListener(callback.keyspace(), callback);
  }
  /**
   * Add a membership event observer to receive Hazelcast membership event callbacks. Membership observers have to be
   * registered before {@link #acceptJoin()} is invoked.
   * @param observer
   * @throws IllegalAccessException if added after service is already started
   */
  public void addMembershipObserver(MembershipEventObserver observer) throws IllegalAccessException
  {
    if(hz.isStarted())
      throw new IllegalAccessException("MembershipEventObserver cannot be added after Hazelcast service has been started");
    hz.addInstanceListenerObserver(observer);
  }
  /**
   * Add a partition migration listener on the given map. Migration listeners have to be
   * registered before {@link #acceptJoin()} is invoked.
   * @param <V>
   * @param callback
   * @throws IllegalAccessException if added after service is already started
   */
  public <V> void addPartitionMigrationListener(MigratedEntryProcessor<V> callback) throws IllegalAccessException
  {
    if (!hz.isStarted()) {
      hz.addPartitionMigrationCallback(callback);
    }
    else
      throw new IllegalAccessException("PartitionMigrationListener cannot be added after Hazelcast service has been started");
  }
  
  /**
   * Starts Hazelcast service and joins to cluster. Basically this registers 
   * the lifecycle listener and any partition migration listeners.
   */
  public void acceptJoin()
  {
    hz.startInstanceListeners();
  }
  @Override
  public Object put(Serializable id, Object item, Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(id, "Cannot add item with null id.");
    Assert.notNull(keyspace, "Cannot add item for null collection.");
    return hz.put(id, item, keyspace.toString());
  }
  public void set(Serializable id, Object item, Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(id, "Cannot add item with null id.");
    Assert.notNull(keyspace, "Cannot add item for null collection.");
    hz.set(id, item, keyspace.toString());
  }

  @Override
  public boolean contains(Serializable id, Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(id, "Cannot check item with null id.");
    Assert.notNull(keyspace, "Cannot check item for null collection.");
    return hz.getMap(keyspace.toString()).containsKey(id);
  }

  @Override
  public Object get(Serializable id, Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(id, "Cannot get item with null id.");
    Assert.notNull(keyspace, "Cannot get item for null collection.");
    return hz.get(id, keyspace.toString());
  }

  @Override
  public Object delete(Serializable id, Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(id, "Cannot delete item with null id.");
    Assert.notNull(keyspace, "Cannot delete item for null collection.");
    return hz.removeNow(id, keyspace.toString());
  }
  public void deleteAsync(Serializable id, Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(id, "Cannot delete item with null id.");
    Assert.notNull(keyspace, "Cannot delete item for null collection.");
    hz.remove(id, keyspace.toString());
  }

  /**
   * @deprecated Expensive statement.
   */
  @Override
  public Iterable<?> getAllOf(Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(keyspace, "Cannot getAllOf for null collection.");
    return hz.getMap(keyspace.toString()).values();
  }

  /**
   * @deprecated Not implemented
   * @throws UnsupportedOperationException
   */
  @Override
  public CloseableIterator<Entry<Serializable, Object>> entries(
      Serializable keyspace) {
    log.error("UnsupportedOperationException:: Gradle compileJava failing on using CloseableIterator\n"+
        "error: incompatible types: Iterator<Entry<Object,Object>> cannot be converted to Iterator<? extends Entry<Serializable,Object>> "
        + "hz.getMap(keyspace.toString()).entrySet().iterator());");
    throw new UnsupportedOperationException("Not implemented. Reason: Check logs!");
    
    /*if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(keyspace, "Cannot iterate entries for null collection.");
    return new ForwardingCloseableIterator<Entry<Serializable, Object>>((Iterator<? extends Entry<Serializable, Object>>) 
        hz.getMap(keyspace.toString()).entrySet().iterator());*/
  }

  @Override
  public void deleteAllOf(Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(keyspace, "Cannot deleteAllOf for null collection.");
    hz.getMap(keyspace.toString()).clear();
  }

  private static final Logger log = LoggerFactory.getLogger(HazelcastKeyValueAdapterBean.class);
  /**
   * @deprecated Not implemented
   */
  @Override
  public void clear() {
    log.warn("<< HazelcastKeyValueAdapter.clear() UnsupportedOperation IGNORED >>");
  }

  @Override
  public long count(Serializable keyspace) {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    Assert.notNull(keyspace, "Cannot count for null collection.");
    return hz.getMap(keyspace.toString()).size();
  }

  @Override
  public void destroy() throws Exception {
    if(!hz.isStarted())
      throw new IllegalStateException("Hazelcast service not started!");
    hz.stopService();
    
  }

}
