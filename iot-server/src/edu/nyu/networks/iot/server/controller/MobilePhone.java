package edu.nyu.networks.iot.server.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.JsonObject;

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
    // if you are iterating, make sure to wrap iteration in a synchronized block
    private List<String> sensors = Collections.synchronizedList(new ArrayList<String>());

    // Concurrent Linked List of received messages
    private ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<String>();

    // Only get/set these variables through getters and setters
    // If you need to do any operation but get and set, use a synchronized method and remove volatile keyword
    Location location;
    volatile boolean isSensing;
    volatile float speed;
    volatile float noise;
    volatile float pm;
    volatile int sendFrequency;
    volatile boolean isSendingData;
    volatile long lastStartTimeStamp;
    volatile long lastPingTimeStamp;
    volatile long batteryLevel;
    volatile int tickRound;

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
            String inString1 = read();
            if (inString != null) {
                String message;
                int messageLength = 0;
                boolean isCompress = false;
                try {
                    messageLength = Integer.parseInt(inString);
                   // System.out.println("Length: "+messageLength);
                    isCompress = Boolean.parseBoolean(inString1);
                  //  System.out.println("Compressed: "+isCompress);
                } catch (NumberFormatException e) {
                    message = inString;
                }
                try {
                    message = readLength(messageLength, isCompress);
                } catch (Exception e) {
                    message = inString;
                }
               // System.out.println("Message:"+message);
                if (!message.equals(""))
                	messageQueue.add(message);
            }
        }
    }

    /**
     * Send a message over associated socket to the client.
     *
     * This publicly accessible method is used to construct the final message format. The length of the message with
     * a carriage return is the only header field, delimited by a \r\n.
     *
     * @param message string message to be sent.
     */
    public void sendMessage(String message) {
        write(message);
    }

    /**
     * Read a message from the socket buffer
     *
     * Use this method from the controller to check for a message, and if there is one, get it as a JsonObject.
     *
     * @return top message from the queue as a JsonObject, or null if none present
     */
    public JsonObject readMessage() {
        String message = messageQueue.poll();
        if (message == null) {
            return null;
        }
        return MessageReader.readMessage(message);
    }

    public int getQueueSize(){
        return this.messageQueue.size();
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

    public String read() {
        String message;

        try {
            message = in.readLine();
        } catch (IOException e) {
            return "ERROR READING IN";
        }

        message = message.trim();
        return message;
    }

    private String readLength(int bytes, Boolean isCompress) throws Exception {
        char[] buffer = new char[bytes];

        try {
            in.read(buffer,0, bytes);
        } catch (IOException e) {
            return "ERROR READING IN";
        }
        if (isCompress) {
            return MessageReader.decompress(new String(buffer));
        }
        return new String(buffer);
    }

    private void write(String message) {
    	//System.out.println("WRITING message to client");
        out.println(message);
        out.flush();
    }

    public void setSensors(List<String> sensors) {
        synchronized (this.sensors) {
            this.sensors = sensors;
        }
    }
}
