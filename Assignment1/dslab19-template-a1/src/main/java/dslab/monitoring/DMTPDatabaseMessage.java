package dslab.monitoring;

import dslab.util.DMTPMessage;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DMTPDatabaseMessage implements Comparable<DMTPDatabaseMessage> {
    private static final AtomicInteger count = new AtomicInteger(0);
    private final int ID;
    private DMTPMessage message;

    public DMTPDatabaseMessage(DMTPMessage message){
        this.message = message;
        ID = count.incrementAndGet();
    }

    public DMTPMessage getMessage(){
        return message;
    }

    public int getID(){
        return ID;
    }

    public String getSender(){
        return message.getSender();
    }

    public String getSubject(){
        return message.getSubject();
    }

    public String getData(){
        return message.getData();
    }

    public ArrayList<String> getRecipients(){
        return message.getRecipients();
    }

    @Override
    public String toString(){
        return getMessage().toString();
    }

    @Override
    public int compareTo(DMTPDatabaseMessage message) {
        /* For Ascending order*/
        return this.getID() - message.getID();

        /* For Descending order do like this */
        //return compareage-this.studentage;
    }
}
