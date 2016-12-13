package edu.nyu.networks.iot.server.controller;

import java.sql.*;

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
        String url = "jdbc:mysql://localhost:3306/test";
        Connection conn = DriverManager.getConnection(url, "root", "123456");
        Statement stat = conn.createStatement();

        // create a new db
        stat.executeUpdate("create database IoT");

        // open db
        stat.close();
        conn.close();
        url = "jdbc:mysql://localhost:3306/IoT";
        conn = DriverManager.getConnection(url, "root", "123456");
        stat = conn.createStatement();

        // create a new table
        // imei number, unix ts, latitude, longitude, noise value
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS noise(imei VARCHAR (80), time BigInt, lat DOUBLE, lon DOUBLE, noise DOUBLE )");

        // example update
        //stat.executeUpdate("insert into noise values('example1', 1234567890, 50.123456, 123.123456, -80.123456)");

        // query
//        ResultSet result = stat.executeQuery("select * from noise");
//        while (result.next())
//        {
//            System.out.println(result.getInt("imei") + " " + result.getString("noise"));
//        }
//        result.close();

        // close db
//        stat.close();
//        conn.close();
    }

    public static void update(String imei, long ts, Value v) throws Exception {
        String query = "INSERT INTO noise (imei, time, lat, lon, noise) VALUES (" + imei
                + ", " + ts + ", " + v.location.x + ", " + v.location.y + ", " + v.value + ")";
        System.out.println(query);
//        PreparedStatement ps = conn.prepareStatement(query);
//        ps.setString(1, imei);
//        ps.setLong(2, ts);
//        ps.setDouble(3, v.location.x);
//        ps.setDouble(4, v.location.y);
//        ps.setDouble(5, v.value);
//        ps.execute();
        stat.executeUpdate(query);
    }

    public static void close() throws Exception {
        conn.close();
        stat.close();
    }
}
