/* ============================================================================
*
* FILE: AsyncEventReceiver.java
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.EntryEvent;
import com.reactivetechnologies.platform.OperationsException;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.LocalPutMapEntryCallback;
import com.reactivetechnologies.platform.message.Event;
import com.reactivetechnologies.platform.utils.GsonWrapper;

public class AsyncEventReceiver implements LocalPutMapEntryCallback<Event<SerializableHttpRequest>> {

  private static final Logger log = LoggerFactory.getLogger(AsyncEventReceiver.class);
  public AsyncEventReceiver() {
    
  }
  /**
   * Generate the async request ID
   * @param event
   * @return
   */
  static String makeAsyncRequestKey(Event<?> event)
  {
    return event.hashBytes()+"-"+event.getCorrelationId();
  }
  @Override
  public void entryAdded(EntryEvent<Serializable, Event<SerializableHttpRequest>> event) {
    doHandle(event.getValue().getPayload(), makeAsyncRequestKey(event.getValue()));
    
  }
  
  final List<MethodInvocationHandler> getHandlers = new ArrayList<>();
  final List<MethodInvocationHandler> postHandlers = new ArrayList<>();
  final List<MethodInvocationHandler> deleteHandlers = new ArrayList<>();
  
  /**
   * 
   * @param request
   * @param mapId
   */
  private void doHandle(SerializableHttpRequest request, String mapId)
  {
    try 
    {
      processRequest(request, mapId);
    } catch (IOException e) {
      log.error("Unable to set response ", e);
      putResponse(mapId, "INTERNAL_SERVER_ERROR");
    } catch (ReflectiveOperationException e) {
      log.error("Exception while invoking service method", e);
      putResponse(mapId, "INTERNAL_SERVER_ERROR");
    }
  }
  
  @Autowired
  private GsonWrapper gson;
  @Autowired
  private HazelcastClusterServiceBean hzService;
  
  private void processRequest(SerializableHttpRequest request, String id) throws IOException, ReflectiveOperationException
  {
    Object returned = invokeServiceMethod(request);
    String jsonResponse = gson.get().toJson(returned);
    log.debug("Returning response JSON: "+jsonResponse);
    try 
    {
      putResponse(id, jsonResponse);
    } catch (IllegalArgumentException e) {
      throw new IOException(e);
    }
  }
  
  private void putResponse(String id, String jsonResponse)
  {
    try 
    {
      hzService.put(id, jsonResponse, WebbitRestServerBean.ASYNC_REST_EVENT_RESPONSE_MAP);
      log.info("Response committed..");
    } catch (Exception e) {
      throw new IllegalArgumentException(id+"", e);
    }
  }
  
  private static Object invokeIfMatch(MethodInvocationHandler handler, SerializableHttpRequest request) throws ReflectiveOperationException, OperationsException
  {
    if(handler.getMethod().getUri().matchesTemplate(request.getRequestUri()))
    {
      List<Object> args = new ArrayList<>();
      args.add(new DefaultAsyncResponse());
      args.addAll(request.getArgs());
      return handler.invokeMethod(args);
    }
    throw new OperationsException();
  }
  
  private Object invokeServiceMethod(SerializableHttpRequest request) throws ReflectiveOperationException
  {
    
    switch(request.getRequestMethod())
    {
    case "GET":
      for(MethodInvocationHandler handler : getHandlers)
      {
        try {
          return invokeIfMatch(handler, request);
        } catch (OperationsException e) {
          continue;
        }
      }
      break;
    case "POST":
      for(MethodInvocationHandler handler : postHandlers)
      {
        try {
          return invokeIfMatch(handler, request);
        } catch (OperationsException e) {
          continue;
        }
      }
      break;
    case "DELETE":
      for(MethodInvocationHandler handler : deleteHandlers)
      {
        try {
          return invokeIfMatch(handler, request);
        } catch (OperationsException e) {
          continue;
        }
      }
      break;
      default: break;
        
    }
    throw new ReflectiveOperationException(new UnsupportedOperationException(request.getRequestMethod()));
  }
  @Override
  public void entryUpdated(EntryEvent<Serializable, Event<SerializableHttpRequest>> event) {
    entryAdded(event);
  }

  @Override
  public String keyspace() {
    return WebbitRestServerBean.ASYNC_REST_EVENT_MAP;
  }

}
