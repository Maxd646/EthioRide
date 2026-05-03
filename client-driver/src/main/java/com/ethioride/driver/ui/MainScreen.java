package com.ethioride.driver.ui;

import com.ethioride.driver.state.DriverSessionState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

public class MainScreen {
    private final Stage stage;
    private Label lblShiftEarnings;
    private ProgressBar earningsProgress;
    private ToggleButton toggleOnline;
    private StackPane requestOverlay;
    private Label lblCountdown;
    private int countdownSeconds = 11;
    private Timeline countdownTimer;

    public MainScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildCenter());

        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");

        Label logo = new Label("🚗 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#22c55e"));
        logo.setPadding(new Insets(24, 20, 4, 20));

        Label sub = new Label("Driver Dashboard");
        sub.setFont(Font.font("Arial", 11));
        sub.setTextFill(Color.web("#475569"));
        sub.setPadding(new Insets(0, 20, 20, 20));

        Button btnMap      = navBtn("🗺  Live Map");
        Button btnHistory  = navBtn("🕐  Ride History");
        Button btnEarnings = navBtn("💰  Earnings");
        Button btnPayments = navBtn("💳  Payments");
        Button btnFleet    = navBtn("🚘  Fleet");
        btnMap.setStyle(btnMap.getStyle() + "-fx-background-color:#1e3a5f;");

        btnHistory.setOnAction(e  -> new RideHistoryScreen(stage).show());
        btnEarnings.setOnAction(e -> new EarningsScreen(stage).show());
        btnPayments.setOnAction(e -> new PaymentsScreen(stage).show());
        btnFleet.setOnAction(e    -> new FleetScreen(stage).show());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        String name = DriverSessionState.getInstance().getCurrentDriver() != null
            ? DriverSessionState.getInstance().getCurrentDriver().getFullName() : "Driver";

        Label lblName = new Label("👤 " + name);
        lblName.setTextFill(Color.web("#f1f5f9"));
        lblName.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblName.setPadding(new Insets(8, 20, 2, 20));

        Button btnSignOut = navBtn("↩  Sign Out");
        btnSignOut.setOnAction(e -> onSignOut());

        sidebar.getChildren().addAll(logo, sub, btnMap, btnHistory, btnEarnings, btnPayments, btnFleet, spacer, lblName, btnSignOut);
        return sidebar;
    }

    private StackPane buildCenter() {
        StackPane center = new StackPane();
        center.setStyle("-fx-background-color:#0d1526;");

        // Map placeholder
        Label mapLabel = new Label("[ Live Map Area ]");
        mapLabel.setTextFill(Color.web("#1e3a5f"));
        mapLabel.setFont(Font.font("Arial", 18));

        // Status panel (top-left)
        VBox statusPanel = buildStatusPanel();
        StackPane.setAlignment(statusPanel, Pos.TOP_LEFT);
        StackPane.setMargin(statusPanel, new Insets(20));

        // Request overlay (center)
        requestOverlay = buildRequestOverlay();
        requestOverlay.setVisible(false);
        requestOverlay.setManaged(false);

        center.getChildren().addAll(mapLabel, statusPanel, requestOverlay);
        return center;
    }

    private VBox buildStatusPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");
        panel.setMaxWidth(260);

        toggleOnline = new ToggleButton("OFFLINE ○");
        toggleOnline.setMaxWidth(Double.MAX_VALUE);
        toggleOnline.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        toggleOnline.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#94a3b8;-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
        toggleOnline.setOnAction(e -> {
            boolean online = toggleOnline.isSelected();
            DriverSessionState.getInstance().setOnline(online);
            toggleOnline.setText(online ? "ONLINE ●" : "OFFLINE ○");
            toggleOnline.setStyle(online
                ? "-fx-background-color:#166534;-fx-text-fill:#22c55e;-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;"
                : "-fx-background-color:#1e3a5f;-fx-text-fill:#94a3b8;-fx-background-radius:8px;-fx-padding:10px;-fx-cursor:hand;");
        });

        Label lblEarningsTitle = new Label("Shift Earnings");
        lblEarningsTitle.setTextFill(Color.web("#94a3b8"));
        lblEarningsTitle.setFont(Font.font("Arial", 12));

        lblShiftEarnings = new Label("ETB 0");
        lblShiftEarnings.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblShiftEarnings.setTextFill(Color.web("#22c55e"));

        earningsProgress = new ProgressBar(0);
        earningsProgress.setMaxWidth(Double.MAX_VALUE);
        earningsProgress.setPrefHeight(6);

        panel.getChildren().addAll(toggleOnline, lblEarningsTitle, lblShiftEarnings, earningsProgress);
        return panel;
    }

    private StackPane buildRequestOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.7);");

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(340);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#3b82f6;-fx-border-radius:16px;-fx-background-radius:16px;-fx-padding:24;");

        lblCountdown = new Label("11s");
        lblCountdown.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblCountdown.setTextFill(Color.web("#f59e0b"));

        Label lblFareTitle = new Label("New Ride Request");
        lblFareTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblFareTitle.setTextFill(Color.web("#f1f5f9"));

        Label lblFare = new Label("ETB 145.00");
        lblFare.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblFare.setTextFill(Color.web("#22c55e"));

        Label lblPickup = new Label("Pickup: Meskel Square");
        lblPickup.setTextFill(Color.web("#94a3b8"));
        lblPickup.setFont(Font.font("Arial", 13));

        Label lblDropoff = new Label("Drop-off: Bole Airport");
        lblDropoff.setTextFill(Color.web("#94a3b8"));
        lblDropoff.setFont(Font.font("Arial", 13));

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);
        Button btnAccept = new Button("Accept");
        btnAccept.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnAccept.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-background-radius:8px;-fx-padding:12 24;-fx-cursor:hand;");
        btnAccept.setOnAction(e -> onAccept());

        Button btnDecline = new Button("Decline");
        btnDecline.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnDecline.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:8px;-fx-padding:12 24;-fx-cursor:hand;");
        btnDecline.setOnAction(e -> dismissRequest());

        buttons.getChildren().addAll(btnAccept, btnDecline);
        card.getChildren().addAll(lblCountdown, lblFareTitle, lblFare, lblPickup, lblDropoff, buttons);
        overlay.getChildren().add(card);
        return overlay;
    }

    private void onAccept() {
        if (countdownTimer != null) countdownTimer.stop();
        DriverSessionState.getInstance().addEarnings(145.0);
        double earnings = DriverSessionState.getInstance().getShiftEarnings();
        lblShiftEarnings.setText(String.format("ETB %.0f", earnings));
        earningsProgress.setProgress(Math.min(earnings / 5000.0, 1.0));
        dismissRequest();
    }

    private void dismissRequest() {
        if (countdownTimer != null) countdownTimer.stop();
        requestOverlay.setVisible(false);
        requestOverlay.setManaged(false);
    }

    private void onSignOut() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "End your shift and sign out?", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Sign Out");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                DriverSessionState.getInstance().clear();
                new LoginScreen(stage).show();
            }
        });
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
