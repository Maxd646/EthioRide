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

        // Step 5: Process ResultSet — iterate rows
        while (rs.next()) {
            TripRequestDTO t = new TripRequestDTO();
            t.setTripId(rs.getString("id"));
            t.setPassengerId(rs.getString("passenger_id"));
            t.setDriverId(rs.getString("driver_id"));
            t.setPickupLocation(rs.getString("pickup_location"));
            t.setDropoffLocation(rs.getString("dropoff_location"));
            t.setFare(rs.getDouble("fare"));
            t.setDistanceKm(rs.getDouble("distance_km"));
            t.setStatus(TripStatus.valueOf(rs.getString("status")));
            list.add(t);
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
