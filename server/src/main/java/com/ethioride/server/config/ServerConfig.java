package com.ethioride.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = ServerConfig.class.getResourceAsStream("/server.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            System.err.println("Could not load server.properties: " + e.getMessage());
        }
    }

    public static String getHost() { return props.getProperty("server.host", "0.0.0.0"); }
    public static int getPort() { return Integer.parseInt(props.getProperty("server.port", "9090")); }
    public static int getThreadPoolSize() { return Integer.parseInt(props.getProperty("server.thread.pool.size", "50")); }
    public static long getHeartbeatInterval() { return Long.parseLong(props.getProperty("server.heartbeat.interval.ms", "5000")); }
    public static long getSessionTimeout() { return Long.parseLong(props.getProperty("server.session.timeout.ms", "300000")); }
    public static int getMaxConnections() { return Integer.parseInt(props.getProperty("server.max.connections", "500")); }
}
