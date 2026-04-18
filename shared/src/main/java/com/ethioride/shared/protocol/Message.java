package com.ethioride.shared.protocol;

import java.io.Serializable;

/**
 * Generic protocol message exchanged between client and server over TCP.
 */
public class Message implements Serializable {
    private MessageType type;
    private Object payload;
    private String senderId;
    private long timestamp;

    public Message(MessageType type, Object payload, String senderId) {
        this.type = type;
        this.payload = payload;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }
    public String getSenderId() { return senderId; }
    public long getTimestamp() { return timestamp; }
}
