package com.ethioride.admin.service;

import com.ethioride.admin.model.DashboardStats;
import com.ethioride.admin.network.AdminSocketClient;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Bridges the admin UI controllers with the socket client.
 * Provides typed callbacks for stats updates and log events.
 */
public class AdminService {
    private static AdminService instance;

    private Consumer<DashboardStats> statsHandler;
    private Consumer<String> logHandler;

    private AdminService() {}

    public static AdminService getInstance() {
        if (instance == null) instance = new AdminService();
        return instance;
    }

    public void setStatsHandler(Consumer<DashboardStats> handler) {
        this.statsHandler = handler;
    }

    public void setLogHandler(Consumer<String> handler) {
        this.logHandler = handler;
    }

    /**
     * Connects to the server and wires up the message handler.
     * Call once on admin login.
     */
    public void connect() {
        AdminSocketClient client = AdminSocketClient.getInstance();
        client.setMessageHandler(this::handleMessage);
        try {
            client.connect();
        } catch (IOException e) {
            if (logHandler != null)
                logHandler.accept("[ERROR] Could not connect to server: " + e.getMessage());
        }
    }

    public void disconnect() {
        AdminSocketClient.getInstance().disconnect();
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case HEARTBEAT -> {
                if (logHandler != null)
                    logHandler.accept("[HEARTBEAT] Server pulse received.");
            }
            case ERROR -> {
                if (logHandler != null)
                    logHandler.accept("[ERROR] " + msg.getPayload());
            }
            default -> {
                if (logHandler != null)
                    logHandler.accept("[" + msg.getType() + "] " + msg.getPayload());
            }
        }
    }

    /** Sends a heartbeat ping to the server. */
    public void sendHeartbeat(String adminId) {
        try {
            AdminSocketClient.getInstance().send(
                new Message(MessageType.HEARTBEAT, "PING", adminId)
            );
        } catch (IOException e) {
            if (logHandler != null)
                logHandler.accept("[ERROR] Heartbeat failed: " + e.getMessage());
        }
    }

    /** Requests the list of all users from the server. */
    public void requestUserList(Consumer<java.util.List<com.ethioride.shared.dto.UserDTO>> callback) {
        try {
            AdminSocketClient client = AdminSocketClient.getInstance();
            
            // Set up a one-time handler for the response
            Consumer<Message> originalHandler = client.getMessageHandler();
            client.setMessageHandler(msg -> {
                if (msg.getType() == MessageType.USER_LIST_RESPONSE) {
                    @SuppressWarnings("unchecked")
                    java.util.List<com.ethioride.shared.dto.UserDTO> users = 
                        (java.util.List<com.ethioride.shared.dto.UserDTO>) msg.getPayload();
                    callback.accept(users);
                    client.setMessageHandler(originalHandler); // Restore original handler
                } else {
                    originalHandler.accept(msg); // Pass through other messages
                }
            });
            
            client.send(new Message(MessageType.USER_LIST_REQUEST, null, "admin"));
        } catch (IOException e) {
            if (logHandler != null)
                logHandler.accept("[ERROR] User list request failed: " + e.getMessage());
        }
    }

    /** Creates a new user (driver or admin). */
    public void createUser(String name, String phone, String email, String password, 
                          com.ethioride.shared.enums.UserRole role,
                          Consumer<Object> callback) {
        try {
            AdminSocketClient client = AdminSocketClient.getInstance();
            
            // Set up a one-time handler for the response
            Consumer<Message> originalHandler = client.getMessageHandler();
            client.setMessageHandler(msg -> {
                if (msg.getType() == MessageType.USER_CREATE_RESPONSE) {
                    callback.accept(msg.getPayload());
                    client.setMessageHandler(originalHandler); // Restore original handler
                } else {
                    originalHandler.accept(msg); // Pass through other messages
                }
            });
            
            // Format: "name|phone|email|password|role"
            String payload = String.format("%s|%s|%s|%s|%s", name, phone, email, password, role.name());
            client.send(new Message(MessageType.USER_CREATE_REQUEST, payload, "admin"));
        } catch (IOException e) {
            if (logHandler != null)
                logHandler.accept("[ERROR] User create request failed: " + e.getMessage());
        }
    }

    /** Deletes a user by ID. */
    public void deleteUser(String userId, Consumer<String> callback) {
        try {
            AdminSocketClient client = AdminSocketClient.getInstance();
            
            // Set up a one-time handler for the response
            Consumer<Message> originalHandler = client.getMessageHandler();
            client.setMessageHandler(msg -> {
                if (msg.getType() == MessageType.USER_DELETE_RESPONSE) {
                    callback.accept(msg.getPayload().toString());
                    client.setMessageHandler(originalHandler); // Restore original handler
                } else {
                    originalHandler.accept(msg); // Pass through other messages
                }
            });
            
            client.send(new Message(MessageType.USER_DELETE_REQUEST, userId, "admin"));
        } catch (IOException e) {
            if (logHandler != null)
                logHandler.accept("[ERROR] User delete request failed: " + e.getMessage());
        }
    }
}
