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

package io.termd.core.pty;

import io.termd.core.function.BiConsumer;
import io.termd.core.function.Consumer;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.util.Helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * todo : integrate with
 * - https://github.com/traff/pty4j
 * - https://github.com/jawi/JPty
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class PtyMaster extends Thread {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private int bufferSize = 512;
  private final String line;
  private BiConsumer<Status, Status> changeHandler;
  private final Consumer<Void> doneHandler;
  private final Consumer<int[]> stdout;
  private Status status;
  private Process process;
  private boolean interrupted;

  public PtyMaster(String line, Consumer<int[]> stdout, Consumer<Void> doneHandler) {
    this.line = line;
    this.doneHandler = doneHandler;
    this.stdout = stdout;
    this.status = Status.NEW;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    if (bufferSize < 2) {
      throw new IllegalStateException("Buffer size is too small");
    }
    this.bufferSize = bufferSize;
  }

  public BiConsumer<Status, Status> getChangeHandler() {
    return changeHandler;
  }

  public void setChangeHandler(BiConsumer<Status, Status> changeHandler) {
    this.changeHandler = changeHandler;
  }

  private class Pipe extends Thread {

    private final Charset charset = UTF_8; // We suppose the process out/err uses UTF-8
    private final InputStream in;
    private final BinaryDecoder decoder = new BinaryDecoder(bufferSize, charset, new Consumer<int[]>() {
      @Override
      public void accept(int[] ints) {
        stdout.accept(ints);
      }
    });

    public Pipe(InputStream in) {
      this.in = in;
    }

    @Override
    public void run() {
      byte[] buffer = new byte[512];
      while (true) {
        try {
          int l = in.read(buffer);
          if (l == -1) {
            break;
          }
          decoder.write(buffer, 0, l);
        } catch (IOException e) {
          e.printStackTrace();
          break; //break endless loop. "IOException: Stream closed" can be thrown when process is destroyed https://bugs.openjdk.java.net/browse/JDK-5101298
        }
      }
    }
  }

  public Process getProcess() {
    return process;
  }

  public Status getStatus() {
    return status;
  }

  public void interruptProcess() {
    if (!interrupted) {
      interrupted = true;
      process.destroy();
    }
  }

  @Override
  public void run() {
    ProcessBuilder builder = new ProcessBuilder(line.split("\\s+"));
    try {
      process = builder.start();
      setStatus(Status.RUNNING);
      Pipe stdout = new Pipe(process.getInputStream());
      Pipe stderr = new Pipe(process.getErrorStream());
      stdout.start();
      stderr.start();
      try {
        int exitValue = process.waitFor();
        if (exitValue == 0) {
          setStatus(Status.COMPLETED);
        } else {
          setStatus(Status.FAILED);
        }
      } catch (InterruptedException e) {
        setStatus(Status.INTERRUPTED);
        Thread.currentThread().interrupt();
      }
      try {
        stdout.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      try {
        stderr.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } catch (IOException e) {
      stdout.accept(Helper.toCodePoints(e.getMessage() + "\r\n"));
    }

    //
    doneHandler.accept(null);
  }

  private void setStatus(Status next) {
    Status prev = status;
    status = next;
    if (changeHandler != null) {
      changeHandler.accept(prev, next);
    }
  }
}
