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

public class SystemScreen {
    private final Stage stage;
    private VBox logContainer;
    private ScrollPane logScroll;
    private Timeline logTimer;
    private boolean paused = false;

    public SystemScreen(Stage stage) { this.stage = stage; }

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
        logo.setPadding(new Insets(24, 20, 20, 20));
        Button btnDash    = navBtn("📊  Dashboard");
        Button btnDrivers = navBtn("🚗  Drivers");
        Button btnTrips   = navBtn("🗺  Trips");
        Button btnSystem  = navBtn("🖥  System");
        btnSystem.setStyle(btnSystem.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e    -> { stopLog(); new DashboardScreen(stage).show(); });
        btnDrivers.setOnAction(e -> { stopLog(); new DriversScreen(stage).show(); });
        btnTrips.setOnAction(e   -> { stopLog(); new TripsScreen(stage).show(); });
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { stopLog(); AdminService.getInstance().disconnect(); AdminSession.getInstance().logout(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnDash, btnDrivers, btnTrips, btnSystem, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("System Monitor");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Metrics
        HBox metrics = new HBox(16);
        metrics.getChildren().addAll(
            metricCard("TCP Requests", "12k",   "#3b82f6", 0.60),
            metricCard("WS Connections", "800", "#22c55e", 0.40),
            metricCard("Memory Usage",  "1.3GB","#f59e0b", 0.26),
            metricCard("Server Load",   "62%",  "#ef4444", 0.62)
        );

        // Actions
        HBox actions = new HBox(12);
        Button btnHeartbeat = new Button("Send Heartbeat");
        btnHeartbeat.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        btnHeartbeat.setOnAction(e -> AdminService.getInstance().sendHeartbeat(AdminSession.getInstance().getUsername()));
        Button btnRestart = new Button("Restart Server");
        btnRestart.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:#fca5a5;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        btnRestart.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "All active connections will be dropped.", ButtonType.OK, ButtonType.CANCEL);
            a.setTitle("Restart Server"); a.setHeaderText("Restart the EthioRide server?"); a.showAndWait();
        });
        actions.getChildren().addAll(btnHeartbeat, btnRestart);

        // Log
        Label lblLog = new Label("System Log");
        lblLog.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblLog.setTextFill(Color.web("#f1f5f9"));

        logContainer = new VBox(2);
        logContainer.setPadding(new Insets(10));
        logContainer.setStyle("-fx-background-color:#060a14;");
        logScroll = new ScrollPane(logContainer);
        logScroll.setFitToWidth(true);
        logScroll.setPrefHeight(300);
        logScroll.setStyle("-fx-background-color:#060a14;-fx-background:#060a14;");

        HBox logControls = new HBox(12);
        Button btnPause = new Button("Pause");
        btnPause.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:6 14;-fx-cursor:hand;");
        btnPause.setOnAction(e -> { paused = !paused; btnPause.setText(paused ? "Resume" : "Pause"); });
        Button btnClear = new Button("Clear");
        btnClear.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:6 14;-fx-cursor:hand;");
        btnClear.setOnAction(e -> logContainer.getChildren().clear());
        logControls.getChildren().addAll(btnPause, btnClear);

        content.getChildren().addAll(title, metrics, actions, lblLog, logControls, logScroll);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private void startLogSimulator() {
        AdminService.getInstance().setLogHandler(msg -> Platform.runLater(() -> appendLog(msg)));
        String[] samples = {
            "[SOCKET_AUTH] Connection received from Client ID: 0029-Px",
            "[DRIVER_LOC] Driver 4442 updated coordinates to 9.0302, 38.7469",
            "[ERROR] Database connection retry attempted (Attempt 1 of 3)",
            "[SUCCESS] Cache handshake verified.",
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
        if (upper.contains("ERROR"))        lbl.setStyle("-fx-text-fill:#ef4444;-fx-font-family:monospace;-fx-font-size:11px;");
        else if (upper.contains("SUCCESS")) lbl.setStyle("-fx-text-fill:#22c55e;-fx-font-family:monospace;-fx-font-size:11px;");
        else                                lbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-family:monospace;-fx-font-size:11px;");
        logContainer.getChildren().add(lbl);
        logScroll.setVvalue(1.0);
    }

    private void stopLog() { if (logTimer != null) logTimer.stop(); }

    private VBox metricCard(String label, String value, String color, double progress) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");
        card.setPrefWidth(200);
        Label lbl = new Label(label); lbl.setTextFill(Color.web("#94a3b8")); lbl.setFont(Font.font("Arial", 11));
        Label val = new Label(value); val.setTextFill(Color.web(color)); val.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        ProgressBar bar = new ProgressBar(progress); bar.setMaxWidth(Double.MAX_VALUE); bar.setPrefHeight(4);
        card.getChildren().addAll(lbl, val, bar);
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
