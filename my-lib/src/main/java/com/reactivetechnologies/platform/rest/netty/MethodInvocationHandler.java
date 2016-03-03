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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.rest.Rest;

import com.google.gson.Gson;
import com.reactivetechnologies.platform.rest.MethodDetail;
/**
 * Request dispatcher to JAX-RS service classes. 
 * Accepts {@linkplain PathParam} and {@linkplain QueryParam} parameters, or a single JSON body.
 */
class MethodInvocationHandler implements HttpHandler {
  private final MethodDetail method;
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
   */
  private void extractArgumentsFromBody(List<Object> args, String jsonBody)
  {
  //this should be a POSTed JSON content
    try {
      Class<?> argTyp = method.getM().getParameterTypes()[0];
      args.add(gson.fromJson(jsonBody, argTyp));
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
  
  @Override
  public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
    log.debug(identifier()+"Got new request. ");
    
    final List<Object> args = new ArrayList<>();
    String reqBody =  request.body();   
    if(StringUtils.hasText(reqBody))
    {
      try 
      {
        extractArgumentsFromBody(args, reqBody);
      } 
      catch (Exception e) {
        throw new IOException(identifier()+"Cannot find a method with single object argument for POST request ["+request.uri()+"]", e);  
      }
          
    }
    else
    {
      extractArgumentsFromParams(args, request);
    }
    
    prepareResponse(args, response);
    
  }
  private void prepareResponse(List<Object> args, HttpResponse response) throws Exception
  {
    Object returned = invokeMethod(args.toArray());
    String json = gson.toJson(returned);
    log.debug(identifier()+" Response=> "+json);
    response.content(json).end();
  }
  private Object invokeMethod(Object... args) throws ReflectiveOperationException
  {
    if (log.isDebugEnabled()) {
      log.debug(
          identifier()+"Invoking method=> " + method + " on instance=> " + instance + ".");
      log.debug(identifier()+"Passed parameters=> " + Arrays.toString(args));
    }
    return method.getM().invoke(instance, args);
  }
  
}