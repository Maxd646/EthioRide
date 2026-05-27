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

/**
 * Driver Payments screen.
 * Shows total earnings from completed trips (real DB data).
 * "Withdraw" button simulates a payout confirmation.
 */
public class PaymentsScreen {
    private final Stage stage;
    private Label lblTotal;
    private VBox tripList;

    public PaymentsScreen(Stage stage) { this.stage = stage; }

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
        btnPay.setStyle(btnPay.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e  -> new MainScreen(stage).show());
        btnHist.setOnAction(e -> new RideHistoryScreen(stage).show());
        btnEarn.setOnAction(e -> new EarningsScreen(stage).show());
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

        Label title = new Label("Payments");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Earnings balance card
        VBox balanceCard = new VBox(10);
        balanceCard.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");
        Label lblBalTitle = new Label("Total Earnings");
        lblBalTitle.setTextFill(Color.web("#94a3b8"));
        lblBalTitle.setFont(Font.font("Arial", 12));
        lblTotal = new Label("...");
        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblTotal.setTextFill(Color.web("#22c55e"));

        Button btnWithdraw = new Button("Withdraw to Telebirr");
        btnWithdraw.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        btnWithdraw.setOnAction(e -> onWithdraw(btnWithdraw));

        balanceCard.getChildren().addAll(lblBalTitle, lblTotal, btnWithdraw);

        // Payout methods (informational)
        Label lblMethods = new Label("Payout Methods");
        lblMethods.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblMethods.setTextFill(Color.web("#f1f5f9"));

        HBox methods = new HBox(12);
        for (String[] m : new String[][]{
            {"📱", "Telebirr"}, {"🏦", "CBE Birr"}
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

        // Completed trip breakdown
        Label lblBreakdown = new Label("Completed Trips");
        lblBreakdown.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblBreakdown.setTextFill(Color.web("#f1f5f9"));

        tripList = new VBox(0);

        content.getChildren().addAll(title, balanceCard, lblMethods, methods, lblBreakdown, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    // ── Load from server ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadEarnings() {
        String driverId = DriverSessionState.getInstance().getCurrentDriver() != null
            ? DriverSessionState.getInstance().getCurrentDriver().getId() : null;
        if (driverId == null) return;

        try {
            NetworkClient.getInstance().connect();
            NetworkClient.getInstance().sendRequest(
                MessageType.DRIVER_EARNINGS_REQUEST, driverId,
                MessageType.DRIVER_EARNINGS_RESPONSE, msg -> {
                    java.util.Map<String, Object> data =
                        (java.util.Map<String, Object>) msg.getPayload();
                    double total = (double) data.get("total");
                    List<TripRequestDTO> trips = (List<TripRequestDTO>) data.get("trips");

                    Platform.runLater(() -> {
                        lblTotal.setText(String.format("ETB %.2f", total));
                        renderTrips(trips.stream()
                            .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                            .collect(Collectors.toList()));
                    });
                });
        } catch (Exception e) {
            Platform.runLater(() -> {
                lblTotal.setText("N/A");
                showEmpty("Could not load earnings.");
            });
        }
    }

    private void renderTrips(List<TripRequestDTO> trips) {
        tripList.getChildren().clear();
        if (trips.isEmpty()) { showEmpty("No completed trips yet."); return; }

        for (TripRequestDTO t : trips) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent;" +
                "-fx-border-width:0 0 1 0;-fx-padding:10 0;");

            Label route = new Label(t.getPickupLocation() + "  →  " + t.getDropoffLocation());
            route.setTextFill(Color.web("#f1f5f9"));
            route.setFont(Font.font("Arial", 13));
            route.setWrapText(true);
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
    }

    /**
     * Simulates a withdrawal to Telebirr.
     * In a real system this would call the Telebirr payout API.
     */
    private void onWithdraw(Button btn) {
        btn.setDisable(true);
        btn.setText("Processing...");

        new Thread(() -> {
            try { Thread.sleep(900); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                btn.setText("✓ Withdrawn");
                btn.setStyle("-fx-background-color:#166534;-fx-text-fill:#22c55e;" +
                    "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;");

                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Your earnings have been sent to your Telebirr account.\n" +
                    "Funds typically arrive within 1-2 minutes.", ButtonType.OK);
                alert.setTitle("Withdrawal Successful");
                alert.setHeaderText("✓ Payout Confirmed");
                alert.showAndWait();

                // Re-enable after confirmation
                btn.setDisable(false);
                btn.setText("Withdraw to Telebirr");
                btn.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
            });
        }, "withdraw-sim").start();
    }

    private void showEmpty(String msg) {
        tripList.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setTextFill(Color.web("#475569"));
        lbl.setFont(Font.font("Arial", 13));
        tripList.getChildren().add(lbl);
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
