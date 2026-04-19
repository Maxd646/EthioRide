package com.ethioride.passenger.ui;

import com.ethioride.passenger.network.ServerConnection;
import com.ethioride.passenger.state.SessionState;
import com.ethioride.shared.dto.UserDTO;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Login screen built entirely in JavaFX Java code — no FXML.
 * Connects to the server via TCP socket and authenticates the user.
 */
public class LoginScreen {

    private final Stage stage;

    // UI fields
    private TextField     tfPhone;
    private PasswordField pfPassword;
    private Label         lblError;
    private Button        btnLogin;

    public LoginScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        // ── Root ──────────────────────────────────────────────────
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40, 40, 40, 40));
        root.setStyle("-fx-background-color: #0a0e1a;");

        // ── Logo / Title ──────────────────────────────────────────
        Label lblLogo = new Label("🚕 EthioRide");
        lblLogo.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblLogo.setTextFill(Color.web("#3b82f6"));

        Label lblSubtitle = new Label("Passenger Login");
        lblSubtitle.setFont(Font.font("Arial", 14));
        lblSubtitle.setTextFill(Color.web("#94a3b8"));

        // ── Phone field ───────────────────────────────────────────
        Label lblPhone = new Label("Phone Number");
        lblPhone.setTextFill(Color.web("#94a3b8"));
        lblPhone.setFont(Font.font("Arial", 12));

        tfPhone = new TextField();
        tfPhone.setPromptText("+251 911 234 567");
        styleField(tfPhone);

        // ── Password field ────────────────────────────────────────
        Label lblPassword = new Label("Password");
        lblPassword.setTextFill(Color.web("#94a3b8"));
        lblPassword.setFont(Font.font("Arial", 12));

        pfPassword = new PasswordField();
        pfPassword.setPromptText("••••••••");
        styleField(pfPassword);

        // ── Error label ───────────────────────────────────────────
        lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("Arial", 12));
        lblError.setVisible(false);
        lblError.setManaged(false);

        // ── Login button ──────────────────────────────────────────
        btnLogin = new Button("Login");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnLogin.setStyle(
            "-fx-background-color: #3b82f6;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 12px;" +
            "-fx-cursor: hand;"
        );
        btnLogin.setOnAction(e -> onLogin());

        // ── Register link ─────────────────────────────────────────
        HBox registerRow = new HBox(6);
        registerRow.setAlignment(Pos.CENTER);
        Label lblNoAccount = new Label("Don't have an account?");
        lblNoAccount.setTextFill(Color.web("#94a3b8"));
        lblNoAccount.setFont(Font.font("Arial", 12));

        Label lblRegister = new Label("Register");
        lblRegister.setTextFill(Color.web("#3b82f6"));
        lblRegister.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblRegister.setStyle("-fx-cursor: hand;");
        lblRegister.setOnMouseClicked(e -> new RegisterScreen(stage).show());

        registerRow.getChildren().addAll(lblNoAccount, lblRegister);

        // ── Assemble ──────────────────────────────────────────────
        root.getChildren().addAll(
            lblLogo, lblSubtitle,
            lblPhone, tfPhone,
            lblPassword, pfPassword,
            lblError,
            btnLogin,
            registerRow
        );

        Scene scene = new Scene(root, 480, 640);
        stage.setScene(scene);
        stage.show();
    }

    private void onLogin() {
        String phone    = tfPhone.getText().trim();
        String password = pfPassword.getText();

        if (phone.isEmpty() || password.isEmpty()) {
            showError("Phone and password are required.");
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("Connecting...");
        hideError();

        // Run network call on background thread — never block JavaFX thread
        Thread loginThread = new Thread(() -> {
            try {
                ServerConnection conn = new ServerConnection();
                conn.connect();

                // Build login request with phone+password as payload "phone:password"
                Message request = new Message(MessageType.LOGIN_REQUEST, phone + ":" + password, "passenger");
                Message response = conn.sendAndReceive(request);
                conn.close();

                Platform.runLater(() -> {
                    if (response.getType() == MessageType.LOGIN_RESPONSE) {
                        Object payload = response.getPayload();
                        if (payload instanceof UserDTO user) {
                            SessionState.getInstance().setCurrentUser(user);
                            new MainScreen(stage).show();
                        } else {
                            showError("Invalid phone or password.");
                            resetButton();
                        }
                    } else {
                        showError("Login failed: " + response.getPayload());
                        resetButton();
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Cannot connect to server. Is it running?");
                    resetButton();
                });
            }
        }, "login-thread");
        loginThread.setDaemon(true);
        loginThread.start();
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

    private void resetButton() {
        btnLogin.setDisable(false);
        btnLogin.setText("Login");
    }

    private void styleField(TextField field) {
        field.setStyle(
            "-fx-background-color: #0d1526;" +
            "-fx-text-fill: #f1f5f9;" +
            "-fx-prompt-text-fill: #475569;" +
            "-fx-border-color: #1e3a5f;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 10px 14px;" +
            "-fx-font-size: 13px;"
        );
        field.setMaxWidth(Double.MAX_VALUE);
    }
}
