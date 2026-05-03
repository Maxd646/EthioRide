package com.ethioride.server;

import com.ethioride.server.config.ServerConfig;
import com.ethioride.server.core.matchmaking.SimpleMatchmaker;
import com.ethioride.server.db.DBConnection;
import com.ethioride.server.logging.ServerLogger;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Main TCP server. Accepts client connections and dispatches them to the thread pool.
 */
public class EthioRideServer {

    private final int port;
    private final ExecutorService threadPool;
    private volatile boolean running;

    public EthioRideServer() {
        this.port = ServerConfig.getPort();
        this.threadPool = Executors.newFixedThreadPool(ServerConfig.getThreadPoolSize());
    }

    public void start() throws IOException {
        running = true;
        SimpleMatchmaker.getInstance().start();
        ServerLogger.getInstance().info("Server starting on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("[EthioRide] Server listening on port %d%n", port);
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    ServerLogger.getInstance().info("Client connected: " + client.getInetAddress());
                    threadPool.submit(new ClientHandler(client));
                } catch (SocketException e) {
                    if (running) ServerLogger.getInstance().error("Accept error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        SimpleMatchmaker.getInstance().stop();
        threadPool.shutdown();
        ServerLogger.getInstance().info("Server stopped.");
        ServerLogger.getInstance().close();
    }

    public static void main(String[] args) throws IOException {
        new EthioRideServer().start();
    }

    // ---- Inner handler ----

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (ObjectInputStream in  = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                System.out.printf("[EthioRide] Client connected: %s%n", socket.getInetAddress());
                String clientId = null;

                while (!socket.isClosed()) {
                    Message msg = (Message) in.readObject();
                    // Register client on first message so we can push to them
                    if (clientId == null && msg.getSenderId() != null) {
                        clientId = msg.getSenderId();
                        com.ethioride.server.core.session.ClientRegistry.getInstance().register(clientId, out);
                    }
                    handleMessage(msg, out);
                }
            } catch (EOFException | SocketException ignored) {
                // client disconnected
            } catch (Exception e) {
                ServerLogger.getInstance().error("Handler error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleMessage(Message msg, ObjectOutputStream out) throws IOException {
            // Route to appropriate subsystem based on message type
            switch (msg.getType()) {
                case LOGIN_REQUEST         -> handleLogin(msg, out);
                case REGISTER_REQUEST      -> handleRegister(msg, out);
                case TRIP_REQUEST          -> handleTripRequest(msg, out);
                case DRIVER_STATUS_UPDATE  -> handleDriverStatus(msg, out);
                case STATS_REQUEST         -> handleStatsRequest(out);
                case DRIVERS_REQUEST       -> handleDriversRequest(out);
                case TRIPS_REQUEST         -> handleTripsRequest(out);
                case HEARTBEAT             -> sendAck(out, msg.getSenderId());
                default                    -> System.out.println("[EthioRide] Unhandled: " + msg.getType());
            }
        }

        private void handleLogin(Message msg, ObjectOutputStream out) throws IOException {
            try {
                // Payload format: "phone:password"
                String[] parts = msg.getPayload().toString().split(":", 2);
                if (parts.length < 2) {
                    out.writeObject(new Message(MessageType.LOGIN_RESPONSE, null, "server"));
                    out.flush();
                    return;
                }
                com.ethioride.shared.dto.UserDTO user =
                    new com.ethioride.server.db.UserRepository()
                        .findByPhoneAndPassword(parts[0], parts[1]);
                if (user != null) ServerLogger.getInstance().info("Login OK: " + parts[0]);
                else              ServerLogger.getInstance().warn("Login FAILED: " + parts[0]);
                out.writeObject(new Message(MessageType.LOGIN_RESPONSE, user, "server"));
                out.flush();
            } catch (Exception e) {
                System.err.println("[Server] Login error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Login failed", "server"));
                out.flush();
            }
        }

        private void handleRegister(Message msg, ObjectOutputStream out) throws IOException {
            try {
                // Payload format: "name|phone|email|password"
                String[] parts = msg.getPayload().toString().split("\\|", 4);
                if (parts.length < 4) {
                    out.writeObject(new Message(MessageType.REGISTER_RESPONSE, "INVALID", "server"));
                    out.flush();
                    return;
                }
                com.ethioride.server.db.UserRepository repo = new com.ethioride.server.db.UserRepository();
                if (repo.phoneExists(parts[1])) {
                    out.writeObject(new Message(MessageType.REGISTER_RESPONSE, "PHONE_EXISTS", "server"));
                    out.flush();
                    return;
                }
                com.ethioride.shared.dto.UserDTO user = new com.ethioride.shared.dto.UserDTO(
                    null, parts[0], parts[1], parts[2], com.ethioride.shared.enums.UserRole.PASSENGER
                );
                repo.save(user, parts[3]);
                out.writeObject(new Message(MessageType.REGISTER_RESPONSE, "OK", "server"));
                out.flush();
            } catch (Exception e) {
                System.err.println("[Server] Register error: " + e.getMessage());
                out.writeObject(new Message(MessageType.REGISTER_RESPONSE, "ERROR", "server"));
                out.flush();
            }
        }

        private void handleTripRequest(Message msg, ObjectOutputStream out) throws IOException {
            try {
                com.ethioride.shared.dto.TripRequestDTO trip =
                    (com.ethioride.shared.dto.TripRequestDTO) msg.getPayload();
                new com.ethioride.server.db.TripRepository().save(trip);
                System.out.printf("[Server] Trip queued: %s -> %s%n",
                    trip.getPickupLocation(), trip.getDropoffLocation());
                ServerLogger.getInstance().info("Trip queued: " + trip.getPickupLocation() + " -> " + trip.getDropoffLocation());
                out.writeObject(new Message(MessageType.ACK, "QUEUED", "server"));
                out.flush();
            } catch (Exception e) {
                System.err.println("[Server] Trip error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Trip failed", "server"));
                out.flush();
            }
        }

        private void sendAck(ObjectOutputStream out, String clientId) throws IOException {
            out.writeObject(new Message(MessageType.ACK, "PONG", "server"));
            out.flush();
        }

        /**
         * Payload: "ONLINE" or "OFFLINE"
         * Registers/deregisters the driver with the matchmaker.
         */
        private void handleDriverStatus(Message msg, ObjectOutputStream out) throws IOException {
            String driverId = msg.getSenderId();
            String status   = msg.getPayload().toString();
            if ("ONLINE".equalsIgnoreCase(status)) {
                com.ethioride.server.core.matchmaking.SimpleMatchmaker.getInstance().setDriverOnline(driverId);
            } else {
                com.ethioride.server.core.matchmaking.SimpleMatchmaker.getInstance().setDriverOffline(driverId);
            }
            out.writeObject(new Message(MessageType.ACK, status, "server"));
            out.flush();
        }

        private void handleDriversRequest(ObjectOutputStream out) throws IOException {
            try {
                java.util.List<com.ethioride.shared.dto.UserDTO> drivers =
                    new com.ethioride.server.db.UserRepository().findAllDrivers();
                out.writeObject(new Message(MessageType.DRIVERS_RESPONSE,
                    new java.util.ArrayList<>(drivers), "server"));
                out.flush();
            } catch (Exception e) {
                out.writeObject(new Message(MessageType.ERROR, "Drivers unavailable", "server"));
                out.flush();
            }
        }

        private void handleTripsRequest(ObjectOutputStream out) throws IOException {
            try {
                java.util.List<com.ethioride.shared.dto.TripRequestDTO> trips =
                    new com.ethioride.server.db.TripRepository().findAll();
                out.writeObject(new Message(MessageType.TRIPS_RESPONSE,
                    new java.util.ArrayList<>(trips), "server"));
                out.flush();
            } catch (Exception e) {
                out.writeObject(new Message(MessageType.ERROR, "Trips unavailable", "server"));
                out.flush();
            }
        }

        /** Build a DashboardStats from live DB counts and JVM metrics, send to admin. */
        private void handleStatsRequest(ObjectOutputStream out) throws IOException {
            try {
                com.ethioride.server.db.TripRepository tripRepo = new com.ethioride.server.db.TripRepository();
                com.ethioride.shared.enums.TripStatus pending    = com.ethioride.shared.enums.TripStatus.PENDING;
                com.ethioride.shared.enums.TripStatus inProgress = com.ethioride.shared.enums.TripStatus.IN_PROGRESS;
                com.ethioride.shared.enums.TripStatus completed  = com.ethioride.shared.enums.TripStatus.COMPLETED;

                com.ethioride.admin.model.DashboardStats stats = new com.ethioride.admin.model.DashboardStats();

                // Online drivers from matchmaker registry
                int onlineDrivers = com.ethioride.server.core.matchmaking.SimpleMatchmaker.getInstance().getOnlineDriverCount();
                stats.setActiveDrivers(onlineDrivers);

                // Trip counts from DB
                stats.setOngoingTrips(tripRepo.countByStatus(inProgress));

                // JVM memory
                Runtime rt = Runtime.getRuntime();
                long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                long totalMb = rt.totalMemory() / (1024 * 1024);
                stats.setMemUsedMb(usedMb);
                stats.setMemTotalMb(totalMb);
                stats.setServerLoad((double) usedMb / totalMb);

                stats.setHeartbeatStatus("PULSE: OK");
                stats.setUptimePercent(99.98);
                stats.setLatencyMs(System.currentTimeMillis() % 50 + 10); // simulated

                out.writeObject(new Message(MessageType.STATS_RESPONSE, stats, "server"));
                out.flush();
            } catch (Exception e) {
                System.err.println("[Server] Stats error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Stats unavailable", "server"));
                out.flush();
            }
        }
    }
}
