package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;

import server.udp.ListenerThread;
import util.Config;

/**
 * Server application that uses UDP to wait for packets send by client.
 */
public class Server implements Runnable {

    private Config config;
    private DatagramSocket datagramSocket;

    public Server(Config config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            // constructs a datagram socket and binds it to the specified port
            datagramSocket = new DatagramSocket(config.getInt("udp.port"));

            // create a new thread to listen for incoming packets
            new ListenerThread(datagramSocket).start();
        } catch (IOException e) {
            throw new RuntimeException("Cannot listen on UDP port.", e);
        }

        System.out.println("Server is up! Hit <ENTER> to exit!");
        // create a new Reader to read commands from System.in
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            // read commands on the server side
            reader.readLine();
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }

        // close socket and listening thread
        close();
    }

    public void close() {
        /*
		 * Note that closing the socket also triggers an exception in the
		 * listening thread
		 */
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    public static void main(String[] args) {
        new Server(new Config("server")).run();
    }

}
