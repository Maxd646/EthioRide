package com.ethioride.driver.ui;

import com.ethioride.driver.network.NetworkClient;
import com.ethioride.driver.state.DriverSessionState;
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

public class LoginScreen {
    private final Stage stage;
    private TextField tfPhone;
    private PasswordField pfPassword;
    private Label lblError;
    private Button btnLogin;

    public LoginScreen(Stage stage) { this.stage = stage; }

    public void show() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color:#0a0e1a;");

        Label logo = new Label("🚗 EthioRide Driver");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        logo.setTextFill(Color.web("#22c55e"));

        Label sub = new Label("Driver Login");
        sub.setFont(Font.font("Arial", 13));
        sub.setTextFill(Color.web("#94a3b8"));

        tfPhone = new TextField();
        tfPhone.setPromptText("+251 911 234 567");
        styleField(tfPhone);

        pfPassword = new PasswordField();
        pfPassword.setPromptText("••••••••");
        styleField(pfPassword);

        lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("Arial", 12));
        lblError.setVisible(false);
        lblError.setManaged(false);

        btnLogin = new Button("Login");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnLogin.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-background-radius:8px;-fx-padding:12px;-fx-cursor:hand;");
        btnLogin.setOnAction(e -> onLogin());

        root.getChildren().addAll(
            logo, sub,
            fieldLabel("Phone Number"), tfPhone,
            fieldLabel("Password"), pfPassword,
            lblError, btnLogin
        );

        stage.setScene(new Scene(root, 480, 580));
        stage.show();
    }

    private void onLogin() {
        String phone = tfPhone.getText().trim();
        String password = pfPassword.getText();
        if (phone.isEmpty() || password.isEmpty()) { showError("Phone and password are required."); return; }
        btnLogin.setDisable(true);
        btnLogin.setText("Connecting...");
        hideError();

        Thread t = new Thread(() -> {
            try {
                NetworkClient.getInstance().connect();
                Message resp = NetworkClient.getInstance().sendAndReceive(
                    new Message(MessageType.LOGIN_REQUEST, phone + ":" + password, "driver"));
                Platform.runLater(() -> {
                    if (resp.getType() == MessageType.LOGIN_RESPONSE && resp.getPayload() instanceof UserDTO user) {
                        DriverSessionState.getInstance().setCurrentDriver(user);
                        new MainScreen(stage).show();
                    } else {
                        showError("Invalid phone or password.");
                        resetBtn();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> { showError("Cannot connect to server."); resetBtn(); });
            }
        }, "driver-login");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String m) { lblError.setText(m); lblError.setVisible(true); lblError.setManaged(true); }
    private void hideError()         { lblError.setVisible(false); lblError.setManaged(false); }
    private void resetBtn()          { btnLogin.setDisable(false); btnLogin.setText("Login"); }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#94a3b8"));
        l.setFont(Font.font("Arial", 12));
        return l;
    }

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;" +
                   "-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:10px 14px;-fx-font-size:13px;");
        f.setMaxWidth(Double.MAX_VALUE);
    }
}
