package com.ethioride.passenger.ui.controllers;

import com.ethioride.passenger.state.SessionState;
import com.ethioride.passenger.ui.navigation.Navigator;
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

    @FXML private Label lblWalletBalance;
    @FXML private VBox paymentMethodsContainer;
    @FXML private ComboBox<String> cbTxFilter;
    @FXML private VBox txContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblWalletBalance.setText("1,250.00");
        cbTxFilter.getItems().addAll("All", "Ride Payment", "Top Up", "Refund");
        cbTxFilter.setValue("All");
        loadPaymentMethods();
        loadTransactions();
    }

    private void loadPaymentMethods() {
        List<String[]> methods = List.of(
            new String[]{"💳", "Telebirr",    "•••• 4521", "Default"},
            new String[]{"🏦", "CBE Birr",    "•••• 8832", ""},
            new String[]{"💵", "Cash",         "",          ""}
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
            info.getChildren().add(name);
            if (!m[2].isEmpty()) {
                Label num = new Label(m[2]);
                num.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
                info.getChildren().add(num);
            }
            if (!m[3].isEmpty()) {
                Label def = new Label(m[3]);
                def.setStyle("-fx-background-color:#1e3a5f; -fx-text-fill:#3b82f6; -fx-padding:2 8; -fx-background-radius:4px; -fx-font-size:10px;");
                row.getChildren().addAll(icon, info, def);
            } else {
                row.getChildren().addAll(icon, info);
            }
            paymentMethodsContainer.getChildren().add(row);
        }
    }

    private void loadTransactions() {
        List<String[]> txs = List.of(
            new String[]{"Apr 18, 2026", "Ride — Edna Mall → Sarbet",  "-145.00", "debit"},
            new String[]{"Apr 18, 2026", "Top Up via Telebirr",         "+500.00", "credit"},
            new String[]{"Apr 17, 2026", "Ride — Bole Medhanialem → Gerji", "-110.00", "debit"},
            new String[]{"Apr 16, 2026", "Refund — Cancelled Trip",     "+110.00", "credit"},
            new String[]{"Apr 15, 2026", "Ride — CMC → Megenagna",      "-75.00",  "debit"}
        );
        renderTransactions(txs);
    }

    private void renderTransactions(List<String[]> txs) {
        txContainer.getChildren().clear();
        for (String[] tx : txs) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color:transparent transparent #1e3a5f transparent; -fx-border-width:0 0 1 0; -fx-padding:10 0;");
            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label desc = new Label(tx[1]);
            desc.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:13px;");
            Label date = new Label(tx[0]);
            date.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
            info.getChildren().addAll(desc, date);
            Label amount = new Label("ETB " + tx[2]);
            amount.setStyle("credit".equals(tx[3])
                ? "-fx-text-fill:#22c55e; -fx-font-size:14px; -fx-font-weight:bold;"
                : "-fx-text-fill:#f1f5f9; -fx-font-size:14px; -fx-font-weight:bold;");
            row.getChildren().addAll(info, amount);
            txContainer.getChildren().add(row);
        }
    }

    @FXML private void onTopUp()            { /* open top-up dialog */ }
    @FXML private void onWithdraw()         { /* open withdraw dialog */ }
    @FXML private void onAddPaymentMethod() { /* open add method dialog */ }
    @FXML private void onTxFilter()         { loadTransactions(); }

    @FXML private void onNavMap()        { Navigator.navigateTo("/ui/views/main.fxml"); }
    @FXML private void onNavHistory()    { Navigator.navigateTo("/ui/views/ride_history.fxml"); }
    @FXML private void onNavPayments()   { /* already here */ }
    @FXML private void onNavPromotions() { Navigator.navigateTo("/ui/views/promotions.fxml"); }
    @FXML private void onNavSettings()   { Navigator.navigateTo("/ui/views/settings.fxml"); }
    @FXML private void onSupport()       { /* open support */ }

    @FXML private void onSignOut() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Sign Out"); a.setHeaderText("Sign out of EthioRide?");
        a.setContentText("You will be returned to the login screen.");
        a.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            SessionState.getInstance().clear();
            Navigator.navigateTo("/ui/views/login.fxml");
        }
    }
}
