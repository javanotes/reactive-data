/* ============================================================================
*
* FILE: DefaultAsyncResponse.java
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
package com.reactivetechnologies.platform.rest;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

public class DefaultAsyncResponse implements AsyncResponse {

  private final AsyncEventReceiverBean eventResponse;
  private final String id;
  /**
   * Default implementation of {@linkplain AsyncResponse}.
   * @param eventReceiver
   * @param id
   */
  DefaultAsyncResponse(AsyncEventReceiverBean eventReceiver, String id) {
    this.eventResponse = eventReceiver;
    this.id = id;
  }

  private AtomicBoolean resumed = new AtomicBoolean();
  @Override
  public boolean resume(Object response) {
        
    return resumeAtomic(response);
  }

  private boolean resumeAtomic(Object response)
  {
    if(resumed.compareAndSet(false, true))
    {
      eventResponse.dispatchResponse(id, response);
    }
    else
      throw new IllegalStateException("Already resumed once!");
    
    return resumed.get();
  }
  @Override
  public boolean resume(Throwable response) {
    return resumeAtomic(response);
  }

  @Override
  public boolean cancel() {
    return false;
  }

  @Override
  public boolean cancel(int retryAfter) {
    return false;
  }

  @Override
  public boolean cancel(Date retryAfter) {
    return false;
  }

  @Override
  public boolean isSuspended() {
    return !resumed.get();
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return resumed.get();
  }

  @Override
  public boolean setTimeout(long time, TimeUnit unit) {
    ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
    es.schedule(new Runnable() {
      
      @Override
      public void run() {
        if(!isDone())
        {
          //even if there is an exception, it will be silently ignored
          resume(new ServiceUnavailableException("Timed-out"));
        }
        
      }
    }, time, unit);
    es.shutdown();
    return true;
  }

  @Override
  public void setTimeoutHandler(TimeoutHandler handler) {
    throw new UnsupportedOperationException();
    
  }

  @Override
  public Collection<Class<?>> register(Class<?> callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback,
      Class<?>... callbacks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Class<?>> register(Object callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(Object callback,
      Object... callbacks) {
    throw new UnsupportedOperationException();
  }

}
