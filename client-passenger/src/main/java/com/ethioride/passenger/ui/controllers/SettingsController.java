package com.ethioride.passenger.ui.controllers;

import com.ethioride.passenger.state.SessionState;
import com.ethioride.passenger.ui.navigation.Navigator;
import com.ethioride.shared.utils.I18n;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private Label lblProfileName;
    @FXML private Label lblProfilePhone;
    @FXML private Label lblProfileEmail;
    @FXML private ComboBox<String> cbLanguage;
    @FXML private CheckBox cbRideUpdates;
    @FXML private CheckBox cbPromoNotifs;
    @FXML private CheckBox cbArrivalAlerts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProfile();
        cbLanguage.getItems().addAll("English (US)", "አማርኛ (Amharic)");
        cbLanguage.setValue("English (US)");
    }

    private void loadProfile() {
        SessionState s = SessionState.getInstance();
        if (s.isLoggedIn()) {
            lblProfileName.setText(s.getCurrentUser().getFullName());
            lblProfilePhone.setText("+251 " + s.getCurrentUser().getPhone());
            lblProfileEmail.setText(s.getCurrentUser().getEmail() != null
                ? s.getCurrentUser().getEmail() : "—");
        } else {
            lblProfileName.setText("Guest");
            lblProfilePhone.setText("—");
            lblProfileEmail.setText("—");
        }
    }

    @FXML private void onEditProfile() { /* open edit profile dialog */ }

    @FXML private void onLanguageChange() {
        if ("አማርኛ (Amharic)".equals(cbLanguage.getValue())) {
            I18n.setLocale(new Locale("am"));
        } else {
            I18n.setLocale(Locale.ENGLISH);
        }
        I18n.load("i18n/messages");
    }

    @FXML private void onChangePassword() { /* open change password dialog */ }

    @FXML private void onSave() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Settings Saved");
        a.setHeaderText(null);
        a.setContentText("Your settings have been saved.");
        a.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        a.showAndWait();
    }

    @FXML private void onNavMap()        { Navigator.navigateTo("/ui/views/main.fxml"); }
    @FXML private void onNavHistory()    { Navigator.navigateTo("/ui/views/ride_history.fxml"); }
    @FXML private void onNavPayments()   { Navigator.navigateTo("/ui/views/payments.fxml"); }
    @FXML private void onNavPromotions() { Navigator.navigateTo("/ui/views/promotions.fxml"); }
    @FXML private void onNavSettings()   { /* already here */ }
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
