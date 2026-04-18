package com.ethioride.admin.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP socket client for the admin dashboard.
 * Connects to the EthioRide server and streams live system events.
 */
public class AdminSocketClient {
    private static AdminSocketClient instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> messageHandler;
    private volatile boolean connected;

    private AdminSocketClient() {}

    public static AdminSocketClient getInstance() {
        if (instance == null) instance = new AdminSocketClient();
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
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) in.readObject();
                    if (messageHandler != null) messageHandler.accept(msg);
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "admin-receiver");
        t.setDaemon(true);
        t.start();
    }
}
