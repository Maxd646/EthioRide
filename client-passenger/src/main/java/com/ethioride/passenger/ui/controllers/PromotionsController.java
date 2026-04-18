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

public class PromotionsController implements Initializable {

    @FXML private TextField tfPromoCode;
    @FXML private VBox promoContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPromos();
    }

    private void loadPromos() {
        List<String[]> promos = List.of(
            new String[]{"🎉", "WELCOME20",  "20% off your first 3 rides",       "Expires Apr 30, 2026", "#22c55e"},
            new String[]{"⚡", "PEAKHOUR",   "ETB 50 off during peak hours",      "Expires Apr 25, 2026", "#f59e0b"},
            new String[]{"🎁", "REFER50",    "ETB 50 for each friend you refer",  "No expiry",            "#3b82f6"},
            new String[]{"🏷", "WEEKEND15",  "15% off all weekend rides",         "Expires Apr 27, 2026", "#a855f7"}
        );
        for (String[] p : promos) {
            promoContainer.getChildren().add(buildPromoCard(p));
        }
    }

    private HBox buildPromoCard(String[] p) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#1a2235; -fx-background-radius:12px; -fx-padding:16; -fx-border-color:" + p[4] + "; -fx-border-width:0 0 0 4px; -fx-border-radius:0 12 12 0;");

        StackPane icon = new StackPane();
        icon.setStyle("-fx-background-color:#111827; -fx-background-radius:10px; -fx-min-width:48px; -fx-min-height:48px;");
        Label ico = new Label(p[0]);
        ico.setStyle("-fx-font-size:22px;");
        icon.getChildren().add(ico);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label code = new Label(p[1]);
        code.setStyle("-fx-text-fill:" + p[4] + "; -fx-font-size:14px; -fx-font-weight:bold;");
        Label desc = new Label(p[2]);
        desc.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:13px;");
        Label exp = new Label(p[3]);
        exp.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
        info.getChildren().addAll(code, desc, exp);

        Button use = new Button("Use");
        use.setStyle("-fx-background-color:" + p[4] + "; -fx-text-fill:white; -fx-font-size:12px; -fx-font-weight:bold; -fx-padding:8 16; -fx-background-radius:8px; -fx-cursor:hand;");
        use.setOnAction(e -> tfPromoCode.setText(p[1]));

        card.getChildren().addAll(icon, info, use);
        return card;
    }

    @FXML private void onApplyCode() {
        String code = tfPromoCode.getText().trim();
        if (code.isEmpty()) return;
        // TODO: validate promo code via server
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Promo Code");
        a.setHeaderText("Code applied: " + code);
        a.setContentText("Your discount will be applied on your next ride.");
        a.getDialogPane().setStyle("-fx-background-color:#1a2235; -fx-border-color:#1e3a5f;");
        a.showAndWait();
    }

    @FXML private void onNavMap()        { Navigator.navigateTo("/ui/views/main.fxml"); }
    @FXML private void onNavHistory()    { Navigator.navigateTo("/ui/views/ride_history.fxml"); }
    @FXML private void onNavPayments()   { Navigator.navigateTo("/ui/views/payments.fxml"); }
    @FXML private void onNavPromotions() { /* already here */ }
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
