package com.ethioride.driver.ui;

import com.ethioride.driver.state.DriverSessionState;
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

public class PaymentsScreen {
    private final Stage stage;

    public PaymentsScreen(Stage stage) { this.stage = stage; }

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
        Label logo = new Label("🚗 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#22c55e"));
        logo.setPadding(new Insets(24, 20, 20, 20));
        Button btnMap  = navBtn("🗺  Live Map");
        Button btnHist = navBtn("🕐  Ride History");
        Button btnEarn = navBtn("💰  Earnings");
        Button btnPay  = navBtn("💳  Payments");
        btnPay.setStyle(btnPay.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e  -> new MainScreen(stage).show());
        btnHist.setOnAction(e -> new RideHistoryScreen(stage).show());
        btnEarn.setOnAction(e -> new EarningsScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { DriverSessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnMap, btnHist, btnEarn, btnPay, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Payments");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Balance card
        VBox balanceCard = new VBox(8);
        balanceCard.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");
        Label lblBalTitle = new Label("Total Earnings");
        lblBalTitle.setTextFill(Color.web("#94a3b8")); lblBalTitle.setFont(Font.font("Arial", 12));
        Label lblBal = new Label("ETB 12,450.00");
        lblBal.setFont(Font.font("Arial", FontWeight.BOLD, 28)); lblBal.setTextFill(Color.web("#22c55e"));
        Button btnWithdraw = new Button("Withdraw");
        btnWithdraw.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-background-radius:8px;-fx-padding:10 20;-fx-cursor:hand;");
        balanceCard.getChildren().addAll(lblBalTitle, lblBal, btnWithdraw);

        // Payout methods
        Label lblMethods = new Label("Payout Methods");
        lblMethods.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblMethods.setTextFill(Color.web("#f1f5f9"));

        VBox methods = new VBox(8);
        for (String[] m : List.of(
            new String[]{"📱", "Telebirr", "•••• 4521", "Default"},
            new String[]{"🏦", "CBE Birr", "•••• 8832", ""}
        )) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#111827;-fx-background-radius:8px;-fx-padding:12;");
            Label icon = new Label(m[0]); icon.setStyle("-fx-font-size:18px;");
            VBox info = new VBox(2); HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(m[1]); name.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;-fx-font-weight:bold;");
            Label num  = new Label(m[2]); num.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
            info.getChildren().addAll(name, num);
            row.getChildren().addAll(icon, info);
            if (!m[3].isEmpty()) {
                Label def = new Label(m[3]);
                def.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#3b82f6;-fx-padding:2 8;-fx-background-radius:4px;-fx-font-size:10px;");
                row.getChildren().add(def);
            }
            methods.getChildren().add(row);
        }

        // Payout history
        Label lblHistory = new Label("Payout History");
        lblHistory.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblHistory.setTextFill(Color.web("#f1f5f9"));

        VBox history = new VBox(0);
        for (String[] p : List.of(
            new String[]{"Apr 18, 2026", "Daily payout", "+2,450.00"},
            new String[]{"Apr 17, 2026", "Daily payout", "+1,980.00"},
            new String[]{"Apr 16, 2026", "Daily payout", "+2,100.00"}
        )) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent;-fx-border-width:0 0 1 0;-fx-padding:10 0;");
            VBox info = new VBox(2); HBox.setHgrow(info, Priority.ALWAYS);
            Label desc = new Label(p[1]); desc.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;");
            Label date = new Label(p[0]); date.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
            info.getChildren().addAll(desc, date);
            Label amount = new Label("ETB " + p[2]);
            amount.setStyle("-fx-text-fill:#22c55e;-fx-font-size:14px;-fx-font-weight:bold;");
            row.getChildren().addAll(info, amount);
            history.getChildren().add(row);
        }

        content.getChildren().addAll(title, balanceCard, lblMethods, methods, lblHistory, history);
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
