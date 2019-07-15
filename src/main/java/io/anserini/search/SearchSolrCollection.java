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

import com.google.common.base.Splitter;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.rerank.lib.ScoreTiesAdjusterReranker;
import io.anserini.search.topicreader.TopicReader;
import io.anserini.index.generator.TweetGenerator;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.Query;
import org.apache.lucene.document.LongPoint;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.anserini.index.generator.LuceneDocumentGenerator.FIELD_ID;

/*
* Entry point of the Retrieval.
 */
public final class SearchSolrCollection implements Closeable {

  private static final Logger LOG = LogManager.getLogger(SearchCollection.class);
  private static final int TIMEOUT = 600 * 1000;

  private final Args args;
  private ObjectPool<SolrClient> solrPool;

  public static final class Args {

    // required arguments

    @Option(name = "-topics", metaVar = "[file]", handler = StringArrayOptionHandler.class, required = true, usage = "topics file")
    public String[] topics;

    @Option(name = "-output", metaVar = "[file]", required = true, usage = "output file")
    public String output;

    @Option(name = "-topicreader", required = true, usage = "define how to read the topic(query) file: one of [Trec|Webxml]")
    public String topicReader;

    @Option(name = "-solr.index", usage = "the name of the index in Solr")
    public String solrIndex = null;

    @Option(name = "-solr.poolSize", metaVar = "[NUMBER]", usage = "the number of clients to keep in the pool")
    public int solrPoolSize = 16;

    @Option(name = "-solr.zkUrl", usage = "the URL of Solr's ZooKeeper (comma separated list of using ensemble)")
    public String zkUrl = null;

    @Option(name = "-solr.zkChroot", usage = "the ZooKeeper chroot")
    public String zkChroot = "/";

    // optional arguments
    @Option(name = "-threads", metaVar = "[Number]", usage = "Number of Threads")
    public int threads = 1;

    @Option(name = "-topicfield", usage = "Which field of the query should be used, default \"title\"." +
            " For TREC ad hoc topics, description or narrative can be used.")
    public String topicfield = "title";

    @Option(name = "-searchtweets", usage = "Whether the search is against a tweet " +
            "index created by IndexCollection -collection TweetCollection")
    public Boolean searchtweets = false;

    @Option(name = "-hits", metaVar = "[number]", required = false, usage = "max number of hits to return")
    public int hits = 1000;

    @Option(name = "-runtag", metaVar = "[tag]", required = false, usage = "runtag")
    public String runtag = null;

  }

  private final class SolrSearcherThread<K> extends Thread {

    final private SortedMap<K, Map<String, String>> topics;
    final private String outputPath;
    final private String runTag;

    private SolrSearcherThread(SortedMap<K, Map<String, String>> topics, String outputPath, String runTag) throws IOException {

      this.topics = topics;
      this.runTag = runTag;
      this.outputPath = outputPath;
      setName(outputPath);
    }

    @Override
    public void run() {
      try {
        LOG.info("[Start] Retrieval with Solr collection: " + args.solrIndex);
        final long start = System.nanoTime();
        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(outputPath), StandardCharsets.US_ASCII));

        // Solr client
        SolrClient client = solrPool.borrowObject();

        for (Map.Entry<K, Map<String, String>> entry : topics.entrySet()) {
          K qid = entry.getKey();
          String queryString = entry.getValue().get(args.topicfield);
          ScoredDocuments docs = searchSolr(client, qid, queryString);

          /**
           * the first column is the topic number.
           * the second column is currently unused and should always be "Q0".
           * the third column is the official document identifier of the retrieved document.
           * the fourth column is the rank the document is retrieved.
           * the fifth column shows the score (integer or floating point) that generated the ranking.
           * the sixth column is called the "run tag" and should be a unique identifier for your
           */
          for (int i = 0; i < docs.documents.length; i++) {
            out.println(String.format(Locale.US, "%s Q0 %s %d %f %s", qid,
                    docs.documents[i].getField(FIELD_ID).stringValue(), (i + 1), docs.scores[i], runTag));
          }
        }
        out.flush();
        out.close();

        // Needed for orderly shutdown so the SolrClient executor does not delay main thread exit
        try {
          solrPool.returnObject(client);
          solrPool.close();
        } catch (Exception e) {
          LOG.error("Exception in SolrClient executor: ", e);
        }

        final long durationMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        LOG.info("[Finished] Run " + topics.size() + " topics searched in "
                + DurationFormatUtils.formatDuration(durationMillis, "HH:mm:ss"));
      } catch (Exception e) {
        LOG.error(Thread.currentThread().getName() + ": Unexpected Exception:", e);
      }
    }
  }

  public SearchSolrCollection(Args args) throws IOException {
    this.args = args;
    LOG.info("Solr index: " + args.solrIndex);
    LOG.info("Solr ZooKeeper URL: " + args.zkUrl);
    LOG.info("SolrClient pool size: " + args.solrPoolSize);
    GenericObjectPoolConfig<SolrClient> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(args.solrPoolSize);
    config.setMinIdle(args.solrPoolSize); // To guard against premature discarding of solrClients
    this.solrPool = new GenericObjectPool(new SolrClientFactory(), config);
  }

  @SuppressWarnings("unchecked")
  public<K> void runTopics() throws IOException {
    TopicReader<K> tr;
    SortedMap<K, Map<String, String>> topics = new TreeMap<>();
    for (String singleTopicsFile : args.topics) {
      Path topicsFilePath = Paths.get(singleTopicsFile);
      if (!Files.exists(topicsFilePath) || !Files.isRegularFile(topicsFilePath) || !Files.isReadable(topicsFilePath)) {
        throw new IllegalArgumentException("Topics file : " + topicsFilePath + " does not exist or is not a (readable) file.");
      }
      try {
        tr = (TopicReader<K>) Class.forName("io.anserini.search.topicreader." + args.topicReader + "TopicReader")
            .getConstructor(Path.class).newInstance(topicsFilePath);
        topics.putAll(tr.read());
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to load topic reader: " + args.topicReader);
      }
    }
  
    final String runTag = args.runtag == null ? "Solrini" : args.runtag;
    final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(args.threads);

    executor.execute(new SolrSearcherThread<K>(topics, args.output, runTag));

    executor.shutdown();

    try {
      // Wait for existing tasks to terminate
      while (!executor.awaitTermination(1, TimeUnit.MINUTES)) {}
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      executor.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }

  private class SolrClientFactory extends BasePooledObjectFactory<SolrClient> {

    @Override
    public SolrClient create() {
      return new CloudSolrClient.Builder(Splitter.on(',').splitToList(args.zkUrl), Optional.of(args.zkChroot))
              .withConnectionTimeout(TIMEOUT)
              .withSocketTimeout(TIMEOUT)
              .build();
    }

    @Override
    public PooledObject<SolrClient> wrap(SolrClient solrClient) {
      return new DefaultPooledObject(solrClient);
    }

    @Override
    public void destroyObject(PooledObject<SolrClient> pooled) throws Exception {
      pooled.getObject().close();
    }
  }

  public<K> ScoredDocuments searchSolr(SolrClient client, K qid, String queryString)
          throws IOException {

    SolrDocumentList results = null;

    SolrQuery solrq = new SolrQuery();
    solrq.set("df", "contents");
    solrq.set("fl", "* score");
    solrq.setQuery(queryString);
    solrq.setRows(args.hits);
    solrq.setSort(SortClause.desc("score"));

    try {
      QueryResponse response = client.query(args.solrIndex, solrq);
      results = response.getResults();
    } catch (Exception e) {
      LOG.error("Exception during Solr query: ", e);
    }

    ScoreTiesAdjusterReranker reranker = new ScoreTiesAdjusterReranker();
    return reranker.rerank(ScoredDocuments.fromSolrDocs(results), null);
  }

  public<K> ScoredDocuments searchSolrTweets(SolrClient client, K qid, String queryString, long t) throws IOException {

    SolrDocumentList results = null;

    SolrQuery solrq = new SolrQuery();
    solrq.set("df", "contents");
    solrq.set("fl", "* score");
    solrq.setQuery(queryString);
    solrq.setRows(args.hits);
    solrq.setSort(SortClause.desc("score"));

    // Do not consider the tweets with tweet ids that are beyond the queryTweetTime
    // <querytweettime> tag contains the timestamp of the query in terms of the
    // chronologically nearest tweet id within the corpus
    Query filter = LongPoint.newRangeQuery(TweetGenerator.StatusField.ID_LONG.name, 0L, t);
    solrq.set("fq", filter.toString());

    try {
      QueryResponse response = client.query(args.solrIndex, solrq);
      results = response.getResults();
    } catch (Exception e) {
      LOG.error("Exception during Solr query: ", e);
    }

    ScoreTiesAdjusterReranker reranker = new ScoreTiesAdjusterReranker();
    return reranker.rerank(ScoredDocuments.fromSolrDocs(results), null);
  }

  @Override
  public void close() {
    solrPool.close();
  }

  public static void main(String[] args) throws Exception {
    Args searchSolrArgs = new Args();
    CmdLineParser parser = new CmdLineParser(searchSolrArgs, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: SearchSolrCollection" + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    final long start = System.nanoTime();
    SearchSolrCollection searcher = new SearchSolrCollection(searchSolrArgs);
    searcher.runTopics();
    searcher.close();
    final long durationMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    LOG.info("Total run time: " + DurationFormatUtils.formatDuration(durationMillis, "HH:mm:ss"));
  }
}
