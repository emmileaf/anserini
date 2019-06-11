/**
 * Anserini: A Lucene toolkit for replicable information retrieval research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.anserini.collection;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Iterator;

public abstract class FileSegment<T extends SourceDocument> implements Iterable<T> {

  public enum Status {
    SKIPPED, ERROR, VOID
  }

  protected Path path;
  protected final int BUFFER_SIZE = 1 << 16; // 64K
  protected BufferedReader bufferedReader;
  protected boolean atEOF = false;
  protected T bufferedRecord = null;
  protected Status nextRecordStatus = Status.VOID;

  // move exception handling for skipped docs to within segment
  // desired behaviour is to continue iteration and increment counter
  // getSkippedCount() at the end of iteration returns count of total
  // docs skipped per segment
  protected int skipped = 0;

  public FileSegment(Path segmentPath) {
    this.path = segmentPath;
  }

  // helpers for concrete classes to implement 
  // depending on desired iterator behaviour
  protected abstract void readNext() throws IOException;

  public final Status getNextRecordStatus() {
    return nextRecordStatus;
  }

  public final int getSkippedCount() {
    return skipped;
  }

  public final Path getSegmentPath() {
    return path;
  }

  public final void close() throws IOException {
    atEOF = true;
    bufferedRecord = null;
    nextRecordStatus = Status.VOID;
    if (bufferedReader != null) {
      bufferedReader.close();
    }
  }

  @Override
  public final Iterator<T> iterator(){

    return new Iterator<T>(){

      @Override
      public T next() throws NoSuchElementException {
        if (nextRecordStatus == Status.ERROR || bufferedRecord == null && !hasNext()) {
          nextRecordStatus = Status.VOID;
          throw new NoSuchElementException("EOF has been reached. No more documents to read.");
        }
        T ret = bufferedRecord;
        bufferedRecord = null;
        return ret;
      }

      @Override
      public boolean hasNext() {
        if (nextRecordStatus == Status.ERROR) {
          return false;
        }

        if (bufferedRecord != null) {
          return true;
        } else if (atEOF) {
          return false;
        }

        try {
          readNext();
        } catch (IOException e1) {
          // move this to be handled in readNext()
          // nextRecordStatus = Status.ERROR;
          return false;
        } catch (NoSuchElementException e2) {
          return false;
        } catch (RuntimeException e3) {
          nextRecordStatus = Status.VOID;
          skipped += 1;
          return hasNext();
        }

        return bufferedRecord != null;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}

