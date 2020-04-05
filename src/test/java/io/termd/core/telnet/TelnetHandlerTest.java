/*
 * Copyright 2014 Julien Viet
 *
 * Julien Viet licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package io.termd.core.telnet;

import io.termd.core.function.BiConsumer;
import io.termd.core.function.Supplier;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * See <a href="http://commons.apache.org/proper/commons-net/examples/telnet/TelnetClientExample.java>for more possibilities</a>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetHandlerTest extends TelnetTestBase {

  private void testOptionValue(Supplier<TelnetHandler> factory, TelnetOptionHandler optionHandler) throws Exception {
    server.start(factory);
    client.setOptionHandler(optionHandler);
    client.connect("localhost", 4000);
    await();
  }

  @Test
  public void testRejectEcho() throws Exception {
    final AtomicReference<Boolean> serverValue = new AtomicReference<Boolean>();
    EchoOptionHandler optionHandler = new EchoOptionHandler(false, false, false, false);
    testOptionValue(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.writeWillOption(Option.ECHO);
          }
          @Override
          protected void onEcho(boolean echo) {
            serverValue.set(echo);
            testComplete();
          }
        };
      }
    }, optionHandler);
    assertEquals(false, serverValue.get());
    assertEquals(false, optionHandler.getAcceptRemote());
  }

  @Test
  public void testAcceptEcho() throws Exception {
    final AtomicReference<Boolean> serverValue = new AtomicReference<Boolean>();
    EchoOptionHandler optionHandler = new EchoOptionHandler(false, false, false, true);
    testOptionValue(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.writeWillOption(Option.ECHO);
          }
          @Override
          protected void onEcho(boolean echo) {
            serverValue.set(echo);
            testComplete();
          }
        };
      }
    }, optionHandler);
    assertEquals(true, serverValue.get());
    assertEquals(true, optionHandler.getAcceptRemote());
  }

  @Test
  public void testRejectSGA() throws Exception {
    final AtomicReference<Boolean> serverValue = new AtomicReference<Boolean>();
    SuppressGAOptionHandler optionHandler = new SuppressGAOptionHandler(false, false, false, false);
    testOptionValue(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.writeWillOption(Option.SGA);
          }

          @Override
          protected void onSGA(boolean sga) {
            serverValue.set(sga);
            testComplete();
          }
        };
      }
    }, optionHandler);
    assertEquals(false, serverValue.get());
    assertEquals(false, optionHandler.getAcceptRemote());
  }

  @Test
  public void testAcceptSGA() throws Exception {
    final AtomicReference<Boolean> serverValue = new AtomicReference<Boolean>();
    SuppressGAOptionHandler optionHandler = new SuppressGAOptionHandler(false, false, false, true);
    testOptionValue(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.writeWillOption(Option.SGA);
          }

          @Override
          protected void onSGA(boolean sga) {
            serverValue.set(sga);
            testComplete();
          }
        };
      }
    }, optionHandler);
    assertEquals(true, serverValue.get());
    assertEquals(true, optionHandler.getAcceptRemote());
  }

  @Test
  public void testRejectNAWS() throws Exception {
    final AtomicReference<Boolean> serverValue = new AtomicReference<Boolean>();
    WindowSizeOptionHandler optionHandler = new WindowSizeOptionHandler(20, 10, false, false, false, false);
    testOptionValue(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.writeDoOption(Option.NAWS);
          }
          @Override
          protected void onNAWS(boolean naws) {
            serverValue.set(naws);
            testComplete();
          }
          @Override
          protected void onSize(int width, int height) {
            super.onSize(width, height);
          }
        };
      }
    }, optionHandler);
    assertEquals(false, serverValue.get());
    assertEquals(false, optionHandler.getAcceptLocal());
  }

  @Test
  public void testAcceptNAWS() throws Exception {
    final AtomicReference<Boolean> serverValue = new AtomicReference<Boolean>();
    final AtomicReference<int[]> size = new AtomicReference<int[]>();
    WindowSizeOptionHandler optionHandler = new WindowSizeOptionHandler(20, 10, false, false, true, false);
    testOptionValue(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.writeDoOption(Option.NAWS);
          }
          @Override
          protected void onNAWS(boolean naws) {
            serverValue.set(naws);
          }
          @Override
          protected void onSize(int width, int height) {
            size.set(new int[]{width, height});
            testComplete();
          }
        };
      }
    }, optionHandler);
    assertEquals(true, serverValue.get());
    assertEquals(true, optionHandler.getAcceptLocal());
    assertEquals(2, size.get().length);
    assertEquals(20, size.get()[0]);
    assertEquals(10, size.get()[1]);
  }

  @Test
  public void testOpen() throws Exception {
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            testComplete();
          }
        };
      }
    });
    client.connect("localhost", 4000);
    await();
  }

  @Test
  public void testClientDisconnect() throws Exception {
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onClose() {
            testComplete();
          }
        };
      }
    });
    try {
      client.connect("localhost", 4000);
    } finally {
      client.disconnect();
    }
    await();
  }

  @Test
  public void testServerClose() throws Exception {
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.close();
          }
          @Override
          protected void onClose() {
            testComplete();
          }
        };
      }
    });
    client.connect("localhost", 4000);
    await();
  }

  @Test
  public void testSend() throws Exception {
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.write(new byte[]{0, 1, 2, 3, 127, (byte) 0x80, (byte) 0x81, 127});
          }
        };
      }
    });
    client.connect("localhost", 4000);
    byte[] data = client.assertReadBytes(8);
    assertEquals((byte)0, data[0]);
    assertEquals((byte)1, data[1]);
    assertEquals((byte)2, data[2]);
    assertEquals((byte)3, data[3]);
    assertEquals((byte)127, data[4]);
    assertEquals((byte) 0x80, data[5]);
    assertEquals((byte) 0x81, data[6]);
    assertEquals((byte)127, data[7]);
  }

  @Test
  public void testReceive() throws Exception {
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onData(byte[] data) {
            assertEquals(7, data.length);
            assertEquals((byte) 0, data[0]);
            assertEquals((byte) 1, data[1]);
            assertEquals((byte) 2, data[2]);
            assertEquals((byte) 3, data[3]);
            assertEquals((byte) 127, data[4]);
            assertEquals((byte) 0x80, data[5]);
            assertEquals((byte) 0x81, data[6]);
            testComplete();
          }
        };
      }
    });
    client.connect("localhost", 4000).write(new byte[]{0, 1, 2, 3, 127, (byte) 0x80, (byte) 0x81}).flush();
    await();
  }

  @Test
  public void testWillUnknownOption() throws Exception {
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler();
      }
    });
    client.registerNotifHandler(new BiConsumer<Integer, Integer>() {
      @Override
      public void accept(Integer negotiation_code, Integer option_code) {
        if (option_code == 47) {
          assertEquals(TelnetNotificationHandler.RECEIVED_DONT, negotiation_code);
          testComplete();
        }
      }
    });
    client.setOptionHandler(new SimpleOptionHandler(47, true, false, false, false));
    client.connect("localhost", 4000);
    await();
  }

  @Test
  public void testDoUnknownOption() throws Exception {
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler();
      }
    });
    client.registerNotifHandler(new BiConsumer<Integer, Integer>() {
      @Override
      public void accept(Integer negotiation_code, Integer option_code) {
        if (option_code == 47) {
          assertEquals(TelnetNotificationHandler.RECEIVED_WONT, negotiation_code);
          testComplete();
        }
      }
    });
    client.setOptionHandler(new SimpleOptionHandler(47, false, true, false, false));
    client.connect("localhost", 4000);
    await();
  }

  @Test
  public void testReceiveBinary() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            conn.writeDoOption(Option.BINARY);
          }

          @Override
          protected void onSendBinary(boolean binary) {
            if (binary) {
              fail("Was not expecting a do for binary option");
            }
          }

          @Override
          protected void onReceiveBinary(boolean binary) {
            if (binary) {
              latch.countDown();
            } else {
              fail("Was not expecting a won't for binary option");
            }
          }

          @Override
          protected void onData(byte[] data) {
            assertEquals(1, data.length);
            assertEquals((byte) -1, data[0]);
            testComplete();
          }
        };
      }
    });

    client.setOptionHandler(new SimpleOptionHandler(0, false, false, true, false));
    client.connect("localhost", 4000);
    latch.await();
    client.writeDirectAndFlush((byte)-1, (byte)-1);
    await();
  }

  @Test
  public void testSendBinary() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    server.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetHandler() {
          private TelnetConnection conn;

          @Override
          protected void onOpen(TelnetConnection conn) {
            this.conn = conn;
            conn.writeWillOption(Option.BINARY);
          }

          @Override
          protected void onSendBinary(boolean binary) {
            if (binary) {
              conn.write(new byte[]{'h', 'e', 'l', 'l', 'o', -1});
              latch.countDown();
            } else {
              fail("Was not expecting a don't for binary option");
            }
          }

          @Override
          protected void onReceiveBinary(boolean binary) {
            if (binary) {
              fail("Was not expecting a will for binary option");
            }
          }
        };
      }
    });
    client.setOptionHandler(new SimpleOptionHandler(0, false, false, false, true));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    client.client.registerSpyStream(baos);
    client.connect("localhost", 4000);
    latch.await();
    Reader reader = new InputStreamReader(client.client.getInputStream());
    int expectedLen = 5;
    char[] hello = new char[expectedLen];
    int num = 0;
    while (num < expectedLen) {
      int read = reader.read(hello, num, expectedLen - num);
      if (read == -1) {
        fail("Unexpected");
      }
      num += read;
    }
    assertEquals(5, num);
    assertEquals("hello", new String(hello));
    byte[] data = baos.toByteArray();
    assertEquals(10, data.length);
    assertEquals((byte)'h', data[3]);
    assertEquals((byte)'e', data[4]);
    assertEquals((byte)'l', data[5]);
    assertEquals((byte)'l', data[6]);
    assertEquals((byte)'o', data[7]);
    assertEquals((byte)-1, data[8]);
    assertEquals((byte)-1, data[9]);
  }
}
