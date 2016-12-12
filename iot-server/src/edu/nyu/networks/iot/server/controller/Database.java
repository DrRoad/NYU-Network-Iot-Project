package edu.nyu.networks.iot.server.controller;

import java.sql.*;

public class Database {
    class ConnState {
        Connection conn;
        Statement stat;
        public ConnState(Connection c, Statement s) {
            conn = c;
            stat = s;
        }
    }

    public static ConnState initialize() throws Exception {
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
        return new ConnState(conn, stat);
    }
}
