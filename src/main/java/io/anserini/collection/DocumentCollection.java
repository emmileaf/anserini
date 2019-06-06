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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// // Option 1
public abstract class DocumentCollection<T extends SourceDocument> implements Iterable<FileSegment<T>> {

  private static final Logger LOG = LogManager.getLogger(DocumentCollection.class);

  protected Path collectionPath;
  protected Set<String> skippedFilePrefix = new HashSet<>();
  protected Set<String> allowedFilePrefix = new HashSet<>();
  protected Set<String> skippedFileSuffix = new HashSet<>();
  protected Set<String> allowedFileSuffix = new HashSet<>();
  protected Set<String> skippedDir = new HashSet<>();

  public DocumentCollection(Path collectionPath) {
    this.collectionPath = collectionPath;
  }

  public final Path getCollectionPath() {
    return collectionPath;
  }

  @Override
  public final Iterator<FileSegment<T>> iterator(){

    List<Path> paths = discover(this.path);
    Iterator<Path> pathsIterator = paths.iterator();

    Iterator<FileSegment<T>> iter = new Iterator<FileSegment<T>>(){

        @Override
        public boolean hasNext(){
            return pathsIterator.hasNext();
        }

        @Override
        public FileSegment<T> next(){
            Path segmentPath = pathsIterator.next();
            return FileSegment(segmentPath);
        }

        @Override
        public void remove(){
            throw new UnsupportedOperationException();
        }
    }
    return iter;
  }

  protected final List<Path> discover(Path path) {

    final List<Path> paths = new ArrayList<>();

    FileVisitor<Path> fv = new CollectionFileVisitor<Path>();  

    try {
      Files.walkFileTree(p, fv);
    } catch (IOException e) {
      LOG.error("IOException during file visiting", e);
    }

    return paths;
  }

  protected class CollectionFileVisitor extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Path name = file.getFileName();
      boolean shouldAdd = true;
      if (Files.isSymbolicLink(file)) {
        name = Files.readSymbolicLink(file);
        if (Files.isDirectory(name)) {
          paths.addAll(discover(name));
          shouldAdd = false;
        }
      }
      if (name != null) {
        String fileName = name.toString();
        for (String s : DocumentCollection.this.skippedFileSuffix) {
          if (fileName.endsWith(s)) {
            shouldAdd = false;
            break;
          }
        }
        if (shouldAdd && !DocumentCollection.this.allowedFileSuffix.isEmpty()) {
          shouldAdd = false;
          for (String s : DocumentCollection.this.allowedFileSuffix) {
            if (fileName.endsWith(s)) {
              shouldAdd = true;
              break;
            }
          }
        }
        if (shouldAdd) {
          for (String s : DocumentCollection.this.skippedFilePrefix) {
            if (fileName.startsWith(s)) {
              shouldAdd = false;
              break;
            }
          }
        }
        if (shouldAdd && !DocumentCollection.this.allowedFilePrefix.isEmpty()) {
          shouldAdd = false;
          for (String s : DocumentCollection.this.allowedFilePrefix) {
            if (fileName.startsWith(s)) {
              shouldAdd = true;
              break;
            }
          }
        }
      }
      if (shouldAdd) {
        paths.add(file);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      if (DocumentCollection.this.skippedDir.contains(dir.getFileName().toString())) {
        LOG.info("Skipping: " + dir);
        return FileVisitResult.SKIP_SUBTREE;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException ioe) {
      LOG.error("Visiting failed for " + file.toString(), ioe);
      return FileVisitResult.SKIP_SUBTREE;
    }
  }
}

// // Option 2
// public interface DocumentCollection<T extends SourceDocument> extends Iterable<FileSegment<T>> {
  
//   public void setCollectionPath(Path path);

//   public Path getCollectionPath();

//   public Iterator<FileSegment<T>> iterator();

//   public interface CollectionIterator<T extends SourceDocument> extends Iterator<FileSegment<T>> {
//   }

//   public static final Set<String> EMPTY_SET = new HashSet<>();

//   public default List<Path> discover(Path p, 
//                                       Set<String> skippedFilePrefix, 
//                                       Set<String> allowedFilePrefix,
//                                       Set<String> skippedFileSuffix, 
//                                       Set<String> allowedFileSuffix, 
//                                       Set<String> skippedDir) {

//     final List<Path> paths = new ArrayList<>();

//     FileVisitor<Path> fv = new CollectionFileVisitor<Path>();  

//     try {
//       Files.walkFileTree(p, fv);
//     } catch (IOException e) {
//       LOG.error("IOException during file visiting", e);
//     }

//     return paths;
//   }

//   public static class CollectionFileVisitor extends SimpleFileVisitor<Path> {

//     @Override
//     public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//       Path name = file.getFileName();
//       boolean shouldAdd = true;
//       if (Files.isSymbolicLink(file)) {
//         name = Files.readSymbolicLink(file);
//         if (Files.isDirectory(name)) {
//           paths.addAll(discover(name, skippedFilePrefix, allowedFilePrefix, skippedFileSuffix,
//               allowedFileSuffix, skippedDir));
//           shouldAdd = false;
//         }
//       }
//       if (name != null) {
//         String fileName = name.toString();
//         for (String s : skippedFileSuffix) {
//           if (fileName.endsWith(s)) {
//             shouldAdd = false;
//             break;
//           }
//         }
//         if (shouldAdd && !allowedFileSuffix.isEmpty()) {
//           shouldAdd = false;
//           for (String s : allowedFileSuffix) {
//             if (fileName.endsWith(s)) {
//               shouldAdd = true;
//               break;
//             }
//           }
//         }
//         if (shouldAdd) {
//           for (String s : skippedFilePrefix) {
//             if (fileName.startsWith(s)) {
//               shouldAdd = false;
//               break;
//             }
//           }
//         }
//         if (shouldAdd && !allowedFilePrefix.isEmpty()) {
//           shouldAdd = false;
//           for (String s : allowedFilePrefix) {
//             if (fileName.startsWith(s)) {
//               shouldAdd = true;
//               break;
//             }
//           }
//         }
//       }
//       if (shouldAdd) {
//         paths.add(file);
//       }
//       return FileVisitResult.CONTINUE;
//     }

//     @Override
//     public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
//       if (skippedDir.contains(dir.getFileName().toString())) {
//         LOG.info("Skipping: " + dir);
//         return FileVisitResult.SKIP_SUBTREE;
//       }
//       return FileVisitResult.CONTINUE;
//     }

//     @Override
//     public FileVisitResult visitFileFailed(Path file, IOException ioe) {
//       LOG.error("Visiting failed for " + file.toString(), ioe);
//       return FileVisitResult.SKIP_SUBTREE;
//     }
//   }

// }




