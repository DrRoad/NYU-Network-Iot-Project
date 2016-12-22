package pl.llp.aircasting.IOTExtension;

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
 * Created by zalbhathena on 12/8/16.
 */

public class ClientTransmitter implements Runnable {
	private Socket socket = null;
	private Object keepAlivePayload;
	private long keepAliveFrequency = -1;

	private PrintWriter out = null;
	private long lastMessageTime = Long.MIN_VALUE;

	public ClientTransmitter(Socket socket, Object keepAlivePayload, long keepAliveFrequency) {
		setSocket(socket);
		setKeepAliveFrequency(keepAliveFrequency);
		setKeepAlivePayload(keepAlivePayload);

	}

	public void run() {
		long currentTime = System.currentTimeMillis();
		
		while (true) {
			if (currentTime >= lastMessageTime + keepAliveFrequency) {
				String message = keepAlivePayload.toString();
				if (!message.equals("")) // was having some issues with this....
					write(keepAlivePayload.toString());
				else{
					continue;
				}

				long tempTime = System.currentTimeMillis();
				try {
					Thread.sleep(currentTime + keepAliveFrequency - tempTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			currentTime = System.currentTimeMillis();
		}
	}

	public boolean write(String message) {
		if (out != null) {
			out.println(message);
			out.flush();
			return true;
		}
		return false;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
		if (this.socket != null) {
			try {
				out = new PrintWriter(this.socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setKeepAlivePayload(Object keepAlivePayload) {
		this.keepAlivePayload = keepAlivePayload;
	}

	public void setKeepAliveFrequency(long keepAliveFrequency) {
		this.keepAliveFrequency = keepAliveFrequency;
	}
}