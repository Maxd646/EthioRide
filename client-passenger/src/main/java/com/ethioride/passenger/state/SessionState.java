package com.ethioride.passenger.state;

import com.ethioride.shared.dto.UserDTO;

/**
 * Holds the current logged-in user session (singleton).
 */
public class SessionState {
    private static volatile SessionState instance;
    private volatile UserDTO currentUser;
    private volatile String authToken;

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

    public void clear() {
        currentUser = null;
        authToken = null;
    }
}
