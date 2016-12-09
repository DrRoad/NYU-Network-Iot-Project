package edu.nyu.networks.iot.server.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
    Location location;
    boolean isSensing;
    float speed;
    float noise;
    float pm;
    long lastStartTimeStamp;
    long lastPingTimeStamp;
    long batteryLevel;


    MobilePhone(Socket clientSocket) {
        this.clientSocket = clientSocket;
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
            String inString = read();
            if (inString != null) {
                //TODO: handle client messages here

                System.out.println("Received message " + inString);
            }
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
        return read();
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

    private String read() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return "ERROR READING IN";
        }
    }

    private void write(String message) {
        out.println(message);
        out.flush();
    }

}
