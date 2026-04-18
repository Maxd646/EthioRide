package com.ethioride.admin.controller;

import com.ethioride.admin.state.AdminSession;
import com.ethioride.shared.utils.I18n;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DriverController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Label lblCount;

    @FXML private TableView<String[]> driversTable;
    @FXML private TableColumn<String[], String> colDriverId;
    @FXML private TableColumn<String[], String> colName;
    @FXML private TableColumn<String[], String> colStatus;
    @FXML private TableColumn<String[], String> colLocation;
    @FXML private TableColumn<String[], String> colRating;
    @FXML private TableColumn<String[], String> colTripsToday;

    private ObservableList<String[]> allDrivers;
    private FilteredList<String[]> filteredDrivers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.setText(I18n.get("drivers.title"));
        setupTable();
        setupFilters();
        loadSampleData();
    }

    private void setupTable() {
        colDriverId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colLocation.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colRating.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        colTripsToday.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[5]));

        // Color-code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
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
    }

    private void setupFilters() {
        cbStatusFilter.getItems().addAll("All", "Online", "On Trip", "Offline");
        cbStatusFilter.setValue("All");
    }

    private void loadSampleData() {
        allDrivers = FXCollections.observableArrayList(List.of(
            new String[]{"DRV-001", "Abebe Girma",   "Online",  "Bole, Addis Ababa",     "4.9", "8"},
            new String[]{"DRV-002", "Tigist Haile",  "On Trip", "Kazanchis, Addis Ababa","4.7", "5"},
            new String[]{"DRV-003", "Dawit Bekele",  "Online",  "Piassa, Addis Ababa",   "4.8", "11"},
            new String[]{"DRV-004", "Hanna Tesfaye", "Offline", "—",                     "4.6", "0"},
            new String[]{"DRV-005", "Yonas Alemu",   "On Trip", "Sarbet, Addis Ababa",   "4.9", "7"}
        ));
        filteredDrivers = new FilteredList<>(allDrivers, d -> true);
        driversTable.setItems(filteredDrivers);
        updateCount();
    }

    @FXML
    private void onSearch() {
        applyFilter();
    }

    @FXML
    private void onFilterChange() {
        applyFilter();
    }

    private void applyFilter() {
        String search = tfSearch.getText().toLowerCase();
        String status = cbStatusFilter.getValue();
        filteredDrivers.setPredicate(d -> {
            boolean matchSearch = search.isEmpty()
                || d[0].toLowerCase().contains(search)
                || d[1].toLowerCase().contains(search);
            boolean matchStatus = "All".equals(status) || d[2].equals(status);
            return matchSearch && matchStatus;
        });
        updateCount();
    }

    private void updateCount() {
        lblCount.setText(filteredDrivers.size() + " drivers");
    }

    @FXML private void onRefresh()       { loadSampleData(); }
    @FXML private void onNavDashboard()  { AdminNavigator.navigateTo("/ui/dashboard.fxml"); }
    @FXML private void onNavDrivers()    { /* already here */ }
    @FXML private void onNavTrips()      { AdminNavigator.navigateTo("/ui/trips.fxml"); }
    @FXML private void onNavSystem()     { AdminNavigator.navigateTo("/ui/system.fxml"); }
    @FXML private void onSupport()       { /* open support */ }

    @FXML
    private void onSignOut() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of Admin Dashboard?");
        confirm.setContentText("Your session will be terminated.");
        confirm.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            AdminSession.getInstance().logout();
            AdminNavigator.navigateTo("/ui/admin_login.fxml");
        }
    }
}
