/* ============================================================================
*
* FILE: AsciiFileDistributor.java
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
package com.reactivetechnologies.platform.files.utf8;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.AbstractLocalMapEntryPutListener;
import com.reactivetechnologies.platform.files.FileShareResponse;
import com.reactivetechnologies.platform.files.FileSharingAgent;
import com.reactivetechnologies.platform.utils.ByteArrayBuilder;

public class AsciiFileDistributor
    extends AbstractLocalMapEntryPutListener<AsciiFileChunk>
    implements FileSharingAgent {

  private static final Logger log = LoggerFactory.getLogger(AsciiFileDistributor.class);
  public AsciiFileDistributor(HazelcastClusterServiceBean hzService) {
    super(hzService);
    
  }

  @PostConstruct
  private void init()
  {
    log.info("-------------------------------- AsciiFileDistributor.init() -----------------------------------");
    try {
      distribute(new File("C:\\Users\\esutdal\\Documents\\vp-client.log"));
    } catch (IOException e) {
      log.error("", e);
    }
    log.info("-------------------------------- AsciiFileDistributor.end() -----------------------------------");
  }
  @Override
  public String keyspace() {
    return "HAZELCAST-DFS";
  }

  private static void readAsUTF(byte[] bytes)
  {
    System.out.println("Received=> "+new String(bytes, StandardCharsets.UTF_8));
  }
  private ByteArrayBuilder builder = new ByteArrayBuilder();
  @Override
  public void entryAdded(EntryEvent<Serializable, AsciiFileChunk> event) {
    AsciiFileChunk chunk = event.getValue();
    if(chunk.isRecordChunk())
    {
      //is a complete record
      readAsUTF(chunk.getChunk());
    }
    else if(chunk.isSplitChunk())
    {
      //append and complete record
      builder.append(chunk.getChunk());
      readAsUTF(builder.toArray());
      builder.free(true);
    }
    else
    {
      //append
      builder.append(chunk.getChunk());
    }

  }

  @Override
  public void entryUpdated(EntryEvent<Serializable, AsciiFileChunk> event) {
    //entryAdded(event);

  }

  @Override
  public Future<FileShareResponse> distribute(File f)
      throws IOException {
    try(AsciiChunkReader reader = new AsciiChunkReader(f, 8192))
    {
      log.info("Submitting file chunks..");
      AsciiFileChunk chunk = null;
      while((chunk = reader.readNext()) != null)
      {
        putEntry(chunk.generateHashCode(), chunk);
        log.debug("Put to distributed map");
      }
    }
    return null;
  }

}
