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

public class RideHistoryScreen {
    private final Stage stage;
    private VBox tripList;
    private List<String[]> allTrips;

    public RideHistoryScreen(Stage stage) { this.stage = stage; }

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
        btnHist.setStyle(btnHist.getStyle() + "-fx-background-color:#1e3a5f;");
        btnMap.setOnAction(e   -> new MainScreen(stage).show());
        btnPay.setOnAction(e   -> new PaymentsScreen(stage).show());
        btnPromo.setOnAction(e -> new PromotionsScreen(stage).show());
        btnSet.setOnAction(e   -> new SettingsScreen(stage).show());
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

        Label title = new Label("Ride History");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        HBox summary = new HBox(16);
        summary.getChildren().addAll(
            statCard("Total Trips", "5", "#22c55e"),
            statCard("Total Spent", "ETB 740", "#3b82f6"),
            statCard("Avg Rating", "4.8 ★", "#f59e0b")
        );

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search trips...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);
        ComboBox<String> cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("All", "COMPLETED", "CANCELLED");
        cbFilter.setValue("All");
        cbFilter.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        toolbar.getChildren().addAll(tfSearch, cbFilter);

        allTrips = List.of(
            new String[]{"Apr 18, 2026  14:28", "Edna Mall, Bole",    "Sarbet",       "145.00", "COMPLETED", "ECONOMY"},
            new String[]{"Apr 18, 2026  12:10", "Meskel Square",       "Bole Airport", "320.00", "COMPLETED", "ELITE"},
            new String[]{"Apr 17, 2026  09:45", "Bole Medhanialem",    "Gerji",        "110.00", "CANCELLED", "ECONOMY"},
            new String[]{"Apr 16, 2026  18:30", "Piassa",              "Kazanchis",    "90.00",  "COMPLETED", "PREMIUM"},
            new String[]{"Apr 15, 2026  08:00", "CMC",                 "Megenagna",    "75.00",  "COMPLETED", "ECONOMY"}
        );

        tripList = new VBox(8);
        renderTrips(allTrips);

        tfSearch.textProperty().addListener((o, ov, nv) -> filter(nv, cbFilter.getValue()));
        cbFilter.setOnAction(e -> filter(tfSearch.getText(), cbFilter.getValue()));

        content.getChildren().addAll(title, summary, toolbar, tripList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private void renderTrips(List<String[]> trips) {
        tripList.getChildren().clear();
        for (String[] t : trips) {
            HBox card = new HBox(16);
            card.setStyle("-fx-background-color:#1a2235;-fx-background-radius:12px;-fx-padding:16;");
            card.setAlignment(Pos.CENTER_LEFT);
            StackPane icon = new StackPane();
            icon.setStyle("-fx-background-color:#1e3a5f;-fx-background-radius:50%;-fx-min-width:44px;-fx-min-height:44px;");
            Label ico = new Label("COMPLETED".equals(t[4]) ? "✓" : "✕");
            ico.setStyle("COMPLETED".equals(t[4]) ? "-fx-text-fill:#22c55e;-fx-font-size:16px;-fx-font-weight:bold;" : "-fx-text-fill:#ef4444;-fx-font-size:16px;-fx-font-weight:bold;");
            icon.getChildren().add(ico);
            VBox info = new VBox(4); HBox.setHgrow(info, Priority.ALWAYS);
            Label route = new Label(t[1] + "  →  " + t[2]);
            route.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:13px;-fx-font-weight:bold;");
            Label meta = new Label(t[0] + "  •  " + t[5]);
            meta.setStyle("-fx-text-fill:#475569;-fx-font-size:11px;");
            info.getChildren().addAll(route, meta);
            VBox right = new VBox(4); right.setAlignment(Pos.CENTER_RIGHT);
            Label fare = new Label("ETB " + t[3]);
            fare.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:14px;-fx-font-weight:bold;");
            Label status = new Label(t[4]);
            status.setStyle("COMPLETED".equals(t[4]) ? "-fx-text-fill:#22c55e;-fx-font-size:10px;" : "-fx-text-fill:#ef4444;-fx-font-size:10px;");
            right.getChildren().addAll(fare, status);
            card.getChildren().addAll(icon, info, right);
            tripList.getChildren().add(card);
        }
    }

    private void filter(String q, String status) {
        renderTrips(allTrips.stream()
            .filter(t -> (q.isEmpty() || t[1].toLowerCase().contains(q.toLowerCase()) || t[2].toLowerCase().contains(q.toLowerCase()))
                      && ("All".equals(status) || t[4].equals(status)))
            .toList());
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:14;");
        card.setPrefWidth(160);
        Label lbl = new Label(label); lbl.setTextFill(Color.web("#94a3b8")); lbl.setFont(Font.font("Arial", 11));
        Label val = new Label(value); val.setTextFill(Color.web(color)); val.setFont(Font.font("Arial", FontWeight.BOLD, 16));
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
