/* ============================================================================
*
* FILE: AsciiFileChunk.java
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.reactivetechnologies.platform.files.FileChunk;
import com.reactivetechnologies.platform.utils.Digestor;

public class AsciiFileChunk extends FileChunk implements Serializable{

  @Override
  public String toString() {
    return "AsciiFileChunk [recordIndex=" + recordIndex + ", getFileName()="
        + getFileName() + ", getFileSize()=" + getFileSize() + ", noOfChunks="
        + getSize() + ", getOffset=" + getOffset() + "]";
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  /*
   *  A line is considered to be terminated by any one of a line feed ('\n'), a carriage return ('\r'), 
   *  or a carriage return followed immediately by a line feed.
   */
  final static byte CARRIAGE_RETURN = 0xD;
  final static byte LINE_FEED = 0xA;
  /**
   * Default cons.
   */
  public AsciiFileChunk() {
    super();
  }
  /**
   * Copy constructor.
   * @param copyFrom
   */
  AsciiFileChunk(AsciiFileChunk copyFrom)
  {
    super(copyFrom);
    setChunk(copyFrom.getChunk());
    setOffset(copyFrom.getOffset());
    setSize(copyFrom.getSize());
    setSplitType(copyFrom.getSplitType());
    this.recordIndex = copyFrom.recordIndex;
    this.recordOffset = copyFrom.recordOffset;
  }
  /**
   * 
   * @param fileName
   * @param fileSize
   * @param creationTime
   * @param lastAccessTime
   * @param lastModifiedTime
   */
  public AsciiFileChunk(String fileName, long fileSize, long creationTime,
      long lastAccessTime, long lastModifiedTime) {
    super(fileName, fileSize, creationTime, lastAccessTime, lastModifiedTime);
  }
  /**
   * If line break is present in this chunk.
   * @return
   */
  boolean isHasLineBreak() {
    if (!checkLineBreakDone) {
      checkLineBreak();
    }
    return !splitBytes.isEmpty();
  }
  private int recordOffset = 0;
  /**
   * Splits this chunk into chunks with the previous record at [0] and next record at [1].. and so on. The next chunk
   * to be submitted should be created with the next record.
   * <pre>
   * Algorithm
   * -----------
   * lastChunk = Fetch next chunk from IO reader with lastChunk;
   * if(lastChunk.isHasLineBreak)
   *  splits[] = lastChunk.splitChunkOnLineBreak;
   *  submit(split[0]);
   *  submit(split[1]);
   *  lastChunk = split[1];
   * end if;
   * </pre>
   * @return splitted chunks or null.
   */
  public List<AsciiFileChunk> getSplitChunks()
  {
    return splitBytes;
  }
  /**
   * Record index for this chunk.
   * @return
   */
  public int getRecordIndex() {
    return recordIndex;
  }

  //private int recordOffset = 0;
  private List<AsciiFileChunk> splitBytes = new ArrayList<>();
  /**
   * 
   * @param unicodeBytes
   * @return
   */
  private void splitBytesAtLineBreak()
  {
      final byte[] unicodeBytes = getChunk();
      byte[] oneSplit;
      int len = unicodeBytes.length;
      int offset = 0; 
      
      int i=0;
      AsciiFileChunk split = null;
      boolean firstSplit = true;
      for (int pos = 0; pos < len; pos++)
      {
          if(pos < len - 1 && unicodeBytes[pos] == CARRIAGE_RETURN && unicodeBytes[pos+1] == LINE_FEED)
          {
            oneSplit = Arrays.copyOfRange(unicodeBytes, offset, pos);
            pos++;
            
            offset = pos +1;
            if (oneSplit != null && oneSplit.length > 0) {
              
              split = addSplitChunk(oneSplit, i++, split);
            }
                        
          }
          else if(unicodeBytes[pos] == LINE_FEED || unicodeBytes[pos] == CARRIAGE_RETURN)
          {
            oneSplit = Arrays.copyOfRange(unicodeBytes, offset, pos);
            
            offset = pos +1;
            if (oneSplit != null && oneSplit.length > 0) {
              
              split = addSplitChunk(oneSplit, i++, split);
            }
            
          }
          else
            split = null;
          
          if(split != null)
          {
            if(firstSplit)
            {
              split.setSplitType(SPLIT_TYPE_POST);
              firstSplit = false;
              split.recordOffset++;
            }
            else
              split.setSplitType(SPLIT_TYPE_FULL);
            
          }
                                
      }
      if(split != null)
      {
        split.setSplitType(SPLIT_TYPE_PRE);
      }
      
      if(splitBytes.isEmpty())
      {
        //no line break. increment record offset
        recordOffset++;
      }
  }
  
  private byte splitType = -1;
  /**
   * 
   * @param oneSplit
   * @param type
   * @param i
   * @return 
   */
  private AsciiFileChunk addSplitChunk(final byte[] oneSplit, final int i, AsciiFileChunk copyFrom)
  {
    AsciiFileChunk chunk = new AsciiFileChunk(copyFrom == null ? this : copyFrom);
    chunk.setChunk(oneSplit);
    
    chunk.setSize(chunk.getSize()+i);
    chunk.setOffset(chunk.getOffset()+i);
    chunk.recordIndex++;
    splitBytes.add(chunk);
    return chunk;
  }
  private int recordIndex = 0;
  /**
   * Generate a consistent hash for a record number. This should enable distributing the record to the same node.
   * @return
   */
  public long generateHashCode()
  {
    
    return Digestor.murmur64().addString(getFileName()).addLong(getFileSize())
        .addLong(getCreationTime()).addLong(getRecordIndex()).toMurmurHash();
    
    
  }
  private volatile boolean checkLineBreakDone;
  /**
   * 
   */
  private void checkLineBreak()
  {
    splitBytesAtLineBreak();
    checkLineBreakDone = true;
  }
    
  @Override
  public void writeData(ObjectDataOutput out) throws IOException {
    super.writeData(out);
    out.writeInt(recordIndex);
    out.writeInt(recordOffset);
    out.writeByte(getSplitType());
  }

  @Override
  public void readData(ObjectDataInput in) throws IOException {
    super.readData(in);
    recordIndex = in.readInt();
    recordOffset = in.readInt();
    setSplitType(in.readByte());
  }
  
  /**
   * The type of split at line break.
   * -1 => prefix
   * 0 => full
   * 1 => postfix
   * <p>
   * Say a chunk is like:-
   * <pre>
   * was waiting.'\n' The good boy. '\n' The next 
   * </pre>
   * <p>
   * So the split chunks will be typed as above.
   */
  public byte getSplitType() {
    return splitType;
  }
  private void setSplitType(byte splitType) {
    this.splitType = splitType;
  }

  public int getRecordOffset() {
    return recordOffset;
  }
  
  static final byte SPLIT_TYPE_PRE = 1;
  static final byte SPLIT_TYPE_FULL = 2;
  static final byte SPLIT_TYPE_POST = 3;
}
