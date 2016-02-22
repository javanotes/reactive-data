/* ============================================================================
*
* FILE: EvaluationDatasetGenerator.java
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
package com.reactivetechnologies.analytics.core.eval;

import java.io.File;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.reactivetechnologies.analytics.core.Dataset;

import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.datagenerators.classifiers.classification.RandomRBF;
@Component
@ConfigurationProperties
public class CombinerDatasetGenerator {

  @Value("${weka.scheduler.combiner.dataFile}")
  private String dummyFile;
  private static final Logger log = LoggerFactory.getLogger(CombinerDatasetGenerator.class);
  private Instances instances;
  
  @PostConstruct
  private void init()
  {
    try 
    {
      ArffLoader loader = new ArffLoader();
      File f = ResourceUtils.getFile(dummyFile);
      loader.setFile(f);
      log.info("Reading file ["+f+"] for creating evaluation dataset");
      instances = loader.getDataSet();
    } catch (Exception e) {
      log.error("EvaluationDatasetGenerator::init [ "+e.getMessage() + "] Will go with a dummy dataset. ");
      log.debug("", e);
      try {
        instances = new RandomRBF().generateExamples();
      } catch (Exception e1) {
        log.debug("", e);
      }
    }
    instances.setClassIndex(instances.numAttributes()-1);
  }
  /**
   * 
   * @return
   */
  public Dataset generate()
  {
    //TODO
    return new Dataset(instances);
    
  }

}
