package com.ethioride.server.db;

import com.ethioride.shared.dto.UserDTO;
import com.ethioride.shared.enums.UserRole;

import java.sql.*;

/**
 * CRUD operations for the users table.
 *
 * Follows the course JDBC pattern:
 *   Step 3 – getConnection()
 *   Step 4 – createStatement() / prepareStatement()
 *   Step 5 – executeQuery() / executeUpdate() + process ResultSet
 *   Step 6 – close ResultSet and Statement
 *   Step 7 – close Connection
 */
public class UserRepository {

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * SELECT — find a user by phone and password.
     * Returns UserDTO if found, null otherwise.
     */
    public UserDTO findByPhoneAndPassword(String phone, String password) throws Exception {
        String sql = "SELECT id, full_name, phone, email, role, rating " +
                     "FROM users WHERE phone = ? AND password_hash = ?";

        // Try-with-resources: Connection, PreparedStatement, and ResultSet all auto-closed
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            stmt.setString(2, hashPassword(password)); // hash on server side

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserDTO user = new UserDTO();
                    user.setId(rs.getString("id"));
                    user.setFullName(rs.getString("full_name"));
                    user.setPhone(rs.getString("phone"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(UserRole.valueOf(rs.getString("role")));
                    user.setRating(rs.getDouble("rating"));
                    return user;
                }
            }
        }
        return null;
    }

    /** SELECT — check if a phone number already exists. */
    public boolean phoneExists(String phone) throws Exception {
        String sql = "SELECT COUNT(*) FROM users WHERE phone = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * INSERT — save a new user to the database.
     * Returns the generated UUID.
     */
    public String save(UserDTO user, String password) throws Exception {
        String id  = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO users (id, full_name, phone, email, role, password_hash, rating) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, hashPassword(password)); // hash on server side
            stmt.setDouble(7, 5.0);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("[DB] User inserted, rows affected: " + rowsAffected);
        }
        return id;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /** UPDATE — update a user's rating. */
    public void updateRating(String userId, double rating) throws Exception {
        String sql = "UPDATE users SET rating = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, rating);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /** DELETE — remove a user by ID. */
    public void delete(String userId) throws Exception {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** SHA-256 password hash. */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash failed", e);
        }
    }
}
