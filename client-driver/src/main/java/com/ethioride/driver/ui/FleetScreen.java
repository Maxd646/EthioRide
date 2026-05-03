package com.ethioride.driver.ui;

import com.ethioride.driver.state.DriverSessionState;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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

/**
 * Fleet screen — shows all drivers, their status, location, rating, and trips today.
 * Converted from fleet.fxml to programmatic JavaFX.
 */
public class FleetScreen {
    private final Stage stage;

    private ObservableList<String[]> allDrivers;
    private FilteredList<String[]> filtered;
    private Label lblCount;
    private TableView<String[]> table;

    public FleetScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 720));
        stage.setResizable(true);
        stage.show();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox s = new VBox(0);
        s.setPrefWidth(220);
        s.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-width:0 1 0 0;");

        Label logo = new Label("🚗 EthioRide");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#22c55e"));
        logo.setPadding(new Insets(24, 20, 4, 20));

        Label sub = new Label("DRIVER DASHBOARD");
        sub.setFont(Font.font("Arial", 10));
        sub.setTextFill(Color.web("#475569"));
        sub.setPadding(new Insets(0, 20, 20, 20));

        Button btnMap      = navBtn("🗺  Map View");
        Button btnHistory  = navBtn("🕐  Ride History");
        Button btnPayments = navBtn("💳  Payments");
        Button btnFleet    = navBtn("🚘  Fleet");
        Button btnEarnings = navBtn("💰  Earnings");

        btnFleet.setStyle(btnFleet.getStyle() + "-fx-background-color:#1e3a5f;");

        btnMap.setOnAction(e      -> new MainScreen(stage).show());
        btnHistory.setOnAction(e  -> new RideHistoryScreen(stage).show());
        btnPayments.setOnAction(e -> new PaymentsScreen(stage).show());
        btnEarnings.setOnAction(e -> new EarningsScreen(stage).show());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnSupport = navBtn("📍  Support");
        Button btnSignOut = navBtn("↩  Sign Out");
        btnSignOut.setOnAction(e -> {
            DriverSessionState.getInstance().clear();
            new LoginScreen(stage).show();
        });

        s.getChildren().addAll(logo, sub, btnMap, btnHistory, btnPayments, btnFleet, btnEarnings,
                spacer, btnSupport, btnSignOut);
        return s;
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(28));
        content.setStyle("-fx-background-color:#0a0e1a;");

        // Header row
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Fleet");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search drivers...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;" +
                "-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8 14;-fx-font-size:13px;-fx-min-width:200px;");

        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("All", "Online", "On Trip", "Offline");
        cbStatus.setValue("All");
        cbStatus.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:6px;");

        header.getChildren().addAll(title, spacer, tfSearch, cbStatus);

        // Stat cards
        HBox stats = new HBox(12);
        Label lblOnline  = statCard("ONLINE",   "0", "#22c55e");
        Label lblOnTrip  = statCard("ON TRIP",  "0", "#f59e0b");
        Label lblOffline = statCard("OFFLINE",  "0", "#ef4444");
        stats.getChildren().addAll(
            wrapCard("ONLINE",  lblOnline,  "#22c55e"),
            wrapCard("ON TRIP", lblOnTrip,  "#f59e0b"),
            wrapCard("OFFLINE", lblOffline, "#ef4444")
        );

        // Table
        table = new TableView<>();
        table.setStyle("-fx-background-color:#0d1526;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().addAll(
            col("Driver ID",   0, "#94a3b8"),
            col("Name",        1, "#f1f5f9"),
            statusCol(),
            col("Location",    3, "#94a3b8"),
            col("Rating",      4, "#f59e0b"),
            col("Trips Today", 5, "#94a3b8")
        );
        table.setPlaceholder(new Label("No drivers found.") {{
            setStyle("-fx-text-fill:#475569;");
        }});

        allDrivers = FXCollections.observableArrayList(List.of(
            new String[]{"DRV-001", "Abebe Girma",   "Online",  "Bole, Addis Ababa",      "4.9", "8"},
            new String[]{"DRV-002", "Tigist Haile",  "On Trip", "Kazanchis, Addis Ababa", "4.7", "5"},
            new String[]{"DRV-003", "Dawit Bekele",  "Online",  "Piassa, Addis Ababa",    "4.8", "11"},
            new String[]{"DRV-004", "Hanna Tesfaye", "Offline", "—",                      "4.6", "0"},
            new String[]{"DRV-005", "Yonas Alemu",   "On Trip", "Sarbet, Addis Ababa",    "4.9", "7"}
        ));
        filtered = new FilteredList<>(allDrivers, d -> true);
        table.setItems(filtered);

        // Update stat cards
        updateStats(lblOnline, lblOnTrip, lblOffline);

        // Footer
        HBox footer = new HBox();
        lblCount = new Label(allDrivers.size() + " drivers");
        lblCount.setTextFill(Color.web("#94a3b8"));
        lblCount.setFont(Font.font("Arial", 12));
        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        Button btnRefresh = new Button("Refresh");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:6 14;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> table.refresh());
        footer.getChildren().addAll(lblCount, fSpacer, btnRefresh);

        // Wire filters
        tfSearch.textProperty().addListener((o, ov, nv) -> applyFilter(nv, cbStatus.getValue(), lblOnline, lblOnTrip, lblOffline));
        cbStatus.setOnAction(e -> applyFilter(tfSearch.getText(), cbStatus.getValue(), lblOnline, lblOnTrip, lblOffline));

        content.getChildren().addAll(header, stats, table, footer);
        return content;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyFilter(String q, String status, Label lblOnline, Label lblOnTrip, Label lblOffline) {
        filtered.setPredicate(d ->
            (q.isEmpty() || d[0].toLowerCase().contains(q.toLowerCase()) || d[1].toLowerCase().contains(q.toLowerCase()))
            && ("All".equals(status) || d[2].equals(status))
        );
        lblCount.setText(filtered.size() + " drivers");
        updateStats(lblOnline, lblOnTrip, lblOffline);
    }

    private void updateStats(Label lblOnline, Label lblOnTrip, Label lblOffline) {
        long online  = allDrivers.stream().filter(d -> "Online".equals(d[2])).count();
        long onTrip  = allDrivers.stream().filter(d -> "On Trip".equals(d[2])).count();
        long offline = allDrivers.stream().filter(d -> "Offline".equals(d[2])).count();
        lblOnline.setText(String.valueOf(online));
        lblOnTrip.setText(String.valueOf(onTrip));
        lblOffline.setText(String.valueOf(offline));
    }

    private VBox wrapCard(String labelText, Label valueLabel, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#0d1526;-fx-border-color:#1e3a5f;-fx-border-radius:12px;-fx-background-radius:12px;-fx-padding:16;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lbl = new Label(labelText);
        lbl.setTextFill(Color.web("#475569"));
        lbl.setFont(Font.font("Arial", 10));
        card.getChildren().addAll(lbl, valueLabel);
        return card;
    }

    private Label statCard(String label, String value, String color) {
        Label val = new Label(value);
        val.setTextFill(Color.web(color));
        val.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        return val;
    }

    private TableColumn<String[], String> col(String header, int idx, String color) {
        TableColumn<String[], String> c = new TableColumn<>(header);
        c.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[idx]));
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(empty ? "" : "-fx-text-fill:" + color + ";");
            }
        });
        return c;
    }

    private TableColumn<String[], String> statusCol() {
        TableColumn<String[], String> c = new TableColumn<>("Status");
        c.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Online"  -> "-fx-text-fill:#22c55e;";
                    case "On Trip" -> "-fx-text-fill:#f59e0b;";
                    default        -> "-fx-text-fill:#ef4444;";
                });
            }
        });
        return c;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
