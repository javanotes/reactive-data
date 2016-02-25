/* ============================================================================
*
* FILE: RestServer.java
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
package com.reactivetechnologies.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

import com.reactivetechnologies.analytics.core.dto.CombinerResult;
import com.reactivetechnologies.analytics.core.handlers.ModelCombinerComponent;
import com.reactivetechnologies.analytics.utils.GsonWrapper;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

class RestServerBean extends NanoHTTPD {

  private static final Logger log = LoggerFactory.getLogger(RestServerBean.class);
  
  /**
   * 
   * @param port
   * @param nThreads
   */
  public RestServerBean(int port, int nThreads) {
    super(port);
    setAsyncRunner(new AsyncRunnerExecutor(nThreads));
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

  @Autowired
  private ModelCombinerComponent combiner;
  protected static Response jsonResponse(String json)
  {
    return newFixedLengthResponse(Status.OK, MimeTypeUtils.APPLICATION_JSON_VALUE, json);
  }
  protected static Response invalid(String message)
  {
    return newFixedLengthResponse(Status.BAD_REQUEST, MimeTypeUtils.TEXT_PLAIN_VALUE, message);
  }
  protected static Response error(String message)
  {
    return newFixedLengthResponse(Status.INTERNAL_ERROR, MimeTypeUtils.TEXT_PLAIN_VALUE, message);
  }
  private Response doPost(IHTTPSession session) {
    return null;
    // TODO Auto-generated method stub
    
  }

  private Response doGet(IHTTPSession session) {
    if(session.getUri().contains("runCombiner"))
    {
      log.info("Request to run combiner");
      try {
        CombinerResult result = combiner.runTask();
        return jsonResponse(GsonWrapper.get().toJson(result));
      } catch (EngineException e) {
        log.error("Engine run failed", e);
        return error("Server error");
      }
    }
    return invalid("No matching URI");
    
  }


}
