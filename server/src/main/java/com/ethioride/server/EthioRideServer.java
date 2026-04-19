package com.ethioride.server;

import com.ethioride.server.config.ServerConfig;
import com.ethioride.server.db.DBConnection;
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
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("[EthioRide] Server listening on port %d%n", port);
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.submit(new ClientHandler(client));
                } catch (SocketException e) {
                    if (running) System.err.println("[EthioRide] Accept error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        threadPool.shutdown();
        System.out.println("[EthioRide] Server stopped.");
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

                while (!socket.isClosed()) {
                    Message msg = (Message) in.readObject();
                    handleMessage(msg, out);
                }
            } catch (EOFException | SocketException ignored) {
                // client disconnected
            } catch (Exception e) {
                System.err.println("[EthioRide] Handler error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleMessage(Message msg, ObjectOutputStream out) throws IOException {
            // Route to appropriate subsystem based on message type
            switch (msg.getType()) {
                case LOGIN_REQUEST    -> handleLogin(msg, out);
                case REGISTER_REQUEST -> handleRegister(msg, out);
                case TRIP_REQUEST     -> handleTripRequest(msg, out);
                case HEARTBEAT        -> sendAck(out, msg.getSenderId());
                default               -> System.out.println("[EthioRide] Unhandled: " + msg.getType());
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
    }
}
