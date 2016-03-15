/* ============================================================================
*
* FILE: MemoryMappedChunkHandler.java
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
package com.reactivetechnologies.platform.files.io;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.reactivetechnologies.platform.files.FileChunk;
import com.reactivetechnologies.platform.files.AbstractFileChunkHandler;
/**
 * Reads and writes using mapped byte buffer
 */
public class MemoryMappedChunkHandler extends AbstractFileChunkHandler {

  private FileChannel iStream;
  private FileChannel oStream;
  private static final Logger log = LoggerFactory.getLogger(MemoryMappedChunkHandler.class);
  /**
   * Write mode.
   * @throws IOException 
   */
  public MemoryMappedChunkHandler(String writeDir) throws IOException
  {
    super(writeDir);
    
  }
  /**
   * Read mode.
   * @param f
   * @param chunkSize
   * @throws IOException
   */
  public MemoryMappedChunkHandler(File f, int chunkSize) throws IOException {
    super(f);
    iStream = FileChannel.open(file.toPath(), StandardOpenOption.READ);
    readSize = chunkSize;
    chunks = fileSize % readSize == 0 ? (int) ((fileSize / readSize)) : (int) ((fileSize / readSize) + 1);
    mapBuff = iStream.map(MapMode.READ_ONLY, 0, getFileSize());
    if(log.isDebugEnabled())
    {
      debugInitialParams();
      log.debug("Reading source file. Expected chunks to send- "+chunks);
    }
  }
  
  private int chunks;
  @Override
  public void close() throws IOException {
    if (iStream != null) {
      iStream.force(true);
      iStream.close();
    }
    if(oStream != null){
      oStream.force(true);
      oStream.close();
    }
    if(mapBuff != null)
      unmap(mapBuff);
  }

  private int idx = 0, readSize;
  @Override
  public FileChunk readNext() throws IOException {
        
    if(mapBuff.hasRemaining())
    {
      log.debug("mapBuff.remaining() => "+mapBuff.remaining());
      byte[] read = new byte[mapBuff.remaining() > readSize ? readSize : mapBuff.remaining()];
      mapBuff.get(read);
      FileChunk chunk = new FileChunk(fileName, fileSize, creationTime, lastAccessTime, lastModifiedTime);
      chunk.setChunk(read);
      chunk.setOffset(idx++);
      chunk.setSize(chunks);
      log.debug("[readNext] "+chunk);
      return chunk;
    }
    
    return null;
  }
  private MappedByteBuffer mapBuff;
  private int position = 0;
  @Override
  public void writeNext(FileChunk chunk) throws IOException {
    log.debug("[writeNext] "+chunk);
    if(file == null)
    {
      initWriteFile(chunk);
      oStream = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
      
      /*
       * From javadocs:
       * "The behavior of this method when the requested region is not completely contained within this channel's file is unspecified. 
       * Whether changes made to the content or size of the underlying file, by this program or another, are propagated to the buffer 
       * is unspecified. The rate at which changes to the buffer are propagated to the file is unspecified."
       * 
       * Initially this is a 0 byte file. So how do we write to a new file??
       */
      log.debug("mapping byte buffer for write");
      mapBuff = oStream.map(MapMode.READ_WRITE, 0, chunk.getFileSize());
      
      if(log.isDebugEnabled())
      {
        debugInitialParams();
        log.debug("Writing to target file- "+file+". Expecting chunks to receive- "+chunk.getSize());
      }
    }
    
    
    doAttribCheck(chunk);
    
    
    //this is probably unreachable
    if(!mapBuff.hasRemaining())
    {
      position += mapBuff.position();
      unmap(mapBuff);
      mapBuff = oStream.map(MapMode.READ_WRITE, position, chunk.getFileSize());
    }
    
    mapBuff.put(chunk.getChunk());
    fileSize += chunk.getChunk().length;
    
    if(fileSize > chunk.getFileSize())
      throw new IOException("File size ["+fileSize+"] greater than expected size ["+chunk.getFileSize()+"]");
        
  }
    
  private static boolean unmap(MappedByteBuffer bb)
  {
    /*
     * From  sun.nio.ch.FileChannelImpl
     * private static void  unmap(MappedByteBuffer bb) {
          
           Cleaner cl = ((DirectBuffer)bb).cleaner();
           if (cl != null)
               cl.clean();


       }
     */
    try 
    {
      Method cleaner_method = ReflectionUtils.findMethod(bb.getClass(), "cleaner");
      if (cleaner_method != null) {
        cleaner_method.setAccessible(true);
        Object cleaner = cleaner_method.invoke(bb);
        if (cleaner != null) {
          Method clean_method = ReflectionUtils.findMethod(cleaner.getClass(), "clean");
          clean_method.setAccessible(true);
          clean_method.invoke(cleaner);
          return true;
        } 
      }

    } catch (Exception ex) {log.debug("", ex);}
    return false;
  }

}
