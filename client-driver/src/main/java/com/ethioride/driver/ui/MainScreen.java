package com.ethioride.driver.ui;

import com.ethioride.driver.network.NetworkClient;
import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

public class MainScreen {
    private final Stage stage;
    private Label lblShiftEarnings;
    private ProgressBar earningsProgress;
    private ToggleButton toggleOnline;
    private StackPane requestOverlay;
    private Label lblCountdown;
    private Label lblOverlayFare;
    private Label lblOverlayPickup;
    private Label lblOverlayDropoff;
    private Timeline countdownTimer;
    private TripRequestDTO pendingTrip;

    public MainScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildCenter());
        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
        startPushListener();
        startLocationUpdater();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");

        Label logo = new Label("🚗 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#22c55e"));
        logo.setPadding(new Insets(24, 20, 4, 20));

        Label sub = new Label("Driver Dashboard");
        sub.setFont(Font.font("Arial", 11));
        sub.setTextFill(Color.web("#475569"));
        sub.setPadding(new Insets(0, 20, 20, 20));

        Button btnMap      = navBtn("�  Live Map");
        Button btnHistory  = navBtn("🕐  Ride History");
        Button btnEarnings = navBtn("💰  Earnings");
        Button btnPayments = navBtn("💳  Payments");
        Button btnFleet    = navBtn("🚘  Fleet");
        btnMap.setStyle(btnMap.getStyle() + "-fx-background-color:#1e3a5f;");

        btnHistory.setOnAction(e  -> new RideHistoryScreen(stage).show());
        btnEarnings.setOnAction(e -> new EarningsScreen(stage).show());
        btnPayments.setOnAction(e -> new PaymentsScreen(stage).show());
        btnFleet.setOnAction(e    -> new FleetScreen(stage).show());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        String name = DriverSessionState.getInstance().getCurrentDriver() != null
            ? DriverSessionState.getInstance().getCurrentDriver().getFullName() : "Driver";
        Label lblName = new Label("👤 " + name);
        lblName.setTextFill(Color.web("#f1f5f9"));
        lblName.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblName.setPadding(new Insets(8, 20, 2, 20));

        Button btnSignOut = navBtn("↩  Sign Out");
        btnSignOut.setOnAction(e -> onSignOut());

        sidebar.getChildren().addAll(logo, sub, btnMap, btnHistory, btnEarnings, btnPayments, btnFleet,
                spacer, lblName, btnSignOut);
        return sidebar;
    }

    // ── Center ────────────────────────────────────────────────────────────────

    private StackPane buildCenter() {
        StackPane center = new StackPane();
        center.setStyle("-fx-background-color:#0d1526;");

        Label mapLabel = new Label("[ Live Map Area ]");
        mapLabel.setTextFill(Color.web("#1e3a5f"));
        mapLabel.setFont(Font.font("Arial", 18));

        VBox statusPanel = buildStatusPanel();
        StackPane.setAlignment(statusPanel, Pos.TOP_LEFT);
        StackPane.setMargin(statusPanel, new Insets(20));

        requestOverlay = buildRequestOverlay();
        requestOverlay.setVisible(false);
        requestOverlay.setManaged(false);
        // Stretch overlay to fill the entire center area
        StackPane.setAlignment(requestOverlay, Pos.CENTER);

        center.getChildren().addAll(mapLabel, statusPanel, requestOverlay);

        // Make overlay fill the StackPane by binding its size
        requestOverlay.prefWidthProperty().bind(center.widthProperty());
        requestOverlay.prefHeightProperty().bind(center.heightProperty());

        return center;
    }

    // ── Status label (shown below the toggle) ────────────────────────────────
    private Label lblDriverStatus;

    private VBox buildStatusPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:12px;" +
                "-fx-background-radius:12px;-fx-padding:16;");
        panel.setMaxWidth(260);

        toggleOnline = new ToggleButton("OFFLINE ○");
        toggleOnline.setMaxWidth(Double.MAX_VALUE);
        toggleOnline.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        toggleOnline.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#94a3b8;" +
                "-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
        toggleOnline.setOnAction(e -> {
            boolean online = toggleOnline.isSelected();
            DriverSessionState.getInstance().setOnline(online);
            toggleOnline.setText(online ? "ONLINE ●" : "OFFLINE ○");
            toggleOnline.setStyle(online
                ? "-fx-background-color:#166534;-fx-text-fill:#22c55e;-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;"
                : "-fx-background-color:#1e3a5f;-fx-text-fill:#94a3b8;-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
            if (lblDriverStatus != null)
                lblDriverStatus.setText(online ? "Waiting for ride requests..." : "Go online to receive trips");
            sendStatusUpdate(online);
        });

        // ── Restore persisted online/offline state on navigation back ─────────
        // DriverSessionState holds the real state; the new ToggleButton starts
        // unselected by default, so we sync it here without sending a server update.
        boolean wasOnline = DriverSessionState.getInstance().isOnline();
        toggleOnline.setSelected(wasOnline);
        if (wasOnline) {
            toggleOnline.setText("ONLINE ●");
            toggleOnline.setStyle("-fx-background-color:#166534;-fx-text-fill:#22c55e;" +
                    "-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
        }

        // ── Shift earnings ────────────────────────────────────────────────────
        Label lblEarningsTitle = new Label("Shift Earnings");
        lblEarningsTitle.setTextFill(Color.web("#94a3b8"));
        lblEarningsTitle.setFont(Font.font("Arial", 12));

        double savedEarnings = DriverSessionState.getInstance().getShiftEarnings();
        lblShiftEarnings = new Label(savedEarnings > 0
                ? String.format("ETB %.0f", savedEarnings) : "ETB 0");
        lblShiftEarnings.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblShiftEarnings.setTextFill(Color.web("#22c55e"));

        earningsProgress = new ProgressBar(savedEarnings > 0
                ? Math.min(savedEarnings / 5000.0, 1.0) : 0);
        earningsProgress.setMaxWidth(Double.MAX_VALUE);
        earningsProgress.setPrefHeight(6);

        // ── Driver status hint ────────────────────────────────────────────────
        lblDriverStatus = new Label(DriverSessionState.getInstance().isOnline()
                ? "Waiting for ride requests..." : "Go online to receive trips");
        lblDriverStatus.setTextFill(Color.web("#475569"));
        lblDriverStatus.setFont(Font.font("Arial", 11));
        lblDriverStatus.setWrapText(true);

        panel.getChildren().addAll(toggleOnline, lblEarningsTitle, lblShiftEarnings, earningsProgress, lblDriverStatus);
        return panel;
    }

    private StackPane buildRequestOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.7);");

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(340);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#3b82f6;" +
                "-fx-border-radius:16px;-fx-background-radius:16px;-fx-padding:24;");

        lblCountdown = new Label("11s");
        lblCountdown.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblCountdown.setTextFill(Color.web("#f59e0b"));

        Label lblTitle = new Label("New Ride Request");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.web("#f1f5f9"));

        lblOverlayFare = new Label("ETB —");
        lblOverlayFare.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblOverlayFare.setTextFill(Color.web("#22c55e"));

        lblOverlayPickup = new Label("Pickup: —");
        lblOverlayPickup.setTextFill(Color.web("#94a3b8"));
        lblOverlayPickup.setFont(Font.font("Arial", 13));

        lblOverlayDropoff = new Label("Drop-off: —");
        lblOverlayDropoff.setTextFill(Color.web("#94a3b8"));
        lblOverlayDropoff.setFont(Font.font("Arial", 13));

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);
        Button btnAccept = new Button("Accept");
        btnAccept.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnAccept.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
                "-fx-background-radius:8px;-fx-padding:12 24;-fx-cursor:hand;");
        btnAccept.setOnAction(e -> onAccept());

        Button btnDecline = new Button("Decline");
        btnDecline.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnDecline.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" +
                "-fx-background-radius:8px;-fx-padding:12 24;-fx-cursor:hand;");
        btnDecline.setOnAction(e -> dismissRequest(true));

        buttons.getChildren().addAll(btnAccept, btnDecline);
        card.getChildren().addAll(lblCountdown, lblTitle, lblOverlayFare,
                lblOverlayPickup, lblOverlayDropoff, buttons);
        overlay.getChildren().add(card);
        return overlay;
    }

    // ── Push listener ─────────────────────────────────────────────────────────

    /**
     * Registers a push handler on the persistent NetworkClient connection.
     * When the server sends MATCH_NOTIFY_DRIVER, the trip popup is shown.
     * Also connects to the server so push messages can be received.
     */
    private void startPushListener() {
        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.setPushHandler(msg -> {
                    switch (msg.getType()) {
                        case MATCH_NOTIFY_DRIVER -> {
                            if (msg.getPayload() instanceof TripRequestDTO trip) {
                                Platform.runLater(() -> showTripRequest(trip));
                            }
                        }
                        case TRIP_CANCELLED -> {
                            // Passenger cancelled while driver had the trip
                            Platform.runLater(() -> onTripCancelledByPassenger());
                        }
                        default -> {} // ignore other push messages
                    }
                });
            } catch (Exception ex) {
                // push listener connection failed — ignore silently
            }
        }, "driver-push-connect");
        t.setDaemon(true);
        t.start();
    }

    /** Called when the server pushes TRIP_CANCELLED (passenger cancelled). */
    private void onTripCancelledByPassenger() {
        // Remove any active trip card from the center StackPane
        if (stage.getScene() != null &&
                stage.getScene().getRoot() instanceof BorderPane bp &&
                bp.getCenter() instanceof StackPane sp) {
            sp.getChildren().removeIf(n -> n instanceof VBox v &&
                    v.getStyle().contains("#22c55e"));
        }
        // Reset driver status label
        if (lblDriverStatus != null) {
            lblDriverStatus.setText("Waiting for ride requests...");
            lblDriverStatus.setTextFill(Color.web("#475569"));
        }
        DriverSessionState.getInstance().setOnline(true);

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "The passenger has cancelled the trip.", ButtonType.OK);
        alert.setTitle("Trip Cancelled");
        alert.setHeaderText("Trip Cancelled by Passenger");
        alert.showAndWait();
    }

    private void showTripRequest(TripRequestDTO trip) {
        pendingTrip = trip;
        lblOverlayFare.setText(String.format("ETB %.2f", trip.getFare()));
        lblOverlayPickup.setText("📍  " + trip.getPickupLocation());
        lblOverlayDropoff.setText("🏁  " + trip.getDropoffLocation());
        if (lblDriverStatus != null) lblDriverStatus.setText("⚡ Incoming ride request!");

        requestOverlay.setVisible(true);
        requestOverlay.setManaged(true);
        startCountdown();
    }

    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
        final int[] seconds = {11};
        lblCountdown.setText(seconds[0] + "s");
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds[0]--;
            lblCountdown.setText(seconds[0] + "s");
            if (seconds[0] <= 0) dismissRequest(true);
        }));
        countdownTimer.setCycleCount(11);
        countdownTimer.play();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void sendStatusUpdate(boolean online) {
        if (DriverSessionState.getInstance().getCurrentDriver() == null) return;
        final String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();
        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.send(new Message(MessageType.DRIVER_STATUS_UPDATE,
                        online ? "ONLINE" : "OFFLINE", driverId));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    // status update failed — ignore silently
                });
            }
        }, "driver-status-update");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Sends the driver's GPS location to the server every 15 seconds while online.
     * The server uses this to sort drivers by proximity to the passenger's pickup.
     *
     * In a real app this would use the device GPS. Here we simulate a location
     * near Addis Ababa with a small random offset so multiple drivers appear
     * at different positions during testing.
     */
    private void startLocationUpdater() {
        if (DriverSessionState.getInstance().getCurrentDriver() == null) return;
        final String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();

        // Simulate a location near Addis Ababa center (9.0320, 38.7469)
        // with a small random offset per driver so they appear at different spots
        double baseLat = 9.0320 + (Math.random() - 0.5) * 0.05;
        double baseLng = 38.7469 + (Math.random() - 0.5) * 0.05;

        java.util.concurrent.ScheduledExecutorService locScheduler =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "driver-location-updater");
                t.setDaemon(true);
                return t;
            });

        locScheduler.scheduleAtFixedRate(() -> {
            if (!DriverSessionState.getInstance().isOnline()) return;
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) return;
                // Small drift each cycle to simulate movement
                double lat = baseLat + (Math.random() - 0.5) * 0.002;
                double lng = baseLng + (Math.random() - 0.5) * 0.002;
                nc.send(new Message(MessageType.DRIVER_LOCATION_UPDATE,
                        String.format("%.6f,%.6f", lat, lng), driverId));
            } catch (Exception ex) {
                // location update failed — ignore silently
            }
        }, 5, 15, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void onAccept() {
        if (countdownTimer != null) countdownTimer.stop();
        if (pendingTrip == null) return;
        // Guard: must have a valid driver session
        if (DriverSessionState.getInstance().getCurrentDriver() == null) {
            dismissRequest(false);
            return;
        }

        final TripRequestDTO trip = pendingTrip;
        final String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();

        // Send TRIP_ACCEPTED to server — server updates DB and notifies passenger
        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.send(new Message(MessageType.TRIP_ACCEPTED, trip.getTripId(), driverId));
            } catch (Exception ex) {
                // accept send failed — ignore silently
            }
        }, "trip-accept");
        t.setDaemon(true);
        t.start();

        // NOTE: earnings are added when the trip is COMPLETED, not when accepted
        dismissRequest(false);
        showActiveTrip(trip, driverId);
    }

    /**
     * Shows the active trip panel with STARTED and COMPLETED buttons.
     * Replaces the request overlay after the driver accepts.
     * Full lifecycle: Accepted → Picked Up → In Progress → Completed.
     */
    private void showActiveTrip(TripRequestDTO trip, String driverId) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#22c55e;" +
                "-fx-border-radius:14px;-fx-background-radius:14px;-fx-padding:20;");

        // ── Status badge ──────────────────────────────────────────────────────
        Label lblStatus = new Label("● Accepted — Heading to pickup");
        lblStatus.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblStatus.setTextFill(Color.web("#22c55e"));
        lblStatus.setWrapText(true);

        // ── Trip details ──────────────────────────────────────────────────────
        Label lblTitle = new Label("Active Trip");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        lblTitle.setTextFill(Color.web("#f1f5f9"));

        Label lblPickup = new Label("📍 " + trip.getPickupLocation());
        lblPickup.setTextFill(Color.web("#f1f5f9"));
        lblPickup.setFont(Font.font("Arial", 12));
        lblPickup.setWrapText(true);

        Label lblDropoff = new Label("🏁 " + trip.getDropoffLocation());
        lblDropoff.setTextFill(Color.web("#94a3b8"));
        lblDropoff.setFont(Font.font("Arial", 12));
        lblDropoff.setWrapText(true);

        Label lblDist = new Label(String.format("📏 %.1f km", trip.getDistanceKm()));
        lblDist.setTextFill(Color.web("#475569"));
        lblDist.setFont(Font.font("Arial", 11));

        Label lblFare = new Label(String.format("ETB %.2f", trip.getFare()));
        lblFare.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblFare.setTextFill(Color.web("#22c55e"));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#1e3a5f;");

        // ── Action buttons ────────────────────────────────────────────────────
        Button btnPickedUp = new Button("✔  Picked Up Passenger");
        btnPickedUp.setMaxWidth(Double.MAX_VALUE);
        btnPickedUp.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btnPickedUp.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:#0a0e1a;" +
                "-fx-background-radius:8px;-fx-padding:10;-fx-cursor:hand;");

        Button btnComplete = new Button("🏁  Complete Trip");
        btnComplete.setMaxWidth(Double.MAX_VALUE);
        btnComplete.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btnComplete.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
                "-fx-background-radius:8px;-fx-padding:10;-fx-cursor:hand;");
        btnComplete.setDisable(true); // only enabled after passenger is picked up

        // ── Picked Up ─────────────────────────────────────────────────────────
        btnPickedUp.setOnAction(e -> {
            btnPickedUp.setDisable(true);
            btnComplete.setDisable(false);
            lblStatus.setText("● In Progress — Trip underway");
            lblStatus.setTextFill(Color.web("#f59e0b"));
            lblTitle.setText("Trip In Progress");

            Thread ts = new Thread(() -> {
                try {
                    NetworkClient.getInstance().send(
                        new Message(MessageType.TRIP_STARTED, trip.getTripId(), driverId));
                } catch (Exception ex) {
                    // trip started send failed — ignore silently
                }
            }, "trip-started");
            ts.setDaemon(true);
            ts.start();
        });

        // ── Complete ──────────────────────────────────────────────────────────
        btnComplete.setOnAction(e -> {
            // Update shift earnings display
            DriverSessionState.getInstance().addEarnings(trip.getFare());
            double earnings = DriverSessionState.getInstance().getShiftEarnings();
            lblShiftEarnings.setText(String.format("ETB %.0f", earnings));
            earningsProgress.setProgress(Math.min(earnings / 5000.0, 1.0));

            // Send TRIP_COMPLETED to server
            Thread tc = new Thread(() -> {
                try {
                    NetworkClient.getInstance().send(
                        new Message(MessageType.TRIP_COMPLETED, trip.getTripId(), driverId));
                } catch (Exception ex) {
                    // trip completed send failed — ignore silently
                }
            }, "trip-completed");
            tc.setDaemon(true);
            tc.start();

            // Replace card content with a completion summary
            card.getChildren().clear();
            Label lblDone = new Label("✅  Trip Completed");
            lblDone.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            lblDone.setTextFill(Color.web("#22c55e"));

            Label lblEarned = new Label("You earned  " + String.format("ETB %.2f", trip.getFare()));
            lblEarned.setFont(Font.font("Arial", 13));
            lblEarned.setTextFill(Color.web("#f1f5f9"));

            Label lblShift = new Label(String.format("Shift total: ETB %.0f", earnings));
            lblShift.setFont(Font.font("Arial", 11));
            lblShift.setTextFill(Color.web("#475569"));

            Button btnDismiss = new Button("Dismiss");
            btnDismiss.setMaxWidth(Double.MAX_VALUE);
            btnDismiss.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;" +
                    "-fx-background-radius:8px;-fx-padding:8;-fx-cursor:hand;");
            btnDismiss.setOnAction(ev -> {
                if (card.getParent() instanceof StackPane sp) sp.getChildren().remove(card);
                if (lblDriverStatus != null) {
                    lblDriverStatus.setText("Waiting for ride requests...");
                    lblDriverStatus.setTextFill(Color.web("#475569"));
                }
            });

            card.getChildren().addAll(lblDone, lblEarned, lblShift, btnDismiss);
            card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#22c55e;" +
                    "-fx-border-radius:14px;-fx-background-radius:14px;-fx-padding:20;");
        });

        card.getChildren().addAll(lblStatus, lblTitle, lblPickup, lblDropoff, lblDist, lblFare, sep, btnPickedUp, btnComplete);

        // Add to the center StackPane
        Platform.runLater(() -> {
            if (lblDriverStatus != null) {
                lblDriverStatus.setText("● On a trip");
                lblDriverStatus.setTextFill(Color.web("#22c55e"));
            }
            if (stage.getScene().getRoot() instanceof BorderPane bp &&
                    bp.getCenter() instanceof StackPane sp) {
                StackPane.setAlignment(card, Pos.BOTTOM_LEFT);
                StackPane.setMargin(card, new Insets(0, 0, 20, 20));
                sp.getChildren().add(card);
            }
        });
    }

    private void dismissRequest(boolean sendDecline) {
        if (countdownTimer != null) countdownTimer.stop();
        if (sendDecline && pendingTrip != null
                && DriverSessionState.getInstance().getCurrentDriver() != null) {
            final String tripId   = pendingTrip.getTripId();
            final String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();
            Thread t = new Thread(() -> {
                try {
                    NetworkClient nc = NetworkClient.getInstance();
                    if (nc.isConnected()) {
                        nc.send(new Message(MessageType.TRIP_DECLINED, tripId, driverId));
                    }
                } catch (Exception ex) {
                    // decline send failed — ignore silently
                }
            }, "trip-decline");
            t.setDaemon(true);
            t.start();

            // Show declined state briefly, then reset to waiting
            if (lblDriverStatus != null) {
                lblDriverStatus.setText("✕  Request declined — waiting for next trip...");
                lblDriverStatus.setTextFill(Color.web("#ef4444"));
                // Reset colour after 3 seconds
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> {
                        if (lblDriverStatus != null) {
                            lblDriverStatus.setText("Waiting for ride requests...");
                            lblDriverStatus.setTextFill(Color.web("#475569"));
                        }
                    });
                }, "decline-reset").start();
            }
        } else {
            if (lblDriverStatus != null) {
                lblDriverStatus.setText("Waiting for ride requests...");
                lblDriverStatus.setTextFill(Color.web("#475569"));
            }
        }
        pendingTrip = null;
        requestOverlay.setVisible(false);
        requestOverlay.setManaged(false);
    }

    private void onSignOut() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "End your shift and sign out?", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Sign Out");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                sendStatusUpdate(false);
                DriverSessionState.getInstance().clear();
                NetworkClient.getInstance().close();
                new LoginScreen(stage).show();
            }
        });
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;" +
                "-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
