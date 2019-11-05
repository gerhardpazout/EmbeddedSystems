package dslab.transfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.DMTPMessage;

public class TransferServer implements ITransferServer, Runnable {

    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;
    private BlockingQueue<DMTPMessage> data = new ArrayBlockingQueue<DMTPMessage>(20);

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
    }

    @Override
    public void run() {
        // TODO
        Thread clientSocketHandler = new ClientSocketHandler(config.getInt("tcp.port"), data);
        Thread mailboxSocketHandler = new MailboxSocketHandler("localhost", 11482, data);

        clientSocketHandler.start();
        mailboxSocketHandler.start();
        printBootUpMessage();
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
    public void shutdown() {
        // TODO
        /*
        try {
            System.out.println("shutting down connection");
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }
}

//Mailbox Server
class MailboxSocketHandler extends Thread {
    private InputStream in;
    private OutputStream out;
    private Socket socket;
    private String host;
    private int port;
    private BlockingQueue<DMTPMessage> data;

    // Constructor
    public MailboxSocketHandler(String host, int port, BlockingQueue<DMTPMessage> data) {
        this.host = host;
        this.port = port;
        this.data = data;
    }

    @Override
    public void run()
    {
        try {
            Socket socket = new Socket(host, port);

            //Receiver message from incoming device
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            BufferedReader bfr = new BufferedReader(isr);

            //Send message to incoming device
            PrintWriter pr = new PrintWriter(socket.getOutputStream());
            //pr.println("Whats up mailbox server?");
            //pr.flush();

            /*
            boolean done = false;
            String response;
            while (!done && (response = bfr.readLine()) != null) {

                //output what mailbox server sent
                System.out.println("Mailbox Server: " + response);

                //send message from client to mailbox server
                try {
                    String messageFromClient = (String)data.take();
                    pr.println(messageFromClient);
                    pr.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            */
            while (true){
                DMTPMessage dmtp;
                while( !data.isEmpty() ){

                    try {
                        dmtp = data.take();

                        for (String recipient : dmtp.getRecipients()){
                            sendDMPT(pr, dmtp, recipient);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDMPT(PrintWriter pr, DMTPMessage dmtp, String recipient){
        sendMessage(pr, "begin");
        sendMessage(pr, "from " + dmtp.getSender());
        sendMessage(pr, "to " + recipient);
        sendMessage(pr, "subject " + dmtp.getSubject());
        sendMessage(pr, "data " + dmtp.getData());
        System.out.println("DMTP sent!");
    }

    public void sendMessage(PrintWriter pr, String message){
        pr.println(message);
        pr.flush();
    }
}

//Clients
class ClientSocketHandler extends Thread {

    ServerSocket serverSocket;
    BlockingQueue<DMTPMessage> data;

    public ClientSocketHandler(int port, BlockingQueue<DMTPMessage> data){
        try {
            serverSocket = new ServerSocket(port);
            this.data = data;
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
                System.out.println("client connected");

                // create a new thread object to allow multiple clients
                Thread client = new ClientHandler(socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), data);

                // Invoking the start() method
                client.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
class ClientHandler extends Thread{
    private InputStream in;
    private OutputStream out;
    private ServerSocket serverSocket;
    private Socket socket;
    private BlockingQueue<DMTPMessage> data;

    // Constructor
    public ClientHandler(Socket socket, InputStream in, OutputStream out, BlockingQueue<DMTPMessage> data)
    {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.data = data;
    }

    @Override
    public void run()
    {
        try {
            //Receiver message from incoming device
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            BufferedReader bfr = new BufferedReader(isr);

            //Send message to incoming device
            PrintWriter pr = new PrintWriter(socket.getOutputStream());

            pr.println("Transfer Server: Welcome! Type 'quit' to exit.");
            pr.flush();

            boolean done = false;
            boolean began = false;
            String messageFromClient;
            String responseToClient;
            DMTPMessage dmtp = new DMTPMessage();

            while (!done && (messageFromClient = bfr.readLine()) != null) {
                if(!messageFromClient.isEmpty()){

                    messageFromClient = messageFromClient.toLowerCase();
                    responseToClient = "";

                    String input = messageFromClient;

                    if(messageFromClient.toLowerCase().trim().equals("quit")){
                        done = true;
                        pr.println("Transfer Server: WOW! I don't need you anyways! Go to hell! Bye!");

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
                    else if(!began && !getCommand(input).equals("begin")){
                        responseToClient = "No DMTP message initiated yet! Type 'begin' to initiate new DMTP Message.";
                    }
                    else if(getCommand(input).equals("begin")){
                        began = true;
                        dmtp = new DMTPMessage();
                        responseToClient = checkClientInput(messageFromClient, dmtp);
                    }
                    else if(getCommand(input).equals("send")){
                        responseToClient = checkClientInput(messageFromClient, dmtp);
                        began = false;
                    }
                    else{
                        responseToClient = checkClientInput(messageFromClient, dmtp);
                        pr.println("Transfer Server: " + messageFromClient);
                        pr.flush();
                    }
                }
                else {
                    responseToClient = "error empty input";
                }
                pr.println("Transfer Server - Command Response: " + responseToClient);
                pr.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String checkClientInput(String input, DMTPMessage dmtp){
        String response = "";
        String command = getCommand(input);
        String context = getContext(input);

        if(isNullOrEmpty(input)) return "error no command";

        switch (command){
            case "begin":
                dmtp = new DMTPMessage();
                response = "ok";
                break;
            case "from":
                if(!isNullOrEmpty(context)){
                    dmtp.setSender(context);
                    response = "ok";
                }
                else {
                    response = "error no sender";
                }
                break;
            case "to":
                //get recipients;
                //TODO: error unknown recipient ford
                if(!isNullOrEmpty(context)){
                    dmtp.setRecipients(getRecipients(context));
                    response = "ok " + getRecipients(context).length;
                }
                else{
                    response = "error no recipient";
                }

                break;
            case "subject":
                dmtp.setSubject(context);
                response = "ok";
                break;
            case "data":
                dmtp.setData(context);
                response = "ok";
                break;
            case "send":
                //
                if(dmtp.getRecipients() == null || dmtp.getRecipients().size() == 0){
                    response = "error no recipient";
                }
                else if(dmtp.getSender() == null){
                    response = "error no sender";
                }
                else {
                    //put client dmtp message in data queue => data queue will be forwarded to mailbox server
                    try {
                        data.put(dmtp);
                        response = "ok";
                        System.out.println("DMTP nachricht in Queue!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                response = "error protocol error";
                break;
        }

        return response;
    }

    public String getCommand(String input){
        return (input.split(" ").length >= 1)? input.split(" ")[0] : input;
    }

    public String getContext(String input){
        //return ((input.split(" ").length >= 2))? input.substring(input.indexOf(" ")).trim() : null;

        if(input.split(" ").length < 2){
            return null;
        }
        return input.substring(input.indexOf(" ")).trim();
    }

    public String[] getRecipients(String context){
        String regex = "\\s*,[,\\s]*";
        return context.split(regex);
    }

    public static boolean isNullOrEmpty(String str) {
        if(str != null && !str.isEmpty())
            return false;
        return true;
    }

    public boolean isValidCommand(String command){
        boolean isValid = false;
        String[] validCommands = {"begin", "from", "to", "subject", "data", "send"};

        for (String validCommand : validCommands){
            if (command.equals(validCommand)) isValid = true;
        }

        return isValid;
    }

}
