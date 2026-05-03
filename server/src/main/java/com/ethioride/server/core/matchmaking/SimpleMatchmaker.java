package com.ethioride.server.core.matchmaking;

import com.ethioride.server.db.TripRepository;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.TripStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runs on a background daemon thread and periodically matches PENDING trips
 * to available (online) drivers.
 *
 * Thread-safety: a ReentrantLock guards the onlineDrivers map during the
 * match cycle so no two threads can assign the same driver simultaneously.
 */
public class SimpleMatchmaker {

    // driverId → true (online and available)
    private final Map<String, Boolean> onlineDrivers = new ConcurrentHashMap<>();

    // Guards the match cycle — prevents double-assignment under concurrent access
    private final ReentrantLock matchLock = new ReentrantLock();

    private final TripRepository tripRepo = new TripRepository();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "matchmaker-thread");
                t.setDaemon(true); // won't block JVM shutdown
                return t;
            });

    private static SimpleMatchmaker instance;

    private SimpleMatchmaker() {}

    public static SimpleMatchmaker getInstance() {
        if (instance == null) instance = new SimpleMatchmaker();
        return instance;
    }

    /** Start the matchmaking loop — runs every 5 seconds. */
    public void start() {
        scheduler.scheduleAtFixedRate(this::runMatchCycle, 0, 5, TimeUnit.SECONDS);
        System.out.println("[Matchmaker] Started — polling every 5s.");
    }

    public void stop() {
        scheduler.shutdown();
    }

    // ── Driver registry ───────────────────────────────────────────────────────

    /** Called when a driver goes online (DRIVER_STATUS_UPDATE received). */
    public void setDriverOnline(String driverId) {
        onlineDrivers.put(driverId, true);
        System.out.println("[Matchmaker] Driver online: " + driverId);
    }

    /** Called when a driver goes offline or accepts a trip. */
    public void setDriverOffline(String driverId) {
        onlineDrivers.remove(driverId);
    }

    // ── Match cycle ───────────────────────────────────────────────────────────

    /**
     * Core matching logic:
     *  1. Fetch all PENDING trips from DB
     *  2. For each trip, find a free online driver
     *  3. Assign driver → update DB → remove driver from pool
     *
     * The ReentrantLock ensures this entire cycle is atomic — if two threads
     * somehow trigger simultaneously, the second waits rather than double-assigning.
     */
    private void runMatchCycle() {
        matchLock.lock();
        try {
            List<TripRequestDTO> pending = tripRepo.findPending();
            if (pending.isEmpty()) return;

            System.out.printf("[Matchmaker] %d pending trip(s) found.%n", pending.size());

            for (TripRequestDTO trip : pending) {
                String driverId = findAvailableDriver();
                if (driverId == null) {
                    System.out.println("[Matchmaker] No drivers available — waiting.");
                    break; // no point checking more trips
                }

                // Assign in DB and mark driver busy
                tripRepo.assignDriver(trip.getTripId(), driverId);
                setDriverOffline(driverId); // remove from pool until trip ends

                System.out.printf("[Matchmaker] Matched trip %s → driver %s%n",
                        trip.getTripId(), driverId);

                // TODO: notify the driver client via their socket (requires ClientHandler registry)
            }
        } catch (Exception e) {
            System.err.println("[Matchmaker] Error during match cycle: " + e.getMessage());
        } finally {
            matchLock.unlock();
        }
    }

    /** Returns the first available driver ID, or null if none are online. */
    private String findAvailableDriver() {
        return onlineDrivers.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
