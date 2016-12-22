package edu.nyu.networks.iot.client.controller;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;


/**
 * Created by zalbhathena on 12/7/16.
 */

public class ClientController implements Runnable{
    private String serverIP = "192.168.1.255";
    private int serverPort = 9001;

    private Socket socket =  null;
    private BufferedReader in = null;

    private Callbackable callback;

    private boolean isConnected = false;
    private long lastPing = -1;

    private final int SLEEP_TIME = 500;
    private final long TIMEOUT_THRESHOLD = 5000;

    private ClientTransmitter clientTransmitter = null;

    public ClientController(String serverIP, int serverPort, Callbackable callback,
                            Object keepAlivePayload, long keepAliveFrequency) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.callback = callback;
        clientTransmitter = new ClientTransmitter(null, keepAlivePayload, keepAliveFrequency);
    }

    public void connect()
    {
        lastPing = -1;
        isConnected = false;
        socket = null;
        in = null;
        try {
            InetAddress serverAddr = InetAddress.getByName(serverIP);
            socket = new Socket(serverAddr, serverPort);
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            lastPing = System.currentTimeMillis();
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void setKeepAlivePayload(Object keepAlivePayload)
    {
        clientTransmitter.setKeepAlivePayload(keepAlivePayload);
    }

    public void setKeepAliveFrequency(long keepAliveFrequency)
    {
        clientTransmitter.setKeepAliveFrequency(keepAliveFrequency);
    }

    public boolean write(String payload)
    {
        if(clientTransmitter != null)
        {
            return clientTransmitter.write(payload);
        }
        return false;
    }

    public boolean isConnected()
    {
        return isConnected;
    }

    public void run(){
        connect();
        try {
            //waiting for initial response back from server
            while(true)
            {
                if(in.ready()) {
                    String inString = in.readLine();
                    if (isConnected) {
                        callback.callback(inString);
                    }
                    //original connection part of handshake
                    else if (inString.equals("OPEN") || inString.equals("OPEN/r/n")) {
                        clientTransmitter.setSocket(socket);
                        new Thread(clientTransmitter).start();
                        isConnected = true;
                    }
                    //if handshake takes too long
                    else if (System.currentTimeMillis() - lastPing > TIMEOUT_THRESHOLD) {
                        throw new TimeoutException("Timed out when initially connecting to server.");
                    }
                }
                else {
                    Thread.sleep(SLEEP_TIME);
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
