package edu.bu.cs622.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONObject;

/**
 * Created by Sichi Zhang on 2019/12/4.
 */
public class LuceneHelper {


    static StandardAnalyzer analyzer = new StandardAnalyzer();


    public static void createIndex() {
        File src = new File("data");
        File[] datas = src.listFiles();
        for (File f : datas) {
            createIndex(f.getName());
        }
    }

    public static void createIndex(String fileName) {
        // write your code here
        File src = new File("data/" + fileName);
        File tar = new File("index/" + fileName);

        try {
            // 0. Specify the analyzer for tokenizing text.
            // The same analyzer should be used for indexing and searching

            // 1. create the index
            Directory index = FSDirectory.open(tar.toPath());
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(OpenMode.CREATE);
            IndexWriter w = new IndexWriter(index, config);
            BufferedReader br = new BufferedReader(new FileReader(src));
            String line;
            while ((line = br.readLine()) != null) {
                addDoc(w, line);
            }
            br.close();
            w.close();
            // 2. query
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<JSONObject> search(String querystr, String dataName) {
        // the "title" arg specifies the default field to use when no field is explicitly specified
        //in the query.
        Query q = null;
        List<JSONObject> res = new ArrayList<>();
        try {
            File indexFile = new File("index/" + dataName + ".datanew");
            Directory index = FSDirectory.open(indexFile.toPath());
            //q = new QueryParser("record", analyzer).parse(querystr);
            q = new TermQuery(new Term("date", querystr));
            // 3. search
            int hitsPerPage = 10000000;
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(q, hitsPerPage);
            ScoreDoc[] hits = docs.scoreDocs;
            // 4. display results
            System.out.println("Found " + hits.length + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                res.add(new JSONObject(d.get("record")));
            }
            // reader can only be closed when there
            // is no need to access the documents any more.
            reader.close();
        } catch (
            IOException ex) {
            ex.printStackTrace();
        }
        return res;

    }

    private static void addDoc(IndexWriter w, String line) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("record", line, Field.Store.YES));
        JSONObject object = new JSONObject(line);
        doc.add(new StringField("date", object.get("timestamp").toString(), Field.Store.YES));
        w.addDocument(doc);
    }

}
