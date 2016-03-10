/* ============================================================================
*
* FILE: FileChannelChunkReader.java
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
package com.reactivetechnologies.platform.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 * Reads from a buffered stream
 */
public class BasicFileChunkHandler extends FileChunkHandler {

  private InputStream iStream;
  private OutputStream oStream;
  
  /**
   * Write mode.
   * @throws IOException 
   */
  public BasicFileChunkHandler(String writeDir) throws IOException
  {
    super(writeDir);
    
  }
  /**
   * Read mode.
   * @param f
   * @param chunkSize
   * @throws IOException
   */
  public BasicFileChunkHandler(File f, int chunkSize) throws IOException {
    super(f);
    iStream = new BufferedInputStream(new FileInputStream(file));
    size = chunkSize;
    chunks = (int) ((fileSize % size) + 1);
  }

  private int chunks;
  @Override
  public void close() throws IOException {
    if (iStream != null) {
      iStream.close();
    }
    if(oStream != null){
      oStream.close();
    }
  }

  private int idx = 0, size;
  @Override
  public FileChunk readNext() throws IOException {
    
    byte[] read = new byte[size];
    int available = iStream.read(read);
    if(available != -1)
    {
      FileChunk chunk = new FileChunk(fileName, fileSize, creationTime, lastAccessTime, lastModifiedTime);
      chunk.setChunk(read);
      chunk.setOffset(idx++);
      chunk.setSize(chunks);
      return chunk;
    }
    
    return null;
  }
  
  @Override
  public void writeNext(FileChunk chunk) throws IOException {
    if(file == null)
    {
      initWriteFile(chunk);
      oStream = new BufferedOutputStream(new FileOutputStream(file));

    }
    
    doAttribCheck(chunk);
    
    oStream.write(chunk.getChunk());
    fileSize += chunk.getChunk().length;
    
    if(fileSize > chunk.getFileSize())
      throw new IOException("File size ["+fileSize+"] greater than expected size ["+chunk.getFileSize()+"]");
    
    oStream.flush();
  }
  
  

}
