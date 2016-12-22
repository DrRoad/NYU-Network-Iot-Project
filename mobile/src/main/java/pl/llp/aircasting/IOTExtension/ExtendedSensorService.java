package pl.llp.aircasting.IOTExtension;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import pl.llp.aircasting.activity.events.SessionStoppedEvent;
import pl.llp.aircasting.api.SyncDriver;
import pl.llp.aircasting.api.data.SyncResponse;
import pl.llp.aircasting.model.Sensor;
import pl.llp.aircasting.model.SensorManager;
import pl.llp.aircasting.model.Session;
import pl.llp.aircasting.model.SessionManager;
import pl.llp.aircasting.storage.repository.SessionRepository;
import roboguice.service.RoboService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static pl.llp.aircasting.util.http.HttpBuilder.http;
import java.util.*;

import com.google.inject.Inject;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;

/**
 * This will eventually become the IOT Service.....
 * 
 * @author josephzuhusky & Soumie Kumar
 * 
 * 
 *         IOT Wrapper service (extension to AirCasting Android Application)
 *         Authors: Joe Zuhusky & Soumie Kumar Foundations of Networks and
 *         Mobile Systems December 2016
 *
 */

public class ExtendedSensorService extends RoboService implements IOTWrapper {

	/**
	 * --------------------------------------------------------------------
	 * BEGIN SERVICE VARS AND DATA
	 * --------------------------------------------------------------------
	 */

	@Inject
	EventBus eventBus;
	@Inject
	SessionRepository sessionRepository;
	@Inject
	SessionManager sessionManager;
	@Inject
	SyncDriver syncDriver;
	@Inject
	SendDataToServerManager dataToServerManager;

	@Inject
	SensorManager sensorManager;

	Gson gson;
	Context context;

	// Some current IOT Sensor readers
	// Not using AirCasting Sensors at the moment...
	private AccelerometerReader accelReader;
	private BatteryReader battReader;
	private CompassReader compassReader;

	// Implement and add to this list as you see fit
	private List<IOTSensor>        availSensors = new ArrayList<IOTSensor>();
	private Map<String, IOTSensor> iotSensorMap = new HashMap<String, IOTSensor>();

	Date on_service_created;

	/**
	 * Samples = A simple object to hold samples from the Current readers ->
	 * I.e. the accelReader and BattReader
	 */

	/**
	 * Some Sync state info....
	 */
	// MOST OF THIS STUFF NOT USED RIGHT NOW
	private final String SYNC_BATCH = "SYNC_BATCH"; // Sync on server command
	private final String SYNC_AUTO  = "SYNC_AUTO"; // Sync every at every time N

	// Not using stuff below at the moment....
	private String SYNC_MODE    = SYNC_AUTO; // by default...
	private int sync_interval = 30000;     // sync every minute right now... (if
										     // state is ONINTERVAL)

	// ---------------------STATE VARIABLES-------------------------
	// INFO for FSM / FSA that will be controlled by Server...

	private boolean batchSync = true; // Sync on command
	private boolean autoSync = false;
	private boolean compressData = false;
	
	private Thread syncThread = null;

	private boolean isSamplingData = false;
	// private boolean

	// Maybe provide a list of available sensors???
	// NEED A LIST OF AVAILABLE SENSORS>...

	/**
	 * -------------------------------------------------------------------- END
	 * SERVICE VARS AND DATA
	 * 
	 * BEGIN SERVICE & IOT API METHODS
	 * --------------------------------------------------------------------
	 */

	// SERVICE METHOD Implemented
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	// SERVICE METHOD
	@Override
	public void onCreate() {
		super.onCreate();
		eventBus.register(this); // hook up with EventBus...

		// Declare GSON helper and context...
		gson = new Gson();
		context = getApplicationContext();

		// create a new date object of the time when started....
		on_service_created = new Date();

		// Create Sensor Reader objects
		accelReader = new AccelerometerReader(this, eventBus);
		battReader = new BatteryReader(this, eventBus);
		compassReader = new CompassReader(this, eventBus);
		
		availSensors.add(accelReader);
		availSensors.add(battReader);
		availSensors.add(compassReader);
	
		iotSensorMap.put(battReader.getName(), battReader);
		iotSensorMap.put(compassReader.getName(), compassReader);
		iotSensorMap.put(accelReader.getName(), accelReader);
		
		/**
		 * NOTE TO SELF-> I think we're going to have to hard code most of the
		 * Toggle commands for the Bluetooth sensor and audioreader that are
		 * already in place.... Lets save this for another time....
		 */

		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				// Toast.makeText(getApplicationContext(), "IOT SERVICE
				// CREATED", Toast.LENGTH_SHORT).show();
			}
		});

		StartSensing();
	}

	// SERVICE METHOD
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	// SERVICE METHOD
	@Override
	public void onDestroy() {
		super.onDestroy();
		StopSensing();
		eventBus.unregister(this);
	}

	// BEGIN IOT API METHODS

	/**
	 * I think that the logic for this method should be to toggle->ON Aircasting
	 * for all sensors Unless another StartSensing method is called with either
	 * a List<SensorNames<String>> or a simple string sensor name..
	 */

	public void StartSensing() {

		if (!compassReader.isRunning()) {
			compassReader.startSensing();
		}
		if (!accelReader.isRunning()) {
			accelReader.startSensing();
		}
		if (!battReader.isRunning()) {
			battReader.startSensing();
		}
	}

	/**
	 * Calling this Method Turns off ALL IOT extension Sensors...
	 */
	public void StopSensing() {
		if (compassReader.isRunning()) {
			compassReader.stopSensing();
		}
		if (accelReader.isRunning()) {
			accelReader.stopSensing();
		}
		if (battReader.isRunning()) {
			battReader.stopSensing();
		}
	}

	/**
	 * These two methods below havent been used, but are more like prototypes
	 * for the idea that im going after....
	 * 
	 * Something a little more complex might be required for toggling acc-x,y,z
	 * individually....
	 */
	@Override
	public void SensorActivate(String name) {
		name = name.toLowerCase(); // this should be conventional???
		if (iotSensorMap.containsKey(name)) {
			IOTSensor current = iotSensorMap.get(name);
			current.startSensing();
		} else {
			if (sensorManager.getSensors().contains(name)) {
				Sensor acSensor = sensorManager.getSensorByName(name);
				if ( acSensor.getSensorName().equals("Phone Microphone")){
					sessionManager.startAudio();
				}
			}
		}

	}

	@Override
	public void SensorDeactivate(String name) {
		name = name.toLowerCase();
		// First check if sensor is IOT Sensor
		if (iotSensorMap.containsKey(name)) {
			iotSensorMap.get(name).stopSensing();
		}

	}

	/**
	 * API exposes the ability to send all cached data between 2 times specified
	 * by the Server
	 * 
	 * @param d1
	 *            from this time
	 * @param d2
	 *            to this time
	 */

	public void sendAllData(Date d1, Date d2) {
		dataToServerManager.sendAllData(d1, d2);
	}

	public void sendCompressedData(Date d1, Date d2) {
		// Input Logic here
	}

	/**
	 * Need to use units of samples per second for T
	 */
	public void sendSampledData(Date t1, Date t2, int T) {
		// Put logic here
		// I guess we could loop through the measurements and
		// only take the measurements in time increments of 1/T???

	}

	public void addSensor() {
		// Not really sure about logic here
	}

	public void SendDatafromSpecificSensor(Date t1, Date t2, String sensor_name) {
		dataToServerManager.SendDatafromSpecificSensor(t1, t2, sensor_name);
	}
	
	/**
	 * 
	 * @param evt
	 * 
	 * This method is used to stop the running threads 
	 * in the event that the owner of the phone wishes to stop sensing data
	 */
	@Subscribe
	public void onEvent(UserStopSensingEvent evt) {
		try{
			if (syncThread.isAlive()){
				syncThread.interrupt();
				syncThread.join();
			}
			isSamplingData = false;
			for (IOTSensor s : availSensors) {
				s.setSamplingRate(1); // back to default??!
			}
			// Set audio freq
			if (sessionManager != null) {
				sessionManager.setAudioSamplingFrequency(1);
			}
		} catch(InterruptedException e){
			Toast.makeText(context, "Couldnt Stop Thread", Toast.LENGTH_SHORT).show();
		}
		
	}
	// Received a Command From the server
	// This event is posted to the bus in client controlled
	// when callback.callback is called with the payload...

	@Subscribe
	public void onEvent(IOTServerCommandEvent evt) {
			
		//return;
		try {
			JSONObject cmd = new JSONObject(evt.getCommand());
			Toast.makeText(getApplicationContext(), evt.getCommand(), Toast.LENGTH_SHORT).show();
			if (!cmd.has("messageType")) {
				Toast.makeText(getApplicationContext(), "Err -> Received Command w/ no Type", Toast.LENGTH_SHORT)
						.show();
				return;
			}
			String cmd_type = cmd.getString("messageType");
			// Check if wants compressed (cant do this rn)
			if (cmd.has("compress")){
				this.compressData = cmd.getBoolean("compress");
			}
			// Okay now see what type of command it is
			// Can't use switch here, need JRE 1.7 (app uses 1.6)
			if (cmd_type.equals("START") && !isSamplingData) {
				if (cmd.has("frequency")){
					int rate = cmd.getInt("frequency");
					for (IOTSensor s : availSensors){
						s.setSamplingRate(rate);
					}
					sessionManager.setAudioSamplingFrequency(rate);
				}
				if (cmd.has("period")){
					sync_interval   = cmd.getInt("period");
					final int interval = sync_interval;
					final Handler h = new Handler(Looper.getMainLooper());
					final EventBus tempEB = this.eventBus;
					syncThread = new Thread(new Runnable(){
						public void run(){
							while(true){
								try {
									Thread.sleep(interval/2);
									h.post(new Runnable(){
										public void run(){
											tempEB.post(new IOTToggleAirCastingEvent()); // turn off
										}
									});
									sendLastSession();
									Thread.sleep(interval/2);
									h.post(new Runnable(){
										public void run(){
											tempEB.post(new IOTToggleAirCastingEvent()); // start anew
										}
									});
								} catch (InterruptedException e) {
							        break; 
								}
							}
						}
					});
					if (interval > 2000){
						syncThread.start();	
					}
				}
				eventBus.post(new IOTToggleAirCastingEvent());
				isSamplingData = true;
			}
			// End start command
			// Being Stop command
			else if (cmd_type.equals("STOP") && isSamplingData) {
				if (syncThread.isAlive()){
					syncThread.interrupt();
					syncThread.join();
				}
				if ( sessionManager.isRecording() ){
					eventBus.post(new IOTToggleAirCastingEvent());
				}// else just do nothing, already stopped
				isSamplingData = false;
				for (IOTSensor s : availSensors) {
					s.setSamplingRate(1); // back to default??!
				}
				// Set audio freq
				if (sessionManager != null) {
					sessionManager.setAudioSamplingFrequency(1);
				}
			}
			else if (cmd_type.equals("send")) {
				String command = cmd.getString("command");
				
				if ( command.equals("last")){
					sendLastSession();
				}
				else if (command.equals("all")){
					sendSampledData();
				}
				else if (command.equals("time series")){
					// do stuff...
				}
				else{
					sendSampledData();
				}
			}
			else if (cmd_type.equals("control")) {
				if (cmd.has("command")){
					String command = cmd.getString("command");
					if ( command.equals("set frequency")){
						int rate = cmd.getInt("frequency");
						for (IOTSensor s : availSensors){
							s.setSamplingRate(rate);
						}
						if (sessionManager != null){
							sessionManager.setAudioSamplingFrequency(rate);
						}
					}
					else if (command.equals("set keep alive frequency")){
						
					}
				}
			}
			else if (cmd_type.equals("add") && cmd.has("sensors")) {
				//Toast.makeText(getApplicationContext(), "add", Toast.LENGTH_SHORT).show();
				JSONArray sensors = cmd.getJSONArray("sensors");
				// Only for IOT Sensors ATM....
				for (int i=0; i<sensors.length(); i++){
					String sensorName = sensors.getString(i);
					if ( iotSensorMap.containsKey(sensorName) ){
						iotSensorMap.get(sensorName).startSensing();
					}
					// Poss accel components that wouldnt be in iotSensorMap
					// Because they are components of the ACC
					else if (sensorName.equals("accelerometer-x")){
						AccelerometerReader x = (AccelerometerReader) iotSensorMap.get(accelReader.getName());
						x.startX();
					}
					else if (sensorName.equals("accelerometer-y")){
						AccelerometerReader y = (AccelerometerReader) iotSensorMap.get(accelReader.getName());
						y.startY();
					}
					else if (sensorName.equals("accelerometer-z")){
						AccelerometerReader y = (AccelerometerReader) iotSensorMap.get(accelReader.getName());
						y.startZ();
					}else if (sensorName.equals("Phone Microphone")){
						sessionManager.startAudio();
					}
				} 
			}
			else if (cmd_type.equals("remove") && cmd.has("sensors")) {
				//Toast.makeText(getApplicationContext(), "remove", Toast.LENGTH_SHORT).show();
				JSONArray sensors = cmd.getJSONArray("sensors");
				// Only for IOT Sensors ATM....
				for (int i=0; i<sensors.length(); i++){
					String sensorName = sensors.getString(i);
					if ( iotSensorMap.containsKey(sensorName) ){
						iotSensorMap.get(sensorName).stopSensing();
					}
					// Poss accel components that wouldnt be in iotSensorMap
					// Because they are components of the ACC
					else if (sensorName.equals("accelerometer-x")){
						AccelerometerReader x = (AccelerometerReader) iotSensorMap.get(accelReader.getName());
						x.stopX();
					}
					else if (sensorName.equals("accelerometer-y")){
						AccelerometerReader y = (AccelerometerReader) iotSensorMap.get(accelReader.getName());
						y.stopY();
					}
					else if (sensorName.equals("accelerometer-z")){
						AccelerometerReader y = (AccelerometerReader) iotSensorMap.get(accelReader.getName());
						y.stopZ();
					}
					else if (sensorName.equals("Phone Microphone")){
						sessionManager.stopAudio();
					}
				} 
			}

			else if (cmd.has("time")){
				// NEED INFO FROM SERVER....
			}

		} catch (JSONException e) {
			//Toast.makeText(getApplicationContext(), "JSON Exception on ServerCmd", Toast.LENGTH_SHORT).show();

		} catch (Exception e) {
			//Toast.makeText(getApplicationContext(), "Exception on ServerCmd "+e.toString(), Toast.LENGTH_SHORT).show();
		} 
	}
	


	public void sendSampledData() {
		dataToServerManager.sendAllData();
	}
	
	public void sendLastSession() {
		Handler h = new Handler(Looper.getMainLooper());
		h.post(new Runnable(){
			public void run(){
				Toast.makeText(context, "Sending Data", Toast.LENGTH_SHORT);
			}
		});
		eventBus.post(new SendDataToServerEvent(dataToServerManager.sendLastSession()));
	}

	public void sendControl(IOTSensor X) {
		// put logic here
	}

	private List<String> sensorList() {
		List<String> returnList = new ArrayList<String>();
		if (sensorManager != null) {
			List<Sensor> sensors = sensorManager.getSensors();
			for (Sensor s : sensors) {
				returnList.add("s.getSensorName()");
			}
		}
		return returnList;
	}
	
	public List<String> runningSensors(){
		List<String> ret = new ArrayList<String>();
		for (IOTSensor s : availSensors){
			if (s.running){
				ret.add(s.getName());
			}
		}
		return ret;
	}
	
}
