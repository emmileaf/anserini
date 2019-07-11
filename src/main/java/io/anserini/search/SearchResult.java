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

package io.anserini.search;

import io.anserini.rerank.ScoredDocuments;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.List;
import java.util.ArrayList;

import static io.anserini.index.generator.LuceneDocumentGenerator.FIELD_ID;
import static io.anserini.index.generator.LuceneDocumentGenerator.FIELD_BODY;
import static io.anserini.index.generator.LuceneDocumentGenerator.FIELD_RAW;

public class SearchResult {
  // similar to ScoredDocuments, but abstracted?
  public String docid;
  public float score;
  public String content;
  public String raw;

  public SearchResult(String docid, float score, String content, String raw) {
    this.docid = docid;
    this.score = score;
    this.content = content;
    this.raw = raw;
  }

  public static List<SearchResult> fromLuceneScoredDocs(ScoredDocuments rs) {
    List<SearchResult> results = new ArrayList<>();
    for (int i = 0; i < rs.documents.length; i++) {
        SearchResult r = new SearchResult(rs.documents[i].getField(FIELD_ID).stringValue(),
                                          rs.scores[i],
                                          rs.documents[i].getField(FIELD_BODY).stringValue(),
                                          rs.documents[i].getField(FIELD_RAW).stringValue());
        results.add(r);
    }

    return results;
  }

  public static List<SearchResult> fromSolrScoredDocs(SolrDocumentList rs) {
    List<SearchResult> results = new ArrayList<>();
    for (SolrDocument d : rs) {
      String id = d.getFieldValue(FIELD_ID).toString();
      String content = d.getFieldValue(FIELD_BODY).toString();
      String raw = d.getFieldValue(FIELD_RAW).toString();
      float score = (float) d.getFieldValue("score");
      SearchResult r = new SearchResult(id, score, content, raw);
      results.add(r);
    }
    return results;
  }
}
