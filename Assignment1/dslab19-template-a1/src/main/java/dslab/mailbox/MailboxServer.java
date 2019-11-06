package dslab.mailbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.ComponentFactory;
import dslab.monitoring.DMTPDatabaseMessage;
import dslab.monitoring.DMTPDatabse;
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
        Thread transferThread = new TransferSocketHandler(config.getInt("dmtp.tcp.port"), db);
        transferThread.start();
        printBootUpMessage();
        Thread client = new ClientSocketHandler(config.getInt("dmap.tcp.port"), db);
        client.start();
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
    private DMTPDatabse db;

    public TransferSocketHandler(int port, DMTPDatabse db){
        try {
            this.serverSocket = new ServerSocket(port);
            this.db = db;
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
                Thread transfer = new TransferHandler(socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), db);

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
    private DMTPDatabse db;

    public TransferHandler(Socket socket, InputStream in, OutputStream out, DMTPDatabse db){
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.db = db;
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
            DMTPMessage dmtp = new DMTPMessage();
            while (!done && (response = bfr.readLine()) != null) {

                System.out.println("response: " + response);

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
                    //parse input into DMTP object;
                    dmtp = parseInputToDMTP(response, dmtp);

                    //put dmtp message into database if fully transmitted
                    if( dmtp.isValid() && getCommand(response).equals("data")){
                        db.addMessage(dmtp);
                    }

                    System.out.println("\nlist:");
                    System.out.println(db.showMessages());
                    System.out.println();

                    pr.println(response);

                }
                pr.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCommand(String input){
        return (input.split(" ").length >= 1)? input.split(" ")[0] : input;
    }

    public String getContext(String input){
        //return ((input.split(" ").length >= 2))? input.substring(input.indexOf(" ")).trim() : null;

        if(input.split(" ").length < 2){
            return "";
        }
        return input.substring(input.indexOf(" ")).trim();
    }

    public DMTPMessage parseInputToDMTP(String input, DMTPMessage message){
        if (message == null){
            message = new DMTPMessage();
        }

        String command = getCommand(input);
        String context = getContext(input);

        switch(command){
            case "begin":
                message = new DMTPMessage();
            case "from":
                message.setSender(context);
                break;
            case "to":
                message.setRecipients(getRecipientsFromInput(context));
                break;
            case "subject":
                message.setSubject(context);
                break;
            case "data":
                message.setData(context);
                break;
        }

        return message;
    }

    public ArrayList<String> getRecipientsFromInput(String context){
        ArrayList<String> result = new ArrayList<>();
        String regex = "\\s*,[,\\s]*";
        String[] recipients = context.split(regex);

        for (String recipient : recipients){
            result.add(recipient);
        }

        return result;
    }
}


//Clients
class ClientSocketHandler extends Thread {

    ServerSocket serverSocket;
    DMTPDatabse db;

    public ClientSocketHandler(int port, DMTPDatabse db){
        try {
            serverSocket = new ServerSocket(port);
            this.db = db;
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
                Thread client = new ClientHandler(socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), db);

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
    private DMTPDatabse db;

    // Constructor
    public ClientHandler(Socket socket, InputStream in, OutputStream out, DMTPDatabse db)
    {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.db = db;
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

            pr.println("DMAP");
            pr.flush();

            boolean done = false;
            boolean began = false;
            boolean isLoggedIn = false;
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
                        pr.println("ok bye");

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
                        responseToClient = checkClientInput(messageFromClient, dmtp, db, isLoggedIn);
                        //pr.println("MailBox Server: " + responseToClient);
                        //pr.flush();
                    }
                }
                else {
                    responseToClient = "error empty input";
                }
                pr.println("Mailbox Server - Command Response: " + responseToClient);
                pr.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String checkClientInput(String input, DMTPMessage dmtp, DMTPDatabse db, boolean isLoggedIn){
        String response = "ok";
        String command = getCommand(input);
        String context = getContext(input);
        int id;
        if(isNullOrEmpty(input)) return "error no command";

        switch (command){
            case "login":
                //
                System.out.println("calling login / check user function...");
                //if login successful
                isLoggedIn = true;
                break;
            case "list":
                response = db.showMessages();
                break;
            case "show":
                response = "error unknown message id";

                id = Integer.parseInt(context);
                DMTPDatabaseMessage dbMessage = db.getMessageById(id);
                if (dbMessage != null){
                    response = response = db.showMessage(id);
                }
                break;
            case "delete":
                id =  Integer.parseInt(context);
                boolean isDeleted = db.deleteMessage(id);
                if(!isDeleted){
                    response = "error unknown message id";
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
