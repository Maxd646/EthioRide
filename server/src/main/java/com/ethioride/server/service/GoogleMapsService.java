package com.ethioride.server.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service for interacting with Google Maps Distance Matrix API
 * to calculate distance and duration between two locations.
 */
public class GoogleMapsService {
    
    private static final String API_KEY = "YOUR_GOOGLE_MAPS_API_KEY"; // TODO: Replace with actual key
    private static final String DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
    
    /**
     * Calculate distance and duration between origin and destination.
     * 
     * @param origin Starting location (address or "lat,lng")
     * @param destination Ending location (address or "lat,lng")
     * @return DistanceResult with distance in km and duration in minutes
     * @throws Exception if API call fails
     */
    public DistanceResult calculateDistance(String origin, String destination) throws Exception {
        // Build API URL
        String urlString = String.format(
            "%s?origins=%s&destinations=%s&key=%s&mode=driving",
            DISTANCE_MATRIX_URL,
            URLEncoder.encode(origin, StandardCharsets.UTF_8),
            URLEncoder.encode(destination, StandardCharsets.UTF_8),
            API_KEY
        );
        
        System.out.println("[GoogleMaps] Calculating distance: " + origin + " → " + destination);
        
        // Make HTTP request
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Google Maps API returned status: " + responseCode);
        }
        
        // Read response
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        // Parse JSON response
        JSONObject json = new JSONObject(response.toString());
        
        String status = json.getString("status");
        if (!"OK".equals(status)) {
            throw new Exception("Google Maps API error: " + status);
        }
        
        JSONArray rows = json.getJSONArray("rows");
        if (rows.length() == 0) {
            throw new Exception("No route found");
        }
        
        JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
        if (elements.length() == 0) {
            throw new Exception("No route found");
        }
        
        JSONObject element = elements.getJSONObject(0);
        String elementStatus = element.getString("status");
        
        if (!"OK".equals(elementStatus)) {
            throw new Exception("Route calculation failed: " + elementStatus);
        }
        
        // Extract distance and duration
        JSONObject distance = element.getJSONObject("distance");
        JSONObject duration = element.getJSONObject("duration");
        
        double distanceMeters = distance.getDouble("value");
        double durationSeconds = duration.getDouble("value");
        
        double distanceKm = distanceMeters / 1000.0;
        double durationMinutes = durationSeconds / 60.0;
        
        System.out.println("[GoogleMaps] Distance: " + String.format("%.2f", distanceKm) + " km");
        System.out.println("[GoogleMaps] Duration: " + String.format("%.0f", durationMinutes) + " minutes");
        
        return new DistanceResult(distanceKm, durationMinutes);
    }
    
    /**
     * Result object containing distance and duration.
     */
    public static class DistanceResult {
        private final double distanceKm;
        private final double durationMinutes;
        
        public DistanceResult(double distanceKm, double durationMinutes) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
        }
        
        public double getDistanceKm() {
            return distanceKm;
        }
        
        public double getDurationMinutes() {
            return durationMinutes;
        }
    }
}
