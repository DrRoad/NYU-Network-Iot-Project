package pl.llp.aircasting.IOTExtension;

import org.json.JSONException;
import org.json.JSONObject;

public class IOTServerCommandEvent {

	public String command;
	
	public IOTServerCommandEvent(String cmd) {
		this.command = cmd;
	}

	public String getCommand(){
		return command;
	}
	
}