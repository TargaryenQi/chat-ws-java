package edu.bu.cs622.search;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class BruteForce {

  public static ArrayList<String> search(String queryItem) {
    ArrayList<String> result = new ArrayList<>();
    try {
      BufferedReader reader = new BufferedReader(new FileReader("MergedData/allDaysData.txt"));
      String line = null;

      line = reader.readLine();
      System.out.println("Search begins!");
      System.out.println("Search for" + queryItem);
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
    return result;
  }

}
