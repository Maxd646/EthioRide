package com.ethioride.passenger.ui.controllers;

import com.ethioride.passenger.state.SessionState;
import com.ethioride.passenger.ui.navigation.Navigator;
import com.ethioride.shared.constants.AppConstants;
import com.ethioride.shared.enums.RideCategory;
import com.ethioride.shared.utils.I18n;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // Top bar
    @FXML private Label lblCurrentLocation;
    @FXML private Button btnBookNow;

    // Sidebar
    @FXML private Label lblSuiteTitle;
    @FXML private Button btnNavMap;
    @FXML private Button btnNavHistory;
    @FXML private Button btnNavPayments;
    @FXML private Button btnNavPromotions;
    @FXML private Button btnNavSettings;
    @FXML private Label lblUserName;
    @FXML private Label lblUserPhone;
    @FXML private Button btnSupport;
    @FXML private Button btnSignOut;

    // Booking panel
    @FXML private Label lblWhereTo;
    @FXML private Label lblWhereToAmharic;
    @FXML private Label lblPickup;
    @FXML private Label lblDestination;
    @FXML private TextField tfPickup;
    @FXML private TextField tfDestination;
    @FXML private Button btnRequestRide;
    @FXML private TextField tfPassengerPhone;
    @FXML private Label lblPriceSurge;

    // Ride tiles
    @FXML private VBox tileEconomy;
    @FXML private VBox tilePremium;
    @FXML private VBox tileElite;
    @FXML private Label lblEconomy;
    @FXML private Label lblPremium;
    @FXML private Label lblElite;
    @FXML private Label lblEconomyFare;
    @FXML private Label lblPremiumFare;
    @FXML private Label lblEliteFare;

    // Quick locations
    @FXML private Label lblHome;
    @FXML private Label lblHomeAddr;
    @FXML private Label lblWork;
    @FXML private Label lblWorkAddr;
    @FXML private Label lblFavorites;
    @FXML private Label lblFavoritesAddr;

    private RideCategory selectedCategory = RideCategory.ECONOMY;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        loadUserSession();
        showPriceSurge(1.2);
    }

    private void applyI18n() {
        lblSuiteTitle.setText(I18n.get("app.subtitle"));
        btnNavMap.setText("🗺  " + I18n.get("nav.map"));
        btnNavHistory.setText("🕐  " + I18n.get("nav.history"));
        btnNavPayments.setText("💳  " + I18n.get("nav.payments"));
        btnNavPromotions.setText("🏷  " + I18n.get("nav.promotions"));
        btnNavSettings.setText("⚙  " + I18n.get("nav.settings"));
        btnSupport.setText("📍  " + I18n.get("nav.support"));
        btnSignOut.setText("↩  " + I18n.get("nav.signout"));

        lblWhereTo.setText(I18n.get("map.where.to"));
        lblWhereToAmharic.setText("የት ወደ ትሄዳለህ?");
        lblPickup.setText(I18n.get("map.pickup") + " • ጅምር");
        lblDestination.setText(I18n.get("map.destination") + " • መዳረሻ");
        tfDestination.setPromptText(I18n.get("map.destination.placeholder"));
        btnRequestRide.setText(I18n.get("map.request.ride") + " • ጉዞ ጠይቅ");
        btnBookNow.setText(I18n.get("booking.header"));

        lblEconomy.setText(I18n.get("ride.economy"));
        lblPremium.setText(I18n.get("ride.premium"));
        lblElite.setText(I18n.get("ride.elite"));
        lblEconomyFare.setText("ETB " + (int) AppConstants.ECONOMY_BASE_FARE);
        lblPremiumFare.setText("ETB " + (int) AppConstants.PREMIUM_BASE_FARE);
        lblEliteFare.setText("ETB " + (int) AppConstants.ELITE_BASE_FARE);

        lblHome.setText(I18n.get("location.home"));
        lblWork.setText(I18n.get("location.work"));
        lblFavorites.setText(I18n.get("location.favorites"));
        lblHomeAddr.setText("Garnet Avenue");
        lblWorkAddr.setText("Kazanchis Square");
        lblFavoritesAddr.setText("Friendship Park");
    }

    private void loadUserSession() {
        SessionState session = SessionState.getInstance();
        if (session.isLoggedIn()) {
            lblUserName.setText(session.getCurrentUser().getFullName());
            lblUserPhone.setText(AppConstants.COUNTRY_CODE + " " + session.getCurrentUser().getPhone());
            lblCurrentLocation.setText("Bole International Airport");
            tfPickup.setText("Meskel Square, Addis Ababa");
        } else {
            lblUserName.setText("Guest");
            lblUserPhone.setText("");
            lblCurrentLocation.setText("Addis Ababa");
        }
    }

    private void showPriceSurge(double multiplier) {
        if (multiplier > 1.0) {
            lblPriceSurge.setText("⚡ PRICE SURGE • " + multiplier + "X");
            lblPriceSurge.setVisible(true);
            lblPriceSurge.setManaged(true);
        }
    }

    @FXML private void onSelectEconomy() { selectCategory(RideCategory.ECONOMY); }
    @FXML private void onSelectPremium() { selectCategory(RideCategory.PREMIUM); }
    @FXML private void onSelectElite()   { selectCategory(RideCategory.ELITE); }

    private void selectCategory(RideCategory cat) {
        selectedCategory = cat;
        tileEconomy.getStyleClass().remove("ride-tile-selected");
        tilePremium.getStyleClass().remove("ride-tile-selected");
        tileElite.getStyleClass().remove("ride-tile-selected");
        switch (cat) {
            case ECONOMY -> tileEconomy.getStyleClass().add("ride-tile-selected");
            case PREMIUM -> tilePremium.getStyleClass().add("ride-tile-selected");
            case ELITE   -> tileElite.getStyleClass().add("ride-tile-selected");
        }
    }

    @FXML
    private void onRequestRide() {
        String pickup = tfPickup.getText().trim();
        String destination = tfDestination.getText().trim();
        if (pickup.isEmpty() || destination.isEmpty()) return;
        // TODO: send TripRequestDTO via NetworkClient
    }

    @FXML private void onClearPhone() { tfPassengerPhone.clear(); }
    @FXML private void onBookNow() { /* scroll to booking panel */ }
    @FXML private void onNavMap() { /* already on map */ }
    @FXML private void onNavHistory() { /* navigate to history */ }
    @FXML private void onNavPayments() { /* navigate to payments */ }
    @FXML private void onNavPromotions() { /* navigate to promotions */ }
    @FXML private void onNavSettings() { /* navigate to settings */ }
    @FXML private void onSupport() { /* open support */ }
    @FXML private void onSignOut() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of EthioRide?");
        confirm.setContentText("You will be returned to the login screen.");
        styleDialog(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SessionState.getInstance().clear();
            Navigator.navigateTo("/ui/views/login.fxml");
        }
    }

    private void styleDialog(Alert alert) {
        alert.getDialogPane().setStyle(
            "-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;"
        );
    }
    @FXML private void onQuickHome() { tfDestination.setText(lblHomeAddr.getText()); }
    @FXML private void onQuickWork() { tfDestination.setText(lblWorkAddr.getText()); }
    @FXML private void onQuickFavorites() { tfDestination.setText(lblFavoritesAddr.getText()); }
    @FXML private void onNotification() { /* show notifications */ }
}
