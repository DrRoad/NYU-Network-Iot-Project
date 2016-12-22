package pl.llp.aircasting.IOTExtension;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ExecutionError;
import com.google.gson.Gson;
import com.google.inject.Inject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.widget.Toast;
import pl.llp.aircasting.Intents;
import pl.llp.aircasting.helper.LocationHelper;
import pl.llp.aircasting.helper.SettingsHelper;
import pl.llp.aircasting.model.Sensor;
import pl.llp.aircasting.model.SensorManager;
import pl.llp.aircasting.model.events.SensorEvent;
import roboguice.service.RoboService;

/*
 * @Author Soumie Kumar
 * For Foundations of Networking class in Fall 2016
 * 
 */
public class KeepAliveService extends RoboService {

	@Inject
	Gson gson;
	@Inject
	ServerConnectionManager connManager;
	@Inject
	LocationHelper locationHelper;
	@Inject
	Context context;
	@Inject SettingsHelper helper;
	
	@Inject EventBus eventBus;
	
	private double currentAudioValue = 0.0;
	
	private ClientController controller;
		
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		eventBus.register(this);
		// Start Networking Thread
		controller = new ClientController(helper.getBackendURL(),helper.getBackendPort(),eventBus,this,1000,context);
		Thread t = new Thread(controller);
		t.start();
		return START_NOT_STICKY;
	}

	@Override
	public String toString(){
		KeepAliveData keepAliveData = new KeepAliveData();
		keepAliveData.setIMEI(getIMEI());
		keepAliveData.setBatteryLife(getBatteryLife());
		keepAliveData.setIp(getPhoneIP());
		keepAliveData.setKeepAliveStatus(getKeepAliveStatus());
		keepAliveData.setlon(getLocation().getLongitude());
		keepAliveData.setlat(getLocation().getLatitude());
		keepAliveData.setAudioValue(this.currentAudioValue);;
		keepAliveData.setSensors(sensorList());
		String keepAliveDataJson = gson.toJson(keepAliveData);
		int size = keepAliveDataJson.getBytes().length;
		String message = size+"\r\n"+keepAliveData.isData+"\r\n"+keepAliveDataJson;
		return message;
	}
	
	public void sendKeepAliveData(){
		KeepAliveData keepAliveData = new KeepAliveData();
		keepAliveData.setIMEI(getIMEI());
		keepAliveData.setBatteryLife(getBatteryLife());
		keepAliveData.setIp(getPhoneIP());
		keepAliveData.setKeepAliveStatus(getKeepAliveStatus());
		keepAliveData.setlon(getLocation().getLongitude());
		keepAliveData.setlat(getLocation().getLatitude());
		keepAliveData.setAudioValue(this.currentAudioValue);;
		String keepAliveDataJson = gson.toJson(keepAliveData);
		int size = keepAliveDataJson.getBytes().length;
		String message = Integer.toString(size)+"\r\n"+keepAliveData.isData+"\r\n"+keepAliveDataJson;
		connManager.sendDataToServer(message);
	}
	
	public String getKeepAliveData(){
		return toString();
	}

	private String getIMEI() {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

	private Location getLocation() {
		Location location = locationHelper.getLastLocation();
		return location;
	}

	private String getPhoneIP() {
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wm.isWifiEnabled()){
			String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
			return ip;
		}
		// Try to get cellular IP
		// Note: Got this code from the web
		// By my searching, this was the default method of getting an IP address while using cellular
		try {
			for (Enumeration<NetworkInterface> enumerator
					= NetworkInterface.getNetworkInterfaces();
					enumerator.hasMoreElements(); ) {
				NetworkInterface nInt = (NetworkInterface) enumerator.nextElement();
				for (Enumeration<InetAddress> enumeratorIpAddress
						= nInt.getInetAddresses(); enumeratorIpAddress.hasMoreElements();) {
					InetAddress inet = enumeratorIpAddress.nextElement();
					if (!inet.isLoopbackAddress()) {
						String ipaddress = inet.getHostAddress().toString();
						return ipaddress;
					}
				}
			}
		} catch (SocketException ex) {
		}
		return "0.0.0.0";
	}
	
	

	private int getBatteryLife() {
		Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float batteryLife = ((float) level / (float) scale) * 100.0f;
		int batt = Math.round(batteryLife);
		return batt;
	}

	private boolean getKeepAliveStatus() {
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Intents.stopKeepAliveService(context); // Redundant?
		eventBus.unregister(this);
	}
	
	/*
	 * Default Method that Updates Microphone KeepAlive value
	 */
	@Subscribe
	public void onEvent(SensorEvent evt){
		String name = evt.getSensorName();
		if (name.equals("Phone Microphone")){
			this.currentAudioValue = evt.getValue();
		}
		
	}
	
	@Inject SensorManager sensorManager;
	private List<String> sensorList() {
		List<String> returnList = new ArrayList<String>();
		if (sensorManager != null) {
			List<Sensor> sensors = sensorManager.getSensors();
			for (Sensor s : sensors) {
				returnList.add(s.getSensorName());
			}
		}
		return returnList;
	}

}