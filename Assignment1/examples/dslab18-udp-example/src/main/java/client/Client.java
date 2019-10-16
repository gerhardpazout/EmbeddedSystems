package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import util.Config;

/**
 * Client application that uses UDP to send provided user-input to the server.
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
        DatagramSocket socket = null;
        BufferedReader userInputReader = null;

        try {
            // open a new DatagramSocket
            socket = new DatagramSocket();
            // create a new Reader to read user-input from System.in
            userInputReader = new BufferedReader(new InputStreamReader(
                    System.in));

            System.out.println("Client: " + name + " is up! Enter command.");

            byte[] buffer;
            DatagramPacket packet;

            while (true) {

                // wait for user-input
                String input = userInputReader.readLine();

                // if user enters !stop (or the end of the stream has been
                // reached) perform a shutdown
                if (input == null || input.startsWith("!stop")) {
                    break;
                }

                // append the name of the client to the user-input
                input = input + " " + name;

                // convert the input String to a byte[]
                buffer = input.getBytes();
                // create the datagram packet with all the necessary information
                // for sending the packet to the server
                packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName(config.getString("server.host")),
                        config.getInt("server.udp.port"));

                // send request-packet to server
                socket.send(packet);

                buffer = new byte[1024];
                // create a fresh packet
                packet = new DatagramPacket(buffer, buffer.length);
                // wait for response-packet from server
                socket.receive(packet);

                System.out.println("Received packet from Server: " + new String(packet.getData()));

            }
        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to host: " + e.getMessage());
        } catch (SocketException e) {
            System.out.println("SocketException: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
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
