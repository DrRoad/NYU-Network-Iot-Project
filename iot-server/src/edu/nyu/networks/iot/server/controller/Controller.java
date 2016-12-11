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
