package edu.nyu.networks.iot.server.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Command {
    String commandName;
    long startTime;
    long endTime;
    long period;
    boolean sendAllAvailable;
    boolean doCompressedData;

    public Command() {
    }

    public Command(String s) {
        commandName = s;
    }
}

//For representing location
class Location {
    float x;
    float y;
    float z;

    public Location() {
    }

    public Location(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

public class Controller {
    private final static long PingInterval = 100;
    private final static long LiveInterval = 10000;
    private final static long MaxRecordInterval = 10000;
    private final static long BatteryLevel = 10;
    private final static long CollectInterval = 1000;
    private final static String START = "START";
    private final static String STOP = "STOP";
    private final static String SEND = "SEND";
    private final static String SENDCOMP = "SENDCOMP";

    private static Map<String, MobilePhone> clientList = Collections.synchronizedMap(new HashMap<String, MobilePhone>());

    public static void startSensing(String imei) {
        System.out.println("Start sensing");
        if (clientList.get(imei).isSensing) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(START);
        sb.append(",");
        sb.append("1000");
        clientList.get(imei).isSensing = true;
        //clientList[imei].lastStartTimeStamp = System.currentTimeMillis();
        clientList.get(imei).sendMessage(sb.toString());
    }

    public static void stopSensing(String imei) {
        if (!clientList.get(imei).isSensing) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(STOP);
        clientList.get(imei).isSensing = false;
        clientList.get(imei).sendMessage(sb.toString());
    }

    public static void sendToDB(String imei, boolean doCompressed) {
        if (!clientList.containsKey(imei)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (doCompressed) {
            sb.append(SENDCOMP);
        } else {
            sb.append(SEND);
        }
        clientList.get(imei).sendMessage(sb.toString());
    }

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
        boolean isStopped=false;
        long time = System.currentTimeMillis();
        System.out.println("Thread about to start");
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                while (!isStopped) {
                    if (clientList.size()==0){
                        continue;
                    }
                    for (String imei : clientList.keySet()) {
                        if (clientList.get(imei).lastPingTimeStamp > LiveInterval) {
                            clientList.remove(imei);
                            continue;
                        }
                        if (clientList.get(imei).batteryLevel < BatteryLevel) {
                            stopSensing(imei);
                            continue;
                        }
                        startSensing(imei);
                        clientList.get(imei).lastPingTimeStamp=System.currentTimeMillis();
                    }

                    try {
                        Thread.sleep(PingInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t1.start();
        System.out.println("Thread passed");

    }
}
