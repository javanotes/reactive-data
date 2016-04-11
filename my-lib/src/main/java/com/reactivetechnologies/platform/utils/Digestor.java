/* ============================================================================
*
* FILE: Digestor.java
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
package com.reactivetechnologies.platform.utils;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.springframework.beans.BeanUtils;
import org.webbitserver.helpers.Base64;

import com.hazelcast.util.HashUtil;

public class Digestor {

  
  /**
   * Deep clone of an object having a default constructor.
   * @param o
   * @return
   * @throws IOException ignored silently
   * @throws ClassNotFoundException ignored silently
   */
  @SuppressWarnings("unchecked")
  public static <T> T deepCopy(Serializable o)
  {
    try 
    {
      T t = (T) o.getClass().newInstance();
      BeanUtils.copyProperties(o, t);
      
      return t;
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
    
  }
  private final MessageDigest md;
  private byte[] byteArray;
  /**
   * 
   * @param algo
   */
  private Digestor(String algo) {
    if("MM".equals(algo))
    {
      md = null;
    }
    else
    {
      try {
        md = MessageDigest.getInstance(algo);
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalArgumentException(e);
      }
    }
     
  }
  /**
   * 
   * @return
   */
  public static Digestor md5()
  {
    Digestor d = new Digestor("MD5");
    return d;
  }
  /**
   * 
   * @return
   */
  public static Digestor murmur64()
  {
    Digestor d = new Digestor("MM");
    return d;
  }
  /**
   * 
   * @return
   */
  public static Digestor sha256()
  {
    //SHA-256
    Digestor d = new Digestor("SHA-256");
    return d;
  }
  private void updateBytes(ByteBuffer buff)
  {
    int i = buff.position();
    buff.flip();
    byte[] b = new byte[i];
    buff.get(b);
    if(byteArray == null)
      byteArray = b;
    else
    {
      i = byteArray.length;
      byteArray = Arrays.copyOf(byteArray, i + b.length);
      System.arraycopy(b, 0, byteArray, i, b.length);
    }
    
  }
  /**
   * 
   * @param l
   * @return
   */
  public Digestor addLong(long l)
  {
    if(md != null)
      md.update(ByteBuffer.allocate(8).putLong(l));
    else
      updateBytes(ByteBuffer.allocate(8).putLong(l));
    return this;
  }
  /**
   * 
   * @param l
   * @return
   */
  public Digestor addInt(int l)
  {
    if(md != null)
      md.update(ByteBuffer.allocate(4).putInt(l));
    else
      updateBytes(ByteBuffer.allocate(4).putInt(l));
    return this;
  }
  /**
   * 
   * @param s
   * @return
   */
  public Digestor addString(String s)
  {
    if(md != null)
      md.update(ByteBuffer.wrap(s.getBytes()));
    else
      updateBytes(ByteBuffer.wrap(s.getBytes()));
    return this;
  }
  /**
   * 
   * @param s
   * @return
   */
  public Digestor addBytes(byte[] s)
  {
    if(md != null)
      md.update(s);
    else
      updateBytes(ByteBuffer.wrap(s));
    return this;
  }
  /**
   * To a 8 byte long value from a corresponding hex string
   * @return
   */
  public long toLong()
  {
    return toBig().longValue();
  }
  /**
   * To a 8 byte murmur hash.
   * @return
   */
  public long toMurmurHash()
  {
    byte[] bytes = getBytes();
    return HashUtil.MurmurHash3_x64_64(bytes, 0, bytes.length);
  }
  /**
   * To a Base64 encoded string.
   */
  public String toString()
  {
    return Base64.encode(getBytes());
    
  }
  private byte[] getBytes()
  {
    if(md != null)
      return md.digest();
    else
      return byteArray;
  }
  /**
   * To a hex string.
   * @return
   */
  public String toHexString()
  {
    char[] hex = encodeHex(getBytes());
    return new String(hex);
    
  }
  /**
   * To a numeric representation of the number. May be bigger than a 8 byte Long.
   * @return
   */
  public String toLongString()
  {
    return toBig().toString();
  }
  
  private BigInteger toBig()
  {
    return new BigInteger(toHexString(), 16);
  }
  
  //From Spring utilities
  private static final char[] HEX_CHARS =
    {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  private static char[] encodeHex(byte[] bytes) {
    char chars[] = new char[32];
    for (int i = 0; i < chars.length; i = i + 2) {
      byte b = bytes[i / 2];
      chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
      chars[i + 1] = HEX_CHARS[b & 0xf];
    }
    return chars;
  }
}
