package edu.bu.cs622.search;

import edu.bu.cs622.message.SearchResult;
import edu.bu.cs622.message.SearchType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BruteForce {

  public static SearchResult search(String queryItem) {
    SearchResult searchResult = new SearchResult(SearchType.BRUTE_FORCE);

    ArrayList<String> result = new ArrayList<>();

    long timeConsuming = 0;
    long start = System.currentTimeMillis();
    try {
      BufferedReader reader = new BufferedReader(new FileReader("MergedData/allDaysData.txt"));
      String line = null;

      line = reader.readLine();
      System.out.println("Brute force Search begins!");
      System.out.println("Brute force Search for " + queryItem);
      while (line != null) {

        if(line.contains(queryItem))  {
          System.out.println(line);
          result.add(line);
        }
        line = reader.readLine();
      }

      reader.close();
    } catch ( IOException e) {
      e.printStackTrace();
    }
    long end = System.currentTimeMillis();
    timeConsuming = end - start;
    searchResult.setResultNumber(result.size());
    searchResult.setResults(result);
    searchResult.setTimeConsuming(timeConsuming);
    return searchResult;
  }

}
