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

public class EarningsScreen {
    private final Stage stage;

    public EarningsScreen(Stage stage) { this.stage = stage; }

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
        btnEarn.setStyle(btnEarn.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e  -> new MainScreen(stage).show());
        btnHist.setOnAction(e -> new RideHistoryScreen(stage).show());
        btnPay.setOnAction(e  -> new PaymentsScreen(stage).show());
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

        Label title = new Label("Earnings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Summary cards
        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("This Week", "ETB 8,450", "#22c55e"),
            statCard("This Month", "ETB 32,100", "#3b82f6"),
            statCard("Total Trips", "42", "#f59e0b"),
            statCard("Online Hours", "38h", "#a855f7")
        );

        // Daily breakdown
        Label lblBreakdown = new Label("Daily Breakdown");
        lblBreakdown.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblBreakdown.setTextFill(Color.web("#f1f5f9"));

        VBox dailyList = new VBox(0);
        List<String[]> days = List.of(
            new String[]{"Mon Apr 14", "8 trips",  "1,200"},
            new String[]{"Tue Apr 15", "7 trips",  "1,050"},
            new String[]{"Wed Apr 16", "9 trips",  "1,350"},
            new String[]{"Thu Apr 17", "6 trips",  "900"},
            new String[]{"Fri Apr 18", "12 trips", "2,450"},
            new String[]{"Sat Apr 19", "0 trips",  "0"},
            new String[]{"Sun Apr 20", "0 trips",  "0"}
        );
        for (String[] d : days) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent;-fx-border-width:0 0 1 0;-fx-padding:10 0;");
            Label day = new Label(d[0]);
            day.setTextFill(Color.web("#94a3b8")); day.setFont(Font.font("Arial", 13)); day.setMinWidth(140);
            Label trips = new Label(d[1]);
            trips.setTextFill(Color.web("#475569")); trips.setFont(Font.font("Arial", 12));
            HBox.setHgrow(trips, Priority.ALWAYS);
            ProgressBar bar = new ProgressBar(Double.parseDouble(d[2]) / 2500.0);
            bar.setPrefWidth(160); bar.setPrefHeight(6);
            Label amount = new Label("ETB " + d[2]);
            amount.setTextFill("0".equals(d[2]) ? Color.web("#475569") : Color.web("#22c55e"));
            amount.setFont(Font.font("Arial", FontWeight.BOLD, 13)); amount.setMinWidth(100);
            amount.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(day, trips, bar, amount);
            dailyList.getChildren().add(row);
        }

        content.getChildren().addAll(title, cards, lblBreakdown, dailyList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");
        card.setPrefWidth(160);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94a3b8")); lbl.setFont(Font.font("Arial", 11));
        Label val = new Label(value);
        val.setTextFill(Color.web(color)); val.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
