package edu.bu.cs622.db;


import java.util.List;
import org.json.JSONObject;

/**
 * Created by Sichi Zhang on 2019/12/4.
 */
public class LuceneSearch implements DBSearch {

    @Override
    public int howManyStepsOnOneDay(String date) {
        List<JSONObject> res = LuceneHelper.search(date, "Activity");
        int max = 0;
        for (JSONObject object : res) {
            max = Math
                .max(max, Integer.parseInt(
                    String.valueOf(object.getJSONObject("sensor_data").get("step_counts"))));
        }
        return max;
    }

    @Override
    public boolean AreThereRunningEvent(String date) {
        List<JSONObject> res = LuceneHelper.search(date, "ActiviFit");
        for (JSONObject o : res) {
            if (o.getJSONObject("sensor_data").get("activity").equals("running")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int howManyHeartRateRecords(String date) {
        return LuceneHelper.search(date, "HeartRate").size();
    }
}
