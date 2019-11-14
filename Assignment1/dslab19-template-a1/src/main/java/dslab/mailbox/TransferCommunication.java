package dslab.mailbox;

import dslab.monitoring.DMTPDatabse;
import dslab.util.DMTPMessage;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class TransferCommunication extends Thread {
    private ServerSocket serverSocket;
    private Socket socket;
    private DMTPDatabse db;

    //Input and output
    private InputStreamReader isr;
    private BufferedReader bfr;
    private PrintWriter pr;

    public TransferCommunication(ServerSocket serverSocket, Socket socket, DMTPDatabse db){
        this.serverSocket = serverSocket;
        this.socket = socket;
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

            boolean done = false;
            String response;
            DMTPMessage dmtp = new DMTPMessage();
            while (!serverSocket.isClosed() && !done && (response = bfr.readLine()) != null) {

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
