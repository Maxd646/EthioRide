package com.ethioride.server.db;

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
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            Properties props = new Properties();
            props.load(in);

            String host = props.getProperty("db.host", "localhost");
            String port = props.getProperty("db.port", "3306");
            String name = props.getProperty("db.name", "ethioride");

            // JDBC URL — jdbc:mysql://host:port/dbname
            url      = "jdbc:mysql://" + host + ":" + port + "/" + name
                     + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            user     = props.getProperty("db.user", "root");
            password = props.getProperty("db.password", "");

            // Step 2: Load and register the MySQL driver using Class.forName()
            // This dynamically loads the driver class into memory at runtime
            Class.forName("com.mysql.cj.jdbc.Driver");

            System.out.println("[DB] Driver registered. URL: " + url);

        } catch (ClassNotFoundException e) {
            System.err.println("[DB] MySQL driver not found — add mysql-connector-j.jar to lib/");
        } catch (Exception e) {
            System.err.println("[DB] Failed to load db.properties: " + e.getMessage());
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
