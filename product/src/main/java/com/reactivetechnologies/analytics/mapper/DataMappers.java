/* ============================================================================
*
* FILE: DataMappers.java
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
package com.reactivetechnologies.analytics.mapper;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.objenesis.ObjenesisHelper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.reactivetechnologies.analytics.core.TrainModel;
import com.reactivetechnologies.analytics.dto.ArffJsonRequest;
/**
 * This is a composite of DataMapper classes. A simple design pattern
 * to provide a strategy based factory
 */
public class DataMappers implements DataMapper{

  private static final Logger log = LoggerFactory.getLogger(DataMappers.class);
  private ClassPathScanningCandidateComponentProvider scanner;
  private final Map<String, DataMapper> cache = new HashMap<>();
    
  DataMappers()
  {
    scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new TypeFilter() {
      
      @Override
      public boolean match(MetadataReader metadataReader,
          MetadataReaderFactory metadataReaderFactory) throws IOException {
        if(metadataReader.getClassMetadata().getClassName().equals(DataMappers.class.getName()))
          return false;
        String[] iFaces = metadataReader.getClassMetadata().getInterfaceNames();
        Arrays.sort(iFaces);
        return Arrays.binarySearch(iFaces, DataMapper.class.getName()) >= 0;
      }
    });
    
    Set<BeanDefinition> set = scanner.findCandidateComponents(ClassUtils.getPackageName(DataMapper.class));
    if(set.isEmpty())
    {
      throw new BeanCreationException("No data mapper implementation classes could be found under package ["+ClassUtils.getPackageName(DataMapper.class)+"]");
    }
    for(BeanDefinition bd : set)
    {
      try 
      {
        DataMapper dm = (DataMapper) ObjenesisHelper.newInstance(Class.forName(bd.getBeanClassName()));
        cache.put(dm.type(), dm);
        log.debug("Found data mapper:: Type ["+dm.type()+"] \t"+dm.getClass());
      } catch (ClassNotFoundException e) {
        throw new BeanCreationException("Unable to instantiate data mapper class", e);
      }
      
    }
  }

  @Override
  public String type() {
    return null;
  }

  @Override
  public TrainModel mapStringToModel(ArffJsonRequest request) throws ParseException {
    Assert.notNull(request);
    if(!cache.containsKey(request.getType()))
      throw new BeansException("No data mapper found for type- "+request.getType()) {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
      };
    return cache.get(request.getType()).mapStringToModel(request);
  }
}
