package edu.bu.cs622.db;

/**
 * Created by Sichi Zhang on 2019/12/3.
 */
public interface DBSearch {

    public int howManyStepsOnOneDay(String date);

    public boolean AreThereRunningEvent(String date);

    public int howManyHeartRateRecords(String date);
}