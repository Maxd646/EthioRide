package com.ethioride.passenger.ui.controllers;

import com.ethioride.passenger.ui.navigation.Navigator;
import com.ethioride.shared.utils.I18n;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private Label lblLoginTitle;
    @FXML private Label lblLoginSubtitle;
    @FXML private Label lblPhone;
    @FXML private Label lblPassword;
    @FXML private Label lblForgot;
    @FXML private Label lblNoAccount;
    @FXML private Label lblCreateAccount;
    @FXML private Label lblError;

    @FXML private TextField tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private Button btnLogin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
    }

    private void applyI18n() {
        lblLoginTitle.setText(I18n.get("login.title"));
        lblLoginSubtitle.setText(I18n.get("login.subtitle"));
        lblPhone.setText(I18n.get("login.phone"));
        lblPassword.setText(I18n.get("login.password"));
        lblForgot.setText(I18n.get("login.forgot"));
        btnLogin.setText(I18n.get("login.button"));
        lblNoAccount.setText(I18n.get("login.no.account"));
        lblCreateAccount.setText(I18n.get("login.create"));
    }

    @FXML
    private void onLogin() {
        String phone = tfPhone.getText().trim();
        String password = pfPassword.getText();

        if (phone.isEmpty() || password.isEmpty()) {
            showError("Phone and password are required.");
            return;
        }

        // TODO: delegate to AuthService -> NetworkClient -> server
        // For now navigate to main view on any non-empty input
        hideError();
        Navigator.navigateTo("/ui/views/main.fxml");
    }

    @FXML
    private void onTogglePassword() {
        // Toggle password visibility — swap PasswordField with TextField
        // Handled via CSS/binding in full implementation
    }

    @FXML
    private void onForgotPassword() {
        // Navigate to forgot password flow
    }

    @FXML
    private void onCreateAccount() {
        Navigator.navigateTo("/ui/views/register.fxml");
    }

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
