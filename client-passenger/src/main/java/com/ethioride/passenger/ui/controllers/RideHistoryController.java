package com.ethioride.passenger.ui.controllers;

import com.ethioride.passenger.state.SessionState;
import com.ethioride.passenger.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class RideHistoryController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblUserPhone;
    @FXML private Label lblTotalTrips;
    @FXML private Label lblTotalSpent;
    @FXML private Label lblAvgRating;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilter;
    @FXML private VBox tripListContainer;

    // [tripId, date, pickup, dropoff, fare, status, category]
    private List<String[]> allTrips;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserSession();
        cbFilter.getItems().addAll("All", "COMPLETED", "CANCELLED");
        cbFilter.setValue("All");
        loadSampleTrips();
    }

    private void loadUserSession() {
        SessionState s = SessionState.getInstance();
        if (s.isLoggedIn()) {
            lblUserName.setText(s.getCurrentUser().getFullName());
            lblUserPhone.setText("+251 " + s.getCurrentUser().getPhone());
        } else {
            lblUserName.setText("Guest");
            lblUserPhone.setText("");
        }
    }

    private void loadSampleTrips() {
        allTrips = List.of(
            new String[]{"T-8821", "Apr 18, 2026  14:28", "Edna Mall, Bole",    "Sarbet, Old Airport", "145.00", "COMPLETED", "ECONOMY"},
            new String[]{"T-8820", "Apr 18, 2026  12:10", "Meskel Square",       "Bole Airport",        "320.00", "COMPLETED", "ELITE"},
            new String[]{"T-8817", "Apr 17, 2026  09:45", "Bole Medhanialem",    "Gerji",               "110.00", "CANCELLED", "ECONOMY"},
            new String[]{"T-8810", "Apr 16, 2026  18:30", "Piassa",              "Kazanchis",           "90.00",  "COMPLETED", "PREMIUM"},
            new String[]{"T-8805", "Apr 15, 2026  08:00", "CMC",                 "Megenagna",           "75.00",  "COMPLETED", "ECONOMY"}
        );
        renderTrips(allTrips);
        updateSummary();
    }

    private void renderTrips(List<String[]> trips) {
        tripListContainer.getChildren().clear();
        for (String[] t : trips) {
            tripListContainer.getChildren().add(buildTripCard(t));
        }
    }

    private HBox buildTripCard(String[] t) {
        HBox card = new HBox(16);
        card.setStyle("-fx-background-color:#1a2235; -fx-background-radius:12px; -fx-padding:16;");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Icon
        StackPane icon = new StackPane();
        icon.setStyle("-fx-background-color:#1e3a5f; -fx-background-radius:50%; -fx-min-width:44px; -fx-min-height:44px;");
        Label ico = new Label("COMPLETED".equals(t[5]) ? "✓" : "✕");
        ico.setStyle("COMPLETED".equals(t[5])
            ? "-fx-text-fill:#22c55e; -fx-font-size:16px; -fx-font-weight:bold;"
            : "-fx-text-fill:#ef4444; -fx-font-size:16px; -fx-font-weight:bold;");
        icon.getChildren().add(ico);

        // Route info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label route = new Label(t[2] + "  →  " + t[3]);
        route.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:13px; -fx-font-weight:bold;");
        Label meta = new Label(t[1] + "  •  " + t[6]);
        meta.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
        info.getChildren().addAll(route, meta);

        // Fare + status
        VBox right = new VBox(4);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label fare = new Label("ETB " + t[4]);
        fare.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:14px; -fx-font-weight:bold;");
        Label status = new Label(t[5]);
        status.setStyle("COMPLETED".equals(t[5])
            ? "-fx-text-fill:#22c55e; -fx-font-size:10px;"
            : "-fx-text-fill:#ef4444; -fx-font-size:10px;");
        right.getChildren().addAll(fare, status);

        card.getChildren().addAll(icon, info, right);
        return card;
    }

    private void updateSummary() {
        long completed = allTrips.stream().filter(t -> "COMPLETED".equals(t[5])).count();
        double total = allTrips.stream()
            .filter(t -> "COMPLETED".equals(t[5]))
            .mapToDouble(t -> Double.parseDouble(t[4])).sum();
        lblTotalTrips.setText(String.valueOf(completed));
        lblTotalSpent.setText(String.format("%.0f", total));
        lblAvgRating.setText("4.8 ★");
    }

    @FXML private void onSearch() { applyFilter(); }
    @FXML private void onFilter() { applyFilter(); }

    private void applyFilter() {
        String q = tfSearch.getText().toLowerCase();
        String status = cbFilter.getValue();
        List<String[]> filtered = allTrips.stream()
            .filter(t -> (q.isEmpty() || t[2].toLowerCase().contains(q) || t[3].toLowerCase().contains(q))
                      && ("All".equals(status) || t[5].equals(status)))
            .toList();
        renderTrips(filtered);
    }

    @FXML private void onNavMap()        { Navigator.navigateTo("/ui/views/main.fxml"); }
    @FXML private void onNavHistory()    { /* already here */ }
    @FXML private void onNavPayments()   { Navigator.navigateTo("/ui/views/payments.fxml"); }
    @FXML private void onNavPromotions() { Navigator.navigateTo("/ui/views/promotions.fxml"); }
    @FXML private void onNavSettings()   { Navigator.navigateTo("/ui/views/settings.fxml"); }
    @FXML private void onSupport()       { /* open support */ }

    @FXML private void onSignOut() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Sign Out"); a.setHeaderText("Sign out of EthioRide?");
        a.setContentText("You will be returned to the login screen.");
        a.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            SessionState.getInstance().clear();
            Navigator.navigateTo("/ui/views/login.fxml");
        }
    }
}
