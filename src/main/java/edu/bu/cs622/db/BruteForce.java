package edu.bu.cs622.db;

import edu.bu.cs622.message.SearchResult;
import edu.bu.cs622.message.SearchType;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BruteForce implements DBSearch{

  @Override
  public int howManyStepsOnOneDay(String date) {
    int count = 0;
    try {
      BufferedReader reader = new BufferedReader(new FileReader("data/Activity.data"));
      String line = null;
      line = reader.readLine();
      while (line != null) {
        if(line.contains(date))  {
          JSONObject jsonObject = new JSONObject(line);
          int step_delta = jsonObject.getJSONObject("sensor_data").getInt("step_delta");
          count += step_delta;
        }
        line = reader.readLine();
      }

      reader.close();
    } catch ( IOException e) {
      e.printStackTrace();
    }
    return count;
  }

  @Override
  public boolean AreThereRunningEvent(String date) {
    boolean result = false;
    try {
      BufferedReader reader = new BufferedReader(new FileReader("data/ActivityFit.data"));
      String line = null;
      line = reader.readLine();
      while (line != null) {
        if(line.contains(date) && line.contains("run"))  {
          return true;
        }
        line = reader.readLine();
      }
      reader.close();
    } catch ( IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public int howManyHeartRateRecords(String date) {
    int count = 0;
    try {
      BufferedReader reader = new BufferedReader(new FileReader("data/Activity.data"));
      String line = null;
      line = reader.readLine();
      while (line != null) {
        if(line.contains(date))  {
          JSONObject jsonObject = new JSONObject(line);
          count += Integer.parseInt(jsonObject.get("bpm").toString());
        }
        line = reader.readLine();
      }
      reader.close();
    } catch ( IOException e) {
      e.printStackTrace();
    }
    return count;
  }

}
