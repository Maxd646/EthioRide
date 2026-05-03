package com.ethioride.server.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple file logger for the EthioRide server.
 * Writes timestamped entries to data/logs/server.log.
 * Thread-safe via synchronized write method.
 */
public class ServerLogger {

    private static final String LOG_DIR  = "data/logs";
    private static final String LOG_FILE = LOG_DIR + "/server.log";
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static ServerLogger instance;
    private PrintWriter writer;

    private ServerLogger() {
        try {
            Path dir = Paths.get(LOG_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            // append=true so logs survive restarts
            writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
            info("=== EthioRide Server started ===");
        } catch (IOException e) {
            System.err.println("[Logger] Could not open log file: " + e.getMessage());
        }
    }

    public static ServerLogger getInstance() {
        if (instance == null) instance = new ServerLogger();
        return instance;
    }

    public synchronized void info(String message) {
        log("INFO ", message);
    }

    public synchronized void warn(String message) {
        log("WARN ", message);
    }

    public synchronized void error(String message) {
        log("ERROR", message);
    }

    private void log(String level, String message) {
        String line = String.format("[%s] [%s] %s",
            LocalDateTime.now().format(FMT), level, message);
        System.out.println(line);
        if (writer != null) writer.println(line);
    }

    public void close() {
        if (writer != null) writer.close();
    }
}
