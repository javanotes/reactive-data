/* ============================================================================
*
* FILE: FeederStrategy.java
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
package com.reactivetechnologies.platform.interceptor;

import java.io.Serializable;
import java.util.Comparator;

public enum FeederStrategy {

  /**
   * Feed in single thread, with no order 
   */
  SEQUENTIAL_RANDOM,
  /**
   * Feed in single thread, with order as specified by {@link #getComparator()}
   */
  SEQUENTIAL_ORDERED,
  /**
   * Feed in multiple threads as specified by {@link #getNoOfThreads()}, with no order
   */
  PARALLEL_RANDOM,
  /**
   * Feed in multiple threads as specified by {@link #getNoOfThreads()}, with order as specified by {@link #getComparator()}
   */
  PARALLEL_ORDERED;
  
  private int noOfThreads = 1;
  private Comparator<OutboundInterceptor<Serializable>> comparator;
  
  private long timeoutSecs = 30;
  public int getNoOfThreads() {
    return noOfThreads;
  }
  public void setNoOfThreads(int noOfThreads) {
    this.noOfThreads = noOfThreads;
  }
  public Comparator<OutboundInterceptor<Serializable>> getComparator() {
    return comparator;
  }
  public void setComparator(Comparator<OutboundInterceptor<Serializable>> comparator) {
    this.comparator = comparator;
  }
  public long getTimeoutSecs() {
    return timeoutSecs;
  }
  public void setTimeoutSecs(long timeoutSecs) {
    this.timeoutSecs = timeoutSecs;
  }
}
