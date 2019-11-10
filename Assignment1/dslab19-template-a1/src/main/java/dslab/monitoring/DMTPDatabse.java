package dslab.monitoring;

import dslab.util.DMTPMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DMTPDatabse {
    CopyOnWriteArrayList<DMTPDatabaseMessage> messages = new CopyOnWriteArrayList<>(); //CopyOnWriteArrayList is Threadsafe

    public ArrayList<DMTPDatabaseMessage> getMessagesByUser(String username){
        ArrayList<DMTPDatabaseMessage> result = new ArrayList<>();

        for (DMTPDatabaseMessage message : messages){
            System.out.println("checking message with id: " + message.getID());

            if(message.getRecipients().contains(username)){
                System.out.println();
                result.add(message);
            }
        }

        return result;
    }

    public ArrayList<DMTPDatabaseMessage> getMessagesByRecipient(String email){
        ArrayList<DMTPDatabaseMessage> result = new ArrayList<>();

        for (DMTPDatabaseMessage message : messages){
            System.out.println("checking message with id: " + message.getID());

            if(message.getRecipients().contains(email)){
                System.out.println();
                result.add(message);
            }
        }

        return result;
    }

    public ArrayList<DMTPDatabaseMessage> getMessages(){
        ArrayList<DMTPDatabaseMessage> result = new ArrayList<>();
        for (DMTPDatabaseMessage message : messages){
            result.add(message);
        }
        return result;
    }

    public void addMessage(DMTPMessage message){
        messages.add(new DMTPDatabaseMessage(message));
    }

    public DMTPDatabaseMessage getMessageById(int id){
        //when there are no messages
        if(messages == null || messages.size() == 0) return null;

        //look for message
        for (DMTPDatabaseMessage message : messages){
            if(message.getID() == id){
                return message;
            }
        }
        //if no message was found
        return null;
    }

    public boolean deleteMessage(int id){
        DMTPDatabaseMessage message;
        if( (message = getMessageById(id)) != null){
            messages.remove(message);
            return true;
        }
        return false;
    }

    public String showMessage(int id){
        return getMessageById(id).toString();
    }

    public String showMessage(int id, String recipient){
        DMTPDatabaseMessage message = getMessageById(id);
        if (message.getRecipients().contains(recipient)){
            return message.toString();
        }
        return null;
    }

    public String showMessages(){
        if (messages == null || messages.size() <= 0){
            return "no messages";
        }

        Collections.sort(messages);
        String result = "";

        for(DMTPDatabaseMessage message: messages){
            result += message.getID() + " " + message.getSender() + " " + message.getSubject() + "\n";
        }
        return result;
    }

    public String showMessages(String recipient){
        ArrayList<DMTPDatabaseMessage> messages = this.getMessagesByRecipient(recipient);

        if (messages == null || messages.size() <= 0){
            return "no messages";
        }

        Collections.sort(messages);
        String result = "";

        for(DMTPDatabaseMessage message: messages){
            result += message.getID() + " " + message.getSender() + " " + message.getSubject() + "\n";
        }
        return result;
    }
}
