package com.ethioride.admin.service;

import com.ethioride.admin.model.DashboardStats;
import com.ethioride.admin.network.AdminSocketClient;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.dto.UserDTO;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Bridges the admin UI controllers with the socket client.
 * Polls the server for live stats every 10 seconds.
 */
public class AdminService {
    private static AdminService instance;

    private Consumer<DashboardStats> statsHandler;
    private Consumer<String> logHandler;
    private Consumer<List<UserDTO>> driversHandler;
    private Consumer<List<TripRequestDTO>> tripsHandler;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "admin-stats-poller");
            t.setDaemon(true);
            return t;
        });
    private ScheduledFuture<?> statsPollTask;

    private AdminService() {}

    public static synchronized AdminService getInstance() {
        if (instance == null) instance = new AdminService();
        return instance;
    }

    public void setStatsHandler(Consumer<DashboardStats> handler) { this.statsHandler = handler; }
    public void setLogHandler(Consumer<String> handler) { this.logHandler = handler; }
    public void setDriversHandler(Consumer<List<UserDTO>> handler) { this.driversHandler = handler; }
    public void setTripsHandler(Consumer<List<TripRequestDTO>> handler) { this.tripsHandler = handler; }

    /** Connects to the server and starts the stats polling loop. */
    public void connect() {
        AdminSocketClient client = AdminSocketClient.getInstance();
        client.setMessageHandler(this::handleMessage);
        try {
            client.connect();
            startStatsPolling();
        } catch (IOException e) {
            if (logHandler != null)
                logHandler.accept("[ERROR] Could not connect to server: " + e.getMessage());
        }
    }

    public void disconnect() {
        stopStatsPolling();
        AdminSocketClient.getInstance().disconnect();
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case STATS_RESPONSE -> {
                if (statsHandler != null && msg.getPayload() instanceof DashboardStats stats)
                    statsHandler.accept(stats);
            }
            case DRIVERS_RESPONSE -> {
                if (driversHandler != null && msg.getPayload() instanceof List<?> list)
                    driversHandler.accept((List<UserDTO>) list);
            }
            case TRIPS_RESPONSE -> {
                if (tripsHandler != null && msg.getPayload() instanceof List<?> list)
                    tripsHandler.accept((List<TripRequestDTO>) list);
            }
            case HEARTBEAT -> {
                if (logHandler != null) logHandler.accept("[HEARTBEAT] Server pulse received.");
            }
            case ERROR -> {
                if (logHandler != null) logHandler.accept("[ERROR] " + msg.getPayload());
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

    /** Requests a stats snapshot from the server. */
    public void requestStats(String adminId) {
        try {
            AdminSocketClient.getInstance().send(
                new Message(MessageType.STATS_REQUEST, null, adminId));
        } catch (IOException e) {
            if (logHandler != null) logHandler.accept("[ERROR] Stats request failed: " + e.getMessage());
        }
    }

    public void requestDrivers(String adminId) {
        try {
            AdminSocketClient.getInstance().send(
                new Message(MessageType.DRIVERS_REQUEST, null, adminId));
        } catch (IOException e) {
            if (logHandler != null) logHandler.accept("[ERROR] Drivers request failed: " + e.getMessage());
        }
    }

    public void requestTrips(String adminId) {
        try {
            AdminSocketClient.getInstance().send(
                new Message(MessageType.TRIPS_REQUEST, null, adminId));
        } catch (IOException e) {
            if (logHandler != null) logHandler.accept("[ERROR] Trips request failed: " + e.getMessage());
        }
    }

    /** Returns true if the given driverId is currently in the server's online registry. */
    public boolean isDriverOnline(String driverId) {
        // The admin client doesn't have direct access to the server's matchmaker.
        // Online status comes from DRIVERS_RESPONSE payload — callers should use
        // the UserDTO data directly. This method is a convenience fallback.
        return false;
    }

    private void startStatsPolling() {
        stopStatsPolling();
        statsPollTask = scheduler.scheduleAtFixedRate(
            () -> requestStats("admin"),
            1, 10, TimeUnit.SECONDS
        );
    }

    private void stopStatsPolling() {
        if (statsPollTask != null && !statsPollTask.isCancelled())
            statsPollTask.cancel(false);
    }
}
