package com.ethioride.driver.network;

import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.protocol.Message;

import java.io.*;
import java.net.Socket;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private NetworkClient() {}

    public static NetworkClient getInstance() {
        if (instance == null) instance = new NetworkClient();
        return instance;
    }

    public void connect() throws IOException {
        socket = new Socket(AppConstants.DEFAULT_HOST, AppConstants.DEFAULT_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
    }

    public Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (Message) in.readObject();
    }

    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
