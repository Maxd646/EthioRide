package com.ethioride.passenger.ui;

import com.ethioride.passenger.network.ServerConnection;
import com.ethioride.shared.dto.UserDTO;
import com.ethioride.shared.enums.UserRole;
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
 * Register screen — pure JavaFX, no FXML.
 * Sends a REGISTER_REQUEST to the server which persists the user in MySQL.
 */
public class RegisterScreen {

    private final Stage stage;

    private TextField     tfFullName;
    private TextField     tfPhone;
    private TextField     tfEmail;
    private PasswordField pfPassword;
    private PasswordField pfConfirm;
    private Label         lblError;
    private Button        btnRegister;

    public RegisterScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40, 40, 40, 40));
        root.setStyle("-fx-background-color: #0a0e1a;");

        // Title
        Label lblTitle = new Label("🚕 Create Account");
        lblTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.web("#3b82f6"));

        Label lblSub = new Label("Join EthioRide as a Passenger");
        lblSub.setFont(Font.font("Arial", 13));
        lblSub.setTextFill(Color.web("#94a3b8"));

        // Fields
        tfFullName = makeField("Full Name", "Abebe Kebede");
        tfPhone    = makeField("Phone", "+251 911 234 567");
        tfEmail    = makeField("Email (optional)", "abebe@example.com");
        pfPassword = makePasswordField("Password");
        pfConfirm  = makePasswordField("Confirm Password");

        // Error
        lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("Arial", 12));
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Register button
        btnRegister = new Button("Create Account");
        btnRegister.setMaxWidth(Double.MAX_VALUE);
        btnRegister.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnRegister.setStyle(
            "-fx-background-color: #22c55e;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 12px;" +
            "-fx-cursor: hand;"
        );
        btnRegister.setOnAction(e -> onRegister());

        // Back to login
        HBox backRow = new HBox(6);
        backRow.setAlignment(Pos.CENTER);
        Label lblHave = new Label("Already have an account?");
        lblHave.setTextFill(Color.web("#94a3b8"));
        lblHave.setFont(Font.font("Arial", 12));
        Label lblLogin = new Label("Login");
        lblLogin.setTextFill(Color.web("#3b82f6"));
        lblLogin.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblLogin.setStyle("-fx-cursor: hand;");
        lblLogin.setOnMouseClicked(e -> new LoginScreen(stage).show());
        backRow.getChildren().addAll(lblHave, lblLogin);

        root.getChildren().addAll(
            lblTitle, lblSub,
            label("Full Name"), tfFullName,
            label("Phone Number"), tfPhone,
            label("Email (optional)"), tfEmail,
            label("Password"), pfPassword,
            label("Confirm Password"), pfConfirm,
            lblError, btnRegister, backRow
        );

        stage.setScene(new Scene(root, 480, 700));
        stage.show();
    }

    private void onRegister() {
        String name     = tfFullName.getText().trim();
        String phone    = tfPhone.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText();
        String confirm  = pfConfirm.getText();

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showError("Name, phone, and password are required.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        btnRegister.setDisable(true);
        btnRegister.setText("Registering...");
        hideError();

        Thread t = new Thread(() -> {
            try {
                UserDTO user = new UserDTO(null, name, phone, email, UserRole.PASSENGER);
                // Payload: "name|phone|email|password"
                String payload = name + "|" + phone + "|" + email + "|" + password;

                ServerConnection conn = new ServerConnection();
                conn.connect();
                Message response = conn.sendAndReceive(
                    new Message(MessageType.REGISTER_REQUEST, payload, "passenger")
                );
                conn.close();

                Platform.runLater(() -> {
                    if (response.getType() == MessageType.REGISTER_RESPONSE
                            && "OK".equals(response.getPayload())) {
                        showSuccess();
                    } else {
                        showError("Registration failed: " + response.getPayload());
                        resetButton();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Cannot connect to server.");
                    resetButton();
                });
            }
        }, "register-thread");
        t.setDaemon(true);
        t.start();
    }

    private void showSuccess() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account Created");
        alert.setHeaderText("Welcome to EthioRide!");
        alert.setContentText("Your account has been created. Please login.");
        alert.showAndWait();
        new LoginScreen(stage).show();
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
        btnRegister.setDisable(false);
        btnRegister.setText("Create Account");
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#94a3b8"));
        l.setFont(Font.font("Arial", 12));
        return l;
    }

    private TextField makeField(String prompt, String hint) {
        TextField f = new TextField();
        f.setPromptText(hint);
        f.setStyle(fieldStyle());
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private PasswordField makePasswordField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText("••••••••");
        f.setStyle(fieldStyle());
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private String fieldStyle() {
        return "-fx-background-color: #0d1526;" +
               "-fx-text-fill: #f1f5f9;" +
               "-fx-prompt-text-fill: #475569;" +
               "-fx-border-color: #1e3a5f;" +
               "-fx-border-radius: 8px;" +
               "-fx-background-radius: 8px;" +
               "-fx-padding: 10px 14px;" +
               "-fx-font-size: 13px;";
    }
}
