package com.ethioride.server.service;

import com.ethioride.server.db.DBConnection;
import com.ethioride.shared.enums.RideCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Service for calculating trip prices based on distance, duration, and category.
 * Uses RoutingService (Nominatim + OSRM) for free distance/duration data.
 */
public class PricingService {
    
    private final RoutingService routingService;
    
    public PricingService() {
        this.routingService = new RoutingService();
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
    public PriceEstimate calculatePrice(String origin, String destination, RideCategory category) throws Exception {
        System.out.println("[Pricing] Calculating price for " + category + " trip (OSM/OSRM)");
        
        // Get distance and duration from OpenStreetMap + OSRM (free, no API key)
        RoutingService.DistanceResult distance = routingService.calculateDistance(origin, destination);
        
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
            category,
            distance.getOriginLat(), distance.getOriginLng(),
            distance.getDestLat(),   distance.getDestLng()
        );
    }
    
    /**
     * Get pricing rules for a specific category from database.
     */
    private PricingRules getPricingRules(RideCategory category) throws Exception {
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
        private final RideCategory category;
        private final double originLat;
        private final double originLng;
        private final double destLat;
        private final double destLng;

        public PriceEstimate(double distanceKm, double durationMinutes, double baseFare,
                           double distanceFare, double timeFare, double bookingFee,
                           double totalFare, RideCategory category,
                           double originLat, double originLng,
                           double destLat,   double destLng) {
            this.distanceKm    = distanceKm;
            this.durationMinutes = durationMinutes;
            this.baseFare      = baseFare;
            this.distanceFare  = distanceFare;
            this.timeFare      = timeFare;
            this.bookingFee    = bookingFee;
            this.totalFare     = totalFare;
            this.category      = category;
            this.originLat     = originLat;
            this.originLng     = originLng;
            this.destLat       = destLat;
            this.destLng       = destLng;
        }
        
        public double getDistanceKm()      { return distanceKm; }
        public double getDurationMinutes() { return durationMinutes; }
        public double getBaseFare()        { return baseFare; }
        public double getDistanceFare()    { return distanceFare; }
        public double getTimeFare()        { return timeFare; }
        public double getBookingFee()      { return bookingFee; }
        public double getTotalFare()       { return totalFare; }
        public RideCategory getCategory()  { return category; }
        public double getOriginLat()       { return originLat; }
        public double getOriginLng()       { return originLng; }
        public double getDestLat()         { return destLat; }
        public double getDestLng()         { return destLng; }
        
        @Override
        public String toString() {
            return String.format(
                "Distance: %.2f km, Duration: %.0f min, Total: %.2f ETB (%s)",
                distanceKm, durationMinutes, totalFare, category
            );
        }
    }
}
