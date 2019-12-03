package edu.bu.cs622.db;

import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;

/**
 * Created by Sichi Zhang on 2019/12/3.
 */
public class MysqlDAO implements DBSearch {


    public int howManyStepsOnOneDay(String date) {
        String sql = "SELECT step_counts FROM activity_step "
            + "WHERE date = ? ORDER BY step_counts DESC LIMIT 1";
        Object[] param = new Object[]{date};
        CachedRowSet crs = MySqlUtil.select(sql, param);
        int count = 0;
        try {
            while (crs.next()) {
                count = crs.getInt("step_counts");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public boolean AreThereRunningEvent(String date) {
        String sql = "select 1 from activity where event = 'running' and date = ? limit 1;";
        Object[] param = new Object[]{date};
        CachedRowSet crs = MySqlUtil.select(sql, param);
        boolean flag = false;
        try {
            while (crs.next()) {
                flag = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public int howManyHeartRateRecords(String date) {
        String sql = "SELECT count(1) FROM heart_rate WHERE date = ? ";
        Object[] param = new Object[]{date};
        CachedRowSet crs = MySqlUtil.select(sql, param);
        int count = 0;
        try {
            while (crs.next()) {
                count = crs.getInt("count(1)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
}
