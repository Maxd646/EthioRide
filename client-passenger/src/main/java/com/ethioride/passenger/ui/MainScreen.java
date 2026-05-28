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
import javafx.scene.control.SplitPane;

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

    // Map view — real Leaflet map
    private MapView mapView;

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0e1a;");
        root.setLeft(buildSidebar());

        // Split: booking panel (left) + live map (right)
        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color:#0a0e1a;-fx-box-border:transparent;");
        split.setDividerPositions(0.38);

        VBox bookingWrapper = new VBox();
        bookingWrapper.setStyle("-fx-background-color:#0a0e1a;");
        bookingWrapper.getChildren().add(buildMainPanel());
        VBox.setVgrow(bookingWrapper.getChildren().get(0), Priority.ALWAYS);

        mapView = new MapView();
        VBox.setVgrow(mapView, Priority.ALWAYS);
        mapView.setMaxHeight(Double.MAX_VALUE);
        mapView.setOnLocationSelected((name, coords) -> {
            // When user picks a location from map search, fill the destination field
            Platform.runLater(() -> {
                if (tfPickup.getText().trim().isEmpty()) {
                    tfPickup.setText(name);
                } else {
                    tfDestination.setText(name);
                    mapView.setDropoff(coords[0], coords[1], name);
                }
                schedulePriceEstimate();
            });
        });
        mapView.setOnMapClick((lat, lng) -> {
            // Map click sets dropoff if pickup is already set
            if (!tfPickup.getText().trim().isEmpty() && tfDestination.getText().trim().isEmpty()) {
                reverseGeocode(lat, lng, addr -> {
                    tfDestination.setText(addr);
                    mapView.setDropoff(lat, lng, addr);
                    schedulePriceEstimate();
                });
            }
        });

        split.getItems().addAll(bookingWrapper, mapView);
        root.setCenter(split);

        stage.setScene(new Scene(root, 1200, 700));
        stage.setResizable(true);
        stage.show();
        detectAndSuggestLocation();
        startDriverMarkerUpdater();
    }

    /** Reverse geocode lat/lng to a human-readable address using Nominatim. */
    private void reverseGeocode(double lat, double lng, java.util.function.Consumer<String> callback) {
        Thread t = new Thread(() -> {
            try {
                String url = String.format(
                    "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json&zoom=16", lat, lng);
                String json = httpGet(url, true);
                String name = jsonStr(json, "display_name");
                // Shorten to first 2 parts
                String[] parts = name.split(",");
                String short_ = parts.length > 1 ? parts[0].trim() + ", " + parts[1].trim() : name;
                Platform.runLater(() -> callback.accept(short_));
            } catch (Exception e) {
                Platform.runLater(() -> callback.accept(String.format("%.4f, %.4f", lat, lng)));
            }
        }, "reverse-geocode");
        t.setDaemon(true);
        t.start();
    }

    /** Periodically updates driver markers on the map from the server. */
    private void startDriverMarkerUpdater() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(6000);
                    fetchAndUpdateDriverMarkers();
                } catch (InterruptedException e) { break; }
            }
        }, "driver-marker-updater");
        t.setDaemon(true);
        t.start();
        // Initial load
        fetchAndUpdateDriverMarkers();
    }

    private void fetchAndUpdateDriverMarkers() {
        try {
            ServerConnection conn = new ServerConnection();
            conn.connect();
            Message resp = conn.sendAndWait(
                new Message(MessageType.DRIVER_LOCATIONS_REQUEST, null, "passenger"),
                MessageType.DRIVER_LOCATIONS_RESPONSE, 5000);
            conn.close();
            if (resp == null || resp.getType() != MessageType.DRIVER_LOCATIONS_RESPONSE) return;
            String payload = resp.getPayload() != null ? resp.getPayload().toString() : "";
            if (payload.isEmpty()) return;

            Platform.runLater(() -> {
                if (mapView == null) return;
                mapView.clearDriverMarkers();
                for (String entry : payload.split("\\|")) {
                    String[] parts = entry.split(",", 5);
                    if (parts.length < 4) continue;
                    try {
                        String id     = parts[0];
                        double lat    = Double.parseDouble(parts[1]);
                        double lng    = Double.parseDouble(parts[2]);
                        String status = parts[3];
                        String name   = parts.length > 4 ? parts[4] : "Driver";
                        String mapStatus = switch (status) {
                            case "AVAILABLE" -> "ONLINE";
                            case "ON_TRIP"   -> "BUSY";
                            default          -> "OFFLINE";
                        };
                        mapView.addDriverMarker(id, lat, lng, name, mapStatus);
                    } catch (NumberFormatException ignored) {}
                }
            });
        } catch (Exception ignored) {}
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

    // Autocomplete suggestion list for destination
    private VBox destSuggestions;
    private java.util.concurrent.ScheduledFuture<?> pendingSuggest;

    private VBox buildBookingPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(24));
        panel.setStyle("-fx-background-color: #0a0e1a;");

        Label lblTitle = new Label("Where to?");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.web("#f1f5f9"));

        // ── Pickup ────────────────────────────────────────────────────────────
        Label lblPickupLbl = new Label("Pickup Location");
        lblPickupLbl.setTextFill(Color.web("#94a3b8"));
        lblPickupLbl.setFont(Font.font("Arial", 11));

        tfPickup = new TextField();
        tfPickup.setPromptText("Detecting your location...");
        styleField(tfPickup);

        lblLocationHint = new Label("📍 Detecting your location...");
        lblLocationHint.setTextFill(Color.web("#475569"));
        lblLocationHint.setFont(Font.font("Arial", 11));
        lblLocationHint.setVisible(false);
        lblLocationHint.setManaged(false);

        // ── Destination with autocomplete ─────────────────────────────────────
        Label lblDestLbl = new Label("Destination");
        lblDestLbl.setTextFill(Color.web("#94a3b8"));
        lblDestLbl.setFont(Font.font("Arial", 11));

        tfDestination = new TextField();
        tfDestination.setPromptText("Type to search (e.g. Bole, Adama, Piassa)...");
        styleField(tfDestination);

        destSuggestions = new VBox(0);
        destSuggestions.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:0 0 8px 8px;-fx-background-radius:0 0 8px 8px;");
        destSuggestions.setVisible(false);
        destSuggestions.setManaged(false);

        tfDestination.textProperty().addListener((o, ov, nv) -> {
            if (nv.length() >= 2) scheduleDestSuggest(nv);
            else hideSuggestions();
        });

        VBox destBox = new VBox(0, tfDestination, destSuggestions);

        // ── Get Price button ──────────────────────────────────────────────────
        Button btnGetPrice = new Button("🔍  Get Price & Availability");
        btnGetPrice.setMaxWidth(Double.MAX_VALUE);
        btnGetPrice.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btnGetPrice.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;" +
            "-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
        btnGetPrice.setOnAction(e -> triggerPriceEstimate(panel));

        // ── Info cards ────────────────────────────────────────────────────────
        Label lblPrice = new Label("—");
        lblPrice.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblPrice.setTextFill(Color.web("#22c55e"));

        Label lblAvail = new Label("—");
        lblAvail.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblAvail.setTextFill(Color.web("#94a3b8"));

        Label lblDist = new Label("—");
        lblDist.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblDist.setTextFill(Color.web("#3b82f6"));

        HBox infoCards = new HBox(8);
        infoCards.getChildren().addAll(
            infoCard("💰 Price",        lblPrice, "#22c55e"),
            infoCard("🚗 Availability", lblAvail, "#f59e0b"),
            infoCard("📍 Distance",     lblDist,  "#3b82f6")
        );

        // Store card labels in panel for access from triggerPriceEstimate
        panel.setUserData(new Label[]{lblPrice, lblAvail, lblDist});

        // Hidden compat label
        lblPriceEstimate = new Label("");
        lblPriceEstimate.setVisible(false);
        lblPriceEstimate.setManaged(false);

        // ── Category ──────────────────────────────────────────────────────────
        Label lblCat = new Label("Ride Type");
        lblCat.setTextFill(Color.web("#94a3b8"));
        lblCat.setFont(Font.font("Arial", 11));

        HBox categoryRow = buildCategoryRow();

        // ── Request button ────────────────────────────────────────────────────
        btnRequest = new Button("Request Ride");
        btnRequest.setMaxWidth(Double.MAX_VALUE);
        btnRequest.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        btnRequest.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;" +
            "-fx-background-radius:10px;-fx-padding:14px;-fx-cursor:hand;");
        btnRequest.setOnAction(e -> onRequestRide());

        HBox quickRow = buildQuickLocations();

        panel.getChildren().addAll(
            lblTitle,
            lblPickupLbl, tfPickup, lblLocationHint,
            lblDestLbl, destBox,
            btnGetPrice, infoCards,
            lblCat, categoryRow,
            lblPriceEstimate, btnRequest,
            new Separator(), quickRow
        );
        return panel;
    }

    private VBox infoCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:10 12;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#475569")); lbl.setFont(Font.font("Arial", 10));
        card.getChildren().addAll(lbl, valueLabel);
        return card;
    }

    private void scheduleDestSuggest(String query) {
        if (pendingSuggest != null && !pendingSuggest.isDone()) pendingSuggest.cancel(false);
        pendingSuggest = debouncer.schedule(() -> fetchDestSuggestions(query), 400, TimeUnit.MILLISECONDS);
    }

    private void fetchDestSuggestions(String query) {
        Thread t = new Thread(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/search?q=" +
                    java.net.URLEncoder.encode(query + " Ethiopia", StandardCharsets.UTF_8) +
                    "&format=json&limit=5&addressdetails=0";
                String json = httpGet(url, true);
                java.util.List<String> names = new java.util.ArrayList<>();
                java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                while (m.find() && names.size() < 5) {
                    String full = m.group(1);
                    String[] parts = full.split(",");
                    names.add(parts.length > 2 ? parts[0].trim() + ", " + parts[1].trim() : full);
                }
                Platform.runLater(() -> showSuggestions(names));
            } catch (Exception ignored) {}
        }, "dest-suggest");
        t.setDaemon(true);
        t.start();
    }

    private void showSuggestions(java.util.List<String> names) {
        destSuggestions.getChildren().clear();
        if (names.isEmpty()) { hideSuggestions(); return; }
        for (String name : names) {
            Label item = new Label("📍  " + name);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setTextFill(Color.web("#f1f5f9"));
            item.setFont(Font.font("Arial", 12));
            item.setPadding(new Insets(8, 14, 8, 14));
            item.setStyle("-fx-cursor:hand;-fx-border-color:transparent transparent #1e3a5f transparent;" +
                "-fx-border-width:0 0 1 0;");
            item.setOnMouseEntered(e -> item.setStyle("-fx-cursor:hand;-fx-background-color:#1e3a5f;"));
            item.setOnMouseExited(e  -> item.setStyle("-fx-cursor:hand;-fx-border-color:transparent transparent #1e3a5f transparent;-fx-border-width:0 0 1 0;"));
            item.setOnMouseClicked(e -> {
                tfDestination.setText(name);
                hideSuggestions();
                // Find the panel and trigger
                VBox p = findBookingPanel();
                if (p != null) triggerPriceEstimate(p);
            });
            destSuggestions.getChildren().add(item);
        }
        destSuggestions.setVisible(true);
        destSuggestions.setManaged(true);
    }

    private void hideSuggestions() {
        if (destSuggestions != null) {
            destSuggestions.setVisible(false);
            destSuggestions.setManaged(false);
        }
    }

    private void triggerPriceEstimate(VBox panel) {
        hideSuggestions();
        String pickup = tfPickup.getText().trim();
        String dest   = tfDestination.getText().trim();
        if (pickup.isEmpty() || dest.isEmpty()) return;

        Label[] cards = panel.getUserData() instanceof Label[] ? (Label[]) panel.getUserData() : null;
        if (cards != null) {
            cards[0].setText("Calculating..."); cards[0].setTextFill(Color.web("#94a3b8"));
            cards[1].setText("Checking...");
            cards[2].setText("...");
        }

        Thread t = new Thread(() -> {
            try {
                ServerConnection conn = new ServerConnection();
                conn.connect();
                Message response = conn.sendAndWait(
                    new Message(MessageType.PRICE_ESTIMATE_REQUEST,
                        pickup + "|" + dest + "|" + selectedCategory.name(), "passenger"),
                    MessageType.PRICE_ESTIMATE_RESPONSE, 20000);
                conn.close();

                Platform.runLater(() -> {
                    if (response != null && response.getType() == MessageType.PRICE_ESTIMATE_RESPONSE) {
                        currentEstimate = (PriceEstimateDTO) response.getPayload();
                        if (cards != null) {
                            cards[0].setText(String.format("ETB %.2f", currentEstimate.getTotalFare()));
                            cards[0].setTextFill(Color.web("#22c55e"));
                            cards[2].setText(String.format("%.1f km  ~%.0f min",
                                currentEstimate.getDistanceKm(), currentEstimate.getDurationMinutes()));
                            cards[2].setTextFill(Color.web("#3b82f6"));
                        }
                        // Check driver availability via server
                        checkDriverAvailability(cards);
                        // Draw route on map
                        if (mapView != null) {
                            mapView.setPickup(currentEstimate.getOriginLat(), currentEstimate.getOriginLng(), pickup);
                            mapView.setDropoff(currentEstimate.getDestLat(), currentEstimate.getDestLng(), dest);
                            mapView.drawRoute(currentEstimate.getOriginLat(), currentEstimate.getOriginLng(),
                                currentEstimate.getDestLat(), currentEstimate.getDestLng());
                        }
                    } else {
                        String err = response != null ? response.getPayload().toString() : "Timeout";
                        if (cards != null) { cards[0].setText("Failed"); cards[0].setTextFill(Color.web("#ef4444")); }
                        showInfo("Price Error", err);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (cards != null) { cards[0].setText("Server offline"); cards[0].setTextFill(Color.web("#ef4444")); }
                });
            }
        }, "price-estimate-thread");
        t.setDaemon(true);
        t.start();
    }

    private void checkDriverAvailability(Label[] cards) {
        if (cards == null) return;
        Thread t = new Thread(() -> {
            try {
                ServerConnection conn = new ServerConnection();
                conn.connect();
                Message resp = conn.sendAndWait(
                    new Message(MessageType.DRIVER_LOCATIONS_REQUEST, null, "passenger"),
                    MessageType.DRIVER_LOCATIONS_RESPONSE, 5000);
                conn.close();
                int count = 0;
                if (resp != null && resp.getType() == MessageType.DRIVER_LOCATIONS_RESPONSE) {
                    String payload = resp.getPayload() != null ? resp.getPayload().toString() : "";
                    if (!payload.isEmpty()) {
                        for (String e : payload.split("\\|")) {
                            if (e.endsWith("AVAILABLE")) count++;
                        }
                    }
                }
                final int online = count;
                Platform.runLater(() -> {
                    cards[1].setText(online > 0 ? online + " driver(s) available" : "No drivers nearby");
                    cards[1].setTextFill(Color.web(online > 0 ? "#22c55e" : "#ef4444"));
                });
            } catch (Exception ignored) {
                Platform.runLater(() -> { cards[1].setText("Unknown"); cards[1].setTextFill(Color.web("#94a3b8")); });
            }
        }, "avail-check");
        t.setDaemon(true);
        t.start();
    }

    private VBox findBookingPanel() {
        try {
            javafx.scene.control.SplitPane split = (javafx.scene.control.SplitPane)
                ((javafx.scene.layout.BorderPane) stage.getScene().getRoot()).getCenter();
            javafx.scene.layout.VBox wrapper = (javafx.scene.layout.VBox) split.getItems().get(0);
            javafx.scene.layout.StackPane stack = (javafx.scene.layout.StackPane) wrapper.getChildren().get(0);
            return (VBox) stack.getChildren().get(0);
        } catch (Exception e) { return null; }
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
                        // Draw route on map
                        if (mapView != null) {
                            mapView.setPickup(currentEstimate.getOriginLat(), currentEstimate.getOriginLng(), pickup);
                            mapView.setDropoff(currentEstimate.getDestLat(), currentEstimate.getDestLng(), dest);
                            mapView.drawRoute(
                                currentEstimate.getOriginLat(), currentEstimate.getOriginLng(),
                                currentEstimate.getDestLat(),   currentEstimate.getDestLng());
                        }
                    } else if (response != null && response.getType() == MessageType.ERROR) {
                        String err = response.getPayload() != null ? response.getPayload().toString() : "unknown";
                        lblPriceEstimate.setText("Estimate failed: " + err);
                        lblPriceEstimate.setTextFill(Color.web("#ef4444"));
                    } else {
                        lblPriceEstimate.setText("Price estimation unavailable");
                        lblPriceEstimate.setTextFill(Color.web("#ef4444"));
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblPriceEstimate.setText("Price estimation unavailable — " + ex.getMessage());
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
        if (mapView != null) mapView.clearRoute();
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

                String ipStatus = jsonStr(ipJson, "status");
                if (!"success".equals(ipStatus)) {
                    suggestPickup("Addis Ababa, Ethiopia", false);
                    return;
                }

                double lat  = jsonDbl(ipJson, "lat");
                double lon  = jsonDbl(ipJson, "lon");
                String city = jsonStr(ipJson, "city");

                // Step 2 — Nominatim reverse geocode to get a street/area name
                String reverseUrl = String.format(
                    "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json&zoom=16",
                    lat, lon);
                String reverseJson = httpGet(reverseUrl, true);

                String area = firstNonEmpty(
                    jsonStr(reverseJson, "neighbourhood"),
                    jsonStr(reverseJson, "suburb"),
                    jsonStr(reverseJson, "quarter"),
                    jsonStr(reverseJson, "road"),
                    jsonStr(reverseJson, "pedestrian")
                );
                String cityName = firstNonEmpty(
                    jsonStr(reverseJson, "city"),
                    jsonStr(reverseJson, "town"),
                    jsonStr(reverseJson, "village"),
                    city
                );
                String displayName = jsonStr(reverseJson, "display_name");
                String suggestion = area.isEmpty()
                    ? (cityName.isEmpty() ? (displayName.isEmpty() ? "Addis Ababa" : displayName) : cityName)
                    : (cityName.isEmpty() ? area : area + ", " + cityName);

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

    /** Extracts first "key":"value" string from a JSON blob without a library. */
    private static String jsonStr(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    /** Extracts first "key": number from a JSON blob without a library. */
    private static double jsonDbl(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\"" + key + "\"\\s*:\\s*([\\d.eE+\\-]+)").matcher(json);
        try { return m.find() ? Double.parseDouble(m.group(1)) : 0.0; }
        catch (NumberFormatException e) { return 0.0; }
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
