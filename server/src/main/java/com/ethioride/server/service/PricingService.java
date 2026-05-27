package com.ethioride.server.service;

import com.ethioride.server.db.DBConnection;
import com.ethioride.shared.enums.TripCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Service for calculating trip prices based on distance, duration, and category.
 */
public class PricingService {
    
    private final GoogleMapsService mapsService;
    
    public PricingService() {
        this.mapsService = new GoogleMapsService();
    }
    
    /**
     * Calculate trip price based on origin, destination, and category.
     * 
     * @param origin Starting location
     * @param destination Ending location
     * @param category Trip category (ECONOMY, PREMIUM, ELITE)
     * @return PriceEstimate with breakdown
     * @throws Exception if calculation fails
     */
    public PriceEstimate calculatePrice(String origin, String destination, TripCategory category) throws Exception {
        System.out.println("[Pricing] Calculating price for " + category + " trip");
        
        // Get distance and duration from Google Maps
        GoogleMapsService.DistanceResult distance = mapsService.calculateDistance(origin, destination);
        
        // Get pricing rules from database
        PricingRules rules = getPricingRules(category);
        
        // Calculate price components
        double baseFare = rules.baseFare;
        double distanceFare = distance.getDistanceKm() * rules.perKmRate;
        double timeFare = distance.getDurationMinutes() * rules.perMinuteRate;
        double bookingFee = rules.bookingFee;
        
        double subtotal = baseFare + distanceFare + timeFare;
        double total = Math.max(subtotal + bookingFee, rules.minimumFare);
        
        System.out.println("[Pricing] Base: " + baseFare + " ETB");
        System.out.println("[Pricing] Distance: " + String.format("%.2f", distanceFare) + " ETB");
        System.out.println("[Pricing] Time: " + String.format("%.2f", timeFare) + " ETB");
        System.out.println("[Pricing] Booking Fee: " + bookingFee + " ETB");
        System.out.println("[Pricing] Total: " + String.format("%.2f", total) + " ETB");
        
        return new PriceEstimate(
            distance.getDistanceKm(),
            distance.getDurationMinutes(),
            baseFare,
            distanceFare,
            timeFare,
            bookingFee,
            total,
            category
        );
    }
    
    /**
     * Get pricing rules for a specific category from database.
     */
    private PricingRules getPricingRules(TripCategory category) throws Exception {
        Connection conn = DBConnection.getConnection();
        
        String sql = "SELECT base_fare, per_km_rate, per_minute_rate, minimum_fare, booking_fee " +
                     "FROM pricing_rules WHERE category = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, category.name());
        
        ResultSet rs = stmt.executeQuery();
        
        if (!rs.next()) {
            // Default fallback values if not in database
            rs.close();
            stmt.close();
            conn.close();
            return new PricingRules(50.0, 15.0, 2.0, 30.0, 10.0);
        }
        
        PricingRules rules = new PricingRules(
            rs.getDouble("base_fare"),
            rs.getDouble("per_km_rate"),
            rs.getDouble("per_minute_rate"),
            rs.getDouble("minimum_fare"),
            rs.getDouble("booking_fee")
        );
        
        rs.close();
        stmt.close();
        conn.close();
        
        return rules;
    }
    
    /**
     * Pricing rules from database.
     */
    private static class PricingRules {
        final double baseFare;
        final double perKmRate;
        final double perMinuteRate;
        final double minimumFare;
        final double bookingFee;
        
        PricingRules(double baseFare, double perKmRate, double perMinuteRate, 
                     double minimumFare, double bookingFee) {
            this.baseFare = baseFare;
            this.perKmRate = perKmRate;
            this.perMinuteRate = perMinuteRate;
            this.minimumFare = minimumFare;
            this.bookingFee = bookingFee;
        }
    }
    
    /**
     * Price estimate result with breakdown.
     */
    public static class PriceEstimate {
        private final double distanceKm;
        private final double durationMinutes;
        private final double baseFare;
        private final double distanceFare;
        private final double timeFare;
        private final double bookingFee;
        private final double totalFare;
        private final TripCategory category;
        
        public PriceEstimate(double distanceKm, double durationMinutes, double baseFare,
                           double distanceFare, double timeFare, double bookingFee,
                           double totalFare, TripCategory category) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.baseFare = baseFare;
            this.distanceFare = distanceFare;
            this.timeFare = timeFare;
            this.bookingFee = bookingFee;
            this.totalFare = totalFare;
            this.category = category;
        }
        
        public double getDistanceKm() { return distanceKm; }
        public double getDurationMinutes() { return durationMinutes; }
        public double getBaseFare() { return baseFare; }
        public double getDistanceFare() { return distanceFare; }
        public double getTimeFare() { return timeFare; }
        public double getBookingFee() { return bookingFee; }
        public double getTotalFare() { return totalFare; }
        public TripCategory getCategory() { return category; }
        
        @Override
        public String toString() {
            return String.format(
                "Distance: %.2f km, Duration: %.0f min, Total: %.2f ETB (%s)",
                distanceKm, durationMinutes, totalFare, category
            );
        }
    }
}
