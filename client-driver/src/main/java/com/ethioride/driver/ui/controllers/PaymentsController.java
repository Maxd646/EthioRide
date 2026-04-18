package com.ethioride.driver.ui.controllers;

import com.ethioride.driver.state.DriverSessionState;
import com.ethioride.driver.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PaymentsController implements Initializable {

    @FXML private Label lblTotalEarnings;
    @FXML private VBox payoutMethodsContainer;
    @FXML private VBox payoutContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTotalEarnings.setText("12,450.00");
        loadPayoutMethods();
        loadPayoutHistory();
    }

    private void loadPayoutMethods() {
        List<String[]> methods = List.of(
            new String[]{"📱", "Telebirr",  "•••• 4521", "Default"},
            new String[]{"🏦", "CBE Birr",  "•••• 8832", ""}
        );
        for (String[] m : methods) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#111827; -fx-background-radius:8px; -fx-padding:12;");
            Label icon = new Label(m[0]);
            icon.setStyle("-fx-font-size:18px;");
            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(m[1]);
            name.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:13px; -fx-font-weight:bold;");
            Label num = new Label(m[2]);
            num.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
            info.getChildren().addAll(name, num);
            row.getChildren().addAll(icon, info);
            if (!m[3].isEmpty()) {
                Label def = new Label(m[3]);
                def.setStyle("-fx-background-color:#1e3a5f; -fx-text-fill:#3b82f6; -fx-padding:2 8; -fx-background-radius:4px; -fx-font-size:10px;");
                row.getChildren().add(def);
            }
            payoutMethodsContainer.getChildren().add(row);
        }
    }

    private void loadPayoutHistory() {
        List<String[]> payouts = List.of(
            new String[]{"Apr 18, 2026", "Daily payout",  "+2,450.00"},
            new String[]{"Apr 17, 2026", "Daily payout",  "+1,980.00"},
            new String[]{"Apr 16, 2026", "Daily payout",  "+2,100.00"},
            new String[]{"Apr 15, 2026", "Daily payout",  "+1,750.00"}
        );
        for (String[] p : payouts) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent; -fx-border-width:0 0 1 0; -fx-padding:10 0;");
            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label desc = new Label(p[1]);
            desc.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:13px;");
            Label date = new Label(p[0]);
            date.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
            info.getChildren().addAll(desc, date);
            Label amount = new Label("ETB " + p[2]);
            amount.setStyle("-fx-text-fill:#22c55e; -fx-font-size:14px; -fx-font-weight:bold;");
            row.getChildren().addAll(info, amount);
            payoutContainer.getChildren().add(row);
        }
    }

    @FXML private void onWithdraw()   { /* open withdraw dialog */ }
    @FXML private void onAddMethod()  { /* open add method dialog */ }

    @FXML private void onNavMap()        { Navigator.navigateTo("/ui/views/driver_main.fxml"); }
    @FXML private void onNavHistory()    { Navigator.navigateTo("/ui/views/ride_history.fxml"); }
    @FXML private void onNavPayments()   { /* already here */ }
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
