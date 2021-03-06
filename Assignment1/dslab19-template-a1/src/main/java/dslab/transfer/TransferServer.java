package dslab.transfer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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

        /*
        shell.register("shutdown", (input, context) -> {
            shutdown();
            throw new StopShellException();
        });
        */
        shell.register(this);
        run();
    }

    @Override
    public void run() {
        // TODO

        Thread clientSocketHandler = new ClientSocketHandler(config.getInt("tcp.port"), data, dataMonitor, serverSocket, isr, bfr, pr);
        Thread mailboxSocketHandler = new MailboxSocketHandler("localhost", 11482, data, dataMonitor, config, ip);
        Thread monitorSocketHandler = new MonitorHandler(config.getString("monitoring.host"), config.getInt("monitoring.port"), dataMonitor);

        clientSocketHandler.start();
        mailboxSocketHandler.start();
        monitorSocketHandler.start();
        printBootUpMessage();

        //new Thread(() -> shell.run()).start();
        shell.run();
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
                // close input & output streams
                /*
                pr.flush();
                isr.close();
                bfr.close();
                pr.close();
                */

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

    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        //server.run();
        //new Thread(server::run).start();
    }
}

//Monitoring Server
class MonitorHandler extends Thread {
    private String host;
    private int port;
    private DatagramSocket socket;
    private BlockingQueue<DatagramPacket> data;

    public MonitorHandler(String host, int port, BlockingQueue<DatagramPacket> data){
        this.host = host;
        this.port = port;
        this.data = data;
    }

    @Override
    public void run(){
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("SocketException: Monitoring server not available");
            e.printStackTrace();
        }

        //if there is data => send data to monitoring server
        while (true){
            while( !data.isEmpty() ){
                try {
                    DatagramPacket packet = data.take();
                    //System.out.println("sending UDP packet to monitoring server...");
                    socket.send(packet);
                    //System.out.println("...packet sent");
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException: run()");
                    //e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("IOException: run()");
                    //e.printStackTrace();
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
    private String ipTransfer;

    private Config domains = new Config("domains.properties");

    // Constructor
    public MailboxSocketHandler(String host, int port, BlockingQueue<DMTPMessage> data, BlockingQueue dataMonitor, Config configTransfer, String ipTransfer) {
        this.host = host;
        this.port = port;
        this.data = data;
        this.dataMonitor = dataMonitor;
        this.configTransfer = configTransfer;
        this.ipTransfer = ipTransfer;

    }

    @Override
    public void run()
    {
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
    }

    public void sendDMPT(PrintWriter pr, DMTPMessage dmtp, String recipient){
        //connect to Server
        try {
            socket = new Socket(getMailboxServerIP(recipient), getMailboxServerPort(recipient));
            pr = new PrintWriter(socket.getOutputStream());

            //send dmtp to mailbox server
            dmptProtocol(pr, dmtp, recipient);
        } catch (ConnectException e){
            //server down => send error mail to sender
            try {
                socket = new Socket(getMailboxServerIP(dmtp.getSender()), getMailboxServerPort(dmtp.getSender()));
                pr = new PrintWriter(socket.getOutputStream());

                String oldSender = dmtp.getSender();
                dmtp.setSender("mailer@" + ipTransfer);
                dmtp.setRecipients(new String[]{oldSender});
                dmtp.setSubject("error - could not send email");
                dmtp.setData("The message could not be delivered, because the server "
                        + getDomainFromEmail(recipient)
                        + "was not reachable");

                //send dmtp to mailbox server
                dmptProtocol(pr, dmtp, recipient);

            } catch (ConnectException e1){
                // server from sender down too => discard error mail
                //e1.printStackTrace();
            } catch (NullPointerException e1){
                //
            } catch (IOException e1) {
                //e1.printStackTrace();
            }
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public void dmptProtocol(PrintWriter pr, DMTPMessage dmtp, String recipient){
        sendMessage(pr, "begin");
        sendMessage(pr, "from " + dmtp.getSender());
        sendMessage(pr, "to " + dmtp.recipientsToString());
        sendMessage(pr, "subject " + dmtp.getSubject());
        sendMessage(pr, "data " + dmtp.getData());
        sendMessage(pr, "quit");
        //System.out.println("DMTP sent!");

        try {
            dataMonitor.put(createPacket(recipient));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public DatagramPacket createPacket(String recipient){
        DatagramPacket result = null;
        InetAddress address = null;
        try {
            address = InetAddress.getByName(configTransfer.getString("monitoring.host")); //to send udp to
            int portUDP = configTransfer.getInt("monitoring.port"); //to send udp to
            int portReceiver = getMailboxServerPort(recipient);

            String message = ipTransfer + ":" + portReceiver + " " + recipient;
            byte[] content = message.getBytes("UTF-8");

            result = new DatagramPacket(content, content.length, address, portUDP);
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
        return (doesDomainExist(domain))?domains.getString(domain):null;
    }

    public String getDomainFromEmail(String email){
        return (email.contains("@"))? email.substring(email.indexOf("@") + 1) : null;
    }

    public boolean doesDomainExist(String domain){
        Config domains = new Config("domains.properties");
        if(domains.containsKey(domain)){
            return true;
        }
        return false;
    }
}

//Clients
class ClientSocketHandler extends Thread {

    ServerSocket serverSocket;
    BlockingQueue<DMTPMessage> data;
    BlockingQueue dataMonitor;
    int port;
    boolean isRunning;
    private InputStreamReader isr;
    private BufferedReader bfr;
    private PrintWriter pr;

    Socket socketClient;

    public ClientSocketHandler(int port, BlockingQueue<DMTPMessage> data, BlockingQueue dataMonitor, ServerSocket serverSocket, InputStreamReader isr, BufferedReader bfr, PrintWriter pr){

            this.serverSocket = serverSocket;
            this.data = data;
            this.dataMonitor = dataMonitor;
            this.port = port;
            this.isr = isr;
            this.bfr = bfr;
            this.pr = pr;

    }

    @Override
    public void run(){
        isRunning = true;
        /*
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        while(isRunning){
            try {
                //accept incoming request / get the socket from incoming device
                socketClient = serverSocket.accept();
                System.out.println("client connected");

                // create a new thread object to allow multiple clients
                Thread client = new ClientHandler(serverSocket, socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), data, isr, bfr, pr);

                // Invoking the start() method
                client.start();

            } catch (ConnectException e){
                System.out.println("ConnectException: Connection lost!");
                isRunning = false;
            } catch (IOException e) {
                //e.printStackhTrace();
                System.out.println("IOException: Socket closed!");
                isRunning = false;
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
    private InputStreamReader isr;
    private BufferedReader bfr;
    private PrintWriter pr;
    private boolean isRunning;

    // Constructor
    public ClientHandler(ServerSocket serverSocket, Socket socket, InputStream in, OutputStream out, BlockingQueue<DMTPMessage> data, InputStreamReader isr, BufferedReader bfr, PrintWriter pr)
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
            isRunning = true;

            //Receiver message from incoming device
            //InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            //BufferedReader bfr = new BufferedReader(isr);
            isr = new InputStreamReader(socket.getInputStream());
            bfr = new BufferedReader(isr);

            //Send message to incoming device
            pr = new PrintWriter(socket.getOutputStream());

            pr.println("S: Welcome! Type 'quit' to exit.");
            pr.flush();

            //boolean done = false;
            boolean began = false;
            String messageFromClient;
            String responseToClient;
            DMTPMessage dmtp = new DMTPMessage();

            while (!serverSocket.isClosed() && isRunning && (messageFromClient = bfr.readLine()) != null) {

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
                        pr.println("S: " + responseToClient);
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
                        pr.println("S: " + messageFromClient);
                        pr.flush();
                    }
                }
                else {
                    responseToClient = "error empty input";
                }
                pr.println("S: " + responseToClient);
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
        // close input & output streams
        isRunning = false;

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
