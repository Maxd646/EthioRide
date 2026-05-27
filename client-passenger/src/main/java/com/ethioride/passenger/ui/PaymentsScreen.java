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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Passenger Payments screen.
 * - Simulated Telebirr / CBE Birr / Cash payment gateway with phone + OTP flow
 * - Refund simulation for cancelled trips
 */
public class PaymentsScreen {
    private final Stage stage;
    private VBox tripList;
    private Label lblTotalSpent;
    private Label lblRefundDue;

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

        // Summary row
        HBox summaryRow = new HBox(16);
        VBox spentCard = summaryCard("Total Spent", "...", "#3b82f6");
        lblTotalSpent = (Label) ((VBox) spentCard).getChildren().get(1);
        VBox refundCard = summaryCard("Pending Refunds", "ETB 0.00", "#f59e0b");
        lblRefundDue = (Label) ((VBox) refundCard).getChildren().get(1);
        summaryRow.getChildren().addAll(spentCard, refundCard);

        // Payment methods
        Label lblMethods = new Label("Payment Methods");
        lblMethods.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblMethods.setTextFill(Color.web("#f1f5f9"));

        HBox methods = new HBox(12);
        for (String[] m : new String[][]{
            {"📱", "Telebirr", "Instant"}, {"🏦", "CBE Birr", "Instant"}, {"💵", "Cash", "On arrival"}
        }) {
            VBox chip = new VBox(4);
            chip.setAlignment(Pos.CENTER);
            chip.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:12 20;");
            Label ico = new Label(m[0]); ico.setFont(Font.font("Arial", 20));
            Label lbl = new Label(m[1]); lbl.setTextFill(Color.web("#f1f5f9")); lbl.setFont(Font.font("Arial", 11));
            Label sub = new Label(m[2]); sub.setTextFill(Color.web("#475569")); sub.setFont(Font.font("Arial", 10));
            chip.getChildren().addAll(ico, lbl, sub);
            methods.getChildren().add(chip);
        }

        Label lblTrips = new Label("Trip Payments");
        lblTrips.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblTrips.setTextFill(Color.web("#f1f5f9"));

        tripList = new VBox(8);

        content.getChildren().addAll(title, summaryRow, lblMethods, methods, lblTrips, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private VBox summaryCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:18;");
        card.setPrefWidth(200);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94a3b8")); lbl.setFont(Font.font("Arial", 12));
        Label val = new Label(value);
        val.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        val.setTextFill(Color.web(color));
        card.getChildren().addAll(lbl, val);
        return card;
    }

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
                Message response = conn.sendAndWait(
                    new Message(MessageType.PASSENGER_TRIP_HISTORY_REQUEST, passengerId, passengerId),
                    MessageType.PASSENGER_TRIP_HISTORY_RESPONSE, 8000);
                conn.close();
                Platform.runLater(() -> {
                    if (response != null && response.getType() == MessageType.PASSENGER_TRIP_HISTORY_RESPONSE) {
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
            .collect(Collectors.toList());
        List<TripRequestDTO> cancelled = trips.stream()
            .filter(t -> t.getStatus() == TripStatus.CANCELLED)
            .collect(Collectors.toList());

        double total = completed.stream().mapToDouble(TripRequestDTO::getFare).sum();
        // Refund = 80% of fare for cancelled trips that had a driver assigned
        double refund = cancelled.stream()
            .filter(t -> t.getDriverId() != null)
            .mapToDouble(t -> t.getFare() * 0.80)
            .sum();

        lblTotalSpent.setText(String.format("ETB %.2f", total));
        lblRefundDue.setText(refund > 0 ? String.format("ETB %.2f", refund) : "ETB 0.00");

        if (completed.isEmpty() && cancelled.isEmpty()) {
            showEmpty("No trips yet.");
            return;
        }

        // Completed trips — show Pay button
        if (!completed.isEmpty()) {
            Label sec = sectionLabel("Completed Trips");
            tripList.getChildren().add(sec);
            for (TripRequestDTO trip : completed) {
                tripList.getChildren().add(buildCompletedRow(trip));
            }
        }

        // Cancelled trips — show Refund button if driver was assigned
        if (!cancelled.isEmpty()) {
            Label sec = sectionLabel("Cancelled Trips");
            tripList.getChildren().add(sec);
            for (TripRequestDTO trip : cancelled) {
                tripList.getChildren().add(buildCancelledRow(trip));
            }
        }
    }

    private HBox buildCompletedRow(TripRequestDTO trip) {
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

        Button btnPay = new Button("Pay");
        btnPay.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:8 18;-fx-cursor:hand;");
        btnPay.setOnAction(e -> showPaymentGateway(trip, btnPay, row));

        row.getChildren().addAll(info, fare, btnPay);
        return row;
    }

    private HBox buildCancelledRow(TripRequestDTO trip) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#0d1526;-fx-border-color:#7f1d1d;" +
            "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:14;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label route = new Label(trip.getPickupLocation() + "  →  " + trip.getDropoffLocation());
        route.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;-fx-font-weight:bold;");
        route.setWrapText(true);
        Label meta = new Label("CANCELLED  •  " + String.format("ETB %.2f fare", trip.getFare()));
        meta.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11px;");
        info.getChildren().addAll(route, meta);

        boolean hadDriver = trip.getDriverId() != null;
        double refundAmt = trip.getFare() * 0.80;

        if (hadDriver && trip.getFare() > 0) {
            Label refundLbl = new Label(String.format("ETB %.2f", refundAmt));
            refundLbl.setStyle("-fx-text-fill:#f59e0b;-fx-font-size:14px;-fx-font-weight:bold;");
            refundLbl.setMinWidth(90);
            refundLbl.setAlignment(Pos.CENTER_RIGHT);

            Button btnRefund = new Button("Refund");
            btnRefund.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:#0a0e1a;" +
                "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:8 18;-fx-cursor:hand;");
            btnRefund.setOnAction(e -> processRefund(trip, refundAmt, btnRefund, row));
            row.getChildren().addAll(info, refundLbl, btnRefund);
        } else {
            Label noRefund = new Label("No charge");
            noRefund.setStyle("-fx-text-fill:#475569;-fx-font-size:12px;");
            row.getChildren().addAll(info, noRefund);
        }
        return row;
    }

    // ── Payment Gateway ───────────────────────────────────────────────────────

    /**
     * Simulated payment gateway: Telebirr / CBE Birr / Cash
     * Step 1 — choose method
     * Step 2 — enter phone (for mobile money) or confirm (for cash)
     * Step 3 — enter OTP (simulated)
     * Step 4 — success
     */
    private void showPaymentGateway(TripRequestDTO trip, Button btnPay, HBox row) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Pay for Ride");
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:#0d1526;");
        root.setPrefWidth(380);

        Label heading = new Label("Choose Payment Method");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        heading.setTextFill(Color.web("#f1f5f9"));

        Label amtLbl = new Label(String.format("Amount: ETB %.2f", trip.getFare()));
        amtLbl.setTextFill(Color.web("#22c55e"));
        amtLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));

        ToggleGroup group = new ToggleGroup();
        RadioButton rbTelebirr = styledRadio("📱  Telebirr", group);
        RadioButton rbCBE      = styledRadio("🏦  CBE Birr", group);
        RadioButton rbCash     = styledRadio("💵  Cash (pay driver on arrival)", group);
        rbTelebirr.setSelected(true);

        Button btnNext = new Button("Continue  →");
        btnNext.setMaxWidth(Double.MAX_VALUE);
        btnNext.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        btnNext.setOnAction(e -> {
            RadioButton sel = (RadioButton) group.getSelectedToggle();
            String method = sel.getText().replaceAll("^[^a-zA-Z]+", "").split("\\(")[0].trim();
            dialog.close();
            if (method.startsWith("Cash")) {
                confirmCash(trip, btnPay, row);
            } else {
                showPhoneEntry(trip, btnPay, row, method);
            }
        });

        root.getChildren().addAll(heading, amtLbl, rbTelebirr, rbCBE, rbCash, btnNext);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    private void showPhoneEntry(TripRequestDTO trip, Button btnPay, HBox row, String method) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle(method + " — Enter Phone");
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:#0d1526;");
        root.setPrefWidth(360);

        Label heading = new Label(method);
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        heading.setTextFill(Color.web("#f1f5f9"));

        Label sub = new Label(String.format("Paying ETB %.2f", trip.getFare()));
        sub.setTextFill(Color.web("#94a3b8")); sub.setFont(Font.font("Arial", 13));

        Label phoneLbl = new Label("Mobile Number");
        phoneLbl.setTextFill(Color.web("#94a3b8")); phoneLbl.setFont(Font.font("Arial", 12));

        TextField tfPhone = new TextField();
        tfPhone.setPromptText("+251 9XX XXX XXX");
        styleField(tfPhone);

        Label errLbl = new Label(""); errLbl.setTextFill(Color.web("#ef4444")); errLbl.setFont(Font.font("Arial", 11));

        Button btnSend = new Button("Send OTP");
        btnSend.setMaxWidth(Double.MAX_VALUE);
        btnSend.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        btnSend.setOnAction(e -> {
            String phone = tfPhone.getText().trim();
            if (phone.length() < 9) { errLbl.setText("Enter a valid phone number."); return; }
            dialog.close();
            showOtpEntry(trip, btnPay, row, method, phone);
        });

        root.getChildren().addAll(heading, sub, phoneLbl, tfPhone, errLbl, btnSend);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    private void showOtpEntry(TripRequestDTO trip, Button btnPay, HBox row, String method, String phone) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Enter OTP");
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:#0d1526;");
        root.setPrefWidth(360);

        Label heading = new Label("Verify Payment");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        heading.setTextFill(Color.web("#f1f5f9"));

        Label sub = new Label("OTP sent to " + phone + "\n(Simulated — enter any 6 digits)");
        sub.setTextFill(Color.web("#94a3b8")); sub.setFont(Font.font("Arial", 12)); sub.setWrapText(true);

        TextField tfOtp = new TextField();
        tfOtp.setPromptText("6-digit OTP");
        styleField(tfOtp);

        Label errLbl = new Label(""); errLbl.setTextFill(Color.web("#ef4444")); errLbl.setFont(Font.font("Arial", 11));

        Button btnVerify = new Button("Verify & Pay");
        btnVerify.setMaxWidth(Double.MAX_VALUE);
        btnVerify.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        btnVerify.setOnAction(e -> {
            String otp = tfOtp.getText().trim();
            if (otp.length() != 6 || !otp.matches("\\d+")) {
                errLbl.setText("Enter a valid 6-digit OTP.");
                return;
            }
            dialog.close();
            simulatePaymentProcessing(trip, btnPay, row, method);
        });

        root.getChildren().addAll(heading, sub, tfOtp, errLbl, btnVerify);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    private void confirmCash(TripRequestDTO trip, Button btnPay, HBox row) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            String.format("Pay ETB %.2f in cash to your driver on arrival?", trip.getFare()),
            ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Cash Payment");
        a.setHeaderText("Confirm Cash Payment");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) simulatePaymentProcessing(trip, btnPay, row, "Cash");
        });
    }

    private void simulatePaymentProcessing(TripRequestDTO trip, Button btnPay, HBox row, String method) {
        btnPay.setDisable(true);
        btnPay.setText("Processing...");
        new Thread(() -> {
            try { Thread.sleep(900); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                btnPay.setText("✓ Paid");
                btnPay.setStyle("-fx-background-color:#166534;-fx-text-fill:#22c55e;" +
                    "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:8 18;");
                Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    String.format("✓ Payment of ETB %.2f confirmed via %s.\nThank you for riding with EthioRide!",
                        trip.getFare(), method), ButtonType.OK);
                ok.setTitle("Payment Successful");
                ok.setHeaderText("Payment Confirmed");
                ok.showAndWait();
            });
        }, "pay-sim").start();
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    private void processRefund(TripRequestDTO trip, double refundAmt, Button btnRefund, HBox row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            String.format("Request a refund of ETB %.2f (80%% of ETB %.2f fare)?\n\n" +
                "Refunds are processed within 1–3 business days.", refundAmt, trip.getFare()),
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Request Refund");
        confirm.setHeaderText("Confirm Refund Request");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            btnRefund.setDisable(true);
            btnRefund.setText("Processing...");
            new Thread(() -> {
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    btnRefund.setText("✓ Refund Requested");
                    btnRefund.setStyle("-fx-background-color:#166534;-fx-text-fill:#22c55e;" +
                        "-fx-font-weight:bold;-fx-background-radius:8px;-fx-padding:8 18;");
                    // Update refund label
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        String.format("Refund of ETB %.2f has been requested.\n" +
                            "It will be credited to your original payment method within 1–3 business days.",
                            refundAmt), ButtonType.OK);
                    ok.setTitle("Refund Requested");
                    ok.setHeaderText("✓ Refund Submitted");
                    ok.showAndWait();
                });
            }, "refund-sim").start();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lbl.setTextFill(Color.web("#94a3b8"));
        lbl.setPadding(new Insets(8, 0, 4, 0));
        return lbl;
    }

    private void showEmpty(String msg) {
        tripList.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setTextFill(Color.web("#475569"));
        lbl.setFont(Font.font("Arial", 13));
        tripList.getChildren().add(lbl);
        lblTotalSpent.setText("ETB 0.00");
    }

    private RadioButton styledRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setTextFill(Color.web("#f1f5f9"));
        rb.setFont(Font.font("Arial", 13));
        rb.setStyle("-fx-padding:8 12;-fx-background-color:#0a0e1a;" +
            "-fx-background-radius:8px;-fx-cursor:hand;");
        rb.setMaxWidth(Double.MAX_VALUE);
        return rb;
    }

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color:#0a0e1a;-fx-text-fill:#f1f5f9;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:6px;-fx-background-radius:6px;" +
            "-fx-padding:8 12;-fx-font-size:14;");
        f.setMaxWidth(Double.MAX_VALUE);
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
