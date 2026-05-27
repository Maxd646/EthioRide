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
 * Free routing service using OpenStreetMap + OSRM.
 *
 * Replaces GoogleMapsService entirely — no API key, no billing.
 *
 * Pipeline:
 *   Step 1 — Geocode origin address via Nominatim → lat/lon
 *   Step 2 — Geocode destination address via Nominatim → lat/lon
 *   Step 3 — Request driving route from OSRM → distance (m) + duration (s)
 *   Step 4 — Convert and return DistanceResult (km, minutes)
 *
 * The returned DistanceResult is identical in structure to the old
 * GoogleMapsService.DistanceResult so PricingService needs zero changes.
 *
 * Free service limits (no account needed):
 *   Nominatim: 1 request/second, must include a User-Agent header.
 *   OSRM demo:  public demo server, no rate limit enforced but be reasonable.
 *               For production, self-host: https://github.com/Project-OSRM/osrm-backend
 */
public class RoutingService {

    // ── Nominatim geocoding ───────────────────────────────────────────────────
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search";

    // ── OSRM routing ──────────────────────────────────────────────────────────
    private static final String OSRM_URL =
            "https://router.project-osrm.org/route/v1/driving";

    // Nominatim requires a descriptive User-Agent (their policy)
    private static final String USER_AGENT = "EthioRide/1.0 (ride-hailing app; contact@ethioride.com)";

    // HTTP timeouts in milliseconds
    private static final int CONNECT_TIMEOUT = 8_000;
    private static final int READ_TIMEOUT    = 8_000;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Calculates the driving distance and estimated duration between two
     * text addresses using Nominatim geocoding + OSRM routing.
     *
     * @param origin      pickup address (e.g. "Meskel Square, Addis Ababa")
     * @param destination dropoff address (e.g. "Bole Airport, Addis Ababa")
     * @return DistanceResult with distanceKm and durationMinutes
     * @throws Exception if geocoding or routing fails
     */
    public DistanceResult calculateDistance(String origin, String destination) throws Exception {
        System.out.printf("[Routing] Calculating route: %s → %s%n", origin, destination);

        // Step 1 & 2 — geocode both addresses (can run sequentially; Nominatim is 1 req/s)
        double[] originCoords      = geocode(origin);
        double[] destinationCoords = geocode(destination);

        // Step 3 — get driving route from OSRM
        // OSRM format: /route/v1/driving/{lng},{lat};{lng},{lat}
        String osrmUrl = String.format("%s/%.6f,%.6f;%.6f,%.6f?overview=false",
                OSRM_URL,
                originCoords[1],      originCoords[0],      // lng,lat of origin
                destinationCoords[1], destinationCoords[0]); // lng,lat of destination

        String osrmResponse = httpGet(osrmUrl, false);
        JSONObject osrmJson = new JSONObject(osrmResponse);

        String osrmCode = osrmJson.getString("code");
        if (!"Ok".equals(osrmCode)) {
            throw new Exception("OSRM routing failed: " + osrmCode);
        }

        JSONArray routes = osrmJson.getJSONArray("routes");
        if (routes.length() == 0) {
            throw new Exception("OSRM returned no routes");
        }

        JSONObject route = routes.getJSONObject(0);

        // Step 4 — extract and convert (same math as the old Google service)
        double distanceMeters  = route.getDouble("distance"); // metres
        double durationSeconds = route.getDouble("duration"); // seconds

        double distanceKm      = distanceMeters  / 1000.0;
        double durationMinutes = durationSeconds / 60.0;

        System.out.printf("[Routing] Distance: %.2f km%n",  distanceKm);
        System.out.printf("[Routing] Duration: %.0f min%n", durationMinutes);

        return new DistanceResult(distanceKm, durationMinutes,
                originCoords[0],      originCoords[1],      // origin lat, lng
                destinationCoords[0], destinationCoords[1]); // dest lat, lng
    }

    // ── Geocoding ─────────────────────────────────────────────────────────────

    /**
     * Converts a text address to [latitude, longitude] using Nominatim.
     *
     * @param address human-readable address
     * @return double[]{lat, lng}
     * @throws Exception if address cannot be resolved
     */
    private double[] geocode(String address) throws Exception {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = String.format("%s?q=%s&format=json&limit=1", NOMINATIM_URL, encodedAddress);

        String responseBody = httpGet(url, true); // true = send User-Agent (required by Nominatim)
        JSONArray results = new JSONArray(responseBody);

        if (results.length() == 0) {
            throw new Exception("Address not found: \"" + address + "\"");
        }

        JSONObject place = results.getJSONObject(0);
        double lat = place.getDouble("lat");
        double lon = place.getDouble("lon");

        System.out.printf("[Routing] Geocoded \"%s\" → %.6f, %.6f%n", address, lat, lon);
        return new double[]{lat, lon};
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    /**
     * Performs a GET request and returns the response body as a String.
     *
     * @param urlString  the full URL to request
     * @param userAgent  whether to send the User-Agent header (required for Nominatim)
     */
    private String httpGet(String urlString, boolean userAgent) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        if (userAgent) {
            // Nominatim policy: must identify your application
            conn.setRequestProperty("User-Agent", USER_AGENT);
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new Exception("HTTP " + status + " from: " + urlString);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * Routing result — distance, duration, and the geocoded coordinates
     * for both origin and destination. Coordinates are passed back to the
     * client so they can be stored on the trip record for proximity matching.
     */
    public static class DistanceResult {
        private final double distanceKm;
        private final double durationMinutes;
        private final double originLat;
        private final double originLng;
        private final double destLat;
        private final double destLng;

        public DistanceResult(double distanceKm, double durationMinutes,
                              double originLat, double originLng,
                              double destLat,   double destLng) {
            this.distanceKm      = distanceKm;
            this.durationMinutes = durationMinutes;
            this.originLat       = originLat;
            this.originLng       = originLng;
            this.destLat         = destLat;
            this.destLng         = destLng;
        }

        public double getDistanceKm()      { return distanceKm;      }
        public double getDurationMinutes() { return durationMinutes; }
        public double getOriginLat()       { return originLat;       }
        public double getOriginLng()       { return originLng;       }
        public double getDestLat()         { return destLat;         }
        public double getDestLng()         { return destLng;         }
    }
}
