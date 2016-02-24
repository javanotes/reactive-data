/* ============================================================================
*
* FILE: CachedIncrementalClassifierBean.java
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
package com.reactivetechnologies.analytics.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.reactivetechnologies.analytics.core.dto.RegressionModel;
import com.reactivetechnologies.analytics.utils.ConfigUtil;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;

import weka.classifiers.Classifier;
import weka.core.Instances;

public class CachedIncrementalClassifierBean extends IncrementalClassifierBean {

  private static final Logger log = LoggerFactory.getLogger(CachedIncrementalClassifierBean.class);
  /**
   * 
   */
  private static final long serialVersionUID = 1643930081471580071L;

  public CachedIncrementalClassifierBean(Classifier c, int size) {
    super(c, size);
    
  }
  @Autowired
  private HazelcastClusterServiceBean hzService;
  
  @Override
  public void buildClassifier(Instances data) throws Exception {
    super.buildClassifier(data);
    hzService.setInstanceCachedValue(ConfigUtil.WEKA_MODEL_CACHE_MAP, new RegressionModel(clazzifier).serializeClassifierAsJson());
  }
  
  @Override
  public boolean loadAndInitializeModel() {
    log.info("Checking for any cached classifier present in cluster..");
    String serialized = hzService.getInstanceCachedValue(ConfigUtil.WEKA_MODEL_CACHE_MAP);
    if (StringUtils.hasText(serialized)) {
      try {
        RegressionModel m = new RegressionModel(serialized);
        clazzifier = m.getTrainedClassifier();
        log.debug("Found pre loaded classifier.. " + clazzifier);
        return true;
      } catch (Exception e) {
        log.warn("Ignoring exception caught while checking- " + e);
        log.debug("", e);
      } 
    }
    return false;
        
  }

}
