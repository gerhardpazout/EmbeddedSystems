package dslab.mailbox;

import dslab.monitoring.DMTPDatabse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

public class TransferConnection extends Thread {
    private ServerSocket serverSocket;
    private Socket socketClient;
    private DMTPDatabse db;
    boolean isRunning;

    public TransferConnection(int port, DMTPDatabse db, ServerSocket serverSocketDMTP){
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
                Thread transfer = new TransferCommunication(serverSocket, socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), db);

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
