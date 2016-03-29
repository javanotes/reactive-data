/* ============================================================================
*
* FILE: SimpleBytesStreamer.java
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;

/**
 * A class to test the distributed piped streams.
 * For experimental purpose. 
 */
public class SimpleBytesStreamer implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(SimpleBytesStreamer.class);
  
  @Autowired
  private HazelcastClusterServiceBean hzService;
  /**
   * 
   */
  public SimpleBytesStreamer()
  {
    
  }
  private DistributedPipedOutputStream out;
  private DistributedPipedInputStream in;
  /**
   * Fetches from a stream source and write to target. Sender side
   * @param source
   * @throws IOException 
   */
  public void write(InputStream source) throws IOException
  {
    if(!running)
      throw new IOException("Illegal state! Not running");
    
    try(BufferedInputStream bs = new BufferedInputStream(new DataInputStream(source)))
    {
      int available = 0;
      while((available = bs.available()) != 0)
      {
        byte[] read = new byte[available];
        bs.read(read);
        out.write(read);
      }
      out.flush();
      out.close();
    }
    
  }
  @Autowired
  private IBytesHandler handler;
  @PostConstruct
  private void onLoad()
  {
    out = new DistributedPipedOutputStream(hzService);
    in = new DistributedPipedInputStream(hzService);
    running = true;
    new Thread(this, "DistributedStream-Reader").start();
  }
  private boolean running;
  @PreDestroy
  private void stop()
  {
    running = false;
    out.disconnect();
    in.disconnect();
  }
  @Override
  public void run() {
    
      int available = 0;
      
        while(running)
        {
          try 
          {
            if ((available = in.awaitUntilAvailable(10, TimeUnit.SECONDS)) != 0) 
            {
              try 
              {
                do 
                {
                  byte[] read = new byte[available];
                  in.read(read);
                  handler.onNextBytes(read);
                } 
                while ((available = in.available()) != 0);
                handler.onNextBytes(null);
                in.close();
              } catch (IOException e) {
                log.error("While reading bytes", e);
              } 
            }
          } catch (InterruptedException e) {
            log.debug("", e);
            Thread.currentThread().interrupt();
          }
          
        }
      
  }
}
