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

    private VBox buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(40));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Promotions");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        Label icon = new Label("🏷");
        icon.setFont(Font.font("Arial", 48));

        Label msg = new Label("No active promotions right now.\nCheck back later for discounts and offers.");
        msg.setTextFill(Color.web("#475569"));
        msg.setFont(Font.font("Arial", 14));
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msg.setWrapText(true);

        content.getChildren().addAll(title, icon, msg);
        return content;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
