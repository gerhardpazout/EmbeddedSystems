package dslab.monitoring;

import dslab.util.DMTPMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class DMTPDatabse {
    ArrayList<DMTPDatabaseMessage> messages = new ArrayList<>();

    public ArrayList<DMTPDatabaseMessage> getMessagesByUser(String username){
        ArrayList<DMTPDatabaseMessage> result = new ArrayList<>();

        for (DMTPDatabaseMessage message : messages){
            if(message.getRecipients().contains(username)){
                result.add(message);
            }
        }

        return result;
    }

    public ArrayList<DMTPDatabaseMessage> getMessages(){
        return messages;
    }

    public void addMessage(DMTPMessage message){
        messages.add(new DMTPDatabaseMessage(message));
    }

    public DMTPDatabaseMessage getMessageById(int id){
        for (DMTPDatabaseMessage message : messages){
            if(message.getID() == id){
                return message;
            }
        }
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
}
