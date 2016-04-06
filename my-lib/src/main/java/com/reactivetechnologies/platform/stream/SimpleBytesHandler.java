/* ============================================================================
*
* FILE: SimpleBytesHandler.java
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
package com.reactivetechnologies.platform.stream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class SimpleBytesHandler implements IBytesHandler, Closeable {

  public SimpleBytesHandler() {
    pOut = new PipedOutputStream();
    try {
      pIn = new PipedInputStream(pOut);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to connect piped channel", e);
    }
  }
  private final PipedOutputStream pOut;
  private final PipedInputStream pIn;
  
  @Override
  public void onNextBytes(byte[] bytes) {
    try 
    {
      if(bytes == null)
      {
        pOut.write(-1);
      }
      else
        pOut.write(bytes);
    } catch (IOException e) {
      throw new RuntimeException("Unable to write to channel", e);
    }
  }
  @Override
  public InputStream getInputStream() {
    return pIn;
  }
  @Override
  public void close() throws IOException {
    pOut.close();
    pIn.close();
  }
  @Override
  public boolean hasStream() {
    return true;
  }
  
  
}
