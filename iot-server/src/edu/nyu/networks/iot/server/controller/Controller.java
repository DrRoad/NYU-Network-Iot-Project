package edu.nyu.networks.iot.server.controller;

import java.math.BigInteger;
import java.util.*;

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

    public Location() {
    }

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

public class Controller {
    private final static long PingInterval = 10000;
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
    private static Map<String, Stack<Value>> data = Collections.synchronizedMap(new HashMap<String, Stack<Value>>());
    private static Database db;
    static {
        Database tmp = null;
        try {
            tmp = new Database();
        } catch (Exception e) {

        }
        db = tmp;
    }


    public static void startSensing(String imei, List<String> sensors) {

        if (clientList.get(imei).isSensing) {
            return;
        }
        MessageBuilder m = new MessageBuilder(START);
        m.withPeriod(10000L);
        clientList.get(imei).isSensing = true;
        clientList.get(imei).lastStartTimeStamp = System.currentTimeMillis();
        clientList.get(imei).sendMessage(m.build());
    }

    public static void stopSensing(String imei, List<String> sensors) {
        if (!clientList.get(imei).isSensing) {
            return;
        }

        MessageBuilder m = new MessageBuilder(STOP);
        clientList.get(imei).isSensing = false;
        clientList.get(imei).sendMessage(m.build());
    }

    public static void send(String imei, boolean doCompressed) {

        MessageBuilder m = new MessageBuilder(SEND);
        m.withCompress(doCompressed);
        m.withFrequency(3);

        clientList.get(imei).sendMessage(m.build());
    }

    public static void addData(String imei, Value v) {
        if (!clientList.containsKey(imei)) {
            return;
        }
        data.get(imei).push(v);
    }

    public static void sendToDB(String imei, Value v) {
        if (!clientList.containsKey(imei)) {
            return;
        }
        long ts = System.currentTimeMillis();
        try {
            db.update(imei, ts, v);
        } catch (Exception e) {

        }
    }

    public static void main(String[] args) {
        System.out.println(Integer.getInteger("1234/r/n"));

        Driver server = new Driver(9002, clientList);
        Thread serverThread = new Thread(server);
        serverThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //TODO Suggest Breaking out into a new class, or not having as a separate thread
        Thread tick = new Thread(new Runnable() {
            long syncDataInterval=0;
            boolean isStopped = false;

            public void run() {
                while (!isStopped) {
                    syncDataInterval += 1;

                    if (clientList.size() == 0) {
                        continue;
                    }
                    for (String imei : clientList.keySet()) {
                        for (int i = 0; i < MAXPOLL; i++) {
                            JsonObject message = clientList.get(imei).readMessage();
//                            if (message != null) {
//                                System.out.println(message.toString());
////                              if (message.get("type").toString()==DATA){
////                              TODO fit data handling part here @wenliang
////                              continue;
////                              }
//                                //clientList.get(imei).batteryLevel=Long.parseLong(message.get("battery-level").toString());
//                            } else {
//                                break;
//                            }
                            if (message == null) { break; }
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
