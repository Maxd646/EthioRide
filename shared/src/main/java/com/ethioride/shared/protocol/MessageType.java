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

    // Admin - User Management
    USER_LIST_REQUEST,
    USER_LIST_RESPONSE,
    USER_CREATE_REQUEST,
    USER_CREATE_RESPONSE,
    USER_DELETE_REQUEST,
    USER_DELETE_RESPONSE,

    // System
    HEARTBEAT,
    ERROR,
    ACK
}
