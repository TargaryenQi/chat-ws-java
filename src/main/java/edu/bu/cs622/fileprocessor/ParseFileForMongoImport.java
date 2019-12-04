package edu.bu.cs622.fileprocessor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.bu.cs622.db.MongoDB;
import edu.bu.cs622.db.MySqlUtil;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

import static edu.bu.cs622.db.MongoDB.convertToDate;
import static edu.bu.cs622.db.MongoDB.convertToTime;


public class ParseFileForMongoImport {

    public static void main(String[] args) {
        MongoDB mongoDB = new MongoDB("smartwatch");
        insertActiviFit(mongoDB.getMongoDatabase());
        insertActivity(mongoDB.getMongoDatabase());
        insertHeartRate(mongoDB.getMongoDatabase());
    }

    public static void insertActiviFit(MongoDatabase MongoDatabase) {
        MongoCollection<Document> collectionActiviFit = MongoDatabase.getCollection("ActiviFit");
        try {
            BufferedReader br = new BufferedReader(new FileReader("./data/ActiviFit.data"));
            String line;
            while ((line = br.readLine()) != null) {
                //Match the target line
                JSONObject record = new JSONObject(line);
                try {
                    String start_time_string = record.getJSONObject("timestamp")
                        .getString("start_time");
                    String date = convertToDate(start_time_string);
                    String start_time = convertToTime(start_time_string);
                    String end_time_string = record.getJSONObject("timestamp")
                        .getString("end_time");
                    String end_time = convertToTime(end_time_string);
                    String activity = record.getJSONObject("sensor_data").getString("activity");
                    int duration = record.getJSONObject("sensor_data").getInt("duration");
                    Document document = new Document();
                    document.put("date",date);
                    document.put("start_time",start_time);
                    document.put("end_time",end_time);
                    document.put("activity",activity);
                    document.put("duration",duration);
                    collectionActiviFit.insertOne(document);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void insertActivity(MongoDatabase MongoDatabase) {
        MongoCollection<Document> collectionActivity = MongoDatabase.getCollection("Activity");


        try {
            BufferedReader br = new BufferedReader(new FileReader("data/Activity.data"));
            String line;
            while ((line = br.readLine()) != null) {
                //Match the target line
                JSONObject record = new JSONObject(line);
                try {
                    String timestamp = record.getString("timestamp");
                    String date = convertToDate(timestamp);
                    String time = convertToTime(timestamp);
                    int step_count = record.getJSONObject("sensor_data").getInt("step_counts");
                    int step_delta = record.getJSONObject("sensor_data").getInt("step_delta");
                    Document document = new Document();
                    document.put("timestamp",timestamp);
                    document.put("date",date);
                    document.put("time",time);
                    document.put("step_count",step_count);
                    document.put("step_delta",step_delta);
                    collectionActivity.insertOne(document);
                } catch (JSONException e) {
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void insertHeartRate(MongoDatabase MongoDatabase) {
        MongoCollection<Document> collectionHeartRate = MongoDatabase.getCollection("HeartRate");
        try {
            BufferedReader br = new BufferedReader(new FileReader("data/HeartRate.data"));
            String line;
            while ((line = br.readLine()) != null) {
                //Match the target line
                JSONObject record = new JSONObject(line);
                try {
                    String timestamp = record.getString("timestamp");
                    String date = convertToDate(timestamp);
                    int bpm = record.getJSONObject("sensor_data").getInt("bpm");
                    Document document = new Document();
                    document.put("timestamp",timestamp);
                    document.put("date",date);
                    document.put("bpm",bpm);
                    collectionHeartRate.insertOne(document);
                } catch (JSONException e) {
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
