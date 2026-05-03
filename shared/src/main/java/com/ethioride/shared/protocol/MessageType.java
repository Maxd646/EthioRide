package com.ethioride.shared.protocol;

public enum MessageType {
    // Auth
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    REGISTER_REQUEST,
    REGISTER_RESPONSE,
    LOGOUT,

    // Trip lifecycle
    TRIP_REQUEST,
    TRIP_ACCEPTED,
    TRIP_DECLINED,
    TRIP_STARTED,
    TRIP_COMPLETED,
    TRIP_CANCELLED,

    // Driver
    DRIVER_LOCATION_UPDATE,
    DRIVER_STATUS_UPDATE,

    // System
    HEARTBEAT,
    ERROR,
    ACK,

    // Matchmaking
    MATCH_FOUND,
    MATCH_NOTIFY_DRIVER,

    // Admin
    STATS_REQUEST,
    STATS_RESPONSE,
    DRIVERS_REQUEST,
    DRIVERS_RESPONSE,
    TRIPS_REQUEST,
    TRIPS_RESPONSE
}
