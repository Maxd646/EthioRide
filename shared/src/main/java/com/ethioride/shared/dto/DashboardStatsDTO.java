package com.ethioride.shared.dto;

import java.io.Serializable;

/**
 * Carries live dashboard statistics from server to admin dashboard.
 */
public class DashboardStatsDTO implements Serializable {
    private int totalDrivers;
    private int onlineDrivers;
    private int totalPassengers;
    private int activeTrips;
    private int completedTrips;
    private int cancelledTrips;
    private double totalRevenue;

    public DashboardStatsDTO() {}

    public int getTotalDrivers()     { return totalDrivers; }
    public void setTotalDrivers(int v)     { this.totalDrivers = v; }
    public int getOnlineDrivers()    { return onlineDrivers; }
    public void setOnlineDrivers(int v)    { this.onlineDrivers = v; }
    public int getTotalPassengers()  { return totalPassengers; }
    public void setTotalPassengers(int v)  { this.totalPassengers = v; }
    public int getActiveTrips()      { return activeTrips; }
    public void setActiveTrips(int v)      { this.activeTrips = v; }
    public int getCompletedTrips()   { return completedTrips; }
    public void setCompletedTrips(int v)   { this.completedTrips = v; }
    public int getCancelledTrips()   { return cancelledTrips; }
    public void setCancelledTrips(int v)   { this.cancelledTrips = v; }
    public double getTotalRevenue()  { return totalRevenue; }
    public void setTotalRevenue(double v)  { this.totalRevenue = v; }
}
