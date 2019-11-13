package dslab.mailbox;

import dslab.monitoring.DMTPDatabse;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientConnection extends Thread {
    ServerSocket serverSocket;
    DMTPDatabse db;
    String componentId;

    public ClientConnection(int port, DMTPDatabse db, String componentId, ServerSocket serverSocket){
        this.db = db;
        this.componentId = componentId;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run(){
        while(!serverSocket.isClosed()){
            try {
                //accept incoming request / get the socket from incoming device
                Socket socketClient = serverSocket.accept();
                System.out.println("client connected");

                // create a new thread object to allow multiple clients
                Thread client = new ClientCommunication(serverSocket, socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), db, componentId);

                // Invoking the start() method
                client.start();

            } catch (IOException e) {
                System.out.println("Socket is closed");
                //e.printStackTrace();
            }
        }
    }
}
