package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
import com.ethioride.shared.dto.TripRequestDTO;
import com.ethioride.shared.protocol.Message;
import com.ethioride.shared.protocol.MessageType;
import javafx.application.Platform;
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
    private ObservableList<TripRequestDTO> allTrips;
    private FilteredList<TripRequestDTO> filtered;
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
        loadTrips();
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
        btnTrips.setStyle(btnTrips.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e    -> new DashboardScreen(stage).show());
        btnDrivers.setOnAction(e -> new DriversScreen(stage).show());
        btnUsers.setOnAction(e   -> new UsersScreen(stage).show());
        btnPricing.setOnAction(e -> new PricingScreen(stage).show());
        btnSystem.setOnAction(e  -> new SystemScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { AdminService.getInstance().disconnect(); AdminSession.getInstance().logout(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnDash, btnDrivers, btnTrips, btnUsers, btnPricing, btnSystem, sp, btnOut);
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
        tfSearch.setPromptText("Search by pickup or dropoff...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;" +
                          "-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);

        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("All", "PENDING", "ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        cbStatus.setValue("All");
        cbStatus.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");

        Button btnRefresh = new Button("↻ Refresh");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:8 14;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadTrips());

        lblCount = new Label("Loading...");
        lblCount.setTextFill(Color.web("#94a3b8"));
        lblCount.setFont(Font.font("Arial", 12));
        toolbar.getChildren().addAll(tfSearch, cbStatus, btnRefresh, lblCount);

        TableView<TripRequestDTO> table = new TableView<>();
        table.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Passenger name is stored in passengerPhone field as "passengerName|driverName"
        TableColumn<TripRequestDTO, String> colPassenger = new TableColumn<>("Passenger");
        colPassenger.setCellValueFactory(d -> {
            String raw = d.getValue().getPassengerPhone();
            String name = raw != null && raw.contains("|") ? raw.split("\\|")[0] : "Unknown";
            return new SimpleStringProperty(name);
        });
        colPassenger.setCellFactory(c -> styledCell("#f1f5f9"));

        TableColumn<TripRequestDTO, String> colDriver = new TableColumn<>("Driver");
        colDriver.setCellValueFactory(d -> {
            String raw = d.getValue().getPassengerPhone();
            String name = raw != null && raw.contains("|") ? raw.split("\\|")[1] : "—";
            return new SimpleStringProperty(name);
        });
        colDriver.setCellFactory(c -> styledCell("#f1f5f9"));

        TableColumn<TripRequestDTO, String> colPickup = new TableColumn<>("Pickup");
        colPickup.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPickupLocation()));
        colPickup.setCellFactory(c -> styledCell("#94a3b8"));

        TableColumn<TripRequestDTO, String> colDropoff = new TableColumn<>("Drop-off");
        colDropoff.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDropoffLocation()));
        colDropoff.setCellFactory(c -> styledCell("#94a3b8"));

        TableColumn<TripRequestDTO, String> colFare = new TableColumn<>("Fare");
        colFare.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("ETB %.2f", d.getValue().getFare())));
        colFare.setCellFactory(c -> styledCell("#22c55e"));

        TableColumn<TripRequestDTO, String> colDist = new TableColumn<>("Distance");
        colDist.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.1f km", d.getValue().getDistanceKm())));
        colDist.setCellFactory(c -> styledCell("#94a3b8"));

        TableColumn<TripRequestDTO, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getStatus() != null ? d.getValue().getStatus().name() : "—"));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "IN_PROGRESS" -> "-fx-text-fill:#f59e0b;";
                    case "COMPLETED"   -> "-fx-text-fill:#22c55e;";
                    case "CANCELLED"   -> "-fx-text-fill:#ef4444;";
                    case "ACCEPTED"    -> "-fx-text-fill:#3b82f6;";
                    default            -> "-fx-text-fill:#94a3b8;";
                });
            }
        });

        table.getColumns().addAll(colPassenger, colDriver, colPickup, colDropoff, colFare, colDist, colStatus);

        allTrips = FXCollections.observableArrayList();
        filtered = new FilteredList<>(allTrips, t -> true);
        table.setItems(filtered);

        tfSearch.textProperty().addListener((o, ov, nv) -> applyFilter(nv, cbStatus.getValue()));
        cbStatus.setOnAction(e -> applyFilter(tfSearch.getText(), cbStatus.getValue()));

        content.getChildren().addAll(title, toolbar, table);
        return content;
    }

    private void loadTrips() {
        lblCount.setText("Loading...");
        AdminService.getInstance().requestTripList(trips -> Platform.runLater(() -> {
            allTrips.setAll(trips);
            lblCount.setText(trips.size() + " trips");
        }));
    }

    private void applyFilter(String q, String status) {
        filtered.setPredicate(t ->
            (q.isEmpty() ||
             t.getPickupLocation().toLowerCase().contains(q.toLowerCase()) ||
             t.getDropoffLocation().toLowerCase().contains(q.toLowerCase()))
            && ("All".equals(status) || (t.getStatus() != null && t.getStatus().name().equals(status)))
        );
        lblCount.setText(filtered.size() + " trips");
    }

    private TableCell<TripRequestDTO, String> styledCell(String color) {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(empty ? "" : "-fx-text-fill:" + color + ";");
            }
        };
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Arial", 13));
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
