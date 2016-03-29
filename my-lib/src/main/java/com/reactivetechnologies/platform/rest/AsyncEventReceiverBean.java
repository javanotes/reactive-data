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
package com.reactivetechnologies.platform.rest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.ServiceUnavailableException;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.EntryEvent;
import com.reactivetechnologies.platform.OperationsException;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.LocalPutMapEntryCallback;
import com.reactivetechnologies.platform.message.Event;
import com.reactivetechnologies.platform.rest.rt.AsyncEventMapConfig;
import com.reactivetechnologies.platform.rest.rt.SerializableHttpRequest;
import com.reactivetechnologies.platform.utils.GsonWrapper;
/**
 * Listener class for processing async REST invocations
 */
public class AsyncEventReceiverBean implements LocalPutMapEntryCallback<Event<SerializableHttpRequest>> {

  private static final Logger log = LoggerFactory.getLogger(AsyncEventReceiverBean.class);
  public AsyncEventReceiverBean() {
    
  }
  
  @Override
  public void entryAdded(EntryEvent<Serializable, Event<SerializableHttpRequest>> event) {
    doHandle(event.getValue().getPayload(), event.getKey().toString());
    
  }
  
  private final List<MethodInvocationHandler> getHandlers = new ArrayList<>();
  private final List<MethodInvocationHandler> postHandlers = new ArrayList<>();
  private final List<MethodInvocationHandler> deleteHandlers = new ArrayList<>();
  
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
  
  @PostConstruct
  private void init()
  {
    hzService.setMapConfiguration(AsyncEventMapConfig.class);
    hzService.addLocalEntryListener(this);
  }
  /**
   * Will be invoked from the {@linkplain DefaultAsyncResponse}.
   * @param id
   * @param returned
   */
  void dispatchResponse(String id, Object returned)
  {
    try 
    {
      if(returned instanceof Throwable)
      {
        log.warn(WebbitRestServerBean.ASYNC_REST_RESPONSE_PROCESS_ERR, (Throwable)returned);
        putResponse(id, WebbitRestServerBean.ASYNC_REST_RESPONSE_PROCESS_ERR+"["+returned.getClass().getName()+"]");
      }
      else
      {
        String jsonResponse = gson.get().toJson(returned);
        log.debug("Returning response JSON: "+jsonResponse);
        putResponse(id, jsonResponse);
      }
      
    } catch (Exception e) {
      log.error("Failed dispatching async response to cluster", e);
    }
  }
  private void processRequest(SerializableHttpRequest request, String id) throws IOException, ReflectiveOperationException
  {
    request.setImapKey(id);
    invokeServiceMethod(request);
    
  }
  /**
   * Check if the message processing is done
   * @param id
   * @return
   */
  synchronized int getResponseCode(String id)
  {
    if(hzService.contains(id, WebbitRestServerBean.ASYNC_REST_EVENT_RESPONSE_MAP))
    {
      String resp = (String) hzService.get(id, WebbitRestServerBean.ASYNC_REST_EVENT_RESPONSE_MAP);
      if(resp != null)
      {
        if(resp.contains(WebbitRestServerBean.ASYNC_REST_RESPONSE_PROCESS_ERR))
        {
          if(resp.contains(ServiceUnavailableException.class.getName()))
          {
            return HttpResponseStatus.SERVICE_UNAVAILABLE.getCode();
          }
          else
          {
            return HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode();
          }
        }
        else
        {
          return HttpResponseStatus.OK.getCode();
        }
      }
      
    }
    if(hzService.contains(id, WebbitRestServerBean.ASYNC_REST_EVENT_MAP))
    {
      return HttpResponseStatus.NO_CONTENT.getCode();//response not yet ready
    }
    return HttpResponseStatus.NOT_FOUND.getCode();
  }
  /**
   * Gets the response
   * @param id
   * @return
   */
  String getResponse(String id)
  {
    return (String) hzService.get(id, WebbitRestServerBean.ASYNC_REST_EVENT_RESPONSE_MAP);
  }
  private synchronized void putResponse(String id, String jsonResponse)
  {
    try 
    {
      hzService.put(id, jsonResponse, WebbitRestServerBean.ASYNC_REST_EVENT_RESPONSE_MAP);
      hzService.remove(id, WebbitRestServerBean.ASYNC_REST_EVENT_MAP);
      log.info("Response committed..");
    } catch (Exception e) {
      throw new IllegalArgumentException(id+"", e);
    }
  }
  
  private Object invokeIfMatch(MethodInvocationHandler handler, SerializableHttpRequest request) throws ReflectiveOperationException, OperationsException
  {
    if(handler.getMethod().getUri().matchesTemplate(request.getRequestUri()))
    {
      List<Object> args = new ArrayList<>();
      args.add(new DefaultAsyncResponse(this, request.getImapKey()));
      args.addAll(request.getArgs());
      return handler.invokeMethod(args.toArray());
    }
    throw new OperationsException("No match found for- "+request.getRequestUri());
  }
  
  private Object invokeServiceMethod(SerializableHttpRequest request) throws ReflectiveOperationException
  {
    
    switch(request.getRequestMethod())
    {
    case "GET":
      for(MethodInvocationHandler handler : getGetHandlers())
      {
        try {
          return invokeIfMatch(handler, request);
        } catch (OperationsException e) {
          continue;
        }
      }
      break;
    case "POST":
      for(MethodInvocationHandler handler : getPostHandlers())
      {
        try {
          return invokeIfMatch(handler, request);
        } catch (OperationsException e) {
          continue;
        }
      }
      break;
    case "DELETE":
      for(MethodInvocationHandler handler : getDeleteHandlers())
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
    //ignore since the response is being stored now
  }

  @Override
  public String keyspace() {
    return WebbitRestServerBean.ASYNC_REST_EVENT_MAP;
  }
  public List<MethodInvocationHandler> getPostHandlers() {
    return postHandlers;
  }
  public List<MethodInvocationHandler> getDeleteHandlers() {
    return deleteHandlers;
  }
  public List<MethodInvocationHandler> getGetHandlers() {
    return getHandlers;
  }

}
