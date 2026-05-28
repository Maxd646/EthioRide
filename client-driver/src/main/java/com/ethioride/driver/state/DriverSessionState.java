package com.ethioride.driver.state;

import com.ethioride.shared.dto.UserDTO;

public class DriverSessionState {
    private static DriverSessionState instance;
    private volatile UserDTO currentDriver;
    private volatile boolean online;
    private double shiftEarnings;
    private volatile double locationLat = 9.0320;  // default: Addis Ababa
    private volatile double locationLng = 38.7469;
    private volatile String locationName = null;    // null = not set by driver yet

    private DriverSessionState() {}

    public static synchronized DriverSessionState getInstance() {
        if (instance == null) instance = new DriverSessionState();
        return instance;
    }

    public synchronized UserDTO getCurrentDriver() { return currentDriver; }
    public synchronized void setCurrentDriver(UserDTO driver) { this.currentDriver = driver; }
    public synchronized boolean isOnline() { return online; }
    public synchronized void setOnline(boolean online) { this.online = online; }
    public synchronized double getShiftEarnings() { return shiftEarnings; }
    public synchronized void addEarnings(double amount) { this.shiftEarnings += amount; }
    public synchronized boolean isLoggedIn() { return currentDriver != null; }

    public double getLocationLat() { return locationLat; }
    public double getLocationLng() { return locationLng; }
    public String getLocationName() { return locationName; }
    public void setLocation(double lat, double lng, String name) {
        this.locationLat = lat;
        this.locationLng = lng;
        this.locationName = name;
    }
    public boolean isLocationSet() { return locationName != null; }

    public synchronized void clear() {
        currentDriver = null; online = false; shiftEarnings = 0;
        locationLat = 9.0320; locationLng = 38.7469; locationName = null;
    }
}
