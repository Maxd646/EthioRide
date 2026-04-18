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
}
