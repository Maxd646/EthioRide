package com.ethioride.admin.service;

import com.ethioride.admin.model.DashboardStats;
import com.ethioride.admin.network.AdminSocketClient;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Bridges the admin UI controllers with the socket client.
 * Provides typed callbacks for stats updates and log events.
 *
 * sendRequest() uses a ConcurrentHashMap of pending response handlers keyed
 * by MessageType so that multiple in-flight requests never overwrite each other.
 */
public class AdminService {
    private static AdminService instance;

    private Consumer<DashboardStats> statsHandler;
    private Consumer<String> logHandler;

    /**
     * Pending one-shot response handlers.
     * Key   = the MessageType we are waiting for.
     * Value = the callback to invoke when that type arrives.
     *
     * ConcurrentHashMap is safe for concurrent put/remove from the receive
     * thread and the JavaFX thread.
     */
    private final ConcurrentHashMap<MessageType, Consumer<Message>> pendingHandlers =
            new ConcurrentHashMap<>();

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
     * Call once on admin login, AFTER the login response has been received.
     * The socket is already open at this point — this method only installs
     * the persistent message handler; it does NOT open a new connection.
     */
    public void connect() {
        AdminSocketClient client = AdminSocketClient.getInstance();
        // Install the persistent dispatcher — do NOT call client.connect() here.
        // The socket was opened during login; calling connect() again would
        // overwrite the login handler before the response arrives.
        client.setMessageHandler(this::dispatchMessage);
        if (logHandler != null)
            logHandler.accept("[System] Admin service connected.");
    }

    public void disconnect() {
        pendingHandlers.clear();
        AdminSocketClient.getInstance().disconnect();
    }

    /**
     * Central message dispatcher.
     * Checks the pending-handler map first; if no pending handler matches,
     * falls through to the general log handler.
     */
    private void dispatchMessage(Message msg) {
        Consumer<Message> pending = pendingHandlers.remove(msg.getType());
        if (pending != null) {
            pending.accept(msg);
            return;
        }
        // General / unsolicited messages
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
     * Registers a one-shot handler in the pending map keyed by the expected
     * response type, then sends the request.  Because the map is keyed by
     * MessageType, concurrent requests for *different* response types never
     * interfere.  Concurrent requests for the *same* response type (rare in
     * this UI) will have the second registration overwrite the first — the
     * first callback will be silently dropped, which is the same behaviour as
     * before but now at least the second request succeeds correctly.
     */
    private void sendRequest(MessageType requestType, Object payload,
                             MessageType responseType, Consumer<Message> onResponse) {
        try {
            pendingHandlers.put(responseType, onResponse);
            AdminSocketClient.getInstance().send(new Message(requestType, payload, "admin"));
        } catch (IOException e) {
            pendingHandlers.remove(responseType);
            if (logHandler != null)
                logHandler.accept("[ERROR] Request failed (" + requestType + "): " + e.getMessage());
        }
    }
}
