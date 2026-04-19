package com.ethioride.passenger.ui;

import com.ethioride.passenger.network.ServerConnection;
import com.ethioride.passenger.state.SessionState;
import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.RideCategory;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.UUID;

/**
 * Main passenger screen — pure JavaFX, no FXML.
 * Allows booking a ride and sends TripRequestDTO to the server.
 */
public class MainScreen {

    private final Stage stage;

    private TextField     tfPickup;
    private TextField     tfDestination;
    private Label         lblStatus;
    private Button        btnRequest;
    private RideCategory  selectedCategory = RideCategory.ECONOMY;

    // Category buttons
    private Button btnEconomy;
    private Button btnPremium;
    private Button btnElite;

    public MainScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0e1a;");

        root.setLeft(buildSidebar());
        root.setCenter(buildBookingPanel());

        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #0d1526; -fx-border-color: #1e3a5f; -fx-border-width: 0 1 0 0;");

        // Logo
        Label logo = new Label("🚕 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#3b82f6"));
        logo.setPadding(new Insets(24, 20, 8, 20));

        Label sub = new Label("Passenger Suite");
        sub.setFont(Font.font("Arial", 11));
        sub.setTextFill(Color.web("#475569"));
        sub.setPadding(new Insets(0, 20, 20, 20));

        // Nav buttons
        Button btnMap      = navButton("🗺  Map");
        Button btnHistory  = navButton("🕐  Ride History");
        Button btnPayments = navButton("💳  Payments");
        Button btnSettings = navButton("⚙  Settings");

        btnMap.setStyle(btnMap.getStyle() + "-fx-background-color: #1e3a5f;");

        btnHistory.setOnAction(e -> showInfo("Ride History", "Your past trips will appear here."));
        btnPayments.setOnAction(e -> showInfo("Payments", "Payment methods and history."));
        btnSettings.setOnAction(e -> showInfo("Settings", "App settings."));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User info
        SessionState session = SessionState.getInstance();
        String name  = session.isLoggedIn() ? session.getCurrentUser().getFullName() : "Guest";
        String phone = session.isLoggedIn() ? session.getCurrentUser().getPhone() : "";

        Label lblName  = new Label("👤 " + name);
        lblName.setTextFill(Color.web("#f1f5f9"));
        lblName.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblName.setPadding(new Insets(8, 20, 2, 20));

        Label lblPhone = new Label(phone);
        lblPhone.setTextFill(Color.web("#475569"));
        lblPhone.setFont(Font.font("Arial", 11));
        lblPhone.setPadding(new Insets(0, 20, 8, 20));

        Button btnSignOut = navButton("↩  Sign Out");
        btnSignOut.setOnAction(e -> onSignOut());

        sidebar.getChildren().addAll(
            logo, sub,
            btnMap, btnHistory, btnPayments, btnSettings,
            spacer,
            lblName, lblPhone, btnSignOut
        );
        return sidebar;
    }

    // ── Booking Panel ─────────────────────────────────────────────────────────

    private VBox buildBookingPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #0a0e1a;");

        Label lblTitle = new Label("Where to?");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        lblTitle.setTextFill(Color.web("#f1f5f9"));

        Label lblAmharic = new Label("Your ride, your way.");
        lblAmharic.setFont(Font.font("Arial", 13));
        lblAmharic.setTextFill(Color.web("#475569"));

        // Pickup
        Label lblPickup = new Label("Pickup Location");
        lblPickup.setTextFill(Color.web("#94a3b8"));
        lblPickup.setFont(Font.font("Arial", 12));

        tfPickup = new TextField("Meskel Square, Addis Ababa");
        styleField(tfPickup);

        // Destination
        Label lblDest = new Label("Destination");
        lblDest.setTextFill(Color.web("#94a3b8"));
        lblDest.setFont(Font.font("Arial", 12));

        tfDestination = new TextField();
        tfDestination.setPromptText("Enter destination...");
        styleField(tfDestination);

        // Ride category tiles
        Label lblCat = new Label("Select Ride Type");
        lblCat.setTextFill(Color.web("#94a3b8"));
        lblCat.setFont(Font.font("Arial", 12));

        HBox categoryRow = buildCategoryRow();

        // Status label
        lblStatus = new Label();
        lblStatus.setFont(Font.font("Arial", 13));
        lblStatus.setTextFill(Color.web("#22c55e"));
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);

        // Request button
        btnRequest = new Button("Request Ride");
        btnRequest.setMaxWidth(Double.MAX_VALUE);
        btnRequest.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        btnRequest.setStyle(
            "-fx-background-color: #3b82f6;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-padding: 14px;" +
            "-fx-cursor: hand;"
        );
        btnRequest.setOnAction(e -> onRequestRide());

        // Quick locations
        HBox quickRow = buildQuickLocations();

        panel.getChildren().addAll(
            lblTitle, lblAmharic,
            lblPickup, tfPickup,
            lblDest, tfDestination,
            lblCat, categoryRow,
            lblStatus,
            btnRequest,
            new Separator(),
            quickRow
        );
        return panel;
    }

    private HBox buildCategoryRow() {
        btnEconomy = categoryButton("🚗", "ECONOMY", "ETB " + (int) AppConstants.ECONOMY_BASE_FARE);
        btnPremium = categoryButton("🚐", "PREMIUM", "ETB " + (int) AppConstants.PREMIUM_BASE_FARE);
        btnElite   = categoryButton("🚙", "ELITE",   "ETB " + (int) AppConstants.ELITE_BASE_FARE);

        selectCategory(btnEconomy, RideCategory.ECONOMY);

        btnEconomy.setOnAction(e -> selectCategory(btnEconomy, RideCategory.ECONOMY));
        btnPremium.setOnAction(e -> selectCategory(btnPremium, RideCategory.PREMIUM));
        btnElite.setOnAction(e   -> selectCategory(btnElite,   RideCategory.ELITE));

        HBox row = new HBox(12, btnEconomy, btnPremium, btnElite);
        return row;
    }

    private Button categoryButton(String icon, String name, String fare) {
        Button btn = new Button(icon + "\n" + name + "\n" + fare);
        btn.setFont(Font.font("Arial", 11));
        btn.setStyle(tilestyle(false));
        btn.setPrefWidth(120);
        btn.setPrefHeight(80);
        return btn;
    }

    private void selectCategory(Button selected, RideCategory cat) {
        selectedCategory = cat;
        btnEconomy.setStyle(tilestyle(false));
        btnPremium.setStyle(tilestyle(false));
        btnElite.setStyle(tilestyle(false));
        selected.setStyle(tilestyle(true));
    }

    private HBox buildQuickLocations() {
        HBox row = new HBox(24);
        row.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(
            quickLocation("🏠", "Home", "Garnet Avenue"),
            quickLocation("💼", "Work", "Kazanchis Square"),
            quickLocation("⭐", "Favorites", "Friendship Park")
        );
        return row;
    }

    private VBox quickLocation(String icon, String name, String addr) {
        Label lblIcon = new Label(icon);
        lblIcon.setFont(Font.font("Arial", 20));

        Label lblName = new Label(name);
        lblName.setTextFill(Color.web("#f1f5f9"));
        lblName.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Label lblAddr = new Label(addr);
        lblAddr.setTextFill(Color.web("#475569"));
        lblAddr.setFont(Font.font("Arial", 10));

        VBox box = new VBox(4, lblIcon, lblName, lblAddr);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-cursor: hand;");
        box.setOnMouseClicked(e -> tfDestination.setText(addr));
        return box;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onRequestRide() {
        String pickup = tfPickup.getText().trim();
        String dest   = tfDestination.getText().trim();

        if (pickup.isEmpty() || dest.isEmpty()) {
            showStatus("Please enter pickup and destination.", false);
            return;
        }

        btnRequest.setDisable(true);
        btnRequest.setText("Finding driver...");

        Thread t = new Thread(() -> {
            try {
                TripRequestDTO trip = new TripRequestDTO();
                trip.setTripId(UUID.randomUUID().toString());
                trip.setPassengerId(SessionState.getInstance().isLoggedIn()
                    ? SessionState.getInstance().getCurrentUser().getId() : "guest");
                trip.setPickupLocation(pickup);
                trip.setDropoffLocation(dest);
                trip.setCategory(selectedCategory);
                trip.setFare(selectedCategory == RideCategory.ECONOMY ? AppConstants.ECONOMY_BASE_FARE
                           : selectedCategory == RideCategory.PREMIUM  ? AppConstants.PREMIUM_BASE_FARE
                           : AppConstants.ELITE_BASE_FARE);
                trip.setDistanceKm(5.0); // placeholder

                ServerConnection conn = new ServerConnection();
                conn.connect();
                Message response = conn.sendAndReceive(
                    new Message(MessageType.TRIP_REQUEST, trip, trip.getPassengerId())
                );
                conn.close();

                Platform.runLater(() -> {
                    showStatus("Ride requested! Looking for a driver near you...", true);
                    btnRequest.setDisable(false);
                    btnRequest.setText("Request Ride");
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showStatus("Cannot reach server. Is it running?", false);
                    btnRequest.setDisable(false);
                    btnRequest.setText("Request Ride");
                });
            }
        }, "trip-request-thread");
        t.setDaemon(true);
        t.start();
    }

    private void onSignOut() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Sign out of EthioRide?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Sign Out");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                SessionState.getInstance().clear();
                new LoginScreen(stage).show();
            }
        });
    }

    private void showStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(success ? Color.web("#22c55e") : Color.web("#ef4444"));
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(title);
        a.showAndWait();
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private void styleField(TextField f) {
        f.setStyle(
            "-fx-background-color: #0d1526;" +
            "-fx-text-fill: #f1f5f9;" +
            "-fx-prompt-text-fill: #475569;" +
            "-fx-border-color: #1e3a5f;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 10px 14px;" +
            "-fx-font-size: 13px;"
        );
        f.setMaxWidth(Double.MAX_VALUE);
    }

    private Button navButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #94a3b8;" +
            "-fx-padding: 10px 20px;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 6px;"
        );
        return btn;
    }

    private String tilestyle(boolean selected) {
        return "-fx-background-color: " + (selected ? "#1e3a5f" : "#0d1526") + ";" +
               "-fx-text-fill: " + (selected ? "#f1f5f9" : "#94a3b8") + ";" +
               "-fx-border-color: " + (selected ? "#3b82f6" : "#1e3a5f") + ";" +
               "-fx-border-radius: 8px;" +
               "-fx-background-radius: 8px;" +
               "-fx-cursor: hand;";
    }
}
