package com.ethioride.passenger.ui.controllers;

import com.ethioride.passenger.ui.navigation.Navigator;
import com.ethioride.shared.utils.I18n;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private Label lblRegisterTitle;
    @FXML private Label lblCreateAccount;
    @FXML private Label lblFullName;
    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblPassword;
    @FXML private Label lblConfirmPassword;
    @FXML private Label lblTerms;
    @FXML private Label lblHaveAccount;
    @FXML private Label lblLogin;
    @FXML private Label lblError;

    @FXML private TextField tfFullName;
    @FXML private TextField tfPhone;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmPassword;
    @FXML private CheckBox cbTerms;
    @FXML private Button btnSignUp;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
    }

    private void applyI18n() {
        lblRegisterTitle.setText("ምዝገባ");
        lblCreateAccount.setText(I18n.get("register.title"));
        lblFullName.setText(I18n.get("register.fullname"));
        lblPhone.setText(I18n.get("register.phone"));
        lblEmail.setText(I18n.get("register.email"));
        lblPassword.setText(I18n.get("register.password"));
        lblConfirmPassword.setText(I18n.get("register.confirm"));
        lblTerms.setText(I18n.get("register.terms"));
        btnSignUp.setText(I18n.get("register.button"));
        lblHaveAccount.setText(I18n.get("register.have.account"));
        lblLogin.setText(I18n.get("register.login"));
    }

    @FXML
    private void onBack() {
        Navigator.navigateTo("/ui/views/login.fxml");
    }

    @FXML
    private void onSignUp() {
        if (!validateForm()) return;
        // TODO: delegate to AuthService -> NetworkClient -> server
        Navigator.navigateTo("/ui/views/login.fxml");
    }

    @FXML
    private void onTogglePassword() {
        // Toggle password visibility
    }

    @FXML
    private void onLogin() {
        Navigator.navigateTo("/ui/views/login.fxml");
    }

    private boolean validateForm() {
        if (tfFullName.getText().trim().isEmpty()) { showError("Full name is required."); return false; }
        if (tfPhone.getText().trim().isEmpty()) { showError("Phone number is required."); return false; }
        if (tfEmail.getText().trim().isEmpty()) { showError("Email is required."); return false; }
        if (pfPassword.getText().isEmpty()) { showError("Password is required."); return false; }
        if (!pfPassword.getText().equals(pfConfirmPassword.getText())) { showError("Passwords do not match."); return false; }
        if (!cbTerms.isSelected()) { showError("You must agree to the Terms of Service."); return false; }
        hideError();
        return true;
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
