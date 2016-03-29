/* ============================================================================
*
* FILE: MethodDetail.java
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
package com.reactivetechnologies.platform.rest.rt;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
/**
 * Metadata for a JAX-RS method that is mapped to URI.
 */
public class MethodDetail
{
  private boolean isAsyncRest;
  /**
   * There should be a parameter of type {@linkplain AsyncResponse} and it should be
   * annotated with {@linkplain Suspended}.
   */
  private void setAsyncRest()
  {
    int i=-1;
    for(Class<?> param : m.getParameterTypes())
    {
      i++;
      if(param == AsyncResponse.class)
        break;
      
    }
    if(i != -1)
    {
      Annotation a[] = m.getParameterAnnotations()[i];
      for(Annotation a_i : a)
      {
        if(a_i.annotationType() == Suspended.class){
          setAsyncRest(true);
          break;
        }
      }
    }
  }
  public Method getM() {
    return m;
  }
  private URIDetail uri;
  private final Method m;
  /**
   *   
   * @param m
   */
  public MethodDetail(Method m) {
    super();
    this.m = m;
    setAsyncRest();
  }
  private final Map<Integer, String> qParams = new TreeMap<>();
  private final Map<Integer, String> pParams = new TreeMap<>();
  public Map<Integer, String> getpParams() {
    return pParams;
  }
  public Map<Integer, String> getqParams() {
    return qParams;
  }
  public void setQueryParam(int i, String value) {
    qParams.put(i, value);
  }
  public URIDetail getUri() {
    return uri;
  }
  public void setUri(URIDetail uri) {
    this.uri = uri;
  }
  public void setPathParam(int i, String value) {
    pParams.put(i, value);
  }
  public boolean isAsyncRest() {
    return isAsyncRest;
  }
  private void setAsyncRest(boolean isAsyncRest) {
    this.isAsyncRest = isAsyncRest;
  }
}