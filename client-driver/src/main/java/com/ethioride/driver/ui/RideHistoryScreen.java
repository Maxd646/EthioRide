package com.ethioride.driver.ui;

import com.ethioride.driver.network.NetworkClient;
import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.TripStatus;
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

import java.util.List;
import java.util.stream.Collectors;

public class RideHistoryScreen {
    private final Stage stage;
    private VBox tripList;
    private List<TripRequestDTO> allTrips;
    private Label lblTotal, lblEarnings, lblRating;

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
        Label logo = new Label("🚗 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#22c55e"));
        logo.setPadding(new Insets(24, 20, 20, 20));
        Button btnMap  = navBtn("🗺  Live Map");
        Button btnHist = navBtn("🕐  Ride History");
        Button btnEarn = navBtn("💰  Earnings");
        Button btnPay  = navBtn("💳  Payments");
        btnHist.setStyle(btnHist.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e  -> new MainScreen(stage).show());
        btnEarn.setOnAction(e -> new EarningsScreen(stage).show());
        btnPay.setOnAction(e  -> new PaymentsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { DriverSessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnEarn, btnPay, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Ride History");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Summary cards
        lblTotal    = new Label("...");
        lblEarnings = new Label("...");
        lblRating   = new Label(String.format("%.1f ★",
            DriverSessionState.getInstance().getCurrentDriver().getRating()));

        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblTotal.setTextFill(Color.web("#22c55e"));
        lblEarnings.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblEarnings.setTextFill(Color.web("#3b82f6"));
        lblRating.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblRating.setTextFill(Color.web("#f59e0b"));

        HBox summary = new HBox(16);
        summary.getChildren().addAll(
            statCard("Total Trips",    lblTotal,    "#22c55e"),
            statCard("Total Earnings", lblEarnings, "#3b82f6"),
            statCard("Your Rating",    lblRating,   "#f59e0b")
        );

        // Search + filter
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search by location...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;" +
                          "-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;" +
                          "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);
        ComboBox<String> cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("All", "COMPLETED", "CANCELLED");
        cbFilter.setValue("All");
        cbFilter.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        toolbar.getChildren().addAll(tfSearch, cbFilter);

        tripList = new VBox(8);

        tfSearch.textProperty().addListener((o, ov, nv) -> filter(nv, cbFilter.getValue()));
        cbFilter.setOnAction(e -> filter(tfSearch.getText(), cbFilter.getValue()));

        content.getChildren().addAll(title, summary, toolbar, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private void loadHistory() {
        String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();
        try {
            NetworkClient.getInstance().connect();
            NetworkClient.getInstance().sendRequest(
                MessageType.DRIVER_TRIP_HISTORY_REQUEST, driverId,
                MessageType.DRIVER_TRIP_HISTORY_RESPONSE, msg -> {
                    @SuppressWarnings("unchecked")
                    List<TripRequestDTO> trips = (List<TripRequestDTO>) msg.getPayload();
                    allTrips = trips;

                    long completed = trips.stream()
                        .filter(t -> t.getStatus() == TripStatus.COMPLETED).count();
                    double earnings = trips.stream()
                        .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                        .mapToDouble(TripRequestDTO::getFare).sum();

                    Platform.runLater(() -> {
                        lblTotal.setText(String.valueOf(completed));
                        lblEarnings.setText(String.format("ETB %.2f", earnings));
                        renderTrips(trips);
                    });
                });
        } catch (Exception e) {
            Platform.runLater(() -> {
                lblTotal.setText("N/A");
                lblEarnings.setText("N/A");
            });
        }
    }

    private void renderTrips(List<TripRequestDTO> trips) {
        tripList.getChildren().clear();
        if (trips.isEmpty()) {
            Label empty = new Label("No trips yet.");
            empty.setTextFill(Color.web("#475569"));
            empty.setFont(Font.font("Arial", 13));
            tripList.getChildren().add(empty);
            return;
        }
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
            String passengerName = t.getPassengerPhone() != null ? t.getPassengerPhone() : "Passenger";
            Label meta = new Label(String.format("%.1f km  •  %s", t.getDistanceKm(), passengerName));
            meta.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
            info.getChildren().addAll(route, meta);

            // Fare + status
            VBox right = new VBox(4);
            right.setAlignment(Pos.CENTER_RIGHT);
            Label fare = new Label(String.format("ETB %.2f", t.getFare()));
            fare.setStyle("-fx-text-fill:#22c55e;-fx-font-size:14px;-fx-font-weight:bold;");
            Label status = new Label(t.getStatus() != null ? t.getStatus().name() : "—");
            status.setStyle((completed ? "-fx-text-fill:#22c55e;" : "-fx-text-fill:#ef4444;") +
                            "-fx-font-size:10px;");
            right.getChildren().addAll(fare, status);

            card.getChildren().addAll(icon, info, right);
            tripList.getChildren().add(card);
        }
    }

    private void filter(String q, String status) {
        if (allTrips == null) return;
        List<TripRequestDTO> filtered = allTrips.stream()
            .filter(t -> (q.isEmpty() ||
                t.getPickupLocation().toLowerCase().contains(q.toLowerCase()) ||
                t.getDropoffLocation().toLowerCase().contains(q.toLowerCase()))
                && ("All".equals(status) || (t.getStatus() != null && t.getStatus().name().equals(status))))
            .collect(Collectors.toList());
        renderTrips(filtered);
    }

    private VBox statCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                      "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:14;");
        card.setPrefWidth(180);
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
