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

/**
 * Passenger Payments screen.
 * Shows completed trips with their fares.
 * "Pay" button on each trip marks it as paid (simulated — no real payment gateway).
 */
public class PaymentsScreen {
    private final Stage stage;
    private VBox tripList;
    private Label lblTotalSpent;

    public PaymentsScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
        loadTrips();
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
        btnPay.setStyle(btnPay.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e   -> new MainScreen(stage).show());
        btnHist.setOnAction(e  -> new RideHistoryScreen(stage).show());
        btnPromo.setOnAction(e -> new PromotionsScreen(stage).show());
        btnSet.setOnAction(e   -> new SettingsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { SessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnPay, btnPromo, btnSet, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Payments");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Total spent card
        VBox balCard = new VBox(8);
        balCard.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");
        Label lblBalTitle = new Label("Total Spent on Rides");
        lblBalTitle.setTextFill(Color.web("#94a3b8"));
        lblBalTitle.setFont(Font.font("Arial", 12));
        lblTotalSpent = new Label("...");
        lblTotalSpent.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblTotalSpent.setTextFill(Color.web("#3b82f6"));
        balCard.getChildren().addAll(lblBalTitle, lblTotalSpent);

        // Payment methods (informational)
        Label lblMethods = new Label("Accepted Payment Methods");
        lblMethods.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblMethods.setTextFill(Color.web("#f1f5f9"));

        HBox methods = new HBox(12);
        for (String[] m : new String[][]{
            {"📱", "Telebirr"}, {"🏦", "CBE Birr"}, {"💵", "Cash"}
        }) {
            VBox chip = new VBox(4);
            chip.setAlignment(Pos.CENTER);
            chip.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:12 20;");
            Label ico = new Label(m[0]); ico.setFont(Font.font("Arial", 20));
            Label lbl = new Label(m[1]); lbl.setTextFill(Color.web("#94a3b8")); lbl.setFont(Font.font("Arial", 11));
            chip.getChildren().addAll(ico, lbl);
            methods.getChildren().add(chip);
        }

        // Trip payment list
        Label lblTrips = new Label("Trip Payments");
        lblTrips.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblTrips.setTextFill(Color.web("#f1f5f9"));

        tripList = new VBox(8);

        content.getChildren().addAll(title, balCard, lblMethods, methods, lblTrips, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    // ── Load trips from server ────────────────────────────────────────────────

    private void loadTrips() {
        String passengerId = SessionState.getInstance().isLoggedIn()
            ? SessionState.getInstance().getCurrentUser().getId() : null;
        if (passengerId == null) return;

        tripList.getChildren().clear();
        Label loading = new Label("Loading...");
        loading.setTextFill(Color.web("#475569"));
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
                        renderPayments(trips);
                    } else {
                        showEmpty("Could not load payment history.");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showEmpty("Server offline."));
            }
        }, "passenger-payments");
        t.setDaemon(true);
        t.start();
    }

    private void renderPayments(List<TripRequestDTO> trips) {
        tripList.getChildren().clear();

        List<TripRequestDTO> completed = trips.stream()
            .filter(t -> t.getStatus() == TripStatus.COMPLETED)
            .collect(java.util.stream.Collectors.toList());

        double total = completed.stream().mapToDouble(TripRequestDTO::getFare).sum();
        lblTotalSpent.setText(String.format("ETB %.2f", total));

        if (completed.isEmpty()) {
            showEmpty("No completed trips yet.");
            return;
        }

        for (TripRequestDTO trip : completed) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:14;");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label route = new Label(trip.getPickupLocation() + "  →  " + trip.getDropoffLocation());
            route.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;-fx-font-weight:bold;");
            route.setWrapText(true);
            String cat = trip.getCategory() != null ? trip.getCategory().name() : "ECONOMY";
            Label meta = new Label(cat + "  •  " + String.format("%.1f km", trip.getDistanceKm()));
            meta.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
            info.getChildren().addAll(route, meta);

            Label fare = new Label(String.format("ETB %.2f", trip.getFare()));
            fare.setStyle("-fx-text-fill:#22c55e;-fx-font-size:15px;-fx-font-weight:bold;");
            fare.setMinWidth(90);
            fare.setAlignment(Pos.CENTER_RIGHT);

            // Pay button — clicking it simulates payment completion
            Button btnPay = new Button("Pay");
            btnPay.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:8 18;-fx-cursor:hand;");
            btnPay.setOnAction(e -> onPay(trip, btnPay, row));

            row.getChildren().addAll(info, fare, btnPay);
            tripList.getChildren().add(row);
        }
    }

    /**
     * Simulates payment: marks the button as "Paid" and shows a confirmation.
     * In a real system this would call a payment gateway (Telebirr, CBE Birr, etc.).
     */
    private void onPay(TripRequestDTO trip, Button btnPay, HBox row) {
        btnPay.setDisable(true);
        btnPay.setText("Processing...");

        // Simulate a short processing delay
        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                btnPay.setText("✓ Paid");
                btnPay.setStyle("-fx-background-color:#166534;-fx-text-fill:#22c55e;" +
                    "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:8 18;");
                row.setStyle(row.getStyle().replace("#0d1526", "#0d2010")
                    .replace("#1e3a5f", "#166534"));

                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    String.format("Payment of ETB %.2f confirmed.\nThank you for riding with EthioRide!",
                        trip.getFare()), ButtonType.OK);
                alert.setTitle("Payment Successful");
                alert.setHeaderText("✓ Payment Confirmed");
                alert.showAndWait();
            });
        }, "payment-sim").start();
    }

    private void showEmpty(String msg) {
        tripList.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setTextFill(Color.web("#475569"));
        lbl.setFont(Font.font("Arial", 13));
        tripList.getChildren().add(lbl);
        lblTotalSpent.setText("ETB 0.00");
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
