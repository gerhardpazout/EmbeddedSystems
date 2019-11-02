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


            //String message = bfr.readLine();
            //System.out.println("transfer server: " + message);

            //Send message to incoming device
            PrintWriter pr = new PrintWriter(transferSocket.getOutputStream());

            pr.println("You are now connected to the mailbox server!");
            pr.flush();


            boolean done = false;
            String response;
            while (!done && (response = bfr.readLine()) != null) {

                //print message that transfer server sent
                System.out.println("Transfer Server: " + response);

                //send message from mailbox server to transfer server
                pr.println(response);
                pr.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
        Thread transferThread = new TransferSocketHandler(config.getInt("dmtp.tcp.port"));
        transferThread.start();
        printBootUpMessage();
        */
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

class TransferSocketHandler extends Thread {
    private ServerSocket serverSocket;

    public TransferSocketHandler(int port){
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        while(true){
            try {
                //accept incoming request / get the socket from incoming device
                Socket socketClient = serverSocket.accept();
                System.out.println("Transfer Server connected");

                // create a new thread object to allow multiple clients
                Thread transfer = new TransferHandler(socketClient, socketClient.getInputStream(), socketClient.getOutputStream());

                // Invoking the start() method
                transfer.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
class TransferHandler extends Thread {

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public TransferHandler(Socket socket, InputStream in, OutputStream out){
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            //Receiver message from incoming device
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            BufferedReader bfr = new BufferedReader(isr);

            //Send message to incoming device
            PrintWriter pr = new PrintWriter(socket.getOutputStream());

            pr.println("You are connected to the mailbox server");
            pr.flush();

            boolean done = false;
            String response;
            while (!done && (response = bfr.readLine()) != null) {

                if(response.toLowerCase().trim().equals("quit")){
                    done = true;
                    pr.println("WOW! I don't need you anyways! Go to hell! Bye!");

                    // close input & output streams
                    pr.flush();
                    isr.close();
                    bfr.close();
                    pr.close();

                    // close socket connection
                    socket.close();

                    // kill thread
                    this.interrupt();
                }
                else{
                    pr.println(response);
                }
                pr.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}