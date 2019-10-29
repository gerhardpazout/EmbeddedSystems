package dslab.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;

import dslab.ComponentFactory;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable {

    private String componentId;
    private Config config;
    private ServerSocket serverSocketDMTP;
    private ServerSocket serverSocketDMAP;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        // TODO
        this.componentId = componentId;
        this.config = config;
        try {
            this.serverSocketDMTP = new ServerSocket(config.getInt("dmtp.tcp.port"));
            this.serverSocketDMAP = new ServerSocket(config.getInt("dmap.tcp.port"));
            printBootUpMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printBootUpMessage(){
        System.out.println("Mailbox Server '" + componentId + "' online. \n" +
                "\tDMTP on port " + config.getInt("dmtp.tcp.port") + "\n" +
                "\tDMAP on port " + config.getInt("dmap.tcp.port") + "\n"
        );
        System.out.println(
                "Use command 'nc " +
                        config.getString("registry.host") + " <port number>" +
                        "' in terminal app to connect"
        );
    }

    @Override
    public void run() {
        // TODO
    }

    @Override
    public void shutdown() {
        // TODO
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
