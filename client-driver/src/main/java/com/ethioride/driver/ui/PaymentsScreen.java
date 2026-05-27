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
import javafx.stage.Modality;
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
    /** Tracks the current available balance so withdrawals can deduct from it. */
    private double currentBalance = 0.0;

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

        Button btnWithdraw = new Button("Withdraw");
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

        // Network I/O must never run on the JavaFX Application Thread
        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.sendRequest(
                    MessageType.DRIVER_EARNINGS_REQUEST, driverId,
                    MessageType.DRIVER_EARNINGS_RESPONSE, msg -> {
                        java.util.Map<String, Object> data =
                            (java.util.Map<String, Object>) msg.getPayload();
                        double total = (double) data.get("total");
                        List<TripRequestDTO> trips = (List<TripRequestDTO>) data.get("trips");

                        Platform.runLater(() -> {
                            currentBalance = total;
                            lblTotal.setText(String.format("ETB %.2f", total));
                            renderTrips(trips.stream()
                                .filter(tr -> tr.getStatus() == TripStatus.COMPLETED)
                                .collect(Collectors.toList()));
                        });
                    });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblTotal.setText("N/A");
                    showEmpty("Could not load earnings.");
                });
            }
        }, "driver-payments-load");
        t.setDaemon(true);
        t.start();
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
     * Full withdrawal flow:
     *  1. Ask for amount
     *  2. Validate against available balance
     *  3. Let driver pick a payment method
     *  4. Confirm and deduct from the displayed balance
     */
    private void onWithdraw(Button btn) {
        if (currentBalance <= 0) {
            showAlert(Alert.AlertType.WARNING, "No Balance",
                "You have no available balance to withdraw.");
            return;
        }

        // ── Step 1: Enter amount ──────────────────────────────────────────────
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Withdraw Funds");
        dialog.setResizable(false);

        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:#0d1526;");
        root.setPrefWidth(360);

        Label heading = new Label("Withdraw Funds");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        heading.setTextFill(Color.web("#f1f5f9"));

        Label available = new Label(String.format("Available: ETB %.2f", currentBalance));
        available.setTextFill(Color.web("#22c55e"));
        available.setFont(Font.font("Arial", 13));

        Label amtLabel = new Label("Enter amount (ETB)");
        amtLabel.setTextFill(Color.web("#94a3b8"));
        amtLabel.setFont(Font.font("Arial", 12));

        TextField amtField = new TextField();
        amtField.setPromptText("e.g. 500");
        amtField.setStyle("-fx-background-color:#0a0e1a;-fx-text-fill:#f1f5f9;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:6px;-fx-background-radius:6px;" +
            "-fx-padding:8 12;-fx-font-size:14;");

        Label errorLbl = new Label("");
        errorLbl.setTextFill(Color.web("#ef4444"));
        errorLbl.setFont(Font.font("Arial", 12));

        Button nextBtn = new Button("Next  →");
        nextBtn.setMaxWidth(Double.MAX_VALUE);
        nextBtn.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");

        nextBtn.setOnAction(e -> {
            String raw = amtField.getText().trim();
            double amount;
            try {
                amount = Double.parseDouble(raw);
            } catch (NumberFormatException ex) {
                errorLbl.setText("Please enter a valid number.");
                return;
            }
            if (amount <= 0) {
                errorLbl.setText("Amount must be greater than 0.");
                return;
            }
            if (amount > currentBalance) {
                errorLbl.setText(String.format(
                    "Insufficient balance. Max: ETB %.2f", currentBalance));
                return;
            }
            // ── Step 2: Pick payment method ───────────────────────────────────
            dialog.close();
            showMethodPicker(btn, amount);
        });

        root.getChildren().addAll(heading, available, amtLabel, amtField, errorLbl, nextBtn);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    /** Step 2 – payment method selection dialog. */
    private void showMethodPicker(Button btn, double amount) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Select Payment Method");
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:#0d1526;");
        root.setPrefWidth(360);

        Label heading = new Label("Select Payment Method");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        heading.setTextFill(Color.web("#f1f5f9"));

        Label sub = new Label(String.format("Withdrawing: ETB %.2f", amount));
        sub.setTextFill(Color.web("#94a3b8"));
        sub.setFont(Font.font("Arial", 13));

        ToggleGroup group = new ToggleGroup();

        RadioButton rbTelebirr = styledRadio("📱  Telebirr", group);
        RadioButton rbCBE      = styledRadio("🏦  CBE Birr", group);
        rbTelebirr.setSelected(true);

        Label errorLbl = new Label("");
        errorLbl.setTextFill(Color.web("#ef4444"));
        errorLbl.setFont(Font.font("Arial", 12));

        Button confirmBtn = new Button("Confirm Withdrawal");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");

        confirmBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            if (selected == null) { errorLbl.setText("Please select a method."); return; }
            String method = selected.getText().replaceAll("^[^a-zA-Z]+", "").trim();
            dialog.close();
            processWithdrawal(btn, amount, method);
        });

        root.getChildren().addAll(heading, sub, rbTelebirr, rbCBE, errorLbl, confirmBtn);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    /** Step 3 – simulate processing and update the balance label. */
    private void processWithdrawal(Button btn, double amount, String method) {
        btn.setDisable(true);
        btn.setText("Processing...");

        new Thread(() -> {
            try { Thread.sleep(900); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                // Deduct from displayed balance
                currentBalance -= amount;
                lblTotal.setText(String.format("ETB %.2f", currentBalance));

                btn.setDisable(false);
                btn.setText("Withdraw");
                btn.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");

                showAlert(Alert.AlertType.INFORMATION,
                    "Withdrawal Successful",
                    String.format("ETB %.2f has been sent to your %s account.\n" +
                        "Funds typically arrive within 1–2 minutes.\n\n" +
                        "Remaining balance: ETB %.2f", amount, method, currentBalance));
            });
        }, "withdraw-sim").start();
    }

    private RadioButton styledRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setTextFill(Color.web("#f1f5f9"));
        rb.setFont(Font.font("Arial", 14));
        rb.setStyle("-fx-padding:10 14;-fx-background-color:#0a0e1a;" +
            "-fx-background-radius:8px;-fx-cursor:hand;");
        rb.setMaxWidth(Double.MAX_VALUE);
        return rb;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
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
