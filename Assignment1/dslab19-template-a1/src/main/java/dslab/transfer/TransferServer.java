package dslab.transfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import dslab.ComponentFactory;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {

    private String componentId;
    private Config config;
    private ServerSocket serverSocket;
    private InputStream in;
    private PrintStream out;

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
        this.in = this.in;
        this.out = out;

        //cli = new CommandLine("cli-transfer-1", config, in, out);

        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));
            printBootUpMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // TODO

        //run infinite loop to get all client requests (e.g. multiple client connections)
        while(true){
            try {
                Socket socketClient = serverSocket.accept(); //accept incoming request / get the socket from incoming device

                // create a new thread object to allow multiple clients
                Thread client = new ClientHandler(socketClient, socketClient.getInputStream(), socketClient.getOutputStream());
                //clientThreads.add(client);
                // Invoking the start() method
                client.start();

                // additional code...

            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }

    private void printBootUpMessage(){
        System.out.println("Transfer Server '" + componentId + "' online on port: " + config.getInt("tcp.port"));
        System.out.println(
                "Use command 'nc " +
                        config.getString("registry.host") + " " +
                        config.getInt("tcp.port") +
                        "' in terminal app to connect"
        );
    }

    @Override
    public void shutdown() {
        // TODO
        try {
            System.out.println("shutting down connection");
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        //server.run();
        new Thread(server).start();
    }

}

// ClientHandler class
class ClientHandler extends Thread{
    private InputStream in;
    private OutputStream out;
    private Socket socket;

    // Constructor
    public ClientHandler(Socket socket, InputStream in, OutputStream out)
    {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run()
    {
        try {
            System.out.println("client connected");

            //Receiver message from incoming device
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            BufferedReader bfr = new BufferedReader(isr);

            //Send message to incoming device
            PrintWriter pr = new PrintWriter(socket.getOutputStream());

            pr.println("Server: Welcome! Type 'quit' to exit.");
            pr.flush();

            boolean done = false;
            String response;
            while (!done && (response = bfr.readLine()) != null) {

                if(response.toLowerCase().trim().equals("quit")){
                    done = true;
                    pr.println("Server: WOW! I don't need you anyways! Go to hell! Bye!");

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
                    pr.println("Server: " + response);
                }
                pr.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
