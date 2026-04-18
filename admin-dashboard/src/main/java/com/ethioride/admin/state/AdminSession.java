package com.ethioride.admin.state;

/**
 * Holds the current admin session (singleton).
 */
public class AdminSession {
    private static AdminSession instance;

    private String username;
    private String token;
    private boolean authenticated;

    private AdminSession() {}

    public static AdminSession getInstance() {
        if (instance == null) instance = new AdminSession();
        return instance;
    }

    public void login(String username, String token) {
        this.username = username;
        this.token = token;
        this.authenticated = true;
    }

    public void logout() {
        this.username = null;
        this.token = null;
        this.authenticated = false;
    }

    public boolean isAuthenticated() { return authenticated; }
    public String getUsername() { return username; }
    public String getToken() { return token; }
}
