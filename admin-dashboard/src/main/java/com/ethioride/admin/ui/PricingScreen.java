package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
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

public class PricingScreen {
    private final Stage stage;

    // ECONOMY fields
    private TextField tfEcoBase, tfEcoPerKm, tfEcoPerMin, tfEcoMin, tfEcoBooking;
    // PREMIUM fields
    private TextField tfPreBase, tfPrePerKm, tfPrePerMin, tfPreMin, tfPreBooking;
    // ELITE fields
    private TextField tfEliBase, tfEliPerKm, tfEliPerMin, tfEliMin, tfEliBooking;

    private Label lblStatus;

    public PricingScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 700));
        stage.setResizable(true);
        stage.show();
        loadPricingRules();
    }

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
        Button btnSystem  = navBtn("🖥  System");
        btnPricing.setStyle(btnPricing.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e    -> new DashboardScreen(stage).show());
        btnDrivers.setOnAction(e -> new DriversScreen(stage).show());
        btnTrips.setOnAction(e   -> new TripsScreen(stage).show());
        btnUsers.setOnAction(e   -> new UsersScreen(stage).show());
        btnSystem.setOnAction(e  -> new SystemScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> {
            AdminService.getInstance().disconnect();
            AdminSession.getInstance().logout();
            new LoginScreen(stage).show();
        });
        s.getChildren().addAll(logo, btnDash, btnDrivers, btnTrips, btnUsers, btnPricing, btnSystem, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Pricing Management");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        Label subtitle = new Label("Set base fares, per-km rates, and per-minute rates for each ride category.");
        subtitle.setTextFill(Color.web("#94a3b8"));
        subtitle.setFont(Font.font("Arial", 13));

        // Pricing cards
        tfEcoBase    = new TextField(); tfEcoPerKm  = new TextField();
        tfEcoPerMin  = new TextField(); tfEcoMin    = new TextField(); tfEcoBooking = new TextField();
        tfPreBase    = new TextField(); tfPrePerKm  = new TextField();
        tfPrePerMin  = new TextField(); tfPreMin    = new TextField(); tfPreBooking = new TextField();
        tfEliBase    = new TextField(); tfEliPerKm  = new TextField();
        tfEliPerMin  = new TextField(); tfEliMin    = new TextField(); tfEliBooking = new TextField();

        HBox cards = new HBox(20);
        cards.getChildren().addAll(
            pricingCard("🚗  ECONOMY",  "#22c55e", tfEcoBase, tfEcoPerKm, tfEcoPerMin, tfEcoMin, tfEcoBooking),
            pricingCard("🚐  PREMIUM",  "#3b82f6", tfPreBase, tfPrePerKm, tfPrePerMin, tfPreMin, tfPreBooking),
            pricingCard("🚙  ELITE",    "#f59e0b", tfEliBase, tfEliPerKm, tfEliPerMin, tfEliMin, tfEliBooking)
        );

        // Status label
        lblStatus = new Label();
        lblStatus.setFont(Font.font("Arial", 13));
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);

        // Save button
        Button btnSave = new Button("💾  Save All Pricing Rules");
        btnSave.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btnSave.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:#0a0e1a;" +
                         "-fx-background-radius:8px;-fx-padding:12 24;-fx-cursor:hand;");
        btnSave.setOnAction(e -> savePricingRules());

        // Formula explanation
        VBox formula = new VBox(8);
        formula.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                         "-fx-border-radius:10px;-fx-background-radius:10px;-fx-padding:16;");
        Label lblFormula = new Label("Pricing Formula");
        lblFormula.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblFormula.setTextFill(Color.web("#f1f5f9"));
        Label lblCalc = new Label(
            "Total = MAX(Base Fare + (Distance km × Per km Rate) + (Duration min × Per min Rate) + Booking Fee,  Minimum Fare)");
        lblCalc.setTextFill(Color.web("#94a3b8"));
        lblCalc.setFont(Font.font("Courier New", 12));
        lblCalc.setWrapText(true);
        Label lblExample = new Label("Example: 5 km trip, 12 min, ECONOMY  →  50 + (5×15) + (12×2) + 10 = 159 ETB");
        lblExample.setTextFill(Color.web("#475569"));
        lblExample.setFont(Font.font("Arial", 12));
        formula.getChildren().addAll(lblFormula, lblCalc, lblExample);

        content.getChildren().addAll(title, subtitle, cards, lblStatus, btnSave, formula);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    private VBox pricingCard(String title, String color,
                              TextField tfBase, TextField tfPerKm,
                              TextField tfPerMin, TextField tfMin, TextField tfBooking) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:" + color + ";" +
                      "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");
        card.setPrefWidth(280);

        Label lbl = new Label(title);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lbl.setTextFill(Color.web(color));

        card.getChildren().addAll(
            lbl,
            fieldRow("Base Fare (ETB)",       tfBase),
            fieldRow("Per km Rate (ETB)",      tfPerKm),
            fieldRow("Per min Rate (ETB)",     tfPerMin),
            fieldRow("Minimum Fare (ETB)",     tfMin),
            fieldRow("Booking Fee (ETB)",      tfBooking)
        );
        return card;
    }

    private VBox fieldRow(String label, TextField tf) {
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web("#94a3b8"));
        lbl.setFont(Font.font("Arial", 11));
        tf.setStyle("-fx-background-color:#060a14;-fx-text-fill:#f1f5f9;" +
                    "-fx-border-color:#1e3a5f;-fx-border-radius:6px;-fx-background-radius:6px;" +
                    "-fx-padding:8 12;-fx-font-size:13px;");
        tf.setMaxWidth(Double.MAX_VALUE);
        VBox row = new VBox(4, lbl, tf);
        return row;
    }

    private void loadPricingRules() {
        AdminService.getInstance().requestPricingRules(rules -> Platform.runLater(() -> {
            if (rules == null) return;
            // rules format: "ECONOMY:base:perKm:perMin:min:booking|PREMIUM:...|ELITE:..."
            for (String part : rules.split("\\|")) {
                String[] f = part.split(":");
                if (f.length < 6) continue;
                switch (f[0]) {
                    case "ECONOMY" -> setFields(tfEcoBase, tfEcoPerKm, tfEcoPerMin, tfEcoMin, tfEcoBooking, f);
                    case "PREMIUM" -> setFields(tfPreBase, tfPrePerKm, tfPrePerMin, tfPreMin, tfPreBooking, f);
                    case "ELITE"   -> setFields(tfEliBase, tfEliPerKm, tfEliPerMin, tfEliMin, tfEliBooking, f);
                }
            }
        }));
    }

    private void setFields(TextField base, TextField perKm, TextField perMin,
                           TextField min, TextField booking, String[] f) {
        base.setText(f[1]); perKm.setText(f[2]); perMin.setText(f[3]);
        min.setText(f[4]);  booking.setText(f[5]);
    }

    private void savePricingRules() {
        try {
            // Validate all fields are numbers
            double ecoBase = Double.parseDouble(tfEcoBase.getText().trim());
            double ecoKm   = Double.parseDouble(tfEcoPerKm.getText().trim());
            double ecoMin  = Double.parseDouble(tfEcoPerMin.getText().trim());
            double ecoMinF = Double.parseDouble(tfEcoMin.getText().trim());
            double ecoBk   = Double.parseDouble(tfEcoBooking.getText().trim());

            double preBase = Double.parseDouble(tfPreBase.getText().trim());
            double preKm   = Double.parseDouble(tfPrePerKm.getText().trim());
            double preMin  = Double.parseDouble(tfPrePerMin.getText().trim());
            double preMinF = Double.parseDouble(tfPreMin.getText().trim());
            double preBk   = Double.parseDouble(tfPreBooking.getText().trim());

            double eliBase = Double.parseDouble(tfEliBase.getText().trim());
            double eliKm   = Double.parseDouble(tfEliPerKm.getText().trim());
            double eliMin  = Double.parseDouble(tfEliPerMin.getText().trim());
            double eliMinF = Double.parseDouble(tfEliMin.getText().trim());
            double eliBk   = Double.parseDouble(tfEliBooking.getText().trim());

            // Format: "ECONOMY:base:perKm:perMin:min:booking|PREMIUM:...|ELITE:..."
            String payload = String.format(
                "ECONOMY:%.2f:%.2f:%.2f:%.2f:%.2f|PREMIUM:%.2f:%.2f:%.2f:%.2f:%.2f|ELITE:%.2f:%.2f:%.2f:%.2f:%.2f",
                ecoBase, ecoKm, ecoMin, ecoMinF, ecoBk,
                preBase, preKm, preMin, preMinF, preBk,
                eliBase, eliKm, eliMin, eliMinF, eliBk
            );

            AdminService.getInstance().savePricingRules(payload, response ->
                Platform.runLater(() -> {
                    if ("OK".equals(response)) {
                        showStatus("✓ Pricing rules saved successfully.", true);
                    } else {
                        showStatus("✗ Failed to save: " + response, false);
                    }
                }));
        } catch (NumberFormatException e) {
            showStatus("✗ All fields must be valid numbers.", false);
        }
    }

    private void showStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(success ? Color.web("#22c55e") : Color.web("#ef4444"));
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;" +
                     "-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
