package com.ethioride.admin.controller;

import com.ethioride.admin.model.DashboardStats;
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

public class DashboardController implements Initializable {

    @FXML private Label lblHealthStatus;
    @FXML private Label lblHealthSubtitle;
    @FXML private Label lblLatency;
    @FXML private Label lblUptime;

    @FXML private Label lblActiveDrivers;
    @FXML private Label lblDriversDelta;
    @FXML private ProgressBar driversUtilization;

    @FXML private Label lblOngoingTrips;
    @FXML private Label lblPeakHour;
    @FXML private ProgressBar tripsProgress;

    @FXML private Label lblHeartbeat;
    @FXML private ProgressBar serverLoad;

    @FXML private VBox logContainer;
    @FXML private ScrollPane logScrollPane;
    @FXML private Label lblTcpReqs;
    @FXML private Label lblWsConnections;
    @FXML private Label lblMemUsage;

    @FXML private ComboBox<String> cbLanguage;
    @FXML private ComboBox<String> cbScript;
    @FXML private Label lblLocalizationNote;
    @FXML private Label lblProjectedDemand;

    private Timeline logSimulator;
    private boolean paused = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindAdminService();
        loadStats(buildSampleStats());
        populateLocalization();
        startLogSimulator();
    }

    private void bindAdminService() {
        AdminService.getInstance().setLogHandler(msg ->
            Platform.runLater(() -> appendLog(msg, classifyLog(msg)))
        );
    }

    private void loadStats(DashboardStats s) {
        lblHealthStatus.setText("Optimal");
        lblHealthSubtitle.setText("የስርዓት ሁኔታ ጥሩ ነው (Current System Status: Normal)");
        lblLatency.setText(s.getLatencyMs() + "ms");
        lblUptime.setText(String.format("%.2f%%", s.getUptimePercent()));

        lblActiveDrivers.setText(String.format("%,d", s.getActiveDrivers()));
        lblDriversDelta.setText("+" + s.getDriversDelta() + "%");
        driversUtilization.setProgress(s.getDriversUtilization());

        lblOngoingTrips.setText(String.valueOf(s.getOngoingTrips()));
        lblPeakHour.setText(s.isPeakHour() ? "Peak Hour" : "");
        tripsProgress.setProgress((double) s.getOngoingTrips() / s.getTripTarget());

        lblHeartbeat.setText(s.getHeartbeatStatus());
        serverLoad.setProgress(s.getServerLoad());

        lblTcpReqs.setText("TCP " + s.getTcpRequests() + "k reqs");
        lblWsConnections.setText("WS " + s.getWsConnections() + " connections");
        lblMemUsage.setText("MEM " + s.getMemUsedMb() + "MB / " + s.getMemTotalMb() + "MB");

        lblProjectedDemand.setText("+" + s.getProjectedDemandDelta() + "%");
    }

    private DashboardStats buildSampleStats() {
        DashboardStats s = new DashboardStats();
        s.setActiveDrivers(1284);
        s.setDriversDelta(12);
        s.setDriversUtilization(0.88);
        s.setOngoingTrips(452);
        s.setTripTarget(500);
        s.setPeakHour(true);
        s.setHeartbeatStatus("PULSE : OK");
        s.setServerLoad(0.62);
        s.setLatencyMs(24);
        s.setUptimePercent(99.98);
        s.setTcpRequests(12);
        s.setWsConnections(800);
        s.setMemUsedMb(1300);
        s.setMemTotalMb(5000);
        s.setProjectedDemandDelta(8.1);
        return s;
    }

    private void populateLocalization() {
        cbLanguage.getItems().addAll("English (US)", "አማርኛ (Amharic)");
        cbLanguage.setValue("English (US)");
        cbScript.getItems().addAll("አማርኛ (Amharic)", "English");
        cbScript.setValue("አማርኛ (Amharic)");
        lblLocalizationNote.setText(
            "Dashboard translates live logs and passenger feedback strings into Ethiopic script for regional dispatchers."
        );
    }

    private void startLogSimulator() {
        String[] samples = {
            "[SOCKET_AUTH] Connection received from Client ID: 0029-Px",
            "[DRIVER_LOC] Driver 4442 updated coordinates to 9.0302, 38.7469",
            "[SOCKET_EMIT] Global surge price update triggered (Addis Ababa Central)",
            "[ERROR] Database connection retry attempted (Attempt 1 of 3)",
            "[SUCCESS] Redis cache handshake verified.",
            "[PASSENGER_REQ] Request for 9L ride from Bole Int. Airport",
            "[SOCKET_AUTH] Connection received from Client ID: 7dFp",
            "[DRIVER_LOC] Driver 1187 updated coordinates to 9.0145, 38.7612",
            "[TRIP_ACCEPTED] Driver 4442 accepted trip T-8821",
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
        // Auto-scroll to bottom
        logScrollPane.setVvalue(1.0);
    }

    private String classifyLog(String text) {
        String upper = text.toUpperCase();
        if (upper.contains("ERROR"))   return "error";
        if (upper.contains("SUCCESS")) return "success";
        return "normal";
    }

    @FXML private void onPauseStream() { paused = !paused; }
    @FXML private void onClearLogs()   { logContainer.getChildren().clear(); }
    @FXML private void onLanguageChange() { /* apply locale */ }
    @FXML private void onScriptChange()   { /* apply script */ }
    @FXML private void onRemoveScript()   { cbScript.setValue(null); }
    @FXML private void onEmergencyAlert() { /* broadcast emergency */ }
    @FXML private void onTabFleet()    { /* fleet tab */ }
    @FXML private void onTabDrivers()  { AdminNavigator.navigateTo("/ui/drivers.fxml"); }
    @FXML private void onTabEarnings() { /* earnings tab */ }
    @FXML private void onTabLiveMap()  { /* live map tab */ }
    @FXML private void onNavDashboard()  { /* already here */ }
    @FXML private void onNavTrips()      { AdminNavigator.navigateTo("/ui/trips.fxml"); }
    @FXML private void onNavPayments()   { /* navigate */ }
    @FXML private void onNavPromotions() { /* navigate */ }
    @FXML private void onNavSystem()     { AdminNavigator.navigateTo("/ui/system.fxml"); }
    @FXML private void onSupport()       { /* open support */ }

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
