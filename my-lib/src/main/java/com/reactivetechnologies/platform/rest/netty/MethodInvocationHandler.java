/* ============================================================================
*
* FILE: MethodInvocationHandler.java
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.rest.Rest;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.reactivetechnologies.platform.ContextAwareComponent;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.message.Event;
import com.reactivetechnologies.platform.rest.MethodDetail;
/**
 * Request dispatcher to JAX-RS service classes. 
 * Accepts {@linkplain PathParam} and {@linkplain QueryParam} parameters, or a single JSON body.
 */
class MethodInvocationHandler implements HttpHandler {
  private final MethodDetail method;
  public MethodDetail getMethod() {
    return method;
  }
  private final Object instance;
  private static final Logger log = LoggerFactory.getLogger(MethodInvocationHandler.class);
  private Gson gson;
  
  public void setGson(Gson gson) {
    this.gson = gson;
  }
  /**
   * 
   * @param method
   * @param instance
   */
  public MethodInvocationHandler(MethodDetail method, Object instance) {
    this.method = method;
    this.instance = instance;
    log.debug(identifier() + " instantiated..");
  }
  /**
   * for debug logging
   * @return
   */
  private String identifier()
  {
    return "[REST @"+hashCode()+"] ";
  }
  /**
   * From body content
   * @param args
   * @param jsonBody
   * @param isAsyncRequest
   */
  private void extractArgumentsFromBody(final List<Object> args, String jsonBody, boolean isAsyncRequest)
  {
    //this should be a POSTed JSON content
    //for async request, the first parameter should be AsyncResponse
    try 
    {
      Class<?> argTyp;
      if(isAsyncRequest)
      {
        if (method.getM().getParameterTypes()[0] != AsyncResponse.class) {
          throw new IllegalArgumentException(
              "Expected first argument to be of AsyncResponse type for "
                  + method.getM());
        }
        argTyp = method.getM().getParameterTypes()[1];
      }
      else
        argTyp = method.getM().getParameterTypes()[0];
        
      try {
        args.add(gson.fromJson(jsonBody, argTyp));
      } catch (JsonSyntaxException e) {
        throw new IllegalArgumentException(
            "Unable to parse request body as json for "
                + method.getM(), e);
      }
    } catch (Exception e) {
      throw e;
    }
  }
  /**
   * From params
   * @param args
   * @param request
   */
  private void extractArgumentsFromParams(List<Object> args, HttpRequest request)
  {
    //set value from path params
    for(Entry<Integer, String> entry : method.getpParams().entrySet())
    {
      args.add(entry.getKey(), Rest.param(request, entry.getValue()));
    }
    //set value from query params
    for(Entry<Integer, String> entry : method.getqParams().entrySet())
    {
      if(args.size() > entry.getKey() && args.get(entry.getKey()) != null)
      {
        //replaces any matching path param!
        args.set(entry.getKey(), request.queryParam(entry.getValue()));
        
      }
      else
        args.add(entry.getKey(), request.queryParam(entry.getValue()));
    }
  }
  /**
   * Extract method arguments from query parameters
   * @param request
   * @return
   * @throws IOException
   */
  private List<Object> extractArguments(HttpRequest request, boolean async) throws IOException
  {
    final List<Object> args = new ArrayList<>();
    String reqBody =  request.body();   
    if(StringUtils.hasText(reqBody))
    {
      try 
      {
        extractArgumentsFromBody(args, reqBody, async);
      } 
      catch (Exception e) {
        throw new IOException(identifier()+"Cannot find a method with single object argument for POST request ["+request.uri()+"]", e);  
      }
          
    }
    else
    {
      extractArgumentsFromParams(args, request);
    }
    return args;
  }
  @Override
  public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
    log.debug(identifier()+"Got new request. ");
    if(method.isAsyncRest())
    {
      prepareAsyncResponse(request, response);
      return;
    }
    
    List<Object> args = extractArguments(request, false);
    prepareResponse(args, response);
    
  }
  /**
   * Prepares an asynchronous rest response. It submits the {@linkplain}
   * @param request
   * @param response
   * @throws IOException 
   */
  private void prepareAsyncResponse(HttpRequest request, HttpResponse response) throws IOException 
  {
    HazelcastClusterServiceBean hzService = ContextAwareComponent.getBean(HazelcastClusterServiceBean.class);
    
    SerializableHttpRequest serReq = new SerializableHttpRequest();
    serReq.getArgs().addAll(extractArguments(request, true));
    serReq.setRequestMethod(request.method());
    serReq.setRequestUri(request.uri());
    
    Event<SerializableHttpRequest> event = new Event<>(serReq);
    event.setCorrelationId(hzService.getNextLong(WebbitRestServerBean.ASYNC_REST_EVENT_RESPONSE_MAP));
    
    String requestKey = AsyncEventReceiver.makeAsyncRequestKey(event);
    
    String redirectUrl = hzService.getRestContextUri(WebbitRestServerBean.ASYNC_REST_EVENT_RESPONSE_MAP)+"/"+requestKey;
    response.header("Location", redirectUrl).status(HttpResponseStatus.ACCEPTED.getCode()).end();
        
    hzService.set(requestKey, event, WebbitRestServerBean.ASYNC_REST_EVENT_MAP);
    
    log.info("[Async rest] Request submitted with redirect url: "+redirectUrl);
  }
  
  private void prepareResponse(List<Object> args, HttpResponse response) throws Exception
  {
    Object returned = invokeMethod(args.toArray());
    String json = gson.toJson(returned);
    log.debug(identifier()+" Response=> "+json);
    response.content(json).end();
  }
  /**
   * Invoke service method
   * @param args
   * @return
   * @throws ReflectiveOperationException
   */
  protected Object invokeMethod(Object... args) throws ReflectiveOperationException
  {
    if (log.isDebugEnabled()) {
      log.debug(
          identifier()+"Invoking method=> " + method + " on instance=> " + instance + ".");
      log.debug(identifier()+"Passed parameters=> " + Arrays.toString(args));
    }
    return method.getM().invoke(instance, args);
  }
  
}