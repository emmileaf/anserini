package io.anserini.collection;

import org.junit.Test;

import javax.xml.transform.Source;

import static org.junit.Assert.*;
import java.nio.file.Paths;
import java.nio.file.Path;

public class TrecCollectionTest {

  @Test
  public void test() throws Exception {
    Path collectionPath = Paths.get("C:/cygwin64/home/Emily/usra/collection/disk45");
    TrecCollection testCollection = new TrecCollection(collectionPath);
    Boolean first = true;
    for (FileSegment<TrecCollection.Document> segment : testCollection){
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

  @Test
  public void test2() throws Exception {
    Path collectionPath = Paths.get("C:/cygwin64/home/Emily/usra/collection/disk45");
    DocumentCollection testCollection = new TrecCollection(collectionPath);
    Boolean first = true;
    for (Object s : testCollection){
      FileSegment segment = (FileSegment) s; // cast here
      System.out.println(segment.path);
      if (first) {
        for (Object d : segment){
          SourceDocument doc = (SourceDocument) d; // cast here
          System.out.println(doc.id());
          System.out.println(doc.content());
        }
        first = false;
      }
    }
  }


}