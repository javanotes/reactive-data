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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.hazelcast.config.ConfigurationException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapStore;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.MigrationEvent;
import com.hazelcast.core.MigrationListener;
import com.hazelcast.map.listener.MapListener;
import com.reactivetechnologies.platform.datagrid.handlers.MembershipEventObserver;
import com.reactivetechnologies.platform.datagrid.handlers.MessagingChannel;
import com.reactivetechnologies.platform.datagrid.handlers.PartitionMigrationCallback;
import com.reactivetechnologies.platform.utils.ResourceLoaderHelper;

/**
 * Hazelcast instance wrapper. This class would expose interactions with the underlying datagrid.
 * A singleton instance across the VM.
 * @author esutdal
 *
 */
public final class HazelcastClusterServiceBean {
	
	private HazelcastInstanceProxy hzInstance = null;
		
	private static final Logger log = LoggerFactory.getLogger(HazelcastClusterServiceBean.class);
	
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
	 * Sets map store using a spring bean	
	 * @param backingStore
	 */
	public void setMapStoreImplementation(String map, MapStore<Long, ? extends Serializable> backingStore)
	{
	  hzInstance.setMapStoreImplementation(map, backingStore, true);
	  log.info("Set write through backing store for saving ensemble models..");
	}
	/**
	 * Gets the underlying Hazelcast instance. Should be used with caution
	 * @return
	 */
	public final HazelcastInstance getHazelcastInstance()
	{
	  if(!isStarted())
	    throw new IllegalStateException("HazelcastInstance not started!");
	  return hzInstance.getHazelcast();
	}
	public void remove(Object key, String map)
  {
	  hzInstance.remove(key, map);
  }
  
  public <K, V> Map<K, V> getMap(String map)
  {
    return hzInstance.getMap(map);
  }
  public Object put(Object key, Object value, String map) {
    return hzInstance.put(key, value, map);
    
  }
  public void set(Object key, Object value, String map) {
    hzInstance.set(key, value, map);
    
  }
  public Object get(Object key, String map) {
    return hzInstance.get(key, map);
    
  }
  Set<Entry<Object, Object>> getAll(String map)
  {
    return hzInstance.getAll(map);
  }
			
				
	/**
	 * To get notifications for member events
	 * @author esutdal
	 *
	 */
	private static class InstanceListener extends Observable implements MembershipListener{

		@Override
		public void memberRemoved(final MembershipEvent event) {
			
			setChanged();
			notifyObservers(event);
			
			
		}
		
		@Override
		public void memberAdded(final MembershipEvent event) {
		  setChanged();
      notifyObservers(event);
		}

		@Override
		public void memberAttributeChanged(MemberAttributeEvent event) {
		  setChanged();
      notifyObservers(event);
		}
		
	}
		
	private final AtomicBoolean migrationRunning = new AtomicBoolean();	
	/**
	 * Is migration ongoing
	 * @return
	 */
	public boolean isMigrationRunning() {
		return migrationRunning.compareAndSet(true, true);
	}

	private final Map<String, PartitionMigrationCallback<?>> migrCallbacks = new HashMap<>();
	/**
	 * 
	 * @param callback
	 * @throws IllegalAccessException 
	 */
	public void addPartitionMigrationCallback(PartitionMigrationCallback<?> callback) throws IllegalAccessException
	{
	  if (!startedListeners) {
      migrCallbacks.put(callback.keyspace(), callback);
    }
	  else
      throw new IllegalAccessException("PartitionMigrationListener cannot be added after Hazelcast service has been started");
	}
	/**
	 * 
	 * @param keyspace
	 * @param listener
	 * @throws IllegalAccessException 
	 */
	public void addLocalEntryListener(Serializable keyspace, MapListener listener)
  {
	  hzInstance.addLocalEntryListener(keyspace.toString(), listener);
  }
	private final InstanceListener instanceListener = new InstanceListener();
	private void init()
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
          for(Entry<String, PartitionMigrationCallback<?>> e : migrCallbacks.entrySet())
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
        throw new IllegalArgumentException(e);
      }
    }
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
      init();
      startedListeners = true;//silently ignore
    }
	}
	
	/**
	 * No of members at this point
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

  public Object removeNow(Serializable id, String string) {
    return hzInstance.removeNow(id, string);
    
  }

  public void addInstanceListenerObserver(MembershipEventObserver observer) {
    instanceListener.addObserver(observer);      
    
  }
	/**
	 * 
	 * @param channel
	 */
  public <E> void addMessageChannel(MessagingChannel<E> channel)
  {
    hzInstance.addMessageChannelHandler(channel);
  }
  /**
   * 
   * @param message
   * @param topic
   */
  public void publish(Object message, String topic) {
    hzInstance.publish(message, topic);
    
  }
	/**
	 * 
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
   * 
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
  
  public <T> void addToSet(String set, T item)
  {
    hzInstance.addToSet(set, item);
  }
  
  public <T> Iterator<T> getSetIterator(String set) {
    return hzInstance.getSetIterator(set);
  }
  public <T> void persistItem(String map, T ensemble, Long id) {
    hzInstance.getMap(map).put(id, ensemble);
    
  }
  
  /**
   * 
   * @param map
   * @return
   */
  public String getInstanceCachedValue(String map) {
    IMap<String, String> cached = hzInstance.getMap(map);
    return cached.get(hzInstance.getInstanceId());
  }
  /**
   * 
   * @param map
   * @param value
   */
  public void setInstanceCachedValue(String map, String value) {
    IMap<String, String> cached = hzInstance.getMap(map);
    cached.set(hzInstance.getInstanceId(), value);
  }
	
}
