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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Created by zalbhathena on 12/7/16.
 */

public class ClientController implements Runnable {
	private String serverIP;
	private int serverPort;

	private Socket socket = null;
	private BufferedReader in = null;

	private boolean isConnected = false;
	private long lastPing = -1;

	private final int SLEEP_TIME = 500;
	private final long TIMEOUT_THRESHOLD = 5000;

	Context context;
	EventBus eventBus;

	private ClientTransmitter clientTransmitter = null;

	public ClientController(String serverIP, int serverPort, EventBus eventBus, Object keepAlivePayload,
			long keepAliveFrequency, Context context) {
		//callback.callback(serverIP+" "+serverPort);
		this.serverIP   = serverIP;
		this.serverPort = serverPort; //serverPort;
		this.eventBus = eventBus;
		clientTransmitter = new ClientTransmitter(null, keepAlivePayload, keepAliveFrequency);
		this.context = context;
		eventBus.register(this);
		Toast.makeText(this.context, "Call connect", Toast.LENGTH_SHORT).show();
	}

	int count = 0;
	
	public boolean connect() {
		lastPing = -1;
		isConnected = false;
		socket = null;
		in = null;
		try {
			InetAddress serverAddr = InetAddress.getByName(serverIP);
			socket = new Socket(serverAddr, serverPort);
			in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			lastPing = System.currentTimeMillis();
			return true;
		} catch (UnknownHostException e1) {
			//Toast.makeText(context, "UHE", Toast.LENGTH_SHORT).show();
			return false;
		} catch (IOException e1) {
			//Toast.makeText(context, "IOE", Toast.LENGTH_SHORT).show();
			return false;
		} catch (NullPointerException e1) {
			//Toast.makeText(context, "NPTR", Toast.LENGTH_SHORT).show();
			return false;
		}
	}

	public void setKeepAlivePayload(Object keepAlivePayload) {
		clientTransmitter.setKeepAlivePayload(keepAlivePayload.toString());
	}

	public void setKeepAliveFrequency(long keepAliveFrequency) {
		clientTransmitter.setKeepAliveFrequency(keepAliveFrequency);
	}
	
	@Subscribe
	public void onEvent(SendDataToServerEvent evt){
		final String payload = evt.getMsg();
		Handler h = new Handler(Looper.getMainLooper());
		h.post(new Runnable(){
			public void run(){
				boolean done = write(payload);
				Toast.makeText(context, "Sending Data to Server: "+done, Toast.LENGTH_SHORT).show();
			}
		});
		
	}

	public boolean write(String payload) {
		if (clientTransmitter != null) {
			return clientTransmitter.write(payload);
		}
		return false;
	}

	public boolean isConnected() {
		return isConnected;
	}

	int loop = 0;

	public int loopNum() {
		return loop;
	}

	@Override
	public void run() {
		while (true) {
			try {
			// waiting for initial response back from server
				if (socket == null){
					while (!connect()){
						try{
							Handler mHandler = new Handler(Looper.getMainLooper());
							mHandler.post(new Runnable(){
								public void run(){
									Toast.makeText(context, "Trying to Connect to IoT Server", Toast.LENGTH_SHORT).show();
								}
							});
							Thread.sleep(4000);
						}catch(Exception e){
							continue;
						}
					}
				}
				if (in.ready()) {
					final String inString = in.readLine();
					if (isConnected) {
						Handler mHandler = new Handler(Looper.getMainLooper());
						mHandler.post(new Runnable(){
							public void run(){
								Toast.makeText(context, inString, Toast.LENGTH_SHORT).show();
								eventBus.post(new IOTServerCommandEvent(inString));
							}
						});
					}
					// original connection part of handshake
					else if (inString.equals("OPEN") || inString.equals("OPEN\r\n")) {
						clientTransmitter.setSocket(socket);
						new Thread(clientTransmitter).start();
						Handler mHandler = new Handler(Looper.getMainLooper());
						mHandler.post(new Runnable(){
							public void run(){
								Toast.makeText(context, "Opened", Toast.LENGTH_SHORT).show();
							}
						});
						isConnected = true;
					}
					// if handshake takes too long
					else if (System.currentTimeMillis() - lastPing > TIMEOUT_THRESHOLD) {
						throw new TimeoutException("Timed out when initially connecting to server.");
					}
				} else {
					Thread.sleep(SLEEP_TIME);
				}
			}catch (UnknownHostException e) {
				//Toast.makeText(context, "UHE", Toast.LENGTH_SHORT).show();
				isConnected = false;
			} catch (IOException e) {
				//Toast.makeText(context, "IOE", Toast.LENGTH_SHORT).show();
				isConnected = false;
				continue;
			} catch (NullPointerException e) {
				//Toast.makeText(context, "NULLPTR", Toast.LENGTH_SHORT).show();
			} catch (TimeoutException e) {
				//Toast.makeText(context, "TEX", Toast.LENGTH_SHORT).show();
				continue;
			} catch (Exception e) {
				//Toast.makeText(context, "EX", Toast.LENGTH_SHORT).show();
				isConnected = false;
				continue;
			}
		} 

	}
}