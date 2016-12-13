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
        stat.executeUpdate("create table noise(imei VARCHAR (80), lat DOUBLE, lon DOUBLE, noise DOUBLE )");

        // example update
        //stat.executeUpdate("insert into noise values('example1', 50.123456, 123.123456, -80.123456)");

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

    public static void update(String imei, Value v) throws Exception {
        stat.executeUpdate("insert into noise values(imei, v.Location.x, v.Location.y, v.value)");
    }

    public static void close() throws Exception {
        conn.close();
        stat.close();
    }
}
