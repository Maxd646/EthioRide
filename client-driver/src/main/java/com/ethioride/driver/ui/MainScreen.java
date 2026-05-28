package com.ethioride.driver.ui;

import com.ethioride.driver.network.NetworkClient;
import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.TripStatus;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Driver main screen — shows live trip list.
 *
 * Trip cards are sorted newest-first. Active trips (PENDING / ACCEPTED / IN_PROGRESS)
 * appear at the very top with inline action buttons:
 *
 *   PENDING     → Accept  |  Decline
 *   ACCEPTED    → Picked Up Passenger
 *   IN_PROGRESS → Complete Trip
 *   COMPLETED / CANCELLED → read-only history card
 *
 * No popup overlay — everything is inline in the list.
 */
public class MainScreen {

    private final Stage stage;

    // Status panel widgets
    private ToggleButton toggleOnline;
    private Label        lblShiftEarnings;
    private ProgressBar  earningsProgress;
    private Label        lblDriverStatus;

    // Trip list
    private VBox         tripListContainer;
    private List<TripRequestDTO> trips = new ArrayList<>();

    // Auto-poll scheduler — refreshes trip list every 5s while screen is open
    private final ScheduledExecutorService poller =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "driver-trip-poller");
            t.setDaemon(true);
            return t;
        });
    private ScheduledFuture<?> pollTask;

    public MainScreen(Stage stage) { this.stage = stage; }

    private MapView mapView;

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());

        // Split: trip list (left) + live map (right)
        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color:#0a0e1a;-fx-box-border:transparent;");
        split.setDividerPositions(0.42);

        split.getItems().add(buildContent());

        mapView = new MapView();
        // Center map on Addis Ababa by default
        split.getItems().add(mapView);

        root.setCenter(split);
        stage.setScene(new Scene(root, 1200, 700));
        stage.setResizable(true);
        stage.show();
        startPushListener();
        startLocationUpdater();
        loadTrips();
        startAutoRefresh();
    }

    /** Polls the server every 5 seconds to catch any trips missed by the push listener. */
    private void startAutoRefresh() {
        if (pollTask != null && !pollTask.isDone()) pollTask.cancel(false);
        pollTask = poller.scheduleAtFixedRate(() -> {
            // Only poll while the scene is still showing this screen
            if (stage.getScene() == null) { pollTask.cancel(false); return; }
            loadTrips();
        }, 5, 5, TimeUnit.SECONDS);
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

        Button btnMap      = navBtn("🗺  Live Map");
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

        sidebar.getChildren().addAll(logo, sub, btnMap, btnHistory, btnEarnings,
                btnPayments, btnFleet, spacer, lblName, btnSignOut);
        return sidebar;
    }

    // ── Main content ──────────────────────────────────────────────────────────

    private ScrollPane buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:#0a0e1a;");

        // ── Status panel ──────────────────────────────────────────────────────
        VBox statusPanel = buildStatusPanel();

        // ── Section header ────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Trips");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblTitle.setTextFill(Color.web("#f1f5f9"));
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button btnRefresh = new Button("↻ Refresh");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;" +
                "-fx-background-radius:6px;-fx-padding:6 14;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadTrips());
        header.getChildren().addAll(lblTitle, hSpacer, btnRefresh);

        // ── Trip list ─────────────────────────────────────────────────────────
        tripListContainer = new VBox(10);

        content.getChildren().addAll(statusPanel, header, tripListContainer);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private VBox buildStatusPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");

        // Online toggle
        toggleOnline = new ToggleButton("OFFLINE ○");
        toggleOnline.setMaxWidth(Double.MAX_VALUE);
        toggleOnline.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        toggleOnline.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#94a3b8;" +
                "-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
        toggleOnline.setOnAction(e -> {
            boolean online = toggleOnline.isSelected();
            DriverSessionState.getInstance().setOnline(online);
            applyToggleStyle(online);
            lblDriverStatus.setText(online ? "Waiting for ride requests..." : "Go online to receive trips");
            lblDriverStatus.setTextFill(Color.web(online ? "#475569" : "#94a3b8"));
            sendStatusUpdate(online);
        });

        // Restore persisted state
        boolean wasOnline = DriverSessionState.getInstance().isOnline();
        toggleOnline.setSelected(wasOnline);
        applyToggleStyle(wasOnline);

        // Earnings row
        HBox earningsRow = new HBox(16);
        earningsRow.setAlignment(Pos.CENTER_LEFT);

        VBox earningsBox = new VBox(2);
        Label lblEarningsTitle = new Label("Shift Earnings");
        lblEarningsTitle.setTextFill(Color.web("#94a3b8"));
        lblEarningsTitle.setFont(Font.font("Arial", 11));
        double saved = DriverSessionState.getInstance().getShiftEarnings();
        lblShiftEarnings = new Label(saved > 0 ? String.format("ETB %.0f", saved) : "ETB 0");
        lblShiftEarnings.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblShiftEarnings.setTextFill(Color.web("#22c55e"));
        earningsBox.getChildren().addAll(lblEarningsTitle, lblShiftEarnings);

        earningsProgress = new ProgressBar(saved > 0 ? Math.min(saved / 5000.0, 1.0) : 0);
        earningsProgress.setMaxWidth(Double.MAX_VALUE);
        earningsProgress.setPrefHeight(5);
        HBox.setHgrow(earningsProgress, Priority.ALWAYS);
        earningsRow.getChildren().addAll(earningsBox, earningsProgress);

        // Status hint
        lblDriverStatus = new Label(wasOnline ? "Waiting for ride requests..." : "Go online to receive trips");
        lblDriverStatus.setTextFill(Color.web("#475569"));
        lblDriverStatus.setFont(Font.font("Arial", 11));

        panel.getChildren().addAll(toggleOnline, earningsRow, lblDriverStatus);
        return panel;
    }

    private void applyToggleStyle(boolean online) {
        toggleOnline.setText(online ? "ONLINE ●" : "OFFLINE ○");
        toggleOnline.setStyle(online
            ? "-fx-background-color:#166534;-fx-text-fill:#22c55e;-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;"
            : "-fx-background-color:#1e3a5f;-fx-text-fill:#94a3b8;-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
    }

    // ── Trip card rendering ───────────────────────────────────────────────────

    /**
     * Renders the full trip list. Active trips (PENDING/ACCEPTED/IN_PROGRESS)
     * are sorted to the top; completed/cancelled go below.
     */
    private void renderTrips() {
        tripListContainer.getChildren().clear();

        if (trips.isEmpty()) {
            Label empty = new Label("No trips yet. Go online to start receiving requests.");
            empty.setTextFill(Color.web("#475569"));
            empty.setFont(Font.font("Arial", 13));
            tripListContainer.getChildren().add(empty);
            return;
        }

        // Active trips first, then history
        List<TripRequestDTO> active = trips.stream()
            .filter(t -> t.getStatus() == TripStatus.PENDING
                      || t.getStatus() == TripStatus.ACCEPTED
                      || t.getStatus() == TripStatus.IN_PROGRESS)
            .toList();
        List<TripRequestDTO> history = trips.stream()
            .filter(t -> t.getStatus() == TripStatus.COMPLETED
                      || t.getStatus() == TripStatus.CANCELLED)
            .toList();

        for (TripRequestDTO t : active)  tripListContainer.getChildren().add(buildActiveTripCard(t));
        for (TripRequestDTO t : history) tripListContainer.getChildren().add(buildHistoryCard(t));
    }

    /** Card for PENDING / ACCEPTED / IN_PROGRESS trips — shows action buttons. */
    private VBox buildActiveTripCard(TripRequestDTO trip) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-radius:12px;" +
                "-fx-background-radius:12px;-fx-padding:16;");

        // Status badge colour
        String badgeColor = switch (trip.getStatus()) {
            case PENDING     -> "#f59e0b";
            case ACCEPTED    -> "#3b82f6";
            case IN_PROGRESS -> "#22c55e";
            default          -> "#94a3b8";
        };
        card.setStyle(card.getStyle() + "-fx-border-color:" + badgeColor + ";");

        // Header row: status badge + fare
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lblBadge = new Label("● " + trip.getStatus().name().replace("_", " "));
        lblBadge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        lblBadge.setTextFill(Color.web(badgeColor));
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Label lblFare = new Label(String.format("ETB %.2f", trip.getFare()));
        lblFare.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblFare.setTextFill(Color.web("#22c55e"));
        topRow.getChildren().addAll(lblBadge, hSpacer, lblFare);

        // Route
        Label lblRoute = new Label(trip.getPickupLocation() + "  →  " + trip.getDropoffLocation());
        lblRoute.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblRoute.setTextFill(Color.web("#f1f5f9"));
        lblRoute.setWrapText(true);

        // Meta
        String passenger = trip.getPassengerName() != null ? trip.getPassengerName() : "Passenger";
        Label lblMeta = new Label(String.format("%.1f km  •  %s", trip.getDistanceKm(), passenger));
        lblMeta.setFont(Font.font("Arial", 11));
        lblMeta.setTextFill(Color.web("#475569"));

        card.getChildren().addAll(topRow, lblRoute, lblMeta);

        // ── Action buttons depending on status ────────────────────────────────
        String driverId = DriverSessionState.getInstance().getCurrentDriver() != null
                ? DriverSessionState.getInstance().getCurrentDriver().getId() : "";

        if (trip.getStatus() == TripStatus.PENDING) {
            // Accept / Decline
            Button btnAccept = actionBtn("✔  Accept", "#22c55e", "white");
            Button btnDecline = actionBtn("✕  Decline", "#7f1d1d", "#fca5a5");

            btnAccept.setOnAction(e -> {
                sendTripMessage(MessageType.TRIP_ACCEPTED, trip.getTripId(), driverId);
                trip.setStatus(TripStatus.ACCEPTED);
                trip.setDriverId(driverId);
                lblDriverStatus.setText("● On a trip");
                lblDriverStatus.setTextFill(Color.web("#22c55e"));
                renderTrips();
            });
            btnDecline.setOnAction(e -> {
                sendTripMessage(MessageType.TRIP_DECLINED, trip.getTripId(), driverId);
                trips.remove(trip);
                lblDriverStatus.setText("Waiting for ride requests...");
                lblDriverStatus.setTextFill(Color.web("#475569"));
                renderTrips();
            });

            HBox btns = new HBox(10, btnAccept, btnDecline);
            btns.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(btns);

        } else if (trip.getStatus() == TripStatus.ACCEPTED) {
            // Picked Up Passenger
            Button btnPickedUp = actionBtn("✔  Picked Up Passenger", "#f59e0b", "#0a0e1a");
            btnPickedUp.setOnAction(e -> {
                sendTripMessage(MessageType.TRIP_STARTED, trip.getTripId(), driverId);
                trip.setStatus(TripStatus.IN_PROGRESS);
                renderTrips();
            });
            card.getChildren().add(btnPickedUp);

        } else if (trip.getStatus() == TripStatus.IN_PROGRESS) {
            // Complete Trip
            Button btnComplete = actionBtn("🏁  Complete Trip", "#22c55e", "white");
            btnComplete.setOnAction(e -> {
                sendTripMessage(MessageType.TRIP_COMPLETED, trip.getTripId(), driverId);
                trip.setStatus(TripStatus.COMPLETED);
                // Update earnings
                DriverSessionState.getInstance().addEarnings(trip.getFare());
                double earnings = DriverSessionState.getInstance().getShiftEarnings();
                lblShiftEarnings.setText(String.format("ETB %.0f", earnings));
                earningsProgress.setProgress(Math.min(earnings / 5000.0, 1.0));
                lblDriverStatus.setText("Waiting for ride requests...");
                lblDriverStatus.setTextFill(Color.web("#475569"));
                renderTrips();
            });
            card.getChildren().add(btnComplete);
        }

        return card;
    }

    /** Read-only card for COMPLETED / CANCELLED trips. */
    private HBox buildHistoryCard(TripRequestDTO t) {
        boolean completed = t.getStatus() == TripStatus.COMPLETED;

        HBox card = new HBox(16);
        card.setStyle("-fx-background-color:#1a2235;-fx-background-radius:12px;-fx-padding:14;");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        StackPane icon = new StackPane();
        icon.setStyle("-fx-background-color:#1e3a5f;-fx-background-radius:50%;" +
                "-fx-min-width:40px;-fx-min-height:40px;");
        Label ico = new Label(completed ? "✓" : "✕");
        ico.setStyle((completed ? "-fx-text-fill:#22c55e;" : "-fx-text-fill:#ef4444;") +
                "-fx-font-size:14px;-fx-font-weight:bold;");
        icon.getChildren().add(ico);

        // Route + meta
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label route = new Label(t.getPickupLocation() + "  →  " + t.getDropoffLocation());
        route.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:12px;-fx-font-weight:bold;");
        route.setWrapText(true);
        String passenger = t.getPassengerName() != null ? t.getPassengerName() : "Passenger";
        Label meta = new Label(String.format("%.1f km  •  %s", t.getDistanceKm(), passenger));
        meta.setStyle("-fx-text-fill:#475569;-fx-font-size:10px;");
        info.getChildren().addAll(route, meta);

        // Fare + status
        VBox right = new VBox(3);
        right.setAlignment(Pos.CENTER_RIGHT);
        Label fare = new Label(String.format("ETB %.2f", t.getFare()));
        fare.setStyle("-fx-text-fill:#22c55e;-fx-font-size:13px;-fx-font-weight:bold;");
        Label status = new Label(t.getStatus().name());
        status.setStyle((completed ? "-fx-text-fill:#22c55e;" : "-fx-text-fill:#ef4444;") +
                "-fx-font-size:10px;");
        right.getChildren().addAll(fare, status);

        card.getChildren().addAll(icon, info, right);
        return card;
    }

    private Button actionBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-background-radius:8px;-fx-padding:8 18;-fx-cursor:hand;");
        return btn;
    }

    // ── Network ───────────────────────────────────────────────────────────────

    /** Load trips from server. Merges with local state to avoid overwriting in-progress actions. */
    private void loadTrips() {
        if (DriverSessionState.getInstance().getCurrentDriver() == null) return;
        final String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();

        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.sendRequest(
                    MessageType.DRIVER_TRIP_HISTORY_REQUEST, driverId,
                    MessageType.DRIVER_TRIP_HISTORY_RESPONSE, msg -> {
                        @SuppressWarnings("unchecked")
                        List<TripRequestDTO> loaded = (List<TripRequestDTO>) msg.getPayload();
                        Platform.runLater(() -> {
                            // Merge: keep local status if driver has already acted on a trip
                            // Exception: CANCELLED always wins — passenger cancelled, no action possible
                            for (TripRequestDTO incoming : loaded) {
                                trips.stream()
                                    .filter(local -> local.getTripId().equals(incoming.getTripId()))
                                    .findFirst()
                                    .ifPresentOrElse(
                                        local -> {
                                            // CANCELLED always overrides local state
                                            if (incoming.getStatus() == TripStatus.CANCELLED) {
                                                local.setStatus(TripStatus.CANCELLED);
                                            } else if (local.getStatus() == incoming.getStatus()) {
                                                // Same status — just refresh display name
                                                local.setPassengerName(incoming.getPassengerName());
                                            }
                                            // If local is more advanced, keep local
                                        },
                                        () -> trips.add(0, incoming) // new trip — add at top
                                    );
                            }
                            // Remove trips that no longer exist on server
                            trips.removeIf(local -> loaded.stream()
                                .noneMatch(s -> s.getTripId().equals(local.getTripId())));

                            // Update status label if there are active trips
                            boolean hasActive = trips.stream().anyMatch(tr ->
                                tr.getStatus() == TripStatus.PENDING ||
                                tr.getStatus() == TripStatus.ACCEPTED ||
                                tr.getStatus() == TripStatus.IN_PROGRESS);
                            if (hasActive && lblDriverStatus != null) {
                                lblDriverStatus.setText("⚡ You have active trips — see below");
                                lblDriverStatus.setTextFill(Color.web("#f59e0b"));
                            }
                            renderTrips();
                        });
                    });
            } catch (Exception e) {
                // load failed — ignore silently
            }
        }, "driver-trip-load");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Listens for server-pushed MATCH_NOTIFY_DRIVER messages.
     * When a new trip arrives it is inserted at the top of the list — no popup.
     */
    private void startPushListener() {
        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.setPushHandler(msg -> {
                    switch (msg.getType()) {
                        case MATCH_NOTIFY_DRIVER -> {
                            if (msg.getPayload() instanceof TripRequestDTO incoming) {
                                Platform.runLater(() -> {
                                    // Remove any existing entry for this trip (avoid duplicates)
                                    trips.removeIf(tr -> tr.getTripId().equals(incoming.getTripId()));
                                    // Insert at top so it's the first thing the driver sees
                                    trips.add(0, incoming);
                                    lblDriverStatus.setText("⚡ New ride request — see below");
                                    lblDriverStatus.setTextFill(Color.web("#f59e0b"));
                                    renderTrips();
                                });
                            }
                        }
                        case TRIP_CANCELLED -> {
                            // Passenger cancelled — update the matching trip in the list
                            Platform.runLater(() -> {
                                String cancelledId = msg.getPayload() != null
                                        ? msg.getPayload().toString() : "";
                                trips.stream()
                                    .filter(tr -> tr.getTripId().equals(cancelledId))
                                    .findFirst()
                                    .ifPresent(tr -> tr.setStatus(TripStatus.CANCELLED));
                                lblDriverStatus.setText("Waiting for ride requests...");
                                lblDriverStatus.setTextFill(Color.web("#475569"));
                                renderTrips();
                                new Alert(Alert.AlertType.INFORMATION,
                                    "The passenger cancelled the trip.", ButtonType.OK) {{
                                    setTitle("Trip Cancelled");
                                    setHeaderText("Cancelled by passenger");
                                }}.showAndWait();
                            });
                        }
                        default -> {}
                    }
                });
            } catch (Exception ex) {
                // push listener failed — ignore silently
            }
        }, "driver-push-connect");
        t.setDaemon(true);
        t.start();
    }

    /** Send a trip lifecycle message (ACCEPTED / DECLINED / STARTED / COMPLETED). */
    private void sendTripMessage(MessageType type, String tripId, String driverId) {
        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.send(new Message(type, tripId, driverId));
            } catch (Exception ex) {
                // send failed — ignore silently
            }
        }, "trip-msg-" + type.name().toLowerCase());
        t.setDaemon(true);
        t.start();
    }

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
                // ignore silently
            }
        }, "driver-status-update");
        t.setDaemon(true);
        t.start();
    }

    private void startLocationUpdater() {
        if (DriverSessionState.getInstance().getCurrentDriver() == null) return;
        final String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();
        double baseLat = 9.0320 + (Math.random() - 0.5) * 0.05;
        double baseLng = 38.7469 + (Math.random() - 0.5) * 0.05;

        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread th = new Thread(r, "driver-location-updater");
            th.setDaemon(true);
            return th;
        }).scheduleAtFixedRate(() -> {
            if (!DriverSessionState.getInstance().isOnline()) return;
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) return;
                double lat = baseLat + (Math.random() - 0.5) * 0.002;
                double lng = baseLng + (Math.random() - 0.5) * 0.002;
                nc.send(new Message(MessageType.DRIVER_LOCATION_UPDATE,
                        String.format("%.6f,%.6f", lat, lng), driverId));
                // Update map marker
                if (mapView != null) {
                    final double fLat = lat, fLng = lng;
                    Platform.runLater(() -> mapView.setDriverPosition(fLat, fLng));
                }
            } catch (Exception ex) {
                // ignore silently
            }
        }, 5, 15, java.util.concurrent.TimeUnit.SECONDS);
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────────

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
