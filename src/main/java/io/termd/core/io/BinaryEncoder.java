/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.io;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import io.termd.core.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryEncoder implements Consumer<int[]> {

  private volatile Charset charset;
  final Consumer<byte[]> onByte;

  public BinaryEncoder(Charset charset, Consumer<byte[]> onByte) {
    this.charset = charset;
    this.onByte = onByte;
  }

  /**
   * Set a new charset on the encoder.
   *
   * @param charset the new charset
   */
  public void setCharset(Charset charset) {
    this.charset = charset;
  }
  
  private static final Method METHOD_FLIP;
  static {
    Method method = null;
    try {
      method = CharBuffer.class.getDeclaredMethod("flip");
    } catch (NoSuchMethodException e1) {
      try {
        method = Buffer.class.getDeclaredMethod("flip");
      }catch(NoSuchMethodException e2) {
        throw new IllegalStateException(e2);
      }
    }
    METHOD_FLIP = method;
  }
  
  @Override
  public void accept(int[] codePoints) {
    final char[] tmp = new char[2];
    int capacity = 0;
    for (int codePoint : codePoints) {
      capacity += Character.charCount(codePoint);
    }
    CharBuffer charBuf = CharBuffer.allocate(capacity);
    for (int codePoint : codePoints) {
      int size = Character.toChars(codePoint, tmp, 0);
      charBuf.put(tmp, 0, size);
    }
    try {
      METHOD_FLIP.invoke(charBuf);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
    ByteBuffer bytesBuf = charset.encode(charBuf);
    byte[] bytes = bytesBuf.array();
    if (bytesBuf.limit() < bytesBuf.array().length) {
      bytes = Arrays.copyOf(bytes, bytesBuf.limit());
    }
    onByte.accept(bytes);
  }
}
