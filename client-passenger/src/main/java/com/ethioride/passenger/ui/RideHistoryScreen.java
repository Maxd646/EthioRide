package com.ethioride.passenger.ui;

import com.ethioride.passenger.network.ServerConnection;
import com.ethioride.passenger.state.SessionState;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.TripStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RideHistoryScreen {
    private final Stage stage;
    private VBox tripList;
    private List<TripRequestDTO> allTrips = new ArrayList<>();
    private Label lblTotalTrips, lblTotalSpent;

    public RideHistoryScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
        loadHistory();
    }

    private VBox buildSidebar() {
        VBox s = new VBox(0);
        s.setPrefWidth(200);
        s.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");
        Label logo = new Label("🚕 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#3b82f6"));
        logo.setPadding(new Insets(24, 20, 20, 20));
        Button btnMap   = navBtn("🗺  Map");
        Button btnHist  = navBtn("🕐  Ride History");
        Button btnPay   = navBtn("💳  Payments");
        Button btnPromo = navBtn("🏷  Promotions");
        Button btnSet   = navBtn("⚙  Settings");
        btnHist.setStyle(btnHist.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e   -> new MainScreen(stage).show());
        btnPay.setOnAction(e   -> new PaymentsScreen(stage).show());
        btnPromo.setOnAction(e -> new PromotionsScreen(stage).show());
        btnSet.setOnAction(e   -> new SettingsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { SessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnPay, btnPromo, btnSet, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Ride History");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Summary cards — updated after load
        lblTotalTrips = new Label("...");
        lblTotalTrips.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblTotalTrips.setTextFill(Color.web("#22c55e"));

        lblTotalSpent = new Label("...");
        lblTotalSpent.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblTotalSpent.setTextFill(Color.web("#3b82f6"));

        HBox summary = new HBox(16);
        summary.getChildren().addAll(
            statCard("Total Trips",  lblTotalTrips, "#22c55e"),
            statCard("Total Spent",  lblTotalSpent, "#3b82f6")
        );

        // Search + filter toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search trips...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;" +
            "-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);
        ComboBox<String> cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("All", "COMPLETED", "CANCELLED");
        cbFilter.setValue("All");
        cbFilter.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");

        Button btnRefresh = new Button("↻ Refresh");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;" +
            "-fx-background-radius:6px;-fx-padding:8 14;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadHistory());
        toolbar.getChildren().addAll(tfSearch, cbFilter, btnRefresh);

        tripList = new VBox(8);

        tfSearch.textProperty().addListener((o, ov, nv) -> filter(nv, cbFilter.getValue()));
        cbFilter.setOnAction(e -> filter(tfSearch.getText(), cbFilter.getValue()));

        content.getChildren().addAll(title, summary, toolbar, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    // ── Load from server ──────────────────────────────────────────────────────

    private void loadHistory() {
        String passengerId = SessionState.getInstance().isLoggedIn()
            ? SessionState.getInstance().getCurrentUser().getId() : null;
        if (passengerId == null) return;

        tripList.getChildren().clear();
        Label loading = new Label("Loading...");
        loading.setTextFill(Color.web("#475569"));
        loading.setFont(Font.font("Arial", 13));
        tripList.getChildren().add(loading);

        Thread t = new Thread(() -> {
            try {
                ServerConnection conn = new ServerConnection();
                conn.connect();
                Message response = conn.sendAndReceive(
                    new Message(MessageType.PASSENGER_TRIP_HISTORY_REQUEST, passengerId, passengerId));
                conn.close();

                Platform.runLater(() -> {
                    if (response.getType() == MessageType.PASSENGER_TRIP_HISTORY_RESPONSE) {
                        @SuppressWarnings("unchecked")
                        List<TripRequestDTO> trips = (List<TripRequestDTO>) response.getPayload();
                        allTrips = trips;
                        updateSummary();
                        renderTrips(trips);
                    } else {
                        showEmpty("Could not load trip history.");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showEmpty("Server offline — cannot load history."));
            }
        }, "passenger-history");
        t.setDaemon(true);
        t.start();
    }

    private void updateSummary() {
        long completed = allTrips.stream()
            .filter(t -> t.getStatus() == TripStatus.COMPLETED).count();
        double spent = allTrips.stream()
            .filter(t -> t.getStatus() == TripStatus.COMPLETED)
            .mapToDouble(TripRequestDTO::getFare).sum();
        lblTotalTrips.setText(String.valueOf(completed));
        lblTotalSpent.setText(String.format("ETB %.2f", spent));
    }

    private void renderTrips(List<TripRequestDTO> trips) {
        tripList.getChildren().clear();
        if (trips.isEmpty()) { showEmpty("No trips yet."); return; }

        for (TripRequestDTO t : trips) {
            boolean completed = t.getStatus() == TripStatus.COMPLETED;

            HBox card = new HBox(16);
            card.setStyle("-fx-background-color:#1a2235;-fx-background-radius:12px;-fx-padding:16;");
            card.setAlignment(Pos.CENTER_LEFT);

            // Status icon
            StackPane icon = new StackPane();
            icon.setStyle("-fx-background-color:#1e3a5f;-fx-background-radius:50%;" +
                "-fx-min-width:44px;-fx-min-height:44px;");
            Label ico = new Label(completed ? "✓" : "✕");
            ico.setStyle((completed ? "-fx-text-fill:#22c55e;" : "-fx-text-fill:#ef4444;") +
                "-fx-font-size:16px;-fx-font-weight:bold;");
            icon.getChildren().add(ico);

            // Route info
            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label route = new Label(t.getPickupLocation() + "  →  " + t.getDropoffLocation());
            route.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;-fx-font-weight:bold;");
            String cat = t.getCategory() != null ? t.getCategory().name() : "ECONOMY";
            String dateStr = t.getCreatedAt() != null ? t.getCreatedAt() : "";
            String metaText = dateStr.isEmpty()
                ? cat + "  •  " + String.format("%.1f km", t.getDistanceKm())
                : dateStr + "  •  " + cat + "  •  " + String.format("%.1f km", t.getDistanceKm());
            Label meta = new Label(metaText);
            meta.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
            info.getChildren().addAll(route, meta);

            // Fare + status
            VBox right = new VBox(4);
            right.setAlignment(Pos.CENTER_RIGHT);
            Label fare = new Label(String.format("ETB %.2f", t.getFare()));
            fare.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:14px;-fx-font-weight:bold;");
            Label status = new Label(t.getStatus() != null ? t.getStatus().name() : "—");
            status.setStyle((completed ? "-fx-text-fill:#22c55e;" : "-fx-text-fill:#ef4444;") +
                "-fx-font-size:10px;");
            right.getChildren().addAll(fare, status);

            card.getChildren().addAll(icon, info, right);
            tripList.getChildren().add(card);
        }
    }

    private void filter(String q, String status) {
        List<TripRequestDTO> filtered = allTrips.stream()
            .filter(t -> (q.isEmpty() ||
                t.getPickupLocation().toLowerCase().contains(q.toLowerCase()) ||
                t.getDropoffLocation().toLowerCase().contains(q.toLowerCase()))
                && ("All".equals(status) || (t.getStatus() != null && t.getStatus().name().equals(status))))
            .collect(Collectors.toList());
        renderTrips(filtered);
    }

    private void showEmpty(String msg) {
        tripList.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setTextFill(Color.web("#475569"));
        lbl.setFont(Font.font("Arial", 13));
        tripList.getChildren().add(lbl);
    }

    private VBox statCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:14;");
        card.setPrefWidth(160);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94a3b8"));
        lbl.setFont(Font.font("Arial", 11));
        card.getChildren().addAll(lbl, valueLabel);
        return card;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;" +
            "-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
