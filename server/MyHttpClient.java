import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;

public class MyHttpClient {

    private static final String USER_AGENT = "Mozilla/5.0";
    
    private static final String POST_URL = "http://localhost:18001";
    
    private static final String POST_PARAMS = "userName=Shiva";

    public static void main(String[] args) throws IOException {

	sendPost();
	System.out.println("POST sent.\n");
    }

    private static void sendPost() throws IOException {
	URL obj = new URL(POST_URL);
	HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	con.setRequestMethod("POST");
	con.setRequestProperty("User-Agent", USER_AGENT);

	con.setDoOutput(true);
	OutputStream os = con.getOutputStream();
	os.write(POST_PARAMS.getBytes());
	os.flush();
	os.close();

	int responseCode = con.getResponseCode();
	System.out.println("POST Response Code :: " + responseCode);

	if (responseCode == HttpURLConnection.HTTP_OK) {
	    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    String inputLine;
	    StringBuffer response = new StringBuffer();

	    while ((inputLine = in.readLine()) != null)
		response.append(inputLine);

	    in.close();

	    System.out.println(response.toString());
	}

	else
	    System.out.println("POST Request Failed.");
    }
}
