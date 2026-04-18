package com.ethioride.admin.model;

import java.io.Serializable;

/**
 * Snapshot of live system metrics sent from the server to the admin dashboard.
 */
public class DashboardStats implements Serializable {

    private int activeDrivers;
    private int driversDelta;        // % change
    private double driversUtilization;

    private int ongoingTrips;
    private int tripTarget;
    private boolean peakHour;

    private String heartbeatStatus;  // e.g. "PULSE : OK"
    private double serverLoad;       // 0.0 – 1.0

    private long latencyMs;
    private double uptimePercent;

    private int tcpRequests;
    private int wsConnections;
    private long memUsedMb;
    private long memTotalMb;

    private double projectedDemandDelta; // % change

    public DashboardStats() {}

    // --- Getters & Setters ---

    public int getActiveDrivers() { return activeDrivers; }
    public void setActiveDrivers(int v) { this.activeDrivers = v; }

    public int getDriversDelta() { return driversDelta; }
    public void setDriversDelta(int v) { this.driversDelta = v; }

    public double getDriversUtilization() { return driversUtilization; }
    public void setDriversUtilization(double v) { this.driversUtilization = v; }

    public int getOngoingTrips() { return ongoingTrips; }
    public void setOngoingTrips(int v) { this.ongoingTrips = v; }

    public int getTripTarget() { return tripTarget; }
    public void setTripTarget(int v) { this.tripTarget = v; }

    public boolean isPeakHour() { return peakHour; }
    public void setPeakHour(boolean v) { this.peakHour = v; }

    public String getHeartbeatStatus() { return heartbeatStatus; }
    public void setHeartbeatStatus(String v) { this.heartbeatStatus = v; }

    public double getServerLoad() { return serverLoad; }
    public void setServerLoad(double v) { this.serverLoad = v; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long v) { this.latencyMs = v; }

    public double getUptimePercent() { return uptimePercent; }
    public void setUptimePercent(double v) { this.uptimePercent = v; }

    public int getTcpRequests() { return tcpRequests; }
    public void setTcpRequests(int v) { this.tcpRequests = v; }

    public int getWsConnections() { return wsConnections; }
    public void setWsConnections(int v) { this.wsConnections = v; }

    public long getMemUsedMb() { return memUsedMb; }
    public void setMemUsedMb(long v) { this.memUsedMb = v; }

    public long getMemTotalMb() { return memTotalMb; }
    public void setMemTotalMb(long v) { this.memTotalMb = v; }

    public double getProjectedDemandDelta() { return projectedDemandDelta; }
    public void setProjectedDemandDelta(double v) { this.projectedDemandDelta = v; }
}
