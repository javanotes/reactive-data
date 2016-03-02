/* ============================================================================
*
* FILE: WebbitRestServerBean.java
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedIOException;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.webbitserver.WebServer;
import org.webbitserver.netty.NettyWebServer;
import org.webbitserver.rest.Rest;

import com.reactivetechnologies.platform.rest.DefaultRequestDispatcher;
import com.reactivetechnologies.platform.rest.MethodDetail;
import com.reactivetechnologies.platform.rest.Serveable;
import com.reactivetechnologies.platform.rest.json.GsonWrapperComponent;
import com.reactivetechnologies.platform.utils.EntityFinder;
/**
 * Simple REST server using {@link NettyWebServer Webbit} library, consuming and producing JSON messages
 */
public class WebbitRestServerBean implements Serveable{

  //TODO - The server is using a single threaded boss and single threaded worker
  
  private static final Logger log = LoggerFactory.getLogger(WebbitRestServerBean.class);
  private String annotatedPkgToScan;
  private final WebServer server;
  
  /**
   * Instantiate a REST server
   * @param port listening port
   * @param nThreads no of worker threads
   * @param annotatedPkgToScan base package to scan for JAX-RS annotated classes
   */
  public WebbitRestServerBean(int port, int nThreads, String annotatedPkgToScan) {
    super();
    this.annotatedPkgToScan = annotatedPkgToScan;
    server = new NettyWebServer(port);
  }
  @Autowired
  private GsonWrapperComponent gsonWrapper;
  /**
   * 
   * @param struct
   * @param rest 
   */
  private void mapRoute(DefaultRequestDispatcher struct) {
    for(MethodDetail m : struct.getDogetMethods())
    {
      Dispatcher rh = new Dispatcher(m, struct.getJaxrsObject());
      rh.setGson(gsonWrapper.get());
      service.GET(m.getUri().getRawUri(), rh);
    }
    for(MethodDetail m : struct.getDopostMethods())
    {
      Dispatcher rh = new Dispatcher(m, struct.getJaxrsObject());
      rh.setGson(gsonWrapper.get());
      service.POST(m.getUri().getRawUri(), rh);
    }
    for(MethodDetail m : struct.getDodelMethods())
    {
      Dispatcher rh = new Dispatcher(m, struct.getJaxrsObject());
      rh.setGson(gsonWrapper.get());
      service.DELETE(m.getUri().getRawUri(), rh);
    }
    
  }
  /**
   * 
   * @param method
   * @return
   */
  private static MethodDetail createMethodDetail(Method method)
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
        if(annot.annotationType() == PathParam.class)
        {
          m.setPathParam(i, ((PathParam)annot).value());
        }
      }
      i++;
    }
    return m;
  }
  /**
   * 
   * @param restletClass
   * @return
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  private DefaultRequestDispatcher scanJaxRsClass(Class<?> restletClass) throws InstantiationException, IllegalAccessException
  {
    final DefaultRequestDispatcher proxy = new DefaultRequestDispatcher(restletClass.newInstance());
    if(restletClass.isAnnotationPresent(Path.class))
    {
      String rootUri = restletClass.getAnnotation(Path.class).value();
      if(rootUri == null)
        rootUri = "";
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
          MethodDetail m = createMethodDetail(method);
          proxy.addGetMethod(proxy.getRootUri()+subUri, m);
        }
        if(method.isAnnotationPresent(POST.class))
        {
          MethodDetail m = createMethodDetail(method);
          proxy.addPostMethod(proxy.getRootUri()+subUri, m);
        }
        if(method.isAnnotationPresent(DELETE.class))
        {
          MethodDetail m = createMethodDetail(method);
          proxy.addDelMethod(proxy.getRootUri()+subUri, m);
        }
        
      }
    }, new MethodFilter() {
      
      @Override
      public boolean matches(Method method) {
        return (method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(POST.class) || method.isAnnotationPresent(DELETE.class));
      }
    });
    
    return proxy;
  }
  private Rest service;
  /**
   * 
   */
  @PostConstruct
  private void defineRoutes() {
    log.info("[REST] Scanning for JAX-RS annotated classes.. ");
    try 
    {
      service = new Rest(server);
      
      for(Class<?> clazz : EntityFinder.findJaxRSClasses(annotatedPkgToScan))
      {
        DefaultRequestDispatcher struct = scanJaxRsClass(clazz);
        mapRoute(struct);
        log.info("[REST] Mapped JAX-RS annotated class "+clazz.getName());
      }
    } catch (Exception e) {
      throw new BeanCreationException("Unable to create factory bean", e);
    }
    log.info("[REST] Loaded JAX-RS classes..");
    run();
  }
  @Override
  public void close() throws IOException {
    try {
      server.stop();
    } catch (Exception e) {
      throw new NestedIOException("With nested cause..", e);
    }
  }

  @Override
  public void run() {
    try {
      server.start().get();
    } catch (ExecutionException e) {
      throw new BeanInitializationException("[REST] Server startup failed!", e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BeanInitializationException("[REST] Server was interrupted while trying to start", e);
    }
    log.info("[REST] Server started listening for HTTP POST/GET on port- "+server.getPort());
  }


}
