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
    public Location(){}
    public Location(Double x, Double y) {
        this.x = x;
        this.y = y;
    }
}

class Value {
    Location location;
    Double value;
    public Value(){}
    public Value(Location l, Double v) {
        this.location = l;
        this.value = v;
    }
}

class QueryData implements Runnable {
    private String command = "sudo python /home/ubuntu/iotQueryPlotter.py";

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
    private final static long PingInterval = 10000;
    private final static long LiveInterval = 180000;
    private final static long LOWBATTERY = 20;

    private final static int MAXPOLL = 3;
    private final static String START = "START";
    private final static String STOP = "STOP";
    private final static String SEND = "SEND";

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
        MessageBuilder m = new MessageBuilder(START);
        //Sense for 10 seconds
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

    public static void send(String imei, int frequency, boolean doCompressed) {
        //If the device is sending with a frequency, return
        if (clientList.get(imei).isSensing == false) {
            return;
        }
        if (clientList.get(imei).isSendingData == true) {
            return;
        }

        clientList.get(imei).isSendingData = true;
        MessageBuilder m = new MessageBuilder(SEND);
        m.withCompress(doCompressed);
        m.withFrequency(frequency);
        clientList.get(imei).sendFrequency = frequency;
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
                boolean isStopped = false;
                while (!isStopped) {

                    //If nothing in the map
                    if (clientList.size() == 0) {
                        System.out.println("Nothing");
                        try {
                            Thread.sleep(PingInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    //If there is something in the map
                    Iterator<Map.Entry<String, MobilePhone>> iter = clientList.entrySet().iterator();
                    while (iter.hasNext()) {
                        boolean dead=false;
                        Map.Entry<String, MobilePhone> entry = iter.next();
//                        if (clientList.get(imei).dead){
//                            break;
//                        }
                        for (int i = 0; i < MAXPOLL; i++) {
                            JsonObject message = clientList.get(entry.getKey()).readMessage();
                            if (message != null) {
                                System.out.println(message.toString());
                            } else {
                                break;
                            }
                            //If battery is low, stop sensing, stop sending data, close the socket
                            long batteryLevel = message.get("battery_life").getAsLong();
                            if ( batteryLevel < LOWBATTERY) {
                                stopSensing(entry.getKey(),new ArrayList<>());
                                //send(imei,0, false);
                                clientList.get(entry.getKey()).closeSocket();
                                iter.remove();
                                dead=true;
                                break;
                            }
                            clientList.get(entry.getKey()).lastPingTimeStamp = System.currentTimeMillis();
                            clientList.get(entry.getKey()).batteryLevel = batteryLevel;
//                            if (syncDataInterval==3){
//                                clientList.get(imei).isSendingData=true;
//                                send(imei,20000, false);
//                            }
                        }
                        if ((clientList.get(entry.getKey()).tickRound-6)%12==0){
                            stopSensing(entry.getKey(),new ArrayList<>());
                        }
                        if (clientList.get(entry.getKey()).tickRound%12==0&&!dead){
                            startSensing(entry.getKey(), new ArrayList<String>());
                        }
                        clientList.get(entry.getKey()).tickRound++;
//                        if (System.currentTimeMillis() - clientList.get(entry.getKey()).lastPingTimeStamp > LiveInterval) {
//                            stopSensing(entry.getKey(), new ArrayList<>());
//                            //send(imei,0, false);
//                        }
                    }

                    try {
                        Thread.sleep(PingInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    //If run for 40 minutes, exit
//                    if (System.currentTimeMillis()-schedulerStartTime>MAXRUNTIME){
//                        isStopped=true;
//                    }
                }
            }
        });
        tick.start();
    }
}
