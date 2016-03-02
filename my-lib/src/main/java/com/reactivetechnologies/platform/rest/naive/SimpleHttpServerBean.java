/* ============================================================================
*
* FILE: RestServer.java
*
Thse MIT License (MIT)

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
package com.reactivetechnologies.platform.rest.naive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

import com.reactivetechnologies.platform.rest.RequestDispatcher;
import com.reactivetechnologies.platform.rest.Serveable;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
/**
 * This class is to be extended as a bean to get a simple general purpose REST server, consuming POST/GET.
 * @deprecated Using Netty based server
 */
public abstract class SimpleHttpServerBean extends NanoHTTPD implements Serveable {

  private static final Logger log = LoggerFactory.getLogger(SimpleHttpServerBean.class);
  @Autowired
  private RequestDispatcher proxy;
  /**
   * 
   * @param port
   * @param nThreads
   */
  public SimpleHttpServerBean(int port, int nThreads) {
    super(port);
    setAsyncRunner(new AsyncRunnerExecutor(nThreads));
  }
  @PostConstruct
  @Override
  public void run()
  {
    try {
      start(SOCKET_READ_TIMEOUT, false);
      log.info("[REST Server] Started listening for HTTP POST/GET on port- "+getListeningPort());
    } catch (IOException e) {
      throw new BeanCreationException("Unable to start REST Server", e);
    }
  }
  @PreDestroy
  @Override
  public void close()
  {
    stop();
    log.info("[REST Server] Stopped listening for HTTP POST/GET ");
  }
  @Override
  public Response serve(IHTTPSession session) {
      
      Method m = session.getMethod();
      switch(m)
      {
      
        case GET:
          return doGet(session);
        case POST:
          return doPost(session);
        default:
          return newFixedLengthResponse(Status.NOT_IMPLEMENTED, MimeTypeUtils.TEXT_PLAIN_VALUE,"HTTP "+m);
      
      }
  }

  /**
   * Send a JSON response with HTTP 200
   * @param json
   * @return
   */
  protected static Response success(String json)
  {
    return newFixedLengthResponse(Status.OK, MimeTypeUtils.APPLICATION_JSON_VALUE, json);
  }
  /**
   * Send a text response with HTTP 400
   * @param message
   * @return
   */
  protected static Response invalid(String message)
  {
    return newFixedLengthResponse(Status.BAD_REQUEST, MimeTypeUtils.TEXT_PLAIN_VALUE, message);
  }
  /**
   * Send a text message with HTTP 500
   * @param message
   * @return
   */
  protected static Response error(String message)
  {
    return newFixedLengthResponse(Status.INTERNAL_ERROR, MimeTypeUtils.TEXT_PLAIN_VALUE, message);
  }
  /**
   * Send a text message with HTTP 404
   * @param message
   * @return
   */
  protected static Response notFound(String message)
  {
    return newFixedLengthResponse(Status.NOT_FOUND, MimeTypeUtils.TEXT_PLAIN_VALUE, message);
  }
  /**
   * Send a text message with HTTP 401
   * @param message
   * @return
   */
  protected static Response unauthorized(String message)
  {
    return newFixedLengthResponse(Status.UNAUTHORIZED, MimeTypeUtils.TEXT_PLAIN_VALUE, message);
  }
  /**
   * Send a text message with HTTP 403
   * @param message
   * @return
   */
  protected static Response forbidden(String message)
  {
    return newFixedLengthResponse(Status.FORBIDDEN, MimeTypeUtils.TEXT_PLAIN_VALUE, message);
  }
  /* (non-Javadoc)
   * @see com.reactivetechnologies.platform.rest.SimpleHttpServer#doPost(fi.iki.elonen.NanoHTTPD.IHTTPSession)
   */
  public Response doPost(IHTTPSession session) {
    StringBuilder s = new StringBuilder();
    try(BufferedReader br = new BufferedReader(new InputStreamReader(session.getInputStream())))
    {
      String line;
      while((line = br.readLine()) != null)
      {
        s.append(line);
      }
      Object o = proxy.invokePostUrl(session.getUri(), session.getParms(), fromJSONRequest(s.toString()));
      return success(toJSONResponse(o));
    } catch (IOException | IllegalAccessException e) {
      log.error("[REST Server] In doPost request stream", e);
      return error("Server error");
    }
    
  }
  /**
   * Convert the input JSON request to a model object
   * @param json
   * @return
   */
  protected abstract <T> T fromJSONRequest(String json);
  /* (non-Javadoc)
   * @see com.reactivetechnologies.platform.rest.SimpleHttpServer#doGet(fi.iki.elonen.NanoHTTPD.IHTTPSession)
   */
  public Response doGet(IHTTPSession session) {
    try 
    {
      Object o = proxy.invokeGetUrl(session.getUri(), session.getParms());
      return success(toJSONResponse(o));
    } catch (IllegalAccessException e) {
      log.error("[REST Server] In doGet request stream", e);
      return error("Server error");
    }
        
  }
  /**
   * Convert the returned object to a JSON response
   * @param <T>
   * @param serviceReturn
   * @return
   */
  protected abstract <T> String toJSONResponse(T serviceReturn) ;


}
