package com.ethioride.shared.constants;

public final class AppConstants {
    private AppConstants() {}

    public static final String APP_NAME = "EthioRide";
    public static final String COUNTRY_CODE = "+251";

    // Server defaults (overridden by config)
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 9090;

    // Fare base rates (ETB) — must match pricing_rules seed in schema.sql
    public static final double ECONOMY_BASE_FARE = 50.0;
    public static final double PREMIUM_BASE_FARE = 80.0;
    public static final double ELITE_BASE_FARE   = 120.0;

    // UI
    public static final String DARK_THEME_CSS = "/ui/styles/dark-theme.css";
}
