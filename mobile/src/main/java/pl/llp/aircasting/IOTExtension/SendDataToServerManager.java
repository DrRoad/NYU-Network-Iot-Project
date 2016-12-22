package pl.llp.aircasting.IOTExtension;
import static pl.llp.aircasting.util.http.HttpBuilder.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.inject.Inject;

import pl.llp.aircasting.helper.GZIPHelper;
import pl.llp.aircasting.model.MeasurementStream;
import pl.llp.aircasting.model.Session;
import pl.llp.aircasting.storage.repository.SessionRepository;

public class SendDataToServerManager {
	@Inject ServerConnectionManager connectionManager;
	@Inject SessionRepository sessionRepository;
	@Inject Gson gson;
	@Inject GZIPHelper gziphelper;
	
	public String sendAllData(Date startDate, Date endDate){
		List<Session> sessionData = sessionRepository.allCompleteSessions();
		List<Session> filteredSessionData = new ArrayList<Session>();
		for (Session s: sessionData) {
			if (s.getStart().after(startDate) && s.getStart().before(endDate)){
				filteredSessionData.add(s);
			}
		}
		String allSessionDataBetweenDates = gson.toJson(filteredSessionData);
		return allSessionDataBetweenDates;
		
	}
	
	public String SendDatafromSpecificSensor (Date startDate, Date endDate, String sensor_name){
		List<Session> sessionData = sessionRepository.allCompleteSessions();
		List<Session> filteredSessionData = new ArrayList<Session>();
		for (Session s: sessionData) {
			if (s.getStart().after(startDate) && s.getStart().before(endDate)){
				for(MeasurementStream m:s.getMeasurementStreams()) {
					if (m.getSensorName().equalsIgnoreCase(sensor_name))
						filteredSessionData.add(s);
				}
				
			}
		}
		String allSessionDataBetweenDates = gson.toJson(filteredSessionData);
		return allSessionDataBetweenDates;
	}
	
	public String sendAllData(){
		List<Session> sessionData = sessionRepository.allCompleteSessions();
		String allSessionDataBetweenDates = gson.toJson(sessionData);
		return allSessionDataBetweenDates;		
	}
	
	public String sendLastSession(){
		List<Session> sessionData = sessionRepository.allCompleteSessions();
		String lastSessionJson = gson.toJson(sessionData.get(sessionData.size()-1));
		return lastSessionJson;		
	}
	
	public String sendLastSessionFullyLoaded(boolean compressed){
		if (!compressed){
			List<Session> sessions = sessionRepository.allCompleteSessions();
			Session last = sessions.get(sessions.size()-1);
			last = sessionRepository.loadFully(last.getUUID());
			String lastSessionJson = gson.toJson(last);
			return lastSessionJson;
		}else{
			List<Session> sessions = sessionRepository.allCompleteSessions();
			Session last = sessions.get(sessions.size()-1);
			last = sessionRepository.loadFully(last.getUUID());
			String zipped;
			try {
				zipped = new String(gziphelper.zippedSession(last));
			} catch (IOException e) {
				return "error zipping";
			}
			return zipped;
		}
		
	}
	

}