/* ============================================================================
*
* FILE: RestletFactory.java
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
package com.reactivetechnologies.platform.rest.depre;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import com.reactivetechnologies.platform.rest.JaxrsInstanceMetadata;
import com.reactivetechnologies.platform.rest.MethodDetail;
import com.reactivetechnologies.platform.utils.EntityFinder;
/**
 * A factory class for @link{RequestDispatcher}
 * @deprecated
 */
class RequestDispatchers implements RequestDispatcher{

  private static final Logger log = LoggerFactory.getLogger(RequestDispatchers.class);
  /**
   * 
   * @param basePkgToScan
   */
  public RequestDispatchers(String basePkgToScan) {
    this.basePkgToScan = basePkgToScan;
    init();
  }
  private final Map<String, RequestDispatcher> loadedClasses = new WeakHashMap<>();
  private final List<JaxrsInstanceMetadata> allClasses = new ArrayList<>();
  
  String basePkgToScan;
  
  private void init()
  {
    log.info("Scanning for JAX-RS annotated classes.. ");
    try {
      for(Class<?> clazz : EntityFinder.findJaxRSClasses(basePkgToScan))
      {
        addAnnotatedClass(clazz);
      }
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
      throw new BeanCreationException("Unable to create factory bean", e);
    }
    log.info("Loaded JAX-RS classes..");
  }
  
  private void addAnnotatedClass(Class<?> restletClass) throws InstantiationException, IllegalAccessException
  {
    final JaxrsInstanceMetadata proxy = new JaxrsInstanceMetadata(restletClass.newInstance());
    if(restletClass.isAnnotationPresent(Path.class))
    {
      String rootUri = restletClass.getAnnotation(Path.class).value();
      proxy.setRootUri(rootUri);
    }
    ReflectionUtils.doWithMethods(restletClass, new MethodCallback() {
      
      @Override
      public void doWith(Method method)
          throws IllegalArgumentException, IllegalAccessException {
        String subUri = "";
        if(method.isAnnotationPresent(Path.class))
        {
          subUri = method.getAnnotation(Path.class).value();
        }
        if(method.isAnnotationPresent(GET.class))
        {
          MethodDetail m = new MethodDetail(method);
          Annotation[][] mAnnots = method.getParameterAnnotations();
          int i = 0;
          for(Annotation[] annots : mAnnots)
          {
            for(Annotation annot : annots)
            {
              if(annot.annotationType() == QueryParam.class)
              {
                m.setQueryParam(i, ((QueryParam)annot).value());
              }
            }
            i++;
          }
          proxy.addGetMethod(proxy.getRootUri()+subUri, m);
        }
        if(method.isAnnotationPresent(POST.class))
        {
          MethodDetail m = new MethodDetail(method);
          Annotation[][] mAnnots = method.getParameterAnnotations();
          int i = 0;
          for(Annotation[] annots : mAnnots)
          {
            for(Annotation annot : annots)
            {
              if(annot.annotationType() == QueryParam.class)
              {
                m.setQueryParam(i, ((QueryParam)annot).value());
              }
            }
            i++;
          }
          proxy.addPostMethod(proxy.getRootUri()+subUri, m);
        }
        
      }
    }, new MethodFilter() {
      
      @Override
      public boolean matches(Method method) {
        return (method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(POST.class));
      }
    });
    
    allClasses.add(proxy);
  }
  @Override
  public Object invokePostUrl(String uri, Map<String, String> params,
      Object requestBody) throws IllegalAccessException {
    if(!loadedClasses.containsKey("POST:"+uri))
    {
      for(JaxrsInstanceMetadata p : allClasses)
      {
        if(p.matchesPost(uri))
        {
          loadedClasses.put("POST:"+uri, p);
          break;
        }
      }
    }
    RequestDispatcher proxy = loadedClasses.get("POST:"+uri);
    return proxy.invokePostUrl(uri, params, requestBody);
  }
  @Override
  public Object invokeGetUrl(String uri, Map<String, String> params)
      throws IllegalAccessException {
    if(loadedClasses.containsKey("GET:"+uri))
    {
      for(JaxrsInstanceMetadata p : allClasses)
      {
        if(p.matchesGet(uri))
        {
          loadedClasses.put("GET:"+uri, p);
          break;
        }
      }
    }
    RequestDispatcher proxy = loadedClasses.get("GET:"+uri);
    return proxy.invokeGetUrl(uri, params);
  }

}
