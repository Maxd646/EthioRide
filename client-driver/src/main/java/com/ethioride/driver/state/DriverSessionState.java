package com.ethioride.driver.state;

import com.ethioride.shared.dto.UserDTO;

public class DriverSessionState {
    private static DriverSessionState instance;
    private volatile UserDTO currentDriver;
    private volatile boolean online;
    private double shiftEarnings; // guarded by synchronized methods

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
    public synchronized void clear() { currentDriver = null; online = false; shiftEarnings = 0; }
}
