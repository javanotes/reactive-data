/* ============================================================================
*
* FILE: EntityFinder.java
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
package com.reactivetechnologies.platform.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.StringUtils;

import com.reactivetechnologies.platform.datagrid.annotation.HzMapConfig;

public class EntityFinder {

  private static final Logger log = LoggerFactory.getLogger(EntityFinder.class);
  /**
   * 
   * @param basePkg
   * @return
   * @throws ClassNotFoundException
   */
  public static Collection<Class<?>> findMapEntityClasses(String basePkg) throws ClassNotFoundException
  {
    ClassPathScanningCandidateComponentProvider provider= new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new TypeFilter() {
      
      @Override
      public boolean match(MetadataReader metadataReader,
          MetadataReaderFactory metadataReaderFactory) throws IOException {
        AnnotationMetadata aMeta = metadataReader.getAnnotationMetadata();
        return aMeta.hasAnnotation(HzMapConfig.class.getName());
      }
    });
    //consider the finder class to be in the root package
    Set<BeanDefinition> beans = null;
    try {
      beans = provider.findCandidateComponents(StringUtils.hasText(basePkg) ? basePkg : 
        EntityFinder.class.getName().substring(0, EntityFinder.class.getName().lastIndexOf(".")));
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to scan for entities under base package", e);
    }
    
    Set<Class<?>> classes = new HashSet<>();
    if (beans != null && !beans.isEmpty()) {
      classes = new HashSet<>(beans.size());
      for (BeanDefinition bd : beans) {
        classes.add(Class.forName(bd.getBeanClassName()));
      } 
    }
    else
    {
      log.warn(">> Did not find any key value entities under the given base scan package ["+basePkg+"]");
    }
    return classes;
    
    
  }
  /**
   * 
   * @param basePkg
   * @return
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   */
  public static List<Class<?>> findJaxRSClasses(String basePkg) throws ClassNotFoundException, IllegalAccessException
  {
    ClassPathScanningCandidateComponentProvider provider= new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new TypeFilter() {
      
      @Override
      public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        AnnotationMetadata aMeta = metadataReader.getAnnotationMetadata();
        return aMeta.hasAnnotation(Path.class.getName()) || 
            (   aMeta.hasAnnotatedMethods(GET.class.getName()) || 
                aMeta.hasAnnotatedMethods(POST.class.getName()) || 
                aMeta.hasAnnotatedMethods(Path.class.getName())
            )
        ;
      }
    });
    
    Set<BeanDefinition> beans = null;
    try {
      beans = provider.findCandidateComponents(StringUtils.hasText(basePkg) ? basePkg : 
        EntityFinder.class.getName().substring(0, EntityFinder.class.getName().lastIndexOf(".")));
    } catch (Exception e) {
      throw new BeanInitializationException("Unable to scan for JAX-RS annotated classes under base package", e);
    }
    List<Class<?>> classes = new ArrayList<Class<?>>();
    if (beans != null && !beans.isEmpty()) {
      Class<?> restletClass;
      for (BeanDefinition bd : beans) {
        restletClass = Class.forName(bd.getBeanClassName());
        classes.add(restletClass);
      } 
    }
    else
    {
      log.warn("** Did not find any JAX-RS annotated classes under the given base scan package ["+basePkg+"]. No REST requests will be served **");
    }
    return classes;
        
  }
}
