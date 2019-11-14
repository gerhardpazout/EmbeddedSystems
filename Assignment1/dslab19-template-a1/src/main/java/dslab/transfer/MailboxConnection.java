package dslab.transfer;

import dslab.util.Config;
import dslab.util.DMTPMessage;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;

public class MailboxConnection extends Thread {
    private Socket socket;
    private BlockingQueue<DMTPMessage> data;
    private BlockingQueue<DatagramPacket> dataMonitor;
    private Config configTransfer;
    private String ipTransfer;

    private Config domains = new Config("domains.properties");
    ServerSocket serverSocket;

    // Constructor
    public MailboxConnection(Config configTransfer, String ipTransfer, ServerSocket serverSocket, BlockingQueue<DMTPMessage> data, BlockingQueue dataMonitor) {
        this.data = data;
        this.dataMonitor = dataMonitor;
        this.configTransfer = configTransfer;
        this.ipTransfer = ipTransfer;
        this.serverSocket = serverSocket;

    }

    @Override
    public void run()
    {
        while (serverSocket != null && !serverSocket.isClosed()){
            DMTPMessage dmtp;
            while( !data.isEmpty() ){

                try {
                    dmtp = data.take();

                    for (String recipient : dmtp.getRecipients()){
                        sendDMPT(null, dmtp, recipient);
                    }

                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    closeConnection();
                }
            }
        }
    }

    public void closeConnection(){
        if (socket != null && !socket.isClosed()){
            try {
                socket.close();
            } catch (IOException e) {
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
