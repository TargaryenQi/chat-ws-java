package edu.bu.cs622.db;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BruteForce implements DBSearch{

  @Override
  public int howManyStepsOnOneDay(String date) {
    int count = 0;
    try {
      BufferedReader reader = new BufferedReader(new FileReader("/Users/hannbial/Documents/GitHub/chat-ws-java/data/Activity.datanew"));
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
      BufferedReader reader = new BufferedReader(new FileReader("/Users/hannbial/Documents/GitHub/chat-ws-java/data/ActiviFit.datanew"));
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
      BufferedReader reader = new BufferedReader(new FileReader("/Users/hannbial/Documents/GitHub/chat-ws-java/data/HeartRate.datanew"));
      String line = null;
      line = reader.readLine();
      while (line != null) {
        if(line.contains(date))  {
          count ++;
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
