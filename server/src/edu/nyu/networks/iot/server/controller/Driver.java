package edu.nyu.networks.iot.server.controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.nyu.networks.iot.server.controller.MessageReader;

/**
 * Driver class for the server to accept incoming client connections.
 *
 * A driver object is instantiated and run as its own thread, listening on the given port for incoming connections,
 * performing pre-processing on those connections such as receiving vital client info, and spawns a new thread for
 * handling client connections.
 *
 * @author Wesley Painter
 *
 */
public class Driver implements Runnable {

	private int serverPort = 1234;
	private ServerSocket serverSocket = null;
	private boolean isStopped = false;
	private Map<String, MobilePhone> clientList = null;

	/**
	 * Instantiates driver
	 *
	 * @param port Port to listen to
	 * @param clientList Synchronized map used to store and retrieve connected clients
	 */
	public Driver(int port, Map<String, MobilePhone> clientList) {
		this.serverPort = port;
		this.clientList = clientList;
	}

	/**
	 * Run the driver.
	 *
	 * Listens for incoming connections on designated port and adds new connections to the client list and starts a new
	 * thread for each client.
	 */
	@Override
	public void run() {

		try {
			this.serverSocket = new ServerSocket(this.serverPort);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port " + this.serverPort, e);
		}

		while(!isStopped()) {
			System.out.println("HI");
			Socket clientSocket = null;

			try {
				clientSocket = this.serverSocket.accept();
			} catch (IOException e) {
				continue;
			}

			MobilePhone client = new MobilePhone(clientSocket);
			client.sendMessage("OPEN\r\n");
			client.read();
			client.read();
            JsonObject messageObject = MessageReader.readMessage(client.read());

			// make sure client opened with keep-alive
			if (!MessageReader.isKeepAlive(messageObject)) {
				client.sendMessage("Invalid keep-alive. Try again");
			}

			// get imei and sensors from initial keep-alive message
			String imei = messageObject.get("i_m_e_i").toString();
			List<String> sensors = MessageReader.extractSensorList(messageObject);
			client.setSensors(sensors);

			clientList.put(imei, client);
			new Thread(client).start();
		}
	}

	/**
	 * Check if the driver is running.
	 *
	 * @return boolean value if driver is running
	 */
	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	/**
	 * Stop the driver.
	 */
	public synchronized void stop() {
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}
}
