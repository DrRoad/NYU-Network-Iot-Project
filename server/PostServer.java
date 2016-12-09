
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
//For representing the type of attributes/sensors data. E.g. speed, temperature.
class Attribute<T> {
    private T t;

    public Attribute(T t) {
        this.t = t;
    }

    public void add(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }

    public String toString() {
        return t.toString();
    }
}

//For representing commands
class Command {
    String commandName;
    //String port;
    //String ip;
    long startTime;
    long endTime;
    long period;
    boolean sendAllAvailable;
    boolean doCompressedData;
    public Command(){}
    public Command(String s){
        commandName=s;
    }
}

//For representing location
class Location{
    float x;
    float y;
    float z;
    public Location(){}
    public Location(float x, float y, float z){
        this.x=x;
        this.y=y;
        this.z=z;
    }
}

//For future use of multithreading
class Communication extends Thread {
    private Thread t;
    private Command command;

    Communication(Command c) {
        this.command = c;
    }

    public void run() {
        try {
            //startSensing()
            Thread.sleep(50);
        }
        catch (InterruptedException e) {
            System.out.println("Thread " +  threadName + " interrupted.");
        }
    }

    public void start () {
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
    }
}

class ClientState {
    String ip;
    String port;
    String imei;
    Location location;
    boolean isSensing;
    long lastStartTimeStamp;
    long lastPingTimeStamp;
    long batteryLevel;
    //Sensor name to its data map, Eg: "Speed":(float)10
    Map<String, Attribute> sensordatalist;

    public ClientState() {
        sensordatalist = new HashMap<String, Attribute>();
    }
}

/**
 * Controller Class!!!
 */

class Controller {
    final long LiveInterval=10000;
    final long MaxRecordInterval=10000;
    final long BatteryLevel=10;
    final String Start="Start";
    final String Stop="Stop";
    final String Send="Send";

    Map<String, ClientState> allStates;
    public Controller(){}
    public Controller() {
        allStates=new HashMap<String,ClientState>();
    }

    //Parsing json to clientstate object
    public ClientState parse(Json data){
        return new ClientState();
    }

    /**
     * /Listening to keep alive msgs, not sure what is the form of this function
     * @param imei
     * @param data
     */
    public void onPing(String imei, Json data) {
        ClientState s = parse(data);
        long currTime=System.currentTimeMillis();
        //If the pinging client hasn't been added to allStates map.
        if (!allStates.containsKey(imei)) {
            allStates.put(imei, s);
        } else {
            //Haven't heard for too long
            if (allStates.get(imei).lastPingTimeStamp-currTime>LiveInterval){
                stopSensing(imei);
            }
            //battery too low
            if (allStates.get(imei).batteryLevel<=BatteryLevel){
                stopSensing(imei);
            }

        }
        //Update ping time
        allStates.get(imei).lastPingTimeStamp=currTime;
    }

    /**
     * THE FUNCTION FOR SENDING COMMANDS OUT!
     * Not sure the exact form of it
     * Not sure what params are needed
     */
    public void Send(String imei, Command command, String ip){

    }

    //Start sensing
    public void startSensing(String imei) {
        if (allStates[imei].isSensing){
            return;
        }
        Command command=new Command(Start);
        allStates[imei].isSensing = true;
        allStates[imei].lastStartTimeStamp = System.currentTimeMillis();
        Send(imei, command, allStates[imei].ip);
    }

    //Stop sensing
    public void stopSensing(String imei) {
        if (!allStates[imei].isSensing){
            return;
        }
        Command command = new Command(Stop);
        allStates[imei].isSensing = false;
        Send(imei, command, allStates[imei].ip);
    }

    //Send all within a time period
    public void sendAllData(String imei) {
        Command command=new Command(Send);
        command.sendAllAvailable=true;
        Send(imei, command, allStates[imei].ip);
    }

    //Send all compressed within a time period t1 to t2
    public void sendAllCompressedData(String imei,long t1, long t2){
        Command command=new Command(Send);
        command.startTime=t1;
        command.endTime=t2;
        command.doCompressedData=true;
        Send(imei, command, allStates[imei].ip);
    }

    //send data from t1 to t2
    public void sampleOverPeriod(String imei,long t1,long t2) {
        Command command=new Command(Send);
        command.startTime=t1;
        command.endTime=t2;
        Send(imei, command, allStates[imei].ip);
    }
}

// class Data{
//   Map<String, String> attributes;
//   public Data(){
//     attributes=new HashMap<String,String>();
//   }
// }
public class PostServer {

    static final int port = 18003;

    public static void main(String[] args) throws Exception {
        Controller controller = new Controller();
        Data data = new Data();
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
