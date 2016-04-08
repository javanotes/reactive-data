/* ============================================================================
*
* FILE: AsciiChunkReader.java
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
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactivetechnologies.platform.files.FileChunk;
import com.reactivetechnologies.platform.files.io.MemoryMappedChunkHandler;
/**
 * 
 */
public class AsciiChunkReader extends MemoryMappedChunkHandler {

  private static final Logger log = LoggerFactory.getLogger(AsciiChunkReader.class);
  
  private final LinkedList<AsciiFileChunk> splitChunks = new LinkedList<>();
  private AsciiFileChunk lastChunk = null;
  
  @Override
  public AsciiFileChunk readNext() throws IOException {

    if(!splitChunks.isEmpty())
    {
      lastChunk = splitChunks.removeFirst();
      idx = lastChunk.getOffset();
      chunks = lastChunk.getSize();
      log.debug("[readNext] "+lastChunk);
      return lastChunk;
    }
    else if(mapBuff.hasRemaining())
    {
      log.debug("mapBuff.remaining() => "+mapBuff.remaining());
      byte[] read = new byte[mapBuff.remaining() > readSize ? readSize : mapBuff.remaining()];
      mapBuff.get(read);
      
      log.debug("got mapped bytes..");
      lastChunk = (lastChunk == null)
          ? new AsciiFileChunk(new FileChunk(fileName, fileSize, creationTime,
              lastAccessTime, lastModifiedTime))
          : new AsciiFileChunk(lastChunk);
      
      lastChunk.setChunk(read);
      lastChunk.setOffset(idx++);
      lastChunk.setSize(chunks);
      log.debug("[before checking break] "+lastChunk);
      
      if(lastChunk.isHasLineBreak())
      {
        log.debug("Splitting chunk on line breaks");
        splitChunks.addAll(lastChunk.splitChunkOnLineBreak());
        lastChunk = splitChunks.removeFirst();
      }
      
      log.debug("[readNext] "+lastChunk);
      return lastChunk;
    }
    return null;
    
    
    //Assert.isTrue(lastIdx+1 == idx, "Chunks not in order. lastIdx=> "+lastIdx+" idx=> "+idx);
    //lastIdx = idx;
    
  }
  /**
   * New reader.
   * @param f
   * @param chunkSize
   * @throws IOException
   */
  public AsciiChunkReader(File f, int chunkSize) throws IOException {
    super(f, chunkSize);
    
  }

  

}
