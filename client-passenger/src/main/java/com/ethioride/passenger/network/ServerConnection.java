package com.ethioride.passenger.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP socket connection to the EthioRide server.
 *
 * Architecture: a single background receive loop owns ObjectInputStream
 * exclusively — no other thread reads from it. This prevents the
 * StreamCorruptedException that occurs when sendAndReceive() and
 * startListening() both read from the same stream simultaneously.
 *
 * Usage patterns:
 *  - Short-lived connections (price estimate, login): create new instance,
 *    connect(), sendAndWait(), close().
 *  - Persistent push connection (trip lifecycle): use getPersistent(),
 *    connect(), send(TRIP_REQUEST), startListening(handler).
 */
public class ServerConnection {

    // ── Singleton persistent connection ──────────────────────────────────────
    private static ServerConnection persistentInstance;

    public static synchronized ServerConnection getPersistent() {
        if (persistentInstance == null) persistentInstance = new ServerConnection();
        return persistentInstance;
    }

    public static synchronized void resetPersistent() {
        if (persistentInstance != null) {
            persistentInstance.close();
            persistentInstance = null;
        }
    }

    // ── Instance state ────────────────────────────────────────────────────────
    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private Consumer<Message>  messageHandler; // one-shot request/response
    private Consumer<Message>  pushHandler;    // persistent push listener
    private volatile boolean   connected;

    public ServerConnection() {}

    // ── Connection ────────────────────────────────────────────────────────────

    public void connect() throws IOException {
        if (connected) return;
        socket = new Socket(AppConstants.DEFAULT_HOST, AppConstants.DEFAULT_PORT);
        out    = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in     = new ObjectInputStream(socket.getInputStream());
        connected = true;
        startReceiveLoop(); // single thread owns 'in' from here on
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    public synchronized void send(Message message) throws IOException {
        if (!connected) throw new IOException("Not connected to server.");
        out.writeObject(message);
        out.flush();
    }

    // ── Async request/response ────────────────────────────────────────────────

    /**
     * Sends a request and blocks until the matching response type arrives
     * (or timeout expires). Safe — the receive loop delivers the response.
     *
     * Use this for price estimates, login, ride history requests, etc.
     */
    public Message sendAndWait(Message request, com.ethioride.shared.protocol.MessageType expectedType,
                               long timeoutMs) throws IOException, InterruptedException {
        final Message[] result = {null};
        final Object lock = new Object();
        Consumer<Message> original = messageHandler;

        messageHandler = msg -> {
            if (msg.getType() == expectedType) {
                synchronized (lock) {
                    result[0] = msg;
                    messageHandler = original;
                    lock.notify();
                }
            } else if (original != null) {
                original.accept(msg);
            }
        };

        send(request);
        synchronized (lock) {
            if (result[0] == null) lock.wait(timeoutMs);
        }
        return result[0];
    }

    // ── Push listener ─────────────────────────────────────────────────────────

    /**
     * Registers a persistent handler for server-pushed messages
     * (TRIP_ACCEPTED, TRIP_STARTED, TRIP_COMPLETED, TRIP_CANCELLED).
     * Call after sending TRIP_REQUEST on the persistent connection.
     */
    public void startListening(Consumer<Message> handler) {
        this.pushHandler = handler;
    }

    public void stopListening() {
        this.pushHandler = null;
    }

    // ── Receive loop ──────────────────────────────────────────────────────────

    /**
     * Single background thread — the only reader of ObjectInputStream.
     * Routes each message to either the one-shot handler or the push handler.
     */
    private void startReceiveLoop() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) in.readObject();
                    if (pushHandler != null) {
                        pushHandler.accept(msg);
                    } else if (messageHandler != null) {
                        messageHandler.accept(msg);
                    }
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "passenger-receiver");
        t.setDaemon(true);
        t.start();
    }
}
