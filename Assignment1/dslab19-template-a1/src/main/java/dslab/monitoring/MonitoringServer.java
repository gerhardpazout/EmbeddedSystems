package dslab.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import dslab.ComponentFactory;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer {

    private String componentId;
    private Config config;
    private ServerSocket serverSocket;
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
        try {
            serverSocket = new ServerSocket(config.getInt("udp.port"));
            printBootUpMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @Override
    public void run() {
        try {
            Socket socket = serverSocket.accept();

            System.out.println("Client connected");

        } catch (IOException e) {
            e.printStackTrace();
        }
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
