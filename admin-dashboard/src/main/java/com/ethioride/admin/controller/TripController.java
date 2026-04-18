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

public class TripController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Label lblCount;

    @FXML private TableView<String[]> tripsTable;
    @FXML private TableColumn<String[], String> colTripId;
    @FXML private TableColumn<String[], String> colPassenger;
    @FXML private TableColumn<String[], String> colDriver;
    @FXML private TableColumn<String[], String> colPickup;
    @FXML private TableColumn<String[], String> colDropoff;
    @FXML private TableColumn<String[], String> colFare;
    @FXML private TableColumn<String[], String> colStatus;

    private ObservableList<String[]> allTrips;
    private FilteredList<String[]> filteredTrips;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.setText(I18n.get("trips.title"));
        setupTable();
        setupFilters();
        loadSampleData();
    }

    private void setupTable() {
        colTripId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colPassenger.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colDriver.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colPickup.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colDropoff.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        colFare.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[5]));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[6]));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
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
    }

    private void setupFilters() {
        cbStatusFilter.getItems().addAll("All", "PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        cbStatusFilter.setValue("All");
    }

    private void loadSampleData() {
        allTrips = FXCollections.observableArrayList(List.of(
            new String[]{"T-8821", "Hanna T.",   "Dawit B.",  "Edna Mall, Bole",      "Sarbet, Old Airport", "145.00", "IN_PROGRESS"},
            new String[]{"T-8820", "Yonas A.",   "Abebe G.",  "Meskel Square",        "Bole Airport",        "320.00", "IN_PROGRESS"},
            new String[]{"T-8819", "Sara M.",    "Tigist H.", "Piassa",               "Kazanchis",           "90.00",  "COMPLETED"},
            new String[]{"T-8818", "Biruk T.",   "Yonas A.",  "CMC",                  "Megenagna",           "75.00",  "COMPLETED"},
            new String[]{"T-8817", "Meron K.",   "—",         "Bole Medhanialem",     "Gerji",               "110.00", "CANCELLED"}
        ));
        filteredTrips = new FilteredList<>(allTrips, t -> true);
        tripsTable.setItems(filteredTrips);
        updateCount();
    }

    @FXML private void onSearch()       { applyFilter(); }
    @FXML private void onFilterChange() { applyFilter(); }

    private void applyFilter() {
        String search = tfSearch.getText().toLowerCase();
        String status = cbStatusFilter.getValue();
        filteredTrips.setPredicate(t -> {
            boolean matchSearch = search.isEmpty()
                || t[0].toLowerCase().contains(search)
                || t[1].toLowerCase().contains(search)
                || t[2].toLowerCase().contains(search);
            boolean matchStatus = "All".equals(status) || t[6].equals(status);
            return matchSearch && matchStatus;
        });
        updateCount();
    }

    private void updateCount() {
        lblCount.setText(filteredTrips.size() + " trips");
    }

    @FXML private void onRefresh()       { loadSampleData(); }
    @FXML private void onNavDashboard()  { AdminNavigator.navigateTo("/ui/dashboard.fxml"); }
    @FXML private void onNavDrivers()    { AdminNavigator.navigateTo("/ui/drivers.fxml"); }
    @FXML private void onNavTrips()      { /* already here */ }
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
