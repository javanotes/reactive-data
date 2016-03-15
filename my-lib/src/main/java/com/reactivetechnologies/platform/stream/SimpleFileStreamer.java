/* ============================================================================
*
* FILE: SimpleFileStreamer.java
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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;

/**
 * A class to test the distributed piped streams.
 * @deprecated For experimental purpose. Use {@linkplain AbstractFileSharingAgent} instead.
 * @see {@linkplain JarFileSharingAgent}
 */
public class SimpleFileStreamer {
  
  @Autowired
  private HazelcastClusterServiceBean hzService;
  public SimpleFileStreamer()
  {
    
  }
  @PostConstruct
  private void onLoad()
  {
    
    OutputStream fileOut = new DistributedPipedOutputStream(hzService) {
      
      @Override
      public String topic() {
        return Configurator.PIPED_TOPIC_FILE;
      }
    };
    
    InputStream fileIn = new DistributedPipedInputStream(hzService) {
      
      @Override
      public String topic() {
        return Configurator.PIPED_TOPIC_FILE;
      }
    };
    try 
    {
      
      DataInputStream bs  = new DataInputStream(new FileInputStream("C:\\Users\\esutdal\\WORK\\workspace\\COX\\spring-data-hazelcast\\test-hz-config.xml"));
      int available = 0;
      while((available = bs.available()) != 0)
      {
        byte[] read = new byte[available];
        bs.read(read);
        fileOut.write(read);
      }
      fileOut.flush();
      bs.close();
      
      bs  = new DataInputStream(fileIn);
      BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File("C:\\Users\\esutdal\\test-hz-config.xml"), true));
      available = 0;
      while((available = bs.available()) != 0)
      {
        byte[] read = new byte[available];
        bs.read(read);
        bout.write(read);
      }
      bout.flush();
      bout.close();
      bs.close();
      
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    try {
      fileOut.close();
      fileIn.close();
    } catch (IOException e) {
      
    }
  }
}
