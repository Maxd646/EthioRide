package com.ethioride.driver.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP socket client for the driver app.
 * Supports both synchronous (sendAndReceive) and async (setMessageHandler) modes.
 */
public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> messageHandler;
    private volatile boolean connected;

    private NetworkClient() {}

    public static NetworkClient getInstance() {
        if (instance == null) instance = new NetworkClient();
        return instance;
    }

    public void connect() throws IOException {
        if (connected) return;
        socket = new Socket(AppConstants.DEFAULT_HOST, AppConstants.DEFAULT_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
        startReceiveLoop();
    }

    public void send(Message message) throws IOException {
        if (!connected) throw new IOException("Not connected to server.");
        out.writeObject(message);
        out.flush();
    }

    /** Synchronous send-and-receive — blocks until response arrives. */
    public Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        send(request);
        return (Message) in.readObject();
    }

    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    public Consumer<Message> getMessageHandler() {
        return messageHandler;
    }

    public boolean isConnected() { return connected; }

    public void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Sends a request and calls the callback when the matching response type arrives.
     */
    public void sendRequest(MessageType requestType, Object payload,
                            MessageType responseType, Consumer<Message> callback) throws IOException {
        Consumer<Message> original = messageHandler;
        setMessageHandler(msg -> {
            if (msg.getType() == responseType) {
                setMessageHandler(original);
                callback.accept(msg);
            } else if (original != null) {
                original.accept(msg);
            }
        });
        send(new Message(requestType, payload, "driver"));
    }

    private void startReceiveLoop() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) in.readObject();
                    if (messageHandler != null) messageHandler.accept(msg);
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "driver-receiver");
        t.setDaemon(true);
        t.start();
    }
}
