package io.anserini.collection;

import org.junit.Test;

import javax.xml.transform.Source;

import static org.junit.Assert.*;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Iterator;
import java.io.IOException;
import java.util.NoSuchElementException;

public class NewYorkTimesCollectionTest {

//  @Test
//  public void test() throws Exception {
//    Path collectionPath = Paths.get("C:/cygwin64/home/Emily/usra/collection/nyt_corpus_sample/data");
//    NewYorkTimesCollection testCollection = new NewYorkTimesCollection();
//    testCollection.setCollectionPath(collectionPath);
//    Boolean first = true;
//    for (FileSegment<NewYorkTimesCollection.Document> segment : testCollection){
//      System.out.println(segment.path);
//      if (first) {
//        for (SourceDocument doc : segment){
//          System.out.println(doc.id());
//          System.out.println(doc.content());
//        }
//        first = false;
//      }
//    }
//  }
//
//  @Test
//  public void test2() throws Exception {
//    Path collectionPath = Paths.get("C:/cygwin64/home/Emily/usra/collection/nyt_corpus_sample/data");
//    DocumentCollection testCollection = new NewYorkTimesCollection();
//    testCollection.setCollectionPath(collectionPath);
//    Boolean first = true;
//    for (Object s : testCollection){
//      FileSegment segment = (FileSegment) s; // cast here
//      System.out.println(segment.path);
//      if (first) {
//        for (Object d : segment){
//          SourceDocument doc = (SourceDocument) d; // cast here
//          System.out.println(doc.id());
//          System.out.println(doc.content());
//        }
//        first = false;
//      }
//    }
//  }

  public class testIterable implements Iterable<Integer> {

    @Override
    public final Iterator<Integer> iterator() {

      return new Iterator<Integer>() {
        Integer count = 10;

        @Override
        public Integer next() {
          if (count % 2 == 0) {
            throw new RuntimeException("Even, record skipped...");
          } else if (count == 0) {
            throw new NoSuchElementException("EOF has been reached. No more documents to read.");
          }
          count = count - 1;
          return count + 1;
        }

        @Override
        public boolean hasNext() {
          try {
            return count > -1;
          } catch (RuntimeException e1){
            System.out.println("Even, record skipped...");
            return true;
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  @Test
  public void test3() throws Exception {
    Iterable<Integer> iterable = new testIterable();
      for (Integer x : iterable) {
        System.out.println(x);
      }

  }


}