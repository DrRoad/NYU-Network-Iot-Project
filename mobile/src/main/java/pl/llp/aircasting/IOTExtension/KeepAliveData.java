package pl.llp.aircasting.IOTExtension;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.inject.Inject;

import pl.llp.aircasting.model.Sensor;
import pl.llp.aircasting.model.SensorManager;

public class KeepAliveData {
	
	@Inject SensorManager sensorManager;

	@Expose
	private String IMEI;
	@Expose
	private int batteryLife;
	@Expose
	private boolean keepAliveStatus;
	@Expose
	private double lon;
	@Expose
	private double lat;
	@Expose
	private String ip;
	@Expose 
	private double noise;
	@Expose 
	boolean isData = false;
	@Expose
	List<String> sensors;
	
	public String getIMEI() {
		return IMEI;
	}
	public void setIMEI(String iMEI) {
		IMEI = iMEI;
	}
	public float getBatteryLife() {
		return batteryLife;
	}
	public void setBatteryLife(int batteryLife) {
		this.batteryLife = batteryLife;
	}
	public boolean isKeepAliveStatus() {
		return keepAliveStatus;
	}
	public void setKeepAliveStatus(boolean keepAliveStatus) {
		this.keepAliveStatus = keepAliveStatus;
	}
	public double getlon() {
		return lon;
	}
	public void setlon(double lon) {
		this.lon = lon;
	}
	public double getlat() {
		return lat;
	}
	public void setlat(double lat) {
		this.lat = lat;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public void setAudioValue(double val) {
		this.noise = val;
	}
	public void setSensors(List<String> sensors){
		this.sensors = sensors;
	}
	
	
	
	
	

}