package com.ethioride.driver;

import com.ethioride.driver.ui.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class DriverApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("EthioRide — Driver");
        stage.setMinWidth(480);
        stage.setMinHeight(640);
        // Login screen is fixed-size; MainScreen will set resizable(true) when it opens
        stage.setResizable(false);
        new LoginScreen(stage).show();
    }
    public static void main(String[] args) { launch(args); }
}
