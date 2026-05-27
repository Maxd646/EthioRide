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

public class EarningsScreen {
    private final Stage stage;
    private Label lblTotal, lblTrips, lblAvg;
    private VBox tripList;

    public EarningsScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
        loadEarnings();
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
        btnEarn.setStyle(btnEarn.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e  -> new MainScreen(stage).show());
        btnHist.setOnAction(e -> new RideHistoryScreen(stage).show());
        btnPay.setOnAction(e  -> new PaymentsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { DriverSessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnEarn, btnPay, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Earnings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Summary cards — populated from DB
        lblTotal = new Label("...");
        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTotal.setTextFill(Color.web("#22c55e"));

        lblTrips = new Label("...");
        lblTrips.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTrips.setTextFill(Color.web("#3b82f6"));

        lblAvg = new Label("...");
        lblAvg.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblAvg.setTextFill(Color.web("#f59e0b"));

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("Total Earnings",  lblTotal, "#22c55e"),
            statCard("Completed Trips", lblTrips, "#3b82f6"),
            statCard("Avg per Trip",    lblAvg,   "#f59e0b")
        );

        // Trip breakdown list
        Label lblBreakdown = new Label("Trip Breakdown");
        lblBreakdown.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblBreakdown.setTextFill(Color.web("#f1f5f9"));

        tripList = new VBox(0);

        content.getChildren().addAll(title, cards, lblBreakdown, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    @SuppressWarnings("unchecked")
    private void loadEarnings() {
        String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();
        try {
            NetworkClient.getInstance().connect();
            NetworkClient.getInstance().sendRequest(
                MessageType.DRIVER_EARNINGS_REQUEST, driverId,
                MessageType.DRIVER_EARNINGS_RESPONSE, msg -> {
                    java.util.Map<String, Object> data =
                        (java.util.Map<String, Object>) msg.getPayload();
                    double total    = (double) data.get("total");
                    int    count    = (int)    data.get("tripCount");
                    List<TripRequestDTO> trips = (List<TripRequestDTO>) data.get("trips");
                    double avg = count > 0 ? total / count : 0;

                    Platform.runLater(() -> {
                        lblTotal.setText(String.format("ETB %.2f", total));
                        lblTrips.setText(String.valueOf(count));
                        lblAvg.setText(String.format("ETB %.2f", avg));
                        renderTrips(trips);
                    });
                });
        } catch (Exception e) {
            Platform.runLater(() -> {
                lblTotal.setText("N/A");
                lblTrips.setText("N/A");
                lblAvg.setText("N/A");
            });
        }
    }

    private void renderTrips(List<TripRequestDTO> trips) {
        tripList.getChildren().clear();
        for (TripRequestDTO t : trips) {
            if (t.getStatus() != TripStatus.COMPLETED) continue;
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent;" +
                         "-fx-border-width:0 0 1 0;-fx-padding:10 0;");

            Label route = new Label(t.getPickupLocation() + "  →  " + t.getDropoffLocation());
            route.setTextFill(Color.web("#f1f5f9"));
            route.setFont(Font.font("Arial", 13));
            HBox.setHgrow(route, Priority.ALWAYS);

            Label dist = new Label(String.format("%.1f km", t.getDistanceKm()));
            dist.setTextFill(Color.web("#475569"));
            dist.setFont(Font.font("Arial", 12));
            dist.setMinWidth(70);

            Label fare = new Label(String.format("ETB %.2f", t.getFare()));
            fare.setTextFill(Color.web("#22c55e"));
            fare.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            fare.setMinWidth(100);
            fare.setAlignment(Pos.CENTER_RIGHT);

            row.getChildren().addAll(route, dist, fare);
            tripList.getChildren().add(row);
        }
        if (tripList.getChildren().isEmpty()) {
            Label empty = new Label("No completed trips yet.");
            empty.setTextFill(Color.web("#475569"));
            empty.setFont(Font.font("Arial", 13));
            tripList.getChildren().add(empty);
        }
    }

    private VBox statCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                      "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");
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
