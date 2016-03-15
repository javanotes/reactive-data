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
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.webbitserver.netty.NettyWebServer;
import org.webbitserver.rest.Rest;

import com.reactivetechnologies.platform.datagrid.HzMapConfig;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.rest.JaxrsInstanceMetadata;
import com.reactivetechnologies.platform.rest.MethodDetail;
import com.reactivetechnologies.platform.rest.Serveable;
import com.reactivetechnologies.platform.utils.EntityFinder;
import com.reactivetechnologies.platform.utils.GsonWrapper;
/**
 * Simple REST server using {@link NettyWebServer Webbit} library, consuming and producing JSON messages.
 */
public class WebbitRestServerBean implements Serveable{

  private static final Logger log = LoggerFactory.getLogger(WebbitRestServerBean.class);
  private String annotatedPkgToScan;
  private final NettyWebServer server;
  private final Rest restWrapper;
  /**
   * Instantiate a REST server
   * @param port listening port
   * @param nThreads no of worker threads
   * @param annotatedPkgToScan base package to scan for JAX-RS annotated classes
   */
  public WebbitRestServerBean(int port, int nThreads, String annotatedPkgToScan) {
    super();
    this.annotatedPkgToScan = annotatedPkgToScan;
    server = new NettyWebServer(Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
      private int n=0;
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "REST.Handler-"+(n++));
        t.setDaemon(false);
        return t;
      }
    }), port).uncaughtExceptionHandler(new UncaughtExceptionHandler() {
      
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        log.error("Worker thread ["+t+"] caught unexpected exception:", e);
        
      }
    });
    restWrapper = new Rest(server);
  }
  @Autowired
  private GsonWrapper gsonWrapper;
  @Autowired
  private HazelcastClusterServiceBean hzService;
  /**
   * 
   * @param meta
   */
  private void defineRoute(JaxrsInstanceMetadata meta) {
    for(MethodDetail m : meta.getDogetMethods())
    {
      MethodInvocationHandler rh = new MethodInvocationHandler(m, meta.getJaxrsObject());
      rh.setGson(gsonWrapper.get());
      restWrapper.GET(m.getUri().getRawUri(), rh);
      eventReceiver.getHandlers.add(rh);
    }
    for(MethodDetail m : meta.getDopostMethods())
    {
      MethodInvocationHandler rh = new MethodInvocationHandler(m, meta.getJaxrsObject());
      rh.setGson(gsonWrapper.get());
      restWrapper.POST(m.getUri().getRawUri(), rh);
      eventReceiver.postHandlers.add(rh);
    }
    for(MethodDetail m : meta.getDodelMethods())
    {
      MethodInvocationHandler rh = new MethodInvocationHandler(m, meta.getJaxrsObject());
      rh.setGson(gsonWrapper.get());
      restWrapper.DELETE(m.getUri().getRawUri(), rh);
      eventReceiver.deleteHandlers.add(rh);
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
  static JaxrsInstanceMetadata scanJaxRsClass(Class<?> restletClass) throws InstantiationException, IllegalAccessException
  {
    final JaxrsInstanceMetadata proxy = new JaxrsInstanceMetadata(restletClass.newInstance());
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
  
  @Autowired
  private AsyncEventReceiver eventReceiver;
  
  /**
   * Maps JAX RS classes as service methods
   * @param basePkgToScan
   * @throws Exception
   */
  public void mapServiceRoute(String basePkgToScan) throws Exception
  {
    try 
    {
      for(Class<?> clazz : EntityFinder.findJaxRsClasses(basePkgToScan))
      {
        JaxrsInstanceMetadata struct = scanJaxRsClass(clazz);
        defineRoute(struct);
        log.info("[REST Listener] Mapped JAX-RS annotated class "+clazz.getName());
      }
    } catch (Exception e) {
      throw e;
    }
  }
  /**
   * 
   */
  @PostConstruct
  private void defineRoutes() {
    hzService.setMapConfiguration(AsyncEventMapConfig.class);
    hzService.addLocalEntryListener(eventReceiver);
    
    log.info("[REST Listener] Scanning for JAX-RS annotated classes.. ");
    try 
    {
      mapServiceRoute(annotatedPkgToScan);
      
    } catch (Exception e) {
      throw new BeanCreationException("Unable to create factory bean", e);
    }
    log.info("[REST Listener] Loaded JAX-RS classes..");
    run();
  }
  @Override@PreDestroy
  public void close() throws IOException {
    try {
      server.stop();
    } catch (Exception e) {
      throw new NestedIOException("With nested cause..", e);
    }
  }
  
  static final String ASYNC_REST_EVENT_MAP = AnnotationUtils.findAnnotation(AsyncEventMapConfig.class, HzMapConfig.class).name();
  static final String ASYNC_REST_EVENT_RESPONSE_MAP = AnnotationUtils.findAnnotation(AsyncEventMapConfig.class, HzMapConfig.class).name();
  
  @Override
  public void run() {
    try {
      server.start().get();
    } catch (ExecutionException e) {
      throw new BeanInitializationException("[REST Listener] Server startup failed!", e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BeanInitializationException("[REST Listener] Server was interrupted while trying to start", e);
    }
    log.info("[REST Listener] Server started for POST/GET/DELETE on port- "+server.getPort());
  }


}
