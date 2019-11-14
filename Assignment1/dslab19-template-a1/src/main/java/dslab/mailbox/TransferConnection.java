package dslab.mailbox;

import dslab.monitoring.DMTPDatabse;
import dslab.util.Config;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

public class TransferConnection extends Thread {
    private Config config;
    private ServerSocket serverSocket;
    private Socket socketClient;
    private DMTPDatabse db;

    public TransferConnection(Config config, ServerSocket serverSocketDMTP, DMTPDatabse db){
        this.config = config;
        this.serverSocket = serverSocketDMTP;
        this.db = db;
    }

    @Override
    public void run(){
        while(serverSocket != null && !serverSocket.isClosed()){
            try {
                //accept incoming request / get the socket from incoming device
                socketClient = serverSocket.accept();
                //System.out.println("client connected to tcp port");

                // create a new thread object to allow multiple clients
                Thread transfer = new TransferCommunication(serverSocket, socketClient, db);

                // Invoking the start() method
                transfer.start();
            }
            catch (ConnectException e){
                System.out.println("No connection");
                closeConnection();
            }catch (IOException e) {
                //e.printStackTrace();
                System.out.println("No connection");
                closeConnection();
            }
        }
    }

    public void closeConnection(){
        if(socketClient != null && !socketClient.isClosed()){
            try {
                socketClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
