package dslab.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.HashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer {

    private String componentId;
    private Config config;
    private Shell shell;
    private HashMap<String, Integer> addresses = new HashMap<>();
    private HashMap<String, Integer> servers = new HashMap<>();
    private DatagramSocket socket;
    private boolean running;


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

        shell = new Shell(in, out);
        shell.register(this);
    }

    @Override
    public void run() {
        //start connection
        try {
            socket = new DatagramSocket(config.getInt("udp.port"));
            printBootUpMessage();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        //start receiving udp packets
        new Thread(() -> {
            running = true;
            byte[] buf;

            while (running) {
                buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);

                    String message = new String(buf, 0, packet.getLength());
                    incrementHashMap(servers, getServerKeyFromMessage(message));
                    incrementHashMap(addresses, getAddressKeyFromMessage(message));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //start shell
        new Thread(() -> shell.run()).start();
    }

    @Override
    @Command
    public void addresses() {
        addresses.forEach((key,value) -> System.out.println(key + "  " + value));
    }

    @Override
    @Command
    public void servers() {
        servers.forEach((key,value) -> System.out.println(key + "  " + value));
    }

    @Override
    @Command
    public void shutdown() {
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

    public void incrementHashMap(HashMap<String, Integer> map, String key){
        if (map.containsKey(key)){
            map.replace(key, map.get(key)+1);
        }
        else {
            map.put(key, 1);
        }
    }

    public String getAddressFromMessage(String message){
        return message.substring(0, message.indexOf(":"));
    }

    public String getPortFromMessage(String message){
        return message.substring(message.indexOf(":") + 1, message.indexOf(" "));
    }

    public String getServerKeyFromMessage(String message){
        return getAddressFromMessage(message) + ":" + getPortFromMessage(message);
    }

    public String getAddressKeyFromMessage(String message){
        return message.split(" ")[1];
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}