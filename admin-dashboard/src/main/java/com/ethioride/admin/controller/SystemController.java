package com.ethioride.admin.controller;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class SystemController implements Initializable {

    @FXML private Label lblTcpReqs;
    @FXML private Label lblWsConns;
    @FXML private Label lblMemUsage;
    @FXML private Label lblServerLoad;

    @FXML private ProgressBar tcpProgress;
    @FXML private ProgressBar wsProgress;
    @FXML private ProgressBar memProgress;
    @FXML private ProgressBar loadProgress;

    @FXML private VBox logContainer;
    @FXML private ScrollPane logScrollPane;

    private Timeline logSimulator;
    private boolean paused = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMetrics();
        bindAdminService();
        startLogSimulator();
    }

    private void loadMetrics() {
        lblTcpReqs.setText("12k");
        lblWsConns.setText("800");
        lblMemUsage.setText("1.3GB");
        lblServerLoad.setText("62%");

        tcpProgress.setProgress(0.60);
        wsProgress.setProgress(0.40);
        memProgress.setProgress(0.26);
        loadProgress.setProgress(0.62);
    }

    private void bindAdminService() {
        AdminService.getInstance().setLogHandler(msg ->
            Platform.runLater(() -> appendLog(msg, classifyLog(msg)))
        );
    }

    private void startLogSimulator() {
        String[] samples = {
            "[SOCKET_AUTH] Connection received from Client ID: 0029-Px",
            "[DRIVER_LOC] Driver 4442 updated coordinates to 9.0302, 38.7469",
            "[ERROR] Database connection retry attempted (Attempt 1 of 3)",
            "[SUCCESS] Redis cache handshake verified.",
            "[TRIP_ACCEPTED] Driver 4442 accepted trip T-8821",
            "[HEARTBEAT] Server pulse OK — 24ms",
        };
        final int[] idx = {0};
        logSimulator = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (!paused) {
                String entry = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                               + samples[idx[0] % samples.length];
                appendLog(entry, classifyLog(entry));
                idx[0]++;
            }
        }));
        logSimulator.setCycleCount(Timeline.INDEFINITE);
        logSimulator.play();
    }

    private void appendLog(String text, String type) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle(switch (type) {
            case "error"   -> "-fx-text-fill:#ef4444; -fx-font-family:'Consolas',monospace; -fx-font-size:11px;";
            case "success" -> "-fx-text-fill:#22c55e; -fx-font-family:'Consolas',monospace; -fx-font-size:11px;";
            default        -> "-fx-text-fill:#94a3b8; -fx-font-family:'Consolas',monospace; -fx-font-size:11px;";
        });
        logContainer.getChildren().add(lbl);
        logScrollPane.setVvalue(1.0);
    }

    private String classifyLog(String text) {
        String u = text.toUpperCase();
        if (u.contains("ERROR"))   return "error";
        if (u.contains("SUCCESS")) return "success";
        return "normal";
    }

    @FXML private void onPause()  { paused = !paused; }
    @FXML private void onClear()  { logContainer.getChildren().clear(); }
    @FXML private void onExport() { /* TODO: export logs to data/logs/ */ }

    @FXML
    private void onSendHeartbeat() {
        AdminService.getInstance().sendHeartbeat(AdminSession.getInstance().getUsername());
    }

    @FXML
    private void onRestartServer() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restart Server");
        confirm.setHeaderText("Restart the EthioRide server?");
        confirm.setContentText("All active connections will be dropped.");
        confirm.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        confirm.showAndWait();
        // TODO: send restart command via AdminSocketClient
    }

    @FXML private void onNavDashboard() { if (logSimulator != null) logSimulator.stop(); AdminNavigator.navigateTo("/ui/dashboard.fxml"); }
    @FXML private void onNavDrivers()   { if (logSimulator != null) logSimulator.stop(); AdminNavigator.navigateTo("/ui/drivers.fxml"); }
    @FXML private void onNavTrips()     { if (logSimulator != null) logSimulator.stop(); AdminNavigator.navigateTo("/ui/trips.fxml"); }
    @FXML private void onNavSystem()    { /* already here */ }
    @FXML private void onSupport()      { /* open support */ }

    @FXML
    private void onSignOut() {
        if (logSimulator != null) logSimulator.stop();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of Admin Dashboard?");
        confirm.setContentText("Your session will be terminated.");
        confirm.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            AdminService.getInstance().disconnect();
            AdminSession.getInstance().logout();
            AdminNavigator.navigateTo("/ui/admin_login.fxml");
        }
    }
}
