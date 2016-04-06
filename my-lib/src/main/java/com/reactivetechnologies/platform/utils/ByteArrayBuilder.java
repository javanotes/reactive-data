/* ============================================================================
*
* FILE: ByteArrayBuilder.java
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

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class ByteArrayBuilder {

  private static Unsafe UNSAFE;

  static

  {

    try {

      Field f = Unsafe.class.getDeclaredField("theUnsafe");

      f.setAccessible(true);

      UNSAFE = (Unsafe) f.get(null);

    } catch (Exception e) {

      throw new ExceptionInInitializerError(
          "sun.misc.Unsafe not instantiated [" + e.toString() + "]");

    }

  }

  public int getByteSize() {
    return byteSize;
  }

  public long getAddress() {
    return address;
  }

  private long address = -1;

  private int byteSize = 0;

  public byte[] toArray() {

    byte[] values = new byte[byteSize];

    UNSAFE.copyMemory(null, address,

        values, Unsafe.ARRAY_BYTE_BASE_OFFSET,

        byteSize);

    return values;

  }

  public ByteArrayBuilder()

  {

    appendFirst(new byte[0]);

  }

  public ByteArrayBuilder(byte[] bytes)

  {

    appendFirst(bytes);

  }

  public ByteArrayBuilder append(byte[] _bytes)

  {

    return appendNext(_bytes);

  }

  private ByteArrayBuilder appendFirst(byte[] _bytes)

  {

    address = UNSAFE.allocateMemory(byteSize + _bytes.length);

    UNSAFE.copyMemory(_bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address,
        _bytes.length);

    byteSize += _bytes.length;

    return this;

  }

  private ByteArrayBuilder appendNext(byte[] _bytes)

  {

    long _address2 = UNSAFE.allocateMemory(byteSize + _bytes.length);

    UNSAFE.copyMemory(address, _address2, byteSize);

    UNSAFE.copyMemory(_bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + byteSize, null,
        _address2, _bytes.length);

    UNSAFE.freeMemory(address);

    address = _address2;

    byteSize += _bytes.length;

    return this;

  }

  public void free() {

    UNSAFE.freeMemory(address);

  }

}
