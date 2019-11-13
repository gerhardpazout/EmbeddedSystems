package dslab.transfer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

public class MonitoringConnection extends Thread {
    private String host;
    private int port;
    private DatagramSocket socket;
    private BlockingQueue<DatagramPacket> data;
    private boolean isRunning = false;

    public MonitoringConnection(String host, int port, BlockingQueue<DatagramPacket> data){
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

        isRunning = true;

        //if there is data => send data to monitoring server
        while (isRunning){
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
                } finally {
                    isRunning = false;
                    if(socket != null && !socket.isClosed()){
                        socket.close();
                    }
                }
            }
        }
    }
}
