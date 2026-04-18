package com.ethioride.driver.ui.controllers;

import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.driver.ui.navigation.Navigator;
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

public class FleetController implements Initializable {

    @FXML private Label lblOnlineCount;
    @FXML private Label lblOnTripCount;
    @FXML private Label lblOfflineCount;
    @FXML private Label lblCount;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatus;

    @FXML private TableView<String[]> fleetTable;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colName;
    @FXML private TableColumn<String[], String> colStatus;
    @FXML private TableColumn<String[], String> colLocation;
    @FXML private TableColumn<String[], String> colRating;
    @FXML private TableColumn<String[], String> colTrips;

    private ObservableList<String[]> allDrivers;
    private FilteredList<String[]> filtered;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        cbStatus.getItems().addAll("All", "Online", "On Trip", "Offline");
        cbStatus.setValue("All");
        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colLocation.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colRating.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        colTrips.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[5]));

        colStatus.setCellFactory(col -> new TableCell<>() {
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
    }

    private void loadData() {
        allDrivers = FXCollections.observableArrayList(List.of(
            new String[]{"DRV-001", "Abebe Girma",   "Online",  "Bole, Addis Ababa",      "4.9", "8"},
            new String[]{"DRV-002", "Tigist Haile",  "On Trip", "Kazanchis, Addis Ababa", "4.7", "5"},
            new String[]{"DRV-003", "Dawit Bekele",  "Online",  "Piassa, Addis Ababa",    "4.8", "11"},
            new String[]{"DRV-004", "Hanna Tesfaye", "Offline", "—",                      "4.6", "0"},
            new String[]{"DRV-005", "Yonas Alemu",   "On Trip", "Sarbet, Addis Ababa",    "4.9", "7"}
        ));
        filtered = new FilteredList<>(allDrivers, d -> true);
        fleetTable.setItems(filtered);
        updateCounts();
    }

    private void updateCounts() {
        long online  = allDrivers.stream().filter(d -> "Online".equals(d[2])).count();
        long onTrip  = allDrivers.stream().filter(d -> "On Trip".equals(d[2])).count();
        long offline = allDrivers.stream().filter(d -> "Offline".equals(d[2])).count();
        lblOnlineCount.setText(String.valueOf(online));
        lblOnTripCount.setText(String.valueOf(onTrip));
        lblOfflineCount.setText(String.valueOf(offline));
        lblCount.setText(filtered.size() + " drivers");
    }

    @FXML private void onSearch()       { applyFilter(); }
    @FXML private void onStatusFilter() { applyFilter(); }

    private void applyFilter() {
        String q = tfSearch.getText().toLowerCase();
        String s = cbStatus.getValue();
        filtered.setPredicate(d ->
            (q.isEmpty() || d[0].toLowerCase().contains(q) || d[1].toLowerCase().contains(q))
            && ("All".equals(s) || d[2].equals(s))
        );
        lblCount.setText(filtered.size() + " drivers");
    }

    @FXML private void onRefresh()       { loadData(); }
    @FXML private void onTabLiveMap()    { Navigator.navigateTo("/ui/views/driver_main.fxml"); }
    @FXML private void onTabFleet()      { /* already here */ }
    @FXML private void onTabEarnings()   { Navigator.navigateTo("/ui/views/earnings.fxml"); }
    @FXML private void onTabDrivers()    { /* already here */ }
    @FXML private void onEmergency()     { /* broadcast emergency */ }
    @FXML private void onNavMap()        { Navigator.navigateTo("/ui/views/driver_main.fxml"); }
    @FXML private void onNavHistory()    { Navigator.navigateTo("/ui/views/ride_history.fxml"); }
    @FXML private void onNavPayments()   { Navigator.navigateTo("/ui/views/payments.fxml"); }
    @FXML private void onNavPromotions() { /* navigate */ }
    @FXML private void onNavSettings()   { /* navigate */ }
    @FXML private void onSupport()       { /* open support */ }

    @FXML private void onSignOut() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Sign Out"); a.setHeaderText("End your shift and sign out?");
        a.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            DriverSessionState.getInstance().clear();
            Navigator.navigateTo("/ui/views/driver_login.fxml");
        }
    }
}
