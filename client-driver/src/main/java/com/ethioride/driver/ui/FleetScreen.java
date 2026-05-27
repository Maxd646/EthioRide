package com.ethioride.driver.ui;

import com.ethioride.driver.network.NetworkClient;
import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.shared.dto.UserDTO;
import com.ethioride.shared.enums.UserRole;
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

/**
 * Fleet screen — shows all registered drivers from the database.
 * Data is fetched live from the server via USER_LIST_REQUEST.
 */
public class FleetScreen {
    private final Stage stage;
    private ObservableList<UserDTO> allDrivers;
    private FilteredList<UserDTO> filtered;
    private Label lblCount;
    private TableView<UserDTO> table;

    public FleetScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 720));
        stage.setResizable(true);
        stage.show();
        loadDrivers();
    }

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
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        Button btnSignOut = navBtn("↩  Sign Out");
        btnSignOut.setOnAction(e -> { DriverSessionState.getInstance().clear(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, sub, btnMap, btnHistory, btnPayments, btnFleet, btnEarnings, spacer, btnSignOut);
        return s;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(28));
        content.setStyle("-fx-background-color:#0a0e1a;");

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Fleet");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search drivers...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;" +
            "-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;" +
            "-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8 14;-fx-min-width:200px;");

        Button btnRefresh = new Button("↻ Refresh");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;" +
            "-fx-background-radius:6px;-fx-padding:8 14;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadDrivers());

        header.getChildren().addAll(title, spacer, tfSearch, btnRefresh);

        // Table
        table = new TableView<>();
        table.setStyle("-fx-background-color:#0d1526;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<UserDTO, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colName.setCellFactory(c -> styledCell("#f1f5f9"));

        TableColumn<UserDTO, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));
        colPhone.setCellFactory(c -> styledCell("#94a3b8"));

        TableColumn<UserDTO, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));
        colEmail.setCellFactory(c -> styledCell("#94a3b8"));

        TableColumn<UserDTO, String> colRating = new TableColumn<>("Rating");
        colRating.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.1f ★", d.getValue().getRating())));
        colRating.setCellFactory(c -> styledCell("#f59e0b"));

        table.getColumns().addAll(colName, colPhone, colEmail, colRating);
        table.setPlaceholder(new Label("No drivers found.") {{
            setStyle("-fx-text-fill:#475569;");
        }});

        allDrivers = FXCollections.observableArrayList();
        filtered = new FilteredList<>(allDrivers, d -> true);
        table.setItems(filtered);

        tfSearch.textProperty().addListener((o, ov, nv) -> {
            filtered.setPredicate(d ->
                nv.isEmpty() ||
                d.getFullName().toLowerCase().contains(nv.toLowerCase()) ||
                d.getPhone().contains(nv));
            lblCount.setText(filtered.size() + " drivers");
        });

        // Footer
        HBox footer = new HBox();
        lblCount = new Label("Loading...");
        lblCount.setTextFill(Color.web("#94a3b8"));
        lblCount.setFont(Font.font("Arial", 12));
        footer.getChildren().add(lblCount);

        content.getChildren().addAll(header, table, footer);
        return content;
    }

    private void loadDrivers() {
        lblCount.setText("Loading...");
        try {
            NetworkClient.getInstance().connect();
            NetworkClient.getInstance().sendRequest(
                MessageType.USER_LIST_REQUEST, null,
                MessageType.USER_LIST_RESPONSE, msg -> {
                    @SuppressWarnings("unchecked")
                    List<UserDTO> users = (List<UserDTO>) msg.getPayload();
                    Platform.runLater(() -> {
                        allDrivers.clear();
                        users.stream()
                            .filter(u -> u.getRole() == UserRole.DRIVER)
                            .forEach(allDrivers::add);
                        lblCount.setText(allDrivers.size() + " drivers");
                    });
                });
        } catch (Exception e) {
            Platform.runLater(() -> lblCount.setText("Could not load drivers"));
        }
    }

    private TableCell<UserDTO, String> styledCell(String color) {
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
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;" +
            "-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
