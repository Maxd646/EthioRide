package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
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

public class DriversScreen {
    private final Stage stage;
    private ObservableList<String[]> allDrivers;
    private FilteredList<String[]> filtered;
    private Label lblCount;

    public DriversScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 700));
        stage.setResizable(true);
        stage.show();
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
        Button btnSystem  = navBtn("🖥  System");
        btnDrivers.setStyle(btnDrivers.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e   -> new DashboardScreen(stage).show());
        btnTrips.setOnAction(e  -> new TripsScreen(stage).show());
        btnSystem.setOnAction(e -> new SystemScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { AdminService.getInstance().disconnect(); AdminSession.getInstance().logout(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnDash, btnDrivers, btnTrips, btnSystem, sp, btnOut);
        return s;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Drivers");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search drivers...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);
        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("All", "Online", "On Trip", "Offline");
        cbStatus.setValue("All");
        cbStatus.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        lblCount = new Label("5 drivers");
        lblCount.setTextFill(Color.web("#94a3b8")); lblCount.setFont(Font.font("Arial", 12));
        toolbar.getChildren().addAll(tfSearch, cbStatus, lblCount);

        // Table
        TableView<String[]> table = new TableView<>();
        table.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getColumns().addAll(
            col("ID",       0, "#94a3b8"),
            col("Name",     1, "#f1f5f9"),
            statusCol(),
            col("Location", 3, "#94a3b8"),
            col("Rating",   4, "#f59e0b"),
            col("Trips",    5, "#94a3b8")
        );

        allDrivers = FXCollections.observableArrayList(List.of(
            new String[]{"DRV-001", "Abebe Girma",   "Online",  "Bole, Addis Ababa",      "4.9", "8"},
            new String[]{"DRV-002", "Tigist Haile",  "On Trip", "Kazanchis, Addis Ababa", "4.7", "5"},
            new String[]{"DRV-003", "Dawit Bekele",  "Online",  "Piassa, Addis Ababa",    "4.8", "11"},
            new String[]{"DRV-004", "Hanna Tesfaye", "Offline", "—",                      "4.6", "0"},
            new String[]{"DRV-005", "Yonas Alemu",   "On Trip", "Sarbet, Addis Ababa",    "4.9", "7"}
        ));
        filtered = new FilteredList<>(allDrivers, d -> true);
        table.setItems(filtered);

        tfSearch.textProperty().addListener((o, ov, nv) -> applyFilter(nv, cbStatus.getValue(), table));
        cbStatus.setOnAction(e -> applyFilter(tfSearch.getText(), cbStatus.getValue(), table));

        content.getChildren().addAll(title, toolbar, table);
        return content;
    }

    private void applyFilter(String q, String status, TableView<?> table) {
        filtered.setPredicate(d ->
            (q.isEmpty() || d[0].toLowerCase().contains(q.toLowerCase()) || d[1].toLowerCase().contains(q.toLowerCase()))
            && ("All".equals(status) || d[2].equals(status))
        );
        lblCount.setText(filtered.size() + " drivers");
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
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
