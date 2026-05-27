package com.ethioride.passenger.ui;

import com.ethioride.passenger.network.ServerConnection;
import com.ethioride.passenger.state.SessionState;
import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.dto.PriceEstimateDTO;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Main passenger screen.
 *
 * States:
 *  BOOKING      — user enters pickup/destination, sees price estimate
 *  SEARCHING    — trip sent, waiting for a driver to be matched
 *  DRIVER_FOUND — driver accepted, shows driver info + Cancel button
 *  IN_PROGRESS  — driver picked up passenger, trip underway
 *  COMPLETED    — trip done, shows final fare + rate button
 */
public class MainScreen {

    private enum TripState { BOOKING, SEARCHING, DRIVER_FOUND, IN_PROGRESS, COMPLETED }

    private final Stage stage;

    // Booking panel widgets
    private TextField     tfPickup;
    private TextField     tfDestination;
    private Label         lblPriceEstimate;
    private Button        btnRequest;
    private RideCategory  selectedCategory = RideCategory.ECONOMY;
    private Button        btnEconomy, btnPremium, btnElite;
    private PriceEstimateDTO currentEstimate;

    // Trip status panel (shown after booking)
    private VBox          tripStatusPanel;
    private Label         lblTripStatus;
    private Label         lblDriverName;
    private Label         lblDriverPhone;
    private Label         lblDriverRating;
    private Label         lblFinalFare;
    private Button        btnCancelTrip;

    // Active trip
    private String        activeTripId;
    private TripState     tripState = TripState.BOOKING;

    // Debounce for price estimate — avoids hammering Nominatim on every keystroke
    private final ScheduledExecutorService debouncer =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "price-debouncer");
            t.setDaemon(true);
            return t;
        });
    private ScheduledFuture<?> pendingEstimate;

    // Location suggestion label
    private Label lblLocationHint;

    public MainScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildMainPanel());
        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
        detectAndSuggestLocation(); // auto-fill pickup from IP location
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #0d1526; -fx-border-color: #1e3a5f; -fx-border-width: 0 1 0 0;");

        Label logo = new Label("🚕 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#3b82f6"));
        logo.setPadding(new Insets(24, 20, 8, 20));

        Label sub = new Label("Passenger Suite");
        sub.setFont(Font.font("Arial", 11));
        sub.setTextFill(Color.web("#475569"));
        sub.setPadding(new Insets(0, 20, 20, 20));

        Button btnMap      = navButton("🗺  Map");
        Button btnHistory  = navButton("🕐  Ride History");
        Button btnPayments = navButton("💳  Payments");
        Button btnSettings = navButton("⚙  Settings");
        btnMap.setStyle(btnMap.getStyle() + "-fx-background-color: #1e3a5f;");

        btnHistory.setOnAction(e -> new RideHistoryScreen(stage).show());
        btnPayments.setOnAction(e -> new PaymentsScreen(stage).show());
        btnSettings.setOnAction(e -> new SettingsScreen(stage).show());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        SessionState session = SessionState.getInstance();
        String name  = session.isLoggedIn() ? session.getCurrentUser().getFullName() : "Guest";
        String phone = session.isLoggedIn() ? session.getCurrentUser().getPhone()    : "";

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

        sidebar.getChildren().addAll(logo, sub, btnMap, btnHistory, btnPayments, btnSettings,
            spacer, lblName, lblPhone, btnSignOut);
        return sidebar;
    }

    // ── Main panel (booking + trip status stacked) ────────────────────────────

    private StackPane buildMainPanel() {
        StackPane stack = new StackPane();

        VBox bookingPanel = buildBookingPanel();
        tripStatusPanel   = buildTripStatusPanel();
        tripStatusPanel.setVisible(false);
        tripStatusPanel.setManaged(false);

        stack.getChildren().addAll(bookingPanel, tripStatusPanel);
        return stack;
    }

    // ── Booking Panel ─────────────────────────────────────────────────────────

    private VBox buildBookingPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #0a0e1a;");

        Label lblTitle = new Label("Where to?");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        lblTitle.setTextFill(Color.web("#f1f5f9"));

        Label lblSub = new Label("Your ride, your way.");
        lblSub.setFont(Font.font("Arial", 13));
        lblSub.setTextFill(Color.web("#475569"));

        Label lblPickupLbl = new Label("Pickup Location");
        lblPickupLbl.setTextFill(Color.web("#94a3b8"));
        lblPickupLbl.setFont(Font.font("Arial", 12));

        tfPickup = new TextField();
        tfPickup.setPromptText("Detecting your location...");
        styleField(tfPickup);
        tfPickup.textProperty().addListener((o, ov, nv) -> schedulePriceEstimate());

        // Location hint shown while auto-detecting
        lblLocationHint = new Label("📍 Detecting your location...");
        lblLocationHint.setTextFill(Color.web("#475569"));
        lblLocationHint.setFont(Font.font("Arial", 11));
        lblLocationHint.setVisible(false);
        lblLocationHint.setManaged(false);

        Label lblDestLbl = new Label("Destination");
        lblDestLbl.setTextFill(Color.web("#94a3b8"));
        lblDestLbl.setFont(Font.font("Arial", 12));

        tfDestination = new TextField();
        tfDestination.setPromptText("Enter destination...");
        styleField(tfDestination);
        tfDestination.textProperty().addListener((o, ov, nv) -> schedulePriceEstimate());

        Label lblCat = new Label("Select Ride Type");
        lblCat.setTextFill(Color.web("#94a3b8"));
        lblCat.setFont(Font.font("Arial", 12));

        HBox categoryRow = buildCategoryRow();

        lblPriceEstimate = new Label("Enter destination for price estimate");
        lblPriceEstimate.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblPriceEstimate.setTextFill(Color.web("#3b82f6"));
        lblPriceEstimate.setStyle("-fx-background-color: #0d1526; -fx-padding: 12px; " +
            "-fx-background-radius: 8px; -fx-border-color: #1e3a5f; -fx-border-radius: 8px;");
        lblPriceEstimate.setMaxWidth(Double.MAX_VALUE);

        btnRequest = new Button("Request Ride");
        btnRequest.setMaxWidth(Double.MAX_VALUE);
        btnRequest.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        btnRequest.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
            "-fx-background-radius: 10px; -fx-padding: 14px; -fx-cursor: hand;");
        btnRequest.setOnAction(e -> onRequestRide());

        HBox quickRow = buildQuickLocations();

        panel.getChildren().addAll(lblTitle, lblSub,
            lblPickupLbl, tfPickup, lblLocationHint,
            lblDestLbl, tfDestination,
            lblCat, categoryRow, lblPriceEstimate, btnRequest,
            new Separator(), quickRow);
        return panel;
    }

    // ── Trip Status Panel ─────────────────────────────────────────────────────

    private VBox buildTripStatusPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: #0a0e1a;");
        panel.setAlignment(Pos.TOP_CENTER);

        lblTripStatus = new Label("Looking for a driver...");
        lblTripStatus.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTripStatus.setTextFill(Color.web("#f1f5f9"));

        // Driver info card (hidden until driver found)
        VBox driverCard = new VBox(10);
        driverCard.setStyle("-fx-background-color: #0d1526; -fx-border-color: #22c55e; " +
            "-fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 20;");
        driverCard.setMaxWidth(400);

        Label lblDriverTitle = new Label("Your Driver");
        lblDriverTitle.setTextFill(Color.web("#94a3b8"));
        lblDriverTitle.setFont(Font.font("Arial", 12));

        lblDriverName = new Label("—");
        lblDriverName.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblDriverName.setTextFill(Color.web("#f1f5f9"));

        lblDriverPhone = new Label("—");
        lblDriverPhone.setTextFill(Color.web("#94a3b8"));
        lblDriverPhone.setFont(Font.font("Arial", 13));

        lblDriverRating = new Label("★ —");
        lblDriverRating.setTextFill(Color.web("#f59e0b"));
        lblDriverRating.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        driverCard.getChildren().addAll(lblDriverTitle, lblDriverName, lblDriverPhone, lblDriverRating);

        // Final fare (shown on completion)
        lblFinalFare = new Label();
        lblFinalFare.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblFinalFare.setTextFill(Color.web("#22c55e"));
        lblFinalFare.setVisible(false);
        lblFinalFare.setManaged(false);

        // Cancel button
        btnCancelTrip = new Button("Cancel Trip");
        btnCancelTrip.setMaxWidth(400);
        btnCancelTrip.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnCancelTrip.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; " +
            "-fx-background-radius: 10px; -fx-padding: 12px; -fx-cursor: hand;");
        btnCancelTrip.setOnAction(e -> onCancelTrip());

        // "Book another" button (shown after completion)
        Button btnBookAnother = new Button("Book Another Ride");
        btnBookAnother.setMaxWidth(400);
        btnBookAnother.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnBookAnother.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
            "-fx-background-radius: 10px; -fx-padding: 12px; -fx-cursor: hand;");
        btnBookAnother.setVisible(false);
        btnBookAnother.setManaged(false);
        btnBookAnother.setOnAction(e -> resetToBooking(driverCard, btnBookAnother));

        panel.getChildren().addAll(lblTripStatus, driverCard, lblFinalFare, btnCancelTrip, btnBookAnother);

        // Store refs for state transitions
        panel.setUserData(new Object[]{driverCard, btnBookAnother});
        return panel;
    }

    // ── Category row ──────────────────────────────────────────────────────────

    private HBox buildCategoryRow() {
        btnEconomy = categoryButton("🚗", "ECONOMY", "ETB " + (int) AppConstants.ECONOMY_BASE_FARE);
        btnPremium = categoryButton("🚐", "PREMIUM", "ETB " + (int) AppConstants.PREMIUM_BASE_FARE);
        btnElite   = categoryButton("🚙", "ELITE",   "ETB " + (int) AppConstants.ELITE_BASE_FARE);
        selectCategory(btnEconomy, RideCategory.ECONOMY);
        btnEconomy.setOnAction(e -> selectCategory(btnEconomy, RideCategory.ECONOMY));
        btnPremium.setOnAction(e -> selectCategory(btnPremium, RideCategory.PREMIUM));
        btnElite.setOnAction(e   -> selectCategory(btnElite,   RideCategory.ELITE));
        return new HBox(12, btnEconomy, btnPremium, btnElite);
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
        schedulePriceEstimate();
    }

    private HBox buildQuickLocations() {
        HBox row = new HBox(24);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            quickLocation("🏠", "Home",      "Garnet Avenue"),
            quickLocation("💼", "Work",      "Kazanchis Square"),
            quickLocation("⭐", "Favorites", "Friendship Park")
        );
        return row;
    }

    private VBox quickLocation(String icon, String name, String addr) {
        Label ico  = new Label(icon); ico.setFont(Font.font("Arial", 20));
        Label lbl  = new Label(name); lbl.setTextFill(Color.web("#f1f5f9")); lbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label addr2 = new Label(addr); addr2.setTextFill(Color.web("#475569")); addr2.setFont(Font.font("Arial", 10));
        VBox box = new VBox(4, ico, lbl, addr2);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-cursor: hand;");
        box.setOnMouseClicked(e -> { tfDestination.setText(addr); schedulePriceEstimate(); });
        return box;
    }

    // ── Price estimate ────────────────────────────────────────────────────────

    /**
     * Debounced wrapper — waits 500ms after the user stops typing before
     * firing the actual estimate request. Prevents hammering Nominatim on
     * every keystroke.
     */
    private void schedulePriceEstimate() {
        if (pendingEstimate != null && !pendingEstimate.isDone()) {
            pendingEstimate.cancel(false);
        }
        pendingEstimate = debouncer.schedule(
            () -> Platform.runLater(this::updatePriceEstimate),
            500, TimeUnit.MILLISECONDS
        );
    }

    private void updatePriceEstimate() {
        String pickup = tfPickup.getText().trim();
        String dest   = tfDestination.getText().trim();

        if (pickup.isEmpty() || dest.isEmpty()) {
            lblPriceEstimate.setText("Enter destination for price estimate");
            lblPriceEstimate.setTextFill(Color.web("#475569"));
            currentEstimate = null;
            return;
        }
        if (pickup.equals(dest)) {
            lblPriceEstimate.setText("Pickup and destination cannot be the same");
            lblPriceEstimate.setTextFill(Color.web("#ef4444"));
            currentEstimate = null;
            return;
        }

        lblPriceEstimate.setText("Calculating price...");
        lblPriceEstimate.setTextFill(Color.web("#94a3b8"));

        Thread t = new Thread(() -> {
            try {
                ServerConnection conn = new ServerConnection();
                conn.connect();
                Message response = conn.sendAndWait(
                    new Message(MessageType.PRICE_ESTIMATE_REQUEST,
                        pickup + "|" + dest + "|" + selectedCategory.name(), "passenger"),
                    MessageType.PRICE_ESTIMATE_RESPONSE, 15000);
                conn.close();

                Platform.runLater(() -> {
                    if (response != null && response.getType() == MessageType.PRICE_ESTIMATE_RESPONSE) {
                        currentEstimate = (PriceEstimateDTO) response.getPayload();
                        lblPriceEstimate.setText(String.format("💰 ETB %.2f  •  %.1f km  •  ~%.0f min",
                            currentEstimate.getTotalFare(),
                            currentEstimate.getDistanceKm(),
                            currentEstimate.getDurationMinutes()));
                        lblPriceEstimate.setTextFill(Color.web("#22c55e"));
                    } else {
                        lblPriceEstimate.setText("Price estimation unavailable");
                        lblPriceEstimate.setTextFill(Color.web("#ef4444"));
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblPriceEstimate.setText("Price estimation unavailable (server offline)");
                    lblPriceEstimate.setTextFill(Color.web("#ef4444"));
                });
            }
        }, "price-estimate-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Request ride ──────────────────────────────────────────────────────────

    private void onRequestRide() {
        String pickup = tfPickup.getText().trim();
        String dest   = tfDestination.getText().trim();

        if (pickup.isEmpty() || dest.isEmpty()) {
            showInfo("Missing Info", "Please enter pickup and destination.");
            return;
        }
        if (currentEstimate == null) {
            showInfo("Price Loading", "Please wait for the price estimate to load.");
            return;
        }

        btnRequest.setDisable(true);
        btnRequest.setText("Sending request...");

        final double fare       = currentEstimate.getTotalFare();
        final double distanceKm = currentEstimate.getDistanceKm();
        final double originLat  = currentEstimate.getOriginLat();
        final double originLng  = currentEstimate.getOriginLng();
        final double destLat    = currentEstimate.getDestLat();
        final double destLng    = currentEstimate.getDestLng();
        final String tripId     = UUID.randomUUID().toString();
        activeTripId = tripId;

        Thread t = new Thread(() -> {
            try {
                TripRequestDTO trip = new TripRequestDTO();
                trip.setTripId(tripId);
                trip.setPassengerId(SessionState.getInstance().isLoggedIn()
                    ? SessionState.getInstance().getCurrentUser().getId() : "guest");
                trip.setPickupLocation(pickup);
                trip.setDropoffLocation(dest);
                trip.setCategory(selectedCategory);
                trip.setFare(fare);
                trip.setDistanceKm(distanceKm);
                trip.setPickupLat(originLat);
                trip.setPickupLng(originLng);
                trip.setDropoffLat(destLat);
                trip.setDropoffLng(destLng);

                // Use the persistent connection so the server can push back to us
                ServerConnection conn = ServerConnection.getPersistent();
                if (!conn.isConnected()) conn.connect();

                // Register passenger socket with server via login-less trip request
                // (server registers the socket on TRIP_REQUEST if not already registered)
                conn.send(new Message(MessageType.TRIP_REQUEST, trip, trip.getPassengerId()));

                // Start listening for push messages (TRIP_ACCEPTED, TRIP_STARTED, etc.)
                conn.startListening(this::handleServerPush);

                Platform.runLater(() -> {
                    btnRequest.setDisable(false);
                    btnRequest.setText("Request Ride");
                    switchToTripStatus("🔍  Looking for a driver near you...", false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnRequest.setDisable(false);
                    btnRequest.setText("Request Ride");
                    showInfo("Error", "Cannot reach server. Is it running?");
                });
            }
        }, "trip-request-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Server push handler ───────────────────────────────────────────────────

    /**
     * Handles all server-pushed messages during an active trip.
     */
    private void handleServerPush(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getType()) {

                case TRIP_ACCEPTED -> {
                    // Payload: "driverName|driverPhone|rating|tripId"
                    String[] parts = msg.getPayload().toString().split("\\|", 4);
                    String driverName   = parts.length > 0 ? parts[0] : "Your driver";
                    String driverPhone  = parts.length > 1 ? parts[1] : "";
                    String ratingStr    = parts.length > 2 ? parts[2] : "5.0";

                    tripState = TripState.DRIVER_FOUND;
                    lblTripStatus.setText("✅  Driver is on the way!");
                    lblTripStatus.setTextFill(Color.web("#22c55e"));
                    lblDriverName.setText(driverName);
                    lblDriverPhone.setText(driverPhone);
                    lblDriverRating.setText("★ " + ratingStr);
                    btnCancelTrip.setVisible(true);
                    btnCancelTrip.setManaged(true);
                }

                case TRIP_STARTED -> {
                    tripState = TripState.IN_PROGRESS;
                    lblTripStatus.setText("🚗  Trip in progress...");
                    lblTripStatus.setTextFill(Color.web("#f59e0b"));
                    // Can still cancel while in progress (though unusual)
                }

                case TRIP_COMPLETED -> {
                    // Payload: final fare as string
                    String fareStr = msg.getPayload().toString();
                    tripState = TripState.COMPLETED;
                    lblTripStatus.setText("🎉  Trip completed!");
                    lblTripStatus.setTextFill(Color.web("#22c55e"));
                    lblFinalFare.setText("ETB " + fareStr);
                    lblFinalFare.setVisible(true);
                    lblFinalFare.setManaged(true);
                    btnCancelTrip.setVisible(false);
                    btnCancelTrip.setManaged(false);

                    // Show "Book another" button
                    Object[] userData = (Object[]) tripStatusPanel.getUserData();
                    if (userData != null) {
                        Button btnBookAnother = (Button) userData[1];
                        btnBookAnother.setVisible(true);
                        btnBookAnother.setManaged(true);
                    }
                    ServerConnection.getPersistent().stopListening();
                }

                case TRIP_CANCELLED -> {
                    tripState = TripState.BOOKING;
                    lblTripStatus.setText("❌  Trip was cancelled.");
                    lblTripStatus.setTextFill(Color.web("#ef4444"));
                    btnCancelTrip.setVisible(false);
                    btnCancelTrip.setManaged(false);

                    Object[] userData = (Object[]) tripStatusPanel.getUserData();
                    if (userData != null) {
                        Button btnBookAnother = (Button) userData[1];
                        btnBookAnother.setVisible(true);
                        btnBookAnother.setManaged(true);
                    }
                    ServerConnection.getPersistent().stopListening();
                }

                default -> {} // ignore other messages
            }
        });
    }

    // ── Trip actions ──────────────────────────────────────────────────────────

    private void onCancelTrip() {
        if (activeTripId == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Cancel your current trip?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Cancel Trip");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String passengerId = SessionState.getInstance().isLoggedIn()
                    ? SessionState.getInstance().getCurrentUser().getId() : "guest";
                Thread t = new Thread(() -> {
                    try {
                        ServerConnection conn = ServerConnection.getPersistent();
                        if (conn.isConnected()) {
                            conn.send(new Message(MessageType.TRIP_CANCELLED, activeTripId, passengerId));
                        }
                    } catch (Exception ex) {
                        System.err.println("[Passenger] Cancel failed: " + ex.getMessage());
                    }
                }, "trip-cancel");
                t.setDaemon(true);
                t.start();
            }
        });
    }

    // ── UI state transitions ──────────────────────────────────────────────────

    private void switchToTripStatus(String statusText, boolean showDriverCard) {
        tripStatusPanel.setVisible(true);
        tripStatusPanel.setManaged(true);
        lblTripStatus.setText(statusText);
        lblTripStatus.setTextFill(Color.web("#94a3b8"));
        lblDriverName.setText("—");
        lblDriverPhone.setText("—");
        lblDriverRating.setText("★ —");
        lblFinalFare.setVisible(false);
        lblFinalFare.setManaged(false);
        btnCancelTrip.setVisible(true);
        btnCancelTrip.setManaged(true);

        Object[] userData = (Object[]) tripStatusPanel.getUserData();
        if (userData != null) {
            VBox driverCard = (VBox) userData[0];
            Button btnBookAnother = (Button) userData[1];
            driverCard.setVisible(showDriverCard);
            driverCard.setManaged(showDriverCard);
            btnBookAnother.setVisible(false);
            btnBookAnother.setManaged(false);
        }
    }

    private void resetToBooking(VBox driverCard, Button btnBookAnother) {
        activeTripId = null;
        currentEstimate = null;
        tripState = TripState.BOOKING;
        ServerConnection.resetPersistent();

        tripStatusPanel.setVisible(false);
        tripStatusPanel.setManaged(false);
        driverCard.setVisible(false);
        driverCard.setManaged(false);
        btnBookAnother.setVisible(false);
        btnBookAnother.setManaged(false);

        tfDestination.clear();
        lblPriceEstimate.setText("Enter destination for price estimate");
        lblPriceEstimate.setTextFill(Color.web("#475569"));
    }

    // ── Location detection ────────────────────────────────────────────────────

    /**
     * Detects the user's approximate location using IP geolocation (ip-api.com,
     * free, no key), then reverse-geocodes the coordinates via Nominatim to get
     * a human-readable street/area name, and pre-fills the pickup field.
     *
     * The user can always override the suggested value by typing.
     */
    private void detectAndSuggestLocation() {
        // Show hint while detecting
        lblLocationHint.setText("📍 Detecting your location...");
        lblLocationHint.setTextFill(Color.web("#475569"));
        lblLocationHint.setVisible(true);
        lblLocationHint.setManaged(true);

        Thread t = new Thread(() -> {
            try {
                // Step 1 — IP geolocation (free, no key, ~city-level accuracy)
                String ipJson = httpGet("http://ip-api.com/json?fields=lat,lon,city,status");
                org.json.JSONObject ipData = new org.json.JSONObject(ipJson);

                if (!"success".equals(ipData.optString("status"))) {
                    // IP lookup failed — fall back to Addis Ababa center
                    suggestPickup("Addis Ababa, Ethiopia", false);
                    return;
                }

                double lat  = ipData.getDouble("lat");
                double lon  = ipData.getDouble("lon");
                String city = ipData.optString("city", "");

                // Step 2 — Nominatim reverse geocode to get a street/area name
                String reverseUrl = String.format(
                    "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json&zoom=16",
                    lat, lon);
                String reverseJson = httpGet(reverseUrl, true);
                org.json.JSONObject place = new org.json.JSONObject(reverseJson);

                // Build a concise address: "Neighbourhood, City" or "Road, City"
                org.json.JSONObject addr = place.optJSONObject("address");
                String suggestion;
                if (addr != null) {
                    String area = firstNonEmpty(
                        addr.optString("neighbourhood"),
                        addr.optString("suburb"),
                        addr.optString("quarter"),
                        addr.optString("road"),
                        addr.optString("pedestrian")
                    );
                    String cityName = firstNonEmpty(
                        addr.optString("city"),
                        addr.optString("town"),
                        addr.optString("village"),
                        city
                    );
                    suggestion = area.isEmpty()
                        ? (cityName.isEmpty() ? place.optString("display_name", "Addis Ababa") : cityName)
                        : (cityName.isEmpty() ? area : area + ", " + cityName);
                } else {
                    suggestion = city.isEmpty() ? "Addis Ababa, Ethiopia" : city + ", Ethiopia";
                }

                suggestPickup(suggestion, true);

            } catch (Exception ex) {
                // Any failure — silently fall back, don't bother the user
                suggestPickup("Addis Ababa, Ethiopia", false);
            }
        }, "location-detect");
        t.setDaemon(true);
        t.start();
    }

    /** Pre-fills the pickup field only if the user hasn't typed anything yet. */
    private void suggestPickup(String address, boolean fromGps) {
        Platform.runLater(() -> {
            // Only auto-fill if the field is still empty or has the default placeholder
            String current = tfPickup.getText().trim();
            if (current.isEmpty()) {
                tfPickup.setText(address);
                lblLocationHint.setText(fromGps
                    ? "📍 Using your approximate location — tap to change"
                    : "📍 Could not detect location — using default");
                lblLocationHint.setTextFill(fromGps
                    ? Color.web("#22c55e") : Color.web("#475569"));
            } else {
                // User already typed something — just hide the hint
                lblLocationHint.setVisible(false);
                lblLocationHint.setManaged(false);
            }
        });
    }

    /** Returns the first non-empty, non-null string from the candidates. */
    private String firstNonEmpty(String... candidates) {
        for (String s : candidates) {
            if (s != null && !s.isBlank()) return s;
        }
        return "";
    }

    /**
     * Simple HTTP GET helper for client-side use (IP geolocation + Nominatim reverse).
     * Separate from the server's RoutingService — runs in the passenger app JVM.
     */
    private String httpGet(String urlString) throws Exception {
        return httpGet(urlString, false);
    }

    private String httpGet(String urlString, boolean nominatimUserAgent) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6_000);
        conn.setReadTimeout(6_000);
        if (nominatimUserAgent) {
            conn.setRequestProperty("User-Agent",
                "EthioRide/1.0 (ride-hailing app; contact@ethioride.com)");
        }
        int status = conn.getResponseCode();
        if (status != 200) throw new Exception("HTTP " + status);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

    private void onSignOut() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Sign out of EthioRide?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Sign Out");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                ServerConnection.resetPersistent();
                SessionState.getInstance().clear();
                new LoginScreen(stage).show();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(title); a.showAndWait();
    }

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color: #0d1526; -fx-text-fill: #f1f5f9; " +
            "-fx-prompt-text-fill: #475569; -fx-border-color: #1e3a5f; " +
            "-fx-border-radius: 8px; -fx-background-radius: 8px; " +
            "-fx-padding: 10px 14px; -fx-font-size: 13px;");
        f.setMaxWidth(Double.MAX_VALUE);
    }

    private Button navButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; " +
            "-fx-padding: 10px 20px; -fx-cursor: hand; -fx-background-radius: 6px;");
        return btn;
    }

    private String tilestyle(boolean selected) {
        return "-fx-background-color: " + (selected ? "#1e3a5f" : "#0d1526") + ";" +
               "-fx-text-fill: "        + (selected ? "#f1f5f9" : "#94a3b8") + ";" +
               "-fx-border-color: "     + (selected ? "#3b82f6" : "#1e3a5f") + ";" +
               "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;";
    }
}
