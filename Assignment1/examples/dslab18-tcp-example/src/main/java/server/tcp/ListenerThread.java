package server.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class ListenerThread extends Thread {

    private ServerSocket serverSocket;

    public ListenerThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void run() {

        while (true) {
            Socket socket = null;
            try {
                // wait for Client to connect
                socket = serverSocket.accept();
                // prepare the input reader for the socket
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // prepare the writer for responding to clients requests
                PrintWriter writer = new PrintWriter(socket.getOutputStream());

                String request;
                // read client requests
                while ((request = reader.readLine()) != null) {
                    System.out.println("Client sent the following request: " + request);

					/*
                     * check if request has the correct format: !ping
					 * <client-name>
					 */
                    String[] parts = request.split("\\s");

                    String response = "!error provided message does not fit the expected format: "
                            + "!ping <client-name> or !stop <client-name>";

                    if (parts.length == 2) {

                        String clientName = parts[1];

                        if (request.startsWith("!ping")) {
                            response = "!pong " + clientName;
                        } else if (request.startsWith("!stop")) {
                            response = "!bye " + clientName;
                        }
                    }

                    // print request
                    writer.println(response);
                    writer.flush();
                }

            } catch (SocketException e) {
                // when the socket is closed, the I/O methods of the Socket will throw a SocketException
                // almost all SocketException cases indicate that the socket was closed
                System.out.println("SocketException while handling socket: " + e.getMessage());
                break;
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

            }

        }
    }
}
