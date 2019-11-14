package dslab.transfer;

import dslab.util.Config;
import dslab.util.DMTPMessage;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ClientCommunication extends Thread {
    private InputStream in;
    private OutputStream out;
    private ServerSocket serverSocket;
    private Socket socket;
    private BlockingQueue<DMTPMessage> data;
    private InputStreamReader isr;
    private BufferedReader bfr;
    private PrintWriter pr;

    // Constructor
    public ClientCommunication(ServerSocket serverSocket, Socket socket, InputStream in, OutputStream out, BlockingQueue<DMTPMessage> data, InputStreamReader isr, BufferedReader bfr, PrintWriter pr)
    {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.data = data;
        this.isr = isr;
        this.bfr = bfr;
        this.pr = pr;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run()
    {
        try {

            //Receiver message from incoming device
            //InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            //BufferedReader bfr = new BufferedReader(isr);
            isr = new InputStreamReader(socket.getInputStream());
            bfr = new BufferedReader(isr);

            //Send message to incoming device
            pr = new PrintWriter(socket.getOutputStream());

            pr.println("DMTP");
            pr.flush();

            //boolean done = false;
            boolean began = false;
            String messageFromClient;
            String responseToClient;
            DMTPMessage dmtp = new DMTPMessage();

            while (!serverSocket.isClosed() && (messageFromClient = bfr.readLine()) != null) {

                if(!messageFromClient.isEmpty()){

                    messageFromClient = messageFromClient.toLowerCase();
                    responseToClient = "";

                    String input = messageFromClient;

                    if(messageFromClient.toLowerCase().trim().equals("quit")){
                        pr.println("ok bye");

                        closeConnection();
                    }
                    else if(!isValidCommand(getCommand(input))){
                        responseToClient = "error protocol error";
                        pr.println(responseToClient);
                        pr.flush();

                        closeConnection();

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
                        //pr.println(responseToClient);
                        //pr.flush();
                    }
                }
                else {
                    responseToClient = "error empty input";
                }
                pr.println(responseToClient);
                pr.flush();
            }
        } catch(ConnectException e){
            System.out.println("ConnectException - ClientHandler.run()");
            closeConnection();

        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("IOException - client disconnected");
            closeConnection();
        }

    }

    public void closeConnection(){
        // close socket connection
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        this.interrupt();
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
                if(!isNullOrEmpty(context)){

                    boolean failed = false;

                    for (String recipient : getRecipients(context)){

                        if (!failed){
                            if(!isEmail(recipient)){
                                failed = true;
                                response = "error " + recipient + "does not have a valid email format.";
                            }
                            else if(!doesRecipientExist(recipient)){
                                failed = true;
                                response = "error unknown recipient " + recipient;
                            }
                        }
                    }

                    if(!failed){
                        //if everything is ok => set recipients
                        dmtp.setRecipients(getRecipients(context));
                        response = "ok " + getRecipients(context).length;
                    }
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
                        //System.out.println("DMTP nachricht in Queue!");
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

    public boolean isEmail(String email){
        String regex = "^(.+)@(.+)$";
        return email.matches(regex);
    }

    public String getDomainFromEmail(String email){
        return (email.contains("@"))? email.substring(email.indexOf("@") + 1) : null;
    }

    public String getUsernameFromEmail(String email){
        return (email.contains("@"))? email.substring(0, email.indexOf("@")) : null;
    }

    public boolean doesRecipientExist(String email){
        String username = getUsernameFromEmail(email);
        String domain = getDomainFromEmail(email);
        String propertyFilename = "users-" + domain.replace('.', '-') + ".properties";

        try{
            Config users = new Config(propertyFilename);
            users.containsKey(users.getString(username));

            return (users.getString(username) != null);
        }
        catch (java.util.MissingResourceException e){
            return false;
        }
    }
}
