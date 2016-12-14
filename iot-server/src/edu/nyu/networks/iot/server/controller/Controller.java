package edu.nyu.networks.iot.server.controller;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

/**
 *
 * <p>
 *
 * @author Wesley Painter, Wenliang Zhao, Hongtao Chen
 */

//For representing location
class Location {
    Double x;
    Double y;

    public Location(Double x, Double y) {
        this.x = x;
        this.y = y;
    }
}

class Value {
    Location location;
    Double value;

    public Value(Location l, Double v) {
        this.location = l;
        this.value = v;
    }
}

class QueryData implements Runnable {
    private String command = "iotQueryPlotter.py";

    public QueryData() throws Exception {
        Thread.sleep(10000);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Process p = Runtime.getRuntime().exec(command);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

public class Controller {
    private final static long PingInterval = 1000;
    private final static long LiveInterval = 50000;
    private final static long MaxRecordInterval = 10000;
    private final static long BatteryLevel = 10;

    private final static String DATA = "data";
    private final static String CONTROL = "data";
    private final static int MAXPOLL = 3;
    private final static String START = "START";
    private final static String STOP = "STOP";
    private final static String SEND = "SEND";
    private final static String SENDCOMP = "SENDCOMP";

    private static Map<String, MobilePhone> clientList = Collections.synchronizedMap(new HashMap<String, MobilePhone>());
    private static Database db;

    public static void initializeDB() {
            Database tmp = null;
            try {
                tmp = new Database();
            } catch (Exception e) {
                e.printStackTrace();
            }
            db = tmp;
    }

    public static void startSensing(String imei, List<String> sensors) {

        if (clientList.get(imei).isSensing) {
            return;
        }
        MessageBuilder m = new MessageBuilder("start");
        m.withPeriod(10000L);
        clientList.get(imei).isSensing = true;
        clientList.get(imei).lastStartTimeStamp = System.currentTimeMillis();
        clientList.get(imei).sendMessage(m.build());
       // System.out.println("STARTING");
    }

    public static void stopSensing(String imei, List<String> sensors) {
        if (!clientList.get(imei).isSensing) {
            return;
        }

        MessageBuilder m = new MessageBuilder("stop");
        clientList.get(imei).isSensing = false;
        clientList.get(imei).sendMessage(m.build());
    }

    public static void send(String imei, boolean doCompressed) {

        MessageBuilder m = new MessageBuilder(SEND);
        m.withCompress(doCompressed);
        m.withFrequency(3);

        clientList.get(imei).sendMessage(m.build());
    }

    public static void sendToDB(String imei, Value v) {
        long ts = System.currentTimeMillis();
        try {
            System.out.println("Add one records!");
            db.update(imei, ts, v);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //System.out.println(Integer.getInteger("1234/r/n"));

        // initialize database -> connection and statement, if empty db or table, create
        Controller.initializeDB();

        // start query code, refresh every 10s, using single thread
        QueryData qd = null;
        try {
            qd = new QueryData();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println("err");
        }
        Thread query = new Thread(qd);
        query.start();

        // create driver
        Driver server = new Driver(9002, clientList);
        Thread serverThread = new Thread(server);
        serverThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        // start communication
        //TODO Suggest Breaking out into a new class, or not having as a separate thread
        Thread tick = new Thread(new Runnable() {

            @Override
            public void run() {
            	//System.out.println("HELLO");
            	boolean isStopped = false;
                long syncDataInterval=0;
                while (!isStopped) {

                    syncDataInterval += 1;

                    if (clientList.size() == 0) {
                        continue;
                    }
                   // System.out.println("Num Clients:" +clientList.size());
                    for (String imei : clientList.keySet()) {
                        for (int i = 0; i < MAXPOLL; i++) {
                            JsonObject message = clientList.get(imei).readMessage();
                          //  System.out.println("Looping thru clients -> Client Message:");
                            if (message != null) {
                            	System.out.println(message.toString());
//                              if (message.get("type").toString()==DATA){
//                              TODO fit data handling part here @wenliang
//                              continue;
//                              }
                                //clientList.get(imei).batteryLevel=Long.parseLong(message.get("battery-level").toString());
                            } else {
                                break;
                            }
                            //if (message == null) { break; }
                            if (clientList.get(imei).lastPingTimeStamp > LiveInterval) {
                                stopSensing(imei, new ArrayList<>());
                                continue;
                            }
//                            if (Integer.parseInt(message.get("battery_level").toString()) < BatteryLevel) {
//                                stopSensing(imei,new ArrayList<>());
//                                continue;
//                            }
                            clientList.get(imei).lastPingTimeStamp = System.currentTimeMillis();
                            startSensing(imei, new ArrayList<String>());
                            if (syncDataInterval==3){
                                send(imei,false);
                            }
                        }
                    }
                    if (syncDataInterval == 3) {
                        syncDataInterval = 0;
                    }
                    try {
                        Thread.sleep(PingInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        tick.start();
    }
}
