/* ============================================================================
*
* FILE: RestletProxyImpl.java
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultRequestDispatcher implements RequestDispatcher {

  private static Logger log = LoggerFactory.getLogger(DefaultRequestDispatcher.class);
  private String rootUri = "";
  private final Object jaxrsObject;
  public Object getJaxrsObject() {
    return jaxrsObject;
  }

  private List<MethodDetail> dogetMethods = new ArrayList<>();
  public List<MethodDetail> getDogetMethods() {
    return Collections.unmodifiableList(dogetMethods);
  }

  private List<MethodDetail> dopostMethods = new ArrayList<>();
  private List<MethodDetail> dodelMethods = new ArrayList<>();
  
  public List<MethodDetail> getDodelMethods() {
    return Collections.unmodifiableList(dodelMethods);
  }

  public List<MethodDetail> getDopostMethods() {
    return Collections.unmodifiableList(dopostMethods);
  }

  public DefaultRequestDispatcher(Object target) {
    this.jaxrsObject = target;
    
  }
  
  private static MethodDetail findMethodForUri(String uri, List<MethodDetail> methods)
  {
    for(MethodDetail m : methods)
    {
      if(m.getUri().matches(uri))
        return m;
    }
    return null;
  }
  
  public String getRootUri() {
    return rootUri;
  }

  public void setRootUri(String rootUri) {
    this.rootUri = rootUri;
  }
 

  @Override
  public Object invokePostUrl(String uri, Map<String, String> params,
      Object requestBody) throws IllegalAccessException {
    MethodDetail m = findMethodForUri(uri, dopostMethods);
    if(m == null)
    {
      throw new IllegalAccessException("Mapping not found");
    }
    return invokeMethod(params, m);
    
  }
  
  public MethodDetail findGetMethodForUri(String uri)
  {
    return findMethodForUri(uri, dogetMethods);
  }
  public MethodDetail findPostMethodForUri(String uri)
  {
    return findMethodForUri(uri, dopostMethods);
  }

  @Override
  public Object invokeGetUrl(String uri, Map<String, String> params) throws IllegalAccessException {
    MethodDetail m = findMethodForUri(uri, dogetMethods);
    if(m == null)
    {
      throw new IllegalAccessException("Mapping not found");
    }
    return invokeMethod(params, m);
        
  }

  private Object invokeMethod(Map<String, String> params, MethodDetail m) throws IllegalAccessException
  {
    if(params != null && !params.isEmpty())
    {
      System.err.println("[RestletProxyImpl] WARN: Request params will be ignored.");
    }
    try {
      return m.getM().invoke(jaxrsObject);
    } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
      IllegalAccessException ig = new IllegalAccessException("Cannot execute method on JAX-RS object");
      ig.initCause(e);
      throw ig;
    }
  }

  public void addGetMethod(String uri, MethodDetail m) {
    m.setUri(new URIDetail(uri));
    dogetMethods.add(m);
    log.info("Mapped: GET "+uri+" to "+m.getM());
  }


  public void addPostMethod(String uri, MethodDetail m) {
    m.setUri(new URIDetail(uri));
    dopostMethods.add(m);
    log.info("Mapped: POST "+uri+" to "+m.getM());
  }
  public void addDelMethod(String uri, MethodDetail m) {
    m.setUri(new URIDetail(uri));
    dodelMethods.add(m);
    log.info("Mapped: DELETE "+uri+" to "+m.getM());
  }

  public boolean matchesPost(String uri) {
    return findMethodForUri(uri, dopostMethods) != null;
  }

  public boolean matchesGet(String uri) {
    return findMethodForUri(uri, dogetMethods) != null;
  }

  

}
