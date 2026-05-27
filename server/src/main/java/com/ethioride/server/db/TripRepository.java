package com.ethioride.server.db;

import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.RideCategory;
import com.ethioride.shared.enums.TripStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD operations for the trips table.
 * Follows the course JDBC steps: connect → statement → execute → process → close.
 */
public class TripRepository {

    // ── CREATE ────────────────────────────────────────────────────────────────

    /** INSERT a new trip request into the database. */
    public void save(TripRequestDTO trip) throws Exception {
        // Step 3: Connect
        Connection conn = DBConnection.getConnection();

        // Step 4: PreparedStatement — INSERT
        String sql = "INSERT INTO trips (id, passenger_id, driver_id, pickup_location, " +
                     "dropoff_location, category, fare, distance_km, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);

        // Step 5: Set parameters
        stmt.setString(1, trip.getTripId());
        stmt.setString(2, trip.getPassengerId());
        stmt.setString(3, trip.getDriverId());   // may be null initially
        stmt.setString(4, trip.getPickupLocation());
        stmt.setString(5, trip.getDropoffLocation());
        stmt.setString(6, trip.getCategory() != null
                          ? trip.getCategory().name() : RideCategory.ECONOMY.name());
        stmt.setDouble(7, trip.getFare());
        stmt.setDouble(8, trip.getDistanceKm());
        stmt.setString(9, TripStatus.PENDING.name());

        int rows = stmt.executeUpdate();
        System.out.println("[DB] Trip saved, rows affected: " + rows);

        // Step 6 & 7: Close
        stmt.close();
        conn.close();
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * SELECT all trips with passenger and driver names joined.
     * Used by admin TripsScreen.
     */
    public List<TripRequestDTO> findAll() throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "SELECT t.id, t.passenger_id, t.driver_id, t.pickup_location, " +
                     "t.dropoff_location, t.category, t.fare, t.distance_km, t.status, " +
                     "t.created_at, " +
                     "p.full_name AS passenger_name, " +
                     "d.full_name AS driver_name " +
                     "FROM trips t " +
                     "LEFT JOIN users p ON t.passenger_id = p.id " +
                     "LEFT JOIN users d ON t.driver_id = d.id " +
                     "ORDER BY t.created_at DESC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        List<TripRequestDTO> list = new ArrayList<>();
        while (rs.next()) {
            TripRequestDTO t = mapRow(rs);
            // Store names in passengerPhone field temporarily (reused for display)
            t.setPassengerPhone(rs.getString("passenger_name"));
            // Store driver name via driverId field prefix trick — use a DTO extension instead
            // We'll pass driver name as part of the DTO by overloading passengerPhone
            // Format: "passengerName|driverName"
            String driverName = rs.getString("driver_name");
            t.setPassengerPhone(
                (rs.getString("passenger_name") != null ? rs.getString("passenger_name") : "Unknown")
                + "|" +
                (driverName != null ? driverName : "—")
            );
            list.add(t);
        }

        rs.close();
        stmt.close();
        conn.close();
        return list;
    }

    /**
     * SELECT trips for a specific driver.
     * Used by driver RideHistoryScreen.
     */
    public List<TripRequestDTO> findByDriver(String driverId) throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "SELECT t.*, p.full_name AS passenger_name " +
                     "FROM trips t " +
                     "LEFT JOIN users p ON t.passenger_id = p.id " +
                     "WHERE t.driver_id = ? ORDER BY t.created_at DESC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, driverId);

        ResultSet rs = stmt.executeQuery();
        List<TripRequestDTO> list = new ArrayList<>();
        while (rs.next()) {
            TripRequestDTO t = mapRow(rs);
            t.setPassengerPhone(rs.getString("passenger_name"));
            list.add(t);
        }

        rs.close();
        stmt.close();
        conn.close();
        return list;
    }

    /**
     * SELECT trips for a specific passenger.
     * Uses ORDER BY created_at DESC — newest first.
     */
    public List<TripRequestDTO> findByPassenger(String passengerId) throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "SELECT * FROM trips WHERE passenger_id = ? ORDER BY created_at DESC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, passengerId);

        ResultSet rs = stmt.executeQuery();
        List<TripRequestDTO> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }

        rs.close();
        stmt.close();
        conn.close();
        return list;
    }

    /**
     * SELECT all PENDING trips ordered by creation time (oldest first).
     * Used by SimpleMatchmaker to find trips waiting for a driver.
     */
    public List<TripRequestDTO> findPending() throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "SELECT * FROM trips WHERE status = 'PENDING' ORDER BY created_at ASC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        List<TripRequestDTO> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }

        rs.close();
        stmt.close();
        conn.close();
        return list;
    }

    /** SELECT COUNT — how many trips have a given status. */
    public int countByStatus(TripStatus status) throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "SELECT COUNT(*) FROM trips WHERE status = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, status.name());

        ResultSet rs = stmt.executeQuery();
        int count = rs.next() ? rs.getInt(1) : 0;

        rs.close();
        stmt.close();
        conn.close();
        return count;
    }

    /**
     * SELECT aggregate stats for admin dashboard.
     * Returns: [totalTrips, completedTrips, cancelledTrips, totalRevenue]
     */
    public double[] getStats() throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "SELECT " +
                     "COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
                     "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled, " +
                     "SUM(CASE WHEN status = 'COMPLETED' THEN fare ELSE 0 END) AS revenue " +
                     "FROM trips";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        double[] stats = {0, 0, 0, 0};
        if (rs.next()) {
            stats[0] = rs.getDouble("total");
            stats[1] = rs.getDouble("completed");
            stats[2] = rs.getDouble("cancelled");
            stats[3] = rs.getDouble("revenue");
        }

        rs.close();
        stmt.close();
        conn.close();
        return stats;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private TripRequestDTO mapRow(ResultSet rs) throws SQLException {
        TripRequestDTO t = new TripRequestDTO();
        t.setTripId(rs.getString("id"));
        t.setPassengerId(rs.getString("passenger_id"));
        t.setDriverId(rs.getString("driver_id"));
        t.setPickupLocation(rs.getString("pickup_location"));
        t.setDropoffLocation(rs.getString("dropoff_location"));
        t.setFare(rs.getDouble("fare"));
        t.setDistanceKm(rs.getDouble("distance_km"));
        String cat = rs.getString("category");
        if (cat != null) t.setCategory(RideCategory.valueOf(cat));
        String status = rs.getString("status");
        if (status != null) t.setStatus(TripStatus.valueOf(status));
        return t;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /** UPDATE trip status (e.g. PENDING → ACCEPTED → COMPLETED). */
    public void updateStatus(String tripId, TripStatus status) throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "UPDATE trips SET status = ? WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, status.name());
        stmt.setString(2, tripId);

        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /** UPDATE — assign a driver to a trip. */
    public void assignDriver(String tripId, String driverId) throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "UPDATE trips SET driver_id = ?, status = ? WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, driverId);
        stmt.setString(2, TripStatus.ACCEPTED.name());
        stmt.setString(3, tripId);

        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /** DELETE — cancel a trip by ID. */
    public void delete(String tripId) throws Exception {
        Connection conn = DBConnection.getConnection();

        String sql = "DELETE FROM trips WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, tripId);

        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }
}
