package com.ethioride.server;

import com.ethioride.server.config.ServerConfig;
import com.ethioride.server.core.matchmaking.SimpleMatchmaker;
import com.ethioride.server.core.session.ClientRegistry;
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

    // Convenience alias — all server output goes through the file logger
    private static final ServerLogger LOG = ServerLogger.getInstance();

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

        // ── Start the proximity matchmaker ────────────────────────────────────
        SimpleMatchmaker.getInstance().start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOG.info("Server listening on port " + port);
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.submit(new ClientHandler(client));
                } catch (SocketException e) {
                    if (running) LOG.error("Accept error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        SimpleMatchmaker.getInstance().stop();
        threadPool.shutdown();
        LOG.info("Server stopped.");
        ServerLogger.getInstance().close();
    }

    public static void main(String[] args) throws IOException {
        new EthioRideServer().start();
    }

    // ---- Inner handler ----

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String registeredClientId = null; // set after successful login

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
                LOG.error("Handler error: " + e.getMessage());
            } finally {
                // Unregister from ClientRegistry and mark driver offline on disconnect
                if (registeredClientId != null) {
                    ClientRegistry.getInstance().unregister(registeredClientId);
                    SimpleMatchmaker.getInstance().setDriverOffline(registeredClientId);
                    LOG.info("Client disconnected: " + registeredClientId);
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleMessage(Message msg, ObjectOutputStream out) throws IOException {
            switch (msg.getType()) {
                case LOGIN_REQUEST               -> handleLogin(msg, out);
                case REGISTER_REQUEST            -> handleRegister(msg, out);
                case TRIP_REQUEST                -> handleTripRequest(msg, out);
                case TRIP_ACCEPTED               -> handleTripAccepted(msg, out);
                case TRIP_DECLINED               -> handleTripDeclined(msg, out);
                case TRIP_STARTED               -> handleTripStarted(msg, out);
                case TRIP_COMPLETED              -> handleTripCompleted(msg, out);
                case TRIP_CANCELLED              -> handleTripCancelled(msg, out);
                case DRIVER_STATUS_UPDATE        -> handleDriverStatusUpdate(msg, out);
                case DRIVER_LOCATION_UPDATE      -> handleDriverLocationUpdate(msg, out);
                case USER_LIST_REQUEST           -> handleUserList(msg, out);
                case USER_CREATE_REQUEST         -> handleUserCreate(msg, out);
                case USER_DELETE_REQUEST         -> handleUserDelete(msg, out);
                case TRIP_LIST_REQUEST           -> handleTripList(msg, out);
                case DASHBOARD_STATS_REQUEST     -> handleDashboardStats(msg, out);
                case DRIVER_TRIP_HISTORY_REQUEST -> handleDriverTripHistory(msg, out);
                case DRIVER_EARNINGS_REQUEST     -> handleDriverEarnings(msg, out);
                case PASSENGER_TRIP_HISTORY_REQUEST -> handlePassengerTripHistory(msg, out);
                case PRICE_ESTIMATE_REQUEST      -> handlePriceEstimate(msg, out);
                case PRICING_RULES_REQUEST       -> handlePricingRulesGet(msg, out);
                case PRICING_RULES_UPDATE        -> handlePricingRulesUpdate(msg, out);
                case HEARTBEAT                   -> sendAck(out, msg.getSenderId());
                default -> LOG.warn("Unhandled message type: " + msg.getType());
            }
        }

        private void handleLogin(Message msg, ObjectOutputStream out) throws IOException {
            try {
                // Payload format: "phone_or_email:password"
                String[] parts = msg.getPayload().toString().split(":", 2);
                if (parts.length < 2) {
                    out.writeObject(new Message(MessageType.LOGIN_RESPONSE, null, "server"));
                    out.flush();
                    return;
                }
                com.ethioride.shared.dto.UserDTO user =
                    new com.ethioride.server.db.UserRepository()
                        .findByPhoneAndPassword(parts[0], parts[1]);
                if (user != null) {
                    LOG.info("Login: " + user.getFullName() + " (" + user.getRole() + ")");
                    ClientRegistry.getInstance().register(user.getId(), out);
                    registeredClientId = user.getId();
                } else {
                    LOG.warn("Login failed - invalid credentials");
                }
                out.writeObject(new Message(MessageType.LOGIN_RESPONSE, user, "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Login error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Login failed: " + e.getMessage(), "server"));
                out.flush();
            }
        }

        /**
         * DRIVER_STATUS_UPDATE — payload is "ONLINE" or "OFFLINE".
         * Registers/removes the driver from the matchmaker pool.
         */
        private void handleDriverStatusUpdate(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String driverId = msg.getSenderId();
                String status   = msg.getPayload().toString().trim().toUpperCase();

                // Ensure this driver's socket is registered for push messages
                if (registeredClientId == null && driverId != null) {
                    ClientRegistry.getInstance().register(driverId, out);
                    registeredClientId = driverId;
                }

                if ("ONLINE".equals(status)) {
                    SimpleMatchmaker.getInstance().setDriverOnline(driverId);
                    LOG.info("Driver " + driverId + " is now ONLINE (" + SimpleMatchmaker.getInstance().countAvailable() + " available)");
                } else {
                    SimpleMatchmaker.getInstance().setDriverOffline(driverId);
                    LOG.info("Driver " + driverId + " is now OFFLINE");
                }
                out.writeObject(new Message(MessageType.ACK, status, "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Driver status update error: " + e.getMessage());
            }
        }

        /**
         * DRIVER_LOCATION_UPDATE — payload is "lat,lng".
         * Updates the driver's position in the matchmaker for proximity sorting.
         */
        private void handleDriverLocationUpdate(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String driverId = msg.getSenderId();
                String[] coords = msg.getPayload().toString().split(",", 2);
                if (coords.length == 2) {
                    double lat = Double.parseDouble(coords[0].trim());
                    double lng = Double.parseDouble(coords[1].trim());
                    SimpleMatchmaker.getInstance().updateDriverLocation(driverId, lat, lng);
                }
                // No response needed for location updates — fire and forget
            } catch (Exception e) {
                LOG.error("Driver location update error: " + e.getMessage());
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
                LOG.error("Register error: " + e.getMessage());
                out.writeObject(new Message(MessageType.REGISTER_RESPONSE, "ERROR", "server"));
                out.flush();
            }
        }

        private void handleTripRequest(Message msg, ObjectOutputStream out) throws IOException {
            try {
                com.ethioride.shared.dto.TripRequestDTO trip =
                    (com.ethioride.shared.dto.TripRequestDTO) msg.getPayload();
                new com.ethioride.server.db.TripRepository().save(trip);
                // Register passenger socket so we can push TRIP_ACCEPTED later
                if (registeredClientId == null && trip.getPassengerId() != null) {
                    ClientRegistry.getInstance().register(trip.getPassengerId(), out);
                    registeredClientId = trip.getPassengerId();
                }
                LOG.info("Trip queued: " + trip.getPickupLocation() + " -> " + trip.getDropoffLocation());
                out.writeObject(new Message(MessageType.ACK, "QUEUED", "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Trip request error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Trip failed", "server"));
                out.flush();
            }
        }

        /**
         * TRIP_ACCEPTED — driver accepted the trip.
         * Payload: tripId
         * 1. Update DB status → ACCEPTED (already done by matchmaker, but confirm)
         * 2. Push TRIP_ACCEPTED to the passenger with driver info
         * 3. ACK back to driver
         */
        private void handleTripAccepted(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String tripId   = msg.getPayload().toString();
                String driverId = msg.getSenderId();

                com.ethioride.server.db.TripRepository tripRepo = new com.ethioride.server.db.TripRepository();
                tripRepo.updateStatus(tripId, com.ethioride.shared.enums.TripStatus.ACCEPTED);

                // Get driver info to send to passenger
                com.ethioride.shared.dto.UserDTO driver =
                    new com.ethioride.server.db.UserRepository().findById(driverId);

                // Find the passenger for this trip and push notification
                com.ethioride.shared.dto.TripRequestDTO trip = tripRepo.findById(tripId);
                if (trip != null && trip.getPassengerId() != null) {
                    String driverName   = driver != null ? driver.getFullName() : "Your driver";
                    String driverPhone  = driver != null ? driver.getPhone()    : "";
                    double driverRating = driver != null ? driver.getRating()   : 5.0;

                    // Payload to passenger: "driverName|driverPhone|rating|tripId"
                    String passengerPayload = String.format("%s|%s|%.1f|%s",
                        driverName, driverPhone, driverRating, tripId);

                    ClientRegistry.getInstance().push(trip.getPassengerId(),
                        new Message(MessageType.TRIP_ACCEPTED, passengerPayload, "server"));

                    LOG.info("Trip " + tripId + " accepted by driver " + driverId + " — notified passenger");
                }

                out.writeObject(new Message(MessageType.ACK, "ACCEPTED", "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Trip accept error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Accept failed", "server"));
                out.flush();
            }
        }

        /**
         * TRIP_DECLINED — driver declined or timed out.
         * Payload: tripId
         * 1. Put trip back to PENDING so matchmaker tries next driver
         * 2. Mark driver OFFLINE (they clearly don't want trips right now)
         */
        private void handleTripDeclined(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String tripId   = msg.getPayload().toString();
                String driverId = msg.getSenderId();

                // clearDriver sets status = PENDING and driver_id = NULL in one query
                new com.ethioride.server.db.TripRepository().clearDriver(tripId);

                // Mark driver offline — they declined, don't spam them
                SimpleMatchmaker.getInstance().setDriverOffline(driverId);

                LOG.info("Trip " + tripId + " declined by driver " + driverId + " — re-queued");

                out.writeObject(new Message(MessageType.ACK, "DECLINED", "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Trip decline error: " + e.getMessage());
            }
        }

        /**
         * TRIP_STARTED — driver picked up the passenger, trip is now in progress.
         * Payload: tripId
         */
        private void handleTripStarted(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String tripId = msg.getPayload().toString();
                new com.ethioride.server.db.TripRepository()
                    .updateStatus(tripId, com.ethioride.shared.enums.TripStatus.IN_PROGRESS);

                // Notify passenger
                com.ethioride.shared.dto.TripRequestDTO trip =
                    new com.ethioride.server.db.TripRepository().findById(tripId);
                if (trip != null) {
                    ClientRegistry.getInstance().push(trip.getPassengerId(),
                        new Message(MessageType.TRIP_STARTED, tripId, "server"));
                }

                LOG.info("Trip " + tripId + " started (IN_PROGRESS)");
                out.writeObject(new Message(MessageType.ACK, "STARTED", "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Trip start error: " + e.getMessage());
            }
        }

        /**
         * TRIP_COMPLETED — driver marked the trip as done.
         * Payload: tripId
         * 1. Update DB status → COMPLETED
         * 2. Mark driver AVAILABLE again
         * 3. Notify passenger with final fare
         */
        private void handleTripCompleted(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String tripId   = msg.getPayload().toString();
                String driverId = msg.getSenderId();

                com.ethioride.server.db.TripRepository tripRepo =
                    new com.ethioride.server.db.TripRepository();
                tripRepo.updateStatus(tripId, com.ethioride.shared.enums.TripStatus.COMPLETED);

                // Driver is free again
                SimpleMatchmaker.getInstance().setDriverAvailable(driverId);

                // Notify passenger
                com.ethioride.shared.dto.TripRequestDTO trip = tripRepo.findById(tripId);
                if (trip != null) {
                    ClientRegistry.getInstance().push(trip.getPassengerId(),
                        new Message(MessageType.TRIP_COMPLETED,
                            String.format("%.2f", trip.getFare()), "server"));
                }

                LOG.info("Trip " + tripId + " COMPLETED — driver " + driverId + " now available");
                out.writeObject(new Message(MessageType.ACK, "COMPLETED", "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Trip complete error: " + e.getMessage());
            }
        }

        /**
         * TRIP_CANCELLED — passenger or driver cancelled.
         * Payload: tripId
         */
        private void handleTripCancelled(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String tripId   = msg.getPayload().toString();
                String senderId = msg.getSenderId();

                com.ethioride.server.db.TripRepository tripRepo =
                    new com.ethioride.server.db.TripRepository();
                com.ethioride.shared.dto.TripRequestDTO trip = tripRepo.findById(tripId);

                tripRepo.updateStatus(tripId, com.ethioride.shared.enums.TripStatus.CANCELLED);

                if (trip != null) {
                    // If driver had the trip, free them up
                    if (trip.getDriverId() != null) {
                        SimpleMatchmaker.getInstance().setDriverAvailable(trip.getDriverId());
                        // Notify the other party
                        if (senderId.equals(trip.getPassengerId())) {
                            // Passenger cancelled → notify driver
                            ClientRegistry.getInstance().push(trip.getDriverId(),
                                new Message(MessageType.TRIP_CANCELLED, tripId, "server"));
                        } else {
                            // Driver cancelled → notify passenger
                            ClientRegistry.getInstance().push(trip.getPassengerId(),
                                new Message(MessageType.TRIP_CANCELLED, tripId, "server"));
                        }
                    }
                }

                LOG.info("Trip " + tripId + " CANCELLED by " + senderId);
                out.writeObject(new Message(MessageType.ACK, "CANCELLED", "server"));
                out.flush();
            } catch (Exception e) {
                LOG.error("Trip cancel error: " + e.getMessage());
            }
        }

        private void handleUserList(Message msg, ObjectOutputStream out) throws IOException {
            try {
                com.ethioride.server.db.UserRepository repo = new com.ethioride.server.db.UserRepository();
                java.util.List<com.ethioride.shared.dto.UserDTO> users = repo.findAll();
                out.writeObject(new Message(MessageType.USER_LIST_RESPONSE, users, "server"));
                out.flush();
                System.out.printf("[Server] Sent %d users to admin%n", users.size());
            } catch (Exception e) {
                System.err.println("[Server] User list error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Failed to fetch users", "server"));
                out.flush();
            }
        }

        private void handleUserCreate(Message msg, ObjectOutputStream out) throws IOException {
            try {
                // Payload format: "name|phone|email|password|role"
                String[] parts = msg.getPayload().toString().split("\\|", 5);
                if (parts.length < 5) {
                    out.writeObject(new Message(MessageType.USER_CREATE_RESPONSE, "INVALID", "server"));
                    out.flush();
                    return;
                }
                
                com.ethioride.server.db.UserRepository repo = new com.ethioride.server.db.UserRepository();
                
                // Check if phone already exists
                if (repo.phoneExists(parts[1])) {
                    out.writeObject(new Message(MessageType.USER_CREATE_RESPONSE, "PHONE_EXISTS", "server"));
                    out.flush();
                    return;
                }
                
                // Create user
                com.ethioride.shared.dto.UserDTO user = new com.ethioride.shared.dto.UserDTO();
                user.setFullName(parts[0]);
                user.setPhone(parts[1]);
                user.setEmail(parts[2].isEmpty() ? null : parts[2]);
                user.setRole(com.ethioride.shared.enums.UserRole.valueOf(parts[4]));
                
                String userId = repo.save(user, parts[3]);
                user.setId(userId);
                user.setRating(5.0);
                
                out.writeObject(new Message(MessageType.USER_CREATE_RESPONSE, user, "server"));
                out.flush();
                System.out.printf("[Server] Created %s: %s (%s)%n", parts[4], parts[0], parts[1]);
            } catch (Exception e) {
                System.err.println("[Server] User create error: " + e.getMessage());
                e.printStackTrace();
                out.writeObject(new Message(MessageType.USER_CREATE_RESPONSE, "ERROR", "server"));
                out.flush();
            }
        }

        private void handleUserDelete(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String userId = msg.getPayload().toString();
                com.ethioride.server.db.UserRepository repo = new com.ethioride.server.db.UserRepository();
                repo.delete(userId);
                out.writeObject(new Message(MessageType.USER_DELETE_RESPONSE, "OK", "server"));
                out.flush();
                System.out.printf("[Server] Deleted user: %s%n", userId);
            } catch (Exception e) {
                System.err.println("[Server] User delete error: " + e.getMessage());
                out.writeObject(new Message(MessageType.USER_DELETE_RESPONSE, "ERROR", "server"));
                out.flush();
            }
        }

        private void handleTripList(Message msg, ObjectOutputStream out) throws IOException {
            try {
                java.util.List<com.ethioride.shared.dto.TripRequestDTO> trips =
                    new com.ethioride.server.db.TripRepository().findAll();
                out.writeObject(new Message(MessageType.TRIP_LIST_RESPONSE, trips, "server"));
                out.flush();
                System.out.printf("[Server] Sent %d trips to admin%n", trips.size());
            } catch (Exception e) {
                System.err.println("[Server] Trip list error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Failed to fetch trips", "server"));
                out.flush();
            }
        }

        private void handleDashboardStats(Message msg, ObjectOutputStream out) throws IOException {
            try {
                com.ethioride.server.db.UserRepository userRepo = new com.ethioride.server.db.UserRepository();
                com.ethioride.server.db.TripRepository tripRepo = new com.ethioride.server.db.TripRepository();

                java.util.List<com.ethioride.shared.dto.UserDTO> allUsers = userRepo.findAll();
                double[] tripStats = tripRepo.getStats();

                com.ethioride.shared.dto.DashboardStatsDTO stats = new com.ethioride.shared.dto.DashboardStatsDTO();
                stats.setTotalDrivers((int) allUsers.stream()
                    .filter(u -> u.getRole() == com.ethioride.shared.enums.UserRole.DRIVER).count());
                stats.setOnlineDrivers(SimpleMatchmaker.getInstance().countAvailable());
                stats.setTotalPassengers((int) allUsers.stream()
                    .filter(u -> u.getRole() == com.ethioride.shared.enums.UserRole.PASSENGER).count());
                stats.setActiveTrips(tripRepo.countByStatus(com.ethioride.shared.enums.TripStatus.IN_PROGRESS)
                    + tripRepo.countByStatus(com.ethioride.shared.enums.TripStatus.ACCEPTED));
                stats.setCompletedTrips((int) tripStats[1]);
                stats.setCancelledTrips((int) tripStats[2]);
                stats.setTotalRevenue(tripStats[3]);

                out.writeObject(new Message(MessageType.DASHBOARD_STATS_RESPONSE, stats, "server"));
                out.flush();
            } catch (Exception e) {
                System.err.println("[Server] Dashboard stats error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Failed to fetch stats", "server"));
                out.flush();
            }
        }

        private void handleDriverTripHistory(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String driverId = msg.getPayload().toString();
                java.util.List<com.ethioride.shared.dto.TripRequestDTO> trips =
                    new com.ethioride.server.db.TripRepository().findByDriver(driverId);
                out.writeObject(new Message(MessageType.DRIVER_TRIP_HISTORY_RESPONSE, trips, "server"));
                out.flush();
                System.out.printf("[Server] Sent %d trips to driver %s%n", trips.size(), driverId);
            } catch (Exception e) {
                System.err.println("[Server] Driver trip history error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Failed to fetch trip history", "server"));
                out.flush();
            }
        }

        private void handleDriverEarnings(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String driverId = msg.getPayload().toString();
                java.util.List<com.ethioride.shared.dto.TripRequestDTO> trips =
                    new com.ethioride.server.db.TripRepository().findByDriver(driverId);

                double total = trips.stream()
                    .filter(t -> t.getStatus() == com.ethioride.shared.enums.TripStatus.COMPLETED)
                    .mapToDouble(com.ethioride.shared.dto.TripRequestDTO::getFare)
                    .sum();

                java.util.Map<String, Object> earnings = new java.util.HashMap<>();
                earnings.put("total", total);
                earnings.put("tripCount", (int) trips.stream()
                    .filter(t -> t.getStatus() == com.ethioride.shared.enums.TripStatus.COMPLETED)
                    .count());
                earnings.put("trips", trips);

                out.writeObject(new Message(MessageType.DRIVER_EARNINGS_RESPONSE,
                    (java.io.Serializable) earnings, "server"));
                out.flush();
            } catch (Exception e) {
                System.err.println("[Server] Driver earnings error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Failed to fetch earnings", "server"));
                out.flush();
            }
        }

        private void handlePassengerTripHistory(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String passengerId = msg.getPayload().toString();
                java.util.List<com.ethioride.shared.dto.TripRequestDTO> trips =
                    new com.ethioride.server.db.TripRepository().findByPassenger(passengerId);
                out.writeObject(new Message(MessageType.PASSENGER_TRIP_HISTORY_RESPONSE, trips, "server"));
                out.flush();
                System.out.printf("[Server] Sent %d trips to passenger %s%n", trips.size(), passengerId);
            } catch (Exception e) {
                System.err.println("[Server] Passenger trip history error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Failed to fetch trip history", "server"));
                out.flush();
            }
        }

        private void handlePricingRulesGet(Message msg, ObjectOutputStream out) throws IOException {
            try {
                String rules = new com.ethioride.server.db.PricingRepository().getAllRules();
                out.writeObject(new Message(MessageType.PRICING_RULES_RESPONSE, rules, "server"));
                out.flush();
            } catch (Exception e) {
                System.err.println("[Server] Pricing rules get error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Failed to fetch pricing rules", "server"));
                out.flush();
            }
        }

        private void handlePricingRulesUpdate(Message msg, ObjectOutputStream out) throws IOException {
            try {
                // Payload: "ECONOMY:base:perKm:perMin:min:booking|PREMIUM:...|ELITE:..."
                String[] parts = msg.getPayload().toString().split("\\|");
                com.ethioride.server.db.PricingRepository repo = new com.ethioride.server.db.PricingRepository();
                for (String part : parts) {
                    String[] f = part.split(":");
                    if (f.length < 6) continue;
                    repo.updateRule(f[0],
                        Double.parseDouble(f[1]), Double.parseDouble(f[2]),
                        Double.parseDouble(f[3]), Double.parseDouble(f[4]),
                        Double.parseDouble(f[5]));
                }
                out.writeObject(new Message(MessageType.PRICING_RULES_UPDATE_RESPONSE, "OK", "server"));
                out.flush();
                System.out.println("[Server] Pricing rules updated by admin");
            } catch (Exception e) {
                System.err.println("[Server] Pricing rules update error: " + e.getMessage());
                out.writeObject(new Message(MessageType.PRICING_RULES_UPDATE_RESPONSE, "ERROR: " + e.getMessage(), "server"));
                out.flush();
            }
        }

        private void handlePriceEstimate(Message msg, ObjectOutputStream out) throws IOException {
            try {
                // Payload format: "origin|destination|category"
                String[] parts = msg.getPayload().toString().split("\\|", 3);
                if (parts.length < 3) {
                    out.writeObject(new Message(MessageType.ERROR, "Invalid price estimate request", "server"));
                    out.flush();
                    return;
                }
                com.ethioride.server.service.PricingService pricing =
                    new com.ethioride.server.service.PricingService();
                com.ethioride.server.service.PricingService.PriceEstimate estimate =
                    pricing.calculatePrice(parts[0], parts[1],
                        com.ethioride.shared.enums.RideCategory.valueOf(parts[2]));

                com.ethioride.shared.dto.PriceEstimateDTO dto = new com.ethioride.shared.dto.PriceEstimateDTO(
                    estimate.getDistanceKm(), estimate.getDurationMinutes(),
                    estimate.getBaseFare(), estimate.getDistanceFare(),
                    estimate.getTimeFare(), estimate.getBookingFee(),
                    estimate.getTotalFare(), estimate.getCategory()
                );
                dto.setOriginLat(estimate.getOriginLat());
                dto.setOriginLng(estimate.getOriginLng());
                dto.setDestLat(estimate.getDestLat());
                dto.setDestLng(estimate.getDestLng());
                out.writeObject(new Message(MessageType.PRICE_ESTIMATE_RESPONSE, dto, "server"));
                out.flush();
                System.out.printf("[Server] Price estimate: %.2f ETB for %s → %s%n",
                    estimate.getTotalFare(), parts[0], parts[1]);
            } catch (Exception e) {
                System.err.println("[Server] Price estimate error: " + e.getMessage());
                out.writeObject(new Message(MessageType.ERROR, "Price calculation failed: " + e.getMessage(), "server"));
                out.flush();
            }
        }

        private void sendAck(ObjectOutputStream out, String clientId) throws IOException {
            out.writeObject(new Message(MessageType.ACK, "PONG", "server"));
            out.flush();
        }
    }
}
