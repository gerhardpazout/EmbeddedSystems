package dslab.transfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import dslab.ComponentFactory;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {

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
    }

    @Override
    public void run() {
        // TODO
        try {

            ServerSocket serverSocket = new ServerSocket(9991);
            Socket socketClient = serverSocket.accept(); //accept incoming request / get the socket from incoming device

            System.out.println("client connected");

            //Receiver message from incoming device
            InputStreamReader isr = new InputStreamReader(socketClient.getInputStream());
            BufferedReader bfr = new BufferedReader(isr);
            // while (response = bfr.readLine() != null) {...}

            //Send message to incoming device
            PrintWriter pr = new PrintWriter(socketClient.getOutputStream());
            //pr.print(message);
            //pr.flush() //clear stream, used when communication is finished


            pr.println("Server: Welcome! Type 'disconnect' to exit.");
            pr.flush();

            boolean done = false;
            String response;
            while (!done && (response = bfr.readLine()) != null) {
                System.out.println("Client: " + response);
                pr.println("Server: " + response);

                if(response.toLowerCase().trim().equals("disconnect")){
                    done = true;
                    pr.println("WOW! Go to hell! Bye!");
                }

                pr.flush();
            }

            /*
            Socket connectionSocket = serverSocket.accept();

            //Create Input&Outputstreams for the connection
            InputStream inputToServer = connectionSocket.getInputStream();
            OutputStream outputFromServer = connectionSocket.getOutputStream();

            Scanner scanner = new Scanner(inputToServer, "UTF-8");
            PrintWriter serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);

            serverPrintOut.println("Server: Welcome! Type 'disconnect' to exit.");

            //Have the server take input from the client and echo it back
            //This should be placed in a loop that listens for a terminator text e.g. bye
            boolean done = false;

            while(!done && scanner.hasNextLine()) {
                String line = scanner.nextLine();
                serverPrintOut.println("Server: " + line);
                serverPrintOut.flush();

                if(line.toLowerCase().trim().equals("disconnect")){
                    done = true;
                    serverPrintOut.println("Server: WOW! Go to hell! Bye!");
                    serverPrintOut.flush();
                }

            }
            */
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        // TODO
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
