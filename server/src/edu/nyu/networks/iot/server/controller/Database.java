package edu.nyu.networks.iot.server.controller;

import java.sql.*;
import java.math.BigInteger;

/**
 * Database class storing data from mobiles
 * <p>
 *
 * @author Wenliang Zhao
 */

public class Database {

    private static Connection conn;
    private static Statement stat;

    public Database() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");

        // direct to an existing db
        String url = "jdbc:mysql://localhost:3306/mysql";
        conn = DriverManager.getConnection(url, "root", "1234");
        conn.setAutoCommit(false);
        stat = conn.createStatement();

        // create a new db
        stat.executeUpdate("CREATE DATABASE IF NOT EXISTS IoT");

        // open db
        stat.close();
        conn.close();
        url = "jdbc:mysql://localhost:3306/IoT";
        conn = DriverManager.getConnection(url, "root", "1234");
        stat = conn.createStatement();

        // create a new table
        // imei number, unix ts, latitude, longitude, noise value
        stat.executeUpdate("DROP TABLE IF EXISTS noise");
        stat.executeUpdate("CREATE TABLE noise(imei VARCHAR (80), time BigInt, lat DOUBLE, lon DOUBLE, noise DOUBLE )");
//        stat.executeUpdate("CREATE TABLE IF NOT EXISTS noise(imei VARCHAR (80), time BigInt, lat DOUBLE, lon DOUBLE, noise DOUBLE )");
    }

    public void update(String imei, long ts, Value v) throws Exception {
        BigInteger ts1 = BigInteger.valueOf(ts);
        String query = "INSERT INTO noise (imei, time, lat, lon, noise) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(query);
        int rowCount = 0;
        ps.setObject(1, imei, Types.VARCHAR);
        try {
            ps.setObject(2, ts1, Types.BIGINT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ps.setObject(3, v.location.y, Types.DOUBLE);
        ps.setObject(4, v.location.x, Types.DOUBLE);
        ps.setObject(5, v.value, Types.DOUBLE);
        try {
            rowCount = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("rowCount: "+rowCount);
        ps.close();
        //conn.commit();
    }

    public static void close() throws Exception {
        conn.close();
        stat.close();
    }
}
