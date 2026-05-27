package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
import com.ethioride.shared.dto.UserDTO;
import com.ethioride.shared.enums.UserRole;
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

public class DriversScreen {
    private final Stage stage;
    private ObservableList<UserDTO> allDrivers;
    private FilteredList<UserDTO> filtered;
    private Label lblCount;
    private TableView<UserDTO> table;

    public DriversScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 700));
        stage.setResizable(true);
        stage.show();
        loadDrivers();
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
        Button btnReport  = navBtn("📈  Financial Report");
        Button btnSystem  = navBtn("🖥  System");
        btnDrivers.setStyle(btnDrivers.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e   -> new DashboardScreen(stage).show());
        btnTrips.setOnAction(e  -> new TripsScreen(stage).show());
        btnUsers.setOnAction(e  -> new UsersScreen(stage).show());
        btnPricing.setOnAction(e -> new PricingScreen(stage).show());
        btnReport.setOnAction(e -> new FinancialReportScreen(stage).show());
        btnSystem.setOnAction(e -> new SystemScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { AdminService.getInstance().disconnect(); AdminSession.getInstance().logout(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnDash, btnDrivers, btnTrips, btnUsers, btnPricing, btnReport, btnSystem, sp, btnOut);
        return s;
    }

    private ScrollPane buildContent() {
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
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;" +
                          "-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);

        Button btnRefresh = new Button("↻ Refresh");
        btnRefresh.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-background-radius:6px;-fx-padding:8 14;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadDrivers());

        Button btnAdd = new Button("+ Add Driver");
        btnAdd.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:8 16;-fx-background-radius:6px;-fx-cursor:hand;");
        btnAdd.setOnAction(e -> new UsersScreen(stage).show());

        lblCount = new Label("Loading...");
        lblCount.setTextFill(Color.web("#94a3b8"));
        lblCount.setFont(Font.font("Arial", 12));
        toolbar.getChildren().addAll(tfSearch, btnRefresh, btnAdd, lblCount);

        // Table
        table = new TableView<>();
        table.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
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

        TableColumn<UserDTO, String> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(c -> new TableCell<>() {
            private final Button btnDelete = new Button("Remove");
            {
                btnDelete.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-font-size:11px;-fx-padding:4 12;-fx-background-radius:4px;-fx-cursor:hand;");
                btnDelete.setOnAction(e -> {
                    UserDTO driver = getTableView().getItems().get(getIndex());
                    confirmRemove(driver);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });

        table.getColumns().addAll(colName, colPhone, colEmail, colRating, colActions);

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

        content.getChildren().addAll(title, toolbar, table);
        return content;
    }

    private void loadDrivers() {
        lblCount.setText("Loading...");
        AdminService.getInstance().requestUserList(users -> Platform.runLater(() -> {
            allDrivers.clear();
            users.stream()
                 .filter(u -> u.getRole() == UserRole.DRIVER)
                 .forEach(allDrivers::add);
            lblCount.setText(allDrivers.size() + " drivers");
        }));
    }

    private void confirmRemove(UserDTO driver) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Driver");
        confirm.setHeaderText("Remove " + driver.getFullName() + "?");
        confirm.setContentText("This will delete the driver account.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                AdminService.getInstance().deleteUser(driver.getId(), response ->
                    Platform.runLater(() -> {
                        if ("OK".equals(response)) {
                            allDrivers.remove(driver);
                            lblCount.setText(allDrivers.size() + " drivers");
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Failed to remove driver.", ButtonType.OK).showAndWait();
                        }
                    }));
            }
        });
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
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#94a3b8;-fx-padding:10px 20px;-fx-cursor:hand;-fx-background-radius:6px;");
        return btn;
    }
}
