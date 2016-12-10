
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

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
    long startTime;
    long endTime;
    long period;
    boolean sendAllAvailable;
    boolean doCompressedData;

    public Command() {
    }

    public Command(String s) {
        commandName = s;
    }
}

//For representing location
class Location {
    float x;
    float y;
    float z;

    public Location() {
    }

    public Location(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
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


///Start sensing, for a time, stop
class Controller {
    final long LiveInterval = 10000;
    final long MaxRecordInterval = 10000;
    final long BatteryLevel = 10;
    final long CollectInterval = 1000;
    final String Start = "Start";
    final String Stop = "Stop";
    final String Send = "Send";

    static Map<String, ClientState> allStates;

    public Controller() {
        allStates = new HashMap<String, ClientState>();
    }

    static class Listen implements Runnable {

        private String data;

        public Listen(){}

        public Listen(String data) {
            this.data = data;
        }
        public ClientState Parse(String data){
            ClientState cs=new ClientState();
            cs.imei=data.split(", +")[0];
            return cs;
        }

        public void run() {
            ClientState s = Parse(data);
            long currTime = System.currentTimeMillis();
            //If the pinging client hasn't been added to allStates map.
            if (!allStates.containsKey(imei)) {
                allStates.put(imei, s);
            } else {
                //Haven't heard for too long
                if (allStates.get(imei).lastPingTimeStamp - currTime > LiveInterval) {
                    stopSensing(imei);
                }
                //battery too low
                if (allStates.get(imei).batteryLevel <= BatteryLevel) {
                    stopSensing(imei);
                }

                if ( allStates.get(imei).isSensing && allStates.get(imei).lastStartTimeStamp-currTime
                        >MaxRecordInterval){
                    stopSensing(imei);
                }

            }
            //Update ping time
            allStates.get(imei).lastPingTimeStamp = currTime;

            Thread.sleep(20);
        }
    }

    static class Send implements Runnable {

        private String ip;
        private String imei;
        private Command command;

        public Send(){}

        public Send(String ip,String imei, Command command) {
            this.ip = ip;
            this.imei=imei;
            this.command=command;
        }

        public String Parse(Command command){
            StringBuilder sb= new StringBuilder();
            sb.append(command.);
        }

        public void run() {
            Socket socket = openSocket(ip, PORT);

            // write-to
            // in this case just write a simple command to a web server.
            String result = writeToAndReadFromSocket(socket, Parse(command));

            // print out the result we got back from the server
            System.out.println(result);

            // close the socket, and we're done
            socket.close();
            Thread.sleep(20);
        }
    }

    //Parsing json to clientstate object
    public ClientState parse(JSONObject data) {
        //JSONParser parser = new JSONParser();
        String imei = data.get("imei");
        if (data.has("location")) {
            cs.location = data.get("location");
        }
        ClientState cs = new ClientState();
        cs.imei = imei;
        return cs;
    }


    //Start sensing
    public static void startSensing(String imei) {
        if (allStates[imei].isSensing) {
            return;
        }
        Command command = new Command(Start);
        allStates[imei].isSensing = true;
        allStates[imei].lastStartTimeStamp = System.currentTimeMillis();
        Send r=new Send(allStates[imei].ip, imei, command);
        Thread t=new Thread(r);
        t.start();
    }

    //Stop sensing
    public static void stopSensing(String imei) {
        if (!allStates[imei].isSensing) {
            return;
        }
        Command command = new Command(Stop);
        allStates[imei].isSensing = false;
        Send r=new Send(allStates[imei].ip, imei, command);
        Thread t=new Thread(r);
        t.start();
    }

    //Send all within a time period
    public static void sendAllData(String imei) {
        Command command = new Command(Send);
        command.sendAllAvailable = true;
        Send r=new Send(allStates[imei].ip, imei, command);
        Thread t=new Thread(r);
        t.start();
    }

    //Send all compressed within a time period t1 to t2
    public static void sendAllCompressedData(String imei, long t1, long t2) {
        Command command = new Command(Send);
        command.startTime = t1;
        command.endTime = t2;
        command.doCompressedData = true;
        Send r=new Send(allStates[imei].ip, imei, command);
        Thread t=new Thread(r);
        t.start();
    }

    //send data from t1 to t2
    public static void sampleOverPeriod(String imei, long t1, long t2) {
        Command command = new Command(Send);
        command.startTime = t1;
        command.endTime = t2;
        Send r=new Send(allStates[imei].ip, imei, command);
        Thread t=new Thread(r);
        t.start();
    }
}

public class PostServer {

    static final int PORT = 18003;

    public static void main(String[] args) throws Exception {
        Controller controller = new Controller();
        Socket s = new Socket();
        //String host = "www.google.com";

        s.connect(new InetSocketAddress(host, 80));

        System.out.println("Connected");
        //HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("Server started at " + port);
//        server.createContext("/", new PostHandler());
//        server.setExecutor(null); // creates a default executor
//        server.start();

//                // write text to the socket
//                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//                bufferedWriter.write(writeTo);
//                bufferedWriter.flush();
        /**
         * Listening to onping
         */

        Thread t1 = new Thread(new Runnable() {
            public void run() {
                while true{
                    long time=System.currentTimeMillis();
                    for(String imei : allStates.keySet()){
                        if (allStates[imei].LastHeard>LiveInterval){
                            allStates.remove();
                        }
                        sampleOverPeriod(imei,time,time+CollectInterval);
                    }

                    Thread.sleep(10);
                }
            }
        });
        t1.start();


        while true {
            // read text from the socket
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            while ((str = bufferedReader.readLine()) != null) {
                sb.append(str);
            }
            String str=sb.toString();

            // close the reader, and return the results as a String
            bufferedReader.close();
            Listen r=new Listen(str);
            Thread t=new Thread(r);
            t.start();
        }

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
