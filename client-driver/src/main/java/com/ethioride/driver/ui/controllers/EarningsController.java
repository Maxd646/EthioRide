package com.ethioride.driver.ui.controllers;

import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.driver.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EarningsController implements Initializable {

    @FXML private Label lblWeekEarnings;
    @FXML private Label lblMonthEarnings;
    @FXML private Label lblWeekTrips;
    @FXML private Label lblOnlineHours;
    @FXML private ComboBox<String> cbPeriod;
    @FXML private VBox dailyContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbPeriod.getItems().addAll("This Week", "This Month", "Last Month", "All Time");
        cbPeriod.setValue("This Week");
        loadStats();
        loadDailyBreakdown();
    }

    private void loadStats() {
        lblWeekEarnings.setText("8,450");
        lblMonthEarnings.setText("32,100");
        lblWeekTrips.setText("42");
        lblOnlineHours.setText("38h");
    }

    private void loadDailyBreakdown() {
        List<String[]> days = List.of(
            new String[]{"Mon Apr 14", "8",  "1,200"},
            new String[]{"Tue Apr 15", "7",  "1,050"},
            new String[]{"Wed Apr 16", "9",  "1,350"},
            new String[]{"Thu Apr 17", "6",  "900"},
            new String[]{"Fri Apr 18", "12", "2,450"},
            new String[]{"Sat Apr 19", "0",  "0"},
            new String[]{"Sun Apr 20", "0",  "0"}
        );
        for (String[] d : days) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent; -fx-border-width:0 0 1 0; -fx-padding:10 0;");

            Label day = new Label(d[0]);
            day.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-min-width:140px;");

            Label trips = new Label(d[1] + " trips");
            trips.setStyle("-fx-text-fill:#475569; -fx-font-size:12px;");
            HBox.setHgrow(trips, Priority.ALWAYS);

            // Progress bar proportional to max day
            double progress = Double.parseDouble(d[2]) / 2500.0;
            ProgressBar bar = new ProgressBar(progress);
            bar.setPrefWidth(160);
            bar.setPrefHeight(6);
            HBox.setHgrow(bar, Priority.NEVER);

            Label amount = new Label("ETB " + d[2]);
            amount.setStyle("0".equals(d[2])
                ? "-fx-text-fill:#475569; -fx-font-size:13px; -fx-font-weight:bold; -fx-min-width:100px;"
                : "-fx-text-fill:#22c55e; -fx-font-size:13px; -fx-font-weight:bold; -fx-min-width:100px;");
            amount.setAlignment(Pos.CENTER_RIGHT);

            row.getChildren().addAll(day, trips, bar, amount);
            dailyContainer.getChildren().add(row);
        }
    }

    @FXML private void onPeriodChange() { dailyContainer.getChildren().clear(); loadStats(); loadDailyBreakdown(); }
    @FXML private void onTabLiveMap()   { Navigator.navigateTo("/ui/views/driver_main.fxml"); }
    @FXML private void onTabFleet()     { Navigator.navigateTo("/ui/views/fleet.fxml"); }
    @FXML private void onTabEarnings()  { /* already here */ }
    @FXML private void onTabDrivers()   { Navigator.navigateTo("/ui/views/fleet.fxml"); }
    @FXML private void onEmergency()    { /* broadcast emergency */ }
    @FXML private void onNavMap()        { Navigator.navigateTo("/ui/views/driver_main.fxml"); }
    @FXML private void onNavHistory()    { Navigator.navigateTo("/ui/views/ride_history.fxml"); }
    @FXML private void onNavPayments()   { Navigator.navigateTo("/ui/views/payments.fxml"); }
    @FXML private void onNavPromotions() { /* navigate */ }
    @FXML private void onNavSettings()   { /* navigate */ }
    @FXML private void onSupport()       { /* open support */ }

    @FXML private void onSignOut() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Sign Out"); a.setHeaderText("End your shift and sign out?");
        a.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            DriverSessionState.getInstance().clear();
            Navigator.navigateTo("/ui/views/driver_login.fxml");
        }
    }
}
