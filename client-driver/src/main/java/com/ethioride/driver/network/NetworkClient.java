package com.ethioride.driver.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Persistent TCP connection to the EthioRide server.
 * Supports both request/response (sendAndReceive) and push messages
 * from the server (e.g. MATCH_NOTIFY_DRIVER) via a background receive loop.
 */
public class NetworkClient {
    private static volatile NetworkClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected;
    private Consumer<Message> pushHandler;

    private NetworkClient() {}

    public static NetworkClient getInstance() {
        if (instance == null) {
            synchronized (NetworkClient.class) {
                if (instance == null) instance = new NetworkClient();
            }
        }
        return instance;
    }

    public void connect() throws IOException {
        socket = new Socket(AppConstants.DEFAULT_HOST, AppConstants.DEFAULT_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
        startReceiveLoop();
    }

    /**
     * Register a handler for server-pushed messages (e.g. MATCH_NOTIFY_DRIVER).
     * Must be set before connect() or immediately after.
     */
    public void setPushHandler(Consumer<Message> handler) {
        this.pushHandler = handler;
    }

    /** Send a message and wait for the immediate response (request/response style). */
    public synchronized Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (Message) in.readObject();
    }

    /** Send a message without waiting for a response (fire-and-forget). */
    public synchronized void send(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Background thread that reads server-pushed messages.
     * Request/response messages are handled by sendAndReceive() directly,
     * so this loop only fires the pushHandler for unsolicited server messages.
     */
    private void startReceiveLoop() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) in.readObject();
                    if (pushHandler != null) pushHandler.accept(msg);
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "driver-receive-loop");
        t.setDaemon(true);
        t.start();
    }
}
