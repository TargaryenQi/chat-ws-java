package edu.bu.cs622.db;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;

/**
 * Created by Sichi Zhang on 2019/12/2.
 */
public class MySqlUtil {

    static final String user = "root";
    static final String pwd = "root";

    private static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager
                .getConnection("jdbc:mysql://localhost:3306/met622", user, pwd);
            return conn;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    // execute a sql statement
    public static boolean dosql(String sql) {
        boolean isSucceed = false;
        Connection conn = getConnection();
        try {
            System.out.println("sql:" + sql);

            PreparedStatement ps = conn.prepareStatement(sql);
            isSucceed = ps.execute();
            System.out.println("dosql: " + sql);
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isSucceed;
    }

    // retrieve data with out user input
    public static CachedRowSet select(String sql) throws SQLException {
        CachedRowSet crs = null;
        try {
            System.out.println("sql:" + sql);
            crs = new CachedRowSetImpl();
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            crs.populate(rs);
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return crs;
    }

    // retrieve data with user input
    public static CachedRowSet select(String sql, Object[] params) {
        CachedRowSet crs = null;
        try {
            System.out.println("sql:" + sql);
            System.out.print("Params: ");
            for (Object param : params) {
                System.out.print(" " + param);
            }
            System.out.println("");
            crs = new CachedRowSetImpl();
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, String.valueOf(params[i]));
            }
            ResultSet rs = ps.executeQuery();
            crs.populate(rs);
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return crs;
    }

    public static boolean dosql(String sql, Object[] params) {
        boolean isSucceed = false;
        Connection conn = getConnection();
        try {
            System.out.println("sql:" + sql);
            System.out.print("Params: ");
            for (Object param : params) {
                System.out.print(param + " ");
            }
            System.out.println("");
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, String.valueOf(params[i]));
            }
            isSucceed = ps.execute();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isSucceed;
    }


}
