package com.ethioride.driver.ui.controllers;

import com.ethioride.driver.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class DriverLoginController implements Initializable {

    @FXML private TextField tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    @FXML
    private void onLogin() {
        String phone = tfPhone.getText().trim();
        String password = pfPassword.getText();
        if (phone.isEmpty() || password.isEmpty()) {
            showError("Phone and password are required.");
            return;
        }
        hideError();
        // TODO: authenticate via NetworkClient
        Navigator.navigateTo("/ui/views/driver_main.fxml");
    }

    @FXML private void onTogglePassword() {}
    @FXML private void onForgotPassword() {}
    @FXML private void onCreateAccount() {}

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
