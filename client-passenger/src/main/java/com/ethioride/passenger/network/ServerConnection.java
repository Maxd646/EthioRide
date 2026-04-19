package com.ethioride.passenger.network;

import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;
import com.ethioride.shared.dto.UserDTO;

import java.io.*;
import java.net.Socket;

/**
 * Handles TCP socket connection to the EthioRide server.
 * Sends requests and reads responses synchronously (request/response style).
 */
public class ServerConnection {

    private static final String HOST = "localhost";
    private static final int    PORT = 9090;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        // IMPORTANT: create OOS before OIS to avoid deadlock
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
        System.out.println("[Client] Connected to server.");
    }

    /** Send a message and wait for the server's response. */
    public Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (Message) in.readObject();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
