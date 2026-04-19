package com.ethioride.passenger;

import com.ethioride.passenger.ui.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the Passenger client.
 * Pure JavaFX — no FXML.
 */
public class PassengerApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("EthioRide — Passenger");
        stage.setMinWidth(480);
        stage.setMinHeight(640);
        stage.setResizable(false);

        new LoginScreen(stage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
