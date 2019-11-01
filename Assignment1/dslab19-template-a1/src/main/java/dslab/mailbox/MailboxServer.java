package dslab.mailbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.ComponentFactory;
import dslab.util.CommandLine;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable {

    private String componentId;
    private Config config;
    private ServerSocket serverSocketDMTP;
    private ServerSocket serverSocketDMAP;
    private CommandLine shellClient;
    private InputStream in;
    private OutputStream out;
    private Socket transferSocket;

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
        this.in = in;
        this.out = out;

        try {
            this.serverSocketDMTP = new ServerSocket(config.getInt("dmtp.tcp.port"));
            //this.serverSocketDMAP = new ServerSocket(config.getInt("dmap.tcp.port"));
            printBootUpMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printBootUpMessage(){
        System.out.println("Mailbox Server '" + componentId + "' online. \n" +
                "\tDMTP via TCP on port " + config.getInt("dmtp.tcp.port") + "\n" +
                "\tDMAP via TCP on port " + config.getInt("dmap.tcp.port") + "\n"
        );
        System.out.println(
                "Use command 'nc " +
                        config.getString("registry.host") + " <port number>" +
                        "' in terminal app to connect \n"
        );
        System.out.println(componentId + " logs:");
    }

    public ServerSocket getServerSocketDMTP() {
        return serverSocketDMTP;
    }

    public ServerSocket getServerSocketDMAP(){
        return serverSocketDMAP;
    }

    @Override
    public void run() {
        // TODO
        //connection to transfer server (functioning)
        try {
            transferSocket = serverSocketDMTP.accept();

            InputStreamReader isr = new InputStreamReader(transferSocket.getInputStream());
            BufferedReader bfr = new BufferedReader(isr);

            String message = bfr.readLine();
            System.out.println("transfer server: " + message);

            //Send message to incoming device
            PrintWriter pr = new PrintWriter(transferSocket.getOutputStream());

            pr.println("Mailbox Server: Welcome! Type 'disconnect' to exit.");
            pr.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
