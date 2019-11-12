package dslab.mailbox;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.annotation.Command;
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
    private Shell shell;

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
        shell.register(this);

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

        Thread transferThread = new TransferSocketHandler(config.getInt("dmtp.tcp.port"), db, serverSocketDMTP);
        transferThread.start();

        Thread client = new ClientSocketHandler(config.getInt("dmap.tcp.port"), db, componentId, serverSocketDMAP);
        client.start();

        printBootUpMessage();
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
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        //server.run();
        new Thread(server::run).start();
    }
}

// Transfer Socket - DMTP
class TransferSocketHandler extends Thread {
    private ServerSocket serverSocket;
    private Socket socketClient;
    private DMTPDatabse db;
    boolean isRunning;

    public TransferSocketHandler(int port, DMTPDatabse db, ServerSocket serverSocketDMTP){
        /*
        try {
            this.serverSocket = new ServerSocket(port);
            this.db = db;
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        this.serverSocket = serverSocketDMTP;
        this.db = db;
    }

    @Override
    public void run(){
        isRunning = true;

        while(isRunning){
            try {
                //accept incoming request / get the socket from incoming device
                socketClient = serverSocket.accept();
                System.out.println("Transfer Server connected");

                // create a new thread object to allow multiple clients
                Thread transfer = new TransferHandler(serverSocket, socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), db);

                // Invoking the start() method
                transfer.start();

            }
            catch (ConnectException e){
                isRunning = false;
            }catch (IOException e) {
                isRunning = false;
                //e.printStackTrace();
            }
        }
    }
}
class TransferHandler extends Thread {

    private ServerSocket serverSocket;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private DMTPDatabse db;

    InputStreamReader isr;
    BufferedReader bfr;
    PrintWriter pr;



    public TransferHandler(ServerSocket serverSocket, Socket socket, InputStream in, OutputStream out, DMTPDatabse db){
        this.serverSocket = serverSocket;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            //Receiver message from incoming device
            isr = new InputStreamReader(socket.getInputStream());
            bfr = new BufferedReader(isr);

            //Send message to incoming device
            pr = new PrintWriter(socket.getOutputStream());

            pr.println("You are connected to the mailbox server");
            pr.flush();

            boolean done = false;
            String response;
            DMTPMessage dmtp = new DMTPMessage();
            while (!serverSocket.isClosed() && !done && (response = bfr.readLine()) != null) {

                System.out.println("response: " + response);

                if(response.toLowerCase().trim().equals("quit")){
                    done = true;
                    pr.println("ok bye");

                    closeConnection();
                }
                else{
                    //parse input into DMTP object;
                    dmtp = parseInputToDMTP(response, dmtp);

                    //put dmtp message into database if fully transmitted
                    if( dmtp.isValid() && getCommand(response).equals("data")){
                        db.addMessage(dmtp);
                    }

                    System.out.println("\nlist:");
                    //System.out.println(db.showMessages());
                    System.out.println();

                    pr.println(response);

                }
                pr.flush();
            }
        } catch (ConnectException e){
            System.out.println("ConnectException");
            closeConnection();
        }
        catch (IOException e) {
            System.out.println("IOException");
            closeConnection();
            //e.printStackTrace();
        }
    }

    public void closeConnection(){
        // close input & output streams
        pr.flush();
        try {
            isr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bfr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pr.close();

        // close socket connection
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // kill thread
        this.interrupt();
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
    String componentId;

    public ClientSocketHandler(int port, DMTPDatabse db, String componentId, ServerSocket serverSocket){
        /*
        try {
            serverSocket = new ServerSocket(port);
            this.db = db;
            this.componentId = componentId;
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        this.db = db;
        this.componentId = componentId;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run(){
        while(!serverSocket.isClosed()){
            try {
                //accept incoming request / get the socket from incoming device
                Socket socketClient = serverSocket.accept();
                System.out.println("client connected");

                // create a new thread object to allow multiple clients
                Thread client = new ClientHandler(serverSocket, socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), db, componentId);

                // Invoking the start() method
                client.start();

            } catch (IOException e) {
                System.out.println("Socket is closed");
                //e.printStackTrace();
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
    private Config users;
    private boolean isLoggedIn = false;
    private String componentId;
    private String user = null;

    InputStreamReader isr;
    BufferedReader bfr;
    PrintWriter pr;

    // Constructor
    public ClientHandler(ServerSocket serverSocket, Socket socket, InputStream in, OutputStream out, DMTPDatabse db, String componentId)
    {
        this.serverSocket = serverSocket;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.db = db;
        this.componentId = componentId;
        users = new Config("users-" + componentId.replace("mailbox-", "") + ".properties");
    }

    @Override
    public void run()
    {
        try {
            //Receiver message from incoming device
            isr = new InputStreamReader(socket.getInputStream());
            bfr = new BufferedReader(isr);

            //Send message to incoming device
            pr = new PrintWriter(socket.getOutputStream());

            pr.println("DMAP");
            pr.flush();

            boolean done = false;
            boolean began = false;
            String messageFromClient;
            String responseToClient;
            DMTPMessage dmtp = new DMTPMessage();

            while (!serverSocket.isClosed() && !done && (messageFromClient = bfr.readLine()) != null) {
                if(!messageFromClient.isEmpty()){

                    messageFromClient = messageFromClient.toLowerCase();
                    responseToClient = "";

                    String input = messageFromClient;

                    if(messageFromClient.toLowerCase().trim().equals("quit")){
                        done = true;
                        pr.println("ok bye");

                        closeConnection();
                    }
                    else if(!isValidCommand(getCommand(input))){
                        responseToClient = "error protocol error";
                        pr.println("S: " + responseToClient);
                        pr.flush();

                        closeConnection();
                    }
                    else{
                        responseToClient = checkClientInput(messageFromClient, dmtp, db);
                        //pr.println("MailBox Server: " + responseToClient);
                        //pr.flush();
                    }
                }
                else {
                    responseToClient = "error empty input";
                }
                pr.println("Server:" + responseToClient);
                pr.flush();
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("IOException");
            closeConnection();
        }

    }

    public void closeConnection(){
        // close input & output streams
        pr.flush();
        try {
            isr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bfr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pr.close();

        // close socket connection
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // kill thread
        this.interrupt();
    }

    public String checkClientInput(String input, DMTPMessage dmtp, DMTPDatabse db){
        String response = "ok";
        String command = getCommand(input);
        String context = getContext(input);
        int id;

        if(isNullOrEmpty(input)) return "error no command.";

        switch (command){
            case "login":
                //
                System.out.println("calling login / check user function...");

                //if login successful
                if(isNullOrEmpty(context)){
                   response = "error command missing parameter.";
                }
                else {
                    String user = getUserFromContext(context);
                    String password = getPasswordFromContext(context);

                    if(password == null){
                        response = "error no password provided.";
                    }
                    else if(!doesUserExist(user)){
                        response = "error unknown user.";
                    }
                    else if(!checkUser(user, password)){
                        response = "error wrong password.";
                    }
                    else{
                        isLoggedIn = checkUser(user, password);
                        this.user = user;
                        System.out.println("login successful!");
                        System.out.println("USER: " + this.user);
                    }
                }
                break;
            case "logout":
                if(!isLoggedIn){
                    response = "error not logged in.";
                }
                else{
                    isLoggedIn = false;
                    this.user = null;
                }
                break;
            case "list":
                response = (isLoggedIn)?db.showMessages(getEmailFromUser(this.user)):"error not logged in.";
                break;
            case "show":
                if(isLoggedIn){
                    response = "error unknown message id.";

                    if(isNullOrEmpty(context)){
                        response = "error command missing parameter.";
                    }
                    else {
                        id = Integer.parseInt(context);
                        ArrayList<DMTPDatabaseMessage> messagesByUser = db.getMessagesByRecipient(getEmailFromUser(this.user));
                        if (messagesByUser == null || messagesByUser.size() == 0){
                            System.out.println("LIST EMPTY");
                            response = "no messages.";
                        }
                        else if(db.showMessage(id, getEmailFromUser(this.user)) == null) {
                            System.out.println("NO MESSAGES FOUND");
                            response = "no messages.";
                        }
                        else {
                            response = db.showMessage(id, getEmailFromUser(this.user));
                        }
                        /*
                        DMTPDatabaseMessage dbMessage = db.getMessageById(id);
                        if (dbMessage != null){
                            response = db.showMessage(id);
                        }
                        */
                    }
                }
                else{
                    response = "error not logged in.";
                }
                break;
            case "delete":
                if(isLoggedIn) {
                    if(isNullOrEmpty(context)){
                        response = "error command missing parameter.";
                    }
                    else {
                        id =  Integer.parseInt(context);
                        boolean isDeleted = db.deleteMessage(id);
                        if(!isDeleted){
                            response = "error unknown message id.";
                        }
                    }
                }
                else {
                    response = "error not logged in.";
                }
                break;
            default:
                response = "error protocol error.";
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
        String[] validCommands = {"login", "list", "show", "delete", "logout"};

        for (String validCommand : validCommands){
            if (command.equals(validCommand)) isValid = true;
        }

        return isValid;
    }

    public String getUserFromContext(String context){
        if (context.split(" ").length < 2){
            return null;
        }
        else {
            return context.split(" ")[0];
        }
    }

    public String getPasswordFromContext(String context){
        if (context.split(" ").length < 2){
            return null;
        }
        else {
            return context.split(" ")[1];
        }
    }

    public boolean doesUserExist(String user){
        return users.containsKey(user);
    }

    public boolean checkUser(String user, String password){
        try{
            return (users.getString(user).equals(password));
        }
        catch (java.util.MissingResourceException e){
            return false;
        }
    }

    public String getDomainFromComponentId(){
        return componentId.replace("mailbox-", "").replace("-", ".");
    }

    public String getEmailFromUser(String username){
        System.out.println("EMAIL: " +  username + "@" + getDomainFromComponentId());
        return username + "@" + getDomainFromComponentId();
    }
}
