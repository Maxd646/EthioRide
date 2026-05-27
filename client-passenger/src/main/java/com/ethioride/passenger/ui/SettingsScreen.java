package com.ethioride.passenger.ui;

import com.ethioride.passenger.state.SessionState;
import com.ethioride.shared.dto.UserDTO;
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
        Button btnMap   = navBtn("🗺  Map");
        Button btnHist  = navBtn("🕐  Ride History");
        Button btnPay   = navBtn("💳  Payments");
        Button btnPromo = navBtn("🏷  Promotions");
        Button btnSet   = navBtn("⚙  Settings");
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
        VBox content = new VBox(24);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Settings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // ── Profile section ───────────────────────────────────────────────────
        content.getChildren().addAll(title, sectionLabel("Profile"), buildProfileCard());

        // ── Notifications section ─────────────────────────────────────────────
        content.getChildren().addAll(sectionLabel("Notifications"),
            toggleRow("Ride status updates",   true),
            toggleRow("Promotions & offers",   true),
            toggleRow("Payment receipts",      true),
            toggleRow("Driver arrival alerts", true));

        // ── App section ───────────────────────────────────────────────────────
        content.getChildren().addAll(sectionLabel("App"),
            toggleRow("Dark mode",             true),
            toggleRow("Save ride history",     true));

        // ── Account section ───────────────────────────────────────────────────
        content.getChildren().add(sectionLabel("Account"));
        Button btnSignOut = new Button("Sign Out");
        btnSignOut.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:8px;-fx-padding:10 24;-fx-cursor:hand;-fx-font-size:13px;");
        btnSignOut.setOnAction(e -> {
            SessionState.getInstance().clear();
            new LoginScreen(stage).show();
        });
        content.getChildren().add(btnSignOut);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private VBox buildProfileCard() {
        UserDTO user = SessionState.getInstance().getCurrentUser();
        String name  = user != null ? user.getFullName() : "Guest";
        String phone = user != null ? user.getPhone()    : "—";
        String email = user != null && user.getEmail() != null ? user.getEmail() : "—";

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                      "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:20;");

        Label avatar = new Label("👤");
        avatar.setFont(Font.font("Arial", 36));

        Label lblName = new Label(name);
        lblName.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblName.setTextFill(Color.web("#f1f5f9"));

        Label lblPhone = new Label("📞  " + phone);
        lblPhone.setFont(Font.font("Arial", 13));
        lblPhone.setTextFill(Color.web("#94a3b8"));

        Label lblEmail = new Label("✉  " + email);
        lblEmail.setFont(Font.font("Arial", 13));
        lblEmail.setTextFill(Color.web("#94a3b8"));

        card.getChildren().addAll(avatar, lblName, lblPhone, lblEmail);
        return card;
    }

    private HBox toggleRow(String label, boolean defaultOn) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent;" +
                     "-fx-border-width:0 0 1 0;-fx-padding:10 0;");
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#f1f5f9"));
        lbl.setFont(Font.font("Arial", 13));
        HBox.setHgrow(lbl, Priority.ALWAYS);
        CheckBox cb = new CheckBox();
        cb.setSelected(defaultOn);
        cb.setStyle("-fx-cursor:hand;");
        row.getChildren().addAll(lbl, cb);
        return row;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        lbl.setTextFill(Color.web("#3b82f6"));
        return lbl;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
