/* ============================================================================
*
* FILE: FileChunk.java
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

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class FileChunk implements DataSerializable {

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  public long getLastAccessTime() {
    return lastAccessTime;
  }

  public void setLastAccessTime(long lastAccessTime) {
    this.lastAccessTime = lastAccessTime;
  }

  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  @Override
  public String toString() {
    return "FileChunk [fileName=" + fileName + ", fileSize=" + fileSize
        + ", creationTime=" + creationTime + ", lastAccessTime="
        + lastAccessTime + ", lastModifiedTime=" + lastModifiedTime + ", size="
        + size + ", offset=" + offset + ", chunkLength=" + chunk.length
        + "]";
  }

  private String fileName;
  private long fileSize, creationTime, lastAccessTime, lastModifiedTime;
  
  /**
   * No of chunks
   */
  private int size;
  /**
   * Index of chunk (0 based)
   */
  private int offset;
  private byte[] chunk;
  
  
  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public byte[] getChunk() {
    return chunk;
  }

  public void setChunk(byte[] chunk) {
    this.chunk = chunk;
  }

  /**
   * Default constructor
   */
  public FileChunk() {
    super();
  }

  /**
   * 
   * @param fileName
   * @param fileSize
   * @param creationTime
   * @param lastAccessTime
   * @param lastModifiedTime
   */
  public FileChunk(String fileName, long fileSize, long creationTime,
      long lastAccessTime, long lastModifiedTime) {
    super();
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.creationTime = creationTime;
    this.lastAccessTime = lastAccessTime;
    this.lastModifiedTime = lastModifiedTime;
  }

  @Override
  public void writeData(ObjectDataOutput out) throws IOException {
    out.writeUTF(getFileName());
    out.writeLong(getFileSize());
    out.writeLong(getCreationTime());
    out.writeLong(getLastAccessTime());
    out.writeLong(getLastModifiedTime());
    out.writeInt(getSize());
    out.writeInt(getOffset());
    out.writeByteArray(getChunk());

  }

  @Override
  public void readData(ObjectDataInput in) throws IOException {
    setFileName(in.readUTF());
    setFileSize(in.readLong());
    setCreationTime(in.readLong());
    setLastAccessTime(in.readLong());
    setLastModifiedTime(in.readLong());
    setSize(in.readInt());
    setOffset(in.readInt());
    setChunk(in.readByteArray());

  }

}
