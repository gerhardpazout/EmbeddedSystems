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
        recipients = null;
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
        return true;
    }
}
