package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class LoginScreen {
    private final Stage stage;
    private TextField tfUsername;
    private PasswordField pfPassword;
    private Label lblError;
    private Button btnLogin;

    public LoginScreen(Stage stage) { this.stage = stage; }

    public void show() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color:#0a0e1a;");

        Label logo = new Label("⚙ EthioRide Admin");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        logo.setTextFill(Color.web("#f59e0b"));

        Label sub = new Label("System Monitor — Admin Login");
        sub.setFont(Font.font("Arial", 13));
        sub.setTextFill(Color.web("#94a3b8"));

        tfUsername = new TextField();
        tfUsername.setPromptText("admin@ethioride.com or +251 900 000 000");
        styleField(tfUsername);

        pfPassword = new PasswordField();
        pfPassword.setPromptText("••••••••");
        styleField(pfPassword);

        lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("Arial", 12));
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblError.setWrapText(true);
        lblError.setMaxWidth(400);

        btnLogin = new Button("Login");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnLogin.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:#0a0e1a;-fx-background-radius:8px;-fx-padding:12px;-fx-cursor:hand;");
        btnLogin.setOnAction(e -> onLogin());

        // Allow Enter key to submit
        pfPassword.setOnAction(e -> onLogin());

        root.getChildren().addAll(
            logo, sub,
            lbl("Username"), tfUsername,
            lbl("Password"), pfPassword,
            lblError, btnLogin
        );

        stage.setScene(new Scene(root, 480, 520));
        stage.show();
    }

    private void onLogin() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("Authenticating...");
        hideError();

        Thread authThread = new Thread(() -> {
            try {
                com.ethioride.admin.network.AdminSocketClient client =
                    com.ethioride.admin.network.AdminSocketClient.getInstance();
                client.connect();

                final boolean[] authenticated = {false};
                final com.ethioride.shared.dto.UserDTO[] user = {null};
                final String[] errorDetail = {null};
                final Object lock = new Object();

                client.setMessageHandler(msg -> {
                    if (msg.getType() == com.ethioride.shared.protocol.MessageType.LOGIN_RESPONSE) {
                        synchronized (lock) {
                            if (msg.getPayload() instanceof com.ethioride.shared.dto.UserDTO) {
                                user[0] = (com.ethioride.shared.dto.UserDTO) msg.getPayload();
                                if (user[0].getRole() == com.ethioride.shared.enums.UserRole.ADMIN) {
                                    authenticated[0] = true;
                                } else {
                                    errorDetail[0] = "Access denied. Admin privileges required.";
                                }
                            } else {
                                errorDetail[0] = "Invalid credentials. Please try again.";
                            }
                            lock.notify();
                        }
                    } else if (msg.getType() == com.ethioride.shared.protocol.MessageType.ERROR) {
                        synchronized (lock) {
                            errorDetail[0] = "Server error: " + msg.getPayload();
                            lock.notify();
                        }
                    }
                });

                client.send(new com.ethioride.shared.protocol.Message(
                    com.ethioride.shared.protocol.MessageType.LOGIN_REQUEST,
                    username + ":" + password,
                    "admin"
                ));

                synchronized (lock) {
                    lock.wait(5000);
                }

                if (authenticated[0] && user[0] != null) {
                    javafx.application.Platform.runLater(() -> {
                        AdminSession.getInstance().login(user[0].getFullName(), user[0].getId());
                        AdminService.getInstance().connect();
                        new DashboardScreen(stage).show();
                    });
                } else {
                    final String msg = errorDetail[0] != null ? errorDetail[0] : "Invalid credentials. Please try again.";
                    javafx.application.Platform.runLater(() -> {
                        showError(msg);
                        resetButton();
                    });
                }
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    showError("Connection failed: " + ex.getMessage());
                    resetButton();
                });
            }
        }, "admin-auth");
        authThread.setDaemon(true);
        authThread.start();
    }

    private void resetButton() {
        btnLogin.setDisable(false);
        btnLogin.setText("Login");
    }

    private void showError(String m) { lblError.setText(m); lblError.setVisible(true); lblError.setManaged(true); }
    private void hideError()         { lblError.setVisible(false); lblError.setManaged(false); }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#94a3b8"));
        l.setFont(Font.font("Arial", 12));
        return l;
    }

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;" +
                   "-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;" +
                   "-fx-padding:10px 14px;-fx-font-size:13px;");
        f.setMaxWidth(Double.MAX_VALUE);
    }
}
