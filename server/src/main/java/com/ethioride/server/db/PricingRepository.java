package com.ethioride.server.db;

import java.sql.*;

/**
 * CRUD operations for the pricing_rules table.
 */
public class PricingRepository {

    /**
     * Returns all pricing rules as a pipe-delimited string.
     * Format: "ECONOMY:base:perKm:perMin:min:booking|PREMIUM:...|ELITE:..."
     */
    public String getAllRules() throws Exception {
        Connection conn = DBConnection.getConnection();
        String sql = "SELECT category, base_fare, per_km_rate, per_minute_rate, minimum_fare, booking_fee " +
                     "FROM pricing_rules ORDER BY FIELD(category,'ECONOMY','PREMIUM','ELITE')";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            if (sb.length() > 0) sb.append("|");
            sb.append(String.format("%s:%.2f:%.2f:%.2f:%.2f:%.2f",
                rs.getString("category"),
                rs.getDouble("base_fare"),
                rs.getDouble("per_km_rate"),
                rs.getDouble("per_minute_rate"),
                rs.getDouble("minimum_fare"),
                rs.getDouble("booking_fee")));
        }

        rs.close(); stmt.close(); conn.close();
        return sb.toString();
    }

    /**
     * Updates a single category's pricing rule.
     */
    public void updateRule(String category, double baseFare, double perKm,
                           double perMin, double minFare, double bookingFee) throws Exception {
        Connection conn = DBConnection.getConnection();
        String sql = "UPDATE pricing_rules SET base_fare=?, per_km_rate=?, per_minute_rate=?, " +
                     "minimum_fare=?, booking_fee=? WHERE category=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDouble(1, baseFare);
        stmt.setDouble(2, perKm);
        stmt.setDouble(3, perMin);
        stmt.setDouble(4, minFare);
        stmt.setDouble(5, bookingFee);
        stmt.setString(6, category);
        stmt.executeUpdate();
        stmt.close(); conn.close();
        System.out.printf("[DB] Pricing updated: %s base=%.2f perKm=%.2f%n", category, baseFare, perKm);
    }
}
