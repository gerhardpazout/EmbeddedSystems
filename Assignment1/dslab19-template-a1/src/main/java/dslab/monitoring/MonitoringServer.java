package dslab.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

import at.ac.tuwien.dsg.orvell.Input;
import at.ac.tuwien.dsg.orvell.Shell;
import dslab.ComponentFactory;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer {

    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;
    private Shell shell;
    private CopyOnWriteArrayList<String> addresses = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> servers = new CopyOnWriteArrayList<>();


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        Thread transferConnection = new TransferConnection(config, addresses, servers);
        transferConnection.start();
    }

    @Override
    public void addresses() {
        // TODO
    }

    @Override
    public void servers() {
        // TODO
    }

    @Override
    public void shutdown() {
        // TODO
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}

class TransferConnection extends Thread {

    private String componentId;
    private Config config;
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private boolean running = false;
    private CopyOnWriteArrayList<String> addresses;
    private CopyOnWriteArrayList<String> servers;

    public TransferConnection(Config config, CopyOnWriteArrayList addresses, CopyOnWriteArrayList servers){
        this.config = config;
        this.addresses = addresses;
        this.servers = servers;
        try {
            socket = new DatagramSocket(config.getInt("udp.port"));
            running = true;
            printBootUpMessage();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   @Override
    public void run(){
        running = true;

        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);

            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            String received
                    = new String(packet.getData(), 0, packet.getLength());

            System.out.println("RECEIVED: " + received);
            if (received.equals("end")) {
                running = false;
                continue;
            }
            //socket.send(packet);
        }
        socket.close();
    }

    private void printBootUpMessage(){
        System.out.println("Monitoring Server '" + componentId + "' online. \n" +
                "\tUDP on port " + config.getInt("udp.port") + "\n"
        );
        System.out.println(
                "Use command 'nc -u localhost " +
                        config.getInt("udp.port") +
                        "' in terminal app to connect"
        );
    }
}
