package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
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
    private Label cardDrivers;
    private Label cardTrips;
    private Label cardLoad;
    private Label cardUptime;
    private VBox logContainer;
    private ScrollPane logScroll;
    private Timeline logTimer;
    private boolean paused = false;

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
        sub.setFont(Font.font("Arial", 11)); sub.setTextFill(Color.web("#475569"));
        sub.setPadding(new Insets(0, 20, 20, 20));
        Button btnDash    = navBtn("📊  Dashboard");
        Button btnDrivers = navBtn("🚗  Drivers");
        Button btnTrips   = navBtn("🗺  Trips");
        Button btnSystem  = navBtn("🖥  System");
        btnDash.setStyle(btnDash.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDrivers.setOnAction(e -> { stopLog(); new DriversScreen(stage).show(); });
        btnTrips.setOnAction(e   -> { stopLog(); new TripsScreen(stage).show(); });
        btnSystem.setOnAction(e  -> { stopLog(); new SystemScreen(stage).show(); });
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Label lblUser = new Label("👤 " + AdminSession.getInstance().getUsername());
        lblUser.setTextFill(Color.web("#f1f5f9")); lblUser.setFont(Font.font("Arial", 12));
        lblUser.setPadding(new Insets(8, 20, 4, 20));
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { stopLog(); AdminService.getInstance().disconnect(); AdminSession.getInstance().logout(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, sub, btnDash, btnDrivers, btnTrips, btnSystem, sp, lblUser, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("System Dashboard");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Stat cards — labels stored so they can be updated live
        HBox cards = new HBox(16);
        cardDrivers = new Label("—");
        cardTrips   = new Label("—");
        cardLoad    = new Label("—");
        cardUptime  = new Label("—");
        cards.getChildren().addAll(
            statCard("Active Drivers", cardDrivers, "+0%",       "#22c55e"),
            statCard("Ongoing Trips",  cardTrips,   "Peak Hour", "#f59e0b"),
            statCard("Server Load",    cardLoad,     "PULSE: OK", "#3b82f6"),
            statCard("Uptime",         cardUptime,   "latency",   "#a855f7")
        );

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

        content.getChildren().addAll(title, cards, lblLog, logControls, logScroll);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private void startLogSimulator() {
        AdminService svc = AdminService.getInstance();
        svc.setLogHandler(msg -> Platform.runLater(() -> appendLog(msg)));
        svc.setStatsHandler(stats -> Platform.runLater(() -> {
            cardDrivers.setText(String.valueOf(stats.getActiveDrivers()));
            cardTrips.setText(String.valueOf(stats.getOngoingTrips()));
            cardLoad.setText(String.format("%.0f%%", stats.getServerLoad() * 100));
            cardUptime.setText(String.format("%.2f%%", stats.getUptimePercent()));
        }));
        String[] samples = {
            "[SOCKET_AUTH] Connection received from Client ID: 0029-Px",
            "[DRIVER_LOC] Driver 4442 updated coordinates to 9.0302, 38.7469",
            "[ERROR] Database connection retry attempted (Attempt 1 of 3)",
            "[SUCCESS] Cache handshake verified.",
            "[TRIP_ACCEPTED] Driver 4442 accepted trip T-8821",
            "[HEARTBEAT] Server pulse OK — 24ms"
        };
        final int[] idx = {0};
        logTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (!paused) {
                String entry = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + samples[idx[0]++ % samples.length];
                appendLog(entry);
            }
        }));
        logTimer.setCycleCount(Timeline.INDEFINITE);
        logTimer.play();
    }

    private void appendLog(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        String upper = text.toUpperCase();
        if (upper.contains("ERROR"))   lbl.setStyle("-fx-text-fill:#ef4444;-fx-font-family:monospace;-fx-font-size:11px;");
        else if (upper.contains("SUCCESS")) lbl.setStyle("-fx-text-fill:#22c55e;-fx-font-family:monospace;-fx-font-size:11px;");
        else lbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-family:monospace;-fx-font-size:11px;");
        logContainer.getChildren().add(lbl);
        logScroll.setVvalue(1.0);
    }

    private void stopLog() { if (logTimer != null) logTimer.stop(); }

    private VBox statCard(String label, Label valueLabel, String sub, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");
        card.setPrefWidth(200);
        Label lbl = new Label(label); lbl.setTextFill(Color.web("#94a3b8")); lbl.setFont(Font.font("Arial", 11));
        valueLabel.setTextFill(Color.web(color)); valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        Label s   = new Label(sub);   s.setTextFill(Color.web("#475569")); s.setFont(Font.font("Arial", 11));
        card.getChildren().addAll(lbl, valueLabel, s);
        return card;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
