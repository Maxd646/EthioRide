package com.ethioride.passenger.state;

import com.ethioride.shared.dto.UserDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the current logged-in user session (singleton).
 */
public class SessionState {
    private static volatile SessionState instance;
    private volatile UserDTO currentUser;
    private volatile String authToken;

    /** A named saved location (e.g. Home, Work, custom). Max 6 per session. */
    public static class SavedLocation {
        private String name;
        private String address;
        public SavedLocation(String name, String address) {
            this.name = name;
            this.address = address;
        }
        public String getName()    { return name; }
        public String getAddress() { return address; }
        public void setName(String n)    { this.name = n; }
        public void setAddress(String a) { this.address = a; }
    }

    private final List<SavedLocation> savedLocations = new ArrayList<>();
    public static final int MAX_SAVED_LOCATIONS = 6;

    private SessionState() {}

    public static SessionState getInstance() {
        if (instance == null) {
            synchronized (SessionState.class) {
                if (instance == null) instance = new SessionState();
            }
        }
        return instance;
    }

    public UserDTO getCurrentUser() { return currentUser; }
    public void setCurrentUser(UserDTO user) { this.currentUser = user; }
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String token) { this.authToken = token; }
    public boolean isLoggedIn() { return currentUser != null; }

    // ── Saved locations ───────────────────────────────────────────────────────

    /** Returns an unmodifiable view of saved locations. */
    public List<SavedLocation> getSavedLocations() {
        return Collections.unmodifiableList(savedLocations);
    }

    /**
     * Adds a saved location. Returns false if the limit (6) is already reached
     * or if a location with the same name already exists.
     */
    public boolean addSavedLocation(String name, String address) {
        if (savedLocations.size() >= MAX_SAVED_LOCATIONS) return false;
        if (name == null || name.isBlank() || address == null || address.isBlank()) return false;
        // Prevent duplicate names
        boolean exists = savedLocations.stream()
            .anyMatch(l -> l.getName().equalsIgnoreCase(name.trim()));
        if (exists) return false;
        savedLocations.add(new SavedLocation(name.trim(), address.trim()));
        return true;
    }

    /** Removes a saved location by index. */
    public void removeSavedLocation(int index) {
        if (index >= 0 && index < savedLocations.size()) {
            savedLocations.remove(index);
        }
    }

    public void clear() {
        currentUser = null;
        authToken = null;
        savedLocations.clear();
    }
}
