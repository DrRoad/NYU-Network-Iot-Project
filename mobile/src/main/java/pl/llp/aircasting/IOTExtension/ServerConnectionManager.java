package pl.llp.aircasting.IOTExtension;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
//import org.json.JSONObject;
import org.json.JSONObject;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import android.os.*;
import pl.llp.aircasting.helper.SettingsHelper;

public class ServerConnectionManager {

	@Inject
	SettingsHelper helper;
	@Inject
	EventBus eventBus;

	private final String ACCEPT_HEADER = "Accept";
	private final String ACCEPT_HEADER_VALUE = "application/json";
	private final String CONTENT_HEADER = "Content-type";
	private final String CONTENT_HEADER_VALUE = "application/json";

	public String sendDataToServer(String jsonData) {
		// Bypass network on Main Thread policy
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String hostUrl = "http://" + helper.getBackendURL() + ":" + helper.getBackendPort() + "/";
			HttpPost post = new HttpPost(hostUrl);
			StringEntity stringEntity = new StringEntity(jsonData);
			post.setEntity(stringEntity);
			post.setHeader(ACCEPT_HEADER, ACCEPT_HEADER_VALUE);
			post.setHeader(CONTENT_HEADER, CONTENT_HEADER_VALUE);
			HttpResponse response = httpClient.execute(post);
			HttpEntity httpEntity = response.getEntity();
			String theString = IOUtils.toString(httpEntity.getContent());
			//JSONObject jObj = new JSONObject(theString);
		//	if (eventBus != null) {
				//eventBus.post(new IOTServerCommandEvent(theString));
			//	return "CMD:"+theString;
			//}
			return "DONE "+theString;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "UEE";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "IOE";
		} catch (Exception e) {
			return "EX" + e.getMessage();
		}

	}
}