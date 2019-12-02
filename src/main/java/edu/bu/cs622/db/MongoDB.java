package edu.bu.cs622.db;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.bu.cs622.message.SearchResult;
import edu.bu.cs622.message.SearchType;
import org.bson.Document;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoDB {
  private MongoClient mongoClient;
  private MongoDatabase mongoDatabase;

  public MongoDB(String databaseName) {
    Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
    mongoLogger.setLevel(Level.SEVERE);
    // initialize the client object
    mongoClient = new MongoClient();
    // get the 'test' dataset
    mongoDatabase = mongoClient.getDatabase(databaseName);
    System.out.println("[Mongo]: The client is connected.");
    System.out.println("[Mongo]: " + mongoDatabase.getName() + " database is initialized.");
  }

  /**
   * MongoDB search.
   * @param date search date.
   * @return search result.
   */
  public SearchResult mongoSearch(String date) {
    SearchResult searchResult = new SearchResult(SearchType.MONGODB);
    ArrayList<String> results = new ArrayList<>();
    long start = System.currentTimeMillis();
    String runningData = checkRunOrNotInCertainDay(date);
    String stepsData =  getTotalStepsInCertainDay(date);
    String heartRateData = getCountOfHeartRateInCertainDay(date);
    long end = System.currentTimeMillis();
    results.add(runningData);
    results.add(stepsData);
    results.add(heartRateData);
    searchResult.setResultNumber(3);
    searchResult.setTimeConsuming(end - start);
    searchResult.setResults(results);
    return searchResult;
  }

  /**
   * Count the number of heart-rate the user has received on his/her smartwatch in a particular day.
   * @param date date that you want to query.
   * @return heart-rate count on that day as an int.
   */
  public String getCountOfHeartRateInCertainDay(String date) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("HeartRate");
    BasicDBObject queryObject = new BasicDBObject();
    queryObject.put("date",date);
    FindIterable<Document> documents = collection.find(queryObject);
    if(documents.first() == null) {
      return "There is no heart-rate data received on " + date + ".";
    }
    for(Document document : documents) {
      count += Integer.parseInt(document.get("bpm").toString());
    }
    return "Number of heart-rate received on " + date + " is " + count + ".";
  }

  /**
   * Check whether in a particular day, the user has running event or not.
   * @param date the date to query.
   * @return true: there is running event; false: no running event.
   */
  public String checkRunOrNotInCertainDay(String date) {
    boolean run = false;
    StringBuilder result = new StringBuilder();
    MongoCollection<Document> collection = mongoDatabase.getCollection("ActivityDuration");
    BasicDBObject queryObject = new BasicDBObject();
    queryObject.put("date",date);
    FindIterable<Document> documents = collection.find(queryObject);
    if(documents.first() == null) {
      return "There is no activity data received on " + date + ".";
    }
    for(Document document : documents) {
      if(document.get("activity").equals("running")) {
        String start = document.get("start_time").toString();
        String end = document.get("end_time").toString();
        result.append("Yes, you ran from ").append(start).append(" to ").append(end).append(" on ").append(date).append(";");
        run = true;
      }
    }
    if(!run) {
      return "No, there is no running on " + date + ".";
    }
    return result.toString();
  }

  /**
   * Get the total amount of steps the user took in certain day.
   * @param date the date to query.
   * @return number of steps as an int.
   */
  public String getTotalStepsInCertainDay(String date) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("ActivityStepCount");
    BasicDBObject queryObject = new BasicDBObject();
    queryObject.put("date",date);
    FindIterable<Document> documents = collection.find(queryObject);
    if(documents.first() == null) {
      return "There is no step data received on " + date + ".";
    }
    for(Document document : documents) {
      count += Integer.parseInt(document.get("step_delta").toString());
    }
    return "The user walked " + count + " steps on " + date + "." ;
  }

  /**
   * List its collections.
   */
  public void printCollections() {
    for (String name : mongoDatabase.listCollectionNames()) {
      System.out.println("Collections inside this db:"+name);
    }
  }

  /**
   * List all the documents in certain collection.
   * @param collectionName the collection you want to read.
   */
  public void printDocumentOfCertainCollection(String collectionName) {
    MongoCollection<Document> col = mongoDatabase.getCollection(collectionName);
    for (Document document : col.find()) {
      System.out.println("docs inside the col:" + document);
    }
  }

  /**
   * Transfer all the data to database.
   * @param sensorDictionary the data to be transferred is stored in a dictionary.
   *                         key of dic: the Sensor Name.
   *                         value of dic: array list of JSON string for particular Sensor.
   */
  public void transferDataToDatabase(HashMap<String, ArrayList<String>> sensorDictionary) {
    insertCollectionBT(sensorDictionary.get("bt"));
    insertCollectionActivity(sensorDictionary.get("activity"));
    insertCollectionBattery(sensorDictionary.get("battery"));
    insertCollectionLight(sensorDictionary.get("light"));
    insertCollectionHearRate(sensorDictionary.get("heartrate"));
  }

  /**
   * Insert data of Sensor Activity to database.
   * @param data arrayList of JSON string of Sensor Activity.
   */
  public void insertCollectionActivity(ArrayList<String> data) {
    int count1 = 0;
    MongoCollection<Document> collectionStepCount = mongoDatabase.getCollection("ActivityStepCount");
    int count2 = 0;
    MongoCollection<Document> collectionDuration = mongoDatabase.getCollection("ActivityDuration");
    try {
      for (String item : data) {
        JSONObject object = new JSONObject(item);
        if (item.contains("step_counts")) {
          count1++;
          String timestamp = object.getString("timestamp");
          String  date = convertToDate(timestamp);
          String time = convertToTime(timestamp);
          int step_count = object.getJSONObject("sensor_data").getInt("step_counts");
          int step_delta = object.getJSONObject("sensor_data").getInt("step_delta");
          Document document = new Document();
          document.put("date",date);
          document.put("time",time);
          document.put("step_count",step_count);
          document.put("step_delta",step_delta);
          collectionStepCount.insertOne(document);
        } else {
          count2++;
          String start_time_string = object.getJSONObject("timestamp").getString("start_time");
          String date = convertToDate(start_time_string);
          String start_time = convertToTime(start_time_string);
          String end_time_string = object.getJSONObject("timestamp").getString("end_time");
          String end_time = convertToTime(end_time_string);
          String activity = object.getJSONObject("sensor_data").getString("activity");
          int duration = object.getJSONObject("sensor_data").getInt("duration");
          Document document = new Document();
          document.put("date", date);
          document.put("start_time", start_time);
          document.put("end_time", end_time);
          document.put("activity", activity);
          document.put("duration", duration);
          collectionDuration.insertOne(document);
        }
      }
      System.out.println( count1 + " documents are inserted to ActivityStepCount collection.");
      System.out.println( count2 + " documents are inserted to ActivityDuration collection.");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Insert data of Sensor BT to database.
   * @param data arrayList of JSON string of Sensor BT.
   */
  public void insertCollectionBT(ArrayList<String> data) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("BT");
    try {
      for (String item : data) {
        count++;
        JSONObject object = new JSONObject(item);
        String timestamp = object.getString("timestamp");
        String date = convertToDate(timestamp);
        String time = convertToTime(timestamp);
        String state = object.getJSONObject("sensor_data").getString("state");
        Document document = new Document();
        document.put("date",date);
        document.put("time",time);
        document.put("state",state);
        collection.insertOne(document);
      }
      System.out.println( count + " documents are inserted to BT collection.");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Insert data of Sensor Battery to database.
   * @param data arrayList of JSON string of Sensor Battery.
   */
  public void insertCollectionBattery(ArrayList<String> data) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("Battery");
    try {
      for (String item : data) {
        count++;
        JSONObject object = new JSONObject(item);
        String timestamp = object.getString("timestamp");
        String date = convertToDate(timestamp);
        String time = convertToTime(timestamp);
        int percent = object.getJSONObject("sensor_data").getInt("percent");
        boolean charging = object.getJSONObject("sensor_data").getBoolean("charging");
        Document document = new Document();
        document.put("date",date);
        document.put("time",time);
        document.put("percent",percent);
        document.put("charging",charging);
        collection.insertOne(document);
      }
      System.out.println( count + " documents are inserted to Battery collection.");

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Insert data of Sensor HearRate to database.
   * @param data arrayList of JSON string of Sensor HeartRate.
   */
  public void insertCollectionHearRate(ArrayList<String> data) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("HeartRate");
    try {
      for(String item : data) {
        count++;
        JSONObject jsonObject = new JSONObject(item);
        String timestamp = jsonObject.getString("timestamp");
        String date = convertToDate(timestamp);
        String time = convertToTime(timestamp);
        int bpm = jsonObject.getJSONObject("sensor_data").getInt("bpm");
        Document document = new Document();
        document.put("date",date);
        document.put("time",time);
        document.put("bpm",bpm);
        collection.insertOne(document);
      }
      System.out.println( count + " documents are inserted to HeartRate collection.");
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Insert data of Sensor Light to database.
   * @param data arrayList of JSON string of Sensor Light.
   */
  public void insertCollectionLight(ArrayList<String> data) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("Light");
    try {
      for (String item : data) {
        count++;
        JSONObject object = new JSONObject(item);
        String timestamp = object.getString("timestamp");
        String date = convertToDate(timestamp);
        String time = convertToTime(timestamp);
        float lux = object.getJSONObject("sensor_data").getInt("lux");
        Document document = new Document();
        document.put("date",date);
        document.put("time",time);
        document.put("lux",lux);
        collection.insertOne(document);
      }
      System.out.println( count + " documents are inserted to Light collection.");

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private String convertToDate(String date) {
    java.util.Date utilDate = new java.util.Date(date);
    String pattern = "yyyy-MM-dd";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    String Date = simpleDateFormat.format(new Date(utilDate.getTime()));
    return Date;
  }

  private String convertToTime(String date) {
    java.util.Date utilDate = new java.util.Date(date);
    String pattern2 = "HH:mm:ss";
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat(pattern2);
    String time = simpleDateFormat2.format(new Date(utilDate.getTime()));
    return time;
  }

  /**
   * Close the connection.
   */
  public void close() {
    if (this.mongoClient != null) {
      this.mongoClient.close();
      System.out.println("[Mongo]: The client is closed.");
    } else {
      System.out.println("[Mongo]: Close failure.");
    }
  }
}

