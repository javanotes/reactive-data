/* ============================================================================
*
* FILE: IncomingInterceptor.java
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
/**
 * 
 *
 * @param <X> input type
 * @param <T> transformed type (can be same as input)
 */
public interface InboundInterceptor<X, T> {
  /**
   * Intercept and transform inbound message
   * @param key
   * @param _new
   * @param _old
   * @return
   */
  T intercept(Serializable key, X _new, X _old) throws ChannelException;
  /**
   * 
   * @return
   */
  String name();
  /**
   * 
   * @return
   */
  Class<X> inType();
  /**
   * 
   * @return
   */
  Class<T> outType();
  /**
   * Set the downstream handler
   * @param outChannel
   */
  void setOutChannel(AbstractOutboundChannel outChannel);
}
