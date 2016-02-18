/* ============================================================================
*
* FILE: AbstractOutboundChannel.java
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
package com.reactivetechnologies.platform.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
/**
 * A composite of a collection of outbound interceptors, invoked serially by default and with no 
 * ordering specified. To change the default behavior, {@link #setStrategy(FeederStrategy)} as needed
 */
@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractOutboundChannel implements OutboundInterceptor<Serializable> {

  private static final Logger log = LoggerFactory.getLogger(AbstractOutboundChannel.class);
  
  protected List<OutboundInterceptor<Serializable>> feeders = new ArrayList<>();
  protected FeederStrategy strategy = FeederStrategy.SEQUENTIAL_RANDOM;
  public FeederStrategy getStrategy() {
    return strategy;
  }
  public void setStrategy(FeederStrategy strategy) {
    this.strategy = strategy;
  }

  /**
   * Add a new outbound path to this channel
   * @param out
   */
  public void addFeeder(OutboundInterceptor<Serializable> out)
  {
    feeders.add(out);
  }
  protected ExecutorService threads;
  private boolean parallel;
  
  public AbstractOutboundChannel() {
    
  }
  @PreDestroy
  protected void destroy()
  {
    if(parallel)
    {
      threads.shutdown();
      try {
        boolean b = threads.awaitTermination(strategy.getTimeoutSecs(), TimeUnit.SECONDS);
        if(!b)
        {
          log.warn("Channel workers did not terminate cleanly!");
        }
      } catch (InterruptedException e) {
        
      }
    }
    log.info("-- Outbound channel destroyed --");
  }
  @PostConstruct
  protected void init()
  {
    switch (strategy) 
    {
    case PARALLEL_ORDERED:
      if(strategy.getComparator() != null)
      {
        Collections.sort(feeders, strategy.getComparator());
      }
      threads = Executors.newFixedThreadPool(strategy.getNoOfThreads(), new ThreadFactory() {
        int n = 0;
        @Override
        public Thread newThread(Runnable arg0) {
          Thread t = new Thread(arg0, "OutboundChannel-Worker-"+(n++));
          return t;
        }
      });
      parallel = true;
      break;
    case PARALLEL_RANDOM:
      //use disruptor??
      threads = Executors.newFixedThreadPool(strategy.getNoOfThreads(), new ThreadFactory() {
        int n = 0;
        @Override
        public Thread newThread(Runnable arg0) {
          Thread t = new Thread(arg0, "OutboundChannel-Worker-"+(n++));
          return t;
        }
      });
      parallel = true;
      break;
    case SEQUENTIAL_ORDERED:
      if(strategy.getComparator() != null)
      {
        Collections.sort(feeders, strategy.getComparator());
      }
      break;
    case SEQUENTIAL_RANDOM:
      break;
    default:
      break;
    
    }
    
    log.info("-- New Outbound channel created ["+name()+"]");
    StringBuilder s = new StringBuilder(" { ");
    for(OutboundInterceptor<Serializable> out : feeders)
    {
      s.append("\n\t").append(out.name()).append(" - IN:").append(out.type());
    }
    s.append("\n}");
    log.info("["+name()+"] Ready to outflow. No. of feeders "+feeders.size()+s);
  }
  /**
   * When a feeding execution throws an exception
   * @param item
   * @param feeder
   * @param exception
   */
  public abstract void onFeedException(Serializable item, OutboundInterceptor<Serializable> feeder, Throwable exception);
  /**
   * Only for parallel strategies. If async execution times out
   * @param item
   * @param feeder
   */
  public abstract void onFeedTimeout(Serializable item, OutboundInterceptor<Serializable> feeder);
  
  @Override
  public void feed(final Serializable item) {
    if(parallel)
    {
      Map<OutboundInterceptor<Serializable>, Future<?>> futures = new LinkedHashMap<>();
      for(final OutboundInterceptor<Serializable> feeder : feeders)
      {
        Future<?> f = threads.submit(new Callable<Void>() {
          
          @Override
          public Void call() throws Exception {
            feeder.feed(item);
            return null;
          }
        });
        futures.put(feeder, f);
      }
      for(Entry<OutboundInterceptor<Serializable>, Future<?>> f : futures.entrySet())
      {
        try {
          f.getValue().get(strategy.getTimeoutSecs(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          onFeedException(item, f.getKey(), e.getCause());
        } catch (TimeoutException e) {
          onFeedTimeout(item, f.getKey());
        }
      }
    }
    else
    {
      for(final OutboundInterceptor<Serializable> feeder : feeders)
      {
        try 
        {
          feeder.feed(item);
        } catch (Exception e) {
          onFeedException(item, feeder, e);
        }
      }
    }
    
  }
  

}
