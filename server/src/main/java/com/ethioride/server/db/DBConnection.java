package com.ethioride.server.db;

import com.ethioride.server.logging.ServerLogger;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Database connection following the course JDBC steps exactly:
 *
 *  Step 1 – import java.sql.*
 *  Step 2 – Load and register the driver  (Class.forName)
 *  Step 3 – Connect to the database       (DriverManager.getConnection)
 *  Step 4 – Create statements
 *  Step 5 – Execute statements & process results
 *  Step 6 – Close ResultSet and Statement
 *  Step 7 – Close the connection
 *
 * Usage:
 *   Connection conn = DBConnection.getConnection();
 *   // ... use conn with PreparedStatement ...
 *   conn.close();   // always close when done
 */
public class DBConnection {

    // JDBC URL format:  jdbc:subprotocol:source
    // MySQL subprotocol: mysql
    private static String url;
    private static String user;
    private static String password;

    static {
        // Load credentials from db.properties
        try {
            Properties props = new Properties();

            // Try classpath first (works when running from 'out/' directory)
            InputStream in = DBConnection.class.getResourceAsStream("/db.properties");

            // Fallback: load directly from the filesystem relative to working directory
            if (in == null) {
                java.io.File f = new java.io.File("db.properties");
                if (!f.exists()) f = new java.io.File("server/src/main/resources/db.properties");
                if (f.exists()) in = new java.io.FileInputStream(f);
            }

            if (in == null) {
                ServerLogger.getInstance().error("DB db.properties not found on classpath or filesystem!");
            } else {
                props.load(in);
                in.close();
            }

            String host = props.getProperty("db.host", "localhost");
            String port = props.getProperty("db.port", "3306");
            String name = props.getProperty("db.name", "ethioride");

            url      = "jdbc:mysql://" + host + ":" + port + "/" + name
                     + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            user     = props.getProperty("db.user", "root");
            password = props.getProperty("db.password", "");

            Class.forName("com.mysql.cj.jdbc.Driver");
            ServerLogger.getInstance().info("DB driver registered. URL: " + url);

        } catch (ClassNotFoundException e) {
            ServerLogger.getInstance().error("DB MySQL driver not found — add mysql-connector-j.jar to lib/");
        } catch (Exception e) {
            ServerLogger.getInstance().error("DB failed to load db.properties: " + e.getMessage());
        }
    }

    /**
     * Step 3: Connect to the database.
     * DriverManager.getConnection(url, user, password) returns a Connection object.
     * Each caller is responsible for closing the connection (conn.close()).
     */
    public static Connection getConnection() throws Exception {
        // DriverManager.getConnection() — gateway to the database
        return DriverManager.getConnection(url, user, password);
    }
}
