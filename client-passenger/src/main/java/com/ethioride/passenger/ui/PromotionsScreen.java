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

import java.util.List;

public class PromotionsScreen {
    private final Stage stage;

    public PromotionsScreen(Stage stage) { this.stage = stage; }

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
        btnPromo.setStyle(btnPromo.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e  -> new MainScreen(stage).show());
        btnHist.setOnAction(e -> new RideHistoryScreen(stage).show());
        btnPay.setOnAction(e  -> new PaymentsScreen(stage).show());
        btnSet.setOnAction(e  -> new SettingsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { SessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnPay, btnPromo, btnSet, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Promotions");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Promo code input
        HBox codeRow = new HBox(12);
        codeRow.setAlignment(Pos.CENTER_LEFT);
        TextField tfCode = new TextField();
        tfCode.setPromptText("Enter promo code...");
        tfCode.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:10px 14px;");
        HBox.setHgrow(tfCode, Priority.ALWAYS);
        Button btnApply = new Button("Apply");
        btnApply.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        btnApply.setOnAction(e -> {
            if (!tfCode.getText().trim().isEmpty()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Discount applied on your next ride.", ButtonType.OK);
                a.setTitle("Promo Applied"); a.setHeaderText("Code: " + tfCode.getText().trim()); a.showAndWait();
            }
        });
        codeRow.getChildren().addAll(tfCode, btnApply);

        // Promo cards
        VBox promoList = new VBox(12);
        for (String[] p : List.of(
            new String[]{"🎉", "WELCOME20",  "20% off your first 3 rides",      "Expires Apr 30, 2026", "#22c55e"},
            new String[]{"⚡", "PEAKHOUR",   "ETB 50 off during peak hours",     "Expires Apr 25, 2026", "#f59e0b"},
            new String[]{"🎁", "REFER50",    "ETB 50 for each friend you refer", "No expiry",            "#3b82f6"},
            new String[]{"🏷", "WEEKEND15",  "15% off all weekend rides",        "Expires Apr 27, 2026", "#a855f7"}
        )) {
            HBox card = new HBox(16);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color:#1a2235;-fx-background-radius:12px;-fx-padding:16;-fx-border-color:" + p[4] + ";-fx-border-width:0 0 0 4px;-fx-border-radius:0 12 12 0;");
            StackPane icon = new StackPane();
            icon.setStyle("-fx-background-color:#111827;-fx-background-radius:10px;-fx-min-width:48px;-fx-min-height:48px;");
            Label ico = new Label(p[0]); ico.setStyle("-fx-font-size:22px;");
            icon.getChildren().add(ico);
            VBox info = new VBox(4); HBox.setHgrow(info, Priority.ALWAYS);
            Label code = new Label(p[1]); code.setStyle("-fx-text-fill:" + p[4] + ";-fx-font-size:14px;-fx-font-weight:bold;");
            Label desc = new Label(p[2]); desc.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;");
            Label exp  = new Label(p[3]); exp.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
            info.getChildren().addAll(code, desc, exp);
            Button use = new Button("Use");
            use.setStyle("-fx-background-color:" + p[4] + ";-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 16;-fx-background-radius:8px;-fx-cursor:hand;");
            use.setOnAction(e -> tfCode.setText(p[1]));
            card.getChildren().addAll(icon, info, use);
            promoList.getChildren().add(card);
        }

        content.getChildren().addAll(title, codeRow, promoList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
