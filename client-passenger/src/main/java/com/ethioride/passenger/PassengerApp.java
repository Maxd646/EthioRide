package com.ethioride.passenger;

import com.ethioride.passenger.ui.navigation.Navigator;
import com.ethioride.shared.utils.I18n;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Locale;

public class PassengerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Load i18n (default English; switch to Amharic via settings)
        I18n.load("i18n/messages");

        primaryStage.setTitle("EthioRide — Passenger Suite");
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(600);

        Navigator.init(primaryStage);
        Navigator.navigateTo("/ui/views/login.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
