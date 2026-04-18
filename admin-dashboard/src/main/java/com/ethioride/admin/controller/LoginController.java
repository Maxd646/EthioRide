package com.ethioride.admin.controller;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
import com.ethioride.shared.utils.I18n;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private Label lblSubtitle;
    @FXML private Label lblUsername;
    @FXML private Label lblPassword;
    @FXML private Label lblError;
    @FXML private TextField tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Button btnLogin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblSubtitle.setText(I18n.get("login.subtitle"));
        lblUsername.setText(I18n.get("login.username"));
        lblPassword.setText(I18n.get("login.password"));
        btnLogin.setText(I18n.get("login.button"));
    }

    @FXML
    private void onLogin() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        // TODO: validate against server AdminAuthService
        hideError();
        AdminSession.getInstance().login(username, "token-placeholder");
        AdminService.getInstance().connect();
        AdminNavigator.navigateTo("/ui/dashboard.fxml");
    }

    @FXML private void onTogglePassword() { /* toggle visibility */ }

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
