package dslab.mailbox;

import dslab.monitoring.DMTPDatabaseMessage;
import dslab.monitoring.DMTPDatabse;
import dslab.util.Config;
import dslab.util.DMTPMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ClientCommunication extends Thread {
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
    public ClientCommunication(ServerSocket serverSocket, Socket socket, InputStream in, OutputStream out, DMTPDatabse db, String componentId)
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
                    }
                }
                else {
                    responseToClient = "error empty input";
                }
                pr.println(responseToClient);
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
                    else if (getIdFromContext(context) != null){
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
                    }
                    else {
                        response = "error too many arguments.";
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
                    else if(getIdFromContext(context) != null) {
                        id =  Integer.parseInt(context);
                        boolean isDeleted = db.deleteMessage(id);
                        if(!isDeleted){
                            response = "error unknown message id.";
                        }
                    }
                    else {
                        response = "error too many arguments.";
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

    public String getIdFromContext(String context){
        context = context.trim();
        String regex = "^[0-9]+$";
        return (context.matches(regex))? context : null;
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
