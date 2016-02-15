/* ============================================================================
*
* FILE: ModelPersistenceStore.java
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
package com.reactivetechnologies.analytics.store;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

import com.hazelcast.core.MapStore;
import com.reactivetechnologies.analytics.core.RegressionModel;

public class WekaModelPersistenceStore implements MapStore<Long, RegressionModel> {

  @Autowired
  private CrudRepository<RegressionModel, Long> repository;
  
  @Override
  public RegressionModel load(Long key) {
    return repository.findOne(key);
  }

  @Override
  public Map<Long, RegressionModel> loadAll(Collection<Long> keys) {
    return null;
  }

  @Override
  public Iterable<Long> loadAllKeys() {
    return null;
    
  }

  @Override
  public void store(Long key, RegressionModel value) {
    value.generateId();//to generate a transient (but consistent) id
    repository.save(value);

  }

  @Override
  public void storeAll(Map<Long, RegressionModel> map) {
    if(map != null && !map.isEmpty())
    {
      for(RegressionModel r : map.values())
      {
        repository.save(r);
      }
    }

  }

  @Override
  public void delete(Long key) {
    repository.delete(key);

  }

  @Override
  public void deleteAll(Collection<Long> keys) {
    Map<Long, RegressionModel> all = loadAll(keys);
    if(all != null && !all.isEmpty())
    {
      for(Long l : all.keySet())
      {
        delete(l);
      }
    }
  }

}
