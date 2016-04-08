package com.reactivetechnologies.platform.datagrid.core;
import java.io.IOException;
/* ============================================================================
*
* FILE: HazelcastClusterServiceBean.java
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
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;

import com.hazelcast.config.ConfigurationException;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MapStore;
import com.hazelcast.core.MigrationEvent;
import com.hazelcast.core.MigrationListener;
import com.hazelcast.map.listener.MapListener;
import com.hazelcast.nio.serialization.DataSerializable;
import com.reactivetechnologies.platform.datagrid.HzMapConfig;
import com.reactivetechnologies.platform.datagrid.handlers.AbstractMessageChannel;
import com.reactivetechnologies.platform.datagrid.handlers.LocalMapEntryPutListener;
import com.reactivetechnologies.platform.datagrid.handlers.MembershipEventObserver;
import com.reactivetechnologies.platform.datagrid.handlers.MessageChannel;
import com.reactivetechnologies.platform.datagrid.handlers.MigratedEntryProcessor;
import com.reactivetechnologies.platform.utils.ResourceLoaderHelper;

/**
 * Hazelcast instance wrapper. This class would expose interactions with the underlying datagrid.
 * A singleton instance across the VM.
 * @author esutdal
 *
 */
public final class HazelcastClusterServiceBean {
	
	private HazelcastInstanceProxy hzInstance = null;
			
	final static String REST_CONTEXT_URI = "http://@IP:@PORT/hazelcast/rest";
	private static final Logger log = LoggerFactory.getLogger(HazelcastClusterServiceBean.class);
	/**
	 * Get the Hazelcast REST context URI
	 * @return
	 */
	public String getRestContextUri()
	{
	  InetSocketAddress sockAddr = hzInstance.getLocalMemberAddress();
    return REST_CONTEXT_URI.replaceFirst("@IP", sockAddr.getHostString()).replaceFirst("@PORT", sockAddr.getPort()+"");
	  
	}
	/**
	 * Get the Hazelcast REST context URI for an IMap
	 * @param map
	 * @return
	 */
	public String getRestContextUri(String map)
  {
    return getRestContextUri() + "/maps/"+map;
    
  }
	//we will be needing these for short time tasks.  Since a member addition / removal operation should not occur very frequently
	private final ExecutorService worker = Executors.newCachedThreadPool(new ThreadFactory() {
		private int n = 0;
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "hz.member.thread" + "-" +(n++));
			t.setDaemon(true);
			return t;
		}
	});
	/**
	 * Set map store using a spring bean. This is for programmatic configuration of {@linkplain MapStore}
	 * @param backingStore
	 */
	public void setMapStoreImplementation(String map, MapStore<? extends Serializable, ? extends Serializable> backingStore)
	{
	  hzInstance.setMapStoreImplementation(map, backingStore, true);
	  log.info("Set write through backing store for saving ensemble models..");
	}
	/**
	 * Sets a map configuration programmatically. The provided class must be annotated with {@linkplain HzMapConfig}.
	 * @param annotatedClass
	 */
	public void setMapConfiguration(Class<?> annotatedClass)
	{
	  hzInstance.addMapConfig(annotatedClass);
	}
	/**
	 * Asynchronous removal of a key from IMap.
	 * @param key
	 * @param map
	 */
	public void remove(Object key, String map)
  {
	  hzInstance.remove(key, map);
  }
  /**
   * Gets the Hazelcast IMap instance.
   * @param map
   * @return
   */
  public <K, V> Map<K, V> getMap(String map)
  {
    return hzInstance.getMap(map);
  }
  /**
   * 
   * @param key
   * @param value
   * @param map
   * @return
   */
  public Object put(Object key, Object value, String map) {
    return put(key, value, map, false);
    
  }
  /**
   * Synchronized put operation across cluster.
   * @param key
   * @param value
   * @param map
   * @param synchronize
   * @return
   */
  public Object put(Object key, Object value, String map, boolean synchronize) {
    return synchronize ? hzInstance.synchronizePut(key, value, map) : hzInstance.put(key, value, map);
    
  }
  /**
   * 
   * @param key
   * @param value
   * @param map
   */
  public void set(Object key, Object value, String map) {
    set(key, value, map, false);
    
  }
  /**
   * Synchronized set operation across cluster.
   * @param key
   * @param value
   * @param map
   * @param synchronize
   */
  public void set(Object key, Object value, String map, boolean synchronize) {
    if(synchronize)
      hzInstance.synchronizeSet(key, value, map);
    else
      hzInstance.set(key, value, map);
    
  }
  /**
   * Get the value corresponding to the key from an {@linkplain IMap}.
   * @param key
   * @param map
   * @return
   */
  public Object get(Object key, String map) {
    return hzInstance.get(key, map);
    
  }
  				
	private final AtomicBoolean migrationRunning = new AtomicBoolean();	
	/**
	 * Is migration ongoing
	 * @return
	 */
	public boolean isMigrationRunning() {
		return migrationRunning.compareAndSet(true, true);
	}

	private final Map<String, MigratedEntryProcessor<?>> migrCallbacks = new HashMap<>();
	/**
	 * Register a new partition migration listener. The listener callback will be invoked on each entry being migrated.
	 * @param callback
	 * @throws IllegalAccessException if service is already started
	 */
	public void addPartitionMigrationCallback(MigratedEntryProcessor<?> callback) throws IllegalAccessException
	{
	  if (!startedListeners) {
      migrCallbacks.put(callback.keyspace(), callback);
    }
	  else
      throw new IllegalAccessException("PartitionMigrationListener cannot be added after Hazelcast service has been started");
	}
	/**
	 * Sequence number generator for a given key.
	 * @param context
	 * @return
	 */
	public Long getNextLong(String context)
	{
	  return hzInstance.getNextLong(context);
	}
	/**
	 * Register a local add/update entry listener on a given {@linkplain IMap} by name. Only a single listener for a given {@linkplain MapListener} instance would be 
	 * registered. So subsequent invocation with the same instance would first remove any existing registration for that instance.
	 * @param keyspace map name
	 * @param listener callback 
	 * @throws IllegalAccessException 
	 */
	public void addLocalEntryListener(Serializable keyspace, MapListener listener)
  {
	  hzInstance.addLocalEntryListener(keyspace.toString(), listener);
  }
	/**
	 * Register a local add/update entry listener on a given {@linkplain IMap} by name. Only a single listener for a given {@linkplain MapListener} instance would be 
   * registered. So subsequent invocation with the same instance would first remove any existing registration for that instance.
	 * @param addUpdateListener listener with map name
	 */
	public <V> void addLocalEntryListener(LocalMapEntryPutListener<V> addUpdateListener)
  {
	  addLocalEntryListener(addUpdateListener.keyspace(), addUpdateListener);
  }
	private final InstanceListener instanceListener = new InstanceListener();
	/**
	 * Register lifecycle listeners.
	 */
	private void registerListeners()
	{
	  hzInstance.init(instanceListener);
	  
    hzInstance.addMigrationListener(new MigrationListener() {
      
      @Override
      public void migrationStarted(MigrationEvent migrationevent) {
        migrationRunning.getAndSet(true);
      }
      
      @Override
      public void migrationFailed(MigrationEvent migrationevent) {
        migrationRunning.getAndSet(false);
        synchronized (migrationRunning) {
          migrationRunning.notifyAll();
        }
      }
      
      @Override
      public void migrationCompleted(MigrationEvent migrationevent) 
      {
        migrationRunning.getAndSet(false);
        synchronized (migrationRunning) {
          migrationRunning.notifyAll();
        }
        if(migrationevent.getNewOwner().localMember())
        {
          log.debug(">>>>>>>>>>>>>>>>> Migration detected of partition ..."+migrationevent.getPartitionId());
          for(Entry<String, MigratedEntryProcessor<?>> e : migrCallbacks.entrySet())
          {
            IMap<Serializable, Object> map = hzInstance.getMap(e.getKey());
            Set<Serializable> keys = new HashSet<>();
            for(Serializable key : map.localKeySet())
            {
              if(hzInstance.getPartitionIDForKey(key) == migrationevent.getPartitionId())
              {
                keys.add(key);
              }
            }
            if(!keys.isEmpty())
            {
              map.executeOnKeys(keys, e.getValue());
            }
          }
          
        }
        
      }
    });
	}
	/**
	 * Set a Hazelcast configuration property.
	 * @param prop
	 * @param val
	 */
	public void setProperty(String prop, String val)
	{
	  hzInstance.setProperty(prop, val);
	}
	/**
	 * Public constructor
	 * @param props
	 * @throws ConfigurationException
	 */
	HazelcastClusterServiceBean(String cfgXml, String entityScanPath) {
		if(hzInstance == null)
 {
      try {
        hzInstance = StringUtils.hasText(cfgXml) ? new HazelcastInstanceProxy(
            ResourceLoaderHelper.loadFromFileOrClassPath(cfgXml),
            entityScanPath) : new HazelcastInstanceProxy(entityScanPath);
      } catch (IOException e) {
        throw new BeanCreationException("Unable to start Hazelcast", e);
      }
    }
	}
	/**
	 * Try and join to default cluster.
	 * @param instanceId
	 */
	public void join(String instanceId)
	{
	  hzInstance.requestJoin(instanceId);
	}
	/**
	 * Try and join join cluster with given name
	 * @param instanceId
	 * @param group
	 */
	public void join(String instanceId, String group)
  {
    hzInstance.requestJoin(instanceId, group);
  }
	private volatile boolean startedListeners;
	/**
	 * 
	 * @return
	 */
	public boolean isStarted() {
    return startedListeners;
  }

  /**
	 * Start lifecycle and partition listeners
	 */
	public void startInstanceListeners()
	{
	  if (!startedListeners) {
      registerListeners();
      startedListeners = true;//silently ignore
    }
	  else
	    log.warn("[startInstanceListeners] invoked more than once. Ignored silently.");
	}
	
	/**
	 * No of members in the cluster at this point.
	 * @return
	 */
	public int size()
	{
		return hzInstance.noOfMembers();
	}
	
	/**
	 * 
	 * @param retry
	 */
	@PreDestroy
	public void stopService() {
		if(hzInstance != null)
		{
			hzInstance.stop();
		}
		worker.shutdown();
		try {
			worker.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			
		}
				
	}
	/**
	 * Synchronous removal of a key from IMap.
	 * @param key
	 * @param map
	 * @return
	 */
  public Object removeNow(Serializable key, String map) {
    return hzInstance.removeNow(key, map);
    
  }
  /**
   * Get a distributed cluster wide lock.
   * @param name
   * @return
   */
  public Lock getClusterLock(String name)
  {
    return hzInstance.getLock(name);
  }
  /**
   * Register a group membership event callback.
   * @param observer
   */
  public void addInstanceListenerObserver(MembershipEventObserver observer) {
    instanceListener.addObserver(observer);      
    
  }
	/**
	 * Adds a message channel with no message ordering.
	 * @param channel
	 * @return regID
	 */
  public <E> String addMessageChannel(MessageChannel<E> channel)
  {
    return addMessageChannel(channel, false);
  }
  /**
   * Adds a message channel.
   * @param channel
   * @param orderingEnabled whether to order messages
   * @return regID
   */
  public <E> String addMessageChannel(MessageChannel<E> channel, boolean orderingEnabled)
  {
    return hzInstance.addMessageChannelHandler(channel, orderingEnabled);
  }
  /**
   * Removes the channel topic listener.
   * @param channel
   */
  public <E> void removeMessageChannel(AbstractMessageChannel<E> channel)
  {
    removeMessageChannel(channel.topic(), channel.getRegistrationId());
  }
  /**
   * Removes a topic listener.
   * @param topic
   * @param regID
   */
  public <E> void removeMessageChannel(String topic, String regID)
  {
    hzInstance.removeTopicListener(topic, regID);
  }
  /**
   * Publish a message to a {@linkplain ITopic}
   * @param message
   * @param topic
   */
  public void publish(Object message, String topic) {
    hzInstance.publish(message, topic);
    
  }
	/**
	 * Acquire a cluster wide lock.
	 * @param unit
	 * @param time
	 * @return
	 * @throws InterruptedException
	 */
  public boolean acquireLock(TimeUnit unit, long time) throws InterruptedException
  {
    return hzInstance.getClusterSyncLock().tryLock(time, unit);
  }
  /**
   * Release a cluster wide lock.
   * @param forced
   */
  public void releaseLock(boolean forced)
  {
    if (forced) {
      hzInstance.getClusterSyncLock().forceUnlock();
    }
    else
      hzInstance.getClusterSyncLock().unlock();
  }
  /**
   * Add to a {@linkplain ISet}.
   * @param set
   * @param item
   */
  public <T> void addToSet(String set, T item)
  {
    hzInstance.addToSet(set, item);
  }
  /**
   * 
   * @param set
   * @return
   */
  public <T> Iterator<T> getSetIterator(String set) {
    return hzInstance.getSetIterator(set);
  }
  /**
   * Persist a unique item to a backing store. This method checks for duplicate key, and would return false in that case.
   * @param map
   * @param ensemble
   * @param id
   * @return
   */
  public <T> boolean persistItem(String map, T ensemble, Serializable id) {
    IMap<Serializable, T> imap = hzInstance.getMap(map);
    if(imap.containsKey(id))
      return false;
    else
      imap.put(id, ensemble);
    return true;
  }
  /**
   * 
   * @return
   */
  private String instanceMapName()
  {
    return hzInstance.getInstanceId().toUpperCase() + "MAP";
  }
  /**
   * 
   * @param map
   * @return
   */
  public DataSerializable getInstanceCachedValue(String map) {
    IMap<String, DataSerializable> cached = hzInstance.getMap(map);
    return cached.get(hzInstance.getInstanceId());
  }
  /**
   * Gets a value from a Map, which is specific to this instance
   * @param key
   * @return
   */
  public <K, V> V getInstanceValue(K key) {
    IMap<K, V> cached = hzInstance.getMap(instanceMapName());
    return cached.get(key);
  }
  /**
   * Gets a copy of the entries for this instance Map. NOTE: Do not let this Map
   * grow arbitrarily. This a sort of static cache with no eviction.
   * @return
   */
  public <K, V> Set<Entry<K, V>> instanceEntrySet() {
    IMap<K, V> cached = hzInstance.getMap(instanceMapName());
    Set<Entry<K, V>> entrySet = cached.entrySet();
    return entrySet;
  }
  /**
   * Clears this instance Map.
   */
  public <K, V> void clearInstanceMap() {
    IMap<K, V> cached = hzInstance.getMap(instanceMapName());
    cached.clear();
  }
  
  /**
   * Puts a value to a Map, which is specific to this instance
   * @param key
   * @param value
   */
  public <K, V> void setInstanceValue(K key, V value) {
    IMap<K, V> cached = hzInstance.getMap(instanceMapName());
    cached.set(key, value);
  }
  /**
   * Puts a cached value to a Map, which is specific to this instance
   * @param map
   * @param value
   */
  public void setInstanceCachedValue(String map, DataSerializable value) {
    IMap<String, DataSerializable> cached = hzInstance.getMap(map);
    cached.set(hzInstance.getInstanceId(), value);
  }
  /**
   * 
   * @param key
   * @return
   */
  public Long getAndIncrementLong(String key) {
    return hzInstance.getAndIncrementLong(key);
  }
  /**
   * 
   * @param key
   * @return
   */
  public Long getCurrentLong(String key) {
    return hzInstance.getLong(key);
  }
  /**
   * Checks if an {@linkplain IMap} contains the given key.
   * @param id
   * @param imap
   * @return
   */
  public boolean contains(Serializable id, String imap) {
    return hzInstance.getMap(imap).containsKey(id);
    
  }
  
}
