package com.ethioride.passenger.ui.controllers;

import com.ethioride.passenger.state.SessionState;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
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
        cbLanguage.getItems().add("English (US)");
        cbLanguage.setValue("English (US)");
    }

    private void loadProfile() {
        SessionState s = SessionState.getInstance();
        if (s.isLoggedIn()) {
            lblProfileName.setText(s.getCurrentUser().getFullName());
            lblProfilePhone.setText(s.getCurrentUser().getPhone());
            lblProfileEmail.setText(s.getCurrentUser().getEmail() != null
                ? s.getCurrentUser().getEmail() : "—");
        } else {
            lblProfileName.setText("Guest");
            lblProfilePhone.setText("—");
            lblProfileEmail.setText("—");
        }
    }

    @FXML private void onEditProfile() { /* open edit profile dialog */ }
    @FXML private void onLanguageChange() { /* English only */ }
    @FXML private void onChangePassword() { /* open change password dialog */ }

    @FXML private void onSave() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Settings Saved");
        a.setHeaderText(null);
        a.setContentText("Your settings have been saved.");
        a.showAndWait();
    }

    @FXML private void onSignOut() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Sign Out");
        a.setHeaderText("Sign out of EthioRide?");
        a.setContentText("You will be returned to the login screen.");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            SessionState.getInstance().clear();
        }
    }
}
