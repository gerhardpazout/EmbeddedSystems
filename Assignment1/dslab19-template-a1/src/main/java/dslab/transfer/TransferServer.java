package dslab.transfer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import at.ac.tuwien.dsg.orvell.Shell;
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
        Thread clientSocketHandler = new ClientSocketHandler(config.getInt("tcp.port"), data, dataMonitor);
        Thread mailboxSocketHandler = new MailboxSocketHandler("localhost", 11482, data, dataMonitor, config);
        Thread monitorSocketHandler = new MonitorHandler(config.getString("monitoring.host"), config.getInt("monitoring.port"), dataMonitor);

        clientSocketHandler.start();
        mailboxSocketHandler.start();
        monitorSocketHandler.start();
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

//Monitoring Server
class MonitorHandler extends Thread {
    private String host;
    private int port;
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private BlockingQueue<DatagramPacket> data;

    public MonitorHandler(String host, int port, BlockingQueue<DatagramPacket> data){
        this.host = host;
        this.port = port;
        this.data = data;
    }

    @Override
    public void run(){
        try {
            System.out.println("trying to create monitoring server connection");
            socket = new DatagramSocket();
            System.out.println("connection to monitoring server successful!");
        } catch (SocketException e) {
            System.out.println("connection to monitoring server failed!");
            e.printStackTrace();
        }

        //if there is data => send data to monitoring server
        while (true){
            while( !data.isEmpty() ){
                try {
                    DatagramPacket packet = data.take();
                    System.out.println("sending packet...");
                    socket.send(packet);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
    private BlockingQueue<DatagramPacket> dataMonitor;
    private Config configTransfer;

    private Config domains = new Config("domains.properties");

    // Constructor
    public MailboxSocketHandler(String host, int port, BlockingQueue<DMTPMessage> data, BlockingQueue dataMonitor, Config configTransfer) {
        this.host = host;
        this.port = port;
        this.data = data;
        this.dataMonitor = dataMonitor;
        this.configTransfer = configTransfer;
    }

    @Override
    public void run()
    {
        //try {
            //Socket socket = new Socket(host, port);

            //Receiver message from incoming device
            //InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            //BufferedReader bfr = new BufferedReader(isr);

            //Send message to incoming device
            //PrintWriter pr = new PrintWriter(socket.getOutputStream());
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
                            sendDMPT(null, dmtp, recipient);
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
        /*
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    public void sendDMPT(PrintWriter pr, DMTPMessage dmtp, String recipient){
        //connect to Server
        try {
            System.out.println("connecting to mailbox server...");
            System.out.println("RECIPIENT: " + recipient);
            System.out.println(getMailboxServerIP(recipient));
            System.out.println(getMailboxServerPort(recipient));
            socket = new Socket(getMailboxServerIP(recipient), getMailboxServerPort(recipient));
            pr = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendMessage(pr, "begin");
        sendMessage(pr, "from " + dmtp.getSender());
        sendMessage(pr, "to " + recipient);
        sendMessage(pr, "subject " + dmtp.getSubject());
        sendMessage(pr, "data " + dmtp.getData());
        System.out.println("DMTP sent!");


        System.out.println("Putting data into dataMonitor...");
        try {
            dataMonitor.put(createPacket(dmtp.getSender()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Data put into dataMonitor");
    }

    public DatagramPacket createPacket(String sender){
        DatagramPacket result = null;
        InetAddress address = null;
        try {
            address = InetAddress.getByName(configTransfer.getString("monitoring.host"));

            String transferIp = "127.0.0.1";
            int port = configTransfer.getInt("monitoring.port");
            String message = transferIp + ":" + port + " " + sender;
            byte[] content = message.getBytes("UTF-8");

            result = new DatagramPacket(content, content.length, address, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    public void sendMessage(PrintWriter pr, String message){
        pr.println(message);
        pr.flush();
    }

    public int getMailboxServerPort(String email){
        int port = -1;
        String fullAddress = getFullAddress(email);
        port = Integer.parseInt(fullAddress.substring(fullAddress.indexOf(":") + 1));
        return port;
    }

    public String getMailboxServerIP(String email){
        String ip = "";
        String fullAddress = getFullAddress(email);
        ip =  fullAddress.substring(0, fullAddress.indexOf(":"));
        return ip;
    }

    public String getFullAddress(String email){
        String domain = getDomainFromEmail(email);
        return domains.getString(domain);
    }

    public String getDomainFromEmail(String email){
        return (email.contains("@"))? email.substring(email.indexOf("@") + 1) : null;
    }
}

//Clients
class ClientSocketHandler extends Thread {

    ServerSocket serverSocket;
    BlockingQueue<DMTPMessage> data;
    BlockingQueue dataMonitor;

    public ClientSocketHandler(int port, BlockingQueue<DMTPMessage> data, BlockingQueue dataMonitor){
        try {
            serverSocket = new ServerSocket(port);
            this.data = data;
            this.dataMonitor = dataMonitor;
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

                    boolean failed = false;

                    System.out.println("checking recipient...");
                    for (String recipient : getRecipients(context)){
                        System.out.println("checking recpient " + recipient + "...");

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
            System.out.println("EXCEPTION!");
            return false;
        }
    }

}
