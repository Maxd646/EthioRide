package com.ethioride.passenger.ui;

import com.ethioride.passenger.state.SessionState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class SettingsScreen {
    private final Stage stage;

    public SettingsScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 900, 640));
        stage.setResizable(true);
        stage.show();
    }

    private VBox buildSidebar() {
        VBox s = new VBox(0);
        s.setPrefWidth(200);
        s.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");
        Label logo = new Label("🚕 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#3b82f6"));
        logo.setPadding(new Insets(24, 20, 20, 20));
        Button btnMap  = navBtn("🗺  Map");
        Button btnHist = navBtn("🕐  Ride History");
        Button btnPay  = navBtn("💳  Payments");
        Button btnPromo = navBtn("🏷  Promotions");
        Button btnSet  = navBtn("⚙  Settings");
        btnSet.setStyle(btnSet.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e   -> new MainScreen(stage).show());
        btnHist.setOnAction(e  -> new RideHistoryScreen(stage).show());
        btnPay.setOnAction(e   -> new PaymentsScreen(stage).show());
        btnPromo.setOnAction(e -> new PromotionsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { SessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnPay, btnPromo, btnSet, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Settings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Profile info (read-only)
        SessionState session = SessionState.getInstance();
        String name  = session.isLoggedIn() ? session.getCurrentUser().getFullName() : "—";
        String phone = session.isLoggedIn() ? session.getCurrentUser().getPhone()    : "—";
        String email = session.isLoggedIn() && session.getCurrentUser().getEmail() != null
                       ? session.getCurrentUser().getEmail() : "—";

        VBox profileCard = new VBox(10);
        profileCard.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");

        Label lblProfile = new Label("Profile");
        lblProfile.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblProfile.setTextFill(Color.web("#f1f5f9"));

        profileCard.getChildren().addAll(
            lblProfile,
            infoRow("Full Name", name),
            infoRow("Phone",     phone),
            infoRow("Email",     email)
        );

        // Sign out
        Button btnSignOut = new Button("Sign Out");
        btnSignOut.setMaxWidth(Double.MAX_VALUE);
        btnSignOut.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnSignOut.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" +
            "-fx-background-radius:10px;-fx-padding:12px;-fx-cursor:hand;");
        btnSignOut.setOnAction(e -> {
            SessionState.getInstance().clear();
            new LoginScreen(stage).show();
        });

        content.getChildren().addAll(title, profileCard, btnSignOut);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private HBox infoRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + ":");
        lbl.setTextFill(Color.web("#475569"));
        lbl.setFont(Font.font("Arial", 12));
        lbl.setMinWidth(80);
        Label val = new Label(value);
        val.setTextFill(Color.web("#f1f5f9"));
        val.setFont(Font.font("Arial", 13));
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
