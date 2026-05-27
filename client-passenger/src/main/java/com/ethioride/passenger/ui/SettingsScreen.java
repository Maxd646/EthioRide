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

public class SettingsScreen {
    private final Stage stage;
    private VBox savedLocationsContainer;

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

    // ── Sidebar ───────────────────────────────────────────────────────────────

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

    // ── Content ───────────────────────────────────────────────────────────────

    private ScrollPane buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Settings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        content.getChildren().addAll(title, buildProfileCard(),
                buildSavedLocationsCard(), buildSignOutButton());

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#0a0e1a;-fx-background:#0a0e1a;");
        return sp;
    }

    // ── Profile card (read-only) ──────────────────────────────────────────────

    private VBox buildProfileCard() {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");

        Label lbl = new Label("Profile");
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lbl.setTextFill(Color.web("#f1f5f9"));

        SessionState s = SessionState.getInstance();
        String name  = s.isLoggedIn() ? s.getCurrentUser().getFullName() : "—";
        String phone = s.isLoggedIn() ? s.getCurrentUser().getPhone()    : "—";
        String email = s.isLoggedIn() && s.getCurrentUser().getEmail() != null
                       ? s.getCurrentUser().getEmail() : "—";

        card.getChildren().addAll(lbl,
                infoRow("Full Name", name),
                infoRow("Phone",     phone),
                infoRow("Email",     email));
        return card;
    }

    // ── Saved locations card ──────────────────────────────────────────────────

    private VBox buildSavedLocationsCard() {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:20;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Saved Locations");
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lbl.setTextFill(Color.web("#f1f5f9"));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblCount = new Label();
        lblCount.setFont(Font.font("Arial", 11));
        lblCount.setTextFill(Color.web("#475569"));
        header.getChildren().addAll(lbl, spacer, lblCount);

        Label lblHint = new Label("Tap a saved location on the map screen to auto-fill your destination. Max 6.");
        lblHint.setTextFill(Color.web("#475569"));
        lblHint.setFont(Font.font("Arial", 11));
        lblHint.setWrapText(true);

        // List of existing locations
        savedLocationsContainer = new VBox(8);
        refreshLocationList(savedLocationsContainer, lblCount);

        // Add new location form
        Label lblAdd = new Label("Add New Location");
        lblAdd.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblAdd.setTextFill(Color.web("#94a3b8"));

        TextField tfName = new TextField();
        tfName.setPromptText("Name  (e.g. Home, Work, Gym)");
        styleField(tfName);

        TextField tfAddress = new TextField();
        tfAddress.setPromptText("Address  (e.g. Bole, Addis Ababa)");
        styleField(tfAddress);

        Label lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("Arial", 11));
        lblError.setVisible(false);
        lblError.setManaged(false);

        Button btnAdd = new Button("+ Add Location");
        btnAdd.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        btnAdd.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;" +
                "-fx-background-radius:8px;-fx-padding:8 18;-fx-cursor:hand;");
        btnAdd.setOnAction(e -> {
            String name    = tfName.getText().trim();
            String address = tfAddress.getText().trim();

            if (name.isEmpty() || address.isEmpty()) {
                showError(lblError, "Both name and address are required.");
                return;
            }
            if (SessionState.getInstance().getSavedLocations().size() >= SessionState.MAX_SAVED_LOCATIONS) {
                showError(lblError, "Maximum 6 saved locations reached. Delete one first.");
                return;
            }
            boolean added = SessionState.getInstance().addSavedLocation(name, address);
            if (!added) {
                showError(lblError, "A location named \"" + name + "\" already exists.");
                return;
            }
            tfName.clear();
            tfAddress.clear();
            lblError.setVisible(false);
            lblError.setManaged(false);
            refreshLocationList(savedLocationsContainer, lblCount);
        });

        card.getChildren().addAll(header, lblHint, savedLocationsContainer,
                new Separator(), lblAdd, tfName, tfAddress, lblError, btnAdd);
        return card;
    }

    /** Rebuilds the list of saved location rows. */
    private void refreshLocationList(VBox container, Label lblCount) {
        container.getChildren().clear();
        List<SessionState.SavedLocation> locs = SessionState.getInstance().getSavedLocations();
        int count = locs.size();
        lblCount.setText(count + " / " + SessionState.MAX_SAVED_LOCATIONS);

        if (locs.isEmpty()) {
            Label empty = new Label("No saved locations yet.");
            empty.setTextFill(Color.web("#475569"));
            empty.setFont(Font.font("Arial", 12));
            container.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < locs.size(); i++) {
            final int idx = i;
            SessionState.SavedLocation loc = locs.get(i);

            // Pick icon
            String icon = switch (loc.getName().toLowerCase()) {
                case "home"     -> "🏠";
                case "work"     -> "💼";
                case "school"   -> "🏫";
                case "gym"      -> "🏋";
                case "hospital" -> "🏥";
                case "airport"  -> "✈";
                default         -> "⭐";
            };

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#060a14;-fx-border-color:#1e3a5f;" +
                    "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:10 14;");

            Label lblIcon = new Label(icon);
            lblIcon.setFont(Font.font("Arial", 16));

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label lblName = new Label(loc.getName());
            lblName.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            lblName.setTextFill(Color.web("#f1f5f9"));
            Label lblAddr = new Label(loc.getAddress());
            lblAddr.setFont(Font.font("Arial", 11));
            lblAddr.setTextFill(Color.web("#475569"));
            info.getChildren().addAll(lblName, lblAddr);

            Button btnDelete = new Button("✕");
            btnDelete.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            btnDelete.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:#fca5a5;" +
                    "-fx-background-radius:6px;-fx-padding:4 10;-fx-cursor:hand;");
            btnDelete.setOnAction(e -> {
                SessionState.getInstance().removeSavedLocation(idx);
                refreshLocationList(container, lblCount);
            });

            row.getChildren().addAll(lblIcon, info, btnDelete);
            container.getChildren().add(row);
        }
    }

    // ── Sign out button ───────────────────────────────────────────────────────

    private Button buildSignOutButton() {
        Button btn = new Button("Sign Out");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btn.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" +
                "-fx-background-radius:10px;-fx-padding:12px;-fx-cursor:hand;");
        btn.setOnAction(e -> {
            SessionState.getInstance().clear();
            new LoginScreen(stage).show();
        });
        return btn;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
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

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color:#060a14;-fx-text-fill:#f1f5f9;" +
                "-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:9 14;-fx-font-size:13px;");
        f.setMaxWidth(Double.MAX_VALUE);
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
