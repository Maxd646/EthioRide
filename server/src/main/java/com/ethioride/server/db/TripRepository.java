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
        String sql = "INSERT INTO trips (id, passenger_id, driver_id, pickup_location, " +
                     "dropoff_location, category, fare, distance_km, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trip.getTripId());
            stmt.setString(2, trip.getPassengerId());
            stmt.setString(3, trip.getDriverId());
            stmt.setString(4, trip.getPickupLocation());
            stmt.setString(5, trip.getDropoffLocation());
            stmt.setString(6, trip.getCategory() != null
                              ? trip.getCategory().name() : RideCategory.ECONOMY.name());
            stmt.setDouble(7, trip.getFare());
            stmt.setDouble(8, trip.getDistanceKm());
            stmt.setString(9, TripStatus.PENDING.name());
            int rows = stmt.executeUpdate();
            System.out.println("[DB] Trip saved, rows affected: " + rows);
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * SELECT trips for a specific passenger.
     * Uses ORDER BY created_at DESC — newest first.
     */
    /** SELECT all trips ordered by newest first — for admin view. */
    public List<TripRequestDTO> findAll() throws Exception {
        String sql = "SELECT * FROM trips ORDER BY created_at DESC LIMIT 100";
        List<TripRequestDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
        }
        return list;
    }

    public List<TripRequestDTO> findByPassenger(String passengerId) throws Exception {
        String sql = "SELECT * FROM trips WHERE passenger_id = ? ORDER BY created_at DESC";
        List<TripRequestDTO> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passengerId);
            try (ResultSet rs = stmt.executeQuery()) {
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
            }
        }
        return list;
    }

    /** SELECT COUNT — how many trips have a given status. */
    public int countByStatus(TripStatus status) throws Exception {
        String sql = "SELECT COUNT(*) FROM trips WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** SELECT all PENDING trips — used by the matchmaker. */
    public List<TripRequestDTO> findPending() throws Exception {
        String sql = "SELECT * FROM trips WHERE status = 'PENDING' ORDER BY created_at ASC";
        List<TripRequestDTO> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                TripRequestDTO t = new TripRequestDTO();
                t.setTripId(rs.getString("id"));
                t.setPassengerId(rs.getString("passenger_id"));
                t.setPickupLocation(rs.getString("pickup_location"));
                t.setDropoffLocation(rs.getString("dropoff_location"));
                t.setFare(rs.getDouble("fare"));
                t.setDistanceKm(rs.getDouble("distance_km"));
                t.setStatus(TripStatus.PENDING);
                list.add(t);
            }
        }
        return list;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /** UPDATE trip status (e.g. PENDING → ACCEPTED → COMPLETED). */
    public void updateStatus(String tripId, TripStatus status) throws Exception {
        String sql = "UPDATE trips SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, tripId);
            stmt.executeUpdate();
        }
    }

    /** UPDATE — assign a driver to a trip. */
    public void assignDriver(String tripId, String driverId) throws Exception {
        String sql = "UPDATE trips SET driver_id = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, driverId);
            stmt.setString(2, TripStatus.ACCEPTED.name());
            stmt.setString(3, tripId);
            stmt.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /** DELETE — cancel a trip by ID. */
    public void delete(String tripId) throws Exception {
        String sql = "DELETE FROM trips WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tripId);
            stmt.executeUpdate();
        }
    }
}
