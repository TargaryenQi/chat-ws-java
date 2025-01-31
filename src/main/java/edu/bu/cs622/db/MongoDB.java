package edu.bu.cs622.db;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.bu.cs622.message.Message;
import edu.bu.cs622.user.User;
import org.bson.Document;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoDB implements DBSearch{
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

  @Override
  public int howManyStepsOnOneDay(String date) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("Activity");
    BasicDBObject queryObject = new BasicDBObject();
    queryObject.put("date",date);
    FindIterable<Document> documents = collection.find(queryObject);
    if(documents.first() == null) {
      return 0;
    }
    for(Document document : documents) {
      count += Integer.parseInt(document.get("step_delta").toString());
    }
    return count;
  }

  /**
   * Check whether in a particular day, the user has running event or not.
   * @param date the date to query.
   * @return true: there is running event; false: no running event.
   */
  @Override
  public boolean AreThereRunningEvent(String date) {
    MongoCollection<Document> collection = mongoDatabase.getCollection("ActiviFit");
    BasicDBObject queryObject = new BasicDBObject();
    queryObject.put("date",date);
    FindIterable<Document> documents = collection.find(queryObject);
    if(documents.first() == null) {
      return false;
    }
    for(Document document : documents) {
      if(document.get("activity").equals("running")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Count the number of heart-rate the user has received on his/her smartwatch in a particular day.
   * @param date date that you want to query.
   * @return heart-rate count on that day as an int.
   */
  @Override
  public int howManyHeartRateRecords(String date) {
    int count = 0;
    MongoCollection<Document> collection = mongoDatabase.getCollection("HeartRate");
    BasicDBObject queryObject = new BasicDBObject();
    queryObject.put("date",date);
    FindIterable<Document> documents = collection.find(queryObject);
    if(documents.first() == null) {
      return 0;
    }
    for(Document document : documents) {
      count ++;
    }
    return count;
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
   * Add message to mongodb.
   * @param message message to add.
   * @param user user who owns the message.
   */
  public void addToUserHistory(Message message, User user) {
    MongoCollection<Document> userHistoryCollection = mongoDatabase.getCollection("UserHistory");
    try {
        Document document = new Document();
        document.put("user_name",user.getName());
        document.put("user_id",user.getId());
        document.put("data",message.getData());
        document.put("date",new Date());
        document.put("message_type",message.getType().toString());
        if(message.getUser().getName().equals("Simple Bot")) {
          document.put("response",message.getSearchResults().toString());
          document.put("from_bot",true);
        } else {
          document.put("response",null);
          document.put("from_bot",false);
        }
        userHistoryCollection.insertOne(document);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
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

  public static String convertToDate(String date) {
    java.util.Date utilDate = new java.util.Date(date);
    String pattern = "yyyy-MM-dd";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    String Date = simpleDateFormat.format(new Date(utilDate.getTime()));
    return Date;
  }

  public static String convertToTime(String date) {
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

  public MongoDatabase getMongoDatabase() {
    return mongoDatabase;
  }
}

