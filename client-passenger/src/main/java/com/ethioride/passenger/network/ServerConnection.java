package com.ethioride.passenger.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP socket connection for the passenger client.
 *
 * Two modes:
 *  1. Short-lived (new ServerConnection()) — connect, sendAndWait, close.
 *     Used for login, price estimates, trip history, etc.
 *
 *  2. Persistent (ServerConnection.getPersistent()) — stays open for the
 *     whole session so the server can push TRIP_ACCEPTED / TRIP_STARTED /
 *     TRIP_COMPLETED messages back to the passenger.
 */
public class ServerConnection {

    // ── Persistent singleton ──────────────────────────────────────────────────
    private static volatile ServerConnection persistent;

    public static ServerConnection getPersistent() {
        if (persistent == null) {
            synchronized (ServerConnection.class) {
                if (persistent == null) persistent = new ServerConnection();
            }
        }
        return persistent;
    }

    public static void resetPersistent() {
        synchronized (ServerConnection.class) {
            if (persistent != null) {
                try { persistent.close(); } catch (Exception ignored) {}
                persistent = null;
            }
        }
    }

    // ── Instance state ────────────────────────────────────────────────────────
    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private volatile boolean   connected;

    // Push listener — receives server-initiated messages on the persistent conn
    private volatile Consumer<Message> pushHandler;
    private volatile boolean           listening;
    private Thread                     listenThread;

    public ServerConnection() {}

    // ── Connection ────────────────────────────────────────────────────────────

    public void connect() throws IOException {
        if (connected) return;
        socket = new Socket(AppConstants.DEFAULT_HOST, AppConstants.DEFAULT_PORT);
        out    = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in     = new ObjectInputStream(socket.getInputStream());
        connected = true;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void close() {
        connected = false;
        listening = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    public synchronized void send(Message message) throws IOException {
        if (!connected) throw new IOException("Not connected to server.");
        out.writeObject(message);
        out.flush();
    }

    // ── Request / Response ────────────────────────────────────────────────────

    /**
     * Sends a message and blocks until a response of the expected type arrives
     * or the timeout elapses.
     *
     * Safe to call from a background thread. Never call from the JavaFX thread.
     */
    public Message sendAndWait(Message request, MessageType expectedType, long timeoutMs)
            throws IOException, ClassNotFoundException {
        send(request);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            socket.setSoTimeout((int) Math.max(1, deadline - System.currentTimeMillis()));
            try {
                Message response = (Message) in.readObject();
                if (response.getType() == expectedType || response.getType() == MessageType.ERROR) {
                    return response;
                }
                // Unexpected message type — keep reading until timeout
            } catch (java.net.SocketTimeoutException e) {
                break;
            }
        }
        return null;
    }

    // ── Push listener (persistent connection only) ────────────────────────────

    /**
     * Starts a background thread that reads server-pushed messages and
     * dispatches them to the given handler.
     * Call this after connecting the persistent connection.
     */
    public void startListening(Consumer<Message> handler) {
        this.pushHandler = handler;
        if (listening) return;
        listening = true;
        listenThread = new Thread(() -> {
            try {
                while (listening && connected) {
                    Message msg = (Message) in.readObject();
                    if (pushHandler != null) pushHandler.accept(msg);
                }
            } catch (Exception e) {
                // connection closed or error — stop listening
            }
            listening = false;
        }, "passenger-push-listener");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public void stopListening() {
        listening = false;
    }

    // ── Legacy alias used by some screens ────────────────────────────────────

    /**
     * Alias for sendAndWait — some screens call sendAndReceive.
     */
    public Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        send(request);
        socket.setSoTimeout(8000);
        try {
            return (Message) in.readObject();
        } catch (java.net.SocketTimeoutException e) {
            return null;
        }
    }
}
