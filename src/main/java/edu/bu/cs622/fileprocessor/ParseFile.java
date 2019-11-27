package edu.bu.cs622.fileprocessor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is used to preParse file and put the content of different sensor
 * to a HashMap, the sensor is the key and the content of certain sensor is stored
 * in a LinkedList.
 *
 * After the preParse, it will be very quick to search the content of certain sensor
 *
 */
public class ParseFile {
  /**
   * The function to preParse file to store content of certain sensor in a HashMap
   * @param filePath Target file that you want to preparse
   * @return HashMap(Key: sensorName, Value: LinkedList of content string)
   */
  public static HashMap<String, ArrayList<String>> preParseFile(String filePath){
    HashMap<String,ArrayList<String>> dictionary = new HashMap<>();
    BufferedReader reader;
    try {
      reader = new BufferedReader(new FileReader(filePath));
      String line = reader.readLine();
      while(line != null) {
        /**
         * According to the features of the file content, if one line start with '{' and end with '}'
         * and contains "sensor_name", then this line should be the content of certain sensor,
         * which means we could store it in the dictionary.
         */
        if(line.charAt(0) == '{' && line.charAt(line.length() - 1) == '}' &&
            line.contains("sensor_name")) {
          String sensorName = getSensorName(line).toLowerCase();
          ArrayList<String> list = dictionary.getOrDefault(sensorName,new ArrayList<>());
          list.add(line);
          dictionary.put(sensorName,list);
        }
        line = reader.readLine();
      }
      reader.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return dictionary;
  }

  /**
   * To get the sensorName from a line of json style string
   * According to the features of the json string, the sensor name
   * locates at particular index range.
   */
  private static String getSensorName(String line) {
    //The first ':' is 2 index ahead the sensor name
    int indexOfFirstSemicolon = line.indexOf(':');
    //The first ',' is 1 index behind the sensor name
    int indexOfFirstComma = line.indexOf(',');
    return line.substring(indexOfFirstSemicolon + 2,indexOfFirstComma - 1);
  }
}

