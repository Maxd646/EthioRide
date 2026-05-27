package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
import com.ethioride.shared.dto.DashboardStatsDTO;
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DashboardScreen {
    private final Stage stage;
    private VBox logContainer;
    private ScrollPane logScroll;
    private Timeline logTimer;
    private boolean paused = false;

    // Stat card value labels — updated live from DB
    private Label lblDriversVal;
    private Label lblPassengersVal;
    private Label lblActiveTripsVal;
    private Label lblRevenueVal;

    public DashboardScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 700));
        stage.setResizable(true);
        stage.show();
        startLogSimulator();
        loadStats();
    }

    private VBox buildSidebar() {
        VBox s = new VBox(0);
        s.setPrefWidth(200);
        s.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");
        Label logo = new Label("⚙ EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#f59e0b"));
        logo.setPadding(new Insets(24, 20, 4, 20));
        Label sub = new Label("Admin Dashboard");
        sub.setFont(Font.font("Arial", 11));
        sub.setTextFill(Color.web("#475569"));
        sub.setPadding(new Insets(0, 20, 20, 20));
        Button btnDash    = navBtn("📊  Dashboard");
        Button btnDrivers = navBtn("🚗  Drivers");
        Button btnTrips   = navBtn("🗺  Trips");
        Button btnUsers   = navBtn("👥  Users");
        Button btnPricing = navBtn("💰  Pricing");
        Button btnReport  = navBtn("📈  Financial Report");
        Button btnSystem  = navBtn("🖥  System");
        btnDash.setStyle(btnDash.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDrivers.setOnAction(e -> { stopLog(); new DriversScreen(stage).show(); });
        btnTrips.setOnAction(e   -> { stopLog(); new TripsScreen(stage).show(); });
        btnUsers.setOnAction(e   -> { stopLog(); new UsersScreen(stage).show(); });
        btnPricing.setOnAction(e -> { stopLog(); new PricingScreen(stage).show(); });
        btnReport.setOnAction(e  -> { stopLog(); new FinancialReportScreen(stage).show(); });
        btnSystem.setOnAction(e  -> { stopLog(); new SystemScreen(stage).show(); });
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Label lblUser = new Label("👤 " + AdminSession.getInstance().getUsername());
        lblUser.setTextFill(Color.web("#f1f5f9"));
        lblUser.setFont(Font.font("Arial", 12));
        lblUser.setPadding(new Insets(8, 20, 4, 20));
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> {
            stopLog();
            AdminService.getInstance().disconnect();
            AdminSession.getInstance().logout();
            new LoginScreen(stage).show();
        });
        s.getChildren().addAll(logo, sub, btnDash, btnDrivers, btnTrips, btnUsers, btnPricing, btnReport, btnSystem, sp, lblUser, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("System Dashboard");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Stat cards — values start as "..." until DB responds
        lblDriversVal     = new Label("...");
        lblPassengersVal  = new Label("...");
        lblActiveTripsVal = new Label("...");
        lblRevenueVal     = new Label("...");

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("Total Drivers",     lblDriversVal,     "from database", "#22c55e"),
            statCard("Total Passengers",  lblPassengersVal,  "from database", "#3b82f6"),
            statCard("Active Trips",      lblActiveTripsVal, "in progress",   "#f59e0b"),
            statCard("Total Revenue",     lblRevenueVal,     "completed trips","#a855f7")
        );

        // Refresh button
        Button btnRefresh = new Button("↻  Refresh Stats");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:8 16;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadStats());

        // Live log
        Label lblLog = new Label("Live System Log");
        lblLog.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblLog.setTextFill(Color.web("#f1f5f9"));

        logContainer = new VBox(2);
        logContainer.setPadding(new Insets(10));
        logContainer.setStyle("-fx-background-color:#060a14;");

        logScroll = new ScrollPane(logContainer);
        logScroll.setFitToWidth(true);
        logScroll.setPrefHeight(280);
        logScroll.setStyle("-fx-background-color:#060a14;-fx-background:#060a14;");

        HBox logControls = new HBox(12);
        Button btnPause = new Button("Pause");
        btnPause.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:6 14;-fx-cursor:hand;");
        btnPause.setOnAction(e -> { paused = !paused; btnPause.setText(paused ? "Resume" : "Pause"); });
        Button btnClear = new Button("Clear");
        btnClear.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:6 14;-fx-cursor:hand;");
        btnClear.setOnAction(e -> logContainer.getChildren().clear());
        logControls.getChildren().addAll(btnPause, btnClear);

        content.getChildren().addAll(title, cards, btnRefresh, lblLog, logControls, logScroll);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private void loadStats() {
        AdminService.getInstance().requestDashboardStats(stats -> Platform.runLater(() -> {
            lblDriversVal.setText(String.valueOf(stats.getTotalDrivers()));
            lblPassengersVal.setText(String.valueOf(stats.getTotalPassengers()));
            lblActiveTripsVal.setText(String.valueOf(stats.getActiveTrips()));
            lblRevenueVal.setText(String.format("ETB %.0f", stats.getTotalRevenue()));
            appendLog("[STATS] Dashboard refreshed — " +
                stats.getTotalDrivers() + " drivers, " +
                stats.getActiveTrips() + " active trips, " +
                String.format("ETB %.0f", stats.getTotalRevenue()) + " revenue");
        }));
    }

    private void startLogSimulator() {
        // Wire real server messages to the log — no fake cycling entries
        AdminService.getInstance().setLogHandler(msg -> Platform.runLater(() -> appendLog(msg)));
        appendLog("[System] Dashboard log started — waiting for server events...");
    }

    private void appendLog(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        String upper = text.toUpperCase();
        if (upper.contains("ERROR"))
            lbl.setStyle("-fx-text-fill:#ef4444;-fx-font-family:monospace;-fx-font-size:11px;");
        else if (upper.contains("SUCCESS") || upper.contains("STATS"))
            lbl.setStyle("-fx-text-fill:#22c55e;-fx-font-family:monospace;-fx-font-size:11px;");
        else
            lbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-family:monospace;-fx-font-size:11px;");
        logContainer.getChildren().add(lbl);
        logScroll.setVvalue(1.0);
    }

    private void stopLog() { if (logTimer != null) logTimer.stop(); }

    private VBox statCard(String label, Label valueLabel, String sub, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                      "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");
        card.setPrefWidth(210);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94a3b8"));
        lbl.setFont(Font.font("Arial", 11));
        valueLabel.setTextFill(Color.web(color));
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        Label s = new Label(sub);
        s.setTextFill(Color.web("#475569"));
        s.setFont(Font.font("Arial", 11));
        card.getChildren().addAll(lbl, valueLabel, s);
        return card;
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
