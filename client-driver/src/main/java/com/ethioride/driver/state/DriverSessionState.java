package com.ethioride.driver.state;

import com.ethioride.shared.dto.UserDTO;

public class DriverSessionState {
    private static DriverSessionState instance;
    private UserDTO currentDriver;
    private boolean online;
    private double shiftEarnings;

    private DriverSessionState() {}

    public static synchronized DriverSessionState getInstance() {
        if (instance == null) instance = new DriverSessionState();
        return instance;
    }

    public UserDTO getCurrentDriver() { return currentDriver; }
    public void setCurrentDriver(UserDTO driver) { this.currentDriver = driver; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public double getShiftEarnings() { return shiftEarnings; }
    public void addEarnings(double amount) { this.shiftEarnings += amount; }
    public boolean isLoggedIn() { return currentDriver != null; }
    public void clear() { currentDriver = null; online = false; shiftEarnings = 0; }
}
