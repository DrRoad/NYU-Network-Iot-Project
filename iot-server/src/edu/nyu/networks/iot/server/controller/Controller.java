package edu.nyu.networks.iot.server.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonObject;

//For representing location
class Location {
    float x;
    float y;

    public Location() {
    }

    public Location(float x, float y) {
        this.x = x;
        this.y = y;
    }
}

public class Controller {
    private final static long TickInterval = 60000;
    //If not responsive for LiveInterval=5 mins
    private final static long LiveInterval = 300000;
    //Record for an interval of 20 seconds
    private final static long MaxRecordInterval = 20000;
    private final static int MAXMSGPOOL = 3;
    private final static long BatteryLevel = 10;
    private final static String DATA = "data";
    private final static String CONTROL = "data";
    private final static String START = "start";
    private final static String STOP = "stop";
    private final static String SEND = "send";

    private static Map<String, MobilePhone> clientList = Collections.synchronizedMap(new HashMap<String, MobilePhone>());

    //    public static void update(String imei,String type,MobilePhone mobilePhone){
////        if (type.equals("data")){
//            clientList.put(imei,mobilePhone);
////        }else if(type.equals("control")){
////
////        }
//
//    }
    public static void startSensing(String imei, List<String> sensors) {
        System.out.println("Start sensing");
        if (clientList.get(imei).isSensing) {
            return;
        }
        MessageBuilder m = new MessageBuilder(START);
        m.withPeriod(MaxRecordInterval);
//        StringBuilder sb = new StringBuilder();
//        sb.append(START);
//        sb.append(",");
//        sb.append(MaxRecordInterval);
//        if (sensors.size()!=0){
//            for(int i=0;i<sensors.size();i++){
//                sb.append(",");
//                sb.append(sensors.get(i));
//            }
//        }
        clientList.get(imei).isSensing = true;
        clientList.get(imei).lastStartTimeStamp = System.currentTimeMillis();
        clientList.get(imei).sendMessage(m.build());
    }

    public static void stopSensing(String imei, List<String> sensors) {
        if (!clientList.get(imei).isSensing) {
            return;
        }
//        StringBuilder sb = new StringBuilder();
//        sb.append(STOP);
//
//        if (sensors.size()!=0){
//            for(int i=0;i<sensors.size();i++){
//                sb.append(",");
//                sb.append(sensors.get(i));
//            }
//        }

        MessageBuilder m = new MessageBuilder(STOP);
        clientList.get(imei).isSensing = false;
        clientList.get(imei).sendMessage(m.build());
    }

    public static void sync(String imei, boolean doCompressed) {

        MessageBuilder m = new MessageBuilder(SEND);
        m.withCompress(doCompressed);
        m.withFrequency(3);
//        StringBuilder sb = new StringBuilder();

//        sb.append(",");
//        sb.append(doCompressed);
        clientList.get(imei).sendMessage(m.build());
    }

//    public static boolean isNotMoving(Location l1,Location l2){
//        double movement=Math.pow((l1.x-l2.x),2)+ Math.pow((l1.y-l2.y),2);
//        if (movement<1){
//            return true;
//        }
//        return false;
//    }

    public static void main(String[] args) {
        Driver server = new Driver(9002, clientList);
        Thread serverThread = new Thread(server);
        serverThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        boolean isStopped = false;

        Thread tick = new Thread(new Runnable() {
            long SyncDataInterval = 0;

            public void run() {
                while (!isStopped) {
                    SyncDataInterval += 1;
                    if (clientList.size() == 0) {
                        continue;
                    }

                    for (String imei : clientList.keySet()) {
                        int msgAmount = clientList.get(imei).getQueueSize();
                        if (msgAmount == 0) {
                            continue;
                        }
                        for (int i = 0; i < msgAmount && i<=MAXMSGPOOL; i++) {
                            //Read back message from the queue
                            JsonObject message = clientList.get(imei).readMessage();
                            if (message != null) {
                                System.out.println(message.get("calibration").toString());
                            }
                            if (message.get("type").toString()==DATA){
                                //TODO fit data handling part here @wenliang
                                continue;
                            }
                            //Else, if message type is control, do the following
                            if (clientList.get(imei).lastPingTimeStamp - System.currentTimeMillis() > LiveInterval) {
                                stopSensing(imei, new ArrayList<String>());
                                continue;
                            }
//                            if (Integer.parseInt(message.get("battery_level").toString()) < BatteryLevel) {
//                                stopSensing(imei, new ArrayList<String>());
//                                continue;
//                            }
                            clientList.get(imei).lastPingTimeStamp = System.currentTimeMillis();
                            startSensing(imei, new ArrayList<String>());
                            if (SyncDataInterval == 3) {
                                sync(imei, false);
                            }
                            //Update state we store in memory
                            clientList.get(imei).batteryLevel=Long.parseLong(message.get("battery_level").toString());
                            //clientList.get(imei).location=message.get("location").getAsJsonArray();
                        }

                    }
                    if (SyncDataInterval == 3) {
                        SyncDataInterval = 0;
                    }
                    try {
                        Thread.sleep(TickInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        tick.start();

    }
}
