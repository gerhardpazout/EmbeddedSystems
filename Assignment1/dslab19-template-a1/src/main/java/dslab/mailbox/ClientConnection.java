package dslab.mailbox;

import dslab.monitoring.DMTPDatabse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientConnection extends Thread {
    private ServerSocket serverSocket;
    private Socket socketClient;
    private DMTPDatabse db;
    private String componentId;

    public ClientConnection(String componentId, ServerSocket serverSocket, DMTPDatabse db){
        this.db = db;
        this.componentId = componentId;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run(){
        while(!serverSocket.isClosed()){
            try {
                //accept incoming request / get the socket from incoming device
                socketClient = serverSocket.accept();
                System.out.println("client connected");

                // create a new thread object to allow multiple clients
                Thread client = new ClientCommunication(componentId, serverSocket, socketClient, db);

                // Invoking the start() method
                client.start();

            } catch (ConnectException e){
                closeConnection();
            } catch (IOException e) {
                System.out.println("Socket is closed");
                closeConnection();
                //e.printStackTrace();
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
