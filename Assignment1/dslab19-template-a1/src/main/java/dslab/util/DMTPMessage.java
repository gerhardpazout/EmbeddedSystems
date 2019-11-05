package dslab.util;

import java.util.ArrayList;
import java.util.Arrays;

public class DMTPMessage {

    private String subject;
    private String data;
    private String sender;
    private ArrayList<String> recipients = new ArrayList<>();

    public DMTPMessage() {
        subject = "";
        data = "";
        sender = "";
        recipients = new ArrayList<>();
    }

    public DMTPMessage(String subject, String data, String sender, ArrayList<String> recipients) {
        this.subject = subject;
        this.data = data;
        this.sender = sender;
        this.recipients = recipients;
    }

    public DMTPMessage(String subject, String data, String sender, String recipients) {
        this.subject = subject;
        this.data = data;
        this.sender = sender;
        addRecipient(recipients);
    }

    @Override
    public String toString(){
        String result = "";

        result += "from " + getSender().toString() + "\n";
        result += "to " + recipientsToString() + "\n";
        result += "subject " + getSubject() + "\n";
        result += "data " + getData() + "\n";

        return result;
    }

    public String recipientsToString(){
        String result = "";

        for(int i = 0; i < recipients.size(); i++){
            result += (i < recipients.size() - 1)? recipients.get(i) + ", " : recipients.get(i) ;
        }

        return result;
    }

    public void addRecipient(String recipient){
        this.recipients.add(recipient);
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public ArrayList<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(ArrayList<String> recipients) {
        this.recipients = recipients;
    }

    public void setRecipients(String[] recipients) {
        this.recipients = new ArrayList<>();
        for (String recipient : recipients){
            this.recipients.add(recipient);
        }
    }

    public boolean isValid(){
        boolean valid = true;

        if(isNullOrEmpty(sender)){
            valid = false;
        }
        if(recipients == null || recipients.size() <= 0){
            valid = false;
        }

        return valid;
    }

    public static boolean isNullOrEmpty(String str) {
        if(str != null && !str.isEmpty())
            return false;
        return true;
    }
}
