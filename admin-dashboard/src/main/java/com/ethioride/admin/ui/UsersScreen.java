package com.ethioride.admin.ui;

import com.ethioride.admin.service.AdminService;
import com.ethioride.admin.state.AdminSession;
import com.ethioride.shared.dto.UserDTO;
import com.ethioride.shared.enums.UserRole;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class UsersScreen {
    private final Stage stage;
    private ObservableList<UserDTO> allUsers;
    private FilteredList<UserDTO> filtered;
    private TableView<UserDTO> table;
    private Label lblCount;

    public UsersScreen(Stage stage) { this.stage = stage; }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e1a;");
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        stage.setScene(new Scene(root, 1100, 700));
        stage.setResizable(true);
        stage.show();
        loadUsers();
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
        btnUsers.setStyle(btnUsers.getStyle() + "-fx-background-color:#1e3a5f;");
        btnDash.setOnAction(e    -> new DashboardScreen(stage).show());
        btnDrivers.setOnAction(e -> new DriversScreen(stage).show());
        btnTrips.setOnAction(e   -> new TripsScreen(stage).show());
        btnPricing.setOnAction(e -> new PricingScreen(stage).show());
        btnReport.setOnAction(e  -> new FinancialReportScreen(stage).show());
        btnSystem.setOnAction(e  -> new SystemScreen(stage).show());
        Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
        Button btnOut = navBtn("↩  Sign Out");
        btnOut.setOnAction(e -> { AdminService.getInstance().disconnect(); AdminSession.getInstance().logout(); new LoginScreen(stage).show(); });
        s.getChildren().addAll(logo, btnDash, btnDrivers, btnTrips, btnUsers, btnPricing, btnReport, btnSystem, sp, btnOut);
        return s;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("User Management");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#f1f5f9"));

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Search users...");
        tfSearch.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:8px 12px;");
        HBox.setHgrow(tfSearch, Priority.ALWAYS);
        
        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("All", "PASSENGER", "DRIVER", "ADMIN");
        cbRole.setValue("All");
        cbRole.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        
        Button btnAddDriver = new Button("+ Add Driver");
        btnAddDriver.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:8 16;-fx-background-radius:6px;-fx-cursor:hand;");
        btnAddDriver.setOnAction(e -> showAddUserDialog(UserRole.DRIVER));
        
        Button btnAddAdmin = new Button("+ Add Admin");
        btnAddAdmin.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:#0a0e1a;-fx-font-weight:bold;-fx-padding:8 16;-fx-background-radius:6px;-fx-cursor:hand;");
        btnAddAdmin.setOnAction(e -> showAddUserDialog(UserRole.ADMIN));
        
        lblCount = new Label("0 users");
        lblCount.setTextFill(Color.web("#94a3b8")); 
        lblCount.setFont(Font.font("Arial", 12));
        
        toolbar.getChildren().addAll(tfSearch, cbRole, btnAddDriver, btnAddAdmin, lblCount);

        // Table
        table = new TableView<>();
        table.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<UserDTO, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colName.setCellFactory(col -> styledCell("#f1f5f9"));

        TableColumn<UserDTO, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));
        colPhone.setCellFactory(col -> styledCell("#94a3b8"));

        TableColumn<UserDTO, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));
        colEmail.setCellFactory(col -> styledCell("#94a3b8"));

        TableColumn<UserDTO, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().name()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "ADMIN"     -> "-fx-text-fill:#f59e0b;-fx-font-weight:bold;";
                    case "DRIVER"    -> "-fx-text-fill:#22c55e;";
                    case "PASSENGER" -> "-fx-text-fill:#3b82f6;";
                    default          -> "-fx-text-fill:#94a3b8;";
                });
            }
        });

        TableColumn<UserDTO, String> colRating = new TableColumn<>("Rating");
        colRating.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f ★", d.getValue().getRating())));
        colRating.setCellFactory(col -> styledCell("#f59e0b"));

        TableColumn<UserDTO, String> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-font-size:11px;-fx-padding:4 12;-fx-background-radius:4px;-fx-cursor:hand;");
                btnDelete.setOnAction(e -> {
                    UserDTO user = getTableView().getItems().get(getIndex());
                    confirmDelete(user);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });

        table.getColumns().addAll(colName, colPhone, colEmail, colRole, colRating, colActions);

        allUsers = FXCollections.observableArrayList();
        filtered = new FilteredList<>(allUsers, u -> true);
        table.setItems(filtered);

        tfSearch.textProperty().addListener((o, ov, nv) -> applyFilter(nv, cbRole.getValue()));
        cbRole.setOnAction(e -> applyFilter(tfSearch.getText(), cbRole.getValue()));

        content.getChildren().addAll(title, toolbar, table);
        return content;
    }

    private void loadUsers() {
        // Fetch users from server via AdminService
        AdminService.getInstance().requestUserList(users -> {
            javafx.application.Platform.runLater(() -> {
                allUsers.clear();
                allUsers.addAll(users);
                updateCount();
            });
        });
    }

    private void applyFilter(String q, String role) {
        filtered.setPredicate(u ->
            (q.isEmpty() || u.getFullName().toLowerCase().contains(q.toLowerCase()) || u.getPhone().contains(q))
            && ("All".equals(role) || u.getRole().name().equals(role))
        );
        updateCount();
    }

    private void updateCount() {
        lblCount.setText(filtered.size() + " users");
    }

    private void showAddUserDialog(UserRole role) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Add " + role.name());

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color:#0a0e1a;");

        Label title = new Label("Add New " + role.name());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#f1f5f9"));

        TextField tfName = new TextField();
        tfName.setPromptText("Full Name");
        styleDialogField(tfName);

        TextField tfPhone = new TextField();
        tfPhone.setPromptText("+251 911 234 567");
        styleDialogField(tfPhone);

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("email@example.com");
        styleDialogField(tfEmail);

        PasswordField pfPassword = new PasswordField();
        pfPassword.setPromptText("Password");
        styleDialogField(pfPassword);

        PasswordField pfConfirm = new PasswordField();
        pfConfirm.setPromptText("Confirm Password");
        styleDialogField(pfConfirm);

        Label lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("Arial", 12));
        lblError.setVisible(false);

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#f1f5f9;-fx-padding:8 20;-fx-background-radius:6px;-fx-cursor:hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("Create " + role.name());
        btnSave.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:8 20;-fx-background-radius:6px;-fx-cursor:hand;");
        btnSave.setOnAction(e -> {
            String name = tfName.getText().trim();
            String phone = tfPhone.getText().trim();
            String email = tfEmail.getText().trim();
            String password = pfPassword.getText();
            String confirm = pfConfirm.getText();

            if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                lblError.setText("Name, phone, and password are required.");
                lblError.setVisible(true);
                return;
            }
            if (!password.equals(confirm)) {
                lblError.setText("Passwords do not match.");
                lblError.setVisible(true);
                return;
            }
            if (password.length() < 6) {
                lblError.setText("Password must be at least 6 characters.");
                lblError.setVisible(true);
                return;
            }

            // Send to server
            createUser(name, phone, email, password, role, dialog);
        });

        buttons.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(
            title,
            fieldLabel("Full Name"), tfName,
            fieldLabel("Phone Number"), tfPhone,
            fieldLabel("Email (optional)"), tfEmail,
            fieldLabel("Password"), pfPassword,
            fieldLabel("Confirm Password"), pfConfirm,
            lblError,
            buttons
        );

        dialog.setScene(new Scene(root, 400, 500));
        dialog.show();
    }

    private void createUser(String name, String phone, String email, String password, UserRole role, Stage dialog) {
        // Send to server via AdminService
        AdminService.getInstance().createUser(name, phone, email, password, role, response -> {
            Platform.runLater(() -> {
                if (response instanceof UserDTO) {
                    // Success - user created
                    UserDTO newUser = (UserDTO) response;
                    allUsers.add(newUser);
                    dialog.close();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Success");
                    success.setHeaderText(role.name() + " Created");
                    success.setContentText(name + " has been added successfully.");
                    success.showAndWait();
                } else if ("PHONE_EXISTS".equals(response)) {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText("Phone Number Already Exists");
                    error.setContentText("A user with phone number " + phone + " already exists.");
                    error.showAndWait();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText("Failed to Create User");
                    error.setContentText("An error occurred while creating the user. Please try again.");
                    error.showAndWait();
                }
            });
        });
    }

    private void confirmDelete(UserDTO user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + user.getFullName() + "?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                // Send delete request to server
                AdminService.getInstance().deleteUser(user.getId(), response -> {
                    Platform.runLater(() -> {
                        if ("OK".equals(response)) {
                            allUsers.remove(user);
                            Alert success = new Alert(Alert.AlertType.INFORMATION, "User deleted successfully.", ButtonType.OK);
                            success.setTitle("Deleted");
                            success.showAndWait();
                        } else {
                            Alert error = new Alert(Alert.AlertType.ERROR);
                            error.setTitle("Error");
                            error.setHeaderText("Failed to Delete User");
                            error.setContentText("An error occurred while deleting the user. Please try again.");
                            error.showAndWait();
                        }
                    });
                });
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

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#94a3b8"));
        l.setFont(Font.font("Arial", 12));
        return l;
    }

    private void styleDialogField(TextField f) {
        f.setStyle("-fx-background-color:#0d1526;-fx-text-fill:#f1f5f9;-fx-prompt-text-fill:#475569;-fx-border-color:#1e3a5f;-fx-border-radius:8px;-fx-background-radius:8px;-fx-padding:10px 14px;-fx-font-size:13px;");
        f.setMaxWidth(Double.MAX_VALUE);
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
