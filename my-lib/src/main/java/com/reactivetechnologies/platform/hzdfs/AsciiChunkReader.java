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
package com.reactivetechnologies.platform.hzdfs;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactivetechnologies.platform.files.io.MemoryMappedChunkHandler;
/**
 * 
 */
public class AsciiChunkReader extends MemoryMappedChunkHandler {

  private static final Logger log = LoggerFactory.getLogger(AsciiChunkReader.class);
  
  private final LinkedList<AsciiFileChunk> splitChunks = new LinkedList<>();
  private AsciiFileChunk lastChunk = null;
  
  private void checkChunkOrder()
  {
    /*Assert.isTrue(lastIdx+1 == idx, "Chunks not in order. lastIdx=> "+lastIdx+" idx=> "+idx);
    lastIdx = idx;*/
  }
  /**
   * Read next split chunk.
   */
  private void readNextSplit()
  {
    lastChunk = splitChunks.removeFirst();
    idx = lastChunk.getOffset();
    chunks = lastChunk.getSize();
    log.debug("[readNextSplitChunk] "+lastChunk);
    
    checkChunkOrder();
  }
  @Override
  public AsciiFileChunk readNext() throws IOException {

    if(!splitChunks.isEmpty())
    {
      readNextSplit();
      return lastChunk;
    }
    else if(mapBuff.hasRemaining())
    {
      log.debug("mapBuff.remaining() => "+mapBuff.remaining());
      byte[] read = new byte[mapBuff.remaining() > readSize ? readSize : mapBuff.remaining()];
      mapBuff.get(read);
      
      log.debug("got mapped bytes..");
      lastChunk = (lastChunk == null)
          ? new AsciiFileChunk(fileName, fileSize, creationTime,
              lastAccessTime, lastModifiedTime)
          : new AsciiFileChunk(lastChunk);
      
      lastChunk.setChunk(read);
      lastChunk.setOffset(idx++);
      lastChunk.setSize(chunks);
      log.debug("[before checking break] "+lastChunk);
      
      if(lastChunk.isHasLineBreak())
      {
        log.debug("Splitting chunk on line breaks");
        splitChunks.addAll(lastChunk.getSplitChunks());
        readNextSplit();
      }
      else
        checkChunkOrder();
      
      log.debug("[readNext] "+lastChunk);
      
      return lastChunk;
    }
    return null;
    
    
    
    
  }
  /**
   * New reader.
   * @param f
   * @param chunkSize
   * @throws IOException
   */
  public AsciiChunkReader(File f, int chunkSize) throws IOException {
    super(f, chunkSize);
    checkFileType(f);
  }
  private void checkUtf8Encoding(byte[] read) throws CharacterCodingException
  {
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
    decoder.onMalformedInput(CodingErrorAction.REPORT);
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
    try {
      decoder.decode(ByteBuffer.wrap(read));
    } catch (CharacterCodingException e) {
      throw e;
    }
  }
  private void checkFileType(File f) throws IOException {
    if(mapBuff.hasRemaining())
    {
      byte[] read = new byte[mapBuff.remaining() > 8192 ? 8192 : mapBuff.remaining()];
      mapBuff.get(read);
      mapBuff.rewind();
      
      try {
        checkUtf8Encoding(read);
      } catch (CharacterCodingException e) {
        log.debug("Unrecognized character set. --Stacktrace--", e);
        throw new IOException("Unrecognized character set. Expected UTF8 for ASCII reader");
      }
      
    }
    else
      throw new IOException("Empty file!");
    
  }

  

}
