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

public class TripsScreen {
    private final Stage stage;
    private ObservableList<String[]> allTrips;
    private FilteredList<String[]> filtered;
    private Label lblCount;

    public TripsScreen(Stage stage) { this.stage = stage; }

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
        btnTrips.setStyle(btnTrips.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e    -> new DashboardScreen(stage).show());
        btnDrivers.setOnAction(e -> new DriversScreen(stage).show());
        btnSystem.setOnAction(e  -> new SystemScreen(stage).show());
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

        Label title = new Label("Trips");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search trips...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);
        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("All", "PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        cbStatus.setValue("All");
        cbStatus.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        lblCount = new Label("5 trips");
        lblCount.setTextFill(Color.web("#94a3b8")); lblCount.setFont(Font.font("Arial", 12));
        toolbar.getChildren().addAll(tfSearch, cbStatus, lblCount);

        TableView<String[]> table = new TableView<>();
        table.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getColumns().addAll(
            col("Trip ID",    0, "#94a3b8"),
            col("Passenger",  1, "#f1f5f9"),
            col("Driver",     2, "#f1f5f9"),
            col("Pickup",     3, "#94a3b8"),
            col("Drop-off",   4, "#94a3b8"),
            col("Fare",       5, "#22c55e"),
            tripStatusCol()
        );

        allTrips = FXCollections.observableArrayList(List.of(
            new String[]{"T-8821", "Hanna T.",  "Dawit B.",  "Edna Mall, Bole",  "Sarbet",    "145.00", "IN_PROGRESS"},
            new String[]{"T-8820", "Yonas A.",  "Abebe G.",  "Meskel Square",    "Bole Airport","320.00","IN_PROGRESS"},
            new String[]{"T-8819", "Sara M.",   "Tigist H.", "Piassa",           "Kazanchis", "90.00",  "COMPLETED"},
            new String[]{"T-8818", "Biruk T.",  "Yonas A.",  "CMC",              "Megenagna", "75.00",  "COMPLETED"},
            new String[]{"T-8817", "Meron K.",  "—",         "Bole Medhanialem", "Gerji",     "110.00", "CANCELLED"}
        ));
        filtered = new FilteredList<>(allTrips, t -> true);
        table.setItems(filtered);

        tfSearch.textProperty().addListener((o, ov, nv) -> applyFilter(nv, cbStatus.getValue()));
        cbStatus.setOnAction(e -> applyFilter(tfSearch.getText(), cbStatus.getValue()));

        content.getChildren().addAll(title, toolbar, table);
        return content;
    }

    private void applyFilter(String q, String status) {
        filtered.setPredicate(t ->
            (q.isEmpty() || t[0].toLowerCase().contains(q.toLowerCase()) || t[1].toLowerCase().contains(q.toLowerCase()))
            && ("All".equals(status) || t[6].equals(status))
        );
        lblCount.setText(filtered.size() + " trips");
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

    private TableColumn<String[], String> tripStatusCol() {
        TableColumn<String[], String> c = new TableColumn<>("Status");
        c.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[6]));
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "IN_PROGRESS" -> "-fx-text-fill:#f59e0b;";
                    case "COMPLETED"   -> "-fx-text-fill:#22c55e;";
                    case "CANCELLED"   -> "-fx-text-fill:#ef4444;";
                    default            -> "-fx-text-fill:#94a3b8;";
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
