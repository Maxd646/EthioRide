package com.ethioride.driver.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP socket client for the driver app.
 *
 * Architecture: single receive loop owns the ObjectInputStream exclusively.
 * All reads go through the loop — there is NO sendAndReceive() method because
 * two threads reading the same ObjectInputStream causes StreamCorruptedException.
 *
 * For request/response patterns use sendRequest() which temporarily installs
 * a one-shot message handler and restores the previous one after the response.
 *
 * For server-pushed messages (MATCH_NOTIFY_DRIVER) use setPushHandler().
 */
public class NetworkClient {
    private static NetworkClient instance;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private Consumer<Message>  messageHandler; // one-shot request/response handler
    private Consumer<Message>  pushHandler;    // persistent push listener
    private volatile boolean   connected;

    private NetworkClient() {}

    public static NetworkClient getInstance() {
        if (instance == null) instance = new NetworkClient();
        return instance;
    }

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

    public boolean isConnected() { return connected; }

    public void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    public synchronized void send(Message message) throws IOException {
        if (!connected) throw new IOException("Not connected to server.");
        out.writeObject(message);
        out.flush();
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    public Consumer<Message> getMessageHandler() {
        return messageHandler;
    }

    /**
     * Persistent push handler — receives server-initiated messages
     * (MATCH_NOTIFY_DRIVER, TRIP_CANCELLED, etc.) independently of
     * the one-shot request/response handler.
     */
    public void setPushHandler(Consumer<Message> handler) {
        this.pushHandler = handler;
    }

    // ── Async request/response ────────────────────────────────────────────────

    /**
     * Sends a request and calls callback when the matching response arrives.
     * Safe to call from any thread — does NOT block.
     */
    public void sendRequest(MessageType requestType, Object payload,
                            MessageType responseType, Consumer<Message> callback) throws IOException {
        Consumer<Message> original = messageHandler;
        setMessageHandler(msg -> {
            if (msg.getType() == responseType) {
                setMessageHandler(original); // restore previous handler
                callback.accept(msg);
            } else if (original != null) {
                original.accept(msg);
            }
        });
        send(new Message(requestType, payload, "driver"));
    }

    /**
     * Sends a message and waits for a specific response type.
     * Uses a lock so the calling thread blocks until the response arrives
     * or the timeout expires. The receive loop delivers the response.
     *
     * This replaces the old sendAndReceive() which read directly from 'in'
     * and raced with the receive loop.
     *
     * @param timeoutMs maximum wait time in milliseconds
     * @return the response Message, or null on timeout
     */
    public Message sendAndWait(Message request, MessageType expectedResponse, long timeoutMs)
            throws IOException, InterruptedException {
        final Message[] result = {null};
        final Object lock = new Object();

        Consumer<Message> original = messageHandler;
        setMessageHandler(msg -> {
            if (msg.getType() == expectedResponse) {
                synchronized (lock) {
                    result[0] = msg;
                    setMessageHandler(original);
                    lock.notify();
                }
            } else if (original != null) {
                original.accept(msg);
            }
        });

        send(request);

        synchronized (lock) {
            if (result[0] == null) lock.wait(timeoutMs);
        }
        return result[0];
    }

    // ── Receive loop ──────────────────────────────────────────────────────────

    /**
     * Single background thread that owns ObjectInputStream exclusively.
     * Routes each message to either the push handler or the message handler.
     */
    private void startReceiveLoop() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) in.readObject();
                    // Push handler gets server-initiated messages
                    if (pushHandler != null && (
                            msg.getType() == MessageType.MATCH_NOTIFY_DRIVER ||
                            msg.getType() == MessageType.TRIP_CANCELLED)) {
                        pushHandler.accept(msg);
                    } else if (messageHandler != null) {
                        messageHandler.accept(msg);
                    }
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "driver-receiver");
        t.setDaemon(true);
        t.start();
    }
}
