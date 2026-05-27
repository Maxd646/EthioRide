package com.ethioride.driver.ui;

import com.ethioride.driver.network.NetworkClient;
import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.enums.RideCategory;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Driver Earnings screen.
 * - Period tabs: Today / This Week / This Month / All Time
 * - Category breakdown: Economy / Premium / Elite
 * - Platform commission (15%) shown clearly
 */
public class EarningsScreen {
    private static final double COMMISSION_RATE = 0.15;

    private final Stage stage;
    private Label lblGross, lblCommission, lblNet, lblTrips, lblAvg;
    private Label lblEconomy, lblPremium, lblElite;
    private VBox tripList;
    private List<TripRequestDTO> allTrips;
    private String currentPeriod = "All";

    public EarningsScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 960, 700));
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

        // ── Period filter tabs ────────────────────────────────────────────────
        HBox tabs = new HBox(8);
        for (String period : new String[]{"Today", "Week", "Month", "All"}) {
            Button btn = new Button(period);
            btn.setStyle(tabStyle(period.equals("All")));
            btn.setOnAction(e -> {
                currentPeriod = period;
                tabs.getChildren().forEach(n -> ((Button) n).setStyle(tabStyle(false)));
                btn.setStyle(tabStyle(true));
                if (allTrips != null) applyFilter();
            });
            tabs.getChildren().add(btn);
        }

        // ── Summary cards ─────────────────────────────────────────────────────
        lblGross      = new Label("...");
        lblCommission = new Label("...");
        lblNet        = new Label("...");
        lblTrips      = new Label("...");
        lblAvg        = new Label("...");

        styleStatLabel(lblGross,      "#f1f5f9");
        styleStatLabel(lblCommission, "#ef4444");
        styleStatLabel(lblNet,        "#22c55e");
        styleStatLabel(lblTrips,      "#3b82f6");
        styleStatLabel(lblAvg,        "#f59e0b");

        HBox cards = new HBox(12);
        cards.getChildren().addAll(
            statCard("Gross Earnings",    lblGross,      "#f1f5f9"),
            statCard("Commission (15%)",  lblCommission, "#ef4444"),
            statCard("Net Earnings",      lblNet,        "#22c55e"),
            statCard("Trips",             lblTrips,      "#3b82f6"),
            statCard("Avg per Trip",      lblAvg,        "#f59e0b")
        );

        // ── Category breakdown ────────────────────────────────────────────────
        lblEconomy = new Label("ETB 0.00");
        lblPremium = new Label("ETB 0.00");
        lblElite   = new Label("ETB 0.00");
        styleStatLabel(lblEconomy, "#22c55e");
        styleStatLabel(lblPremium, "#3b82f6");
        styleStatLabel(lblElite,   "#f59e0b");

        HBox catCards = new HBox(12);
        catCards.getChildren().addAll(
            statCard("Economy Trips", lblEconomy, "#22c55e"),
            statCard("Premium Trips", lblPremium, "#3b82f6"),
            statCard("Elite Trips",   lblElite,   "#f59e0b")
        );

        // Commission note
        Label commNote = new Label(
            "ℹ  EthioRide retains 15% platform commission. Net earnings = Gross × 85%.");
        commNote.setTextFill(Color.web("#475569"));
        commNote.setFont(Font.font("Arial", 11));
        commNote.setWrapText(true);
        commNote.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:10 14;");

        // ── Trip list ─────────────────────────────────────────────────────────
        Label lblBreakdown = new Label("Trip Breakdown");
        lblBreakdown.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        lblBreakdown.setTextFill(Color.web("#f1f5f9"));

        tripList = new VBox(0);

        content.getChildren().addAll(title, tabs, cards, catCards, commNote, lblBreakdown, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadEarnings() {
        if (DriverSessionState.getInstance().getCurrentDriver() == null) return;
        final String driverId = DriverSessionState.getInstance().getCurrentDriver().getId();

        Thread t = new Thread(() -> {
            try {
                NetworkClient nc = NetworkClient.getInstance();
                if (!nc.isConnected()) nc.connect();
                nc.sendRequest(
                    MessageType.DRIVER_EARNINGS_REQUEST, driverId,
                    MessageType.DRIVER_EARNINGS_RESPONSE, msg -> {
                        java.util.Map<String, Object> data =
                            (java.util.Map<String, Object>) msg.getPayload();
                        List<TripRequestDTO> trips = (List<TripRequestDTO>) data.get("trips");
                        Platform.runLater(() -> {
                            allTrips = trips;
                            applyFilter();
                        });
                    });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblGross.setText("N/A"); lblNet.setText("N/A");
                });
            }
        }, "earnings-load");
        t.setDaemon(true);
        t.start();
    }

    private void applyFilter() {
        if (allTrips == null) return;
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");

        List<TripRequestDTO> filtered = allTrips.stream()
            .filter(t -> t.getStatus() == TripStatus.COMPLETED)
            .filter(t -> {
                if ("All".equals(currentPeriod)) return true;
                if (t.getCreatedAt() == null) return false;
                try {
                    LocalDate tripDate = LocalDate.parse(
                        t.getCreatedAt().substring(0, 12).trim(), fmt.withZone(null));
                    return switch (currentPeriod) {
                        case "Today" -> tripDate.equals(today);
                        case "Week"  -> !tripDate.isBefore(today.minusDays(6));
                        case "Month" -> !tripDate.isBefore(today.withDayOfMonth(1));
                        default      -> true;
                    };
                } catch (Exception ex) { return true; }
            })
            .collect(Collectors.toList());

        updateStats(filtered);
        renderTrips(filtered);
    }

    private void updateStats(List<TripRequestDTO> trips) {
        double gross = trips.stream().mapToDouble(TripRequestDTO::getFare).sum();
        double commission = gross * COMMISSION_RATE;
        double net = gross - commission;
        int count = trips.size();
        double avg = count > 0 ? net / count : 0;

        // Category breakdown
        double eco = trips.stream()
            .filter(t -> t.getCategory() == RideCategory.ECONOMY)
            .mapToDouble(TripRequestDTO::getFare).sum() * (1 - COMMISSION_RATE);
        double pre = trips.stream()
            .filter(t -> t.getCategory() == RideCategory.PREMIUM)
            .mapToDouble(TripRequestDTO::getFare).sum() * (1 - COMMISSION_RATE);
        double eli = trips.stream()
            .filter(t -> t.getCategory() == RideCategory.ELITE)
            .mapToDouble(TripRequestDTO::getFare).sum() * (1 - COMMISSION_RATE);

        lblGross.setText(String.format("ETB %.2f", gross));
        lblCommission.setText(String.format("- ETB %.2f", commission));
        lblNet.setText(String.format("ETB %.2f", net));
        lblTrips.setText(String.valueOf(count));
        lblAvg.setText(String.format("ETB %.2f", avg));
        lblEconomy.setText(String.format("ETB %.2f", eco));
        lblPremium.setText(String.format("ETB %.2f", pre));
        lblElite.setText(String.format("ETB %.2f", eli));
    }

    private void renderTrips(List<TripRequestDTO> trips) {
        tripList.getChildren().clear();
        if (trips.isEmpty()) {
            Label empty = new Label("No completed trips for this period.");
            empty.setTextFill(Color.web("#475569"));
            empty.setFont(Font.font("Arial", 13));
            tripList.getChildren().add(empty);
            return;
        }
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

            String cat = t.getCategory() != null ? t.getCategory().name() : "—";
            Label catLbl = new Label(cat);
            catLbl.setTextFill(Color.web("#475569"));
            catLbl.setFont(Font.font("Arial", 11));
            catLbl.setMinWidth(70);

            double gross = t.getFare();
            double net   = gross * (1 - COMMISSION_RATE);
            Label grossLbl = new Label(String.format("ETB %.2f", gross));
            grossLbl.setTextFill(Color.web("#94a3b8"));
            grossLbl.setFont(Font.font("Arial", 12));
            grossLbl.setMinWidth(90);

            Label netLbl = new Label(String.format("→ ETB %.2f", net));
            netLbl.setTextFill(Color.web("#22c55e"));
            netLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            netLbl.setMinWidth(100);

            row.getChildren().addAll(route, catLbl, grossLbl, netLbl);
            tripList.getChildren().add(row);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox statCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:14;");
        card.setPrefWidth(160);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94a3b8"));
        lbl.setFont(Font.font("Arial", 10));
        card.getChildren().addAll(lbl, valueLabel);
        return card;
    }

    private void styleStatLabel(Label lbl, String color) {
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lbl.setTextFill(Color.web(color));
    }

    private String tabStyle(boolean active) {
        return active
            ? "-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-font-weight:bold;" +
              "-fx-background-radius:6px;-fx-padding:7 18;-fx-cursor:hand;"
            : "-fx-background-color:#0d1526;-fx-text-fill:#94a3b8;" +
              "-fx-background-radius:6px;-fx-padding:7 18;-fx-cursor:hand;";
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
