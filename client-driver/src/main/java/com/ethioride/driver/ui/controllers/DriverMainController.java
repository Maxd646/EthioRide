package com.ethioride.driver.ui.controllers;

import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.driver.ui.navigation.Navigator;
import com.ethioride.shared.dto.TripRequestDTO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class DriverMainController implements Initializable {

    @FXML private ToggleButton toggleOnline;
    @FXML private StackPane requestOverlay;

    // Request card fields
    @FXML private Label lblCountdown;
    @FXML private Label lblRequestFare;
    @FXML private Label lblPickupTag;
    @FXML private Label lblPickupLocation;
    @FXML private Label lblDropoffTag;
    @FXML private Label lblDropoffLocation;
    @FXML private Label lblPassengerName;
    @FXML private Label lblPassengerRating;
    @FXML private Label lblDistance;

    // Earnings
    @FXML private Label lblShiftEarnings;
    @FXML private ProgressBar earningsProgress;

    private TripRequestDTO pendingRequest;
    private Timeline countdownTimer;
    private int countdownSeconds = 11;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateEarningsDisplay();
        toggleOnline.setText("ONLINE STATUS");
    }

    /** Called by the network layer when a new trip request arrives. */
    public void showTripRequest(TripRequestDTO request) {
        this.pendingRequest = request;
        countdownSeconds = 11;

        lblRequestFare.setText(String.format("ETB %.2f", request.getFare()));
        lblPickupTag.setText("PICKUP • ጅምር");
        lblPickupLocation.setText(request.getPickupLocation());
        lblDropoffTag.setText("DROP-OFF • መዳረሻ");
        lblDropoffLocation.setText(request.getDropoffLocation());
        lblDistance.setText(String.format("%.1f km", request.getDistanceKm()));
        lblPassengerName.setText("Passenger");
        lblPassengerRating.setText("★ 4.9 (124 rides)");

        requestOverlay.setVisible(true);
        requestOverlay.setManaged(true);
        startCountdown();
    }

    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdownSeconds--;
            lblCountdown.setText(countdownSeconds + "s");
            if (countdownSeconds <= 0) {
                countdownTimer.stop();
                dismissRequest();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        lblCountdown.setText(countdownSeconds + "s");
        countdownTimer.play();
    }

    @FXML
    private void onAcceptRequest() {
        if (countdownTimer != null) countdownTimer.stop();
        dismissRequest();
        // TODO: send TRIP_ACCEPTED message via NetworkClient
        DriverSessionState.getInstance().addEarnings(pendingRequest != null ? pendingRequest.getFare() : 0);
        updateEarningsDisplay();
    }

    @FXML
    private void onDeclineRequest() {
        if (countdownTimer != null) countdownTimer.stop();
        dismissRequest();
        // TODO: send TRIP_DECLINED message via NetworkClient
    }

    private void dismissRequest() {
        requestOverlay.setVisible(false);
        requestOverlay.setManaged(false);
        pendingRequest = null;
    }

    private void updateEarningsDisplay() {
        double earnings = DriverSessionState.getInstance().getShiftEarnings();
        lblShiftEarnings.setText(String.format("ETB %.0f", earnings));
        earningsProgress.setProgress(Math.min(earnings / 5000.0, 1.0));
    }

    @FXML private void onToggleOnline() {
        boolean online = toggleOnline.isSelected();
        DriverSessionState.getInstance().setOnline(online);
        toggleOnline.setText(online ? "ONLINE ●" : "OFFLINE ○");
    }

    @FXML private void onEmergencyAlert() {
        // TODO: send emergency signal to server
    }

    @FXML private void onTabLiveMap()  { /* already on map */ }
    @FXML private void onTabFleet()    { /* TODO: navigate to fleet */ }
    @FXML private void onTabEarnings() { /* TODO: navigate to earnings */ }
    @FXML private void onTabDrivers()  { /* TODO: navigate to drivers list */ }

    @FXML private void onNavMap()        { /* already on map */ }
    @FXML private void onNavHistory()    { /* navigate */ }
    @FXML private void onNavPayments()   { /* navigate */ }
    @FXML private void onNavPromotions() { /* navigate */ }
    @FXML private void onNavSettings()   { /* navigate */ }
    @FXML private void onSupport()       { /* open support */ }
    @FXML private void onSignOut() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("End your shift and sign out?");
        confirm.setContentText("Any active trip request will be dismissed.");
        styleDialog(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (countdownTimer != null) countdownTimer.stop();
            DriverSessionState.getInstance().clear();
            Navigator.navigateTo("/ui/views/driver_login.fxml");
        }
    }

    private void styleDialog(Alert alert) {
        alert.getDialogPane().setStyle(
            "-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;"
        );
    }
}
