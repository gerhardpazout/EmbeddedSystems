package dslab.transfer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.*;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.DMTPMessage;

public class TransferServer implements ITransferServer, Runnable {

    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;
    private BlockingQueue<DMTPMessage> data = new ArrayBlockingQueue<DMTPMessage>(20);
    private BlockingQueue<DatagramPacket> dataMonitor = new ArrayBlockingQueue<>(20);
    private String ip = "127.0.0.1";
    private ServerSocket serverSocket;
    private boolean running = true;
    private Shell shell;
    private InputStreamReader isr;
    private BufferedReader bfr;
    private PrintWriter pr;
    ExecutorService pool;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        // TODO
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        this.shell = new Shell(in, out);
        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        shell.register("shutdown", (input, context) -> {
            shutdown();
            throw new StopShellException();
        });
    }

    @Override
    public void run() {
        // TODO

        /*
        Thread clientSocketHandler = new ClientConnectionHandler(config.getInt("tcp.port"), data, dataMonitor, serverSocket, isr, bfr, pr);
        Thread mailboxSocketHandler = new MailboxConnection("localhost", 11482, data, dataMonitor, config, ip);
        Thread monitorSocketHandler = new MonitoringConnection(config.getString("monitoring.host"), config.getInt("monitoring.port"), dataMonitor);

        clientSocketHandler.start();
        mailboxSocketHandler.start();
        monitorSocketHandler.start();
        printBootUpMessage();

        shell.run();
        */
        printBootUpMessage();

        pool = Executors.newFixedThreadPool(20);

        //Executors pool = Executors.newFixedThreadPool(20);

        //while (true) {
            pool.execute(new ClientConnectionHandler(data, dataMonitor, serverSocket, isr, bfr, pr));
            pool.execute(new MailboxConnection(config, ip, serverSocket, data, dataMonitor));
            pool.execute(new MonitoringConnection(dataMonitor, serverSocket));
            pool.execute(shell);
        //}


    }

    private void printBootUpMessage(){
        System.out.println("Transfer Server '" + componentId + "' online on port: " + config.getInt("tcp.port"));
        System.out.println(
                "Use command 'nc " +
                        config.getString("registry.host") + " " +
                        config.getInt("tcp.port") +
                        "' in terminal app to connect \n"
        );
        System.out.println(componentId + " logs:");
    }

    @Override
    @Command
    public void shutdown() {
        // TODO
        try {
            if(serverSocket != null && !serverSocket.isClosed()){

                // close socket connection
                System.out.println("shutting down socket...");
                serverSocket.close();
                System.out.println("...socket closed!");
            }
            else {
                System.out.println("...socket not even opened!");
            }
        } catch (NullPointerException e){
            System.out.println("NullpointerException: shutdown()");
            e.printStackTrace();
        }catch (IOException e) {
            System.out.println("IOException: shutdown()");
            //e.printStackTrace();
        }
        pool.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }
}