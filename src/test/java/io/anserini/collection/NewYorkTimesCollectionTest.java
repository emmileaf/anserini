package io.anserini.collection;

import org.junit.Test;

import static org.junit.Assert.*;
import java.nio.file.Paths;
import java.nio.file.Path;

public class NewYorkTimesCollectionTest {

  @Test
  public void test() throws Exception {
    Path collectionPath = Paths.get("C:/cygwin64/home/Emily/usra/collection/nyt_corpus_sample/data");
    NewYorkTimesCollection testCollection = new NewYorkTimesCollection(collectionPath);
    Boolean first = true;
    for (FileSegment<NewYorkTimesCollection.Document> segment : testCollection){
      System.out.println(segment.path);
      if (first) {
        for (SourceDocument doc : segment){
          System.out.println(doc.id());
          System.out.println(doc.content());
        }
        first = false;
      }
    }
  }


}