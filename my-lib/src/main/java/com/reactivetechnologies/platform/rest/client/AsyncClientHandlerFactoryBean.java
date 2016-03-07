/* ============================================================================
*
* FILE: AsyncClientHandlerFactoryBean.java
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
package com.reactivetechnologies.platform.rest.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.FactoryBean;
/**
 * Factory for {@linkplain AsyncClientHandler}. 
 */
public class AsyncClientHandlerFactoryBean implements FactoryBean<AsyncClientHandler>{
 
  private ExecutorService threads;
  
  public AsyncClientHandlerFactoryBean() {
    
  }
  @PostConstruct
  private void init()
  {
    threads = Executors.newCachedThreadPool(new ThreadFactory() {
      private int n=0;
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "REST-AsyncClientHandler-"+(n++));
        t.setDaemon(true);
        return t;
      }
    });
  }
  @PreDestroy
  private void destroy()
  {
    threads.shutdown();
  }
  /**
   * 
   * @return
   */
  public AsyncClientHandler newAsyncClientHandler()
  {
    AsyncClientHandler handler = new AsyncClientHandler();
    threads.submit(handler);
    return handler;
  }
  @Override
  public AsyncClientHandler getObject() throws Exception {
    return newAsyncClientHandler();
  }
  @Override
  public Class<?> getObjectType() {
    return AsyncClientHandler.class;
  }
  @Override
  public boolean isSingleton() {
    return false;
  }
}
