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
        sendRequest(MessageType.USER_LIST_REQUEST, null,
            MessageType.USER_LIST_RESPONSE, msg -> {
                @SuppressWarnings("unchecked")
                java.util.List<com.ethioride.shared.dto.UserDTO> users =
                    (java.util.List<com.ethioride.shared.dto.UserDTO>) msg.getPayload();
                callback.accept(users);
            });
    }

    /** Requests the list of all trips from the server. */
    public void requestTripList(Consumer<java.util.List<com.ethioride.shared.dto.TripRequestDTO>> callback) {
        sendRequest(MessageType.TRIP_LIST_REQUEST, null,
            MessageType.TRIP_LIST_RESPONSE, msg -> {
                @SuppressWarnings("unchecked")
                java.util.List<com.ethioride.shared.dto.TripRequestDTO> trips =
                    (java.util.List<com.ethioride.shared.dto.TripRequestDTO>) msg.getPayload();
                callback.accept(trips);
            });
    }

    /** Requests live dashboard statistics from the server. */
    public void requestDashboardStats(Consumer<com.ethioride.shared.dto.DashboardStatsDTO> callback) {
        sendRequest(MessageType.DASHBOARD_STATS_REQUEST, null,
            MessageType.DASHBOARD_STATS_RESPONSE, msg ->
                callback.accept((com.ethioride.shared.dto.DashboardStatsDTO) msg.getPayload()));
    }

    /** Creates a new user (driver or admin). */
    public void createUser(String name, String phone, String email, String password,
                          com.ethioride.shared.enums.UserRole role,
                          Consumer<Object> callback) {
        String payload = String.format("%s|%s|%s|%s|%s", name, phone, email, password, role.name());
        sendRequest(MessageType.USER_CREATE_REQUEST, payload,
            MessageType.USER_CREATE_RESPONSE, msg -> callback.accept(msg.getPayload()));
    }

    /** Deletes a user by ID. */
    public void deleteUser(String userId, Consumer<String> callback) {
        sendRequest(MessageType.USER_DELETE_REQUEST, userId,
            MessageType.USER_DELETE_RESPONSE, msg -> callback.accept(msg.getPayload().toString()));
    }

    /** Requests pricing rules from the server. */
    public void requestPricingRules(Consumer<String> callback) {
        sendRequest(MessageType.PRICING_RULES_REQUEST, null,
            MessageType.PRICING_RULES_RESPONSE, msg -> callback.accept(msg.getPayload().toString()));
    }

    /** Saves updated pricing rules to the server. */
    public void savePricingRules(String payload, Consumer<String> callback) {
        sendRequest(MessageType.PRICING_RULES_UPDATE, payload,
            MessageType.PRICING_RULES_UPDATE_RESPONSE, msg -> callback.accept(msg.getPayload().toString()));
    }

    /**
     * Generic request-response helper.
     * Temporarily overrides the message handler to capture a specific response type,
     * then restores the original handler.
     */
    private void sendRequest(MessageType requestType, Object payload,
                             MessageType responseType, Consumer<Message> onResponse) {
        try {
            AdminSocketClient client = AdminSocketClient.getInstance();
            Consumer<Message> original = client.getMessageHandler();

            client.setMessageHandler(msg -> {
                if (msg.getType() == responseType) {
                    client.setMessageHandler(original);
                    onResponse.accept(msg);
                } else if (original != null) {
                    original.accept(msg);
                }
            });

            client.send(new Message(requestType, payload, "admin"));
        } catch (IOException e) {
            if (logHandler != null)
                logHandler.accept("[ERROR] Request failed (" + requestType + "): " + e.getMessage());
        }
    }
}
