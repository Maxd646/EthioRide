package com.ethioride.passenger.network;

import com.ethioride.shared.protocol.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP socket connection to the EthioRide server.
 *
 * Two modes:
 *  - Synchronous (sendAndReceive): used for price estimates, trip requests.
 *  - Persistent push (startListening): used after booking to receive
 *    TRIP_ACCEPTED, TRIP_STARTED, TRIP_COMPLETED, TRIP_CANCELLED pushes.
 */
public class ServerConnection {

    private static final String HOST = "localhost";
    private static final int    PORT = 9090;

    // Singleton persistent connection for push messages
    private static ServerConnection persistentInstance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private Consumer<Message>  pushHandler;
    private volatile boolean   listening;

    public ServerConnection() {}

    /** Returns the singleton persistent connection (creates if needed). */
    public static synchronized ServerConnection getPersistent() {
        if (persistentInstance == null) persistentInstance = new ServerConnection();
        return persistentInstance;
    }

    /** Closes and resets the singleton (call on sign-out). */
    public static synchronized void resetPersistent() {
        if (persistentInstance != null) {
            persistentInstance.close();
            persistentInstance = null;
        }
    }

    public void connect() throws IOException {
        if (isConnected()) return;
        socket = new Socket(HOST, PORT);
        // IMPORTANT: create OOS before OIS to avoid deadlock
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
    }

    /** Send a message and wait for the server's response (synchronous). */
    public Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (Message) in.readObject();
    }

    /** Send a message without waiting for a response (fire-and-forget). */
    public void send(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    /**
     * Start a background receive loop that calls pushHandler for every
     * server-pushed message. Used after booking to receive trip lifecycle events.
     */
    public void startListening(Consumer<Message> handler) {
        this.pushHandler = handler;
        if (listening) return;
        listening = true;
        Thread t = new Thread(() -> {
            try {
                while (listening && isConnected()) {
                    Message msg = (Message) in.readObject();
                    if (pushHandler != null) pushHandler.accept(msg);
                }
            } catch (Exception e) {
                listening = false;
            }
        }, "passenger-push-listener");
        t.setDaemon(true);
        t.start();
    }

    public void stopListening() { listening = false; }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        listening = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
    }
}
