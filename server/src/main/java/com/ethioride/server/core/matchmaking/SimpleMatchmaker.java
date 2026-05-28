package com.ethioride.server.core.matchmaking;

import com.ethioride.server.core.session.ClientRegistry;
import com.ethioride.server.db.TripRepository;
import com.ethioride.server.logging.ServerLogger;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Proximity-aware matchmaker.
 *
 * Flow:
 *  1. Passenger requests a trip → saved as PENDING in DB.
 *  2. Every 5 seconds this cycle runs:
 *     a. Fetch all PENDING trips.
 *     b. For each trip, find the nearest AVAILABLE driver (by Haversine distance).
 *     c. Assign that driver → DB status → ACCEPTED, driver status → ON_TRIP.
 *     d. Push MATCH_NOTIFY_DRIVER to the driver's live socket.
 *  3. When a driver goes ONLINE they register here with their last known location.
 *  4. When a driver goes OFFLINE or is assigned, they are removed from the pool.
 *
 * Driver statuses (in-memory):
 *   AVAILABLE  — online and free
 *   ON_TRIP    — currently serving a passenger
 *   OFFLINE    — not connected / toggled offline
 */
public class SimpleMatchmaker {

    // ── Driver status ─────────────────────────────────────────────────────────

    public enum DriverStatus { AVAILABLE, ON_TRIP, OFFLINE }

    /** Snapshot of a driver's current state. */
    public static class DriverInfo {
        public final String driverId;
        public volatile DriverStatus status;
        public volatile double lat;   // last known latitude
        public volatile double lng;   // last known longitude

        public DriverInfo(String driverId, double lat, double lng) {
            this.driverId = driverId;
            this.status   = DriverStatus.AVAILABLE;
            this.lat      = lat;
            this.lng      = lng;
        }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static SimpleMatchmaker instance;

    private static final ServerLogger LOG = ServerLogger.getInstance();

    private SimpleMatchmaker() {}

    public static synchronized SimpleMatchmaker getInstance() {
        if (instance == null) instance = new SimpleMatchmaker();
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    // driverId → DriverInfo
    private final Map<String, DriverInfo> drivers = new ConcurrentHashMap<>();

    // Guards the match cycle — prevents double-assignment under concurrent access
    private final ReentrantLock matchLock = new ReentrantLock();

    private final TripRepository tripRepo = new TripRepository();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "matchmaker-thread");
                t.setDaemon(true);
                return t;
            });

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Start the matchmaking loop — runs every 5 seconds. */
    public void start() {
        scheduler.scheduleAtFixedRate(this::runMatchCycle, 2, 5, TimeUnit.SECONDS);
        LOG.info("Matchmaker started — proximity matching every 5s.");
    }

    public void stop() {
        scheduler.shutdown();
    }

    // ── Driver registry ───────────────────────────────────────────────────────

    /**
     * Called when a driver sends DRIVER_STATUS_UPDATE with "ONLINE".
     * Registers them as AVAILABLE with a default location (Addis Ababa center)
     * until a DRIVER_LOCATION_UPDATE arrives.
     */
    public void setDriverOnline(String driverId) {
        drivers.compute(driverId, (id, existing) -> {
            if (existing == null) {
                // Default to Addis Ababa center until real location arrives
                return new DriverInfo(id, 9.0320, 38.7469);
            }
            existing.status = DriverStatus.AVAILABLE;
            return existing;
        });
        LOG.info("Matchmaker driver ONLINE: " + driverId + " (available drivers: " + countAvailable() + ")");
    }

    /**
     * Called when a driver sends DRIVER_STATUS_UPDATE with "OFFLINE".
     */
    public void setDriverOffline(String driverId) {
        DriverInfo info = drivers.get(driverId);
        if (info != null) info.status = DriverStatus.OFFLINE;
        LOG.info("Matchmaker driver OFFLINE: " + driverId);
    }

    /**
     * Called when a driver sends DRIVER_LOCATION_UPDATE.
     * Payload format: "lat,lng"
     */
    public void updateDriverLocation(String driverId, double lat, double lng) {
        drivers.compute(driverId, (id, existing) -> {
            if (existing == null) {
                DriverInfo info = new DriverInfo(id, lat, lng);
                info.status = DriverStatus.OFFLINE; // location update alone doesn't make them available
                return info;
            }
            existing.lat = lat;
            existing.lng = lng;
            return existing;
        });
    }

    /** Mark a driver as ON_TRIP (called after successful match). */
    public void setDriverOnTrip(String driverId) {
        DriverInfo info = drivers.get(driverId);
        if (info != null) info.status = DriverStatus.ON_TRIP;
    }

    /** Mark a driver as AVAILABLE again (called when trip completes/cancels). */
    public void setDriverAvailable(String driverId) {
        DriverInfo info = drivers.get(driverId);
        if (info != null) info.status = DriverStatus.AVAILABLE;
    }

    public int countAvailable() {
        return (int) drivers.values().stream()
                .filter(d -> d.status == DriverStatus.AVAILABLE).count();
    }

    /** Returns a snapshot of all known drivers (for map display). */
    public java.util.List<DriverInfo> getAllDrivers() {
        return new java.util.ArrayList<>(drivers.values());
    }

    // ── Match cycle ───────────────────────────────────────────────────────────

    /**
     * Core proximity matching logic:
     *  1. Fetch all PENDING trips from DB (oldest first).
     *  2. For each trip, find the nearest AVAILABLE driver by Haversine distance.
     *  3. Assign driver → update DB → mark driver ON_TRIP → push to driver socket.
     *
     * ReentrantLock ensures the cycle is atomic — no double-assignment.
     */
    private void runMatchCycle() {
        matchLock.lock();
        try {
            List<TripRequestDTO> pending = tripRepo.findPending();
            if (pending.isEmpty()) return;

            LOG.info("Matchmaker " + pending.size() + " pending trip(s), " + countAvailable() + " available driver(s).");

            for (TripRequestDTO trip : pending) {
                // Find nearest available driver to the pickup location
                DriverInfo nearest = findNearestDriver(trip.getPickupLat(), trip.getPickupLng());

                if (nearest == null) {
                    LOG.info("Matchmaker no available drivers — will retry next cycle.");
                    break; // no point checking more trips
                }

                // Assign in DB: status → ACCEPTED, driver_id set
                // Re-check the trip hasn't been cancelled between findPending() and now
                TripRequestDTO fresh = tripRepo.findById(trip.getTripId());
                if (fresh == null || fresh.getStatus() == com.ethioride.shared.enums.TripStatus.CANCELLED) {
                    LOG.info("Matchmaker skipping trip " + trip.getTripId() + " — cancelled before assignment.");
                    continue;
                }
                tripRepo.assignDriver(trip.getTripId(), nearest.driverId);

                // Mark driver as ON_TRIP so they won't be matched again
                setDriverOnTrip(nearest.driverId);

                double distKm = haversineKm(
                        nearest.lat, nearest.lng,
                        trip.getPickupLat(), trip.getPickupLng());

                LOG.info("Matchmaker matched trip " + trip.getTripId() + " -> driver " + nearest.driverId + " (" + String.format("%.1f", distKm) + " km away)");

                // Push MATCH_NOTIFY_DRIVER to the driver's live socket
                trip.setDriverId(nearest.driverId);
                boolean pushed = ClientRegistry.getInstance().push(
                        nearest.driverId,
                        new Message(MessageType.MATCH_NOTIFY_DRIVER, trip, "server")
                );

                if (!pushed) {
                    // Driver socket is dead — roll back: put trip back to PENDING, driver OFFLINE
                    LOG.error("Matchmaker driver " + nearest.driverId + " unreachable — rolling back assignment.");
                    tripRepo.updateStatus(trip.getTripId(),
                            com.ethioride.shared.enums.TripStatus.PENDING);
                    setDriverOffline(nearest.driverId);
                }
            }
        } catch (Exception e) {
            LOG.error("Matchmaker error during match cycle: " + e.getMessage());
        } finally {
            matchLock.unlock();
        }
    }

    /**
     * Finds the nearest AVAILABLE driver to the given coordinates.
     * Falls back to any available driver if pickup coords are (0,0)
     * (i.e. passenger didn't send GPS — text-only address).
     *
     * @return nearest DriverInfo, or null if no drivers are available
     */
    private DriverInfo findNearestDriver(double pickupLat, double pickupLng) {
        boolean hasCoords = pickupLat != 0.0 || pickupLng != 0.0;

        return drivers.values().stream()
                .filter(d -> d.status == DriverStatus.AVAILABLE)
                .min(hasCoords
                        ? Comparator.comparingDouble(d -> haversineKm(d.lat, d.lng, pickupLat, pickupLng))
                        : Comparator.comparingDouble(d -> 0.0)) // no coords → any driver
                .orElse(null);
    }

    // ── Haversine distance ────────────────────────────────────────────────────

    /**
     * Calculates the great-circle distance between two GPS coordinates in km.
     * Used to rank drivers by proximity to the pickup point.
     *
     * Formula: https://en.wikipedia.org/wiki/Haversine_formula
     */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
