package dslab.transfer;

import dslab.util.DMTPMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ClientConnectionHandler extends Thread {
    ServerSocket serverSocket;
    BlockingQueue<DMTPMessage> data;
    BlockingQueue dataMonitor;
    int port;
    boolean isRunning;
    private InputStreamReader isr;
    private BufferedReader bfr;
    private PrintWriter pr;

    Socket socketClient;

    public ClientConnectionHandler(int port, BlockingQueue<DMTPMessage> data, BlockingQueue dataMonitor, ServerSocket serverSocket, InputStreamReader isr, BufferedReader bfr, PrintWriter pr){

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

        while(isRunning){
            try {
                //accept incoming request / get the socket from incoming device
                socketClient = serverSocket.accept();
                System.out.println("client connected");

                // create a new thread object to allow multiple clients
                Thread client = new ClientCommunication(serverSocket, socketClient, socketClient.getInputStream(), socketClient.getOutputStream(), data, isr, bfr, pr);

                // Invoking the start() method
                client.start();

            } catch (ConnectException e){
                System.out.println("ConnectException: Connection lost!");
                isRunning = false;
                closeConnection();
            } catch (IOException e) {
                //e.printStackhTrace();
                System.out.println("IOException: Socket closed!");
                isRunning = false;
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
        this.interrupt();

    }
}
