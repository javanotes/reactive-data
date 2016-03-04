/* ============================================================================
*
* FILE: RegressionService.java
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
package com.reactivetechnologies.analytics.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactivetechnologies.analytics.core.dto.CombinerResult;
import com.reactivetechnologies.analytics.core.handlers.ModelCombinerComponent;
import com.reactivetechnologies.platform.ContextAwareComponent;

public class RegressionService{

  public RegressionService() {
    
  }
  private static final Logger log = LoggerFactory.getLogger(RegressionService.class);
  @Path("/reactive/runTask")
  @GET
  public CombinerResult runCombinerTask()
  {
    log.info("-- Start combiner run --");
    try 
    {
      ModelCombinerComponent combiner = ContextAwareComponent.getBean(ModelCombinerComponent.class);
      CombinerResult result = combiner.runTask();
      log.info("Result: "+result);
      return result;
    } catch (Exception e) {
      log.error("Exception while running combiner", e);
    }
    log.info("-- End combiner run --");
    return null;
    
  }
  @Path("/reactive/runTask/{group}/set/{id}")
  @GET
  public String runTask(@PathParam("group") String group, @PathParam("id") String id, @QueryParam("qp") String param)
  {
    return new String("Got group:"+group+" and id:"+id+" and qp:"+param);
    
  }
  @GET
  @Path("/async")
  public String helloAsync(@Suspended AsyncResponse resp)
  {
    return "async!";
  }

}
