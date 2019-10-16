package server.udp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Thread to listen for incoming data packets on the given socket.
 */
public class ListenerThread extends Thread {

    private DatagramSocket datagramSocket;

    public ListenerThread(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public void run() {

        byte[] buffer;
        DatagramPacket packet;
        try {
            while (true) {
                buffer = new byte[1024];
                // create a datagram packet of specified length (buffer.length)
                /*
                 * Keep in mind that, in UDP, packet delivery is not guaranteed,
				 * and the order of the delivery/processing is also not guaranteed.
				 */
                packet = new DatagramPacket(buffer, buffer.length);

                // wait for incoming packets from client
                datagramSocket.receive(packet);
                // get the data from the packet
                String request = new String(packet.getData());

                System.out.println("Received request-packet from client: " + request);

                // check if request has the correct format:
                // !ping <client-name>
                String[] parts = request.split("\\s");

                String response = "!error provided message does not fit the expected format: !ping <client-name>";

                if (parts.length == 2) {
                    String clientName = parts[1];

                    if (request.startsWith("!ping")) {
                        response = "!pong " + clientName;
                    }
                }
                // get the address of the sender (client) from the received
                // packet
                InetAddress address = packet.getAddress();
                // get the port of the sender from the received packet
                int port = packet.getPort();
                buffer = response.getBytes();
                /*
                 * create a new datagram packet, and write the response bytes,
				 * at specified address and port. the packet contains all the
				 * needed information for routing.
				 */
                packet = new DatagramPacket(buffer, buffer.length, address, port);
                // finally send the packet
                datagramSocket.send(packet);
            }

        } catch (SocketException e) {
            // when the socket is closed, the send or receive methods of the DatagramSocket will throw a SocketException
            System.out.println("SocketException while waiting for/handling packets: " + e.getMessage());
            return;
        } catch (IOException e) {
            // other exceptions should be handled correctly in your implementation
            throw new UncheckedIOException(e);
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }

    }
}
