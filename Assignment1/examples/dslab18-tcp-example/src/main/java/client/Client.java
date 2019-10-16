package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import util.Config;

/**
 * Client application that connects to a Server via TCP to send provided user-input.
 */
public class Client implements Runnable {

    private Config config;
    private String name;

    public Client(Config config) {
        this.config = config;
        this.name = config.getString("name");
    }

    @Override
    public void run() {

        Socket socket = null;
        BufferedReader userInputReader = null;

        try {
            /*
             * create a new tcp socket at specified host and port - make sure
			 * you specify them correctly in the client properties file(see
			 * client1.properties and client2.properties)
			 */
            socket = new Socket(config.getString("server.host"), config.getInt("server.tcp.port"));
            // create a reader to retrieve messages send by the server
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // create a writer to send messages to the server
            PrintWriter serverWriter = new PrintWriter(socket.getOutputStream());
            // create the client input reader from command line
            userInputReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Client: " + name + " is up! Enter command.");

            while (true) {
                // read user input
                String input = userInputReader.readLine();
                // if the stream was closed, send !stop
                if (input == null) {
                    input = "!stop";
                }
                // write provided user input to the socket
                serverWriter.println(input + " " + name);
                serverWriter.flush();
                // read server response and write it to console
                System.out.println(serverReader.readLine());

                // in case a user enters "!stop" stop the client
                if (input.startsWith("!stop")) {
                    break;
                }
            }

        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to host: " + e.getMessage());
        } catch (SocketException e) {
            // when the socket is closed, the I/O methods of the Socket will throw a SocketException
            // almost all SocketException cases indicate that the socket was closed
            System.out.println("SocketException while handling socket: " + e.getMessage());
        } catch (IOException e) {
            // you should properly handle all other exceptions
            throw new UncheckedIOException(e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }

            if (userInputReader != null) {
                try {
                    userInputReader.close();
                } catch (IOException e) {
                    // Ignored because we cannot handle it
                }
            }
        }
    }

    public static void main(String[] args) {
        new Client(new Config(args[0])).run();
    }

}
