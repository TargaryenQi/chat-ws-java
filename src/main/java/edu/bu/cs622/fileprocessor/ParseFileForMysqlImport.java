package edu.bu.cs622.fileprocessor;

import static edu.bu.cs622.db.MongoDB.convertToDate;
import static edu.bu.cs622.db.MongoDB.convertToTime;

import edu.bu.cs622.db.MongoDB;
import edu.bu.cs622.db.MySqlUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Sichi Zhang on 2019/12/2.
 */
public class ParseFileForMysqlImport {

    public static void main(String[] args) {
        insertHeartRate();
        insertActivi();
        insertActiviFit();
    }

    public static void insertActiviFit() {
        File data = new File("/Users/hannbial/Documents/GitHub/chat-ws-java/data/ActiviFit.data");
        try {
            BufferedReader br = new BufferedReader(new FileReader(data));
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
                    String sql = "insert into activity values(?,?,?,?,?)";
                    Object[] param = new Object[]{start_time, end_time, activity, duration, date};
                    MySqlUtil.dosql(sql, param);
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

    public static void insertActivi() {
        File data = new File("/Users/hannbial/Documents/GitHub/chat-ws-java/data/Activity.data");
        try {
            BufferedReader br = new BufferedReader(new FileReader(data));
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
                    String sql = "insert into activity_step values(?,?,?)";
                    Object[] param = new Object[]{date, step_count, step_delta};
                    MySqlUtil.dosql(sql, param);
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

    public static void insertHeartRate() {
        File data = new File("/Users/hannbial/Documents/GitHub/chat-ws-java/data/HeartRate.data");
        try {
            BufferedReader br = new BufferedReader(new FileReader(data));
            String line;
            while ((line = br.readLine()) != null) {
                //Match the target line
                JSONObject record = new JSONObject(line);
                try {
                    String timestamp = record.getString("timestamp");
                    String date = convertToDate(timestamp);
                    int bpm = record.getJSONObject("sensor_data").getInt("bpm");
                    String sql = "insert into heart_Rate values(?,?)";
                    Object[] param = new Object[]{date, bpm};
                    MySqlUtil.dosql(sql, param);
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
