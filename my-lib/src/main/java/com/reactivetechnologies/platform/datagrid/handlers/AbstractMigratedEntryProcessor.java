/* ============================================================================
*
* FILE: AbstractMigratedEntryProcessor.java
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
package com.reactivetechnologies.platform.datagrid.handlers;

import java.io.Serializable;
import java.util.Map.Entry;

import com.hazelcast.map.EntryProcessor;
/**
 * An {@linkplain EntryProcessor} that would be invoked on all entries migrated due to a partition migration.
 *
 * @param <V>
 */
public abstract class AbstractMigratedEntryProcessor<V> implements MigratedEntryProcessor<V> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  /**
   * Handle the migrated entry value, and return a transformed value as necessary.
   * @param key
   * @param value
   * @return
   */
  protected abstract V handleEntry(Serializable key, V value);
  @Override
  public Object process(Entry<Serializable, V> entry) {
    V value = handleEntry(entry.getKey(), entry.getValue());
    entry.setValue(value);
    return value;
  }
  
}
