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
package com.reactivetechnologies.platform.rest.netty;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

public class DefaultAsyncResponse implements AsyncResponse {

  public DefaultAsyncResponse() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public boolean resume(Object response) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean resume(Throwable response) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean cancel() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean cancel(int retryAfter) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean cancel(Date retryAfter) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isSuspended() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isCancelled() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isDone() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean setTimeout(long time, TimeUnit unit) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setTimeoutHandler(TimeoutHandler handler) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Collection<Class<?>> register(Class<?> callback) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback,
      Class<?>... callbacks) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<Class<?>> register(Object callback) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(Object callback,
      Object... callbacks) {
    // TODO Auto-generated method stub
    return null;
  }

}
