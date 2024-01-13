package org.example.Messeging;

import java.sql.Timestamp;

/**
 * Class to represent Messages
 */
public class Message{

    String Content;
    String From;
    String To;
    Timestamp timestamp;

    public Message(String content, String from, long timestamp) {
        Content = content;
        From = from;
        this.timestamp = new Timestamp(timestamp);
    }

    public  Message(String content, String from, String to){
        Content = content;
        From = from;
        To = to;
        this.timestamp =  new Timestamp(System.currentTimeMillis());
    }

    public String toString(){
        return this.Content;
    }

    public String getContent() {
        return Content;
    }

    public String getFrom() {
        return From;
    }
    
    public String getTo() {
        return To;
    }


    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    
}
