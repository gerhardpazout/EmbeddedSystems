package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import server.tcp.ListenerThread;
import util.Config;

/**
 * Server application that creates a TCP socket and waits for messages send by client.
 */
public class Server implements Runnable {

    private Config config;
    private ServerSocket serverSocket;

    public Server(Config config) {
        this.config = config;
    }

    @Override
    public void run() {

        // create and start a new TCP ServerSocket
        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));
            // handle incoming connections from client in a separate thread
            new ListenerThread(serverSocket).start();
        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
        }

        System.out.println("Server is up! Hit <ENTER> to exit!");
        // create a reader to read from the console
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            // read commands from the console
            reader.readLine();
        } catch (IOException e) {
            // IOException from System.in is very very unlikely (or impossible)
            // and cannot be handled
        }

        // close socket and listening thread
        close();
    }

    public void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error while closing server socket: " + e.getMessage());
            }
        }

    }

    public static void main(String[] args) {
        new Server(new Config("server")).run();
    }

}
