package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
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

import java.util.Map;

/**
 * Admin Financial Report screen.
 * Shows gross revenue, platform commission (15%), net revenue,
 * breakdown by ride category, and daily revenue for the last 30 days.
 */
public class FinancialReportScreen {
    private final Stage stage;

    private Label lblGross, lblCommission, lblNet;
    private Label lblEconomy, lblPremium, lblElite;
    private VBox dailyList;

    public FinancialReportScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 700));
        stage.setResizable(true);
        stage.show();
        loadReport();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox s = new VBox(0);
        s.setPrefWidth(200);
        s.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");
        Label logo = new Label("⚙ EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#f59e0b"));
        logo.setPadding(new Insets(24, 20, 20, 20));
        Button btnDash    = navBtn("📊  Dashboard");
        Button btnDrivers = navBtn("🚗  Drivers");
        Button btnTrips   = navBtn("🗺  Trips");
        Button btnUsers   = navBtn("👥  Users");
        Button btnPricing = navBtn("💰  Pricing");
        Button btnReport  = navBtn("📈  Financial Report");
        Button btnSystem  = navBtn("🖥  System");
        btnReport.setStyle(btnReport.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e    -> new DashboardScreen(stage).show());
        btnDrivers.setOnAction(e -> new DriversScreen(stage).show());
        btnTrips.setOnAction(e   -> new TripsScreen(stage).show());
        btnUsers.setOnAction(e   -> new UsersScreen(stage).show());
        btnPricing.setOnAction(e -> new PricingScreen(stage).show());
        btnSystem.setOnAction(e  -> new SystemScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> {
            AdminService.getInstance().disconnect();
            AdminSession.getInstance().logout();
            new LoginScreen(stage).show();
        });
        s.getChildren().addAll(logo, btnDash, btnDrivers, btnTrips, btnUsers,
            btnPricing, btnReport, btnSystem, sp, btnOut);
        return s;
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private ScrollPane buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Financial Report");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        Label sub = new Label("Platform revenue summary — all completed trips");
        sub.setTextFill(Color.web("#475569"));
        sub.setFont(Font.font("Arial", 13));

        // ── Top summary cards ─────────────────────────────────────────────────
        lblGross      = statLabel("#f1f5f9");
        lblCommission = statLabel("#ef4444");
        lblNet        = statLabel("#22c55e");

        HBox topCards = new HBox(16);
        topCards.getChildren().addAll(
            bigCard("Gross Revenue",       lblGross,      "#f1f5f9",
                "Total fares from completed trips"),
            bigCard("Platform Commission", lblCommission, "#ef4444",
                "15% retained by EthioRide"),
            bigCard("Net to Drivers",      lblNet,        "#22c55e",
                "85% paid out to drivers")
        );

        // Commission note
        Label note = new Label(
            "ℹ  Commission rate: 15% of each completed trip fare is retained as platform revenue.");
        note.setTextFill(Color.web("#475569"));
        note.setFont(Font.font("Arial", 11));
        note.setWrapText(true);
        note.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:10 14;");

        // ── Category breakdown ────────────────────────────────────────────────
        Label catTitle = new Label("Revenue by Category");
        catTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        catTitle.setTextFill(Color.web("#f1f5f9"));

        lblEconomy = statLabel("#22c55e");
        lblPremium = statLabel("#3b82f6");
        lblElite   = statLabel("#f59e0b");

        HBox catCards = new HBox(16);
        catCards.getChildren().addAll(
            bigCard("Economy",  lblEconomy, "#22c55e", "Gross from Economy trips"),
            bigCard("Premium",  lblPremium, "#3b82f6", "Gross from Premium trips"),
            bigCard("Elite",    lblElite,   "#f59e0b", "Gross from Elite trips")
        );

        // ── Daily revenue (last 30 days) ──────────────────────────────────────
        Label dailyTitle = new Label("Daily Revenue — Last 30 Days");
        dailyTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        dailyTitle.setTextFill(Color.web("#f1f5f9"));

        Button btnRefresh = new Button("↻ Refresh");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;" +
            "-fx-background-radius:6px;-fx-padding:7 16;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadReport());

        HBox dailyHeader = new HBox(12);
        dailyHeader.setAlignment(Pos.CENTER_LEFT);
        dailyHeader.getChildren().addAll(dailyTitle, btnRefresh);

        dailyList = new VBox(0);
        dailyList.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:4;");

        content.getChildren().addAll(title, sub, topCards, note,
            catTitle, catCards, dailyHeader, dailyList);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadReport() {
        lblGross.setText("Loading...");
        lblCommission.setText("...");
        lblNet.setText("...");
        dailyList.getChildren().clear();

        AdminService.getInstance().requestFinancialReport(report ->
            Platform.runLater(() -> renderReport(report)));
    }

    @SuppressWarnings("unchecked")
    private void renderReport(Map<String, Object> report) {
        if (report == null) {
            lblGross.setText("Error"); return;
        }

        double gross      = (double) report.getOrDefault("grossRevenue", 0.0);
        double commission = (double) report.getOrDefault("commission",   0.0);
        double net        = (double) report.getOrDefault("netRevenue",   0.0);

        lblGross.setText(String.format("ETB %.2f", gross));
        lblCommission.setText(String.format("ETB %.2f", commission));
        lblNet.setText(String.format("ETB %.2f", net));

        Map<String, Double> byCategory =
            (Map<String, Double>) report.getOrDefault("byCategory", Map.of());
        lblEconomy.setText(String.format("ETB %.2f", byCategory.getOrDefault("ECONOMY", 0.0)));
        lblPremium.setText(String.format("ETB %.2f", byCategory.getOrDefault("PREMIUM", 0.0)));
        lblElite.setText(String.format("ETB %.2f",   byCategory.getOrDefault("ELITE",   0.0)));

        Map<String, Double> byDay =
            (Map<String, Double>) report.getOrDefault("byDay", Map.of());

        dailyList.getChildren().clear();
        if (byDay.isEmpty()) {
            Label empty = new Label("No completed trips in the last 30 days.");
            empty.setTextFill(Color.web("#475569"));
            empty.setFont(Font.font("Arial", 13));
            empty.setPadding(new Insets(12));
            dailyList.getChildren().add(empty);
            return;
        }

        // Header row
        dailyList.getChildren().add(tableRow("Date", "Gross Revenue", "Commission (15%)", "Net", true));

        double maxVal = byDay.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        for (Map.Entry<String, Double> entry : byDay.entrySet()) {
            double dayGross = entry.getValue();
            double dayComm  = dayGross * 0.15;
            double dayNet   = dayGross - dayComm;
            HBox row = tableRow(entry.getKey(),
                String.format("ETB %.2f", dayGross),
                String.format("ETB %.2f", dayComm),
                String.format("ETB %.2f", dayNet), false);

            // Mini bar chart
            double pct = dayGross / maxVal;
            Region bar = new Region();
            bar.setPrefHeight(4);
            bar.setPrefWidth(pct * 200);
            bar.setStyle("-fx-background-color:#3b82f6;-fx-background-radius:2px;");
            ((VBox) row.getChildren().get(0)).getChildren().add(bar);

            dailyList.getChildren().add(row);
        }
    }

    private HBox tableRow(String date, String gross, String comm, String net, boolean header) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent;" +
            "-fx-border-width:0 0 1 0;-fx-padding:10 14;");

        String style = header
            ? "-fx-text-fill:#94a3b8;-fx-font-size:11px;-fx-font-weight:bold;"
            : "-fx-text-fill:#f1f5f9;-fx-font-size:12px;";

        VBox dateBox = new VBox();
        dateBox.setPrefWidth(160);
        Label dateLbl = new Label(date); dateLbl.setStyle(style);
        dateBox.getChildren().add(dateLbl);

        Label grossLbl = new Label(gross); grossLbl.setStyle(style); grossLbl.setPrefWidth(160);
        Label commLbl  = new Label(comm);  commLbl.setStyle(
            header ? style : "-fx-text-fill:#ef4444;-fx-font-size:12px;"); commLbl.setPrefWidth(160);
        Label netLbl   = new Label(net);   netLbl.setStyle(
            header ? style : "-fx-text-fill:#22c55e;-fx-font-size:12px;-fx-font-weight:bold;");
        netLbl.setPrefWidth(140);

        row.getChildren().addAll(dateBox, grossLbl, commLbl, netLbl);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label statLabel(String color) {
        Label lbl = new Label("...");
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lbl.setTextFill(Color.web(color));
        return lbl;
    }

    private VBox bigCard(String label, Label valueLabel, String color, String subText) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");
        card.setPrefWidth(240);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94a3b8")); lbl.setFont(Font.font("Arial", 12));
        Label sub = new Label(subText);
        sub.setTextFill(Color.web("#475569")); sub.setFont(Font.font("Arial", 10));
        card.getChildren().addAll(lbl, valueLabel, sub);
        return card;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;" +
            "-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
