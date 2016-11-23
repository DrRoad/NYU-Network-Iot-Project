
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

// import com.google.gson.Gson;

public class PostServer {
    
    static final int port = 18002;
    
    public static void main(String[] args) throws Exception {
	HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
	System.out.println("Server started at " + port);
	server.createContext("/", new PostHandler());
	server.setExecutor(null); // creates a default executor
	server.start();
    }
    
    public static class PostHandler implements HttpHandler {
	
	@Override
	public void handle(HttpExchange he) throws IOException {
	    System.out.println("\n--------------------------------------------");
	    System.out.println("Got new POST request -");
	    System.out.println("Sender: " + he.getRemoteAddress().toString());
	    System.out.println("Protocol: " + he.getProtocol().toString());
	    System.out.println("\n** Begin Request body ** \n");
	    
	    // read full request into a String object
	    InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
	    BufferedReader br = new BufferedReader(isr);
	    String fullrequest = "";
	    String line;

	    while ((line = br.readLine()) != null) {
		System.out.println(line);
		fullrequest += line;
	    }

	    br.close();

	    System.out.println("\n** End Request body **\n");
	    
	    String response = "\"Thank you.\"";
	    he.sendResponseHeaders(200, response.length());
	    OutputStream os = he.getResponseBody();
	    os.write(response.toString().getBytes());
	    os.close();
	    
	    System.out.println("Sent response.");
	    System.out.println("--------------------------------------------\n");
	}
    }
}
