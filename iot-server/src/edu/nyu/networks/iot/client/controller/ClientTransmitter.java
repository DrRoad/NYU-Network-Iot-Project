import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

/**
 * Created by zalbhathena on 12/8/16.
 */

public class ClientTransmitter implements Runnable{
    private Socket socket = null;
    private String keepAliveMessage;
    private long keepAliveFrequency = -1;

    private PrintWriter out = null;
    private long lastMessageTime = Long.MIN_VALUE;

    public ClientTransmitter(Socket socket, String keepAliveMessage, long keepAliveFrequency)
    {
        this.socket = socket;
        this.keepAliveFrequency = keepAliveFrequency;
        this.keepAliveMessage = keepAliveMessage;

        try {
            out = new PrintWriter(this.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void run() {
        long currentTime = System.currentTimeMillis();

        while(true)
        {
            if(currentTime >= lastMessageTime + keepAliveFrequency)
            {
                System.out.println("yo!");
                write(keepAliveMessage);

                long tempTime = System.currentTimeMillis();
                try {
                    Thread.sleep(currentTime + keepAliveFrequency - tempTime);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            currentTime = System.currentTimeMillis();
        }
    }

    public void write(String message) {
        out.println(message);
        out.flush();
    }
}
