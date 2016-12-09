package edu.nyu.networks.iot.server.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;


/**
 * Threaded client object for control of connected mobile phone clients.
 * <p>
 * A new mobile phone object is created when a client establishes a connection with the driver class.
 *
 * @author Wesley Painter
 */
class MobilePhone implements Runnable {

    private Socket clientSocket = null;
    private BufferedReader in = null;
    private PrintWriter out = null;
    private String imei = null;
    private String type=null;
    String ip;
    Location location;
    boolean isSensing;
    float speed;
    float noise;
    float pm;
    long lastStartTimeStamp;
    long lastPingTimeStamp;
    long batteryLevel;
    List<String> sensors;


    MobilePhone(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.location = new Location(0, 0);
        try {
            in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        write("Connection open.");

        while (true) {
            read();
            //TODO: handle client messages here
            Controller.update(imei, this.type,this);
        }
    }

    /**
     * Send a message over associated socket to the client
     *
     * @param message string message to be sent.
     */
    public void sendMessage(String message) {
        write(message);
    }

    /**
     * Read a message from the socket
     *
     * @return message
     */
    public String readMessage() {
        read();
        return this.imei;
    }

    public String getImei() {
        return this.imei;
    }

    public boolean closeSocket() {
        try {
            this.clientSocket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    private boolean read() {
        try {
            String indata = in.readLine();
            JsonObject tokens=indata.parseJson();
            this.imei=JsonObject.get("i_m_e_i");
            this.sensors=JsonObject.get("avail_sensors");
            this.type=JsonObject.get("type");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void write(String message) {
        System.out.println(message);
        out.println(message);
        out.flush();
    }

}
