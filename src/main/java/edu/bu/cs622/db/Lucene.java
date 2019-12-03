package edu.bu.cs622.db;

import edu.bu.cs622.message.SearchResult;
import edu.bu.cs622.message.SearchType;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *  Create a Lucene class
 *
 */
public class Lucene {
  private StandardAnalyzer analyzer;
  private Directory index;
  private IndexWriterConfig config;
  private IndexWriter indexWriter;
  private String fileName;

  public Lucene(String fileName) throws Exception {
    // 0. Specify the analyzer for tokenizing text.
    // The same analyzer should be used for indexing and searching
    analyzer = new StandardAnalyzer();
    File folder = new File("luceneIndex/"+fileName +"index");
    // Clear the content in the folder
    FileUtils.cleanDirectory(folder);
    // 1. create the index
    index = new MMapDirectory(folder.toPath());
    config = new IndexWriterConfig(analyzer);
    indexWriter = new IndexWriter(index,config);
    this.fileName = fileName;
  }

  public SearchResult luceneSearch(String target) throws IOException, ParseException {
    processFile();
    return query(target);
  }

  /**
   * Add data to the doc of Lucene
   */
  private void processFile() {
    BufferedReader bufferedReader;
    try {
      bufferedReader = new BufferedReader(new FileReader(fileName));
      String line = bufferedReader.readLine();
      while(line != null) {
        addDoc(indexWriter,line);
        line = bufferedReader.readLine();
      }
      bufferedReader.close();
      indexWriter.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Create query string and made the search
   * @param target the string to query
   */
  public SearchResult query(String target) throws ParseException, IOException {
    SearchResult searchResult = new SearchResult(SearchType.LUCENE);

    long start = System.currentTimeMillis();
    // the "sensorData" arg specifies the default field to use when no field is explicitly specified in the query.
    Query q = new QueryParser("sensorData", analyzer).parse(target);
    int hitsPerPage = 1000000;
    IndexReader indexReader = DirectoryReader.open(index);
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    TopDocs docs = indexSearcher.search(q,hitsPerPage);
    ScoreDoc[] hits = docs.scoreDocs;
    indexReader.close();
    long end = System.currentTimeMillis();
    long timeConsuming = end - start;

    ArrayList<String> results = new ArrayList<>();
    for(ScoreDoc hit : hits) {
      results.add(hit.toString());
    }
    searchResult.setResultNumber(hits.length);
    searchResult.setTimeConsuming(timeConsuming);
    searchResult.setResults(results);
    System.out.println("Lucene Search for " + target);
    System.out.println("Lucene search result number:" + hits.length);
    return searchResult;
  }

  /**
   * Add doc the lucene.
   * @param w indexWriter.
   * @param sensorData the data to be added to the doc.
   * @throws IOException
   */
  private static void addDoc(IndexWriter w, String sensorData) throws IOException {
    Document doc = new Document();
    doc.add(new TextField("sensorData", sensorData, Field.Store.YES));
    w.addDocument(doc);
  }

}


