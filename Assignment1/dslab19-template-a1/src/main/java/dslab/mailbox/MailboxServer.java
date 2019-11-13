package dslab.mailbox;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.monitoring.DMTPDatabaseMessage;
import dslab.monitoring.DMTPDatabse;
import dslab.transfer.ClientConnectionHandler;
import dslab.transfer.MailboxConnection;
import dslab.transfer.MonitoringConnection;
import dslab.util.CommandLine;
import dslab.util.Config;
import dslab.util.DMTPMessage;

public class MailboxServer implements IMailboxServer, Runnable {

    private String componentId;
    private Config config;
    private ServerSocket serverSocketDMTP;
    private ServerSocket serverSocketDMAP;
    private CommandLine shellClient;
    private InputStream in;
    private OutputStream out;
    private Socket transferSocket;
    private DMTPDatabse db;
    private Shell shell;
    ExecutorService pool;

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
        this.db = new DMTPDatabse();

        shell = new Shell(in, out);
        shell.register("shutdown", (input, context) -> {
            shutdown();
            throw new StopShellException();
        });

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
        try {
            serverSocketDMTP = new ServerSocket(config.getInt("dmtp.tcp.port"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverSocketDMAP = new ServerSocket(config.getInt("dmap.tcp.port"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        printBootUpMessage();

        pool = Executors.newFixedThreadPool(20);
        pool.execute(new TransferConnection(config.getInt("dmtp.tcp.port"), db, serverSocketDMTP));
        pool.execute(new ClientConnection(config.getInt("dmap.tcp.port"), db, componentId, serverSocketDMAP));
        pool.execute(shell);
    }

    @Override
    @Command
    public void shutdown() {
        //close DMTP
        if(serverSocketDMTP != null && !serverSocketDMTP.isClosed()){
            try {
                serverSocketDMTP.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //close DMAP
        if(serverSocketDMAP != null && !serverSocketDMAP.isClosed()){
            try {
                serverSocketDMAP.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pool.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}