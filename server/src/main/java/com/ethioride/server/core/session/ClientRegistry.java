package com.ethioride.server.core.session;

import com.ethioride.shared.protocol.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active client output streams by their sender ID (driverId, passengerId).
 * Allows the server to push messages to specific connected clients.
 */
public class ClientRegistry {
    private static volatile ClientRegistry instance;

    // clientId → their ObjectOutputStream
    private final ConcurrentHashMap<String, ObjectOutputStream> clients = new ConcurrentHashMap<>();

    private ClientRegistry() {}

    public static ClientRegistry getInstance() {
        if (instance == null) {
            synchronized (ClientRegistry.class) {
                if (instance == null) instance = new ClientRegistry();
            }
        }
        return instance;
    }

    public void register(String clientId, ObjectOutputStream out) {
        clients.put(clientId, out);
    }

    public void unregister(String clientId) {
        clients.remove(clientId);
    }

    /** Push a message to a specific client. Returns false if client not found. */
    public boolean push(String clientId, Message message) {
        ObjectOutputStream out = clients.get(clientId);
        if (out == null) return false;
        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
            return true;
        } catch (IOException e) {
            clients.remove(clientId); // dead connection
            return false;
        }
    }

    public boolean isOnline(String clientId) {
        return clients.containsKey(clientId);
    }
}
