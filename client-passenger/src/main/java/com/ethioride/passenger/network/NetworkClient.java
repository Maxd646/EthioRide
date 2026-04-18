package com.ethioride.passenger.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Manages the TCP connection from the Passenger client to the server.
 * Runs the receive loop on a background thread.
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

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        connected = true;
        startReceiveLoop();
    }

    public void connect() throws IOException {
        connect(AppConstants.DEFAULT_HOST, AppConstants.DEFAULT_PORT);
    }

    public void send(Message message) throws IOException {
        if (!connected) throw new IOException("Not connected to server.");
        out.writeObject(message);
        out.flush();
    }

    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    public boolean isConnected() { return connected; }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void startReceiveLoop() {
        Thread receiver = new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) in.readObject();
                    if (messageHandler != null) messageHandler.accept(msg);
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "passenger-receiver");
        receiver.setDaemon(true);
        receiver.start();
    }
}
