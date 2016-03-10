/* ============================================================================
*
* FILE: FileChunkReader.java
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public abstract class FileChunkHandler implements Closeable {

  /**
   * Checks file attributes
   * @param chunk
   * @throws IOException
   */
  protected void doAttribCheck(FileChunk chunk) throws IOException
  {
    if(!fileName.equals(chunk.getFileName()))
      throw new IOException("Got chunk file name ["+chunk.getFileName()+"] rather than expected ["+fileName+"]");
    
    if(lastModifiedTime != chunk.getLastModifiedTime())
      throw new IOException("Got chunk lastModifiedTime ["+chunk.getLastModifiedTime()+"] rather than expected ["+lastModifiedTime+"]");
    
    if(lastAccessTime != chunk.getLastAccessTime())
      throw new IOException("Got chunk lastAccessTime ["+chunk.getLastAccessTime()+"] rather than expected ["+lastAccessTime+"]");
    
    if(creationTime != chunk.getCreationTime())
      throw new IOException("Got chunk creationTime ["+chunk.getCreationTime()+"] rather than expected ["+creationTime+"]");
    
    if(chunk.getOffset() >= chunk.getSize())
      throw new IOException("Got chunk offset ["+chunk.getOffset()+"] greater than expected size ["+chunk.getSize()+"]");
  }
  /**
   * Creates the new file for writing.
   * @param chunk
   * @throws IOException
   */
  protected void initWriteFile(FileChunk chunk) throws IOException
  {
    file = new File(dir, chunk.getFileName());
    fileName = chunk.getFileName();
    BasicFileAttributeView attrView = Files
        .getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
    
    attrView.setTimes(FileTime.fromMillis(chunk.getLastModifiedTime()),
        FileTime.fromMillis(chunk.getLastAccessTime()),
        FileTime.fromMillis(chunk.getCreationTime()));
    
    lastModifiedTime = chunk.getLastModifiedTime();
    lastAccessTime = chunk.getLastAccessTime();
    creationTime = chunk.getCreationTime();
  }
  
  protected File dir;
  protected File file;
  protected String fileName;
  protected long fileSize = 0, creationTime, lastAccessTime, lastModifiedTime;
  /**
   * Write mode.
   * @param writeDir
   * @throws IOException
   */
  public FileChunkHandler(String writeDir) throws IOException{
    dir = new File(writeDir);
    if(!dir.exists())
      dir.mkdirs();
    
    if(!dir.exists() || !dir.isDirectory())
      throw new IOException("Not a valid directory");
  }
  /**
   * Read mode.
   * @param f
   * @throws IOException
   */
  public FileChunkHandler(File f) throws IOException {
    super();
    this.file = f;
    if(!file.exists() || !file.isFile())
      throw new IOException("Not a valid file");
    
    
    fileName = file.getName();
    fileSize = file.length();
    
    Path filePath = file.toPath();
    try 
    {
      if(Files.getFileStore(filePath).supportsFileAttributeView(BasicFileAttributeView.class))
      {
        BasicFileAttributes attribs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        if(!attribs.isRegularFile())
          throw new IOException("Not a regular file");
        
        creationTime = attribs.creationTime().toMillis();
        lastAccessTime = attribs.lastAccessTime().toMillis();
        lastModifiedTime = attribs.lastModifiedTime().toMillis();
        
      }
      else
        throw new IOException("Unable to read basic file attributes");
      
    } catch (IOException e1) {
      throw e1;
    }
    
    
    
  }
  /**
   * Reads the next chunk available or returns null if EOF encountered
   * @return
   * @throws IOException
   */
  public abstract FileChunk readNext() throws IOException;
  /**
   * Writes next chunk of file to the underlying stream
   * @param chunk
   * @throws IOException
   */
  public abstract void writeNext(FileChunk chunk) throws IOException;
  public File getFile() {
    return file;
  }

  public String getFileName() {
    return fileName;
  }

  public long getFileSize() {
    return fileSize;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public long getLastAccessTime() {
    return lastAccessTime;
  }

  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

}
